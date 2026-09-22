package com.teachnet.auth;

import com.teachnet.auth.AuthDtos.AccountType;
import com.teachnet.auth.AuthDtos.AuthResponse;
import com.teachnet.auth.AuthDtos.LoginRequest;
import com.teachnet.auth.AuthDtos.MeDto;
import com.teachnet.auth.AuthDtos.RegisterRequest;
import com.teachnet.common.ApiException;
import com.teachnet.common.Strings;
import com.teachnet.institution.Institution;
import com.teachnet.institution.InstitutionRepository;
import com.teachnet.institution.InstitutionType;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import com.teachnet.security.JwtService;
import com.teachnet.user.Role;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import com.teachnet.user.UserSummaryService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final TeacherProfileRepository profiles;
    private final InstitutionRepository institutions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserSummaryService summaries;

    public AuthService(UserRepository users, TeacherProfileRepository profiles, InstitutionRepository institutions,
                       PasswordEncoder passwordEncoder, JwtService jwtService, UserSummaryService summaries) {
        this.users = users;
        this.profiles = profiles;
        this.institutions = institutions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.summaries = summaries;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        Role role = req.accountType() == AccountType.TEACHER ? Role.TEACHER : Role.INSTITUTION;
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.fullName().trim(), role));

        String city = Strings.blankToNull(req.city());
        if (role == Role.TEACHER) {
            TeacherProfile profile = new TeacherProfile(user);
            profile.setCity(city);
            profiles.save(profile);
        } else {
            InstitutionType type = req.institutionType() != null ? req.institutionType() : InstitutionType.SCHOOL;
            Institution inst = new Institution(user, user.getFullName(), type);
            inst.setCity(city);
            institutions.save(inst);
        }
        return new AuthResponse(jwtService.issue(user), me(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }
        return new AuthResponse(jwtService.issue(user), me(user));
    }

    @Transactional(readOnly = true)
    public MeDto me(Long userId) {
        return me(users.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
    }

    private MeDto me(User user) {
        return new MeDto(user.getId(), user.getEmail(), user.getRole(), summaries.summarize(user));
    }
}

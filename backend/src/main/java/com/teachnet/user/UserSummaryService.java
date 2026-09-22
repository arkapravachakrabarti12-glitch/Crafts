package com.teachnet.user;

import com.teachnet.institution.Institution;
import com.teachnet.institution.InstitutionRepository;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds {@link UserSummary} objects in batches to avoid one query per row. */
@Service
@Transactional(readOnly = true)
public class UserSummaryService {

    private final TeacherProfileRepository profiles;
    private final InstitutionRepository institutions;

    public UserSummaryService(TeacherProfileRepository profiles, InstitutionRepository institutions) {
        this.profiles = profiles;
        this.institutions = institutions;
    }

    public UserSummary summarize(User user) {
        return summarize(List.of(user)).get(user.getId());
    }

    public Map<Long, UserSummary> summarize(Collection<User> users) {
        Map<Long, User> byId = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (byId.isEmpty()) {
            return Map.of();
        }
        Map<Long, TeacherProfile> profileByUser = profiles.findByUserIdIn(byId.keySet()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        Map<Long, Institution> institutionByOwner = institutions.findByOwnerIdIn(byId.keySet()).stream()
                .collect(Collectors.toMap(i -> i.getOwner().getId(), Function.identity()));

        Map<Long, UserSummary> result = new LinkedHashMap<>();
        for (User user : byId.values()) {
            result.put(user.getId(), build(user, profileByUser.get(user.getId()), institutionByOwner.get(user.getId())));
        }
        return result;
    }

    private UserSummary build(User user, TeacherProfile profile, Institution institution) {
        if (institution != null) {
            String subtitle = humanize(institution.getInstitutionType().name())
                    + (institution.getCity() != null ? " · " + institution.getCity() : "");
            return new UserSummary(user.getId(), institution.getName(), user.getRole(), subtitle,
                    institution.getLogoUrl(), institution.isVerified(), institution.getId());
        }
        if (profile != null) {
            return new UserSummary(user.getId(), user.getFullName(), user.getRole(), profile.getHeadline(),
                    profile.getPhotoUrl(), profile.isVerified(), null);
        }
        String subtitle = user.getRole() == Role.ADMIN ? "TeachNet team" : null;
        return new UserSummary(user.getId(), user.getFullName(), user.getRole(), subtitle, null, false, null);
    }

    private static String humanize(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

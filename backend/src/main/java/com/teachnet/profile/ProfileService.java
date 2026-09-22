package com.teachnet.profile;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.common.Strings;
import com.teachnet.network.Connection;
import com.teachnet.network.ConnectionRepository;
import com.teachnet.network.ConnectionStatus;
import com.teachnet.profile.ProfileDtos.ConnectionState;
import com.teachnet.profile.ProfileDtos.ExperienceDto;
import com.teachnet.profile.ProfileDtos.ExperienceRequest;
import com.teachnet.profile.ProfileDtos.PortfolioDto;
import com.teachnet.profile.ProfileDtos.PortfolioRequest;
import com.teachnet.profile.ProfileDtos.ProfileView;
import com.teachnet.profile.ProfileDtos.QualificationDto;
import com.teachnet.profile.ProfileDtos.QualificationRequest;
import com.teachnet.profile.ProfileDtos.SubjectDto;
import com.teachnet.profile.ProfileDtos.SubjectRequest;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import com.teachnet.profile.ProfileDtos.UpdateProfileRequest;
import com.teachnet.security.AuthUser;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final TeacherProfileRepository profiles;
    private final ConnectionRepository connections;

    public ProfileService(TeacherProfileRepository profiles, ConnectionRepository connections) {
        this.profiles = profiles;
        this.connections = connections;
    }

    @Transactional(readOnly = true)
    public ProfileView view(Long userId, AuthUser viewer) {
        TeacherProfile profile = profiles.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Teacher profile"));
        return toView(profile, viewer);
    }

    public ProfileView updateMine(AuthUser me, UpdateProfileRequest req) {
        TeacherProfile p = mine(me);
        p.getUser().setFullName(req.fullName().trim());
        p.setHeadline(Strings.blankToNull(req.headline()));
        p.setBio(Strings.blankToNull(req.bio()));
        p.setCity(Strings.blankToNull(req.city()));
        p.setState(Strings.blankToNull(req.state()));
        p.setYearsExperience(req.yearsExperience());
        p.setPhotoUrl(Strings.blankToNull(req.photoUrl()));
        p.setOpenToWork(req.openToWork());
        return save(p, me);
    }

    public ProfileView addSubject(AuthUser me, SubjectRequest req) {
        if (req.gradeFrom() > req.gradeTo()) {
            throw ApiException.badRequest("'Grade from' must not be after 'grade to'");
        }
        TeacherProfile p = mine(me);
        if (p.getSubjects().size() >= 20) {
            throw ApiException.badRequest("You can list at most 20 subjects");
        }
        TeacherSubject s = new TeacherSubject();
        s.setProfile(p);
        s.setSubject(req.subject().trim());
        s.setBoard(req.board());
        s.setGradeFrom(req.gradeFrom());
        s.setGradeTo(req.gradeTo());
        p.getSubjects().add(s);
        return save(p, me);
    }

    public ProfileView removeSubject(AuthUser me, Long id) {
        TeacherProfile p = mine(me);
        if (!p.getSubjects().removeIf(s -> s.getId().equals(id))) {
            throw ApiException.notFound("Subject");
        }
        return save(p, me);
    }

    public ProfileView addQualification(AuthUser me, QualificationRequest req) {
        TeacherProfile p = mine(me);
        Qualification q = new Qualification();
        q.setProfile(p);
        q.setDegree(req.degree().trim());
        q.setInstitute(req.institute().trim());
        q.setCompletionYear(req.completionYear());
        p.getQualifications().add(q);
        return save(p, me);
    }

    public ProfileView removeQualification(AuthUser me, Long id) {
        TeacherProfile p = mine(me);
        if (!p.getQualifications().removeIf(q -> q.getId().equals(id))) {
            throw ApiException.notFound("Qualification");
        }
        return save(p, me);
    }

    public ProfileView addExperience(AuthUser me, ExperienceRequest req) {
        if (req.endYear() != null && req.endYear() < req.startYear()) {
            throw ApiException.badRequest("End year must not be before start year");
        }
        TeacherProfile p = mine(me);
        Experience e = new Experience();
        e.setProfile(p);
        e.setTitle(req.title().trim());
        e.setOrganization(req.organization().trim());
        e.setStartYear(req.startYear());
        e.setEndYear(req.endYear());
        e.setDescription(Strings.blankToNull(req.description()));
        p.getExperiences().add(e);
        return save(p, me);
    }

    public ProfileView removeExperience(AuthUser me, Long id) {
        TeacherProfile p = mine(me);
        if (!p.getExperiences().removeIf(e -> e.getId().equals(id))) {
            throw ApiException.notFound("Experience");
        }
        return save(p, me);
    }

    public ProfileView addPortfolioItem(AuthUser me, PortfolioRequest req) {
        TeacherProfile p = mine(me);
        PortfolioItem item = new PortfolioItem();
        item.setProfile(p);
        item.setItemType(req.itemType());
        item.setTitle(req.title().trim());
        item.setUrl(req.url().trim());
        item.setDescription(Strings.blankToNull(req.description()));
        p.getPortfolio().add(item);
        return save(p, me);
    }

    public ProfileView removePortfolioItem(AuthUser me, Long id) {
        TeacherProfile p = mine(me);
        if (!p.getPortfolio().removeIf(i -> i.getId().equals(id))) {
            throw ApiException.notFound("Portfolio item");
        }
        return save(p, me);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherCard> search(String q, String city, String subject, Board board, Boolean openToWork,
                                            Integer minExperience, int page, int size) {
        var result = profiles.search(like(q), lower(city), like(subject), board, openToWork, minExperience,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "verified")
                        .and(Sort.by(Sort.Direction.DESC, "yearsExperience"))
                        .and(Sort.by("id"))));
        return PageResponse.of(result, list -> list.stream().map(ProfileService::toCard).toList());
    }

    public static TeacherCard toCard(TeacherProfile p) {
        List<String> subjects = p.getSubjects().stream().map(TeacherSubject::getSubject).distinct().toList();
        return new TeacherCard(p.getUser().getId(), p.getUser().getFullName(), p.getHeadline(), p.getCity(),
                p.getYearsExperience(), p.getPhotoUrl(), p.isVerified(), p.isOpenToWork(), subjects);
    }

    private TeacherProfile mine(AuthUser me) {
        return profiles.findByUserId(me.id())
                .orElseThrow(() -> ApiException.forbidden("Only teacher accounts have a teacher profile"));
    }

    private ProfileView save(TeacherProfile p, AuthUser me) {
        p.setUpdatedAt(Instant.now());
        // Flush so newly added child rows get ids before we render them.
        return toView(profiles.saveAndFlush(p), me);
    }

    private ProfileView toView(TeacherProfile p, AuthUser viewer) {
        Long ownerId = p.getUser().getId();
        ConnectionState state = ConnectionState.NONE;
        Long connectionId = null;
        if (viewer == null) {
            state = ConnectionState.NONE;
        } else if (viewer.id().equals(ownerId)) {
            state = ConnectionState.SELF;
        } else {
            Optional<Connection> c = connections.findBetween(viewer.id(), ownerId);
            if (c.isPresent()) {
                connectionId = c.get().getId();
                if (c.get().getStatus() == ConnectionStatus.ACCEPTED) {
                    state = ConnectionState.CONNECTED;
                } else if (c.get().getRequester().getId().equals(viewer.id())) {
                    state = ConnectionState.PENDING_SENT;
                } else {
                    state = ConnectionState.PENDING_RECEIVED;
                }
            }
        }
        return new ProfileView(
                ownerId,
                p.getUser().getFullName(),
                p.getHeadline(),
                p.getBio(),
                p.getCity(),
                p.getState(),
                p.getYearsExperience(),
                p.getPhotoUrl(),
                p.isOpenToWork(),
                p.isVerified(),
                p.getSubjects().stream()
                        .map(s -> new SubjectDto(s.getId(), s.getSubject(), s.getBoard(), s.getGradeFrom(), s.getGradeTo()))
                        .toList(),
                p.getQualifications().stream()
                        .map(q -> new QualificationDto(q.getId(), q.getDegree(), q.getInstitute(), q.getCompletionYear(),
                                q.isVerified()))
                        .toList(),
                p.getExperiences().stream()
                        .map(e -> new ExperienceDto(e.getId(), e.getTitle(), e.getOrganization(), e.getStartYear(),
                                e.getEndYear(), e.getDescription()))
                        .toList(),
                p.getPortfolio().stream()
                        .map(i -> new PortfolioDto(i.getId(), i.getItemType(), i.getTitle(), i.getUrl(), i.getDescription()))
                        .toList(),
                connections.countAccepted(ownerId),
                state,
                connectionId);
    }

    private static String lower(String value) {
        String v = Strings.blankToNull(value);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    private static String like(String value) {
        String v = lower(value);
        return v == null ? null : "%" + v + "%";
    }
}

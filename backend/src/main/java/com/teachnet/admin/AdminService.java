package com.teachnet.admin;

import com.teachnet.common.ApiException;
import com.teachnet.feed.PostRepository;
import com.teachnet.institution.Institution;
import com.teachnet.institution.InstitutionRepository;
import com.teachnet.jobs.JobOpeningRepository;
import com.teachnet.jobs.JobStatus;
import com.teachnet.notification.NotificationService;
import com.teachnet.notification.NotificationType;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import com.teachnet.profile.ProfileService;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import com.teachnet.security.AuthUser;
import com.teachnet.user.Role;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

    public record Stats(long teachers, long institutions, long posts, long openJobs, long unverifiedTeachers,
                        long unverifiedInstitutions) {}

    public record AdminInstitution(Long id, Long ownerUserId, String name, String type, String city, boolean verified,
                                  boolean ownerEnabled) {}

    private final UserRepository users;
    private final TeacherProfileRepository profiles;
    private final InstitutionRepository institutions;
    private final PostRepository posts;
    private final JobOpeningRepository jobs;
    private final NotificationService notifications;

    public AdminService(UserRepository users, TeacherProfileRepository profiles, InstitutionRepository institutions,
                        PostRepository posts, JobOpeningRepository jobs, NotificationService notifications) {
        this.users = users;
        this.profiles = profiles;
        this.institutions = institutions;
        this.posts = posts;
        this.jobs = jobs;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        return new Stats(users.countByRole(Role.TEACHER), users.countByRole(Role.INSTITUTION), posts.count(),
                jobs.countByStatus(JobStatus.OPEN), profiles.countByVerifiedFalse(),
                institutions.countByVerifiedFalse());
    }

    @Transactional(readOnly = true)
    public List<TeacherCard> teachers() {
        return profiles.findAll(Sort.by("verified").and(Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(ProfileService::toCard).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminInstitution> institutions() {
        return institutions.findAll(Sort.by("verified").and(Sort.by(Sort.Direction.DESC, "id"))).stream()
                .map(i -> new AdminInstitution(i.getId(), i.getOwner().getId(), i.getName(),
                        i.getInstitutionType().name(), i.getCity(), i.isVerified(), i.getOwner().isEnabled()))
                .toList();
    }

    public void verifyTeacher(AuthUser admin, Long userId, boolean verified) {
        TeacherProfile p = profiles.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Teacher"));
        if (verified && !p.isVerified()) {
            notifications.notify(p.getUser(), admin.id(), NotificationType.VERIFIED,
                    "Your profile is now verified. A verified badge is shown to schools and other teachers.",
                    "/profile/" + userId);
        }
        p.setVerified(verified);
    }

    public void verifyInstitution(AuthUser admin, Long institutionId, boolean verified) {
        Institution i = institutions.findById(institutionId).orElseThrow(() -> ApiException.notFound("Institution"));
        if (verified && !i.isVerified()) {
            notifications.notify(i.getOwner(), admin.id(), NotificationType.VERIFIED,
                    i.getName() + " is now verified.", "/institutions/" + institutionId);
        }
        i.setVerified(verified);
    }

    public void setEnabled(AuthUser admin, Long userId, boolean enabled) {
        if (admin.id().equals(userId)) {
            throw ApiException.badRequest("You cannot disable your own account");
        }
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        u.setEnabled(enabled);
    }
}

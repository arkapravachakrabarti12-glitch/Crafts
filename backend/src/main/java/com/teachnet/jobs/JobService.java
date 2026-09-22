package com.teachnet.jobs;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.common.Strings;
import com.teachnet.institution.Institution;
import com.teachnet.institution.InstitutionDtos.InstitutionCard;
import com.teachnet.institution.InstitutionService;
import com.teachnet.jobs.JobDtos.ApplicantDto;
import com.teachnet.jobs.JobDtos.ApplyRequest;
import com.teachnet.jobs.JobDtos.JobDetail;
import com.teachnet.jobs.JobDtos.JobRequest;
import com.teachnet.jobs.JobDtos.JobSummary;
import com.teachnet.jobs.JobDtos.MyApplicationDto;
import com.teachnet.jobs.JobDtos.MyJobRow;
import com.teachnet.notification.NotificationService;
import com.teachnet.notification.NotificationType;
import com.teachnet.profile.Board;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import com.teachnet.profile.ProfileService;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import com.teachnet.security.AuthUser;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobService {

    private final JobOpeningRepository jobs;
    private final JobApplicationRepository applications;
    private final InstitutionService institutionService;
    private final TeacherProfileRepository profiles;
    private final UserRepository users;
    private final NotificationService notifications;

    public JobService(JobOpeningRepository jobs, JobApplicationRepository applications,
                      InstitutionService institutionService, TeacherProfileRepository profiles,
                      UserRepository users, NotificationService notifications) {
        this.jobs = jobs;
        this.applications = applications;
        this.institutionService = institutionService;
        this.profiles = profiles;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> search(String q, String subject, String city, Board board, EmploymentType type,
                                           Long institutionId, int page, int size) {
        var result = jobs.search(like(q), like(subject), lower(city), board, type, institutionId,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.of(result, list -> list.stream().map(JobService::toSummary).toList());
    }

    @Transactional(readOnly = true)
    public JobDetail detail(Long jobId, AuthUser viewer) {
        JobOpening job = find(jobId);
        boolean canManage = viewer != null && job.getInstitution().getOwner().getId().equals(viewer.id());
        if (job.getStatus() == JobStatus.CLOSED && !canManage && (viewer == null || !viewer.isAdmin())) {
            // Closed jobs stay visible to applicants so they can see what they applied to.
            if (viewer == null || !applications.existsByJobIdAndTeacherId(jobId, viewer.id())) {
                throw ApiException.notFound("Job");
            }
        }
        ApplicationStatus myStatus = null;
        boolean canApply = false;
        if (viewer != null && viewer.isTeacher()) {
            myStatus = applications.findByJobIdAndTeacherId(jobId, viewer.id())
                    .map(JobApplication::getStatus).orElse(null);
            canApply = myStatus == null && job.getStatus() == JobStatus.OPEN;
        }
        Long count = canManage ? applications.countByJobId(jobId) : null;
        return new JobDetail(toSummary(job), job.getDescription(), canManage, canApply, myStatus, count);
    }

    public JobDetail create(AuthUser me, JobRequest req) {
        Institution inst = institutionService.findOwnedBy(me);
        JobOpening job = new JobOpening();
        job.setInstitution(inst);
        apply(job, req);
        jobs.save(job);
        return detail(job.getId(), me);
    }

    public JobDetail update(AuthUser me, Long jobId, JobRequest req) {
        JobOpening job = findManaged(me, jobId);
        apply(job, req);
        return detail(jobId, me);
    }

    public JobDetail setStatus(AuthUser me, Long jobId, JobStatus status) {
        findManaged(me, jobId).setStatus(status);
        return detail(jobId, me);
    }

    public void delete(AuthUser me, Long jobId) {
        JobOpening job = me.isAdmin() ? find(jobId) : findManaged(me, jobId);
        jobs.delete(job);
    }

    @Transactional(readOnly = true)
    public List<MyJobRow> myJobs(AuthUser me) {
        Institution inst = institutionService.findOwnedBy(me);
        List<JobOpening> list = jobs.findByInstitutionIdOrderByCreatedAtDesc(inst.getId());
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = applications.countByJobIds(list.stream().map(JobOpening::getId).toList()).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return list.stream().map(j -> new MyJobRow(toSummary(j), counts.getOrDefault(j.getId(), 0L))).toList();
    }

    public MyApplicationDto applyTo(AuthUser me, Long jobId, ApplyRequest req) {
        if (!me.isTeacher()) {
            throw ApiException.forbidden("Only teachers can apply to jobs");
        }
        JobOpening job = find(jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw ApiException.badRequest("This job is no longer accepting applications");
        }
        if (applications.existsByJobIdAndTeacherId(jobId, me.id())) {
            throw ApiException.conflict("You have already applied to this job");
        }
        User teacher = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        JobApplication saved = applications.save(
                new JobApplication(job, teacher, Strings.blankToNull(req == null ? null : req.coverNote())));
        notifications.notify(job.getInstitution().getOwner(), me.id(), NotificationType.JOB_APPLICATION,
                teacher.getFullName() + " applied for " + job.getTitle(), "/jobs/" + jobId + "/applicants");
        return toMyApplication(saved);
    }

    public void withdraw(AuthUser me, Long applicationId) {
        JobApplication app = applications.findById(applicationId)
                .filter(a -> a.getTeacher().getId().equals(me.id()))
                .orElseThrow(() -> ApiException.notFound("Application"));
        applications.delete(app);
    }

    @Transactional(readOnly = true)
    public List<MyApplicationDto> myApplications(AuthUser me) {
        return applications.findByTeacherIdOrderByCreatedAtDesc(me.id()).stream()
                .map(JobService::toMyApplication).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicantDto> applicants(AuthUser me, Long jobId) {
        findManaged(me, jobId);
        List<JobApplication> list = applications.findByJobIdOrderByCreatedAtDesc(jobId);
        Map<Long, TeacherProfile> byUser = profiles
                .findByUserIdIn(list.stream().map(a -> a.getTeacher().getId()).toList()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        return list.stream().map(a -> {
            TeacherProfile p = byUser.get(a.getTeacher().getId());
            TeacherCard card = p != null
                    ? ProfileService.toCard(p)
                    : new TeacherCard(a.getTeacher().getId(), a.getTeacher().getFullName(), null, null, 0, null,
                            false, false, List.of());
            return new ApplicantDto(a.getId(), card, a.getCoverNote(), a.getStatus(), a.getCreatedAt());
        }).toList();
    }

    public ApplicantDto updateApplicationStatus(AuthUser me, Long applicationId, ApplicationStatus status) {
        JobApplication app = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        findManaged(me, app.getJob().getId());
        if (app.getStatus() != status) {
            app.setStatus(status);
            notifications.notify(app.getTeacher(), me.id(), NotificationType.APPLICATION_STATUS,
                    "Your application for " + app.getJob().getTitle() + " at " + app.getJob().getInstitution().getName()
                            + " is now " + status.name().toLowerCase(Locale.ROOT),
                    "/applications");
        }
        return applicants(me, app.getJob().getId()).stream()
                .filter(a -> a.applicationId().equals(applicationId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<JobSummary> recommended(AuthUser me, int limit) {
        TeacherProfile profile = profiles.findByUserId(me.id()).orElse(null);
        if (profile == null) {
            return List.of();
        }
        Set<String> subjects = new HashSet<>();
        profile.getSubjects().forEach(s -> subjects.add(s.getSubject().toLowerCase(Locale.ROOT)));
        subjects.add(""); // keeps the IN clause valid when the teacher has no subjects yet
        String city = profile.getCity() == null ? "" : profile.getCity().toLowerCase(Locale.ROOT);
        return jobs.recommend(subjects, city, me.id(), PageRequest.of(0, Math.min(limit, 20))).stream()
                .map(JobService::toSummary).toList();
    }

    private void apply(JobOpening job, JobRequest req) {
        if (req.gradeFrom() != null && req.gradeTo() != null && req.gradeFrom() > req.gradeTo()) {
            throw ApiException.badRequest("'Grade from' must not be after 'grade to'");
        }
        if (req.salaryMin() != null && req.salaryMax() != null && req.salaryMin() > req.salaryMax()) {
            throw ApiException.badRequest("Minimum salary must not exceed maximum salary");
        }
        job.setTitle(req.title().trim());
        job.setDescription(req.description().trim());
        job.setSubject(req.subject().trim());
        job.setBoard(req.board());
        job.setGradeFrom(req.gradeFrom());
        job.setGradeTo(req.gradeTo());
        job.setEmploymentType(req.employmentType());
        job.setSalaryMin(req.salaryMin());
        job.setSalaryMax(req.salaryMax());
        String city = Strings.blankToNull(req.city());
        job.setCity(city != null ? city : job.getInstitution().getCity());
    }

    private JobOpening find(Long id) {
        return jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job"));
    }

    private JobOpening findManaged(AuthUser me, Long jobId) {
        JobOpening job = find(jobId);
        if (!job.getInstitution().getOwner().getId().equals(me.id())) {
            throw ApiException.forbidden("Only the institution that posted this job can manage it");
        }
        return job;
    }

    static JobSummary toSummary(JobOpening j) {
        Institution i = j.getInstitution();
        return new JobSummary(j.getId(), j.getTitle(), j.getSubject(), j.getBoard(), j.getGradeFrom(), j.getGradeTo(),
                j.getEmploymentType(), j.getSalaryMin(), j.getSalaryMax(), j.getCity(), j.getStatus(), j.getCreatedAt(),
                new InstitutionCard(i.getId(), i.getName(), i.getInstitutionType(), i.getCity(), i.getLogoUrl(),
                        i.isVerified()));
    }

    private static MyApplicationDto toMyApplication(JobApplication a) {
        return new MyApplicationDto(a.getId(), toSummary(a.getJob()), a.getStatus(), a.getCreatedAt());
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

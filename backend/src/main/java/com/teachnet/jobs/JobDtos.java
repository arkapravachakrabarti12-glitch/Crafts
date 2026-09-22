package com.teachnet.jobs;

import com.teachnet.institution.InstitutionDtos.InstitutionCard;
import com.teachnet.profile.Board;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class JobDtos {

    private JobDtos() {}

    public record JobSummary(
            Long id,
            String title,
            String subject,
            Board board,
            Integer gradeFrom,
            Integer gradeTo,
            EmploymentType employmentType,
            Integer salaryMin,
            Integer salaryMax,
            String city,
            JobStatus status,
            Instant createdAt,
            InstitutionCard institution) {}

    public record JobDetail(
            JobSummary job,
            String description,
            boolean canManage,
            boolean canApply,
            ApplicationStatus myApplicationStatus,
            Long applicationCount) {}

    public record MyJobRow(JobSummary job, long applicationCount) {}

    public record ApplicantDto(Long applicationId, TeacherCard teacher, String coverNote, ApplicationStatus status,
                               Instant appliedAt) {}

    public record MyApplicationDto(Long applicationId, JobSummary job, ApplicationStatus status, Instant appliedAt) {}

    public record JobRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10000) String description,
            @NotBlank @Size(max = 100) String subject,
            Board board,
            @Min(0) @Max(14) Integer gradeFrom,
            @Min(0) @Max(14) Integer gradeTo,
            @NotNull EmploymentType employmentType,
            @Min(0) Integer salaryMin,
            @Min(0) Integer salaryMax,
            @Size(max = 100) String city) {}

    public record StatusRequest(@NotNull JobStatus status) {}

    public record ApplyRequest(@Size(max = 3000) String coverNote) {}

    public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {}
}

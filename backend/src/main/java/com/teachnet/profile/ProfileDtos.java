package com.teachnet.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {}

    /** Relationship between the viewer and the profile owner. */
    public enum ConnectionState { SELF, NONE, PENDING_SENT, PENDING_RECEIVED, CONNECTED }

    public record SubjectDto(Long id, String subject, Board board, int gradeFrom, int gradeTo) {}

    public record QualificationDto(Long id, String degree, String institute, Integer completionYear, boolean verified) {}

    public record ExperienceDto(Long id, String title, String organization, int startYear, Integer endYear,
                                String description) {}

    public record PortfolioDto(Long id, PortfolioType itemType, String title, String url, String description) {}

    public record ProfileView(
            Long userId,
            String fullName,
            String headline,
            String bio,
            String city,
            String state,
            int yearsExperience,
            String photoUrl,
            boolean openToWork,
            boolean verified,
            List<SubjectDto> subjects,
            List<QualificationDto> qualifications,
            List<ExperienceDto> experiences,
            List<PortfolioDto> portfolio,
            long connectionCount,
            ConnectionState connectionState,
            Long connectionId) {}

    public record TeacherCard(
            Long userId,
            String fullName,
            String headline,
            String city,
            int yearsExperience,
            String photoUrl,
            boolean verified,
            boolean openToWork,
            List<String> subjects) {}

    private static final String URL_PATTERN = "^(https?://.*)?$";

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 200) String headline,
            @Size(max = 5000) String bio,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Min(0) @Max(60) int yearsExperience,
            @Size(max = 500) @Pattern(regexp = URL_PATTERN, message = "must start with http:// or https://")
            String photoUrl,
            boolean openToWork) {}

    public record SubjectRequest(
            @NotBlank @Size(max = 100) String subject,
            @NotNull Board board,
            @Min(0) @Max(14) int gradeFrom,
            @Min(0) @Max(14) int gradeTo) {}

    public record QualificationRequest(
            @NotBlank @Size(max = 150) String degree,
            @NotBlank @Size(max = 200) String institute,
            @Min(1950) @Max(2100) Integer completionYear) {}

    public record ExperienceRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 200) String organization,
            @Min(1950) @Max(2100) int startYear,
            @Min(1950) @Max(2100) Integer endYear,
            @Size(max = 3000) String description) {}

    public record PortfolioRequest(
            @NotNull PortfolioType itemType,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "must start with http:// or https://")
            String url,
            @Size(max = 2000) String description) {}
}

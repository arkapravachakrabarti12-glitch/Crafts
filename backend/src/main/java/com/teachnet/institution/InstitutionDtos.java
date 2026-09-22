package com.teachnet.institution;

import com.teachnet.profile.Board;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class InstitutionDtos {

    private InstitutionDtos() {}

    public record InstitutionView(
            Long id,
            Long ownerUserId,
            String name,
            InstitutionType institutionType,
            Board board,
            String city,
            String state,
            String about,
            String website,
            String logoUrl,
            boolean verified,
            long followerCount,
            long openJobCount,
            boolean following,
            boolean owner) {}

    public record InstitutionCard(Long id, String name, InstitutionType institutionType, String city,
                                  String logoUrl, boolean verified) {}

    private static final String URL_PATTERN = "^(https?://.*)?$";

    public record UpdateInstitutionRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull InstitutionType institutionType,
            Board board,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 5000) String about,
            @Size(max = 300) @Pattern(regexp = URL_PATTERN, message = "must start with http:// or https://")
            String website,
            @Size(max = 500) @Pattern(regexp = URL_PATTERN, message = "must start with http:// or https://")
            String logoUrl) {}
}

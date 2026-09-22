package com.teachnet.auth;

import com.teachnet.institution.InstitutionType;
import com.teachnet.user.Role;
import com.teachnet.user.UserSummary;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public enum AccountType { TEACHER, INSTITUTION }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @NotBlank @Size(max = 150) String fullName,
            @NotNull AccountType accountType,
            InstitutionType institutionType,
            @Size(max = 100) String city) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record MeDto(Long id, String email, Role role, UserSummary summary) {}

    public record AuthResponse(String token, MeDto user) {}
}

package com.teachnet.security;

import com.teachnet.user.Role;

/** The authenticated principal, rebuilt from the JWT on every request. */
public record AuthUser(Long id, String email, Role role) {

    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    public boolean isInstitution() {
        return role == Role.INSTITUTION;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

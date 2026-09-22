package com.teachnet.user;

/**
 * Compact view of a user shown on posts, comments, connection lists, etc.
 * For institutions {@code institutionId} points at their page; for teachers it is null.
 */
public record UserSummary(
        Long id,
        String fullName,
        Role role,
        String subtitle,
        String photoUrl,
        boolean verified,
        Long institutionId) {}

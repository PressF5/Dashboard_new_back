package org.pressf5.dashboard.dto;

public record UserIdentity(
        Long id,
        String username,
        String email,
        String roleCode,
        String roleName
) {}
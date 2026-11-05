package org.pressf5.dashboard.dto;

public record UserIdentity(
        Long id,
        String username,
        String roleCode,
        String roleName
) {}
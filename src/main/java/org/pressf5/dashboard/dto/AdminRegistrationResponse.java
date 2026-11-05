package org.pressf5.dashboard.dto;

public record AdminRegistrationResponse(
        Long id,
        String username,
        String roleCode,
        String roleName
) {
}

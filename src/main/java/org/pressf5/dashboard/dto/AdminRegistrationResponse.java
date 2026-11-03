package org.pressf5.dashboard.dto;

public record AdminRegistrationResponse(
        Long id,
        String username,
        String email,
        String roleCode,
        String roleName
) {
}

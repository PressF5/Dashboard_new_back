package org.pressf5.dashboard.dto;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long id,
        String username,
        String roleCode,
        String roleName
) {
}

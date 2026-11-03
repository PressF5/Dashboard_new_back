package org.pressf5.dashboard.dto;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long id,
        String username,
        String email,
        String roleCode,
        String roleName
) {
}

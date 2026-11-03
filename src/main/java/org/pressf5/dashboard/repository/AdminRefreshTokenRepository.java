package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);
    List<AdminRefreshToken> findAllByTokenFamilyAndRevokedAtIsNull(UUID tokenFamily);
    List<AdminRefreshToken> findAllByAdminUser_IdAndRevokedAtIsNull(Long adminUserId);
}
package org.pressf5.dashboard.repository;

import jakarta.persistence.LockModeType;
import org.pressf5.dashboard.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);
    List<AdminRefreshToken> findAllByTokenFamilyAndRevokedAtIsNull(UUID tokenFamily);
    List<AdminRefreshToken> findAllByAdminUser_IdAndRevokedAtIsNull(Long adminUserId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from AdminRefreshToken token
            where token.tokenHash = :tokenHash
            """)
    Optional<AdminRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
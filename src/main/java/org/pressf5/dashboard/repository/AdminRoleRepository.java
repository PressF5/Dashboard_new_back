package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {

    Optional<AdminRole> findByCode(String code);
    @Query("""
            SELECT role
            FROM AdminRole role
            WHERE NOT EXISTS (
                SELECT admin.id
                FROM AdminUser admin
                WHERE admin.role = role
                  AND admin.enabled = true
            )
            ORDER BY role.id
            """)
    List<AdminRole> findAvailableRoles();
}

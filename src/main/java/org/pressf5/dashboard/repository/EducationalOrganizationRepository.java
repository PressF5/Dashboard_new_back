package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.EducationalOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationalOrganizationRepository extends JpaRepository<EducationalOrganization, Long> {

    List<EducationalOrganization> findAllByOrderByNameAsc();
    List<EducationalOrganization> findAllByMsu_IdOrderByNameAsc(Long msuId);
    Optional<EducationalOrganization> findByMsu_IdAndName(Long msuId, String name);
    boolean existsByMsu_IdAndName(Long msuId, String name);
}

package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.SchoolStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolStatisticsRepository extends JpaRepository<SchoolStatistics, Long> {

    List<SchoolStatistics> findAllBySnapshot_Id(Long snapshotId);
    List<SchoolStatistics> findAllBySnapshot_IdAndOrganization_Msu_Id(Long snapshotId, Long msuId);
    void deleteAllBySnapshot_Id(Long snapshotId);
}

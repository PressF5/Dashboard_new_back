package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.PublicationMsuStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationMsuStatisticsRepository extends JpaRepository<PublicationMsuStatistics, Long> {

    List<PublicationMsuStatistics> findAllBySnapshot_Id(Long snapshotId);
    List<PublicationMsuStatistics> findAllBySnapshot_IdAndMsu_Id(Long snapshotId, Long msuId);
    void deleteAllBySnapshot_Id(Long snapshotId);
}

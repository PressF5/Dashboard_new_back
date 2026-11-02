package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.MsuStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MsuStatisticsRepository extends JpaRepository<MsuStatistics, Long> {

    List<MsuStatistics> findAllBySnapshot_Id(Long snapshotId);
    void deleteAllBySnapshot_Id(Long snapshotId);
}

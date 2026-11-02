package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.StatisticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatisticsSnapshotRepository extends JpaRepository<StatisticsSnapshot, Long> {

    Optional<StatisticsSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    Optional<StatisticsSnapshot> findTopByOrderBySnapshotDateDesc();
    List<StatisticsSnapshot> findAllByOrderBySnapshotDateDesc();
    boolean existsBySnapshotDate(LocalDate snapshotDate);
}

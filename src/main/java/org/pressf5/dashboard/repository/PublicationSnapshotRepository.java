package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.PublicationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationSnapshotRepository extends JpaRepository<PublicationSnapshot, Long> {

    Optional<PublicationSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    Optional<PublicationSnapshot> findTopByOrderBySnapshotDateDesc();
    List<PublicationSnapshot> findAllByOrderBySnapshotDateDesc();
    boolean existsBySnapshotDate(LocalDate snapshotDate);
}

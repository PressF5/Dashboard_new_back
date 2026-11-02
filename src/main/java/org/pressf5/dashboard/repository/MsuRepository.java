package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.Msu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MsuRepository extends JpaRepository<Msu, Long> {

    Optional<Msu> findByName(String name);
    boolean existsByName(String name);
    List<Msu> findAllByOrderByNameAsc();
}

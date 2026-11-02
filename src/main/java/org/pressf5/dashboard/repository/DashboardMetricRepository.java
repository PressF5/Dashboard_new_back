package org.pressf5.dashboard.repository;

import org.pressf5.dashboard.entity.DashboardMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardMetricRepository extends JpaRepository<DashboardMetric, String> {

    List<DashboardMetric> findAllByOrderByMetricOrderAsc();
    List<DashboardMetric> findAllByCategoryKeyOrderByMetricOrderAsc(String categoryKey);
}

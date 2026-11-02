package org.pressf5.dashboard.service;

import lombok.AllArgsConstructor;
import org.pressf5.dashboard.entity.DashboardMetric;
import org.pressf5.dashboard.entity.MsuStatistics;
import org.pressf5.dashboard.entity.SchoolStatistics;
import org.pressf5.dashboard.entity.StatisticsSnapshot;
import org.pressf5.dashboard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final StatisticsSnapshotRepository statisticsSnapshotRepository;
    private final MsuRepository msuRepository;
    private final EducationalOrganizationRepository organizationRepository;
    private final SchoolStatisticsRepository schoolStatisticsRepository;
    private final MsuStatisticsRepository msuStatisticsRepository;
    private final DashboardMetricRepository dashboardMetricRepository;


    public List<Map<String, Object>> getDates() {
        return statisticsSnapshotRepository.findAllByOrderBySnapshotDateDesc()
                .stream()
                .map(snapshot -> Map.<String, Object>of("date", snapshot.getSnapshotDate().toString()))
                .toList();
    }

    public List<Map<String, Object>> getMsu() {
        return msuRepository.findAllByOrderByNameAsc()
                .stream()
                .map(msu -> Map.<String, Object>of("id", msu.getId(), "name", msu.getName()))
                .toList();
    }

    public List<Map<String, Object>> getOrganizations(Long msuId) {
        var organizations = msuId == null ? organizationRepository.findAllByOrderByNameAsc() : organizationRepository.findAllByMsu_IdOrderByNameAsc(msuId);
        return organizations.stream()
                .map(organization -> Map.<String, Object>of(
                        "id", organization.getId(),
                        "msuId", organization.getMsu().getId(),
                        "name", organization.getName()))
                .toList();
    }

    public Map<String, Object> getStatistics(LocalDate requestedDate, Long msuId, Long organizationId) {
        // Если дата не передана, используем последнюю загруженную
        // Если статистики ещё нет, возвращаем пустой результат
        StatisticsSnapshot snapshot;

        if (requestedDate == null) {
            Optional<StatisticsSnapshot> latestSnapshot = statisticsSnapshotRepository.findTopByOrderBySnapshotDateDesc();
            if (latestSnapshot.isEmpty()) {
                return Map.of(
                        "date", "",
                        "scope", "empty",
                        "metrics", List.of(),
                        "message", "Нет загруженной статистики"
                );
            }
            snapshot = latestSnapshot.get();
        } else {
            Optional<StatisticsSnapshot> requestedSnapshot = statisticsSnapshotRepository.findBySnapshotDate(requestedDate);
            if (requestedSnapshot.isEmpty()) {
                return Map.of(
                        "date", requestedDate.toString(),
                        "scope", "empty",
                        "metrics", List.of(),
                        "message", "Нет данных за выбранную дату"
                );
            }
            snapshot = requestedSnapshot.get();
        }

        String title;
        String scope;
        Map<String, Object> values;

        // Приоритет у выбранной ОО
        if (organizationId != null) {
            Optional<SchoolStatistics> statistics =
                    schoolStatisticsRepository.findBySnapshot_IdAndOrganization_Id(snapshot.getId(), organizationId);
            if (statistics.isEmpty()) {
                return Map.of(
                        "date", snapshot.getSnapshotDate().toString(),
                        "scope", "organization",
                        "metrics", List.of(),
                        "message", "Нет данных для выбранной ОО за эту дату"
                );
            }

            SchoolStatistics schoolStatistics = statistics.get();
            title = schoolStatistics.getOrganization().getName();
            values = schoolStatistics.getMetrics();
            scope = "organization";
        } else if (msuId != null) {
            Optional<MsuStatistics> statistics =
                    msuStatisticsRepository.findBySnapshot_IdAndMsu_Id(snapshot.getId(), msuId);
            if (statistics.isEmpty()) {
                return Map.of(
                        "date", snapshot.getSnapshotDate().toString(),
                        "scope", "msu",
                        "metrics", List.of(),
                        "message", "Нет данных для выбранного МСУ за эту дату"
                );
            }

            MsuStatistics msuStatistics = statistics.get();
            title = msuStatistics.getMsu().getName();
            values = msuStatistics.getMetrics();
            scope = "msu";
        } else {
            return Map.of(
                    "date", snapshot.getSnapshotDate().toString(),
                    "scope", "region",
                    "metrics", List.of(),
                    "message", "Выберите МСУ или ОО для просмотра статистики"
            );
        }

        // Получаем определения показателей из справочника
        List<DashboardMetric> definitions = dashboardMetricRepository.findAllByOrderByMetricOrderAsc();
        List<Map<String, Object>> metrics = new ArrayList<>();

        for (DashboardMetric definition : definitions) {
            Map<String, Object> metric = new LinkedHashMap<>();

            String key = definition.getMetricKey();
            Object value = values.get(key);

            metric.put("key", key);
            metric.put("label", definition.getLabel());
            metric.put("order", definition.getMetricOrder());
            metric.put("categoryKey", definition.getCategoryKey());
            metric.put("categoryLabel", definition.getCategoryLabel());
            metric.put("unit", definition.getUnit());
            metric.put("percentage", definition.isPercentage());
            metric.put("value", value);
            metric.put("hasData", value != null);

            metrics.add(metric);
        }

        return Map.of(
                "date", snapshot.getSnapshotDate().toString(),
                "scope", scope,
                "title", title,
                "metrics", metrics
        );
    }
}

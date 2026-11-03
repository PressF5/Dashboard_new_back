package org.pressf5.dashboard.service;

import lombok.AllArgsConstructor;
import org.pressf5.dashboard.entity.PublicationSnapshot;
import org.pressf5.dashboard.repository.PublicationMsuStatisticsRepository;
import org.pressf5.dashboard.repository.PublicationSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class PublicationDashboardService {

    private final PublicationSnapshotRepository publicationSnapshotRepository;
    private final PublicationMsuStatisticsRepository publicationMsuStatisticsRepository;

    public List<Map<String, Object>> getDates() {
        return publicationSnapshotRepository.findAllByOrderBySnapshotDateDesc()
                .stream()
                .map(snapshot -> Map.<String, Object>of("date", snapshot.getSnapshotDate().toString()))
                .toList();
    }

    public Map<String, Object> getStatistics(LocalDate requestedDate, Long msuId) {
        PublicationSnapshot snapshot;

        if (requestedDate == null) {
            snapshot = publicationSnapshotRepository.findTopByOrderBySnapshotDateDesc().orElse(null);

            if (snapshot == null) {
                return Map.of(
                        "date", "",
                        "metrics", List.of(),
                        "message", "Нет загруженной статистики публикаций"
                );
            }
        } else {
            snapshot = publicationSnapshotRepository.findBySnapshotDate(requestedDate).orElse(null);

            if (snapshot == null) {
                return Map.of(
                        "date", requestedDate.toString(),
                        "metrics", List.of(),
                        "message", "Нет данных за выбранную дату"
                );
            }
        }

        var statistics = msuId == null
                ? publicationMsuStatisticsRepository.findAllBySnapshot_Id(snapshot.getId())
                : publicationMsuStatisticsRepository.findAllBySnapshot_IdAndMsu_Id(snapshot.getId(), msuId);

        List<Map<String, Object>> metrics = statistics.stream()
                .map(item -> {
                    Map<String, Object> metric = new HashMap<>();
                    metric.put("msuId", item.getMsu().getId());
                    metric.put("name", item.getMsu().getName());
                    metric.put("sourceCode", item.getSourceCode());
                    metric.put("socialPublications", item.getSocialPublications());
                    metric.put("websitePublications", item.getWebsitePublications());
                    metric.put("parentChatPublications", item.getParentChatPublications());
                    metric.put("thematicVideos", item.getThematicVideos());
                    metric.put("publicationReach", item.getPublicationReach());
                    metric.put("organizationsPercent", item.getOrganizationsPercent());

                    return metric;
                })
                .toList();

        return Map.of(
                "date", snapshot.getSnapshotDate().toString(),
                "metrics", metrics
        );
    }
}

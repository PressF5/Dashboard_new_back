package org.pressf5.dashboard.service;

import lombok.AllArgsConstructor;
import org.pressf5.dashboard.entity.ImportBatch;
import org.pressf5.dashboard.repository.ImportBatchRepository;
import org.pressf5.dashboard.status.ImportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory() {
        return importBatchRepository.findTop100ByOrderByUploadedAtDesc()
                .stream()
                .map(batch -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", batch.getId());
                    result.put("uploaded_at", batch.getUploadedAt());
                    result.put("snapshot_date", batch.getSnapshotDate());
                    result.put("original_filename", batch.getOriginalFilename());
                    result.put("status", batch.getStatus());
                    result.put("message", batch.getMessage());
                    result.put("schools_read", batch.getSchoolsRead());
                    result.put("created_or_updated_msu", batch.getCreatedOrUpdatedMsu());
                    return result;
                })
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(LocalDate snapshotDate, String filename, String message) {
        ImportBatch batch = new ImportBatch();
        batch.setSnapshotDate(snapshotDate);
        batch.setOriginalFilename(filename);
        batch.setStatus(ImportStatus.FAILED);
        batch.setMessage(message);

        importBatchRepository.save(batch);
    }
}

package org.pressf5.dashboard.api;

import lombok.AllArgsConstructor;
import org.pressf5.dashboard.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ApiController {

    private final DashboardService dashboardService;
    private final ExcelImportService excelImportService;
    private final PublicationImportService publicationImportService;
    private final PublicationDashboardService publicationDashboardService;
    private final ImportBatchService importBatchService;

    @GetMapping("/dashboard/dates")
    public Object dates() {
        return dashboardService.getDates();
    }

    @GetMapping("/dashboard/msu")
    public Object msu() {
        return dashboardService.getMsu();
    }

    @GetMapping("/dashboard/organizations")
    public Object organizations(@RequestParam(required = false) Long msuId) {
        return dashboardService.getOrganizations(msuId);
    }

    @GetMapping("/dashboard/statistics")
    public Object statistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long msuId,
            @RequestParam(required = false) Long organizationId) {
        return dashboardService.getStatistics(date, msuId, organizationId);
    }

    @PostMapping(value = "/admin/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        try {
            return ResponseEntity.ok(excelImportService.importWorkbook(file, snapshotDate));
        } catch (RuntimeException ex) {
            String filename = file == null ? null : file.getOriginalFilename();
            String message = ex.getMessage() == null ? "Неизвестная ошибка импорта" : ex.getMessage();

            if (message.length() > 1800) {
                message = message.substring(0, 1800);
            }

            importBatchService.recordFailure(filename, message);
            throw ex;
        }
    }

    @GetMapping("/publications/dates")
    public Object publicationDates() {
        return publicationDashboardService.getDates();
    }

    @GetMapping("/publications/statistics")
    public Object publicationStatistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long msuId) {
        return publicationDashboardService.getStatistics(date, msuId);
    }

    @PostMapping(value = "/admin/publications/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPublications(@RequestPart("file") MultipartFile file, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        try {
            return ResponseEntity.ok(publicationImportService.importWorkbook(file, snapshotDate));
        } catch (RuntimeException ex) {
            String filename = file == null ? null : file.getOriginalFilename();
            String message = ex.getMessage() == null ? "Неизвестная ошибка импорта" : ex.getMessage();

            if (message.length() > 1800) {
                message = message.substring(0, 1800);
            }

            importBatchService.recordFailure(filename, "Публикации: " + message);
            throw ex;
        }
    }

    @GetMapping("/admin/imports")
    public Object importHistory() {
        return importBatchService.getHistory();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

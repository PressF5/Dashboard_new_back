package org.pressf5.dashboard.service;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.pressf5.dashboard.entity.*;
import org.pressf5.dashboard.repository.*;
import org.pressf5.dashboard.status.ImportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@AllArgsConstructor
public class ExcelImportService {
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private final MsuRepository msuRepository;
    private final EducationalOrganizationRepository organizationRepository;
    private final DashboardMetricRepository dashboardMetricRepository;
    private final StatisticsSnapshotRepository statisticsSnapshotRepository;
    private final ImportBatchRepository importBatchRepository;
    private final SchoolStatisticsRepository schoolStatisticsRepository;
    private final MsuStatisticsRepository msuStatisticsRepository;

    @Transactional
    public Map<String, Object> importWorkbook(MultipartFile upload) {
        if (upload == null || upload.isEmpty())
            throw new IllegalArgumentException("Выберите Excel-файл.");

        String filename = Optional.ofNullable(upload.getOriginalFilename()).orElse("report.xlsx");
        String lower = filename.toLowerCase(Locale.ROOT);

        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls"))
            throw new IllegalArgumentException("Поддерживаются файлы .xlsx и .xls.");

        LocalDate date = LocalDate.now(ZONE);
        Path temp = null;
        try {
            temp = Files.createTempFile("school-dashboard-", lower.endsWith(".xls") ? ".xls" : ".xlsx");
            upload.transferTo(temp);

            try (InputStream in = Files.newInputStream(temp); Workbook workbook = WorkbookFactory.create(in)) {
                Sheet general = workbook.getSheet("Общее");

                if (general == null)
                    throw new IllegalArgumentException("В книге отсутствует лист «Общее».");
                Row headersRow = general.getRow(1);

                if (headersRow == null || headersRow.getLastCellNum() < 2)
                    throw new IllegalArgumentException("Не найдена строка заголовков на листе «Общее».");

                List<Metric> metrics = buildMetrics(headersRow, general);
                upsertDefinitions(metrics);

                ImportBatch batch = new ImportBatch();
                batch.setSnapshotDate(date);
                batch.setOriginalFilename(filename);
                batch.setStatus(ImportStatus.PROCESSING);
                batch.setMessage("Обработка файла");
                batch = importBatchRepository.save(batch);

                StatisticsSnapshot snapshot = statisticsSnapshotRepository
                        .findBySnapshotDate(date)
                        .orElseGet(StatisticsSnapshot::new);
                snapshot.setSnapshotDate(date);
                snapshot.setSourceImport(batch);
                snapshot = statisticsSnapshotRepository.save(snapshot);

                schoolStatisticsRepository.deleteAllBySnapshot_Id(snapshot.getId());
                msuStatisticsRepository.deleteAllBySnapshot_Id(snapshot.getId());

                // Принудительно выполняем DELETE до последующей вставки новых записей.
                schoolStatisticsRepository.flush();
                msuStatisticsRepository.flush();

                int schoolCount = 0;
                Set<String> msuNames = new LinkedHashSet<>();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

                List<SchoolStatistics> schoolStatisticsList = new ArrayList<>();
                List<MsuStatistics> msuStatisticsList = new ArrayList<>();

                for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                    Sheet sheet = workbook.getSheetAt(s);
                    String msuName = sheet.getSheetName().trim();

                    if (msuName.equalsIgnoreCase("Общее") || msuName.equalsIgnoreCase("Подсветка") || sheet.getLastRowNum() < 2)
                        continue;

                    Msu msu = ensureMsu(msuName);
                    msuNames.add(msuName);

                    Row aggregate = sheet.getRow(2);

                    if (aggregate != null)
                        msuStatisticsList.add(new MsuStatistics(snapshot, msu, readMetrics(aggregate, metrics, evaluator)));

                    for (int ri = 3; ri <= sheet.getLastRowNum(); ri++) {
                        Row row = sheet.getRow(ri);
                        if (row == null)
                            continue;

                        String orgName = cellText(row.getCell(0), evaluator).trim();

                        if (orgName.isBlank() || orgName.equalsIgnoreCase("Школа"))
                            continue;

                        EducationalOrganization organization = ensureOrganization(msu, orgName);
                        Map<String, Object> values = readMetrics(row, metrics, evaluator);

                        if (values.values().stream().allMatch(Objects::isNull))
                            continue;

                        schoolStatisticsList.add(new SchoolStatistics(snapshot, organization, values));
                        schoolCount++;
                    }
                }
                if (msuNames.isEmpty())
                    throw new IllegalArgumentException("Не найдены листы МСУ в книге.");

                msuStatisticsRepository.saveAll(msuStatisticsList);
                schoolStatisticsRepository.saveAll(schoolStatisticsList);

                batch.setStatus(ImportStatus.SUCCESS);
                batch.setMessage("Импорт завершён");
                batch.setSchoolsRead(schoolCount);
                batch.setCreatedOrUpdatedMsu(msuNames.size());

                importBatchRepository.save(batch);

                return Map.of(
                        "batchId", batch.getId(),
                        "snapshotDate", date.toString(),
                        "status", "SUCCESS",
                        "schoolsRead", schoolCount,
                        "msuCount", msuNames.size(),
                        "message", "Импорт завершён; исходный файл будет удалён.");
            }
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException iae)
                throw iae;

            throw new IllegalArgumentException("Не удалось обработать Excel: " + (ex.getMessage() == null ? "ошибка чтения файла" : ex.getMessage()), ex);
        } finally {
            if (temp != null) try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
        }
    }

    private List<Metric> buildMetrics(Row row, Sheet sheet) {
        List<Metric> result = new ArrayList<>();
        Row groups = sheet.getRow(0);
        String currentGroup = "Показатели";
        for (int c = 1; c < row.getLastCellNum(); c++) {
            String label = cellText(row.getCell(c), null).trim();
            if (label.isBlank()) continue;
            String group = groups == null ? "" : cellText(groups.getCell(c), null).trim();
            if (group.isBlank() && groups != null) {
                for (org.apache.poi.ss.util.CellRangeAddress range : sheet.getMergedRegions()) {
                    if (range.getFirstRow() <= 0 && range.getLastRow() >= 0 && range.getFirstColumn() <= c && range.getLastColumn() >= c) {
                        group = cellText(groups.getCell(range.getFirstColumn()), null).trim();
                        break;
                    }
                }
            }
            if (!group.isBlank()) currentGroup = group;
            String categoryLabel = currentGroup;
            String category = "group_" + categoryLabel.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
            boolean percent = label.contains("%") || label.toLowerCase(Locale.ROOT).contains("процент");
            result.add(new Metric("metric_" + c, label, c, category, categoryLabel, percent ? "%" : null, percent));
        }
        return result;
    }

    private void upsertDefinitions(List<Metric> metrics) {
        List<DashboardMetric> definitions = metrics.stream()
                .map(metric -> new DashboardMetric(
                        metric.key(),
                        metric.label(),
                        metric.column(),
                        metric.category(),
                        metric.categoryLabel(),
                        metric.unit(),
                        metric.percentage()))
                .toList();

        dashboardMetricRepository.saveAll(definitions);
    }

    private Map<String, Object> readMetrics(Row row, List<Metric> metrics, FormulaEvaluator evaluator) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Metric m : metrics) {
            Cell cell = row.getCell(m.column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            Object v = value(cell, evaluator);
            String format = cell == null ? "" : cell.getCellStyle().getDataFormatString();
            if (m.percentage && v instanceof Number n && format.contains("%") && n.doubleValue() >= 0 && n.doubleValue() <= 1)
                v = n.doubleValue() * 100.0;
            map.put(m.key, v);
        }
        return map;
    }

    private Object value(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        CellType type = cell.getCellType();

        if (type == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);

            if (evaluated == null) {
                return null;
            }

            return switch (evaluated.getCellType()) {
                case NUMERIC -> evaluated.getNumberValue();
                case STRING -> clean(evaluated.getStringValue());
                case BOOLEAN -> evaluated.getBooleanValue();
                default -> null;
            };
        }

        return switch (type) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> clean(cell.getStringCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            default -> null;
        };
    }

    private String cellText(Cell cell, FormulaEvaluator evaluator) {
        Object v = value(cell, evaluator);
        return v == null ? "" : String.valueOf(v);
    }

    private String clean(String value) {
        if (value == null) return null;
        String s = value.trim();
        return s.isEmpty() || s.equals("-") || s.equals("—") ? null : s;
    }

    private Msu ensureMsu(String name) {
        return msuRepository.findByName(name)
                .orElseGet(() -> {
                    Msu msu = new Msu();
                    msu.setName(name);
                    return msuRepository.save(msu);
                });
    }

    private EducationalOrganization ensureOrganization(Msu msu, String name) {
        return organizationRepository.findByMsu_IdAndName(msu.getId(), name)
                .orElseGet(() -> {
                    EducationalOrganization organization = new EducationalOrganization();
                    organization.setMsu(msu);
                    organization.setName(name);
                    return organizationRepository.save(organization);
                });
    }

    private record Metric(String key, String label, int column, String category, String categoryLabel, String unit, boolean percentage) {
    }
}

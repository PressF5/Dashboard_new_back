package org.pressf5.dashboard.service;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.pressf5.dashboard.entity.ImportBatch;
import org.pressf5.dashboard.entity.Msu;
import org.pressf5.dashboard.entity.PublicationMsuStatistics;
import org.pressf5.dashboard.entity.PublicationSnapshot;
import org.pressf5.dashboard.repository.ImportBatchRepository;
import org.pressf5.dashboard.repository.MsuRepository;
import org.pressf5.dashboard.repository.PublicationMsuStatisticsRepository;
import org.pressf5.dashboard.repository.PublicationSnapshotRepository;
import org.pressf5.dashboard.status.ImportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@AllArgsConstructor
public class PublicationImportService {
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private final ImportBatchRepository importBatchRepository;
    private final PublicationSnapshotRepository publicationSnapshotRepository;
    private final PublicationMsuStatisticsRepository publicationMsuStatisticsRepository;
    private final MsuRepository msuRepository;

    @Transactional
    public Map<String, Object> importWorkbook(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw new IllegalArgumentException("Выберите Excel-файл.");
        String filename = Optional.ofNullable(upload.getOriginalFilename()).orElse("publications.xlsx");
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls"))
            throw new IllegalArgumentException("Поддерживаются файлы .xlsx и .xls.");
        LocalDate date = LocalDate.now(ZONE);
        Path temp = null;
        try {
            temp = Files.createTempFile("publication-statistics-", lower.endsWith(".xls") ? ".xls" : ".xlsx");
            upload.transferTo(temp);
            try (InputStream in = Files.newInputStream(temp); Workbook workbook = WorkbookFactory.create(in)) {
                Sheet sheet = workbook.getSheetAt(0);
                Row header = sheet.getRow(0);
                if (header == null || sheet.getLastRowNum() < 1 || !text(header.getCell(1)).toLowerCase(Locale.ROOT).contains("округ"))
                    throw new IllegalArgumentException("Структура файла не соответствует шаблону статистики публикаций.");

                ImportBatch batch = new ImportBatch();
                batch.setSnapshotDate(date);
                batch.setOriginalFilename(filename);
                batch.setStatus(ImportStatus.PROCESSING);
                batch.setMessage("Импорт публикаций");
                batch = importBatchRepository.save(batch);
                Long batchId = batch.getId();

                PublicationSnapshot snapshot = publicationSnapshotRepository.findBySnapshotDate(date).orElseGet(PublicationSnapshot::new);
                snapshot.setSnapshotDate(date);
                snapshot.setSourceImport(batch);
                snapshot = publicationSnapshotRepository.save(snapshot);
                Long snapshotId = snapshot.getId();

                publicationMsuStatisticsRepository.deleteAllBySnapshot_Id(snapshotId);

                int count = 0;
                List<PublicationMsuStatistics> statisticsList = new ArrayList<>();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String name = text(row.getCell(1));
                    if (name.isBlank() || name.equals("-"))
                        continue;

                    Msu msu = msuRepository.findByName(name)
                            .orElseGet(() -> {
                                Msu newMsu = new Msu();
                                newMsu.setName(name);
                                return msuRepository.save(newMsu);
                            });

                    String code = text(row.getCell(0));

                    PublicationMsuStatistics statistics = new PublicationMsuStatistics();
                    statistics.setSnapshot(snapshot);
                    statistics.setMsu(msu);
                    statistics.setSourceCode(code);
                    statistics.setSocialPublications(number(row.getCell(2)));
                    statistics.setWebsitePublications(number(row.getCell(3)));
                    statistics.setParentChatPublications(number(row.getCell(4)));
                    statistics.setThematicVideos(number(row.getCell(5)));
                    statistics.setPublicationReach(number(row.getCell(6)));
                    Double organizationsPercent = decimal(row.getCell(7));

                    statistics.setOrganizationsPercent(organizationsPercent == null ? null : BigDecimal.valueOf(organizationsPercent));
                    statisticsList.add(statistics);

                    count++;
                }
                if (count == 0)
                    throw new IllegalArgumentException("В файле не найдены строки с данными по МСУ.");
                publicationMsuStatisticsRepository.saveAll(statisticsList);

                batch.setStatus(ImportStatus.SUCCESS);
                batch.setMessage("Импорт публикаций завершён");
                batch.setSchoolsRead(count);
                batch.setCreatedOrUpdatedMsu(count);
                importBatchRepository.save(batch);

                return Map.of("batchId", batchId,
                        "snapshotDate", date.toString(),
                        "status", "SUCCESS",
                        "msuCount", count,
                        "message", "Статистика публикаций импортирована.");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Не удалось обработать файл публикаций: " + (ex.getMessage() == null ? "ошибка чтения" : ex.getMessage()), ex);
        } finally {
            if (temp != null) try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
            }
        }
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING)
            return Optional.ofNullable(cell.getStringCellValue()).orElse("").trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return "";
    }

    private Long number(Cell cell) {
        String s = text(cell).replace(" ", "").replace(",", ".");
        if (s.isBlank() || s.equals("-") || s.equals("—")) return null;
        try {
            return (long) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double decimal(Cell cell) {
        String s = text(cell).replace(" ", "").replace(",", ".");
        if (s.isBlank() || s.equals("-") || s.equals("—")) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

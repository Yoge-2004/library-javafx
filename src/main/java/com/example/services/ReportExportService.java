package com.example.services;

import com.example.entities.AppConfiguration;
import com.example.entities.BorrowRequest;
import com.example.entities.BooksDB.IssueRecord;
import com.example.storage.AppPaths;
import com.example.storage.DataStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ReportExportService {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ReportExportService() {
    }

    public static Path exportOverdueReportCsv(List<IssueRecord> overdueBooks) throws IOException {
        List<String> rows = overdueBooks.stream()
                .map(record -> csv(
                        record.getBookTitle(),
                        record.getUserId(),
                        record.getDueDate().toString(),
                        String.valueOf(record.getDaysOverdue()),
                        String.format("%.2f", record.calculateFine())
                ))
                .toList();
        return writeCsv("overdue_report", "Book Title,User ID,Due Date,Days Overdue,Fine", rows);
    }

    public static Path exportIssuedBooksCsv(List<IssueRecord> records) throws IOException {
        List<String> rows = records.stream()
                .map(record -> csv(
                        record.getBookTitle(),
                        record.getUserId(),
                        record.getIssueDate().toString(),
                        record.getDueDate().toString(),
                        String.valueOf(record.getQuantity()),
                        String.valueOf(record.isReturned())
                ))
                .toList();
        return writeCsv("issued_books", "Book Title,User ID,Issue Date,Due Date,Quantity,Returned", rows);
    }

    public static Path exportBorrowRequestsCsv(List<BorrowRequest> requests) throws IOException {
        List<String> rows = requests.stream()
                .map(request -> csv(
                        request.getBookTitle(),
                        request.getUserId(),
                        String.valueOf(request.getQuantity()),
                        request.getRequestedAt().toString(),
                        request.getStatus().name(),
                        request.getProcessedBy(),
                        request.getNote()
                ))
                .toList();
        return writeCsv("borrow_requests",
                "Book Title,User ID,Quantity,Requested At,Status,Processed By,Note",
                rows);
    }

    private static Path writeCsv(String prefix, String header, List<String> rows) throws IOException {
        AppConfiguration config = AppConfigurationService.getConfiguration();
        DataStorage.ensureDirectoryExists(config.getExportDirectory());
        Path file = AppPaths.resolveExportDirectory()
                .resolve(prefix + "_" + LocalDateTime.now().format(FILE_TS) + ".csv");

        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(rows);
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    private static String csv(String... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            String value = values[i] == null ? "" : values[i];
            builder.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        return builder.toString();
    }
}

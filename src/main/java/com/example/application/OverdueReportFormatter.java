package com.example.application;

import com.example.entities.BooksDB.IssueRecord;

import java.util.List;
import java.util.Objects;

/**
 * FIXED: Use platform-specific line separator for cross-platform compatibility.
 */
public final class OverdueReportFormatter {

    private static final String LINE_SEP = System.lineSeparator();

    private OverdueReportFormatter() {
    }

    public static String format(List<IssueRecord> overdueBooks) {
        Objects.requireNonNull(overdueBooks, "overdueBooks cannot be null");

        StringBuilder report = new StringBuilder();
        report.append("Overdue Books Report:").append(LINE_SEP).append(LINE_SEP);
        double totalFines = 0;

        for (IssueRecord record : overdueBooks) {
            double fine = record.calculateFine();
            totalFines += fine;
            report.append(String.format(
                    "Book: %s%sUser: %s%sDue Date: %s%sDays Overdue: %d%sFine: $%.2f%s%s",
                    record.getBookTitle(), LINE_SEP,
                    record.getUserId(), LINE_SEP,
                    record.getDueDate(), LINE_SEP,
                    record.getDaysOverdue(), LINE_SEP,
                    fine, LINE_SEP,
                    LINE_SEP
            ));
        }

        report.append(String.format("Total Outstanding Fines: $%.2f", totalFines));
        return report.toString();
    }
}
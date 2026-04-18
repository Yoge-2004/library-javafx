package com.example.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable IssueRecord representing a book borrowing transaction.
 * Enhanced with comprehensive tracking, fine calculation, and renewal support.
 */
public final class IssueRecord implements Serializable, Comparable<IssueRecord> {
    private static final long serialVersionUID = 4L;

    private final String recordId;
    private final String isbn;
    private final String bookTitle;
    private final String userId;
    private final LocalDate issueDate;
    private final LocalDate originalDueDate;
    private final double finePerDayRate;
    private final int maxRenewals;

    private LocalDate currentDueDate;
    private int quantity;
    private boolean returned;
    private LocalDate returnDate;
    private double fineAmount;
    private int renewalCount;
    private String notes;

    /**
     * Creates a new issue record with default loan settings.
     */
    public IssueRecord(String isbn, String bookTitle, String userId, LocalDate issueDate, int quantity) {
        this(isbn, bookTitle, userId, issueDate, quantity,
                BooksDB.DEFAULT_LOAN_DAYS, BooksDB.FINE_PER_DAY, BooksDB.MAX_RENEWAL_COUNT);
    }

    /**
     * Creates a new issue record with custom loan settings.
     */
    public IssueRecord(String isbn, String bookTitle, String userId, LocalDate issueDate,
                       int quantity, int loanDays, double finePerDayRate, int maxRenewals) {
        this.recordId = UUID.randomUUID().toString();
        this.isbn = Objects.requireNonNull(isbn, "ISBN cannot be null");
        this.bookTitle = Objects.requireNonNull(bookTitle, "Book title cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.issueDate = Objects.requireNonNull(issueDate, "Issue date cannot be null");
        this.originalDueDate = issueDate.plusDays(Math.max(1, loanDays));
        this.currentDueDate = originalDueDate;
        this.quantity = Math.max(1, quantity);
        this.finePerDayRate = Math.max(0.0, finePerDayRate);
        this.maxRenewals = Math.max(0, maxRenewals);
        this.returned = false;
        this.returnDate = null;
        this.fineAmount = 0.0;
        this.renewalCount = 0;
        this.notes = null;
    }

    // --- Getters ---
    public String getRecordId() { return recordId; }
    public String getIsbn() { return isbn; }
    public String getBookTitle() { return bookTitle; }
    public String getUserId() { return userId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return currentDueDate; }
    public LocalDate getOriginalDueDate() { return originalDueDate; }
    public int getQuantity() { return quantity; }
    public boolean isReturned() { return returned; }
    public LocalDate getReturnDate() { return returnDate; }
    public double getFineAmount() { return fineAmount; }
    public int getRenewalCount() { return renewalCount; }
    public String getNotes() { return notes; }
    public double getFinePerDayRate() { return finePerDayRate; }
    public int getMaxRenewals() { return maxRenewals; }

    // --- State Checkers ---
    public boolean isPending() { return !returned; }
    public boolean isOverdue() { return !returned && getDaysOverdue() > 0; }
    public boolean isDueSoon(int daysThreshold) {
        if (returned) return false;
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), currentDueDate);
        return daysUntilDue >= 0 && daysUntilDue <= daysThreshold;
    }
    public boolean canRenew() { return !returned && renewalCount < maxRenewals; }

    // --- Calculations ---
    public long getDaysOverdue() {
        LocalDate checkDate = returned && returnDate != null ? returnDate : LocalDate.now();
        long overdueDays = ChronoUnit.DAYS.between(currentDueDate, checkDate);
        return Math.max(0, overdueDays);
    }

    public double calculateFine() {
        return getDaysOverdue() * finePerDayRate * quantity;
    }

    public long getDaysRemaining() {
        if (returned) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), currentDueDate);
        return Math.max(0, days);
    }

    // --- Mutators ---
    public void setReturned(boolean returned) {
        this.returned = returned;
        if (returned && returnDate == null) {
            this.returnDate = LocalDate.now();
            this.fineAmount = calculateFine();
        }
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = Math.max(0.0, fineAmount);
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public void setNotes(String notes) {
        this.notes = notes != null && !notes.isBlank() ? notes.trim() : null;
    }

    public void setDueDate(LocalDate dueDate) {
        this.currentDueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
    }

    /**
     * Renews the book for additional days if allowed.
     * @param additionalDays days to extend
     * @return true if renewal was successful
     */
    public boolean renew(int additionalDays) {
        if (!canRenew() || additionalDays <= 0) {
            return false;
        }
        currentDueDate = currentDueDate.plusDays(additionalDays);
        renewalCount++;
        return true;
    }

    /**
     * Gets a status summary for display.
     */
    public String getStatusText() {
        if (returned) return "Returned";
        if (isOverdue()) return "Overdue " + getDaysOverdue() + " days";
        if (isDueSoon(3)) return "Due in " + getDaysRemaining() + " days";
        return "Active - " + getDaysRemaining() + " days left";
    }

    /**
     * Gets the CSS style class for the current status.
     */
    public String getStatusStyleClass() {
        if (returned) return "chip-success";
        if (isOverdue()) return "chip-error";
        if (isDueSoon(3)) return "chip-warning";
        return "chip-primary";
    }

    // --- Object Methods ---
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IssueRecord that = (IssueRecord) obj;
        return Objects.equals(recordId, that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }

    @Override
    public int compareTo(IssueRecord other) {
        return this.issueDate.compareTo(other.issueDate);
    }

    @Override
    public String toString() {
        return String.format("IssueRecord{id=%s, book='%s', user='%s', qty=%d, issued=%s, due=%s, returned=%s}",
                recordId.substring(0, 8), bookTitle, userId, quantity, issueDate, currentDueDate, returned);
    }
}
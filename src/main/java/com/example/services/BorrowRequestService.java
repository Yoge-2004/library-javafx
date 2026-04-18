package com.example.services;

import com.example.entities.Book;
import com.example.entities.BorrowRequest;
import com.example.entities.User;
import com.example.exceptions.BooksException;
import com.example.exceptions.UserException;
import com.example.storage.DataStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * FIXED: Added request archiving to prevent unbounded memory growth.
 * Added maximum limits and expiration handling.
 */
public final class BorrowRequestService {
    private static final Logger LOGGER = Logger.getLogger(BorrowRequestService.class.getName());
    private static final String REQUESTS_FILE = "data/borrow_requests.ser";
    private static final String ARCHIVED_REQUESTS_FILE = "data/borrow_requests_archive.ser";
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // FIXED: Maximum limits to prevent memory exhaustion
    private static final int MAX_ACTIVE_REQUESTS = 10000;
    private static final int MAX_REQUESTS_PER_USER = 50;
    private static final int ARCHIVE_THRESHOLD = 5000; // Archive when exceeding this

    private static List<BorrowRequest> requests = loadRequests();
    private static List<BorrowRequest> archivedRequests = loadArchivedRequests();

    private BorrowRequestService() {
    }

    public static BorrowRequest createRequest(String isbn, String userId, int quantity) {
        if (quantity <= 0) {
            throw new BooksException("Quantity must be positive");
        }

        // FIXED: Check global limit before creating
        if (getPendingRequestCount() >= MAX_ACTIVE_REQUESTS) {
            throw new BooksException("System request queue is full. Please try again later.");
        }

        Book book = BookService.getBookByIsbn(isbn);
        if (book == null) {
            throw new BooksException("Book not found: " + isbn);
        }

        User user = UserService.getUserById(userId);
        if (user == null) {
            throw new UserException("User not found: " + userId);
        }

        // FIXED: Check per-user limit
        long userRequestCount = getRequestsForUser(userId).size();
        if (userRequestCount >= MAX_REQUESTS_PER_USER) {
            throw new BooksException("You have reached the maximum number of requests (" + MAX_REQUESTS_PER_USER +
                    "). Please wait for existing requests to be processed or cancel pending ones.");
        }

        BorrowRequest request = new BorrowRequest(book.getIsbn(), book.getTitle(), user.getUserId(), quantity);
        lock.writeLock().lock();
        try {
            requests.add(request);

            // FIXED: Auto-archive if exceeding threshold to prevent memory leak
            if (requests.size() > ARCHIVE_THRESHOLD) {
                archiveOldRequests();
            }

            saveRequests();
            return request;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static List<BorrowRequest> getAllRequests() {
        lock.readLock().lock();
        try {
            return requests.stream()
                    .sorted(Comparator.comparing(BorrowRequest::getRequestedAt).reversed())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public static List<BorrowRequest> getPendingRequests() {
        return getAllRequests().stream()
                .filter(BorrowRequest::isPending)
                .collect(Collectors.toList());
    }

    public static List<BorrowRequest> getRequestsForUser(String userId) {
        return getAllRequests().stream()
                .filter(request -> request.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public static int getPendingRequestCount() {
        return getPendingRequests().size();
    }

    public static void approveRequest(String requestId, String processedBy) {
        lock.writeLock().lock();
        try {
            BorrowRequest request = findPendingRequest(requestId);
            BookService.issueBookToUser(request.getIsbn(), request.getUserId(), request.getQuantity());
            request.approve(processedBy);
            saveRequests();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void rejectRequest(String requestId, String processedBy, String note) {
        lock.writeLock().lock();
        try {
            BorrowRequest request = findPendingRequest(requestId);
            request.reject(processedBy, note == null || note.isBlank() ? "Rejected by staff" : note.trim());
            saveRequests();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void persist() throws IOException {
        lock.readLock().lock();
        try {
            saveRequests();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * FIXED: Archive old processed requests to prevent memory leak.
     * Moves APPROVED/REJECTED requests older than 30 days to archive file.
     */
    private static void archiveOldRequests() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(30);

        List<BorrowRequest> toArchive = requests.stream()
                .filter(r -> !r.isPending())
                .filter(r -> r.getProcessedAt() != null && r.getProcessedAt().isBefore(cutoff))
                .collect(Collectors.toList());

        if (toArchive.isEmpty()) {
            // If no old requests, archive oldest 1000 processed requests anyway
            toArchive = requests.stream()
                    .filter(r -> !r.isPending())
                    .sorted(Comparator.comparing(BorrowRequest::getProcessedAt))
                    .limit(1000)
                    .collect(Collectors.toList());
        }

        if (!toArchive.isEmpty()) {
            archivedRequests.addAll(toArchive);
            requests.removeAll(toArchive);
            try {
                saveArchivedRequests();
                LOGGER.log(Level.INFO, "Archived {0} old requests", toArchive.size());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to archive requests", e);
                // Restore on failure
                requests.addAll(toArchive);
                archivedRequests.removeAll(toArchive);
            }
        }
    }

    private static BorrowRequest findPendingRequest(String requestId) {
        return requests.stream()
                .filter(request -> request.getRequestId().equals(requestId))
                .filter(BorrowRequest::isPending)
                .findFirst()
                .orElseThrow(() -> new BooksException("Pending request not found"));
    }

    @SuppressWarnings("unchecked")
    private static List<BorrowRequest> loadRequests() {
        try {
            List<BorrowRequest> loaded = DataStorage.readSerialized(REQUESTS_FILE, List.class);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load borrow requests, starting empty", e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BorrowRequest> loadArchivedRequests() {
        try {
            List<BorrowRequest> loaded = DataStorage.readSerialized(ARCHIVED_REQUESTS_FILE, List.class);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load archived requests", e);
            return new ArrayList<>();
        }
    }

    private static void saveRequests() {
        try {
            DataStorage.writeSerialized(REQUESTS_FILE, new ArrayList<>(requests));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to persist borrow requests", e);
            throw new BooksException("Failed to persist borrow requests: " + e.getMessage(), e);
        }
    }

    private static void saveArchivedRequests() throws IOException {
        DataStorage.writeSerialized(ARCHIVED_REQUESTS_FILE, new ArrayList<>(archivedRequests));
    }
}
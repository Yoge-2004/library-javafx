package com.example.test;

import com.example.entities.*;
import com.example.entities.BooksDB.IssueRecord;
import com.example.exceptions.BooksException;
import com.example.exceptions.UserException;
import com.example.exceptions.ValidationException;
import com.example.services.*;
import com.example.storage.DataStorage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Library OS - Modular Test Suite
 * 
 * Provides full coverage for:
 * 1.  Core Entities (Book, User, BorrowRequest, IssueRecord)
 * 2.  Business Logic (Fine calculation, State machines)
 * 3.  Storage & Persistence (Serialization, Singletons)
 * 4.  Services & Validation (User creation, Login, Book management)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LibraryTestSuite {

    private static Path tempHome;

    @BeforeAll
    static void initTestSuite() throws IOException {
        // Isolate tests from production data
        tempHome = Files.createTempDirectory("libraryos_test_home");
        System.setProperty("libraryos.home", tempHome.toString());
        
        // Ensure data directory exists in the temp home if needed
        Files.createDirectories(tempHome.resolve("data"));
    }

    @AfterAll
    static void teardownTestSuite() throws IOException {
        if (tempHome != null && Files.exists(tempHome)) {
            try (Stream<Path> walk = Files.walk(tempHome)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }

    // ── Helper Utilities ───────────────────────────────────────────

    private static void resetSingleton(Class<?> clazz) {
        try {
            Field instanceField = clazz.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception ignored) {}
    }

    private static Book createSampleBook(String isbn) {
        return new Book(isbn, "Test Book " + isbn, "Tester", "Software", 5);
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Entity Tests (Logic & Invariants)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unit: Core Entities")
    class EntityTests {

        @Test
        @DisplayName("Book: Validates inputs and clamps quantity")
        void testBookEntity() {
            Book b = new Book("9781234567890", "Clean Code", "Robert Martin", "Tech", 10);
            assertEquals("Clean Code", b.getTitle());
            assertEquals(10, b.getQuantity());

            assertThrows(IllegalArgumentException.class, () -> new Book(null, "T", "A", "C", 1));
            assertThrows(IllegalArgumentException.class, () -> new Book("123", "T", "A", "C", -5));
        }

        @Test
        @DisplayName("User: Validates password strength and roles")
        void testUserEntity() throws UserException {
            User u = new User("tester_01", "securePassword123");
            assertEquals("tester_01", u.getUserId());
            assertEquals(UserRole.USER, u.getRole());

            // Password too short
            assertThrows(UserException.class, () -> new User("tester_02", "123"));
            
            u.setRole(UserRole.ADMIN);
            assertTrue(u.isAdmin());
            assertTrue(u.isStaff());
        }

        @Test
        @DisplayName("BorrowRequest: State machine transitions")
        void testBorrowRequestLifecycle() {
            BorrowRequest req = new BorrowRequest("ISBN-001", "Book One", "user123", 1);
            assertTrue(req.isPending());

            req.approve("admin_user");
            assertEquals(BorrowRequest.Status.APPROVED, req.getStatus());
            assertEquals("admin_user", req.getProcessedBy());

            // Illegal transition: Approved -> Rejected
            assertThrows(IllegalStateException.class, () -> req.reject("admin_user", "Already approved"));
        }

        @Test
        @DisplayName("IssueRecord: Fine calculation and overdue logic")
        void testIssueRecordFines() {
            // Issued 20 days ago, due in 14 days -> 6 days overdue
            LocalDate issueDate = LocalDate.now().minusDays(20);
            IssueRecord record = new IssueRecord("ISBN-X", "Overdue Book", "user1", issueDate, 1);
            
            // Assume default fine is 2.0 per day
            double expectedFine = 6 * 2.0; 
            assertTrue(record.isOverdue());
            assertEquals(6, record.getDaysOverdue());
            assertEquals(expectedFine, record.calculateFine(), 0.01);
            
            record.setReturned(true);
            assertTrue(record.isReturned());
            assertNotNull(record.getReturnDate());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. Persistence & Storage
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unit: Data Storage & DBs")
    class StorageTests {

        @BeforeEach
        void clearDBs() {
            resetSingleton(BooksDB.class);
            resetSingleton(UsersDB.class);
        }

        @Test
        @DisplayName("DataStorage: Binary serialization round-trip")
        void testSerialization() throws Exception {
            Path testFile = tempHome.resolve("test_data.ser");
            List<String> original = List.of("data1", "data2", "data3");
            
            DataStorage.writeSerialized(testFile.toString(), original);
            List<String> loaded = DataStorage.readSerialized(testFile.toString(), List.class);
            
            assertEquals(original, loaded);
        }

        @Test
        @DisplayName("BooksDB: Singleton management and book CRUD")
        void testBooksDB() throws BooksException {
            BooksDB db = BooksDB.getInstance();
            db.setAutoSave(false); // Speed up tests
            
            Book b = createSampleBook("TEST-ISBN-001");
            db.addBook(b);
            
            assertNotNull(db.getBook("TEST-ISBN-001"));
            assertEquals(1, db.getBooks().size());
            
            // Duplicate ISBN
            assertThrows(BooksException.class, () -> db.addBook(createSampleBook("TEST-ISBN-001")));
            
            db.removeBook("TEST-ISBN-001");
            assertNull(db.getBook("TEST-ISBN-001"));
        }

        @Test
        @DisplayName("UsersDB: Authentication and role defaults")
        void testUsersDB() throws UserException {
            UsersDB db = UsersDB.getInstance();
            db.setAutoSave(false);
            
            User u = new User("admin_test", "password123");
            db.addUser(u); // First user should become ADMIN automatically
            
            assertEquals(UserRole.ADMIN, db.getUser("admin_test").getRole());
            assertTrue(db.authenticate("admin_test", "password123"));
            assertFalse(db.authenticate("admin_test", "wrong_pass"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Service Layer (Integration)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Integration: Services")
    class ServiceTests {

        @BeforeEach
        void reset() {
            resetSingleton(UsersDB.class);
            resetSingleton(BooksDB.class);
        }

        @Test
        @DisplayName("UserService: User creation and duplicate prevention")
        void testUserService() throws ValidationException, UserException {
            String userId = "new_user_" + System.currentTimeMillis();
            UserService.createUser(userId, "validPassword123");
            
            assertTrue(UserService.userExists(userId));
            
            // Duplicate ID
            assertThrows(UserException.class, () -> UserService.createUser(userId, "anotherPass123"));
        }

        @Test
        @DisplayName("BookService: Inventory tracking and search")
        void testBookService() throws BooksException {
            String isbn = "SEARCH-001";
            BookService.addBook(new Book(isbn, "Java Patterns", "Gang of Four", "Tech", 3));
            
            List<Book> results = BookService.searchBooks("patterns");
            assertFalse(results.isEmpty());
            assertEquals("Java Patterns", results.get(0).getTitle());
            
            Book b = BookService.getBookByIsbn(isbn);
            assertEquals(3, b.getQuantity());
        }
        
        @Test
        @DisplayName("BorrowRequestService: Request processing flow")
        void testBorrowService() {
            String isbn = "BORROW-001";
            String userId = "borrower_1";
            
            BorrowRequestService.createRequest(isbn, userId, 1);
            List<BorrowRequest> pending = BorrowRequestService.getPendingRequests();
            
            Optional<BorrowRequest> match = pending.stream()
                .filter(r -> r.getUserId().equals(userId) && r.getIsbn().equals(isbn))
                .findFirst();
                
            assertTrue(match.isPresent());
            String rid = match.get().getRequestId();
            
            BorrowRequestService.approveRequest(rid, "staff_01");
            
            BorrowRequest processed = BorrowRequestService.getRequestById(rid);
            assertEquals(BorrowRequest.Status.APPROVED, processed.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. Edge Cases & Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unit: Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Clamping negative inputs")
        void testClamping() {
            // Quantity should be clamped to at least 0 in entity, but services might handle it differently.
            // In our current Book entity constructor, it allows 0 but might throw for negative.
            Book b = new Book("CLAMP-1", "Title", "Author", "Cat", 0);
            assertEquals(0, b.getQuantity());
            
            // BorrowRequest clamps quantity to 1 minimum
            BorrowRequest br = new BorrowRequest("C", "T", "U", -5);
            assertEquals(1, br.getQuantity());
        }

        @Test
        @DisplayName("Null handling in search")
        void testNullSearch() {
            assertDoesNotThrow(() -> BookService.searchBooks(null));
            assertDoesNotThrow(() -> UserService.searchUsers(null));
        }

        @Test
        @DisplayName("Empty password validation")
        void testEmptyPassword() {
            assertThrows(UserException.class, () -> new User("valid_id", ""));
            assertThrows(UserException.class, () -> new User("valid_id", "  "));
        }

        @Test
        @DisplayName("Large note truncation in BorrowRequest")
        void testNoteTruncation() {
            BorrowRequest req = new BorrowRequest("X", "T", "U", 1);
            String hugeNote = "A".repeat(5000);
            req.reject("admin", hugeNote);
            
            assertTrue(req.getNote().length() <= 1000);
        }
    }
}

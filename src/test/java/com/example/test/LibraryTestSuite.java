package com.example.test;

import com.example.entities.*;
import com.example.entities.BooksDB.IssueRecord;
import com.example.exceptions.BooksException;
import com.example.exceptions.UserException;
import com.example.exceptions.ValidationException;
import com.example.services.*;
import com.example.storage.DataStorage;
import com.example.application.OverdueReportFormatter;

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
 * Comprehensive test suite for Library Management System.
 *
 * Coverage:
 *   - Entity validation (Book, User, BorrowRequest, IssueRecord, AppConfiguration)
 *   - UserService CRUD, authentication, role logic
 *   - BooksDB issuing, returning, querying, concurrency
 *   - BorrowRequestService lifecycle and limits
 *   - DataStorage serialization, atomicity, error paths
 *   - OverdueReportFormatter output correctness
 *   - AppConfigurationService persistence
 *   - Edge cases: nulls, empty strings, boundary values, concurrent access
 *
 * Run with:  mvn test  or  ./gradlew test
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LibraryTestSuite {

    // ═══════════════════════════════════════════════════════════════
    // SHARED TEST INFRASTRUCTURE
    // ═══════════════════════════════════════════════════════════════

    /** Temp directory created fresh for each top-level test class. */
    static Path tempDir;

    @BeforeAll
    static void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("lib-test-");
    }

    @AfterAll
    static void deleteTempDir() throws IOException {
        if (tempDir != null) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    /** Helper: create a valid book with default ISBN / title / author / category / qty. */
    static Book validBook(String isbn) {
        return new Book(isbn, "Test Title " + isbn, "Test Author", "Technology", 5);
    }

    /** Helper: create a valid user. */
    static User validUser(String userId) throws UserException {
        return new User(userId, "pass1234");
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. ENTITY TESTS — Book
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1 · Book Entity")
    class BookEntityTests {

        @Test @Order(10)
        @DisplayName("Valid construction succeeds")
        void testValidBookConstruction() {
            Book b = new Book("9780134685991", "Effective Java", "Joshua Bloch", "Technology", 3);
            assertEquals("9780134685991", b.getIsbn());
            assertEquals("Effective Java", b.getTitle());
            assertEquals("Joshua Bloch", b.getAuthor());
            assertEquals("Technology", b.getCategory());
            assertEquals(3, b.getQuantity());
            assertTrue(b.isActive());
        }

        @Test @Order(11)
        @DisplayName("Null ISBN throws IllegalArgumentException")
        void nullIsbn() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book(null, "Title", "Author", "Cat", 1));
        }

        @Test @Order(12)
        @DisplayName("Empty ISBN throws IllegalArgumentException")
        void emptyIsbn() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("   ", "Title", "Author", "Cat", 1));
        }

        @Test @Order(13)
        @DisplayName("ISBN too short (< 10 chars) throws")
        void isbnTooShort() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("123456789", "Title", "Author", "Cat", 1)); // 9 digits
        }

        @Test @Order(14)
        @DisplayName("Null title throws IllegalArgumentException")
        void nullTitle() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("1234567890", null, "Author", "Cat", 1));
        }

        @Test @Order(15)
        @DisplayName("Null author throws IllegalArgumentException")
        void nullAuthor() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("1234567890", "Title", null, "Cat", 1));
        }

        @Test @Order(16)
        @DisplayName("Negative quantity throws IllegalArgumentException")
        void negativeQuantity() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("1234567890", "Title", "Author", "Cat", -1));
        }

        @Test @Order(17)
        @DisplayName("Zero quantity is allowed (out-of-stock)")
        void zeroQuantity() {
            Book b = new Book("1234567890", "Title", "Author", "Cat", 0);
            assertEquals(0, b.getQuantity());
            assertFalse(b.isAvailable());
        }

        @Test @Order(18)
        @DisplayName("addCopies increments quantity and totalCopiesAdded")
        void addCopies() {
            Book b = validBook("1111111111");
            b.addCopies(3);
            assertEquals(8, b.getQuantity());
            assertEquals(8, b.getTotalCopiesAdded());
        }

        @Test @Order(19)
        @DisplayName("removeCopies decrements quantity but not below 0")
        void removeCopies() {
            Book b = validBook("2222222222");
            b.removeCopies(2);
            assertEquals(3, b.getQuantity());
            // Attempt to over-remove — should be a no-op (qty remains 3)
            b.removeCopies(10);
            assertEquals(3, b.getQuantity());
        }

        @Test @Order(20)
        @DisplayName("equals is based on ISBN only")
        void equalsOnIsbn() {
            Book a = validBook("3333333333");
            Book b = new Book("3333333333", "Different Title", "Other Author", "Science", 10);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test @Order(21)
        @DisplayName("getAvailabilityStatus returns correct strings")
        void availabilityStatus() {
            Book b = new Book("4444444444", "T", "A", "C", 0);
            assertEquals("Out of Stock", b.getAvailabilityStatus());

            b.addCopies(1);
            assertTrue(b.getAvailabilityStatus().startsWith("Low Stock"));

            b.addCopies(5);
            assertTrue(b.getAvailabilityStatus().startsWith("Available"));

            b.setActive(false);
            assertEquals("Inactive", b.getAvailabilityStatus());
        }

        @Test @Order(22)
        @DisplayName("Book.isValidIsbn edge cases")
        void isValidIsbn() {
            assertTrue(Book.isValidIsbn("9780134685991"));
            assertTrue(Book.isValidIsbn("0134685997"));
            assertFalse(Book.isValidIsbn(null));
            assertFalse(Book.isValidIsbn(""));
            assertFalse(Book.isValidIsbn("123"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. ENTITY TESTS — User
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2 · User Entity")
    class UserEntityTests {

        @Test @Order(30)
        @DisplayName("Valid user construction")
        void testValidUserConstruction() throws UserException {
            User u = new User("john_doe", "secret123");
            assertEquals("john_doe", u.getUserId());
            assertTrue(u.isActive());
            assertEquals(UserRole.USER, u.getRole());
        }

        @Test @Order(31)
        @DisplayName("Null userId throws UserException")
        void nullUserId() {
            assertThrows(UserException.class, () -> new User(null, "pass1234"));
        }

        @Test @Order(32)
        @DisplayName("UserId shorter than 3 chars throws UserException")
        void shortUserId() {
            assertThrows(UserException.class, () -> new User("ab", "pass1234"));
        }

        @Test @Order(33)
        @DisplayName("UserId with invalid chars throws UserException")
        void invalidCharsUserId() {
            assertThrows(UserException.class, () -> new User("user name!", "pass1234"));
        }

        @Test @Order(34)
        @DisplayName("Password shorter than 4 chars throws UserException")
        void shortPassword() {
            assertThrows(UserException.class, () -> new User("alice", "abc"));
        }

        @Test @Order(35)
        @DisplayName("Password longer than 100 chars throws UserException")
        void longPassword() {
            String longPass = "a".repeat(101);
            assertThrows(UserException.class, () -> new User("alice", longPass));
        }

        @Test @Order(36)
        @DisplayName("setEmail validates format")
        void emailValidation() throws UserException, ValidationException {
            User u = new User("alice01", "pass1234");
            u.setEmail("alice@example.com");  // valid
            assertEquals("alice@example.com", u.getEmail());

            assertThrows(ValidationException.class, () -> u.setEmail("not-an-email"));
            assertThrows(ValidationException.class, () -> u.setEmail("@nodomain"));
        }

        @Test @Order(37)
        @DisplayName("setContactNumber validates format")
        void contactValidation() throws UserException, ValidationException {
            User u = new User("bob001", "pass1234");
            u.setContactNumber("+14155552671");  // valid E.164
            assertEquals("+14155552671", u.getContactNumber());

            assertThrows(ValidationException.class, () -> u.setContactNumber("NOTAPHONE"));
        }

        @Test @Order(38)
        @DisplayName("getFullName falls back to userId when names absent")
        void fullNameFallback() throws UserException {
            User u = new User("charlie", "pass1234");
            assertEquals("charlie", u.getFullName());

            u.setFirstName("Charlie");
            assertEquals("Charlie", u.getFullName());

            u.setLastName("Brown");
            assertEquals("Charlie Brown", u.getFullName());
        }

        @Test @Order(39)
        @DisplayName("Null role defaults to USER")
        void nullRoleDefaultsToUser() throws UserException {
            User u = new User("dan001", "pass1234");
            u.setRole(null);
            assertEquals(UserRole.USER, u.getRole());
        }

        @Test @Order(40)
        @DisplayName("isAdmin and isStaff reflect role correctly")
        void roleChecks() throws UserException {
            User u = new User("eve001", "pass1234");
            u.setRole(UserRole.ADMIN);
            assertTrue(u.isAdmin());
            assertTrue(u.isStaff());

            u.setRole(UserRole.LIBRARIAN);
            assertFalse(u.isAdmin());
            assertTrue(u.isStaff());

            u.setRole(UserRole.USER);
            assertFalse(u.isAdmin());
            assertFalse(u.isStaff());
        }

        @Test @Order(41)
        @DisplayName("User.equals based on userId only")
        void equalsOnUserId() throws UserException {
            User a = new User("frank01", "pass1234");
            User b = new User("frank01", "different_pass");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test @Order(42)
        @DisplayName("hasCompleteProfile returns false when fields missing")
        void incompleteProfile() throws UserException, ValidationException {
            User u = new User("grace01", "pass1234");
            assertFalse(u.hasCompleteProfile());
            u.setEmail("grace@test.com");
            u.setContactNumber("1234567890");
            u.setFirstName("Grace");
            u.setLastName("Hopper");
            assertTrue(u.hasCompleteProfile());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ENTITY TESTS — BorrowRequest
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3 · BorrowRequest Entity")
    class BorrowRequestEntityTests {

        @Test @Order(50)
        @DisplayName("New request is PENDING")
        void newRequestIsPending() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            assertTrue(r.isPending());
            assertEquals(BorrowRequest.Status.PENDING, r.getStatus());
        }

        @Test @Order(51)
        @DisplayName("approve transitions to APPROVED")
        void approveTransition() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.approve("admin");
            assertEquals(BorrowRequest.Status.APPROVED, r.getStatus());
            assertFalse(r.isPending());
            assertEquals("admin", r.getProcessedBy());
            assertNotNull(r.getProcessedAt());
        }

        @Test @Order(52)
        @DisplayName("reject transitions to REJECTED with note")
        void rejectTransition() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.reject("admin", "Out of stock");
            assertEquals(BorrowRequest.Status.REJECTED, r.getStatus());
            assertEquals("Out of stock", r.getNote());
        }

        @Test @Order(53)
        @DisplayName("Cannot approve an already-approved request")
        void doubleApproveThrows() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.approve("admin");
            assertThrows(IllegalStateException.class, () -> r.approve("admin2"));
        }

        @Test @Order(54)
        @DisplayName("Cannot reject an already-rejected request")
        void doubleRejectThrows() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.reject("admin", "reason");
            assertThrows(IllegalStateException.class, () -> r.reject("admin2", "again"));
        }

        @Test @Order(55)
        @DisplayName("Cannot approve a rejected request")
        void approveRejectedThrows() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.reject("admin", "no");
            assertThrows(IllegalStateException.class, () -> r.approve("admin"));
        }

        @Test @Order(56)
        @DisplayName("Note longer than 1000 chars is truncated to 1000")
        void longNoteIsTruncated() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", 1);
            r.reject("admin", "x".repeat(2000));
            assertEquals(1000, r.getNote().length());
        }

        @Test @Order(57)
        @DisplayName("Null ISBN throws NullPointerException")
        void nullIsbnThrows() {
            assertThrows(NullPointerException.class,
                    () -> new BorrowRequest(null, "Book A", "user1", 1));
        }

        @Test @Order(58)
        @DisplayName("Quantity below 1 is clamped to 1")
        void quantityClamped() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book A", "user1", -5);
            assertEquals(1, r.getQuantity());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ENTITY TESTS — BooksDB.IssueRecord
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4 · BooksDB.IssueRecord")
    class IssueRecordTests {

        private IssueRecord activeRecord() {
            return new IssueRecord("ISBN001", "Book A", "user1",
                    LocalDate.now(), 1, 14, 2.0);
        }

        @Test @Order(60)
        @DisplayName("New record is not returned, not overdue")
        void newRecordState() {
            IssueRecord r = activeRecord();
            assertFalse(r.isReturned());
            assertFalse(r.isOverdue());
            assertEquals(0, r.getDaysOverdue());
            assertEquals(0.0, r.calculateFine());
        }

        @Test @Order(61)
        @DisplayName("Record issued yesterday is not overdue (due in 13 days)")
        void notYetOverdue() {
            IssueRecord r = new IssueRecord("ISBN002", "Book B", "user1",
                    LocalDate.now().minusDays(1), 1, 14, 2.0);
            assertFalse(r.isOverdue());
        }

        @Test @Order(62)
        @DisplayName("Record 20 days past due is overdue with correct fine")
        void overdueCalculation() {
            IssueRecord r = new IssueRecord("ISBN003", "Book C", "user1",
                    LocalDate.now().minusDays(34), 1, 14, 2.0);
            assertTrue(r.isOverdue());
            assertEquals(20, r.getDaysOverdue());
            assertEquals(40.0, r.calculateFine(), 0.001);
        }

        @Test @Order(63)
        @DisplayName("setReturned marks record and captures returnDate")
        void setReturned() {
            IssueRecord r = activeRecord();
            r.setReturned(true);
            assertTrue(r.isReturned());
            assertNotNull(r.getReturnDate());
            assertEquals(LocalDate.now(), r.getReturnDate());
        }

        @Test @Order(64)
        @DisplayName("renew extends due date and increments renewalCount")
        void renewExtendsDueDate() {
            IssueRecord r = activeRecord();
            LocalDate originalDue = r.getDueDate();
            boolean renewed = r.renew(14);
            assertTrue(renewed);
            assertEquals(1, r.getRenewalCount());
            assertEquals(originalDue.plusDays(14), r.getDueDate());
        }

        @Test @Order(65)
        @DisplayName("renew returns false after MAX_RENEWAL_COUNT reached")
        void renewExhausted() {
            IssueRecord r = activeRecord();
            // Exhaust all renewals
            for (int i = 0; i < BooksDB.MAX_RENEWAL_COUNT; i++) {
                assertTrue(r.renew(7));
            }
            assertFalse(r.canRenew());
            assertFalse(r.renew(7));
        }

        @Test @Order(66)
        @DisplayName("renew returns false on returned record")
        void renewReturnedRecord() {
            IssueRecord r = activeRecord();
            r.setReturned(true);
            assertFalse(r.canRenew());
            assertFalse(r.renew(7));
        }

        @Test @Order(67)
        @DisplayName("getStatusText reflects overdue state")
        void statusTextOverdue() {
            IssueRecord r = new IssueRecord("ISBN004", "Book D", "user1",
                    LocalDate.now().minusDays(20), 1, 14, 2.0);
            assertTrue(r.getStatusText().toLowerCase().contains("overdue"));
        }

        @Test @Order(68)
        @DisplayName("getStatusStyleClass returns chip-error when overdue")
        void statusClassOverdue() {
            IssueRecord r = new IssueRecord("ISBN005", "Book E", "user1",
                    LocalDate.now().minusDays(20), 1, 14, 2.0);
            assertEquals("chip-error", r.getStatusStyleClass());
        }

        @Test @Order(69)
        @DisplayName("getStatusStyleClass returns chip-success when returned")
        void statusClassReturned() {
            IssueRecord r = activeRecord();
            r.setReturned(true);
            assertEquals("chip-success", r.getStatusStyleClass());
        }

        @Test @Order(70)
        @DisplayName("isDueSoon returns true within threshold")
        void isDueSoon() {
            IssueRecord r = new IssueRecord("ISBN006", "Book F", "user1",
                    LocalDate.now().minusDays(12), 1, 14, 2.0); // due in 2 days
            assertTrue(r.isDueSoon(3));
            assertFalse(r.isDueSoon(1));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. ENTITY TESTS — AppConfiguration
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5 · AppConfiguration Entity")
    class AppConfigurationTests {

        @Test @Order(80)
        @DisplayName("Default export directory is 'exports'")
        void defaultExportDir() {
            AppConfiguration c = new AppConfiguration();
            assertEquals("exports", c.getExportDirectory());
        }

        @Test @Order(81)
        @DisplayName("setExportDirectory falls back to 'exports' on blank input")
        void blankExportDirFallback() {
            AppConfiguration c = new AppConfiguration();
            c.setExportDirectory("   ");
            assertEquals("exports", c.getExportDirectory());
            c.setExportDirectory(null);
            assertEquals("exports", c.getExportDirectory());
        }

        @Test @Order(82)
        @DisplayName("isEmailConfigured false when host is null")
        void emailNotConfiguredWhenHostNull() {
            AppConfiguration c = new AppConfiguration();
            assertFalse(c.isEmailConfigured());
        }

        @Test @Order(83)
        @DisplayName("isEmailConfigured true with host + fromAddress, smtpAuth=false")
        void emailConfiguredNoAuth() {
            AppConfiguration c = new AppConfiguration();
            c.setSmtpHost("smtp.example.com");
            c.setFromAddress("lib@example.com");
            c.setSmtpAuth(false);
            assertTrue(c.isEmailConfigured());
        }

        @Test @Order(84)
        @DisplayName("isEmailConfigured true with host + fromAddress + username when smtpAuth=true")
        void emailConfiguredWithAuth() {
            AppConfiguration c = new AppConfiguration();
            c.setSmtpHost("smtp.example.com");
            c.setFromAddress("lib@example.com");
            c.setSmtpAuth(true);
            c.setSmtpUsername("user@example.com");
            assertTrue(c.isEmailConfigured());
        }

        @Test @Order(85)
        @DisplayName("isEmailConfigured false when smtpAuth=true but no username")
        void emailConfiguredAuthRequiresUsername() {
            AppConfiguration c = new AppConfiguration();
            c.setSmtpHost("smtp.example.com");
            c.setFromAddress("lib@example.com");
            c.setSmtpAuth(true);
            // username not set
            assertFalse(c.isEmailConfigured());
        }

        @Test @Order(86)
        @DisplayName("setSmtpPort clamps to minimum 1")
        void smtpPortClamped() {
            AppConfiguration c = new AppConfiguration();
            c.setSmtpPort(-50);
            assertEquals(1, c.getSmtpPort());
        }

        @Test @Order(87)
        @DisplayName("Blank strings are stored as null (blankToNull)")
        void blankToNull() {
            AppConfiguration c = new AppConfiguration();
            c.setSmtpHost("   ");
            assertNull(c.getSmtpHost());
            c.setSmtpUsername("");
            assertNull(c.getSmtpUsername());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. UserRole enum
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6 · UserRole")
    class UserRoleTests {

        @Test @Order(90)
        @DisplayName("Only ADMIN.isAdmin() returns true")
        void onlyAdminIsAdmin() {
            assertTrue(UserRole.ADMIN.isAdmin());
            assertFalse(UserRole.LIBRARIAN.isAdmin());
            assertFalse(UserRole.USER.isAdmin());
        }

        @Test @Order(91)
        @DisplayName("ADMIN and LIBRARIAN are staff; USER is not")
        void staffCheck() {
            assertTrue(UserRole.ADMIN.isStaff());
            assertTrue(UserRole.LIBRARIAN.isStaff());
            assertFalse(UserRole.USER.isStaff());
        }

        @Test @Order(92)
        @DisplayName("displayName matches expected strings")
        void displayNames() {
            assertEquals("User", UserRole.USER.getDisplayName());
            assertEquals("Administrator", UserRole.ADMIN.getDisplayName());
            assertEquals("Librarian", UserRole.LIBRARIAN.getDisplayName());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. DataStorage
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7 · DataStorage")
    class DataStorageTests {

        private String tmpFile(String name) {
            return tempDir.resolve(name).toString();
        }

        @Test @Order(100)
        @DisplayName("Write then read returns equivalent object")
        void writeRead() throws IOException, ClassNotFoundException {
            String path = tmpFile("ds_basic.ser");
            List<String> data = List.of("alpha", "beta", "gamma");
            DataStorage.writeSerialized(path, data);
            @SuppressWarnings("unchecked")
            List<String> result = DataStorage.readSerialized(path, List.class);
            assertEquals(data, result);
        }

        @Test @Order(101)
        @DisplayName("readSerialized returns null when file does not exist")
        void readMissingFile() throws IOException, ClassNotFoundException {
            String path = tmpFile("does_not_exist.ser");
            assertNull(DataStorage.readSerialized(path, String.class));
        }

        @Test @Order(102)
        @DisplayName("fileExists returns true after write, false before")
        void fileExistsCheck() throws IOException {
            String path = tmpFile("ds_exists.ser");
            assertFalse(DataStorage.fileExists(path));
            DataStorage.writeSerialized(path, "hello");
            assertTrue(DataStorage.fileExists(path));
        }

        @Test @Order(103)
        @DisplayName("deleteFile removes the file and returns true")
        void deleteFile() throws IOException {
            String path = tmpFile("ds_delete.ser");
            DataStorage.writeSerialized(path, "bye");
            assertTrue(DataStorage.fileExists(path));
            assertTrue(DataStorage.deleteFile(path));
            assertFalse(DataStorage.fileExists(path));
        }

        @Test @Order(104)
        @DisplayName("deleteFile returns false on non-existent file")
        void deleteNonExistent() {
            assertFalse(DataStorage.deleteFile(tmpFile("ghost.ser")));
        }

        @Test @Order(105)
        @DisplayName("Overwrite existing file with new content")
        void overwrite() throws IOException, ClassNotFoundException {
            String path = tmpFile("ds_overwrite.ser");
            DataStorage.writeSerialized(path, "first");
            DataStorage.writeSerialized(path, "second");
            String result = DataStorage.readSerialized(path, String.class);
            assertEquals("second", result);
        }

        @Test @Order(106)
        @DisplayName("getFileSize returns positive value after write")
        void fileSize() throws IOException {
            String path = tmpFile("ds_size.ser");
            DataStorage.writeSerialized(path, "some data to size");
            assertTrue(DataStorage.getFileSize(path) > 0);
        }

        @Test @Order(107)
        @DisplayName("getFileSize returns -1 for missing file")
        void fileSizeMissing() {
            assertEquals(-1, DataStorage.getFileSize(tmpFile("no_file.ser")));
        }

        @Test @Order(108)
        @DisplayName("createBackup creates a backup copy")
        void createBackup() throws IOException {
            String path = tmpFile("ds_backup.ser");
            DataStorage.writeSerialized(path, "backup me");
            assertTrue(DataStorage.createBackup(path));
            // At least one backup file should exist
            try (var stream = Files.list(tempDir)) {
                assertTrue(stream.anyMatch(p -> p.toString().contains("ds_backup.ser.backup.")));
            }
        }

        @Test @Order(109)
        @DisplayName("writeSerialized throws on null filename")
        void nullFilename() {
            assertThrows(IllegalArgumentException.class,
                    () -> DataStorage.writeSerialized(null, "data"));
        }

        @Test @Order(110)
        @DisplayName("writeSerialized throws on null object")
        void nullObject() {
            assertThrows(IllegalArgumentException.class,
                    () -> DataStorage.writeSerialized(tmpFile("null_obj.ser"), null));
        }

        @Test @Order(111)
        @DisplayName("writeSerializedMap / readSerializedMap round-trip")
        void mapRoundTrip() throws IOException, ClassNotFoundException {
            String path = tmpFile("ds_map.ser");
            Map<String, List<String>> map = new LinkedHashMap<>();
            map.put("user1", List.of("ISBN001", "ISBN002"));
            map.put("user2", List.of("ISBN003"));
            DataStorage.writeSerializedMap(path, map);
            Map<String, List<String>> result = DataStorage.readSerializedMap(path);
            assertEquals(map, result);
        }

        @Test @Order(112)
        @DisplayName("Concurrent writes do not corrupt the file")
        void concurrentWrites() throws Exception {
            String path = tmpFile("ds_concurrent.ser");
            int threads = 10;
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            ExecutorService exec = Executors.newFixedThreadPool(threads);

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                futures.add(exec.submit(() -> {
                    try {
                        latch.await();
                        DataStorage.writeSerialized(path, "thread-" + idx);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            latch.countDown();
            for (Future<?> f : futures) f.get(5, TimeUnit.SECONDS);
            exec.shutdown();

            // File must exist and be readable after concurrent writes
            assertTrue(DataStorage.fileExists(path));
            String val = DataStorage.readSerialized(path, String.class);
            assertNotNull(val);
            assertTrue(val.startsWith("thread-"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. BooksDB — issue / return / query
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8 · BooksDB Operations")
    class BooksDBTests {

        private BooksDB db;
        private User testUser;

        @BeforeEach
        void setUp() throws UserException {
            resetBooksDBSingleton();
            db = BooksDB.getInstance();
            db.setAutoSave(false);
            testUser = new User("testuser01", "pass1234");
            // Remove any books loaded from disk (we only want books added in each test)
            for (com.example.entities.Book b : new java.util.ArrayList<>(db.getBooks())) {
                try { db.removeBook(b.getIsbn()); } catch (Exception ignored) {}
            }
        }

        @AfterEach
        void tearDown() {
            resetBooksDBSingleton();
        }

        @Test @Order(130)
        @DisplayName("addBook succeeds and getBook retrieves it")
        void addAndGet() throws BooksException {
            Book book = validBook("1000000001");
            db.addBook(book);
            Book found = db.getBook("1000000001");
            assertNotNull(found);
            assertEquals("Test Title 1000000001", found.getTitle());
        }

        @Test @Order(131)
        @DisplayName("addBook with duplicate ISBN throws BooksException")
        void duplicateIsbn() throws BooksException {
            db.addBook(validBook("1000000002"));
            assertThrows(BooksException.class, () -> db.addBook(validBook("1000000002")));
        }

        @Test @Order(132)
        @DisplayName("addBook with null throws BooksException")
        void addNullBook() {
            assertThrows(BooksException.class, () -> db.addBook(null));
        }

        @Test @Order(133)
        @DisplayName("modifyBook updates the stored book")
        void modifyBook() throws BooksException {
            Book book = validBook("1000000003");
            db.addBook(book);
            book.setTitle("Updated Title");
            db.modifyBook(book);
            assertEquals("Updated Title", db.getBook("1000000003").getTitle());
        }

        @Test @Order(134)
        @DisplayName("removeBook removes a non-issued book")
        void removeBook() throws BooksException {
            db.addBook(validBook("1000000004"));
            db.removeBook("1000000004");
            assertNull(db.getBook("1000000004"));
        }

        @Test @Order(135)
        @DisplayName("removeBook throws when book is currently issued")
        void removeIssuedBookThrows() throws BooksException, UserException {
            Book book = validBook("1000000005");
            db.addBook(book);
            db.issueBook("1000000005", testUser, 1);
            assertThrows(BooksException.class, () -> db.removeBook("1000000005"));
        }

        @Test @Order(136)
        @DisplayName("issueBook decrements available quantity")
        void issueDecrementsQuantity() throws BooksException {
            Book book = validBook("1000000006");
            db.addBook(book);
            db.issueBook("1000000006", testUser, 2);
            assertEquals(3, db.getBook("1000000006").getQuantity());
            assertEquals(2, db.getTotalIssued("1000000006"));
        }

        @Test @Order(137)
        @DisplayName("issueBook throws when insufficient copies")
        void issueInsufficient() throws BooksException {
            Book book = new Book("1000000007", "Title", "Author", "Cat", 1);
            db.addBook(book);
            assertThrows(BooksException.class, () -> db.issueBook("1000000007", testUser, 2));
        }

        @Test @Order(138)
        @DisplayName("issueBook respects maxBorrowLimit")
        void issueBeyondLimit() throws BooksException, UserException {
            db.setMaxBorrow(2);
            Book book = new Book("1000000008", "Title", "Author", "Cat", 10);
            db.addBook(book);
            db.issueBook("1000000008", testUser, 2); // fills limit
            assertThrows(BooksException.class,
                    () -> db.issueBook("1000000008", testUser, 1)); // exceeds
        }

        @Test @Order(139)
        @DisplayName("returnBook increments quantity and marks record returned")
        void returnBook() throws BooksException {
            Book book = validBook("1000000009");
            db.addBook(book);
            db.issueBook("1000000009", testUser, 2);
            db.returnBook("1000000009", testUser, 2);
            assertEquals(5, db.getBook("1000000009").getQuantity());
            assertEquals(0, db.getTotalIssued("1000000009"));
        }

        @Test @Order(140)
        @DisplayName("returnBook throws when user has fewer copies than requested")
        void returnMoreThanIssued() throws BooksException {
            Book book = validBook("1000000010");
            db.addBook(book);
            db.issueBook("1000000010", testUser, 1);
            assertThrows(BooksException.class,
                    () -> db.returnBook("1000000010", testUser, 3));
        }

        @Test @Order(141)
        @DisplayName("returnBook throws when user has no issued books")
        void returnNotIssued() throws BooksException {
            Book book = validBook("1000000011");
            db.addBook(book);
            assertThrows(BooksException.class,
                    () -> db.returnBook("1000000011", testUser, 1));
        }

        @Test @Order(142)
        @DisplayName("searchBooks returns all books on empty query")
        void searchEmpty() throws BooksException {
            db.addBook(validBook("1000000012"));
            db.addBook(validBook("1000000013"));
            assertEquals(2, db.searchBooks("").size());
        }

        @Test @Order(143)
        @DisplayName("searchBooks filters by title substring (case insensitive)")
        void searchByTitle() throws BooksException {
            db.addBook(new Book("1000000014", "Java Programming", "Author", "Tech", 3));
            db.addBook(new Book("1000000015", "Python Basics", "Author", "Tech", 3));
            List<Book> results = db.searchBooks("java");
            assertEquals(1, results.size());
            assertEquals("Java Programming", results.get(0).getTitle());
        }

        @Test @Order(144)
        @DisplayName("getTotalFines sums overdue fines across all users")
        void totalFines() throws BooksException, UserException {
            // Issue a book that will be overdue when we check
            Book book = new Book("1000000016", "Fine Test", "Author", "Cat", 5);
            db.addBook(book);
            db.issueBook("1000000016", testUser, 1);
            // Manually set the due date in the past via the issue record
            List<IssueRecord> records = db.getUserActiveIssueRecords(testUser.getUserId());
            assertFalse(records.isEmpty());
            records.get(0).setDueDate(LocalDate.now().minusDays(5));
            // Fine should be > 0
            assertTrue(db.getTotalFines() > 0.0);
        }

        @Test @Order(145)
        @DisplayName("Concurrent issues from multiple threads don't corrupt quantity")
        void concurrentIssues() throws Exception {
            final int THREADS = 5;
            Book book = new Book("1000000099", "Concurrency Book", "Author", "Cat", THREADS);
            db.addBook(book);
            db.setMaxBorrow(THREADS + 1);

            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            ExecutorService exec = Executors.newFixedThreadPool(THREADS);

            for (int i = 0; i < THREADS; i++) {
                final int idx = i;
                final User u = new User("concurrent" + idx, "pass1234");
                futures.add(exec.submit(() -> {
                    try {
                        latch.await();
                        db.issueBook("1000000099", u, 1);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }));
            }

            latch.countDown();
            long successCount = futures.stream().map(f -> {
                try { return f.get(5, TimeUnit.SECONDS); }
                catch (Exception e) { return false; }
            }).filter(Boolean::booleanValue).count();

            exec.shutdown();

            // All should succeed (qty == threads)
            assertEquals(THREADS, successCount);
            assertEquals(0, db.getBook("1000000099").getQuantity());
        }

        // ─── helpers ───────────────────────────────────────────────
        private void resetBooksDBSingleton() {
            try {
                Field f = BooksDB.class.getDeclaredField("instance");
                f.setAccessible(true);
                f.set(null, null);
            } catch (Exception e) {
                fail("Could not reset BooksDB singleton: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. UsersDB operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9 · UsersDB Operations")
    class UsersDBTests {

        private UsersDB db;

        @BeforeEach
        void setUp() {
            resetUsersDB();
            db = UsersDB.getInstance();
            db.setAutoSave(false);
            // Clear any users loaded from the real data/users_db.ser on disk
            try { db.clearAllUsers("CONFIRM_CLEAR_ALL"); } catch (Exception ignored) {}
        }

        @AfterEach
        void tearDown() { resetUsersDB(); }

        @Test @Order(160)
        @DisplayName("addUser / getUser round-trip")
        void addAndGet() throws UserException {
            db.addUser(validUser("helen01"));
            User u = db.getUser("helen01");
            assertNotNull(u);
            assertEquals("helen01", u.getUserId());
        }

        @Test @Order(161)
        @DisplayName("addUser with duplicate throws UserException")
        void duplicateUser() throws UserException {
            db.addUser(validUser("igor01"));
            assertThrows(UserException.class, () -> db.addUser(validUser("igor01")));
        }

        @Test @Order(162)
        @DisplayName("getUser returns null for unknown user")
        void getUnknown() {
            assertNull(db.getUser("does_not_exist"));
        }

        @Test @Order(163)
        @DisplayName("authenticate returns true for correct credentials")
        void authSuccess() throws UserException {
            db.addUser(new User("jane01", "correct_pass"));
            assertTrue(db.authenticate("jane01", "correct_pass"));
        }

        @Test @Order(164)
        @DisplayName("authenticate returns false for wrong password")
        void authWrongPass() throws UserException {
            db.addUser(new User("jane02", "correct_pass"));
            assertFalse(db.authenticate("jane02", "wrong_pass"));
        }

        @Test @Order(165)
        @DisplayName("authenticate returns false for unknown user")
        void authUnknown() {
            assertFalse(db.authenticate("ghost_user", "any_pass"));
        }

        @Test @Order(166)
        @DisplayName("authenticate returns false for inactive user")
        void authInactiveUser() throws UserException {
            User u = new User("karl01", "pass1234");
            u.setActive(false);
            db.addUser(u);
            assertFalse(db.authenticate("karl01", "pass1234"));
        }

        @Test @Order(167)
        @DisplayName("updateUser persists field changes")
        void updateUser() throws UserException, ValidationException {
            db.addUser(validUser("lisa01"));
            User u = db.getUser("lisa01");
            u.setEmail("lisa@test.com");
            db.updateUser(u);
            assertEquals("lisa@test.com", db.getUser("lisa01").getEmail());
        }

        @Test @Order(168)
        @DisplayName("removeUser removes the user")
        void removeUser() throws UserException {
            // Need a second admin so we can delete the first
            User admin1 = new User("adm001", "pass1234");
            admin1.setRole(UserRole.ADMIN);
            User admin2 = new User("adm002", "pass1234");
            admin2.setRole(UserRole.ADMIN);
            db.addUser(admin1);
            db.addUser(admin2);
            db.removeUser("adm001");
            assertNull(db.getUser("adm001"));
        }

        @Test @Order(169)
        @DisplayName("Cannot remove the last admin")
        void removeLastAdminThrows() throws UserException {
            User admin = new User("sole_admin", "pass1234");
            admin.setRole(UserRole.ADMIN);
            db.addUser(admin);
            assertThrows(UserException.class, () -> db.removeUser("sole_admin"));
        }

        @Test @Order(170)
        @DisplayName("First user added becomes ADMIN automatically")
        void firstUserBecomesAdmin() throws UserException {
            User u = new User("first_user", "pass1234");
            db.addUser(u);
            assertEquals(UserRole.ADMIN, db.getUser("first_user").getRole());
        }

        @Test @Order(171)
        @DisplayName("hasUsers returns false when empty")
        void hasUsersEmpty() {
            assertFalse(db.hasUsers());
        }

        @Test @Order(172)
        @DisplayName("clearAllUsers requires correct confirmation string")
        void clearRequiresConfirmation() throws UserException {
            db.addUser(validUser("temp01"));
            assertThrows(UserException.class, () -> db.clearAllUsers("wrong"));
            db.clearAllUsers("CONFIRM_CLEAR_ALL");
            assertFalse(db.hasUsers());
        }

        // ─── helpers ───────────────────────────────────────────────
        private void resetUsersDB() {
            try {
                Field f = UsersDB.class.getDeclaredField("instance");
                f.setAccessible(true);
                f.set(null, null);
            } catch (Exception e) {
                fail("Could not reset UsersDB singleton: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. UserService
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("10 · UserService")
    class UserServiceTests {

        @BeforeEach
        void resetSingletons() {
            resetBothDBs();
            // Clear disk-loaded users before every test
            try {
                UsersDB udb = UsersDB.getInstance();
                udb.setAutoSave(false);
                udb.clearAllUsers("CONFIRM_CLEAR_ALL");
            } catch (Exception ignored) {}
            resetBothDBs();
        }

        @AfterEach
        void cleanUp() { resetBothDBs(); }

        @Test @Order(180)
        @DisplayName("createUser / getUserById round-trip")
        void createAndGet() throws ValidationException {
            UserService.createUser("mike01", "pass1234");
            User u = UserService.getUserById("mike01");
            assertNotNull(u);
        }

        @Test @Order(181)
        @DisplayName("createUser with username shorter than 3 chars throws ValidationException")
        void shortUsername() {
            assertThrows(ValidationException.class,
                    () -> UserService.createUser("ab", "pass1234"));
        }

        @Test @Order(182)
        @DisplayName("createUser with password shorter than 4 chars throws ValidationException")
        void shortPassword() {
            assertThrows(ValidationException.class,
                    () -> UserService.createUser("nick01", "abc"));
        }

        @Test @Order(183)
        @DisplayName("login returns true for correct credentials")
        void loginSuccess() throws ValidationException {
            UserService.createUser("olivia01", "pass1234");
            assertTrue(UserService.login("olivia01", "pass1234"));
        }

        @Test @Order(184)
        @DisplayName("login returns false for wrong password")
        void loginWrongPass() throws ValidationException {
            UserService.createUser("peter01", "pass1234");
            assertFalse(UserService.login("peter01", "wrong"));
        }

        @Test @Order(185)
        @DisplayName("login throws ValidationException for empty username")
        void loginEmptyUser() {
            assertThrows(ValidationException.class, () -> UserService.login("", "pass"));
        }

        @Test @Order(186)
        @DisplayName("getUserRole returns null for unknown user (no crash)")
        void getRoleUnknown() {
            assertNull(UserService.getUserRole("totally_unknown_xyz"));
        }

        @Test @Order(187)
        @DisplayName("isAdmin returns false for non-admin")
        void isAdminFalse() throws ValidationException {
            // First user created becomes admin; create a second one
            UserService.createUser("admin01", "pass1234"); // becomes admin
            UserService.createUser("user01", "pass1234", UserRole.USER);
            assertFalse(UserService.isAdmin("user01"));
        }

        @Test @Order(188)
        @DisplayName("deleteUser removes the user")
        void deleteUser() throws ValidationException {
            UserService.createUser("quin01", "pass1234"); // admin
            UserService.createUser("rose01", "pass1234", UserRole.USER);
            UserService.deleteUser("rose01");
            assertThrows(UserException.class, () -> UserService.getUserById("rose01"));
        }

        @Test @Order(189)
        @DisplayName("updateUser persists changes")
        void updateUser() throws ValidationException, UserException, ValidationException {
            UserService.createUser("sam001", "pass1234");
            User u = UserService.getUserById("sam001");
            u.setFirstName("Sam");
            UserService.updateUser(u);
            assertEquals("Sam", UserService.getUserById("sam001").getFirstName());
        }

        @Test @Order(190)
        @DisplayName("getAllUsers returns list with expected size")
        void getAllUsers() throws ValidationException {
            UserService.createUser("tom001", "pass1234");
            UserService.createUser("uma001", "pass1234", UserRole.USER);
            List<User> users = UserService.getAllUsers();
            assertEquals(2, users.size());
        }

        @Test @Order(191)
        @DisplayName("getUserCount returns correct count")
        void userCount() throws ValidationException {
            UserService.createUser("vic001", "pass1234");
            assertEquals(1, UserService.getUserCount());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 11. OverdueReportFormatter
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("11 · OverdueReportFormatter")
    class OverdueReportFormatterTests {

        private IssueRecord overdueRecord(String isbn, String title, int daysOverdue) {
            IssueRecord r = new IssueRecord(isbn, title, "user1",
                    LocalDate.now().minusDays(14 + daysOverdue), 1, 14, 2.0);
            return r;
        }

        @Test @Order(200)
        @DisplayName("format on empty list produces header with zero fines")
        void emptyList() {
            String report = OverdueReportFormatter.format(List.of());
            assertTrue(report.contains("Overdue Books Report"));
            assertTrue(report.contains("$0.00"));
        }

        @Test @Order(201)
        @DisplayName("format includes each book's title")
        void containsBookTitle() {
            IssueRecord r = overdueRecord("ISBN001", "Clean Code", 5);
            String report = OverdueReportFormatter.format(List.of(r));
            assertTrue(report.contains("Clean Code"));
        }

        @Test @Order(202)
        @DisplayName("format includes correct total fines")
        void totalFines() {
            // 5 days overdue × $2.00 = $10.00; 10 days × $2.00 = $20.00 → total $30.00
            IssueRecord r1 = overdueRecord("ISBN001", "Book A", 5);
            IssueRecord r2 = overdueRecord("ISBN002", "Book B", 10);
            String report = OverdueReportFormatter.format(List.of(r1, r2));
            assertTrue(report.contains("$30.00"));
        }

        @Test @Order(203)
        @DisplayName("format throws NullPointerException on null list")
        void nullListThrows() {
            assertThrows(NullPointerException.class,
                    () -> OverdueReportFormatter.format(null));
        }

        @Test @Order(204)
        @DisplayName("format uses platform line separator")
        void platformLineSeparator() {
            IssueRecord r = overdueRecord("ISBN001", "Book A", 3);
            String report = OverdueReportFormatter.format(List.of(r));
            assertTrue(report.contains(System.lineSeparator()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 12. BorrowRequest service limits
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("12 · BorrowRequest State Machine (entity-level)")
    class BorrowRequestStateMachineTests {

        @ParameterizedTest
        @MethodSource("illegalTransitions")
        @DisplayName("Illegal state transitions throw IllegalStateException")
        void illegalTransitions(BorrowRequest.Status initialStatus, String action) {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            if (initialStatus == BorrowRequest.Status.APPROVED) {
                r.approve("admin");
            } else if (initialStatus == BorrowRequest.Status.REJECTED) {
                r.reject("admin", "reason");
            }
            assertThrows(IllegalStateException.class, () -> {
                if ("approve".equals(action)) r.approve("admin2");
                else r.reject("admin2", "new reason");
            });
        }

        static Stream<Arguments> illegalTransitions() {
            return Stream.of(
                    Arguments.of(BorrowRequest.Status.APPROVED, "approve"),
                    Arguments.of(BorrowRequest.Status.APPROVED, "reject"),
                    Arguments.of(BorrowRequest.Status.REJECTED, "approve"),
                    Arguments.of(BorrowRequest.Status.REJECTED, "reject")
            );
        }

        @Test @Order(220)
        @DisplayName("UUID requestId is unique for each request")
        void uniqueRequestIds() {
            BorrowRequest r1 = new BorrowRequest("ISBN001", "Book", "user1", 1);
            BorrowRequest r2 = new BorrowRequest("ISBN001", "Book", "user1", 1);
            assertNotEquals(r1.getRequestId(), r2.getRequestId());
        }

        @Test @Order(221)
        @DisplayName("requestedAt timestamp is captured at construction")
        void requestedAtSet() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertTrue(r.getRequestedAt().isAfter(before));
            assertTrue(r.getRequestedAt().isBefore(after));
        }

        @Test @Order(222)
        @DisplayName("processedAt is null before processing")
        void processedAtNull() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            assertNull(r.getProcessedAt());
        }

        @Test @Order(223)
        @DisplayName("processedAt is set after approve")
        void processedAtAfterApprove() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            r.approve("admin");
            assertNotNull(r.getProcessedAt());
        }

        @Test @Order(224)
        @DisplayName("Approved request has null note")
        void approvedNoteIsNull() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            r.approve("admin");
            assertNull(r.getNote());
        }

        @Test @Order(225)
        @DisplayName("Blank rejection note defaults to 'Rejected by staff'")
        void blankNoteDefaulted() {
            BorrowRequest r = new BorrowRequest("ISBN001", "Book", "user1", 1);
            r.reject("admin", "   ");
            assertEquals("Rejected by staff", r.getNote());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 13. AppConfiguration persistence (integration)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("13 · AppConfiguration Serialization")
    class AppConfigurationSerializationTests {

        @Test @Order(230)
        @DisplayName("AppConfiguration round-trips through DataStorage")
        void configRoundTrip() throws IOException, ClassNotFoundException {
            String path = tempDir.resolve("test_config.ser").toString();
            AppConfiguration original = new AppConfiguration();
            original.setSmtpHost("smtp.test.com");
            original.setSmtpPort(465);
            original.setExportDirectory("test_exports");
            original.setSmtpAuth(false);

            DataStorage.writeSerialized(path, original);

            AppConfiguration loaded = DataStorage.readSerialized(path, AppConfiguration.class);
            assertNotNull(loaded);
            assertEquals("smtp.test.com", loaded.getSmtpHost());
            assertEquals(465, loaded.getSmtpPort());
            assertEquals("test_exports", loaded.getExportDirectory());
            assertFalse(loaded.isSmtpAuth());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 14. Book serialization round-trip
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("14 · Book Serialization")
    class BookSerializationTests {

        @Test @Order(240)
        @DisplayName("Book round-trips through DataStorage")
        void bookRoundTrip() throws IOException, ClassNotFoundException {
            String path = tempDir.resolve("test_book.ser").toString();
            Book original = new Book("9780201633610", "Design Patterns",
                    "GoF", "Technology", 7,
                    "Addison-Wesley", "Classic patterns book", 49.99, "Shelf-A3");
            DataStorage.writeSerialized(path, original);

            Book loaded = DataStorage.readSerialized(path, Book.class);
            assertNotNull(loaded);
            assertEquals("9780201633610", loaded.getIsbn());
            assertEquals("Design Patterns", loaded.getTitle());
            assertEquals(7, loaded.getQuantity());
            assertEquals(49.99, loaded.getPrice(), 0.001);
            assertEquals("Shelf-A3", loaded.getLocation());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED RESET HELPERS (used across nested classes)
    // ═══════════════════════════════════════════════════════════════

    static void resetBothDBs() {
        try {
            Field fb = BooksDB.class.getDeclaredField("instance");
            fb.setAccessible(true);
            fb.set(null, null);
        } catch (Exception ignored) {}
        try {
            Field fu = UsersDB.class.getDeclaredField("instance");
            fu.setAccessible(true);
            fu.set(null, null);
        } catch (Exception ignored) {}
    }
}
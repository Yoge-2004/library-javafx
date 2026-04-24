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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Library OS Test Suite
 *
 * Isolation strategy:
 * - Entity tests (Book, User, BorrowRequest, IssueRecord, AppConfiguration):
 *   Pure unit tests, no singletons, no disk I/O.
 * - BooksDB/UsersDB tests: reset singleton via reflection + clear data.
 * - UserService/BookService tests: work with counts relative to baseline
 *   because static final fields load real DB at class init time.
 * - DataStorage tests: use a temp directory, never touch data/.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LibraryTestSuite {

    static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        tempDir = Files.createTempDirectory("lib-test-");
        Files.createDirectories(Paths.get("data"));
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (tempDir != null) {
            try (var w = Files.walk(tempDir)) {
                w.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    // ── Shared helpers ────────────────────────────────────────────

    static Book book(String isbn) {
        return new Book(isbn, "Title " + isbn, "Author", "Technology", 5);
    }
    static User user(String id) throws UserException {
        return new User(id, "pass1234");
    }

    static void resetBooksDB() {
        try {
            Field f = BooksDB.class.getDeclaredField("instance");
            f.setAccessible(true); f.set(null, null);
        } catch (Exception ignored) {}
    }
    static void resetUsersDB() {
        try {
            Field f = UsersDB.class.getDeclaredField("instance");
            f.setAccessible(true); f.set(null, null);
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Book Entity — pure unit tests, no singletons
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("1 · Book Entity")
    class BookEntityTests {
        @Test @DisplayName("Valid construction")
        void valid() {
            Book b = new Book("9780134685991", "Effective Java", "Joshua Bloch", "Technology", 3);
            assertEquals("9780134685991", b.getIsbn());
            assertEquals(3, b.getQuantity());
            assertTrue(b.isActive());
        }
        @Test @DisplayName("Null ISBN throws")
        void nullIsbn() { assertThrows(IllegalArgumentException.class, () -> new Book(null,"T","A","C",1)); }
        @Test @DisplayName("Negative quantity throws")
        void negQty() { assertThrows(IllegalArgumentException.class, () -> new Book("1234567890","T","A","C",-1)); }
        @Test @DisplayName("Zero quantity allowed")
        void zeroQty() { assertEquals(0, new Book("1234567890","T","A","C",0).getQuantity()); }
        @Test @DisplayName("addCopies increments")
        void addCopies() { Book b = book("1111111111"); b.addCopies(3); assertEquals(8, b.getQuantity()); }
        @Test @DisplayName("equals by ISBN")
        void eq() { assertEquals(book("9780134685991"), new Book("9780134685991","Other","Auth","Cat",1)); }
        @Test @DisplayName("isValidIsbn")
        void isbn() { assertTrue(Book.isValidIsbn("9780134685991")); assertFalse(Book.isValidIsbn(null)); }
        @Test @DisplayName("getAvailabilityStatus")
        void avail() {
            Book b = new Book("1234567890","T","A","C",0);
            assertEquals("Out of Stock", b.getAvailabilityStatus());
            b.addCopies(10);
            assertTrue(b.getAvailabilityStatus().startsWith("Available"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. User Entity — pure unit tests
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("2 · User Entity")
    class UserEntityTests {
        @Test @DisplayName("Valid construction")
        void valid() throws UserException {
            User u = new User("john_doe","secret123");
            assertEquals("john_doe", u.getUserId());
            assertEquals(UserRole.USER, u.getRole());
        }
        @Test @DisplayName("Short userId throws")
        void shortId() { assertThrows(UserException.class, () -> new User("ab","pass1234")); }
        @Test @DisplayName("Short password throws")
        void shortPass() { assertThrows(UserException.class, () -> new User("alice","abc")); }
        @Test @DisplayName("Valid email accepted")
        void email() throws Exception {
            User u = new User("alice01","pass1234");
            u.setEmail("alice@example.com");
            assertEquals("alice@example.com", u.getEmail());
        }
        @Test @DisplayName("Invalid email throws")
        void badEmail() throws UserException {
            assertThrows(Exception.class, () -> new User("alice01","pass1234").setEmail("bad"));
        }
        @Test @DisplayName("Null role defaults to USER")
        void nullRole() throws UserException { User u = new User("b01","pass1234"); u.setRole(null); assertEquals(UserRole.USER, u.getRole()); }
        @Test @DisplayName("Role checks")
        void roles() throws UserException {
            User u = new User("e01","pass1234");
            u.setRole(UserRole.ADMIN); assertTrue(u.isAdmin()); assertTrue(u.isStaff());
            u.setRole(UserRole.USER); assertFalse(u.isAdmin()); assertFalse(u.isStaff());
        }
        @Test @DisplayName("getFullName fallback")
        void fullName() throws UserException {
            User u = new User("charlie","pass1234");
            assertEquals("charlie", u.getFullName());
            u.setFirstName("Charlie"); u.setLastName("B");
            assertEquals("Charlie B", u.getFullName());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. BorrowRequest — pure unit tests
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("3 · BorrowRequest")
    class BorrowRequestTests {
        @Test @DisplayName("New is PENDING") void pending() { assertTrue(new BorrowRequest("X","B","u",1).isPending()); }
        @Test @DisplayName("approve transitions") void approve() { BorrowRequest r = new BorrowRequest("X","B","u",1); r.approve("a"); assertEquals(BorrowRequest.Status.APPROVED, r.getStatus()); }
        @Test @DisplayName("reject transitions") void reject() { BorrowRequest r = new BorrowRequest("X","B","u",1); r.reject("a","reason"); assertEquals(BorrowRequest.Status.REJECTED, r.getStatus()); assertEquals("reason", r.getNote()); }
        @Test @DisplayName("Double approve throws") void dblApprove() { BorrowRequest r = new BorrowRequest("X","B","u",1); r.approve("a"); assertThrows(IllegalStateException.class, () -> r.approve("b")); }
        @Test @DisplayName("Note > 1000 truncated") void truncNote() { BorrowRequest r = new BorrowRequest("X","B","u",1); r.reject("a","x".repeat(2000)); assertEquals(1000, r.getNote().length()); }
        @Test @DisplayName("Qty clamped to 1") void qtyClamp() { assertEquals(1, new BorrowRequest("X","B","u",-5).getQuantity()); }

        @ParameterizedTest @MethodSource("illegalTransitions")
        @DisplayName("Illegal transitions throw")
        void illegal(BorrowRequest.Status init, String action) {
            BorrowRequest r = new BorrowRequest("X","B","u",1);
            if (init == BorrowRequest.Status.APPROVED) r.approve("a"); else r.reject("a","r");
            assertThrows(IllegalStateException.class, () -> { if ("approve".equals(action)) r.approve("b"); else r.reject("b","r"); });
        }
        static Stream<Arguments> illegalTransitions() {
            return Stream.of(
                    Arguments.of(BorrowRequest.Status.APPROVED,"approve"),
                    Arguments.of(BorrowRequest.Status.APPROVED,"reject"),
                    Arguments.of(BorrowRequest.Status.REJECTED,"approve"),
                    Arguments.of(BorrowRequest.Status.REJECTED,"reject")
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. BooksDB.IssueRecord — pure unit tests
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("4 · IssueRecord")
    class IssueRecordTests {
        IssueRecord active() { return new IssueRecord("X","Book","u", LocalDate.now(),1,14,2.0); }

        @Test @DisplayName("Not overdue when new") void notOverdue() { assertFalse(active().isOverdue()); assertEquals(0.0, active().calculateFine()); }
        @Test @DisplayName("Overdue after due date") void overdue() {
            IssueRecord r = new IssueRecord("X","B","u", LocalDate.now().minusDays(20),1,14,2.0);
            assertTrue(r.isOverdue()); assertEquals(6, r.getDaysOverdue()); assertEquals(12.0, r.calculateFine(), 0.001);
        }
        @Test @DisplayName("setReturned") void returned() { IssueRecord r = active(); r.setReturned(true); assertTrue(r.isReturned()); assertNotNull(r.getReturnDate()); }
        @Test @DisplayName("renew extends due") void renew() { IssueRecord r = active(); LocalDate d = r.getDueDate(); assertTrue(r.renew(14)); assertEquals(d.plusDays(14), r.getDueDate()); }
        @Test @DisplayName("canRenew false after max") void noRenew() { IssueRecord r = active(); for (int i=0;i<BooksDB.MAX_RENEWAL_COUNT;i++) r.renew(7); assertFalse(r.canRenew()); }
        @Test @DisplayName("statusStyleClass chip-error when overdue") void chipError() { IssueRecord r = new IssueRecord("X","B","u", LocalDate.now().minusDays(20),1,14,2.0); assertEquals("chip-error", r.getStatusStyleClass()); }
        @Test @DisplayName("statusStyleClass chip-success when returned") void chipOk() { IssueRecord r = active(); r.setReturned(true); assertEquals("chip-success", r.getStatusStyleClass()); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. AppConfiguration — pure unit tests
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("5 · AppConfiguration")
    class ConfigTests {
        @Test @DisplayName("Default export dir") void def() { assertEquals("exports", new AppConfiguration().getExportDirectory()); }
        @Test @DisplayName("Blank export falls back") void blank() { AppConfiguration c = new AppConfiguration(); c.setExportDirectory("  "); assertEquals("exports", c.getExportDirectory()); }
        @Test @DisplayName("Email not configured by default") void noEmail() { assertFalse(new AppConfiguration().isEmailConfigured()); }
        @Test @DisplayName("Email configured without auth") void emailNoAuth() { AppConfiguration c = new AppConfiguration(); c.setSmtpHost("s"); c.setFromAddress("a@b.c"); c.setSmtpAuth(false); assertTrue(c.isEmailConfigured()); }
        @Test @DisplayName("Port clamped to 1") void portClamp() { AppConfiguration c = new AppConfiguration(); c.setSmtpPort(-1); assertEquals(1, c.getSmtpPort()); }
        @Test @DisplayName("formatAmount") void format() { AppConfiguration c = new AppConfiguration(); c.setCurrencySymbol("Rs."); assertEquals("Rs.10.00", c.formatAmount(10.0)); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. UserRole
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("6 · UserRole")
    class RoleTests {
        @Test void adminIsAdmin() { assertTrue(UserRole.ADMIN.isAdmin()); assertFalse(UserRole.USER.isAdmin()); }
        @Test void staffCheck()  { assertTrue(UserRole.LIBRARIAN.isStaff()); assertFalse(UserRole.USER.isStaff()); }
        @Test void names()       { assertEquals("User", UserRole.USER.getDisplayName()); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. DataStorage — uses tempDir, never touches data/
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("7 · DataStorage")
    class StorageTests {
        String p(String n) { return tempDir.resolve(n).toString(); }

        @Test @DisplayName("Write + read") void writeRead() throws Exception {
            DataStorage.writeSerialized(p("basic.ser"), List.of("a","b"));
            @SuppressWarnings("unchecked") List<String> r = DataStorage.readSerialized(p("basic.ser"), List.class);
            assertEquals(List.of("a","b"), r);
        }
        @Test @DisplayName("Missing file = null") void missing() throws Exception { assertNull(DataStorage.readSerialized(p("no.ser"), String.class)); }
        @Test @DisplayName("fileExists") void exists() throws Exception { String f = p("ex.ser"); assertFalse(DataStorage.fileExists(f)); DataStorage.writeSerialized(f,"x"); assertTrue(DataStorage.fileExists(f)); }
        @Test @DisplayName("deleteFile") void del() throws Exception { String f = p("d.ser"); DataStorage.writeSerialized(f,"bye"); assertTrue(DataStorage.deleteFile(f)); assertFalse(DataStorage.fileExists(f)); }
        @Test @DisplayName("Overwrite") void ow() throws Exception { String f = p("ow.ser"); DataStorage.writeSerialized(f,"first"); DataStorage.writeSerialized(f,"second"); assertEquals("second", DataStorage.readSerialized(f, String.class)); }
        @Test @DisplayName("Map round-trip") void map() throws Exception { String f = p("m.ser"); Map<String,List<String>> m = Map.of("u", List.of("a")); DataStorage.writeSerializedMap(f, m); assertEquals(m, DataStorage.readSerializedMap(f)); }
        @Test @DisplayName("Null filename throws") void nullFile() { assertThrows(IllegalArgumentException.class, () -> DataStorage.writeSerialized(null,"x")); }
        @Test @DisplayName("Concurrent writes safe") void concurrent() throws Exception {
            String f = p("c.ser");
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> fs = new ArrayList<>();
            ExecutorService ex = Executors.newFixedThreadPool(8);
            for (int i=0;i<8;i++) { final int idx=i; fs.add(ex.submit(() -> { go.await(); DataStorage.writeSerialized(f,"t"+idx); return null; })); }
            go.countDown();
            for (Future<?> fut : fs) fut.get(5, TimeUnit.SECONDS);
            ex.shutdown();
            assertNotNull(DataStorage.readSerialized(f, String.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. BooksDB — reset singleton, clear disk books
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("8 · BooksDB")
    class BooksDBTests {
        BooksDB db;
        User testUser;

        @BeforeEach void setUp() throws UserException {
            resetBooksDB();
            db = BooksDB.getInstance();
            db.setAutoSave(false);
            // Clear any pre-existing books from disk
            for (Book b : new ArrayList<>(db.getBooks())) {
                try { db.removeBook(b.getIsbn()); } catch (Exception ignored) {}
            }
            testUser = new User("tuser01","pass1234");
        }
        @AfterEach void tearDown() { resetBooksDB(); }

        @Test @DisplayName("addBook / getBook") void add() throws BooksException { db.addBook(book("9780000000001")); assertNotNull(db.getBook("9780000000001")); }
        @Test @DisplayName("Duplicate ISBN throws") void dup() throws BooksException { db.addBook(book("9780000000002")); assertThrows(BooksException.class, () -> db.addBook(book("9780000000002"))); }
        @Test @DisplayName("modifyBook") void mod() throws BooksException { Book b = book("9780000000003"); db.addBook(b); b.setTitle("New"); db.modifyBook(b); assertEquals("New", db.getBook("9780000000003").getTitle()); }
        @Test @DisplayName("removeBook") void rem() throws BooksException { db.addBook(book("9780000000004")); db.removeBook("9780000000004"); assertNull(db.getBook("9780000000004")); }
        @Test @DisplayName("issueBook decrements qty") void issue() throws BooksException { Book b = book("9780000000005"); db.addBook(b); db.issueBook("9780000000005",testUser,2); assertEquals(3, db.getBook("9780000000005").getQuantity()); }
        @Test @DisplayName("issueBook insufficient throws") void issueErr() throws BooksException { db.addBook(new Book("9780000000006","T","A","C",1)); assertThrows(BooksException.class, () -> db.issueBook("9780000000006",testUser,2)); }
        @Test @DisplayName("returnBook restores qty") void ret() throws BooksException { Book b = book("9780000000007"); db.addBook(b); db.issueBook("9780000000007",testUser,2); db.returnBook("9780000000007",testUser,2); assertEquals(5, db.getBook("9780000000007").getQuantity()); }
        @Test @DisplayName("searchBooks empty = all") void srchAll() throws BooksException {
            int baseline = db.getBooks().size();
            db.addBook(book("9780000000008"));
            db.addBook(book("9780000000009"));
            assertEquals(baseline + 2, db.searchBooks("").size());
        }
        @Test @DisplayName("searchBooks filters by title") void srchTitle() throws BooksException {
            int baseline = db.searchBooks("java").size();
            db.addBook(new Book("9780000000010","Java Guide","A","T",3));
            db.addBook(new Book("9780000000011","Python Tips","A","T",3));
            assertEquals(baseline + 1, db.searchBooks("java").size());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. UsersDB — reset singleton, clear all users
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("9 · UsersDB")
    class UsersDBTests {
        UsersDB db;

        @BeforeEach void setUp() {
            resetUsersDB();
            db = UsersDB.getInstance();
            db.setAutoSave(false);
            try { db.clearAllUsers("CONFIRM_CLEAR_ALL"); } catch (Exception ignored) {}
        }
        @AfterEach void tearDown() { resetUsersDB(); }

        @Test @DisplayName("addUser / getUser") void add() throws UserException { db.addUser(user("u01")); assertNotNull(db.getUser("u01")); }
        @Test @DisplayName("Duplicate throws") void dup() throws UserException { db.addUser(user("u02")); assertThrows(UserException.class, () -> db.addUser(user("u02"))); }
        @Test @DisplayName("auth correct") void authOk() throws UserException { db.addUser(new User("u03","secret")); assertTrue(db.authenticate("u03","secret")); }
        @Test @DisplayName("auth wrong pass") void authFail() throws UserException { db.addUser(new User("u04","secret")); assertFalse(db.authenticate("u04","wrong")); }
        @Test @DisplayName("auth inactive") void authInactive() throws UserException { User u = new User("u05","p1234"); u.setActive(false); db.addUser(u); assertFalse(db.authenticate("u05","p1234")); }
        @Test @DisplayName("updateUser") void upd() throws UserException, ValidationException { db.addUser(user("u06")); User u = db.getUser("u06"); u.setEmail("a@b.co"); db.updateUser(u); assertEquals("a@b.co", db.getUser("u06").getEmail()); }
        @Test @DisplayName("removeUser") void rem() throws UserException { User a1=new User("adm01","pass"); a1.setRole(UserRole.ADMIN); User a2=new User("adm02","pass"); a2.setRole(UserRole.ADMIN); db.addUser(a1); db.addUser(a2); db.removeUser("adm01"); assertNull(db.getUser("adm01")); }
        @Test @DisplayName("Cannot remove last admin") void lastAdmin() throws UserException { User a=new User("solo1","pass"); a.setRole(UserRole.ADMIN); db.addUser(a); assertThrows(UserException.class, () -> db.removeUser("solo1")); }
        @Test @DisplayName("hasUsers false when empty") void empty() { assertFalse(db.hasUsers()); }
        @Test @DisplayName("First user becomes admin") void firstAdmin() throws UserException { db.addUser(user("first")); assertEquals(UserRole.ADMIN, db.getUser("first").getRole()); }
        @Test @DisplayName("clearAllUsers confirmation") void clear() throws UserException { db.addUser(user("t01")); assertThrows(UserException.class, () -> db.clearAllUsers("wrong")); db.clearAllUsers("CONFIRM_CLEAR_ALL"); assertFalse(db.hasUsers()); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. UserService — count relative to baseline (avoids disk issue)
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("10 · UserService")
    class UserServiceTests {
        int baseline;

        @BeforeEach void setUp() { baseline = UserService.getUserCount(); }

        @Test @DisplayName("createUser adds 1 user") void create() throws ValidationException {
            String uid = "svc_" + System.nanoTime();
            UserService.createUser(uid, "pass1234");
            assertEquals(baseline + 1, UserService.getUserCount());
        }
        @Test @DisplayName("Short username throws") void shortName() { assertThrows(ValidationException.class, () -> UserService.createUser("ab","pass1234")); }
        @Test @DisplayName("login correct") void login() throws ValidationException {
            String uid = "svc2_" + System.nanoTime();
            UserService.createUser(uid, "pass1234");
            assertTrue(UserService.login(uid, "pass1234"));
        }
        @Test @DisplayName("login wrong pass") void loginFail() throws ValidationException {
            String uid = "svc3_" + System.nanoTime();
            UserService.createUser(uid, "pass1234");
            assertFalse(UserService.login(uid, "wrongpass"));
        }
        @Test @DisplayName("getUserRole null for unknown") void roleUnknown() { assertNull(UserService.getUserRole("completely_unknown_user_xyz_abc")); }
        @Test @DisplayName("updateUser persists changes") void update() throws ValidationException, UserException {
            String uid = "svc4_" + System.nanoTime();
            UserService.createUser(uid, "pass1234");
            User u = UserService.getUserById(uid);
            u.setFirstName("TestName");
            UserService.updateUser(u);
            assertEquals("TestName", UserService.getUserById(uid).getFirstName());
        }
        @Test @DisplayName("userExists returns true after create") void exists() throws ValidationException {
            String uid = "svc5_" + System.nanoTime();
            assertFalse(UserService.userExists(uid));
            UserService.createUser(uid, "pass1234");
            assertTrue(UserService.userExists(uid));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 11. OverdueReportFormatter
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("11 · OverdueReportFormatter")
    class FormatterTests {
        IssueRecord overdue(String isbn, int daysOver) {
            return new IssueRecord(isbn, "Book"+isbn, "user", LocalDate.now().minusDays(14+daysOver),1,14,2.0);
        }
        @Test void emptyList() {
            assertTrue(OverdueReportFormatter.format(List.of())
                    .contains(AppConfigurationService.getConfiguration().formatAmount(0.0)));
        }
        @Test void containsTitle() { assertTrue(OverdueReportFormatter.format(List.of(overdue("X",5))).contains("BookX")); }
        @Test void totalFines() {
            String r = OverdueReportFormatter.format(List.of(overdue("A",5), overdue("B",10)));
            assertTrue(r.contains(AppConfigurationService.getConfiguration().formatAmount(30.0)));
        }
        @Test void nullThrows() { assertThrows(NullPointerException.class, () -> OverdueReportFormatter.format(null)); }
    }

    // ═══════════════════════════════════════════════════════════════
    // 12. Serialization round-trips
    // ═══════════════════════════════════════════════════════════════
    @Nested @DisplayName("12 · Serialization")
    class SerializationTests {
        @Test @DisplayName("Book round-trip") void book() throws Exception {
            String p = tempDir.resolve("book.ser").toString();
            Book b = new Book("9780201633610","Design Patterns","GoF","Tech",7);
            DataStorage.writeSerialized(p, b);
            Book loaded = DataStorage.readSerialized(p, Book.class);
            assertNotNull(loaded); assertEquals("9780201633610", loaded.getIsbn()); assertEquals(7, loaded.getQuantity());
        }
        @Test @DisplayName("AppConfiguration round-trip") void config() throws Exception {
            String p = tempDir.resolve("cfg.ser").toString();
            AppConfiguration c = new AppConfiguration(); c.setSmtpHost("smtp.test.com"); c.setSmtpPort(465);
            DataStorage.writeSerialized(p, c);
            AppConfiguration loaded = DataStorage.readSerialized(p, AppConfiguration.class);
            assertEquals("smtp.test.com", loaded.getSmtpHost()); assertEquals(465, loaded.getSmtpPort());
        }
    }
}

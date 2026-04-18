package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.entities.Book;
import com.example.entities.BorrowRequest;
import com.example.entities.BooksDB;
import com.example.entities.BooksDB.IssueRecord;
import com.example.entities.User;
import com.example.services.BookService;
import com.example.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Circulation view — issues, returns, renewals, and borrow-request approval.
 *
 * Fixes applied vs. original:
 *  - IssueRecord uses BooksDB.IssueRecord (inner class) — no import mismatch.
 *  - CONSTRAINED_RESIZE_POLICY replaced with CONSTRAINED_RESIZE_POLICY.
 *  - Issue Book dialog rebuilt: searchable book picker + searchable user picker +
 *    live availability feedback, not a raw "ISBN,UserID" text box.
 *  - Role-based columns: staff sees all users; regular user sees own records only.
 */
public class CirculationView extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    private final ObservableList<IssueRecord> issueRecords;
    private final ObservableList<BorrowRequest> borrowRequests;
    private final boolean isStaff;
    private final String currentUser;
    private final Runnable onRefresh;
    private final ToastDisplay toast;

    private TableView<IssueRecord>  issuesTable;
    private TableView<BorrowRequest> requestsTable;

    public CirculationView(ObservableList<IssueRecord> issueRecords,
                           ObservableList<BorrowRequest> borrowRequests,
                           boolean isStaff, String currentUser,
                           Runnable onRefresh, ToastDisplay toast) {
        this.issueRecords  = issueRecords;
        this.borrowRequests = borrowRequests;
        this.isStaff       = isStaff;
        this.currentUser   = currentUser;
        this.onRefresh     = onRefresh;
        this.toast         = toast;
        initUI();
        bind();
    }

    // ═══════════════════════════════════════════════════════════════
    // Init
    // ═══════════════════════════════════════════════════════════════

    private void initUI() {
        setStyle("-fx-background-color: #F1F5F9;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:#F1F5F9;");

        content.getChildren().addAll(buildHeader(), buildTabs());
        scroll.setContent(content);
        setCenter(scroll);
    }

    private VBox buildHeader() {
        Label title = new Label("Circulation");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage book issues, returns, renewals and borrow requests");
        sub.getStyleClass().add("page-subtitle");
        VBox h = new VBox(4, title, sub);
        return h;
    }

    private TabPane buildTabs() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.setStyle("-fx-background-color:transparent;");

        tp.getTabs().add(new Tab("📚  Active Issues", issuesPanel()));
        tp.getTabs().add(new Tab("📝  Borrow Requests", requestsPanel()));
        if (isStaff) tp.getTabs().add(new Tab("⚠  Overdue", overduePanel()));

        return tp;
    }

    // ═══════════════════════════════════════════════════════════════
    // Issues panel
    // ═══════════════════════════════════════════════════════════════

    private VBox issuesPanel() {
        VBox p = new VBox(12);
        p.setFillWidth(true);

        if (isStaff) {
            Button issueBtn = AppTheme.createIconTextButton(
                    "Issue Book", AppTheme.ICON_ADD, AppTheme.ButtonStyle.PRIMARY);
            issueBtn.setOnAction(e -> showIssueDialog());

            HBox bar = new HBox(issueBtn);
            bar.setAlignment(Pos.CENTER_LEFT);
            p.getChildren().add(bar);
        }

        issuesTable = buildIssuesTable();
        VBox.setVgrow(issuesTable, Priority.ALWAYS);
        p.getChildren().add(issuesTable);
        return p;
    }

    private TableView<IssueRecord> buildIssuesTable() {
        TableView<IssueRecord> t = new TableView<>();
        t.getStyleClass().add("table-view");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No active issues"));

        TableColumn<IssueRecord, String> titleC = col("Book Title",
                r -> r.getBookTitle(), 200);
        TableColumn<IssueRecord, String> userC  = col("Borrower",
                r -> r.getUserId(), 110);
        TableColumn<IssueRecord, String> issueC = col("Issued",
                r -> r.getIssueDate().format(DATE_FMT), 100);
        TableColumn<IssueRecord, String> dueC   = col("Due Date",
                r -> r.getDueDate().format(DATE_FMT), 100);
        TableColumn<IssueRecord, String> qtyC   = col("Qty",
                r -> String.valueOf(r.getQuantity()), 50);

        // Status chip
        TableColumn<IssueRecord, Void> statusC = new TableColumn<>("Status");
        statusC.setPrefWidth(110);
        statusC.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                { setGraphic(null); return; }
                IssueRecord r = getTableRow().getItem();
                Label chip = new Label(r.getStatusText());
                chip.getStyleClass().addAll("chip", r.getStatusStyleClass());
                setGraphic(chip);
            }
        });

        // Actions
        TableColumn<IssueRecord, Void> actC = new TableColumn<>("Actions");
        actC.setPrefWidth(160);
        actC.setCellFactory(c -> new TableCell<>() {
            final Button retBtn  = actionBtn("Return", "#0D9488");
            final Button renBtn  = actionBtn("Renew",  "#3B82F6");
            {
                retBtn.setOnAction(e -> returnBook(getRow()));
                renBtn.setOnAction(e -> renewBook(getRow()));
            }
            private IssueRecord getRow() {
                return getTableView().getItems().get(getIndex());
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                { setGraphic(null); return; }
                IssueRecord r = getTableRow().getItem();
                HBox box = new HBox(6);
                if (isStaff || currentUser.equals(r.getUserId()))
                    box.getChildren().add(retBtn);
                if (r.canRenew()) box.getChildren().add(renBtn);
                setGraphic(box);
            }
        });

        if (isStaff) {
            t.getColumns().addAll(titleC, userC, issueC, dueC, qtyC, statusC, actC);
        } else {
            t.getColumns().addAll(titleC, issueC, dueC, qtyC, statusC, actC);
        }
        return t;
    }

    // ═══════════════════════════════════════════════════════════════
    // Requests panel
    // ═══════════════════════════════════════════════════════════════

    private VBox requestsPanel() {
        VBox p = new VBox(12);
        p.setFillWidth(true);

        requestsTable = buildRequestsTable();
        VBox.setVgrow(requestsTable, Priority.ALWAYS);
        p.getChildren().add(requestsTable);
        return p;
    }

    private TableView<BorrowRequest> buildRequestsTable() {
        TableView<BorrowRequest> t = new TableView<>();
        t.getStyleClass().add("table-view");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No borrow requests"));

        TableColumn<BorrowRequest, String> titleC = col2("Book Title",
                r -> r.getBookTitle(), 240);
        TableColumn<BorrowRequest, String> userC  = col2("Requested By",
                r -> r.getUserId(), 110);
        TableColumn<BorrowRequest, String> qtyC   = col2("Qty",
                r -> String.valueOf(r.getQuantity()), 50);
        TableColumn<BorrowRequest, String> dateC  = col2("Requested",
                r -> r.getRequestedAt().format(DATETIME_FMT), 110);

        // Status chip
        TableColumn<BorrowRequest, Void> statusC = new TableColumn<>("Status");
        statusC.setPrefWidth(100);
        statusC.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                { setGraphic(null); return; }
                BorrowRequest req = getTableRow().getItem();
                Label chip = new Label(req.getStatus().name());
                String sc = switch (req.getStatus()) {
                    case PENDING  -> "chip-warning";
                    case APPROVED -> "chip-success";
                    case REJECTED -> "chip-error";
                };
                chip.getStyleClass().addAll("chip", sc);
                setGraphic(chip);
            }
        });

        // Note/rejection reason column - visible to users so they know why rejected
        TableColumn<BorrowRequest, String> noteC = makeCol("Reason",
                r -> r.getNote() != null ? r.getNote() : "", 160);
        noteC.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || s.isEmpty()) { setText(null); setGraphic(null); return; }
                Label l = new Label(s);
                l.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12px;");
                l.setWrapText(true);
                setGraphic(l); setText(null);
            }
        });

        t.getColumns().addAll(titleC, userC, qtyC, dateC, statusC, noteC);

        if (isStaff) {
            TableColumn<BorrowRequest, Void> actC = new TableColumn<>("Actions");
            actC.setPrefWidth(110);
            actC.setCellFactory(c -> new TableCell<>() {
                final Button appr = actionBtn("✓", "#16A34A");
                final Button rej  = actionBtn("✕", "#DC2626");
                {
                    appr.setOnAction(e -> approveRequest(getTableView().getItems().get(getIndex())));
                    rej .setOnAction(e -> rejectRequest (getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null)
                    { setGraphic(null); return; }
                    BorrowRequest req = getTableRow().getItem();
                    setGraphic(req.isPending() ? new HBox(6, appr, rej) : null);
                }
            });
            t.getColumns().add(actC);
        }
        return t;
    }

    // ═══════════════════════════════════════════════════════════════
    // Overdue panel
    // ═══════════════════════════════════════════════════════════════

    private VBox overduePanel() {
        VBox p = new VBox(12);
        p.setFillWidth(true);

        HBox banner = new HBox(12);
        banner.setStyle("-fx-background-color:#FEF2F2; -fx-background-radius:12px; " +
                "-fx-border-radius:12px; -fx-border-color:#FECACA; -fx-border-width:1;");
        banner.setPadding(new Insets(16));
        banner.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size:28px;");
        VBox txt = new VBox(2,
                styledLabel("Overdue Books Alert", 16, "#991B1B", true),
                styledLabel("These records have exceeded their due date.", 13, "#B91C1C", false));
        banner.getChildren().addAll(icon, txt);

        TableView<IssueRecord> ot = new TableView<>();
        ot.getStyleClass().add("table-view");
        ot.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ot.setPlaceholder(new Label("No overdue books! 🎉"));

        ot.getColumns().addAll(
                colIR("Book Title",    r -> r.getBookTitle(), 200),
                colIR("Borrower",      r -> r.getUserId(), 120),
                colIR("Due Date",      r -> r.getDueDate().format(DATE_FMT), 110),
                colIR("Days Overdue",  r -> String.valueOf(r.getDaysOverdue()), 100),
                colIR("Fine",          r -> "$" + String.format("%.2f", r.calculateFine()), 90)
        );

        ObservableList<IssueRecord> overdueData =
                FXCollections.observableArrayList(BookService.getAllOverdueBooks());
        ot.setItems(overdueData);
        VBox.setVgrow(ot, Priority.ALWAYS);

        // Export button
        Button exportBtn = AppTheme.createIconTextButton(
                "Export Overdue CSV", AppTheme.ICON_DOWNLOAD, AppTheme.ButtonStyle.OUTLINE);
        exportBtn.setOnAction(e -> exportOverdueReport(overdueData));
        HBox bar2 = new HBox(exportBtn);
        bar2.setAlignment(Pos.CENTER_RIGHT);

        p.getChildren().addAll(banner, bar2, ot);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // Issue Book Dialog — proper UI
    // ═══════════════════════════════════════════════════════════════

    private void showIssueDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Issue Book");
        dlg.initOwner(getScene().getWindow());

        DialogPane dp = dlg.getDialogPane();
        AppTheme.applyTheme(dp);
        dp.setPrefWidth(520);

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        Label heading = new Label("📤  Issue Book to User");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#0F172A;");

        // ── Book picker ──────────────────────────────────────────
        Label bookLbl = fieldLabel("Select Book");
        TextField bookSearch = new TextField();
        bookSearch.setPromptText("Search by title, author or ISBN");
        bookSearch.setStyle(inputStyle());

        ListView<Book> bookList = new ListView<>();
        bookList.setPrefHeight(130);
        bookList.setStyle("-fx-background-color:white; -fx-border-color:#E2E8F0; " +
                "-fx-border-radius:8px; -fx-background-radius:8px;");
        bookList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setText(null); return; }
                setText(b.getTitle() + " — " + b.getAuthor() +
                        "  [" + b.getQuantity() + " available]");
            }
        });

        Label bookAvail = new Label();
        bookAvail.setStyle("-fx-font-size:12px;");

        Runnable refreshBooks = () -> {
            String q = bookSearch.getText().trim().toLowerCase();
            List<Book> all = BookService.getAllBooks().stream()
                    .filter(b -> b.getQuantity() > 0)
                    .filter(b -> q.isEmpty() || b.getTitle().toLowerCase().contains(q)
                            || b.getAuthor().toLowerCase().contains(q)
                            || b.getIsbn().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            bookList.setItems(FXCollections.observableArrayList(all));
        };
        refreshBooks.run();
        bookSearch.textProperty().addListener((o, old, v) -> refreshBooks.run());
        bookList.getSelectionModel().selectedItemProperty().addListener((o, old, b) -> {
            if (b != null) {
                bookAvail.setText("✓  " + b.getQuantity() + " cop" +
                        (b.getQuantity() == 1 ? "y" : "ies") + " available");
                bookAvail.setStyle("-fx-font-size:12px; -fx-text-fill:#16A34A; -fx-font-weight:600;");
            }
        });

        // ── User picker ──────────────────────────────────────────
        Label userLbl = fieldLabel("Select User");
        TextField userSearch = new TextField();
        userSearch.setPromptText("Search by username or name");
        userSearch.setStyle(inputStyle());

        ListView<User> userListView = new ListView<>();
        userListView.setPrefHeight(120);
        userListView.setStyle("-fx-background-color:white; -fx-border-color:#E2E8F0; " +
                "-fx-border-radius:8px; -fx-background-radius:8px;");
        userListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); return; }
                setText(u.getUserId() + "  —  " + u.getFullName());
            }
        });

        Runnable refreshUsers = () -> {
            String q = userSearch.getText().trim().toLowerCase();
            List<User> all = UserService.getAllUsers().stream()
                    .filter(u -> q.isEmpty()
                            || u.getUserId().toLowerCase().contains(q)
                            || u.getFullName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            userListView.setItems(FXCollections.observableArrayList(all));
        };
        refreshUsers.run();
        userSearch.textProperty().addListener((o, old, v) -> refreshUsers.run());

        // ── Quantity ─────────────────────────────────────────────
        Label qtyLbl = fieldLabel("Quantity");
        Spinner<Integer> qtySpin = new Spinner<>(1, 20, 1);
        qtySpin.setEditable(true);
        qtySpin.setPrefWidth(90);

        // ── Error feedback ────────────────────────────────────────
        Label errLbl = new Label();
        errLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#DC2626;");
        errLbl.setVisible(false);

        root.getChildren().addAll(
                heading,
                bookLbl, bookSearch, bookList, bookAvail,
                userLbl, userSearch, userListView,
                new HBox(12, qtyLbl, qtySpin),
                errLbl
        );
        dp.setContent(root);

        ButtonType issueType = new ButtonType("Issue", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(ButtonType.CANCEL, issueType);
        Button issueBtn = (Button) dp.lookupButton(issueType);
        issueBtn.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:600; -fx-font-size:14px; " +
                "-fx-background-radius:10px; -fx-padding:10 24;");

        issueBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            Book book = bookList.getSelectionModel().getSelectedItem();
            User user = userListView.getSelectionModel().getSelectedItem();
            int qty   = qtySpin.getValue();

            if (book == null) { showErr(errLbl, "Please select a book."); ev.consume(); return; }
            if (user == null) { showErr(errLbl, "Please select a user."); ev.consume(); return; }
            if (book.getQuantity() < qty) {
                showErr(errLbl, "Only " + book.getQuantity() + " copies available."); ev.consume(); return;
            }
            errLbl.setVisible(false);
            try {
                BookService.issueBookToUser(book.getIsbn(), user.getUserId(), qty);
                if (toast != null) toast.showSuccess("Issued: " + book.getTitle() + " to " + user.getUserId());
                if (onRefresh != null) onRefresh.run();
            } catch (Exception ex) {
                if (toast != null) toast.showError("Issue failed: " + ex.getMessage());
            }
        });

        dlg.setResultConverter(b -> null);
        dlg.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    // Actions
    // ═══════════════════════════════════════════════════════════════

    private void returnBook(IssueRecord r) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Return Book");
        a.setHeaderText("Return: " + r.getBookTitle());
        double fine = r.calculateFine();
        a.setContentText("Borrower: " + r.getUserId() + "\nQty: " + r.getQuantity()
                + (fine > 0 ? "\nFine outstanding: $" + String.format("%.2f", fine) : ""));
        a.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                BookService.returnBookFromUser(r.getIsbn(), r.getUserId(), r.getQuantity());
                if (toast != null) toast.showSuccess("Book returned successfully.");
                if (onRefresh != null) onRefresh.run();
            } catch (Exception ex) {
                if (toast != null) toast.showError("Return failed: " + ex.getMessage());
            }
        });
    }

    private void renewBook(IssueRecord r) {
        int days = BookService.getLoanPeriodDays();
        if (r.renew(days)) {
            if (toast != null) toast.showSuccess("Renewed! New due date: " +
                    r.getDueDate().format(DATE_FMT));
            if (onRefresh != null) onRefresh.run();
        } else {
            if (toast != null) toast.showError("Cannot renew — max renewals reached.");
        }
    }

    private void approveRequest(BorrowRequest req) {
        try {
            BookService.approveBorrowRequest(req.getRequestId(), currentUser);
            if (toast != null) toast.showSuccess("Request approved!");
            if (onRefresh != null) onRefresh.run();
        } catch (Exception ex) {
            if (toast != null) toast.showError("Approve failed: " + ex.getMessage());
        }
    }

    private void rejectRequest(BorrowRequest req) {
        TextInputDialog td = new TextInputDialog();
        td.setTitle("Reject Request");
        td.setHeaderText("Reject request for: " + req.getBookTitle());
        td.setContentText("Reason (optional):");
        td.showAndWait().ifPresent(reason -> {
            try {
                BookService.rejectBorrowRequest(req.getRequestId(), currentUser, reason);
                if (toast != null) toast.showSuccess("Request rejected.");
                if (onRefresh != null) onRefresh.run();
            } catch (Exception ex) {
                if (toast != null) toast.showError("Reject failed: " + ex.getMessage());
            }
        });
    }

    private void exportOverdueReport(ObservableList<IssueRecord> data) {
        try {
            java.nio.file.Path p = com.example.services.ReportExportService
                    .exportOverdueReportCsv(data);
            if (toast != null) toast.showSuccess("Exported to: " + p.toAbsolutePath());
        } catch (Exception ex) {
            if (toast != null) toast.showError("Export failed: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Binding
    // ═══════════════════════════════════════════════════════════════

    private void bind() {
        issuesTable.setItems(issueRecords);
        requestsTable.setItems(borrowRequests);
    }

    // ═══════════════════════════════════════════════════════════════
    // Column helpers
    // ═══════════════════════════════════════════════════════════════

    @FunctionalInterface interface StrFn<T> { String apply(T t); }

    private static <T> TableColumn<T, String> makeCol(String name, StrFn<T> fn, double w) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private TableColumn<IssueRecord,  String> col(String n, StrFn<IssueRecord>  f, double w) { return makeCol(n,f,w); }
    private TableColumn<BorrowRequest,String> col2(String n, StrFn<BorrowRequest> f, double w) { return makeCol(n,f,w); }
    private TableColumn<IssueRecord,  String> colIR(String n, StrFn<IssueRecord> f, double w) { return makeCol(n,f,w); }

    // ═══════════════════════════════════════════════════════════════
    // Style helpers
    // ═══════════════════════════════════════════════════════════════

    private static Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                "-fx-font-size:12px; -fx-background-radius:6px; -fx-padding:4 10; " +
                "-fx-cursor:hand;");
        return b;
    }
    private static Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#374151;");
        return l;
    }
    private static Label styledLabel(String t, int size, String color, boolean bold) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:" + size + "px; -fx-text-fill:" + color + ";" +
                (bold ? "-fx-font-weight:700;" : ""));
        l.setWrapText(true);
        return l;
    }
    private static String inputStyle() {
        return "-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:10 14; -fx-font-size:14px;";
    }
    private static void showErr(Label lbl, String msg) { lbl.setText(msg); lbl.setVisible(true); }
}
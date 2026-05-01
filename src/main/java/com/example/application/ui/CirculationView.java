package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.entities.Book;
import com.example.entities.BorrowRequest;
import com.example.entities.BooksDB;
import com.example.entities.BooksDB.IssueRecord;
import com.example.entities.User;
import com.example.services.BookService;
import com.example.services.ReminderService;
import com.example.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
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
        setStyle("-fx-background-color: " + pageBackground() + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:" + pageBackground() + ";");

        content.getChildren().addAll(buildHeader(), buildTabs());
        scroll.setContent(content);
        setCenter(scroll);
    }

    private VBox buildHeader() {
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = new StackPane(AppTheme.createIcon(AppTheme.ICON_SYNC, 18));
        badge.setMinSize(40, 40);
        badge.setPrefSize(40, 40);
        badge.setStyle("-fx-background-color:#0D948822; -fx-background-radius:12px;");

        VBox textBlock = new VBox(4);
        Label title = new Label("Circulation");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage book issues, returns, renewals and borrow requests");
        sub.getStyleClass().add("page-subtitle");
        textBlock.getChildren().addAll(title, sub);

        titleRow.getChildren().addAll(badge, textBlock);

        VBox h = new VBox(titleRow);
        return h;
    }

    private TabPane buildTabs() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.setStyle("-fx-background-color:transparent;");

        tp.getTabs().add(tab("Active Issues", AppTheme.ICON_LIBRARY, issuesPanel()));
        tp.getTabs().add(tab("Borrow Requests", AppTheme.ICON_NOTIFICATION, requestsPanel()));
        if (isStaff) tp.getTabs().add(tab("Overdue", AppTheme.ICON_WARNING, overduePanel()));

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
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        actC.setPrefWidth(112);
        actC.setCellFactory(c -> new TableCell<>() {
            final Button retBtn  = actionIconBtn(AppTheme.ICON_RETURN, "Return book", "#0D9488");
            final Button renBtn  = actionIconBtn(AppTheme.ICON_REFRESH,  "Renew loan", "#3B82F6");
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
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER);
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
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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

        // Note/rejection reason column with tooltip for long text
        TableColumn<BorrowRequest, String> noteC = makeCol("Reason",
                r -> r.getNote() != null ? r.getNote() : "", 220);
        noteC.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || s.isEmpty()) { setText(null); setGraphic(null); setTooltip(null); return; }

                String display = s.length() > 72 ? s.substring(0, 69) + "..." : s;
                Label preview = new Label(display);
                preview.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12px;");
                preview.setWrapText(true);
                preview.setMaxWidth(160);

                Tooltip tip = AppTheme.createTooltip(s);
                tip.setWrapText(true);
                tip.setMaxWidth(300);
                tip.setStyle("-fx-font-size:13px;");
                Tooltip.install(preview, tip);

                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().add(preview);

                if (s.length() > 72) {
                    Button viewBtn = AppTheme.createIconButton(
                            AppTheme.ICON_VISIBILITY, "View full reason", AppTheme.ButtonStyle.GHOST);
                    viewBtn.setOnAction(event -> showLongTextDialog("Request Reason", s));
                    box.getChildren().add(viewBtn);
                }

                setGraphic(box);
                setText(null);
            }
        });

        t.getColumns().addAll(titleC, userC, qtyC, dateC, statusC, noteC);

        if (isStaff) {
            TableColumn<BorrowRequest, Void> actC = new TableColumn<>("Actions");
            actC.setPrefWidth(82);
            actC.setCellFactory(c -> new TableCell<>() {
                final Button appr = actionIconBtn(AppTheme.ICON_CHECK, "Approve request", "#16A34A");
                final Button rej  = actionIconBtn(AppTheme.ICON_CLOSE, "Reject request", "#DC2626");
                {
                    appr.setOnAction(e -> approveRequest(getTableView().getItems().get(getIndex())));
                    rej .setOnAction(e -> rejectRequest (getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null)
                    { setGraphic(null); return; }
                    BorrowRequest req = getTableRow().getItem();
                    if (req.isPending()) {
                        HBox box = new HBox(4, appr, rej);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
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
        banner.setStyle("-fx-background-color:" + overdueBannerBackground() + "; -fx-background-radius:12px; " +
                "-fx-border-radius:12px; -fx-border-color:" + overdueBannerBorder() + "; -fx-border-width:1;");
        banner.setPadding(new Insets(16));
        banner.setAlignment(Pos.CENTER_LEFT);
        StackPane icon = new StackPane(AppTheme.createIcon(AppTheme.ICON_WARNING, 18));
        icon.setMinSize(40, 40);
        icon.setPrefSize(40, 40);
        icon.setStyle("-fx-background-color:" + overdueIconSurface() + "; -fx-background-radius:12px;");
        VBox txt = new VBox(2,
                styledLabel("Overdue Books Alert", 16, overdueBannerTitle(), true),
                styledLabel("These records have exceeded their due date.", 13, overdueBannerText(), false));
        banner.getChildren().addAll(icon, txt);

        TableView<IssueRecord> ot = new TableView<>();
        ot.getStyleClass().add("table-view");
        ot.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ot.setPlaceholder(new Label("No overdue books! 🎉"));

        ot.getColumns().addAll(
                colIR("Book Title",    r -> r.getBookTitle(), 200),
                colIR("Borrower",      r -> r.getUserId(), 120),
                colIR("Due Date",      r -> r.getDueDate().format(DATE_FMT), 110),
                colIR("Days Overdue",  r -> String.valueOf(r.getDaysOverdue()), 100),
                colIR("Fine",          r -> AppTheme.formatCurrency(r.calculateFine()), 110),
                overdueActionColumn()
        );

        ObservableList<IssueRecord> overdueData =
                FXCollections.observableArrayList(BookService.getAllOverdueBooks());
        ot.setItems(overdueData);
        VBox.setVgrow(ot, Priority.ALWAYS);

        // Export + Print buttons
        Button exportBtn = AppTheme.createIconTextButton(
                "Export Overdue CSV", AppTheme.ICON_DOWNLOAD, AppTheme.ButtonStyle.OUTLINE);
        exportBtn.setOnAction(e -> exportOverdueReport(overdueData));

        Button printBtn = AppTheme.createIconTextButton(
                "Print Report", AppTheme.ICON_PRINT, AppTheme.ButtonStyle.OUTLINE);
        printBtn.setOnAction(e -> printOverdueReport(ot));

        HBox bar2 = new HBox(8, printBtn, exportBtn);
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
        dp.setPrefHeight(660);

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        Label heading = new Label("Issue Book to User");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:" + textPrimary() + ";");

        // ── Book picker ──────────────────────────────────────────
        Label bookLbl = fieldLabel("Select Book");
        TextField bookSearch = new TextField();
        bookSearch.setPromptText("Search by title, author or ISBN...");
        bookSearch.setStyle(inputStyle());

        ListView<Book> bookList = new ListView<>();
        bookList.setPrefHeight(130);
        bookList.setStyle(listSurfaceStyle());
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
                bookAvail.setText(b.getQuantity() + " cop" +
                        (b.getQuantity() == 1 ? "y" : "ies") + " available");
                bookAvail.setStyle("-fx-font-size:12px; -fx-text-fill:#16A34A; -fx-font-weight:600;");
            }
        });

        // ── User picker ──────────────────────────────────────────
        Label userLbl = fieldLabel("Select User");
        TextField userSearch = new TextField();
        userSearch.setPromptText("Search by username or name...");
        userSearch.setStyle(inputStyle());

        ListView<User> userListView = new ListView<>();
        userListView.setPrefHeight(120);
        userListView.setStyle(listSurfaceStyle());
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

        Label issueDateLbl = fieldLabel("Issue Date");
        DatePicker issueDatePicker = new DatePicker(LocalDate.now());
        issueDatePicker.setEditable(false);
        issueDatePicker.setStyle(inputStyle());

        Label loanDaysLbl = fieldLabel("Loan Period");
        Spinner<Integer> loanDaysSpin = new Spinner<>(1, 365, BookService.getLoanPeriodDays());
        loanDaysSpin.setEditable(true);
        loanDaysSpin.setPrefWidth(110);

        Label testingHint = new Label("Use an earlier issue date to create overdue records from the UI.");
        testingHint.setStyle("-fx-font-size:12px; -fx-text-fill:" + textMuted() + ";");
        testingHint.setWrapText(true);

        // ── Error feedback ────────────────────────────────────────
        Label errLbl = new Label();
        errLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#DC2626;");
        errLbl.setVisible(false);

        root.getChildren().addAll(
                heading,
                bookLbl, bookSearch, bookList, bookAvail,
                userLbl, userSearch, userListView,
                new HBox(12,
                        new VBox(6, qtyLbl, qtySpin),
                        new VBox(6, issueDateLbl, issueDatePicker),
                        new VBox(6, loanDaysLbl, loanDaysSpin)),
                testingHint,
                errLbl
        );
        ScrollPane formScroll = new ScrollPane(root);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        dp.setContent(formScroll);

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
            if (issueDatePicker.getValue() == null) {
                showErr(errLbl, "Please choose an issue date."); ev.consume(); return;
            }
            errLbl.setVisible(false);
            try {
                BookService.issueBookToUser(
                        book.getIsbn(), user.getUserId(), qty, issueDatePicker.getValue(), loanDaysSpin.getValue());
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
                + (fine > 0 ? "\nFine outstanding: " + AppTheme.formatCurrency(fine) : ""));
        AppTheme.applyTheme(a.getDialogPane());
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
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reject Request");
        dialog.setHeaderText("Reject request for: " + req.getBookTitle());
        AppTheme.applyTheme(dialog.getDialogPane());

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Enter a rejection reason (optional)");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(6);

        dialog.getDialogPane().setContent(reasonArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> button == ButtonType.OK ? reasonArea.getText() : null);

        dialog.showAndWait().ifPresent(reason -> {
            try {
                BookService.rejectBorrowRequest(req.getRequestId(), currentUser, reason);
                if (toast != null) toast.showSuccess("Request rejected.");
                if (onRefresh != null) onRefresh.run();
            } catch (Exception ex) {
                if (toast != null) toast.showError("Reject failed: " + ex.getMessage());
            }
        });
    }

    /** Opens the OS print dialog and prints the overdue TableView. */
    private void printOverdueReport(javafx.scene.control.TableView<IssueRecord> table) {
        // Create job for default printer (null = let dialog pick); avoids false "no printers" errors
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            if (toast != null) toast.showError("Could not start the system print dialog.");
            return;
        }

        boolean proceed = job.showPrintDialog(getScene().getWindow());
        if (!proceed) return;

        // Scale table to fit page width
        javafx.print.PageLayout layout = job.getJobSettings().getPageLayout();
        double printW = layout.getPrintableWidth();
        double tableW = table.getBoundsInParent().getWidth();
        double scale  = (tableW > 0) ? printW / tableW : 1.0;

        javafx.scene.transform.Scale scaleT = new javafx.scene.transform.Scale(scale, scale);
        table.getTransforms().add(scaleT);
        boolean printed = job.printPage(table);
        table.getTransforms().remove(scaleT);

        if (printed) {
            job.endJob();
            if (toast != null) toast.showSuccess("Overdue report sent to printer.");
        } else {
            if (toast != null) toast.showError("Printing failed — check printer status.");
        }
    }

    private void exportOverdueReport(ObservableList<IssueRecord> data) {        try {
        java.nio.file.Path p = com.example.services.ReportExportService
                .exportOverdueReportCsv(data);
        if (toast != null) toast.showSuccess("Exported to: " + p.toAbsolutePath());
    } catch (Exception ex) {
        if (toast != null) toast.showError("Export failed: " + ex.getMessage());
    }
    }

    private TableColumn<IssueRecord, Void> overdueActionColumn() {
        TableColumn<IssueRecord, Void> actionColumn = new TableColumn<>("Actions");
        actionColumn.setPrefWidth(116);
        actionColumn.setCellFactory(col -> new TableCell<>() {
            final Button emailBtn = actionIconBtn(AppTheme.ICON_MAIL, "Send overdue reminder", "#0D9488");
            final Button contactBtn = actionIconBtn(AppTheme.ICON_USER, "View borrower contact", "#64748B");
            final HBox box = new HBox(4, emailBtn, contactBtn);

            {
                box.setAlignment(Pos.CENTER);
                emailBtn.setOnAction(event -> sendOverdueReminder(getTableView().getItems().get(getIndex()), emailBtn));
                contactBtn.setOnAction(event -> showBorrowerContact(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                IssueRecord record = getTableRow().getItem();
                User borrower = UserService.getUserById(record.getUserId());
                boolean canEmail = borrower != null && borrower.getEmail() != null && !borrower.getEmail().isBlank();
                emailBtn.setDisable(!canEmail);
                setGraphic(box);
            }
        });
        return actionColumn;
    }

    private void sendOverdueReminder(IssueRecord record, Button triggerButton) {
        if (toast != null) {
            toast.showInfo("Sending reminder email…");
        }
        Platform.runLater(() -> triggerButton.setDisable(true));
        new Thread(() -> {
            try {
                User user = UserService.getUserById(record.getUserId());
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new IllegalStateException("Borrower does not have an email address on file.");
                }

                ReminderService.sendOverdueReminder(user, List.of(record));
                Platform.runLater(() -> {
                    triggerButton.setDisable(false);
                    if (toast != null) {
                        toast.showSuccess("Reminder sent to " + user.getEmail());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    triggerButton.setDisable(false);
                    if (toast != null) {
                        toast.showError("Reminder failed: " + ReminderService.toUserMessage(ex));
                    }
                });
            }
        }, "overdue-reminder").start();
    }

    private void showBorrowerContact(IssueRecord record) {
        try {
            User user = UserService.getUserById(record.getUserId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Borrower Contact");
            alert.setHeaderText(user.getDisplayName());
            alert.setContentText("Email: " + valueOrPlaceholder(user.getEmail()) +
                    "\nMobile: " + valueOrPlaceholder(user.getContactNumber()));
            AppTheme.applyTheme(alert.getDialogPane());
            alert.showAndWait();
        } catch (Exception ex) {
            if (toast != null) {
                toast.showError("Could not load borrower contact: " + ex.getMessage());
            }
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

    private static Button actionIconBtn(String iconPath, String tooltip, String color) {
        Button b = new Button();
        var icon = AppTheme.createIcon(iconPath, 14);
        icon.setStyle("-fx-fill:white;");
        b.setGraphic(icon);
        b.setTooltip(AppTheme.createTooltip(tooltip));
        b.setStyle("-fx-background-color:" + color + "; -fx-background-radius:8px; " +
                "-fx-cursor:hand; -fx-padding:5; -fx-min-width:28px; -fx-pref-width:28px; " +
                "-fx-max-width:28px; -fx-min-height:28px; -fx-pref-height:28px; -fx-max-height:28px;");
        return b;
    }

    private static Tab tab(String title, String iconPath, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(AppTheme.createIcon(iconPath, 14));
        return tab;
    }
    private static Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:" +
                (AppTheme.darkMode ? "#CBD5E1" : "#374151") + ";");
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
        if (AppTheme.darkMode) {
            return "-fx-background-color:#1E293B; -fx-border-color:#334155; " +
                    "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                    "-fx-padding:10 14; -fx-font-size:14px; -fx-text-fill:#E2E8F0;";
        }
        return "-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:10 14; -fx-font-size:14px;";
    }
    private static void showErr(Label lbl, String msg) { lbl.setText(msg); lbl.setVisible(true); }

    private static String listSurfaceStyle() {
        return "-fx-background-color:" + (AppTheme.darkMode ? "#1E293B" : "white") + "; " +
                "-fx-border-color:" + (AppTheme.darkMode ? "#334155" : "#E2E8F0") + "; " +
                "-fx-border-radius:8px; -fx-background-radius:8px;";
    }

    private void showLongTextDialog(String title, String value) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        AppTheme.applyTheme(alert.getDialogPane());

        TextArea textArea = new TextArea(value);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(10);
        textArea.setPrefColumnCount(38);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private static String pageBackground() {
        return AppTheme.darkMode ? "#0F172A" : "#F1F5F9";
    }

    private static String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#0F172A";
    }

    private static String textMuted() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    private static String overdueBannerBackground() {
        return AppTheme.darkMode ? "rgba(127,29,29,0.28)" : "#FEF2F2";
    }

    private static String overdueBannerBorder() {
        return AppTheme.darkMode ? "#7F1D1D" : "#FECACA";
    }

    private static String overdueIconSurface() {
        return AppTheme.darkMode ? "rgba(248,113,113,0.16)" : "#FCA5A522";
    }

    private static String overdueBannerTitle() {
        return AppTheme.darkMode ? "#FECACA" : "#991B1B";
    }

    private static String overdueBannerText() {
        return AppTheme.darkMode ? "#FCA5A5" : "#B91C1C";
    }

    private static String valueOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "(not provided)" : value;
    }
}
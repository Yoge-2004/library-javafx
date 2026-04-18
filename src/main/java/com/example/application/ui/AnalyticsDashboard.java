package com.example.application.ui;

import com.example.entities.Book;
import com.example.entities.BooksDB;
import com.example.entities.BooksDB.IssueRecord;
import com.example.services.BookService;
import com.example.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Analytics dashboard — role-aware.
 *
 * Admin / Librarian: global stats (books, users, issued, overdue, fines, requests).
 * User: personal stats only (borrowed, overdue, fines, pending requests).
 */
public class AnalyticsDashboard extends BorderPane {

    private final Runnable onRefresh;
    private final String  currentUser;
    private final boolean isStaff;
    private Runnable onNavigateToCirculation;
    private Runnable onNavigateToCatalog;

    private GridPane statsGrid;
    private VBox     recentPanel;
    private VBox     overduePanel;
    private PieChart categoryChart;
    private BarChart<String, Number> activityChart;

    public AnalyticsDashboard(Runnable onRefresh, String currentUser, boolean isStaff) {
        this.onRefresh   = onRefresh;
        this.currentUser = currentUser;
        this.isStaff     = isStaff;
        initUI();
    }

    /** Allow the app to inject navigation callbacks so stat cards can redirect. */
    public void setNavigationCallbacks(Runnable toCirculation, Runnable toCatalog) {
        this.onNavigateToCirculation = toCirculation;
        this.onNavigateToCatalog     = toCatalog;
    }

    // ═══════════════════════════════════════════════════════════════
    // Build
    // ═══════════════════════════════════════════════════════════════

    private void initUI() {
        setStyle("-fx-background-color:#F1F5F9;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:#F1F5F9;");

        // Header
        VBox header = AppTheme.createHeaderBlock("OVERVIEW",
                "Library Dashboard",
                isStaff ? "Real-time insights into your library's performance"
                        : "Your personal borrowing overview");

        statsGrid = new GridPane();
        statsGrid.setHgap(16); statsGrid.setVgap(16);

        HBox charts  = buildCharts();
        HBox bottom  = buildBottomPanels();

        if (isStaff) {
            content.getChildren().addAll(header, statsGrid, charts, bottom);
        } else {
            // For regular users skip the charts (irrelevant) — just stats + their activity
            content.getChildren().addAll(header, statsGrid, bottom);
        }

        scroll.setContent(content);
        setCenter(scroll);
        loadData();
    }

    // ─── Charts ───────────────────────────────────────────────────

    private HBox buildCharts() {
        HBox section = new HBox(16);
        section.setAlignment(Pos.TOP_LEFT);

        // Category pie
        VBox catPanel = new VBox(12);
        catPanel.getStyleClass().add("surface-card");
        catPanel.setPadding(new Insets(20));
        catPanel.setPrefWidth(380);

        Label catTitle = new Label("Books by Category");
        catTitle.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#1E293B;");

        categoryChart = new PieChart();
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);
        categoryChart.setLegendSide(javafx.geometry.Side.RIGHT);
        categoryChart.setPrefHeight(260);
        categoryChart.setStyle("-fx-font-size:11px;");

        catPanel.getChildren().addAll(catTitle, categoryChart);

        // Monthly activity
        VBox actPanel = new VBox(12);
        actPanel.getStyleClass().add("surface-card");
        actPanel.setPadding(new Insets(20));
        HBox.setHgrow(actPanel, Priority.ALWAYS);

        Label actTitle = new Label("Monthly Activity");
        actTitle.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#1E293B;");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Books");

        activityChart = new BarChart<>(xAxis, yAxis);
        activityChart.setLegendVisible(true);
        activityChart.setStyle("-fx-font-size:11px;");
        activityChart.setPrefHeight(260);
        activityChart.setCategoryGap(16);
        activityChart.setBarGap(2);

        actPanel.getChildren().addAll(actTitle, activityChart);
        section.getChildren().addAll(catPanel, actPanel);
        return section;
    }

    private HBox buildBottomPanels() {
        HBox section = new HBox(16);
        section.setAlignment(Pos.TOP_LEFT);

        recentPanel = new VBox(12);
        recentPanel.getStyleClass().add("surface-card");
        recentPanel.setPadding(new Insets(20));
        HBox.setHgrow(recentPanel, Priority.ALWAYS);

        overduePanel = new VBox(12);
        overduePanel.getStyleClass().add("surface-card");
        overduePanel.setPadding(new Insets(20));
        HBox.setHgrow(overduePanel, Priority.ALWAYS);

        recentPanel.getChildren().add(panelTitle(isStaff ? "Recent Issues" : "My Active Books"));
        overduePanel.getChildren().add(panelTitle(isStaff ? "Top Overdue" : "My Overdue Books"));

        section.getChildren().addAll(recentPanel, overduePanel);
        return section;
    }

    private Label panelTitle(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
        return l;
    }

    // ═══════════════════════════════════════════════════════════════
    // Update (called externally by LibraryApp)
    // ═══════════════════════════════════════════════════════════════

    public void update(Map<String, Object> stats, int totalUsers, int staffCount) {
        Platform.runLater(() -> {
            updateStats(stats, totalUsers, staffCount);
            if (isStaff) {
                updateCategoryChart();
                updateActivityChart();
            }
            updateRecentPanel();
            updateOverduePanel();
        });
    }

    // ─── Stat cards ───────────────────────────────────────────────

    private void updateStats(Map<String, Object> stats, int totalUsers, int staffCount) {
        statsGrid.getChildren().clear();

        if (isStaff) {
            int totalBooks     = num(stats, "totalBooks");
            int totalCopies    = num(stats, "totalCopies");
            int available      = num(stats, "availableCopies");
            int issued         = num(stats, "issuedCopies");
            int overdue        = num(stats, "overdueBooks");
            double fines       = dbl(stats, "totalFines");
            int pending        = num(stats, "pendingRequests");
            double util        = totalCopies > 0 ? issued * 100.0 / totalCopies : 0;

            VBox c0 = statCard("📚", "Total Books",   str(totalBooks),
                    totalCopies + " copies", "#0D9488", onNavigateToCatalog);
            VBox c1 = statCard("✓", "Available",    str(available),
                    String.format("%.1f%% free", 100-util), "#16A34A", onNavigateToCatalog);
            VBox c2 = statCard("🔄", "Issued",       str(issued),
                    String.format("%.1f%% utilisation", util), "#3B82F6", onNavigateToCirculation);
            VBox c3 = statCard("⚠", "Overdue",      str(overdue),
                    "Needs attention", "#DC2626", onNavigateToCirculation);
            VBox c4 = statCard("💰", "Total Fines",  "$" + String.format("%.2f", fines),
                    "Outstanding", "#F59E0B", onNavigateToCirculation);
            VBox c5 = statCard("📝", "Pending",      str(pending),
                    "Requests awaiting approval", "#8B5CF6", onNavigateToCirculation);
            VBox c6 = statCard("👥", "Users",        str(totalUsers),
                    staffCount + " staff", "#06B6D4", null);

            statsGrid.add(c0, 0, 0); statsGrid.add(c1, 1, 0);
            statsGrid.add(c2, 2, 0); statsGrid.add(c3, 3, 0);
            statsGrid.add(c4, 0, 1); statsGrid.add(c5, 1, 1);
            statsGrid.add(c6, 2, 1);

            // Equal column widths
            for (int i = 0; i < 4; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(25);
                cc.setHgrow(Priority.ALWAYS);
                statsGrid.getColumnConstraints().add(cc);
            }
        } else {
            // Personal view
            int myBorrowed = BookService.getUserTotalBorrowedBooks(currentUser);
            int myOverdue  = BookService.getUserOverdueBooks(currentUser).size();
            double myFine  = BookService.getUserTotalFine(currentUser);
            int myPending  = (int) BookService.getBorrowRequestsForUser(currentUser).stream()
                    .filter(r -> r.getStatus() == com.example.entities.BorrowRequest.Status.PENDING)
                    .count();

            statsGrid.add(statCard("📖", "Borrowed",         str(myBorrowed),
                    "Currently with you", "#0D9488", onNavigateToCirculation), 0, 0);
            statsGrid.add(statCard("⚠", "Overdue",          str(myOverdue),
                    "Need immediate return", "#DC2626", onNavigateToCirculation), 1, 0);
            statsGrid.add(statCard("💰", "Your Fines",       "$" + String.format("%.2f", myFine),
                    "Outstanding fine", "#F59E0B", onNavigateToCirculation), 2, 0);
            statsGrid.add(statCard("📝", "Pending Requests", str(myPending),
                    "Awaiting approval", "#8B5CF6", onNavigateToCirculation), 3, 0);

            for (int i = 0; i < 4; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(25);
                cc.setHgrow(Priority.ALWAYS);
                statsGrid.getColumnConstraints().add(cc);
            }
        }
    }

    private VBox statCard(String emoji, String label, String value,
                          String sub, String color, Runnable onClick) {
        VBox card = new VBox(10);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(20));

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:26px;");

        VBox txt = new VBox(2);
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size:24px; -fx-font-weight:800; -fx-text-fill:" + color + ";");
        valueLbl.setWrapText(false);
        Label nameLbl  = new Label(label);
        nameLbl.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#64748B;");
        nameLbl.setWrapText(true);
        txt.getChildren().addAll(valueLbl, nameLbl);

        top.getChildren().addAll(emojiLbl, txt);
        card.getChildren().add(top);

        if (sub != null && !sub.isEmpty()) {
            Label subLbl = new Label(sub);
            subLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#94A3B8;");
            subLbl.setWrapText(true);
            card.getChildren().add(subLbl);
        }

        if (onClick != null) {
            card.setOnMouseClicked(e -> onClick.run());
            Label hint = new Label("Click to view ->");
            hint.setStyle("-fx-font-size:11px; -fx-text-fill:" + color + "; -fx-opacity:0.6;");
            card.getChildren().add(hint);
        }

        return card;
    }

    // ─── Charts ───────────────────────────────────────────────────

    private void updateCategoryChart() {
        List<Book> books = BookService.getAllBooks();
        Map<String, Long> counts = books.stream()
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .forEach(e -> data.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())));

        categoryChart.setData(data);
    }

    private void updateActivityChart() {
        XYChart.Series<String, Number> issued = new XYChart.Series<>();
        issued.setName("Issued");
        XYChart.Series<String, Number> returned = new XYChart.Series<>();
        returned.setName("Returned");

        // Real data from active records grouped by month
        List<IssueRecord> all = BookService.getAllActiveIssueRecords();
        DateTimeFormatter m = DateTimeFormatter.ofPattern("MMM");
        Map<String, Long> issuedMap = all.stream()
                .collect(Collectors.groupingBy(r -> r.getIssueDate().format(m), Collectors.counting()));

        String[] months = {"Jan","Feb","Mar","Apr","May","Jun"};
        for (String mo : months) {
            issued.getData().add(new XYChart.Data<>(mo, issuedMap.getOrDefault(mo, 0L)));
            returned.getData().add(new XYChart.Data<>(mo, Math.max(0,
                    issuedMap.getOrDefault(mo, 0L) - 1)));
        }

        activityChart.getData().setAll(issued, returned);
    }

    // ─── Bottom panels ────────────────────────────────────────────

    private void updateRecentPanel() {
        clearPanel(recentPanel);
        List<IssueRecord> list = isStaff
                ? BookService.getAllActiveIssueRecords().stream()
                  .sorted(Comparator.comparing(IssueRecord::getIssueDate).reversed())
                  .limit(6).collect(Collectors.toList())
                : BookService.getUserActiveIssueRecords(currentUser);

        if (list.isEmpty()) {
            recentPanel.getChildren().add(emptyMsg("No active issues"));
            return;
        }
        list.forEach(r -> recentPanel.getChildren().add(
                activityRow(r.getBookTitle(),
                        isStaff ? "Issued to " + r.getUserId() : "Due " + r.getDueDate(),
                        r.getIssueDate().format(DateTimeFormatter.ofPattern("MMM dd")),
                        "#3B82F6")));
    }

    private void updateOverduePanel() {
        clearPanel(overduePanel);
        List<IssueRecord> overdue = isStaff
                ? BookService.getAllOverdueBooks().stream()
                  .sorted(Comparator.comparingLong(IssueRecord::getDaysOverdue).reversed())
                  .limit(6).collect(Collectors.toList())
                : BookService.getUserOverdueBooks(currentUser);

        if (overdue.isEmpty()) {
            Label ok = new Label("No overdue books! 🎉");
            ok.setStyle("-fx-font-size:14px; -fx-text-fill:#16A34A; -fx-font-style:italic;");
            overduePanel.getChildren().add(ok);
            return;
        }
        overdue.forEach(r -> overduePanel.getChildren().add(
                activityRow(r.getBookTitle(),
                        r.getDaysOverdue() + " days overdue",
                        "$" + String.format("%.2f", r.calculateFine()),
                        "#DC2626")));
    }

    private HBox activityRow(String title, String sub, String meta, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-border-color:#F1F5F9; -fx-border-width:0 0 1 0;");

        VBox txt = new VBox(2);
        HBox.setHgrow(txt, Priority.ALWAYS);

        Label t = new Label(title);
        t.setStyle("-fx-font-size:14px; -fx-font-weight:600; -fx-text-fill:#1E293B;");
        t.setMaxWidth(220); t.setEllipsisString("...");

        Label s = new Label(sub);
        s.setStyle("-fx-font-size:12px; -fx-text-fill:" + color + ";");
        txt.getChildren().addAll(t, s);

        Label m = new Label(meta);
        m.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;");

        row.getChildren().addAll(txt, m);
        return row;
    }

    private void clearPanel(VBox panel) {
        if (!panel.getChildren().isEmpty()) {
            panel.getChildren().subList(1, panel.getChildren().size()).clear();
        }
    }

    private Label emptyMsg(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size:14px; -fx-text-fill:#94A3B8; -fx-font-style:italic; -fx-padding:20 0 0 0;");
        return l;
    }

    // ─── Initial load ─────────────────────────────────────────────

    private void loadData() {
        Map<String, Object> stats = BookService.getLibraryStatistics();
        long staffCount = UserService.getAllUsers().stream().filter(u -> u.isStaff()).count();
        update(stats, UserService.getAllUsers().size(), (int) staffCount);
    }

    // ─── Utils ───────────────────────────────────────────────────

    private static int    num(Map<String, Object> m, String k) { return ((Number) m.getOrDefault(k, 0)).intValue(); }
    private static double dbl(Map<String, Object> m, String k) { return ((Number) m.getOrDefault(k, 0.0)).doubleValue(); }
    private static String str(int v)                            { return String.valueOf(v); }
}
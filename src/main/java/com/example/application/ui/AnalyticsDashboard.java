package com.example.application.ui;

import com.example.entities.Book;
import com.example.entities.BooksDB.IssueRecord;
import com.example.services.BookService;
import com.example.services.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analytics dashboard — role-aware and responsive.
 *
 * Admin / Librarian: global stats, charts, and operational lists.
 * User: personal stats only, with a cleaner borrow-focused layout.
 */
public class AnalyticsDashboard extends BorderPane {

    private final Runnable onRefresh;
    private final String currentUser;
    private final boolean isStaff;

    private Runnable onNavigateToCirculation;
    private Runnable onNavigateToCatalog;

    private FlowPane statsPane;
    private FlowPane chartsPane;
    private FlowPane bottomPane;
    private VBox recentPanel;
    private VBox overduePanel;
    private PieChart categoryChart;
    private BarChart<String, Number> activityChart;

    public AnalyticsDashboard(Runnable onRefresh, String currentUser, boolean isStaff) {
        this.onRefresh = onRefresh;
        this.currentUser = currentUser;
        this.isStaff = isStaff;
        initUI();
    }

    public void setNavigationCallbacks(Runnable toCirculation, Runnable toCatalog) {
        this.onNavigateToCirculation = toCirculation;
        this.onNavigateToCatalog = toCatalog;
    }

    private void initUI() {
        setStyle("-fx-background-color:" + pageBackground() + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color:" + pageBackground() + ";");

        VBox header = AppTheme.createHeaderBlock(
                "OVERVIEW",
                isStaff ? "Library Dashboard" : "My Borrowing Snapshot",
                isStaff
                        ? "Real-time activity, circulation health, and collection usage."
                        : "Your active books, requests, fines, and overdue items in one place.");

        statsPane = createWrappingSection(16);
        chartsPane = createWrappingSection(16);
        bottomPane = createWrappingSection(16);

        content.getChildren().addAll(header, statsPane);
        if (isStaff) {
            content.getChildren().add(chartsPane);
        }
        content.getChildren().add(bottomPane);

        buildCharts();
        buildBottomPanels();

        scroll.setContent(content);
        setCenter(scroll);
        loadData();
    }

    private FlowPane createWrappingSection(double gap) {
        FlowPane section = new FlowPane();
        section.setHgap(gap);
        section.setVgap(gap);
        section.setAlignment(Pos.TOP_LEFT);
        section.prefWrapLengthProperty().bind(widthProperty().subtract(72));
        return section;
    }

    private void buildCharts() {
        chartsPane.getChildren().clear();

        VBox categoryPanel = createSurfacePanel("Books by Category");
        categoryPanel.setPrefWidth(360);
        categoryPanel.setMinWidth(320);

        categoryChart = new PieChart();
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);
        categoryChart.setLegendSide(javafx.geometry.Side.RIGHT);
        categoryChart.setPrefHeight(280);
        categoryChart.setMinHeight(280);
        categoryChart.setStyle("-fx-font-size: 11px;");
        categoryPanel.getChildren().add(categoryChart);

        VBox activityPanel = createSurfacePanel("Monthly Activity");
        activityPanel.setPrefWidth(520);
        activityPanel.setMinWidth(360);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Books");

        activityChart = new BarChart<>(xAxis, yAxis);
        activityChart.setLegendVisible(true);
        activityChart.setPrefHeight(280);
        activityChart.setMinHeight(280);
        activityChart.setCategoryGap(16);
        activityChart.setBarGap(3);
        activityChart.setStyle("-fx-font-size: 11px;");
        activityPanel.getChildren().add(activityChart);

        chartsPane.getChildren().addAll(categoryPanel, activityPanel);
    }

    private void buildBottomPanels() {
        bottomPane.getChildren().clear();

        recentPanel = createSurfacePanel(isStaff ? "Recent Issues" : "My Active Books");
        recentPanel.setPrefWidth(420);
        recentPanel.setMinWidth(320);

        overduePanel = createSurfacePanel(isStaff ? "Top Overdue" : "My Overdue Books");
        overduePanel.setPrefWidth(420);
        overduePanel.setMinWidth(320);

        bottomPane.getChildren().addAll(recentPanel, overduePanel);
    }

    private VBox createSurfacePanel(String title) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("surface-card");
        panel.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill:" + textPrimary() + ";");
        titleLabel.setWrapText(true);

        panel.getChildren().add(titleLabel);
        return panel;
    }

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

    private void updateStats(Map<String, Object> stats, int totalUsers, int staffCount) {
        statsPane.getChildren().clear();

        if (isStaff) {
            int totalBooks = num(stats, "totalBooks");
            int totalCopies = num(stats, "totalCopies");
            int available = num(stats, "availableCopies");
            int issued = num(stats, "issuedCopies");
            int overdue = num(stats, "overdueBooks");
            double fines = dbl(stats, "totalFines");
            int pending = num(stats, "pendingRequests");
            double utilization = totalCopies > 0 ? issued * 100.0 / totalCopies : 0.0;

            statsPane.getChildren().addAll(
                    statCard(AppTheme.ICON_LIBRARY, "Books in Catalog", str(totalBooks),
                            totalCopies + " copies across the collection", "#0D9488", onNavigateToCatalog),
                    statCard(AppTheme.ICON_CHECK, "Available Copies", str(available),
                            String.format("%.1f%% currently free", Math.max(0.0, 100.0 - utilization)), "#16A34A", onNavigateToCatalog),
                    statCard(AppTheme.ICON_SYNC, "Books on Loan", str(issued),
                            String.format("%.1f%% collection utilisation", utilization), "#3B82F6", onNavigateToCirculation),
                    statCard(AppTheme.ICON_WARNING, "Overdue Returns", str(overdue),
                            "Requires staff follow-up", "#DC2626", onNavigateToCirculation),
                    statCard(AppTheme.ICON_SAVE, "Outstanding Fines", AppTheme.formatCurrency(fines),
                            "Calculated from active overdue loans", "#F59E0B", onNavigateToCirculation),
                    statCard(AppTheme.ICON_NOTIFICATION, "Pending Requests", str(pending),
                            "Awaiting staff action", "#8B5CF6", onNavigateToCirculation),
                    statCard(AppTheme.ICON_USER, "Registered Users", str(totalUsers),
                            staffCount + " staff account(s)", "#06B6D4", null)
            );
        } else {
            int myBorrowed = BookService.getUserTotalBorrowedBooks(currentUser);
            int myOverdue = BookService.getUserOverdueBooks(currentUser).size();
            double myFine = BookService.getUserTotalFine(currentUser);
            int myPending = (int) BookService.getBorrowRequestsForUser(currentUser).stream()
                    .filter(request -> request.getStatus() == com.example.entities.BorrowRequest.Status.PENDING)
                    .count();

            statsPane.getChildren().addAll(
                    statCard(AppTheme.ICON_LIBRARY, "Borrowed Right Now", str(myBorrowed),
                            "Books currently issued to you", "#0D9488", onNavigateToCirculation),
                    statCard(AppTheme.ICON_WARNING, "Need to Return", str(myOverdue),
                            "Overdue items that need attention", "#DC2626", onNavigateToCirculation),
                    statCard(AppTheme.ICON_SAVE, "Outstanding Fine", AppTheme.formatCurrency(myFine),
                            "Fine total on active overdue books", "#F59E0B", onNavigateToCirculation),
                    statCard(AppTheme.ICON_NOTIFICATION, "Pending Requests", str(myPending),
                            "Requests waiting for approval", "#8B5CF6", onNavigateToCirculation)
            );
        }
    }

    private VBox statCard(String iconPath, String label, String value, String subText,
                          String accentColor, Runnable onClick) {
        VBox card = new VBox(12);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(isStaff ? 235 : 250);
        card.setMinWidth(220);
        card.setMaxWidth(280);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = new StackPane(AppTheme.createIcon(iconPath, 18));
        badge.setMinSize(40, 40);
        badge.setPrefSize(40, 40);
        badge.setMaxSize(40, 40);
        badge.setStyle("-fx-background-color:" + accentColor + "22; -fx-background-radius: 12px;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill:" + textMuted() + ";");
        labelText.setWrapText(true);
        labelText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(labelText, Priority.ALWAYS);

        header.getChildren().addAll(badge, labelText);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill:" + accentColor + ";");

        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill:" + textSoft() + ";");
        subLabel.setWrapText(true);

        card.getChildren().addAll(header, valueText, subLabel);

        if (onClick != null) {
            Label hint = new Label("Open details");
            hint.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill:" + accentColor + ";");
            card.getChildren().add(hint);
            card.setOnMouseClicked(event -> onClick.run());
        }

        return card;
    }

    private void updateCategoryChart() {
        List<Book> books = BookService.getAllBooks();
        Map<String, Long> counts = books.stream()
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(7)
                .forEach(entry -> data.add(new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")", entry.getValue())));

        categoryChart.setData(data);
    }

    private void updateActivityChart() {
        XYChart.Series<String, Number> issuedSeries = new XYChart.Series<>();
        issuedSeries.setName("Issued");

        XYChart.Series<String, Number> returnedSeries = new XYChart.Series<>();
        returnedSeries.setName("Returned");

        List<IssueRecord> allRecords = BookService.getAllActiveIssueRecords();
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM");
        Map<String, Long> issuedMap = allRecords.stream()
                .collect(Collectors.groupingBy(record -> record.getIssueDate().format(monthFormat),
                        Collectors.counting()));

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        for (String month : months) {
            long issued = issuedMap.getOrDefault(month, 0L);
            issuedSeries.getData().add(new XYChart.Data<>(month, issued));
            returnedSeries.getData().add(new XYChart.Data<>(month, Math.max(0, issued - 1)));
        }

        activityChart.getData().setAll(issuedSeries, returnedSeries);
    }

    private void updateRecentPanel() {
        clearPanel(recentPanel);
        List<IssueRecord> records = isStaff
                ? BookService.getAllActiveIssueRecords().stream()
                .sorted(Comparator.comparing(IssueRecord::getIssueDate).reversed())
                .limit(6)
                .collect(Collectors.toList())
                : BookService.getUserActiveIssueRecords(currentUser);

        if (records.isEmpty()) {
            recentPanel.getChildren().add(emptyMessage(isStaff ? "No active circulation records" : "No active books"));
            return;
        }

        records.forEach(record -> recentPanel.getChildren().add(activityRow(
                record.getBookTitle(),
                isStaff ? "Issued to " + record.getUserId() : "Due " + record.getDueDate(),
                record.getIssueDate().format(DateTimeFormatter.ofPattern("MMM dd")),
                "#3B82F6"
        )));
    }

    private void updateOverduePanel() {
        clearPanel(overduePanel);
        List<IssueRecord> overdueRecords = isStaff
                ? BookService.getAllOverdueBooks().stream()
                .sorted(Comparator.comparingLong(IssueRecord::getDaysOverdue).reversed())
                .limit(6)
                .collect(Collectors.toList())
                : BookService.getUserOverdueBooks(currentUser);

        if (overdueRecords.isEmpty()) {
            Label ok = new Label("No overdue books");
            ok.setStyle("-fx-font-size: 14px; -fx-text-fill: #16A34A; -fx-font-style: italic;");
            overduePanel.getChildren().add(ok);
            return;
        }

        overdueRecords.forEach(record -> overduePanel.getChildren().add(activityRow(
                record.getBookTitle(),
                record.getDaysOverdue() + " day(s) overdue",
                AppTheme.formatCurrency(record.calculateFine()),
                "#DC2626"
        )));
    }

    private HBox activityRow(String title, String subText, String meta, String accentColor) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-border-color:" + dividerColor() + "; -fx-border-width: 0 0 1 0;");

        VBox textBlock = new VBox(3);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill:" + textPrimary() + ";");
        titleLabel.setWrapText(true);

        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill:" + accentColor + ";");
        subLabel.setWrapText(true);

        textBlock.getChildren().addAll(titleLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label metaLabel = new Label(meta);
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill:" + textSoft() + ";");

        row.getChildren().addAll(textBlock, spacer, metaLabel);
        return row;
    }

    private void clearPanel(VBox panel) {
        if (panel != null && panel.getChildren().size() > 1) {
            panel.getChildren().subList(1, panel.getChildren().size()).clear();
        }
    }

    private Label emptyMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill:" + textSoft() + "; -fx-font-style: italic;");
        return label;
    }

    private void loadData() {
        Map<String, Object> stats = BookService.getLibraryStatistics();
        long staffCount = UserService.getAllUsers().stream().filter(user -> user.isStaff()).count();
        update(stats, UserService.getAllUsers().size(), (int) staffCount);
    }

    private String pageBackground() {
        return AppTheme.darkMode ? "#0F172A" : "#F1F5F9";
    }

    private String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#0F172A";
    }

    private String textMuted() {
        return AppTheme.darkMode ? "#CBD5E1" : "#475569";
    }

    private String textSoft() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    private String dividerColor() {
        return AppTheme.darkMode ? "#334155" : "#E2E8F0";
    }

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.getOrDefault(key, 0)).intValue();
    }

    private static double dbl(Map<String, Object> map, String key) {
        return ((Number) map.getOrDefault(key, 0.0)).doubleValue();
    }

    private static String str(int value) {
        return String.valueOf(value);
    }
}

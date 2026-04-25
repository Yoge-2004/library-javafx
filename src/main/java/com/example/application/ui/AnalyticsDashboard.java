package com.example.application.ui;

import com.example.entities.Book;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Analytics dashboard — role-aware and responsive.
 *
 * Staff users see configurable charts built from real issue history.
 * Regular users see a lighter borrowing snapshot.
 */
public class AnalyticsDashboard extends BorderPane {

    private enum TrendGrouping {
        DAILY,
        WEEKLY,
        MONTHLY
    }

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
    private StackPane trendChartHolder;
    private StackPane overdueChartHolder;

    private ComboBox<String> trendRangeFilter;
    private ComboBox<String> trendGroupingFilter;
    private ComboBox<String> trendSeriesFilter;
    private ComboBox<String> trendCategoryFilter;
    private ComboBox<String> trendStyleFilter;

    private ComboBox<String> categoryMetricFilter;
    private ComboBox<String> categorySortFilter;
    private ComboBox<String> categoryLimitFilter;

    private ComboBox<String> overdueMetricFilter;
    private ComboBox<String> overdueSortFilter;

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
                        ? "Real-time circulation health, category mix, and overdue follow-up."
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
        if (!isStaff) {
            return;
        }

        chartsPane.getChildren().clear();

        VBox trendPanel = createSurfacePanel("Circulation Trends");
        trendPanel.setPrefWidth(760);
        trendPanel.setMinWidth(420);

        trendRangeFilter = filterCombo("Last 90 days",
                List.of("Last 7 days", "Last 30 days", "Last 90 days", "Last 12 months", "All time"));
        trendGroupingFilter = filterCombo("Auto", List.of("Auto", "Daily", "Weekly", "Monthly"));
        trendSeriesFilter = filterCombo("Issues & Returns", List.of("Issues & Returns", "Issues only", "Returns only"));
        trendCategoryFilter = filterCombo("All categories", List.of("All categories"));
        trendStyleFilter = filterCombo("Bar", List.of("Bar", "Line"));

        trendPanel.getChildren().addAll(
                filterRow(
                        filterGroup("Range", trendRangeFilter),
                        filterGroup("Group", trendGroupingFilter),
                        filterGroup("Series", trendSeriesFilter)),
                filterRow(
                        filterGroup("Category", trendCategoryFilter),
                        filterGroup("Style", trendStyleFilter))
        );

        trendChartHolder = new StackPane();
        trendChartHolder.setMinHeight(320);
        trendChartHolder.setPrefHeight(320);
        trendPanel.getChildren().add(trendChartHolder);

        VBox categoryPanel = createSurfacePanel("Category Mix");
        categoryPanel.setPrefWidth(360);
        categoryPanel.setMinWidth(320);

        categoryMetricFilter = filterCombo("Inventory Size",
                List.of("Inventory Size", "Available Copies", "Issued Copies", "Overdue Items"));
        categorySortFilter = filterCombo("Highest first", List.of("Highest first", "A-Z"));
        categoryLimitFilter = filterCombo("Top 7", List.of("Top 5", "Top 7", "Top 10", "All"));

        categoryPanel.getChildren().add(filterRow(
                filterGroup("Measure", categoryMetricFilter),
                filterGroup("Sort", categorySortFilter),
                filterGroup("Limit", categoryLimitFilter)
        ));

        categoryChart = new PieChart();
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);
        categoryChart.setLegendSide(javafx.geometry.Side.RIGHT);
        categoryChart.setPrefHeight(300);
        categoryChart.setMinHeight(300);
        categoryChart.setStyle("-fx-font-size: 11px;");
        categoryPanel.getChildren().add(categoryChart);

        VBox overdueInsightsPanel = createSurfacePanel("Overdue Insights");
        overdueInsightsPanel.setPrefWidth(520);
        overdueInsightsPanel.setMinWidth(360);

        overdueMetricFilter = filterCombo("Outstanding Fine",
                List.of("Outstanding Fine", "Days Overdue", "Books on Loan"));
        overdueSortFilter = filterCombo("Highest first", List.of("Highest first", "Borrower A-Z"));

        overdueInsightsPanel.getChildren().add(filterRow(
                filterGroup("Measure", overdueMetricFilter),
                filterGroup("Sort", overdueSortFilter)
        ));

        overdueChartHolder = new StackPane();
        overdueChartHolder.setMinHeight(300);
        overdueChartHolder.setPrefHeight(300);
        overdueInsightsPanel.getChildren().add(overdueChartHolder);

        chartsPane.getChildren().addAll(trendPanel, categoryPanel, overdueInsightsPanel);

        attachChartListeners();
    }

    private void attachChartListeners() {
        List<ComboBox<String>> controls = List.of(
                trendRangeFilter, trendGroupingFilter, trendSeriesFilter, trendCategoryFilter, trendStyleFilter,
                categoryMetricFilter, categorySortFilter, categoryLimitFilter,
                overdueMetricFilter, overdueSortFilter
        );
        controls.forEach(control -> control.setOnAction(event -> refreshStaffCharts()));
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
                refreshStaffCharts();
            }
            updateRecentPanel();
            updateOverduePanel();
        });
    }

    private void refreshStaffCharts() {
        refreshSelectableCategories();
        updateCategoryChart();
        updateTrendChart();
        updateOverdueInsightsChart();
    }

    private void refreshSelectableCategories() {
        if (trendCategoryFilter == null) {
            return;
        }

        String currentSelection = trendCategoryFilter.getValue();
        List<String> items = new ArrayList<>();
        items.add("All categories");
        BookService.getAllBooks().stream()
                .map(Book::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(items::add);

        trendCategoryFilter.getItems().setAll(items);
        trendCategoryFilter.setValue(items.contains(currentSelection) ? currentSelection : "All categories");
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
        if (categoryChart == null) {
            return;
        }

        Map<String, Double> values = buildCategoryMetricValues();
        Comparator<Map.Entry<String, Double>> comparator = "A-Z".equals(categorySortFilter.getValue())
                ? Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)
                : Map.Entry.<String, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        int limit = switch (categoryLimitFilter.getValue()) {
            case "Top 5" -> 5;
            case "Top 10" -> 10;
            case "All" -> Integer.MAX_VALUE;
            default -> 7;
        };

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        values.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(comparator)
                .limit(limit)
                .forEach(entry -> data.add(new PieChart.Data(
                        entry.getKey() + " (" + AppTheme.formatNumber(Math.round(entry.getValue())) + ")",
                        entry.getValue())));

        categoryChart.setData(data);
    }

    private Map<String, Double> buildCategoryMetricValues() {
        String metric = categoryMetricFilter.getValue();
        List<Book> books = BookService.getAllBooks();

        if ("Overdue Items".equals(metric)) {
            return BookService.getAllOverdueBooks().stream()
                    .collect(Collectors.groupingBy(
                            record -> categoryForRecord(record, books),
                            LinkedHashMap::new,
                            Collectors.summingDouble(IssueRecord::getQuantity)));
        }

        return books.stream().collect(Collectors.groupingBy(
                book -> safeCategory(book.getCategory()),
                LinkedHashMap::new,
                Collectors.summingDouble(book -> switch (metric) {
                    case "Available Copies" -> book.getQuantity();
                    case "Issued Copies" -> BookService.getTotalIssuedQuantityForBook(book.getIsbn());
                    default -> BookService.getOriginalQuantityForBook(book.getIsbn());
                })
        ));
    }

    private void updateTrendChart() {
        if (trendChartHolder == null) {
            return;
        }

        List<IssueRecord> records = BookService.getAllIssueRecords();
        LocalDate end = LocalDate.now();
        LocalDate start = resolveTrendStartDate(records);
        TrendGrouping grouping = resolveTrendGrouping(start, end);
        Map<String, Book> booksByIsbn = BookService.getAllBooks().stream()
                .collect(Collectors.toMap(Book::getIsbn, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<LocalDate, Long> issueCounts = new LinkedHashMap<>();
        Map<LocalDate, Long> returnCounts = new LinkedHashMap<>();

        for (IssueRecord record : records) {
            if (!matchesSelectedCategory(record, booksByIsbn)) {
                continue;
            }

            if (isWithinRange(record.getIssueDate(), start, end)) {
                issueCounts.merge(bucketStart(record.getIssueDate(), grouping), (long) record.getQuantity(), Long::sum);
            }
            if (record.isReturned() && record.getReturnDate() != null && isWithinRange(record.getReturnDate(), start, end)) {
                returnCounts.merge(bucketStart(record.getReturnDate(), grouping), (long) record.getQuantity(), Long::sum);
            }
        }

        List<LocalDate> buckets = buildBuckets(start, end, grouping);
        XYChart<String, Number> chart = createCartesianChart(
                trendStyleFilter.getValue(),
                groupingAxisLabel(grouping),
                "Books");

        List<XYChart.Series<String, Number>> seriesList = new ArrayList<>();
        if (!"Returns only".equals(trendSeriesFilter.getValue())) {
            XYChart.Series<String, Number> issuesSeries = new XYChart.Series<>();
            issuesSeries.setName("Issues");
            buckets.forEach(bucket -> issuesSeries.getData().add(
                    new XYChart.Data<>(bucketLabel(bucket, grouping), issueCounts.getOrDefault(bucket, 0L))));
            seriesList.add(issuesSeries);
        }
        if (!"Issues only".equals(trendSeriesFilter.getValue())) {
            XYChart.Series<String, Number> returnsSeries = new XYChart.Series<>();
            returnsSeries.setName("Returns");
            buckets.forEach(bucket -> returnsSeries.getData().add(
                    new XYChart.Data<>(bucketLabel(bucket, grouping), returnCounts.getOrDefault(bucket, 0L))));
            seriesList.add(returnsSeries);
        }

        chart.getData().setAll(seriesList);
        trendChartHolder.getChildren().setAll(chart);
    }

    private void updateOverdueInsightsChart() {
        if (overdueChartHolder == null) {
            return;
        }

        Map<String, Double> values = new LinkedHashMap<>();
        String metric = overdueMetricFilter.getValue();
        for (IssueRecord record : BookService.getAllOverdueBooks()) {
            String borrowerLabel = borrowerLabel(record.getUserId());
            double value = switch (metric) {
                case "Days Overdue" -> record.getDaysOverdue();
                case "Books on Loan" -> record.getQuantity();
                default -> record.calculateFine();
            };
            values.merge(borrowerLabel, value, Double::sum);
        }

        if (values.isEmpty()) {
            overdueChartHolder.getChildren().setAll(emptyMessage("No overdue data"));
            return;
        }

        Comparator<Map.Entry<String, Double>> comparator = "Borrower A-Z".equals(overdueSortFilter.getValue())
                ? Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)
                : Map.Entry.<String, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

        XYChart<String, Number> chart = createCartesianChart("Bar", "Borrower", metric);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(metric);
        values.entrySet().stream()
                .sorted(comparator)
                .limit(8)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        chart.getData().setAll(series);
        overdueChartHolder.getChildren().setAll(chart);
    }

    private XYChart<String, Number> createCartesianChart(String style, String xLabel, String yLabel) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);

        XYChart<String, Number> chart;
        if ("Line".equals(style)) {
            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setCreateSymbols(true);
            lineChart.setAnimated(false);
            chart = lineChart;
        } else {
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setCategoryGap(16);
            barChart.setBarGap(3);
            barChart.setAnimated(false);
            chart = barChart;
        }

        chart.setLegendVisible(true);
        chart.setPrefHeight(300);
        chart.setMinHeight(300);
        chart.setStyle("-fx-font-size: 11px;");
        return chart;
    }

    private LocalDate resolveTrendStartDate(List<IssueRecord> records) {
        LocalDate today = LocalDate.now();
        return switch (trendRangeFilter.getValue()) {
            case "Last 7 days" -> today.minusDays(6);
            case "Last 30 days" -> today.minusDays(29);
            case "Last 12 months" -> today.minusMonths(11).withDayOfMonth(1);
            case "All time" -> records.stream()
                    .flatMap(record -> {
                        List<LocalDate> dates = new ArrayList<>();
                        dates.add(record.getIssueDate());
                        if (record.getReturnDate() != null) {
                            dates.add(record.getReturnDate());
                        }
                        return dates.stream();
                    })
                    .min(LocalDate::compareTo)
                    .orElse(today.minusDays(29));
            default -> today.minusDays(89);
        };
    }

    private TrendGrouping resolveTrendGrouping(LocalDate start, LocalDate end) {
        return switch (trendGroupingFilter.getValue()) {
            case "Daily" -> TrendGrouping.DAILY;
            case "Weekly" -> TrendGrouping.WEEKLY;
            case "Monthly" -> TrendGrouping.MONTHLY;
            default -> {
                long days = ChronoUnit.DAYS.between(start, end);
                if (days <= 31) {
                    yield TrendGrouping.DAILY;
                }
                if (days <= 180) {
                    yield TrendGrouping.WEEKLY;
                }
                yield TrendGrouping.MONTHLY;
            }
        };
    }

    private List<LocalDate> buildBuckets(LocalDate start, LocalDate end, TrendGrouping grouping) {
        List<LocalDate> buckets = new ArrayList<>();
        LocalDate cursor = bucketStart(start, grouping);
        LocalDate endBucket = bucketStart(end, grouping);

        while (!cursor.isAfter(endBucket)) {
            buckets.add(cursor);
            cursor = switch (grouping) {
                case DAILY -> cursor.plusDays(1);
                case WEEKLY -> cursor.plusWeeks(1);
                case MONTHLY -> cursor.plusMonths(1);
            };
        }

        return buckets;
    }

    private LocalDate bucketStart(LocalDate date, TrendGrouping grouping) {
        return switch (grouping) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    private String bucketLabel(LocalDate bucket, TrendGrouping grouping) {
        return switch (grouping) {
            case DAILY -> bucket.format(DateTimeFormatter.ofPattern("MMM dd"));
            case WEEKLY -> bucket.format(DateTimeFormatter.ofPattern("MMM dd"));
            case MONTHLY -> bucket.format(DateTimeFormatter.ofPattern("MMM yy"));
        };
    }

    private String groupingAxisLabel(TrendGrouping grouping) {
        return switch (grouping) {
            case DAILY -> "Day";
            case WEEKLY -> "Week";
            case MONTHLY -> "Month";
        };
    }

    private boolean isWithinRange(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean matchesSelectedCategory(IssueRecord record, Map<String, Book> booksByIsbn) {
        String selectedCategory = trendCategoryFilter.getValue();
        if (selectedCategory == null || "All categories".equals(selectedCategory)) {
            return true;
        }
        Book book = booksByIsbn.get(record.getIsbn());
        return safeCategory(book != null ? book.getCategory() : null).equalsIgnoreCase(selectedCategory);
    }

    private String categoryForRecord(IssueRecord record, List<Book> books) {
        return books.stream()
                .filter(book -> book.getIsbn().equals(record.getIsbn()))
                .map(Book::getCategory)
                .map(AnalyticsDashboard::safeCategory)
                .findFirst()
                .orElse("Uncategorised");
    }

    private String borrowerLabel(String userId) {
        try {
            User user = UserService.getUserById(userId);
            return user != null ? user.getDisplayName() : userId;
        } catch (Exception ex) {
            return userId;
        }
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
            overduePanel.getChildren().add(emptyMessage("No overdue books"));
            return;
        }

        overdueRecords.forEach(record -> overduePanel.getChildren().add(createOverdueRow(record)));
    }

    private HBox createOverdueRow(IssueRecord record) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-border-color:" + dividerColor() + "; -fx-border-width: 0 0 1 0;");

        VBox textBlock = new VBox(3);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label titleLabel = new Label(record.getBookTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill:" + textPrimary() + ";");
        titleLabel.setWrapText(true);

        String subText = isStaff
                ? record.getUserId() + " - " + record.getDaysOverdue() + " day(s) overdue"
                : record.getDaysOverdue() + " day(s) overdue";
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
        subLabel.setWrapText(true);

        textBlock.getChildren().addAll(titleLabel, subLabel);

        Label metaLabel = new Label(AppTheme.formatCurrency(record.calculateFine()));
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill:" + textSoft() + ";");

        if (isStaff) {
            Button contactBtn = new Button();
            contactBtn.setGraphic(AppTheme.createIcon(AppTheme.ICON_USER, 14));
            contactBtn.getStyleClass().addAll("app-button", "btn-ghost");
            contactBtn.setTooltip(new Tooltip("View borrower contact"));
            contactBtn.setOnAction(event -> showContactSummary(record.getUserId()));

            Button reminderBtn = new Button();
            reminderBtn.setGraphic(AppTheme.createIcon(AppTheme.ICON_MAIL, 14));
            reminderBtn.getStyleClass().addAll("app-button", "btn-ghost");
            reminderBtn.setTooltip(new Tooltip("Send overdue reminder"));
            reminderBtn.setOnAction(event -> sendDashboardReminder(record));

            HBox actions = new HBox(4, contactBtn, reminderBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(textBlock, metaLabel, actions);
        } else {
            row.getChildren().addAll(textBlock, metaLabel);
        }

        return row;
    }

    private void sendDashboardReminder(IssueRecord record) {
        new Thread(() -> {
            try {
                User user = UserService.getUserById(record.getUserId());
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new IllegalStateException("Borrower does not have an email address on file.");
                }

                ReminderService.sendOverdueReminder(user, List.of(record));
                Platform.runLater(() -> showInfoAlert(
                        "Reminder Sent",
                        "Reminder sent to " + user.getEmail(),
                        "An overdue reminder was emailed successfully."));
            } catch (Exception ex) {
                Platform.runLater(() -> showInfoAlert(
                        "Reminder Failed",
                        "Could not send the overdue reminder",
                        ex.getMessage()));
            }
        }, "dashboard-reminder").start();
    }

    private void showContactSummary(String userId) {
        try {
            User user = UserService.getUserById(userId);
            showInfoAlert(
                    "Borrower Contact",
                    user.getDisplayName(),
                    "Email: " + valueOrPlaceholder(user.getEmail()) +
                            "\nMobile: " + valueOrPlaceholder(user.getContactNumber()));
        } catch (Exception ex) {
            showInfoAlert("Borrower Contact", "Could not load borrower details", ex.getMessage());
        }
    }

    private void showInfoAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        AppTheme.applyTheme(alert.getDialogPane());
        alert.showAndWait();
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

    private ComboBox<String> filterCombo(String initialValue, List<String> items) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(items));
        comboBox.setValue(initialValue);
        comboBox.setVisibleRowCount(Math.min(8, items.size()));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private VBox filterGroup(String label, ComboBox<String> comboBox) {
        Label filterLabel = new Label(label);
        filterLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill:" + textSoft() + ";");

        VBox box = new VBox(6, filterLabel, comboBox);
        box.setMinWidth(140);
        box.setPrefWidth(160);
        return box;
    }

    private FlowPane filterRow(Node... nodes) {
        FlowPane row = new FlowPane();
        row.setHgap(10);
        row.setVgap(10);
        row.getChildren().addAll(nodes);
        return row;
    }

    private void loadData() {
        Map<String, Object> stats = BookService.getLibraryStatistics();
        long staffCount = UserService.getAllUsers().stream().filter(User::isStaff).count();
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

    private static String safeCategory(String category) {
        return category == null || category.isBlank() ? "Uncategorised" : category.trim();
    }

    private static String valueOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "(not provided)" : value;
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

package com.example.application.ui;

import com.example.application.ToastDisplay;
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
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnalyticsDashboard extends BorderPane {

    private enum TrendGrouping {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private final Runnable onRefresh;
    private final String currentUser;
    private final boolean isStaff;
    private final ToastDisplay toastDisplay;

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
    private ComboBox<String> overdueLimitFilter;
    private ComboBox<String> overdueListSortFilter;

    private boolean isUpdatingCategories = false;

    private static final String[] CATEGORY_CHART_COLORS = {
            "#0D9488", "#3B82F6", "#8B5CF6", "#F59E0B", "#06B6D4", "#16A34A",
            "#EC4899", "#EF4444", "#84CC16", "#F97316", "#14B8A6", "#6366F1",
            "#A855F7", "#D97706", "#10B981", "#0EA5E9", "#7C3AED", "#F43F5E",
            "#FB923C", "#FBBF24", "#A3E635", "#34D399", "#22D3EE", "#60A5FA",
            "#C084FC", "#F87171", "#FCA5A5", "#FED7AA", "#FECDC3", "#FECACA",
            "#FBCFE8", "#F5D0FC", "#E9D5FF", "#DDD6FE", "#C7D2FE", "#BFDBFE",
            "#BAE6FD", "#B4E7FF", "#BFDBFE", "#99F6E4", "#86EFAC", "#BBF7D0"
    };

    public AnalyticsDashboard(Runnable onRefresh, String currentUser, boolean isStaff, ToastDisplay toastDisplay) {
        this.onRefresh = onRefresh;
        this.currentUser = currentUser;
        this.isStaff = isStaff;
        this.toastDisplay = toastDisplay;
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
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24, 24, 48, 24));
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
        widthProperty().addListener((obs, oldValue, newValue) -> updateResponsiveSections());
        loadData();
        Platform.runLater(this::updateResponsiveSections);
    }

    private FlowPane createWrappingSection(double gap) {
        FlowPane section = new FlowPane();
        section.setHgap(gap);
        section.setVgap(gap);
        section.setAlignment(Pos.TOP_LEFT);
        // Do NOT bind prefWrapLengthProperty here — applyResponsiveWidths() calls
        // setPrefWrapLength() at runtime, which throws if the property is bound:
        //   "FlowPane.prefWrapLength: A bound value cannot be set."
        // The widthProperty listener + Platform.runLater call in initUI() handles this.
        return section;
    }

    private void buildCharts() {
        if (!isStaff) {
            return;
        }

        chartsPane.getChildren().clear();

        VBox trendPanel = createSurfacePanel("Circulation Trends");
        trendPanel.setMinWidth(320);
        trendPanel.setMaxWidth(Double.MAX_VALUE);

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
        categoryPanel.setMinWidth(320);
        categoryPanel.setMaxWidth(Double.MAX_VALUE);

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
        categoryChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        categoryChart.setPrefHeight(360);
        categoryChart.setMinHeight(320);
        categoryChart.setStyle("-fx-font-size: 11px;");
        categoryPanel.getChildren().add(categoryChart);

        VBox overdueInsightsPanel = createSurfacePanel("Overdue Insights");
        overdueInsightsPanel.setMinWidth(300);
        overdueInsightsPanel.setMaxWidth(Double.MAX_VALUE);

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
        AppTheme.staggeredEntrance(chartsPane.getChildren(), 35, 45);

        attachChartListeners();
    }

    private void attachChartListeners() {
        List<ComboBox<String>> trendControls = List.of(
                trendRangeFilter, trendGroupingFilter, trendSeriesFilter, trendCategoryFilter, trendStyleFilter
        );
        trendControls.forEach(control -> control.setOnAction(event -> {
            if (!isUpdatingCategories) {
                updateTrendChart();
            }
        }));

        List<ComboBox<String>> categoryControls = List.of(
                categoryMetricFilter, categorySortFilter, categoryLimitFilter
        );
        categoryControls.forEach(control -> control.setOnAction(event -> updateCategoryChart()));

        List<ComboBox<String>> overdueControls = List.of(overdueMetricFilter, overdueSortFilter);
        overdueControls.forEach(control -> control.setOnAction(event -> updateOverdueInsightsChart()));

        // Add listeners for overdue list filters
        if (overdueLimitFilter != null) {
            overdueLimitFilter.setOnAction(event -> updateOverduePanel());
        }
        if (overdueListSortFilter != null) {
            overdueListSortFilter.setOnAction(event -> updateOverduePanel());
        }
    }

    private void buildBottomPanels() {
        bottomPane.getChildren().clear();

        recentPanel = createSurfacePanel(isStaff ? "Recent Issues" : "My Active Books");
        recentPanel.setPrefWidth(USE_COMPUTED_SIZE);
        recentPanel.setMaxWidth(Double.MAX_VALUE);
        recentPanel.setMinWidth(320);

        overduePanel = createSurfacePanel(isStaff ? "Top Overdue" : "My Overdue Books");
        overduePanel.setPrefWidth(USE_COMPUTED_SIZE);
        overduePanel.setMaxWidth(Double.MAX_VALUE);
        overduePanel.setMinWidth(320);

        // Add filter controls to overdue panel
        if (isStaff) {
            overdueLimitFilter = filterCombo("Top 6", List.of("Top 5", "Top 10", "Top 15", "All"));
            overdueListSortFilter = filterCombo("Days Overdue (High)",
                    List.of("Days Overdue (High)", "Days Overdue (Low)", "Fine Amount (High)", "Fine Amount (Low)", "Borrower (A-Z)"));

            FlowPane overdueControls = filterRow(
                    filterGroup("Limit", overdueLimitFilter),
                    filterGroup("Sort", overdueListSortFilter)
            );
            overduePanel.getChildren().add(overdueControls);
        }

        bottomPane.getChildren().addAll(recentPanel, overduePanel);
        AppTheme.staggeredEntrance(bottomPane.getChildren(), 40, 55);
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
            updateResponsiveSections();
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

        try {
            isUpdatingCategories = true;
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
        } finally {
            isUpdatingCategories = false;
        }
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

        AppTheme.staggeredEntrance(statsPane.getChildren(), 25, 35);
        Platform.runLater(this::updateResponsiveSections);
    }

    private VBox statCard(String iconPath, String label, String value, String subText,
                          String accentColor, Runnable onClick) {
        VBox card = new VBox(12);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(18));
        // Set a sensible initial width; responsive layout will override later
        card.setMinWidth(200);
        card.setPrefWidth(240);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setScaleX(1.0);
        card.setScaleY(1.0);

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

        // Animate count-up for pure numeric values
        try {
            int numericValue = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (!value.contains(".") && !value.startsWith("$") && !value.startsWith("£")) {
                valueText.setText("0");
                AppTheme.animateCount(valueText, numericValue, "", "");
            }
        } catch (NumberFormatException ignored) { }

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

        for (int index = 0; index < data.size(); index++) {
            final int colorIndex = index;
            data.get(index).nodeProperty().addListener((obs, oldNode, newNode) -> applyPieColor(newNode, colorIndex));
        }

        categoryChart.setData(data);
        Platform.runLater(() -> {
            applyCategoryChartPalette();
            applyPieLegendColors();
            refreshLayout();
        });
        // Second pass after scene has rendered nodes
        javafx.animation.PauseTransition colorDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(120));
        colorDelay.setOnFinished(e -> Platform.runLater(() -> {
            applyCategoryChartPalette();
            applyPieLegendColors();
        }));
        colorDelay.play();
    }

    private void applyCategoryChartPalette() {
        ObservableList<PieChart.Data> pieData = categoryChart.getData();
        for (int i = 0; i < pieData.size(); i++) {
            applyPieColor(pieData.get(i).getNode(), i);
        }
    }

    private void applyPieLegendColors() {
        if (categoryChart == null) return;
        java.util.Set<Node> symbols = categoryChart.lookupAll(".chart-legend-item-symbol");
        int i = 0;
        for (Node sym : symbols) {
            sym.setStyle("-fx-background-color: " + CATEGORY_CHART_COLORS[i % CATEGORY_CHART_COLORS.length]
                    + "; -fx-background-radius: 3px;");
            i++;
        }
    }

    private void applyPieColor(Node node, int index) {
        if (node != null) {
            node.setStyle("-fx-pie-color: " + CATEGORY_CHART_COLORS[index % CATEGORY_CHART_COLORS.length] + ";");
        }
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
                ? BookService.getAllOverdueBooks()
                : BookService.getUserOverdueBooks(currentUser);

        if (overdueRecords.isEmpty()) {
            overduePanel.getChildren().add(emptyMessage("No overdue books"));
            return;
        }

        // Apply sorting based on the selected filter
        if (isStaff && overdueListSortFilter != null) {
            String sortOption = overdueListSortFilter.getValue();
            if ("Days Overdue (Low)".equals(sortOption)) {
                overdueRecords.sort(Comparator.comparingLong(IssueRecord::getDaysOverdue));
            } else if ("Fine Amount (High)".equals(sortOption)) {
                overdueRecords.sort((a, b) -> Double.compare(b.calculateFine(), a.calculateFine()));
            } else if ("Fine Amount (Low)".equals(sortOption)) {
                overdueRecords.sort((a, b) -> Double.compare(a.calculateFine(), b.calculateFine()));
            } else if ("Borrower (A-Z)".equals(sortOption)) {
                overdueRecords.sort(Comparator.comparing(IssueRecord::getUserId));
            } else {
                // Default: Days Overdue (High)
                overdueRecords.sort(Comparator.comparingLong(IssueRecord::getDaysOverdue).reversed());
            }
        } else {
            // Default sorting
            overdueRecords.sort(Comparator.comparingLong(IssueRecord::getDaysOverdue).reversed());
        }

        // Apply limit based on the selected filter
        long limit = 6;
        if (isStaff && overdueLimitFilter != null) {
            String limitOption = overdueLimitFilter.getValue();
            if ("Top 5".equals(limitOption)) {
                limit = 5;
            } else if ("Top 10".equals(limitOption)) {
                limit = 10;
            } else if ("Top 15".equals(limitOption)) {
                limit = 15;
            } else if ("All".equals(limitOption)) {
                limit = Long.MAX_VALUE;
            }
        }

        overdueRecords.stream()
                .limit(limit)
                .forEach(record -> overduePanel.getChildren().add(createOverdueRow(record)));
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
            contactBtn.setTooltip(AppTheme.createTooltip("View borrower contact"));
            contactBtn.setOnAction(event -> showContactSummary(record.getUserId()));

            Button reminderBtn = new Button();
            reminderBtn.setGraphic(AppTheme.createIcon(AppTheme.ICON_MAIL, 14));
            reminderBtn.getStyleClass().addAll("app-button", "btn-ghost");
            reminderBtn.setTooltip(AppTheme.createTooltip("Send overdue reminder"));
            reminderBtn.setOnAction(event -> sendDashboardReminder(record, reminderBtn));

            HBox actions = new HBox(4, contactBtn, reminderBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(textBlock, metaLabel, actions);
        } else {
            row.getChildren().addAll(textBlock, metaLabel);
        }

        return row;
    }

    private void sendDashboardReminder(IssueRecord record, Button triggerButton) {
        if (toastDisplay != null) {
            toastDisplay.showInfo("Sending reminder email…");
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
                    if (toastDisplay != null) {
                        toastDisplay.showSuccess("Reminder sent to " + user.getEmail());
                    } else {
                        showInfoAlert(
                                "Reminder Sent",
                                "Reminder sent to " + user.getEmail(),
                                "An overdue reminder was emailed successfully.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    triggerButton.setDisable(false);
                    if (toastDisplay != null) {
                        toastDisplay.showError("Reminder failed: " + ReminderService.toUserMessage(ex));
                    } else {
                        showInfoAlert(
                                "Reminder Failed",
                                "Could not send the overdue reminder",
                                ReminderService.toUserMessage(ex));
                    }
                });
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

    private void updateResponsiveSections() {
        double availableWidth = Math.max(360, getWidth() - 112);

        applyResponsiveWidths(statsPane, availableWidth, 3, 2, 1, 220, Double.MAX_VALUE);
        applyResponsiveWidths(bottomPane, availableWidth,
                availableWidth >= 1080 ? 2 : 1,
                availableWidth >= 1080 ? 2 : 1,
                1, 320, Double.MAX_VALUE);
        applyResponsiveWidths(chartsPane, availableWidth, 1, 1, 1, 320, Double.MAX_VALUE);

        if (trendChartHolder != null) {
            double chartHeight = availableWidth < 760 ? 320 : 360;
            trendChartHolder.setMinHeight(chartHeight);
            trendChartHolder.setPrefHeight(chartHeight);
        }
        if (overdueChartHolder != null) {
            double chartHeight = availableWidth < 760 ? 300 : 340;
            overdueChartHolder.setMinHeight(chartHeight);
            overdueChartHolder.setPrefHeight(chartHeight);
        }
        if (categoryChart != null) {
            categoryChart.setMinHeight(availableWidth < 760 ? 320 : 360);
            categoryChart.setPrefHeight(availableWidth < 760 ? 320 : 380);
        }
    }

    public void refreshLayout() {
        updateResponsiveSections();
    }

    private void applyResponsiveWidths(FlowPane pane, double availableWidth, int wideColumns,
                                       int mediumColumns, int narrowColumns,
                                       double minWidth, double maxWidth) {
        if (pane == null || pane.getChildren().isEmpty()) {
            return;
        }

        int columns;
        if (availableWidth >= 1280) {
            columns = wideColumns;
        } else if (availableWidth >= 860) {
            columns = mediumColumns;
        } else {
            columns = narrowColumns;
        }
        columns = Math.max(1, columns);

        double gap = pane.getHgap();
        double targetWidth = (availableWidth - ((columns - 1) * gap)) / columns;
        targetWidth = Math.max(minWidth, targetWidth);
        if (Double.isFinite(maxWidth)) {
            targetWidth = Math.min(maxWidth, targetWidth);
        }

        pane.setPrefWrapLength(availableWidth);
        for (Node child : pane.getChildren()) {
            if (child instanceof Region region) {
                region.setPrefWidth(targetWidth);
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
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
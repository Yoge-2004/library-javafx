package com.example.application;

import com.example.application.ui.*;
import com.example.entities.*;
import com.example.entities.BooksDB.IssueRecord;
import com.example.services.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

public class LibraryApp extends Application implements ToastDisplay {

    private static final Logger LOG = Logger.getLogger(LibraryApp.class.getName());

    private Stage     primaryStage;
    private StackPane rootStack;
    private StackPane contentArea;
    private VBox      sidebar;
    private Label     statusLabel;
    private ProgressIndicator loadingIndicator;
    private Label     userNameLabel;
    private Label     userRoleLabel;
    private Button    activeNavBtn;

    private String   currentUser;
    private UserRole currentUserRole = UserRole.USER;
    private Timeline autoRefreshTimer;
    private int      toastSlot = 0;

    private final ObservableList<Book>          booksList    = FXCollections.observableArrayList();
    private final ObservableList<IssueRecord>   issuesList   = FXCollections.observableArrayList();
    private final ObservableList<BorrowRequest> requestsList = FXCollections.observableArrayList();

    private AnalyticsDashboard analyticsDashboard;
    private CatalogView        catalogView;
    private CirculationView    circulationView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Library OS");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        rootStack = new StackPane();

        double w = Math.min(1400, Screen.getPrimary().getVisualBounds().getWidth() * 0.9);
        double h = Math.min(900,  Screen.getPrimary().getVisualBounds().getHeight() * 0.9);

        Scene scene = AppTheme.createScene(rootStack, w, h);
        stage.setScene(scene);

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        if (cfg.isDarkMode()) applyDarkMode(true);

        if (!cfg.isInitialSetupDone()) {
            showSetupWizard();
        } else {
            showLoginScreen();
        }

        stage.show();
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> shutdown());
    }

    // --- First-run wizard ---

    private void showSetupWizard() {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Library OS — First-time Setup");
        dlg.initOwner(primaryStage);
        dlg.initModality(Modality.APPLICATION_MODAL);

        DialogPane dp = dlg.getDialogPane();
        AppTheme.applyTheme(dp);
        dp.setPrefWidth(520);

        VBox root = new VBox(0);

        VBox hero = new VBox(10);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(32, 32, 24, 32));
        hero.setStyle("-fx-background-color:#0F172A;");
        Label logo = new Label("Library OS Setup");
        logo.setStyle("-fx-font-size:24px; -fx-font-weight:800; -fx-text-fill:white;");
        Label sub = new Label("Configure your library profile to get started");
        sub.setStyle("-fx-font-size:14px; -fx-text-fill:#94A3B8;");
        hero.getChildren().addAll(logo, sub);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24, 32, 16, 32));

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        TextField libNameField  = wField(cfg.getLibraryName(),  "e.g. City Public Library");
        TextField branchField   = wField(cfg.getBranchName(),   "e.g. Main Branch");
        TextField dataDirField  = wField(cfg.getDataDirectory(),   "data");
        TextField exportField   = wField(cfg.getExportDirectory(),  "exports");

        Button browseData = browseBtn(dataDirField, "Choose data folder");
        Button browseExp  = browseBtn(exportField,  "Choose export folder");

        HBox dataRow   = new HBox(8, dataDirField, browseData);  HBox.setHgrow(dataDirField, Priority.ALWAYS);
        HBox exportRow = new HBox(8, exportField,  browseExp);   HBox.setHgrow(exportField,  Priority.ALWAYS);

        form.getChildren().addAll(
                wRow("Library Name", libNameField),
                wRow("Branch Name",  branchField),
                wRow("Data Folder",  dataRow),
                wRow("Export Folder", exportRow)
        );

        root.getChildren().addAll(hero, form);
        dp.setContent(root);

        ButtonType doneBt = new ButtonType("Continue \u2192", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().add(doneBt);
        Button ok = (Button) dp.lookupButton(doneBt);
        ok.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:700; -fx-font-size:14px; " +
                "-fx-background-radius:10px; -fx-padding:10 24;");

        dlg.setResultConverter(bt -> bt == doneBt);
        dlg.showAndWait().ifPresent(ok2 -> {
            if (ok2) {
                cfg.setLibraryName(libNameField.getText());
                cfg.setBranchName(branchField.getText());
                cfg.setDataDirectory(dataDirField.getText());
                cfg.setExportDirectory(exportField.getText());
                cfg.markSetupDone();
                try { AppConfigurationService.updateConfiguration(cfg); }
                catch (IOException ex) { LOG.warning("Config save: " + ex.getMessage()); }
            }
        });
        showLoginScreen();
    }

    private TextField wField(String val, String prompt) {
        TextField f = new TextField(val);
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:10 14; -fx-font-size:14px;");
        return f;
    }
    private Button browseBtn(TextField target, String title) {
        Button b = new Button("Browse\u2026");
        b.setStyle("-fx-background-color:#E2E8F0; -fx-background-radius:8px; " +
                "-fx-border-radius:8px; -fx-cursor:hand; -fx-padding:8 14; -fx-font-weight:600;");
        b.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle(title);
            java.io.File dir = dc.showDialog(primaryStage);
            if (dir != null) target.setText(dir.getAbsolutePath());
        });
        return b;
    }
    private VBox wRow(String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#374151;");
        return new VBox(6, l, field);
    }

    // --- Login ---

    private void showLoginScreen() {
        stopAutoRefresh();
        analyticsDashboard = null; catalogView = null; circulationView = null;
        LoginView lv = new LoginView(this::handleLoginSuccess, this::showRegistrationDialog);
        lv.setOpacity(0);
        rootStack.getChildren().setAll(lv);
        FadeTransition _ft = new FadeTransition(Duration.millis(350), lv); _ft.setToValue(1); _ft.play();
    }

    private void showRegistrationDialog() {
        RegistrationDialog.show(primaryStage, !UserService.hasRegisteredUsers())
                .ifPresent(req -> {
                    try {
                        // Check username uniqueness before creating
                        if (UserService.userExists(req.username())) {
                            showError("Username \"" + req.username() + "\" is already taken. Please choose another.");
                            return;
                        }
                        UserService.createUser(req.username(), req.password(), req.role());
                        if (req.pendingApproval()) {
                            User created = UserService.getUserById(req.username());
                            created.setActive(false);
                            UserService.updateUser(created);
                            showSuccess("Librarian request submitted. An admin must approve it before you can log in.");
                        } else {
                            showSuccess("Account created! Please sign in.");
                        }
                        UserService.persistDatabase();
                    } catch (Exception ex) {
                        showError("Registration failed: " + ex.getMessage());
                    }
                });
    }

    // --- Main layout ---

    private void handleLoginSuccess(String username) {
        this.currentUser     = username;
        this.currentUserRole = UserService.getUserRole(username);
        if (currentUserRole == null) currentUserRole = UserRole.USER;

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("content-area");

        sidebar     = buildSidebar();
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24, 24, 24, 24));
        contentArea.getStyleClass().add("content-area");

        layout.setLeft(sidebar);
        layout.setTop(buildHeader());
        layout.setCenter(contentArea);
        layout.setBottom(buildStatusBar());

        rootStack.getChildren().setAll(layout);
        primaryStage.setMaximized(true);

        refreshAllData();
        startAutoRefresh();
        Platform.runLater(this::navigateToDashboard);
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(4);
        sb.setPrefWidth(248); sb.setMinWidth(248); sb.setMaxWidth(248);
        sb.getStyleClass().add("sidebar");

        HBox logoBox = new HBox(4);
        logoBox.setPadding(new Insets(0, 0, 28, 10));
        Label lib = new Label("LIBRARY"); lib.getStyleClass().add("sidebar-logo");
        Label os  = new Label("OS");      os.getStyleClass().addAll("sidebar-logo","sidebar-logo-accent");
        logoBox.getChildren().addAll(lib, os);

        Label navHdr = new Label("NAVIGATION"); navHdr.getStyleClass().add("sidebar-section-label");
        Button dash = navBtn("\uD83D\uDCCA  Dashboard",   AppTheme.ICON_DASHBOARD, this::navigateToDashboard);
        Button cat  = navBtn("\uD83D\uDCDA  Catalog",     AppTheme.ICON_LIBRARY,   this::navigateToCatalog);
        Button circ = navBtn("\uD83D\uDD04  Circulation", AppTheme.ICON_SYNC,      this::navigateToCirculation);
        VBox nav = new VBox(4, navHdr, dash, cat, circ);

        VBox mgmt = new VBox(4);
        if (currentUserRole.isStaff()) {
            Label mgmtHdr = new Label("MANAGEMENT"); mgmtHdr.getStyleClass().add("sidebar-section-label");
            Button users = navBtn("\uD83D\uDC65  Users",    AppTheme.ICON_USER,     this::showUserManagement);
            Button sett  = navBtn("\u2699  Settings", AppTheme.ICON_SETTINGS, this::showSettings);
            mgmt.getChildren().addAll(mgmtHdr, users, sett);
        }

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        User u = UserService.getUserById(currentUser);
        userNameLabel = new Label(u != null ? u.getFullName() : currentUser);
        userNameLabel.getStyleClass().add("sidebar-profile-name");
        userRoleLabel = new Label(currentUserRole.getDisplayName());
        userRoleLabel.getStyleClass().add("sidebar-profile-role");
        VBox profile = new VBox(3, userNameLabel, userRoleLabel);
        profile.getStyleClass().add("sidebar-profile");

        sb.getChildren().addAll(logoBox, nav, mgmt, spacer, profile);
        return sb;
    }

    private Button navBtn(String text, String icon, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("sidebar-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> { setActiveNav(b); action.run(); });
        return b;
    }
    private void setActiveNav(Button btn) {
        if (activeNavBtn != null) activeNavBtn.getStyleClass().remove("active");
        activeNavBtn = btn;
        btn.getStyleClass().add("active");
    }

    private HBox buildHeader() {
        HBox h = new HBox(10);
        h.setPadding(new Insets(14, 20, 14, 20));
        h.getStyleClass().add("app-header");
        h.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Dashboard"); title.getStyleClass().add("header-title");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = AppTheme.createIconButton(AppTheme.ICON_REFRESH, "Refresh", AppTheme.ButtonStyle.GHOST);
        refreshBtn.setOnAction(e -> {
            title.setText("Refreshing\u2026");
            refreshAllData();
            PauseTransition p = new PauseTransition(Duration.millis(900));
            p.setOnFinished(ev -> title.setText("Dashboard"));
            p.play();
        });

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        Button themeBtn = new Button(cfg.isDarkMode() ? "\uD83C\uDF19" : "\u2600");
        themeBtn.getStyleClass().add("theme-toggle-btn");
        // Inline fallback style - CSS theme-toggle-btn handles dark mode visibility
        themeBtn.setStyle("-fx-font-size:16px; -fx-cursor:hand;");
        themeBtn.setOnAction(e -> {
            cfg.toggleDarkMode();
            applyDarkMode(cfg.isDarkMode());
            themeBtn.setText(cfg.isDarkMode() ? "\uD83C\uDF19" : "\u2600");
            try { AppConfigurationService.updateConfiguration(cfg); } catch (IOException ignored) {}
        });

        Button logoutBtn = AppTheme.createIconButton(AppTheme.ICON_LOGOUT, "Sign out", AppTheme.ButtonStyle.GHOST);
        logoutBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Sign out of Library OS?", ButtonType.YES, ButtonType.NO);
            a.setTitle("Sign Out"); a.initOwner(primaryStage);
            a.showAndWait().filter(bt -> bt == ButtonType.YES)
                    .ifPresent(bt -> showLoginScreen());
        });

        h.getChildren().addAll(title, spacer, refreshBtn, themeBtn, logoutBtn);
        return h;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        loadingIndicator = new ProgressIndicator(); loadingIndicator.setMaxSize(14, 14); loadingIndicator.setVisible(false);
        statusLabel = new Label("Ready"); statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        Label lib = new Label(cfg.getLibraryName() + " \u00B7 " + cfg.getBranchName());
        lib.setStyle("-fx-font-size:11px; -fx-text-fill:#94A3B8;");
        Label ver = new Label("Library OS v3.1"); ver.setStyle("-fx-font-size:11px; -fx-text-fill:#94A3B8;");

        bar.getChildren().addAll(loadingIndicator, statusLabel, sp, lib, new Label("  |  "), ver);
        return bar;
    }

    // --- Navigation ---

    private void navigateToDashboard() {
        if (analyticsDashboard == null) {
            analyticsDashboard = new AnalyticsDashboard(
                    this::refreshAllData, currentUser, currentUserRole.isStaff());
            analyticsDashboard.setNavigationCallbacks(
                    this::navigateToCirculation, this::navigateToCatalog);
        }
        showView(analyticsDashboard);
    }
    private void navigateToCatalog() {
        if (catalogView == null)
            catalogView = new CatalogView(booksList, currentUserRole.isStaff(),
                    currentUser, this::refreshAllData, this);
        showView(catalogView);
    }
    private void navigateToCirculation() {
        if (circulationView == null)
            circulationView = new CirculationView(issuesList, requestsList,
                    currentUserRole.isStaff(), currentUser, this::refreshAllData, this);
        showView(circulationView);
    }
    private void showView(Region view) {
        contentArea.getChildren().setAll(view);
        AppTheme.slideUp(view, 0);
    }

    // --- Settings dialogs ---

    private void showUserManagement() {
        UserAccountDialogs.showUserManagement(primaryStage, currentUser);
        refreshAllData();
    }

    private void showSettings() {
        SettingsDialog.show(primaryStage, currentUserRole, new SettingsDialog.Actions() {
            @Override public void openProfile() {
                if (UserAccountDialogs.showProfileEditor(primaryStage, currentUser)) {
                    showSuccess("Profile updated.");
                    User u = UserService.getUserById(currentUser);
                    if (u != null && userNameLabel != null) userNameLabel.setText(u.getFullName());
                }
            }
            @Override public void openPassword() {
                if (UserAccountDialogs.showPasswordEditor(primaryStage, currentUser))
                    showSuccess("Password changed.");
            }
            @Override public void openUserManagement() { showUserManagement(); }
            @Override public void openLibraryConfiguration() { showLibraryConfig(); }
            @Override public void openDataManagement() { showDataManagement(); }
            @Override public void openAnalytics() { navigateToDashboard(); }
        });
    }

    private void showLibraryConfig() {
        try {
            AppConfiguration cfg = AppConfigurationService.getConfiguration();
            LibraryConfigurationDialog.show(primaryStage, cfg,
                            BookService.getMaxBorrowLimit(),
                            BookService.getLoanPeriodDays(),
                            BookService.getFinePerDay())
                    .ifPresent(data -> {
                        try {
                            BookService.updateLibraryConfiguration(
                                    data.maxBorrowLimit(), data.loanDays(), data.finePerDay());
                            cfg.setExportDirectory(data.exportDirectory());
                            cfg.setCurrencySymbol(data.currencySymbol());
                            cfg.setCurrencyCode(data.currencyCode());
                            cfg.setSmtpHost(data.smtpHost()); cfg.setSmtpPort(data.smtpPort());
                            cfg.setSmtpUsername(data.smtpUsername()); cfg.setSmtpPassword(data.smtpPassword());
                            cfg.setFromAddress(data.fromAddress());
                            cfg.setSmtpAuth(data.smtpAuth()); cfg.setStartTlsEnabled(data.startTlsEnabled());
                            cfg.setLibraryName(data.libraryName());
                            cfg.setBranchName(data.branchName());
                            AppConfigurationService.updateConfiguration(cfg);
                            showSuccess("Configuration saved.");
                        } catch (Exception ex) { showError("Save failed: " + ex.getMessage()); }
                    });
        } catch (Exception ex) { showError("Could not load config: " + ex.getMessage()); }
    }

    private void showDataManagement() {
        try {
            Map<String,Object> s = BookService.getLibraryStatistics();
            AppConfiguration cfg = AppConfigurationService.getConfiguration();
            DataManagementDialog.show(primaryStage, new DataManagementDialog.Snapshot(
                    n(s,"totalBooks"), n(s,"totalCopies"), n(s,"availableCopies"),
                    n(s,"issuedCopies"), n(s,"overdueBooks"), UserService.getAllUsers().size(),
                    n(s,"pendingRequests"),
                    ((Number) s.getOrDefault("totalFines", 0.0)).doubleValue(),
                    cfg.getExportDirectory(), cfg.isEmailConfigured()));
        } catch (Exception ex) { showError("Data management error: " + ex.getMessage()); }
    }

    private static int n(Map<String,Object> m, String k) {
        return ((Number) m.getOrDefault(k, 0)).intValue();
    }

    // --- Data refresh ---

    private void refreshAllData() {
        if (loadingIndicator != null) { loadingIndicator.setVisible(true); statusLabel.setText("Syncing\u2026"); }
        CompletableFuture.supplyAsync(() -> {
            Map<String,Object> r = new HashMap<>();
            r.put("books",    BookService.getAllBooks());
            r.put("issues",   currentUserRole.isStaff()
                    ? BookService.getAllActiveIssueRecords()
                    : BookService.getUserActiveIssueRecords(currentUser));
            r.put("requests", currentUserRole.isStaff()
                    ? BookService.getAllBorrowRequests()
                    : BookService.getBorrowRequestsForUser(currentUser));
            r.put("stats",    BookService.getLibraryStatistics());
            return r;
        }).thenAcceptAsync(data -> Platform.runLater(() -> {
            @SuppressWarnings("unchecked") List<Book>          books = (List<Book>)          data.get("books");
            @SuppressWarnings("unchecked") List<IssueRecord>   iss   = (List<IssueRecord>)   data.get("issues");
            @SuppressWarnings("unchecked") List<BorrowRequest> reqs  = (List<BorrowRequest>) data.get("requests");
            booksList.setAll(books); issuesList.setAll(iss); requestsList.setAll(reqs);

            @SuppressWarnings("unchecked") Map<String,Object> stats = (Map<String,Object>) data.get("stats");
            if (analyticsDashboard != null) {
                long sc = UserService.getAllUsers().stream().filter(User::isStaff).count();
                analyticsDashboard.update(stats, UserService.getAllUsers().size(), (int) sc);
            }
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
                statusLabel.setText("Updated " + java.time.LocalTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (loadingIndicator != null) { loadingIndicator.setVisible(false); statusLabel.setText("Sync failed"); }
                showError("Refresh failed: " + ex.getMessage());
            });
            return null;
        });
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshAllData()));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }
    private void stopAutoRefresh() { if (autoRefreshTimer != null) autoRefreshTimer.stop(); }

    // --- Toast (compact) ---

    @Override public void showSuccess(String m) { toast(m, "toast-success", "\u2713"); }
    @Override public void showError  (String m) { toast(m, "toast-error",   "\u2715"); }
    @Override public void showInfo   (String m) { toast(m, "toast-info",    "\u2139"); }
    @Override public void showWarning(String m) { toast(m, "toast-warning", "\u26A0"); }

    private void toast(String message, String style, String icon) {
        Platform.runLater(() -> {
            HBox t = new HBox(8);
            t.setAlignment(Pos.CENTER_LEFT);
            t.getStyleClass().addAll("toast-notification", style);
            t.setMaxWidth(340);

            Label ico = new Label(icon); ico.setStyle("-fx-font-size:14px;");
            Label msg = new Label(message);
            msg.setStyle("-fx-font-size:13px; -fx-font-weight:500;");
            msg.setWrapText(true); msg.setMaxWidth(280);
            t.getChildren().addAll(ico, msg);

            int slot = toastSlot++ % 5;
            StackPane.setAlignment(t, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(t, new Insets(0, 16, 16 + slot * 56, 0));
            rootStack.getChildren().add(t);
            t.setOpacity(0); t.setTranslateY(10);

            FadeTransition     fi = new FadeTransition(Duration.millis(200), t);     fi.setToValue(1);
            TranslateTransition si = new TranslateTransition(Duration.millis(200), t); si.setToY(0);
            PauseTransition     pa = new PauseTransition(Duration.seconds(3.5));
            FadeTransition      fo = new FadeTransition(Duration.millis(180), t);    fo.setToValue(0);
            fo.setOnFinished(e -> { rootStack.getChildren().remove(t); toastSlot = Math.max(0, toastSlot-1); });
            new SequentialTransition(new ParallelTransition(fi, si), pa, fo).play();
        });
    }

    // --- Dark mode ---

    private void applyDarkMode(boolean dark) {
        Scene s = primaryStage != null ? primaryStage.getScene() : null;
        if (s == null) return;
        if (dark) { if (!s.getRoot().getStyleClass().contains("dark-mode")) s.getRoot().getStyleClass().add("dark-mode"); }
        else        s.getRoot().getStyleClass().remove("dark-mode");
    }

    // --- Lifecycle ---

    private void shutdown() {
        stopAutoRefresh();
        try { UserService.persistDatabase(); } catch (Exception ignored) {}
        try { BookService.persistBooksDatabase(); } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
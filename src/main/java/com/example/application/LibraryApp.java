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
import javafx.scene.Node;
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
    private HBox     activeToast = null;  // track current visible toast
    private SequentialTransition activeToastAnim = null;

    private enum SetupAccess {
        STAFF,
        USER
    }

    private final ObservableList<Book>          booksList    = FXCollections.observableArrayList();
    private final ObservableList<IssueRecord>   issuesList   = FXCollections.observableArrayList();
    private final ObservableList<BorrowRequest> requestsList = FXCollections.observableArrayList();

    private AnalyticsDashboard analyticsDashboard;
    private CatalogView        catalogView;
    private CirculationView    circulationView;

    @Override
    public void start(Stage stage) {
        LoggingConfigurator.configure();
        this.primaryStage = stage;
        stage.setTitle("Library OS");
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = screen.getWidth() * 0.95;
        double h = screen.getHeight() * 0.95;
        stage.setMinWidth(Math.min(900, w));
        stage.setMinHeight(Math.min(600, h));
        rootStack = new StackPane();

        Scene scene = AppTheme.createScene(rootStack, w, h);
        stage.setScene(scene);
        AppTheme.applyWindowIcon(stage);

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        initializeLibrarySelection(cfg);
        initializeOptionalDatabase(cfg);
        AppTheme.darkMode = cfg.isDarkMode();
        if (cfg.isDarkMode()) applyDarkMode(true);
        if (cfg.isInitialSetupDone()) {
            showLoginScreen();
        } else {
            rootStack.getChildren().setAll(new StackPane());
        }

        stage.show();
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> shutdown());
        if (!cfg.isInitialSetupDone()) {
            Platform.runLater(this::showSetupWizard);
        }
    }

    // --- First-run wizard ---

    private void showSetupWizard() {
        Optional<SetupAccess> setupAccess = promptInitialSetupAccess();
        if (setupAccess.isEmpty()) {
            Platform.exit();
            return;
        }

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        if (setupAccess.get() == SetupAccess.USER) {
            cfg.markSetupDone();
            try {
                AppConfigurationService.updateConfiguration(cfg);
                showLoginScreen();
                showInfo("Initial setup was skipped. A librarian or administrator can configure Library OS later from Settings.");
            } catch (IOException ex) {
                LOG.warning("Config save: " + ex.getMessage());
                showError("Could not save the initial library configuration: " + ex.getMessage());
            }
            return;
        }

        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Library OS - Staff Setup");
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

        TextField libNameField  = wField(cfg.getLibraryName(),  "e.g. City Public Library");
        TextField branchField   = wField(cfg.getBranchName(),   "e.g. Main Branch");
        TextField dataDirField  = wField(cfg.getDataDirectory(),   "data");
        TextField exportField   = wField(cfg.getExportDirectory(),  "exports");
        final DatabaseConfiguration[] databaseConfigHolder = {
                cfg.getDatabaseConfiguration() != null
                        ? cfg.getDatabaseConfiguration()
                        : new DatabaseConfiguration()
        };

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

        Label dbStatus = new Label();
        dbStatus.setWrapText(true);
        dbStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");
        Runnable refreshDatabaseStatus = () -> {
            DatabaseConfiguration dbConfig = databaseConfigHolder[0];
            if (dbConfig == null || !dbConfig.isConfigured()) {
                dbStatus.setText("Database setup is optional. File storage will be used by default.");
                return;
            }

            String location = dbConfig.getEngine() == DatabaseConfiguration.Engine.SQLITE
                    ? dbConfig.getSqliteFile()
                    : dbConfig.getHost() + ":" + dbConfig.getPort() + " / " + dbConfig.getDatabase();
            dbStatus.setText("Configured: " + dbConfig.getEngine().getDisplayName() + " - " + location);
        };
        refreshDatabaseStatus.run();

        Button dbConfigBtn = AppTheme.createIconTextButton(
                "Optional Database Setup", AppTheme.ICON_SAVE, AppTheme.ButtonStyle.OUTLINE);
        dbConfigBtn.setOnAction(e -> DatabaseConfigurationDialog.show(primaryStage, databaseConfigHolder[0])
                .ifPresent(updated -> {
                    databaseConfigHolder[0] = updated;
                    refreshDatabaseStatus.run();
                }));

        VBox databaseBox = new VBox(8, dbConfigBtn, dbStatus);
        form.getChildren().add(wRow("Database", databaseBox));

        // ── Import existing data (optional) ───────────────────────────────
        Label importHint = new Label("Optionally restore data from a previous backup or a database.");
        importHint.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");
        importHint.setWrapText(true);

        Button importFileBtn = AppTheme.createIconTextButton(
                "Import from Backup File", AppTheme.ICON_SAVE, AppTheme.ButtonStyle.OUTLINE);
        Button importDbBtn = AppTheme.createIconTextButton(
                "Import from Database", AppTheme.ICON_SYNC, AppTheme.ButtonStyle.OUTLINE);
        importFileBtn.setMaxWidth(Double.MAX_VALUE);
        importDbBtn.setMaxWidth(Double.MAX_VALUE);

        Label importStatus = new Label();
        importStatus.setWrapText(true);
        importStatus.setStyle("-fx-font-size:12px;");

        importFileBtn.setOnAction(ev -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select a .ser backup file from the backup folder");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Serialized backup (*.ser)", "*.ser"));
            java.io.File chosen = fc.showOpenDialog(primaryStage);
            if (chosen == null) return;
            try {
                java.nio.file.Path backupDir = chosen.getParentFile().toPath();
                java.nio.file.Path dataDir   = com.example.storage.AppPaths.resolveDataDirectory();
                java.nio.file.Files.createDirectories(dataDir);
                try (var stream = java.nio.file.Files.list(backupDir)) {
                    stream.filter(p -> p.toString().endsWith(".ser"))
                            .forEach(p -> {
                                try {
                                    java.nio.file.Files.copy(p, dataDir.resolve(p.getFileName()),
                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                } catch (Exception ignored) {}
                            });
                }
                importStatus.setText("✓ Files restored from: " + backupDir.getFileName());
                importStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#16A34A;");
                AppTheme.pulse(importFileBtn, 1);
            } catch (Exception ex) {
                importStatus.setText("✗ Import failed: " + ex.getMessage());
                importStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#DC2626;");
            }
        });

        importDbBtn.setOnAction(ev -> {
            DatabaseConfiguration dbCfg = databaseConfigHolder[0];
            if (dbCfg == null || !dbCfg.isConfigured()) {
                importStatus.setText("⚠ Configure a database connection above first.");
                importStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#D97706;");
                AppTheme.shake(importDbBtn);
                return;
            }
            boolean connected = com.example.services.DatabaseConnectionService.connect(dbCfg);
            if (!connected) {
                importStatus.setText("✗ Could not connect to the database. Check settings.");
                importStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#DC2626;");
                AppTheme.shake(importDbBtn);
                return;
            }
            importStatus.setText("✓ Connected to " + dbCfg.getEngine().getDisplayName() +
                    ". Data will be loaded from the database on startup.");
            importStatus.setStyle("-fx-font-size:12px; -fx-text-fill:#16A34A;");
            AppTheme.pulse(importDbBtn, 1);
        });

        VBox importBox = new VBox(8, importHint, importFileBtn, importDbBtn, importStatus);
        form.getChildren().add(wRow("Restore / Import", importBox));

        root.getChildren().addAll(hero, form);
        dp.setContent(root);

        ButtonType doneBt = new ButtonType("Save & Continue", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().add(doneBt);
        Button ok = (Button) dp.lookupButton(doneBt);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (libNameField.getText().trim().isEmpty()) {
                showError("Library name is required.");
                event.consume();
                return;
            }
            if (branchField.getText().trim().isEmpty()) {
                showError("Branch name is required.");
                event.consume();
            }
        });

        dlg.setResultConverter(bt -> bt == doneBt);
        Optional<Boolean> setupResult = dlg.showAndWait();
        if (setupResult.orElse(false)) {
            cfg.setLibraryName(libNameField.getText());
            cfg.setBranchName(branchField.getText());
            cfg.setDataDirectory(dataDirField.getText());
            cfg.setExportDirectory(exportField.getText());
            cfg.setDatabaseConfiguration(databaseConfigHolder[0]);
            cfg.markSetupDone();
            try {
                AppConfigurationService.updateConfiguration(cfg);
                showLoginScreen();
                showConfigurationSavedToast(new DatabaseConfiguration(), cfg.getDatabaseConfiguration());
            } catch (IOException ex) {
                LOG.warning("Config save: " + ex.getMessage());
                showError("Could not save the initial library configuration: " + ex.getMessage());
            }
        } else {
            Platform.exit();
        }
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

    private Optional<SetupAccess> promptInitialSetupAccess() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Library OS Setup");
        alert.setHeaderText("Who is setting up this library?");
        alert.setContentText("Librarians and administrators can configure library settings now. Regular users can skip setup and continue to sign in.");
        alert.initOwner(primaryStage);
        AppTheme.applyTheme(alert.getDialogPane());

        ButtonType staffType = new ButtonType("Librarian / Admin", ButtonBar.ButtonData.OK_DONE);
        ButtonType userType = new ButtonType("User", ButtonBar.ButtonData.OTHER);
        alert.getButtonTypes().setAll(staffType, userType, ButtonType.CANCEL);

        return alert.showAndWait().flatMap(result -> {
            if (result == staffType) {
                return Optional.of(SetupAccess.STAFF);
            }
            if (result == userType) {
                return Optional.of(SetupAccess.USER);
            }
            return Optional.empty();
        });
    }

    // --- Login ---

    private void initializeLibrarySelection(AppConfiguration cfg) {
        List<String> knownLibraries = cfg.getKnownLibraries();
        cfg.normalize();
        cfg.rememberCurrentLibrary();
        if (cfg.isInitialSetupDone() && !knownLibraries.equals(cfg.getKnownLibraries())) {
            try {
                AppConfigurationService.updateConfiguration(cfg);
            } catch (IOException ex) {
                LOG.warning("Could not persist default library metadata: " + ex.getMessage());
            }
        }
    }

    private void initializeOptionalDatabase(AppConfiguration cfg) {
        try {
            DatabaseConfiguration databaseConfiguration = cfg.getDatabaseConfiguration();
            if (databaseConfiguration != null && databaseConfiguration.isConfigured()) {
                if (!DatabaseConnectionService.connect(databaseConfiguration)) {
                    LOG.warning("Database configuration is present but the connection could not be established.");
                }
            } else {
                DatabaseConnectionService.disconnect();
            }
        } catch (Exception ex) {
            LOG.warning("Database initialization failed: " + ex.getMessage());
        }
    }

    private void showLoginScreen() {
        stopAutoRefresh();
        analyticsDashboard = null; catalogView = null; circulationView = null;
        LoginView lv = new LoginView(this::handleLoginSuccess, this::showRegistrationDialog, this);
        lv.setOpacity(0);
        rootStack.getChildren().setAll(lv);
        FadeTransition _ft = new FadeTransition(Duration.millis(350), lv); _ft.setToValue(1); _ft.play();
    }

    private void showRegistrationDialog() {
        RegistrationDialog.show(primaryStage, !UserService.hasRegisteredUsers(), false)
                .ifPresent(req -> {
                    try {
                        // Check username uniqueness before creating
                        if (UserService.userExists(req.username())) {
                            showError("Username \"" + req.username() + "\" is already taken. Please choose another.");
                            return;
                        }
                        if (UserService.emailExists(req.email())) {
                            showError("Email address \"" + req.email() + "\" is already in use.");
                            return;
                        }
                        UserService.createUser(req.username(), req.password(), req.role());
                        User created = UserService.getUserById(req.username());
                        created.setEmail(req.email());
                        created.setContactNumber(req.phoneNumber());
                        created.setActive(!req.pendingApproval());
                        UserService.updateUser(created);
                        if (req.pendingApproval()) {
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
        contentArea.setPadding(new Insets(24, 0, 0, 24));
        contentArea.getStyleClass().add("content-area");

        layout.setLeft(sidebar);
        layout.setTop(buildHeader());
        layout.setCenter(contentArea);
        layout.setBottom(buildStatusBar());

        rootStack.getChildren().setAll(layout);
        primaryStage.setMaximized(true);

        refreshAllData();
        startAutoRefresh();
        Platform.runLater(() -> {
            navigateToDashboard();
            showSuccess("Signed in as " + currentUser + ".");
        });
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
        Button dash = navBtn("Dashboard", AppTheme.ICON_DASHBOARD, true, this::navigateToDashboard);
        Button cat  = navBtn("Catalog", AppTheme.ICON_LIBRARY, true, this::navigateToCatalog);
        Button circ = navBtn("Circulation", AppTheme.ICON_SYNC, true, this::navigateToCirculation);
        VBox nav = new VBox(4, navHdr, dash, cat, circ);

        VBox mgmt = new VBox(4);
        if (currentUserRole.isStaff()) {
            Label mgmtHdr = new Label("MANAGEMENT"); mgmtHdr.getStyleClass().add("sidebar-section-label");
            Button users = navBtn("Users", AppTheme.ICON_USER, false, this::showUserManagement);
            Button sett  = navBtn("Settings", AppTheme.ICON_SETTINGS, false, this::showSettings);
            mgmt.getChildren().addAll(mgmtHdr, users, sett);
        }

        // Account quick-access (all users)
        Label accountHdr = new Label("ACCOUNT"); accountHdr.getStyleClass().add("sidebar-section-label");
        Button profileBtn = navBtn("My Profile", AppTheme.ICON_USER, false, () -> {
            if (UserAccountDialogs.showProfileEditor(primaryStage, currentUser)) {
                showSuccess("Profile updated.");
                User updated = UserService.getUserById(currentUser);
                if (updated != null && userNameLabel != null) userNameLabel.setText(updated.getFullName());
            }
        });
        Button passBtn = navBtn("Change Password", AppTheme.ICON_LOCK, false, () -> {
            if (UserAccountDialogs.showPasswordEditor(primaryStage, currentUser))
                showSuccess("Password changed.");
        });
        VBox accountSection = new VBox(4, accountHdr, profileBtn, passBtn);

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        User u = UserService.getUserById(currentUser);
        userNameLabel = new Label(u != null ? u.getFullName() : currentUser);
        userNameLabel.getStyleClass().add("sidebar-profile-name");
        userRoleLabel = new Label(currentUserRole.getDisplayName());
        userRoleLabel.getStyleClass().add("sidebar-profile-role");
        VBox profile = new VBox(3, userNameLabel, userRoleLabel);
        profile.getStyleClass().add("sidebar-profile");

        sb.getChildren().addAll(logoBox, nav, mgmt, accountSection, spacer, profile);
        return sb;
    }

    private Button navBtn(String text, String iconPath, boolean activatesNav, Runnable action) {
        Button b = new Button(text);
        if (iconPath != null && !iconPath.isBlank()) {
            b.setGraphic(AppTheme.createIcon(iconPath, 18));
        }
        b.getStyleClass().add("sidebar-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            if (activatesNav) {
                setActiveNav(b);
            }
            action.run();
        });
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
            refreshAllData(true);
            PauseTransition p = new PauseTransition(Duration.millis(900));
            p.setOnFinished(ev -> title.setText("Dashboard"));
            p.play();
        });

        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        Button themeBtn = AppTheme.createIconButton(
                cfg.isDarkMode() ? AppTheme.ICON_MOON : AppTheme.ICON_SUN,
                cfg.isDarkMode() ? "Dark theme enabled" : "Light theme enabled",
                AppTheme.ButtonStyle.GHOST);
        themeBtn.getStyleClass().add("theme-toggle-btn");
        themeBtn.setOnAction(e -> {
            cfg.toggleDarkMode();
            applyDarkMode(cfg.isDarkMode());
            themeBtn.setGraphic(AppTheme.createIcon(
                    cfg.isDarkMode() ? AppTheme.ICON_MOON : AppTheme.ICON_SUN, 18));
            themeBtn.setTooltip(AppTheme.createTooltip(cfg.isDarkMode() ? "Dark theme enabled" : "Light theme enabled"));
            try { AppConfigurationService.updateConfiguration(cfg); } catch (IOException ignored) {}
        });

        Button logoutBtn = AppTheme.createIconButton(AppTheme.ICON_LOGOUT, "Sign out", AppTheme.ButtonStyle.GHOST);
        logoutBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Sign out of Library OS?", ButtonType.YES, ButtonType.NO);
            a.setTitle("Sign Out"); a.initOwner(primaryStage);
            AppTheme.applyTheme(a.getDialogPane());
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
        bar.setMinHeight(36); bar.setPrefHeight(36); bar.setMaxHeight(36);

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
            analyticsDashboard = new AnalyticsDashboard(currentUser, currentUserRole.isStaff(), this);
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
        Region outgoing = contentArea.getChildren().isEmpty()
                ? null : (Region) contentArea.getChildren().get(0);
        if (outgoing == view) return;
        // Use crossfade for smooth page transitions
        AppTheme.crossfadeViews(outgoing, view, contentArea);
        if (view instanceof AnalyticsDashboard dashboard) {
            Platform.runLater(dashboard::refreshLayout);
        }
    }

    // --- Settings dialogs ---

    private void showUserManagement() {
        showView(new UserManagementView(currentUser, this));
    }

    private void showSettings() {
        SettingsView settingsView = new SettingsView(currentUserRole, new SettingsView.Actions() {
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
        showView(settingsView);
    }

    private void showLibraryConfig() {
        try {
            AppConfiguration cfg = AppConfigurationService.getConfiguration();
            showView(new LibraryConfigurationView(cfg, msg -> {
                showSuccess(msg);
                refreshAllData();
            }));
        } catch (Exception ex) {
            showError("Could not load config: " + ex.getMessage());
        }
    }

    private void showDataManagement() {
        try {
            Map<String,Object> s = BookService.getLibraryStatistics();
            AppConfiguration cfg = AppConfigurationService.getConfiguration();
            DataManagementView view = new DataManagementView(primaryStage, new DataManagementView.Snapshot(
                    n(s,"totalBooks"), n(s,"totalCopies"), n(s,"availableCopies"),
                    n(s,"issuedCopies"), n(s,"overdueBooks"), UserService.getAllUsers().size(),
                    n(s,"pendingRequests"),
                    ((Number) s.getOrDefault("totalFines", 0.0)).doubleValue(),
                    cfg.getExportDirectory(), cfg.isEmailConfigured()), this);
            showView(view);
        } catch (Exception ex) { showError("Data management error: " + ex.getMessage()); }
    }

    private static int n(Map<String,Object> m, String k) {
        return ((Number) m.getOrDefault(k, 0)).intValue();
    }

    // --- Data refresh ---

    private void refreshAllData() {
        refreshAllData(false);
    }

    private void refreshAllData(boolean showToastOnSuccess) {
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
            if (showToastOnSuccess) {
                showInfo("Library data refreshed.");
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
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshAllData(false)));
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
            // Dismiss any existing toast immediately so they don't stack
            if (activeToast != null) {
                if (activeToastAnim != null) activeToastAnim.stop();
                rootStack.getChildren().remove(activeToast);
                activeToast = null;
                activeToastAnim = null;
            }

            HBox t = new HBox(8);
            t.setAlignment(Pos.CENTER_LEFT);
            t.getStyleClass().addAll("toast-notification", style);
            // Keep toast compact — max 420px, min content width
            t.setMaxWidth(420);
            t.setMaxHeight(Region.USE_PREF_SIZE);

            Label ico = new Label(icon);
            ico.setStyle("-fx-font-size:13px; -fx-min-width:16px; -fx-max-width:16px; " +
                    "-fx-alignment:center; -fx-font-weight:700;");
            Label msg = new Label(message);
            msg.setStyle("-fx-font-size:13px; -fx-font-weight:500;");
            msg.setWrapText(false);
            msg.setEllipsisString("…");
            t.getChildren().addAll(ico, msg);

            StackPane.setAlignment(t, Pos.TOP_CENTER);
            StackPane.setMargin(t, new Insets(84, 0, 0, 0));
            rootStack.getChildren().add(t);
            t.setOpacity(0);
            t.setTranslateY(-15);
            activeToast = t;

            FadeTransition      fi = new FadeTransition(Duration.millis(180), t); fi.setToValue(1);
            TranslateTransition si = new TranslateTransition(Duration.millis(180), t); si.setToY(0);
            PauseTransition     pa = new PauseTransition(Duration.seconds(3.0));
            FadeTransition      fo = new FadeTransition(Duration.millis(200), t); fo.setToValue(0);
            fo.setOnFinished(e -> {
                rootStack.getChildren().remove(t);
                if (activeToast == t) { activeToast = null; activeToastAnim = null; }
            });
            SequentialTransition seq = new SequentialTransition(new ParallelTransition(fi, si), pa, fo);
            activeToastAnim = seq;
            seq.play();
        });
    }

    // --- Dark mode ---

    private void applyDarkMode(boolean dark) {
        AppTheme.darkMode = dark;
        Scene s = primaryStage != null ? primaryStage.getScene() : null;
        if (s == null) return;
        AppTheme.animateThemeChange(s.getRoot(), () -> {
            if (dark) { if (!s.getRoot().getStyleClass().contains("dark-mode")) s.getRoot().getStyleClass().add("dark-mode"); }
            else        s.getRoot().getStyleClass().remove("dark-mode");
            rebuildCurrentViewForTheme();
        });
    }

    private void rebuildCurrentViewForTheme() {
        if (contentArea == null || contentArea.getChildren().isEmpty()) {
            return;
        }

        Node currentView = contentArea.getChildren().getFirst();
        analyticsDashboard = null;
        catalogView = null;
        circulationView = null;

        if (currentView instanceof AnalyticsDashboard) {
            navigateToDashboard();
        } else if (currentView instanceof CatalogView) {
            navigateToCatalog();
        } else if (currentView instanceof CirculationView) {
            navigateToCirculation();
        }
    }

    /** Apply dark mode to a DialogPane so dialogs respect the current theme. */
    public static void applyDialogTheme(DialogPane pane) {
        AppTheme.applyTheme(pane);
        AppConfiguration cfg = AppConfigurationService.getConfiguration();
        if (cfg.isDarkMode()) {
            if (!pane.getStyleClass().contains("dark-mode")) {
                pane.getStyleClass().add("dark-mode");
            }
        }
    }

    // --- Lifecycle ---

    private void shutdown() {
        stopAutoRefresh();
        DatabaseConnectionService.disconnect();
        try { UserService.persistDatabase(); } catch (Exception ignored) {}
        try { BookService.persistBooksDatabase(); } catch (Exception ignored) {}
    }

    private void showConfigurationSavedToast(DatabaseConfiguration previousDatabaseConfiguration,
                                             DatabaseConfiguration currentDatabaseConfiguration) {
        DatabaseConfiguration previous = previousDatabaseConfiguration != null
                ? previousDatabaseConfiguration
                : new DatabaseConfiguration();
        DatabaseConfiguration current = currentDatabaseConfiguration != null
                ? currentDatabaseConfiguration
                : new DatabaseConfiguration();

        if (!current.isConfigured()) {
            DatabaseConnectionService.disconnect();
            if (previous.isConfigured()) {
                showSuccess("Configuration saved. Database sync is disabled and file storage is active.");
            } else {
                showSuccess("Configuration saved.");
            }
            return;
        }

        boolean changed = !current.equals(previous);
        if (DatabaseConnectionService.connect(current)) {
            if (changed) {
                showSuccess("Configuration saved. " + current.getEngine().getDisplayName() + " is connected.");
            } else {
                showSuccess("Configuration saved.");
            }
        } else {
            showWarning(changed
                    ? "Configuration saved, but the database connection could not be established. File storage remains active."
                    : "Configuration saved. The current database connection is unavailable, so file storage remains active.");
        }
    }

    public static void main(String[] args) { launch(args); }
}
package com.example.application.ui;

import com.example.entities.AppConfiguration;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Library configuration dialog.
 * Tabs: Borrowing Rules | Email | Export & Storage
 * Added: currency symbol, fine rate, data directory browse.
 */
public class LibraryConfigurationDialog {

    public static Optional<ConfigData> show(Stage owner, AppConfiguration config,
                                            int maxBorrowLimit, int loanDays, double finePerDay) {
        Dialog<ConfigData> dialog = new Dialog<>();
        dialog.setTitle("Library Configuration");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(580);
        pane.setMinWidth(520);
        pane.setPrefHeight(620);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ─── Tab 1: Borrowing Rules ──────────────────────────────
        VBox rulesPanel = panelContainer();

        Label rulesTitle = panelTitle("Borrowing Rules");
        Label rulesDesc  = panelDesc("Configure how many books users can borrow and for how long.");

        GridPane rulesGrid = grid();

        Spinner<Integer> maxBorrowSpin = new Spinner<>(1, 100, maxBorrowLimit);
        maxBorrowSpin.setEditable(true); 
        maxBorrowSpin.setId("maxBorrow");
        maxBorrowSpin.setStyle("-fx-font-size: 14px;");

        Spinner<Integer> loanDaysSpin = new Spinner<>(1, 365, loanDays);
        loanDaysSpin.setEditable(true); 
        loanDaysSpin.setId("loanDays");
        loanDaysSpin.setStyle("-fx-font-size: 14px;");

        Spinner<Integer> renewalSpin = new Spinner<>(0, 10, 2);
        renewalSpin.setEditable(true); 
        renewalSpin.setId("renewals");
        renewalSpin.setStyle("-fx-font-size: 14px;");

        rulesGrid.addRow(0, gridLabel("Max Books per User:"),  maxBorrowSpin);
        rulesGrid.addRow(1, gridLabel("Loan Period (days):"),  loanDaysSpin);
        rulesGrid.addRow(2, gridLabel("Max Renewals:"),        renewalSpin);

        // ─── Fine / Currency sub-section ────────────────────────
        Separator sep = new Separator();
        Label fineTitle = new Label("Fine and Currency Settings");
        fineTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:" + textPrimary() + ";");

        GridPane fineGrid = grid();

        Spinner<Double> fineSpin = new Spinner<>(0.0, 1000.0, finePerDay, 0.50);
        fineSpin.setEditable(true); fineSpin.setId("finePerDay");

        TextField currSymbolField = new TextField(
                config.getCurrencySymbol() != null ? config.getCurrencySymbol() : "$");
        currSymbolField.setId("currencySymbol");
        currSymbolField.setPrefWidth(60);
        currSymbolField.setMaxWidth(60);
        currSymbolField.setStyle(inputStyle());

        TextField currCodeField = new TextField(
                config.getCurrencyCode() != null ? config.getCurrencyCode() : "USD");
        currCodeField.setId("currencyCode");
        currCodeField.setPrefWidth(80);
        currCodeField.setMaxWidth(80);
        currCodeField.setStyle(inputStyle());

        fineGrid.addRow(0, gridLabel("Fine per Day:"),      fineSpin);
        fineGrid.addRow(1, gridLabel("Currency Symbol:"),   currSymbolField);
        fineGrid.addRow(2, gridLabel("Currency Code:"),     currCodeField);

        rulesPanel.getChildren().addAll(rulesTitle, rulesDesc, rulesGrid,
                sep, fineTitle, fineGrid);

        // ─── Tab 2: Email ────────────────────────────────────────
        VBox emailPanel = panelContainer();
        emailPanel.getChildren().addAll(
                panelTitle("Email / SMTP"),
                panelDesc("Configure outgoing email for overdue reminders."));

        GridPane emailGrid = grid();

        TextField smtpHostField = inputTF("smtpHost",
                config.getSmtpHost() != null ? config.getSmtpHost() : "", "smtp.example.com");
        ComboBox<String> smtpPortCombo = new ComboBox<>(FXCollections.observableArrayList(
                config.getCommonSmtpPorts().stream().map(LibraryConfigurationDialog::smtpPortLabel).toList()));
        smtpPortCombo.setId("smtpPort");
        smtpPortCombo.setMaxWidth(Double.MAX_VALUE);
        smtpPortCombo.setStyle(inputStyle());
        smtpPortCombo.setValue(smtpPortLabel(config.getSmtpPort()));

        TextField smtpUserField = inputTF("smtpUsername",
                config.getSmtpUsername() != null ? config.getSmtpUsername() : "", "user@example.com");
        PasswordField smtpPassField = new PasswordField();
        smtpPassField.setId("smtpPassword"); smtpPassField.setStyle(inputStyle());
        smtpPassField.setPromptText("SMTP password");
        if (config.getSmtpPassword() != null) smtpPassField.setText(config.getSmtpPassword());

        TextField fromField = inputTF("fromAddress",
                config.getFromAddress() != null ? config.getFromAddress() : "", "noreply@library.com");

        CheckBox authCheck = new CheckBox("Enable SMTP Authentication");
        authCheck.setSelected(config.isSmtpAuth()); authCheck.setId("smtpAuth");
        CheckBox tlsCheck  = new CheckBox("Enable STARTTLS");
        tlsCheck.setSelected(config.isStartTlsEnabled()); tlsCheck.setId("startTls");

        emailGrid.addRow(0, gridLabel("SMTP Host:"),     smtpHostField);
        emailGrid.addRow(1, gridLabel("SMTP Port:"),     smtpPortCombo);
        emailGrid.addRow(2, gridLabel("Username:"),      smtpUserField);
        emailGrid.addRow(3, gridLabel("Password:"),      smtpPassField);
        emailGrid.addRow(4, gridLabel("From Address:"),  fromField);
        emailGrid.add(authCheck, 0, 5, 2, 1);
        emailGrid.add(tlsCheck,  0, 6, 2, 1);

        emailPanel.getChildren().add(emailGrid);

        // ─── Tab 3: Storage & Export ─────────────────────────────
        VBox storagePanel = panelContainer();
        storagePanel.getChildren().addAll(
                panelTitle("Storage and Export"),
                panelDesc("Set where Library OS stores data and exports reports."));

        GridPane storageGrid = grid();

        TextField exportDirField = inputTF("exportDirectory",
                config.getExportDirectory(), "exports");
        Button exportBrowse = browseBtn("Choose export folder", exportDirField, owner);
        HBox exportRow = new HBox(8, exportDirField, exportBrowse);
        HBox.setHgrow(exportDirField, Priority.ALWAYS);

        storageGrid.addRow(0, gridLabel("Export Directory:"), exportRow);

        // Library / Branch identity
        Separator sep2 = new Separator();
        Label idTitle = new Label("Library Identity");
        idTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:" + textPrimary() + ";");

        TextField libNameField   = inputTF("libraryName",  config.getLibraryName(),  "My Library");
        TextField branchNameField = inputTF("branchName", config.getBranchName(), "Main Branch");

        GridPane idGrid = grid();
        idGrid.addRow(0, gridLabel("Library Name:"), libNameField);
        idGrid.addRow(1, gridLabel("Branch Name:"),  branchNameField);

        storagePanel.getChildren().addAll(storageGrid, sep2, idTitle, idGrid);

        // ─── Assemble tabs ───────────────────────────────────────
        tabs.getTabs().addAll(
                tab("Borrowing", AppTheme.ICON_LIBRARY, rulesPanel),
                tab("Email", AppTheme.ICON_MAIL, emailPanel),
                tab("Storage", AppTheme.ICON_SAVE, storagePanel)
        );
        pane.setContent(tabs);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        okBtn.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:600; -fx-font-size:14px; " +
                "-fx-background-radius:10px; -fx-padding:10 24;");
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#334155" : "#E5E7EB") + "; " +
                    "-fx-text-fill:" + (AppTheme.darkMode ? "#F8FAFC" : "#1F2937") + "; " +
                    "-fx-font-weight:600; -fx-font-size:14px; -fx-background-radius:10px; -fx-padding:10 20;");
        }

        // ─── Result converter ────────────────────────────────────
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            return new ConfigData(
                    maxBorrowSpin.getValue(),
                    loanDaysSpin.getValue(),
                    fineSpin.getValue(),
                    exportDirField.getText().trim(),
                    currSymbolField.getText().trim(),
                    currCodeField.getText().trim(),
                    smtpHostField.getText().trim(),
                    smtpPortValue(smtpPortCombo.getValue()),
                    smtpUserField.getText().trim(),
                    smtpPassField.getText(),
                    fromField.getText().trim(),
                    authCheck.isSelected(),
                    tlsCheck.isSelected(),
                    libNameField.getText().trim(),
                    branchNameField.getText().trim()
            );
        });

        return dialog.showAndWait();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private static VBox panelContainer() {
        VBox p = new VBox(14);
        p.setPadding(new Insets(20));
        return p;
    }
    private static Label panelTitle(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:" + textPrimary() + ";");
        return l;
    }
    private static Label panelDesc(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:" + textMuted() + ";");
        l.setWrapText(true);
        return l;
    }
    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(14); g.setVgap(12);
        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }
    private static Label gridLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:" + textPrimary() + ";");
        return l;
    }
    private static TextField inputTF(String id, String val, String prompt) {
        TextField f = new TextField(val);
        f.setId(id); f.setPromptText(prompt);
        f.setStyle(inputStyle());
        return f;
    }
    private static String inputStyle() {
        if (AppTheme.darkMode) {
            return "-fx-background-color:#1E293B; -fx-border-color:#334155; " +
                    "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                    "-fx-padding:9 12; -fx-font-size:14px; -fx-text-fill:#E2E8F0;";
        }
        return "-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:9 12; -fx-font-size:14px;";
    }
    private static Button browseBtn(String title, TextField target, Stage owner) {
        Button b = new Button("Browse...");
        b.setStyle("-fx-background-color:#E2E8F0; -fx-background-radius:8px; " +
                "-fx-border-radius:8px; -fx-cursor:hand; -fx-padding:8 14; -fx-font-weight:600;");
        b.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle(title);
            java.io.File dir = dc.showDialog(owner);
            if (dir != null) target.setText(dir.getAbsolutePath());
        });
        return b;
    }

    private static String smtpPortLabel(int port) {
        return switch (port) {
            case 25 -> "25 (Plain SMTP)";
            case 465 -> "465 (SSL/TLS)";
            case 587 -> "587 (STARTTLS)";
            case 2525 -> "2525 (Alternative SMTP)";
            default -> port + " (Custom)";
        };
    }

    private static int smtpPortValue(String label) {
        if (label == null || label.isBlank()) {
            return 587;
        }
        String numeric = label.split(" ", 2)[0].trim();
        return Integer.parseInt(numeric);
    }

    private static Tab tab(String title, String iconPath, VBox panel) {
        Tab tab = new Tab(title, wrap(panel));
        tab.setGraphic(AppTheme.createIcon(iconPath, 14));
        return tab;
    }

    private static ScrollPane wrap(VBox panel) {
        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        return scrollPane;
    }

    private static String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#0F172A";
    }

    private static String textMuted() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    // ─── Record ──────────────────────────────────────────────────

    public record ConfigData(
            int maxBorrowLimit, int loanDays, double finePerDay,
            String exportDirectory, String currencySymbol, String currencyCode,
            String smtpHost, int smtpPort,
            String smtpUsername, String smtpPassword, String fromAddress,
            boolean smtpAuth, boolean startTlsEnabled,
            String libraryName, String branchName) {}
}

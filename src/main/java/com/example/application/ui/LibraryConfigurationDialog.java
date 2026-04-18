package com.example.application.ui;

import com.example.entities.AppConfiguration;
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
        pane.setPrefHeight(520);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ─── Tab 1: Borrowing Rules ──────────────────────────────
        VBox rulesPanel = panelContainer();

        Label rulesTitle = panelTitle("📚  Borrowing Rules");
        Label rulesDesc  = panelDesc("Configure how many books users can borrow and for how long.");

        GridPane rulesGrid = grid();

        Spinner<Integer> maxBorrowSpin = new Spinner<>(1, 100, maxBorrowLimit);
        maxBorrowSpin.setEditable(true); maxBorrowSpin.setId("maxBorrow");

        Spinner<Integer> loanDaysSpin = new Spinner<>(1, 365, loanDays);
        loanDaysSpin.setEditable(true); loanDaysSpin.setId("loanDays");

        Spinner<Integer> renewalSpin = new Spinner<>(0, 10, 2);
        renewalSpin.setEditable(true); renewalSpin.setId("renewals");

        rulesGrid.addRow(0, gridLabel("Max Books per User:"),  maxBorrowSpin);
        rulesGrid.addRow(1, gridLabel("Loan Period (days):"),  loanDaysSpin);
        rulesGrid.addRow(2, gridLabel("Max Renewals:"),        renewalSpin);

        // ─── Fine / Currency sub-section ────────────────────────
        Separator sep = new Separator();
        Label fineTitle = new Label("💰  Fine & Currency Settings");
        fineTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#1E293B;");

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
                panelTitle("📧  Email / SMTP"),
                panelDesc("Configure outgoing email for overdue reminders."));

        GridPane emailGrid = grid();

        TextField smtpHostField = inputTF("smtpHost",
                config.getSmtpHost() != null ? config.getSmtpHost() : "", "smtp.example.com");
        Spinner<Integer> smtpPortSpin = new Spinner<>(1, 65535, config.getSmtpPort());
        smtpPortSpin.setEditable(true); smtpPortSpin.setId("smtpPort");

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
        emailGrid.addRow(1, gridLabel("SMTP Port:"),     smtpPortSpin);
        emailGrid.addRow(2, gridLabel("Username:"),      smtpUserField);
        emailGrid.addRow(3, gridLabel("Password:"),      smtpPassField);
        emailGrid.addRow(4, gridLabel("From Address:"),  fromField);
        emailGrid.add(authCheck, 0, 5, 2, 1);
        emailGrid.add(tlsCheck,  0, 6, 2, 1);

        emailPanel.getChildren().add(emailGrid);

        // ─── Tab 3: Storage & Export ─────────────────────────────
        VBox storagePanel = panelContainer();
        storagePanel.getChildren().addAll(
                panelTitle("💾  Storage & Export"),
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
        Label idTitle = new Label("🏛️  Library Identity");
        idTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#1E293B;");

        TextField libNameField   = inputTF("libraryName",  config.getLibraryName(),  "My Library");
        TextField branchNameField = inputTF("branchName", config.getBranchName(), "Main Branch");

        GridPane idGrid = grid();
        idGrid.addRow(0, gridLabel("Library Name:"), libNameField);
        idGrid.addRow(1, gridLabel("Branch Name:"),  branchNameField);

        storagePanel.getChildren().addAll(storageGrid, sep2, idTitle, idGrid);

        // ─── Assemble tabs ───────────────────────────────────────
        tabs.getTabs().addAll(
                new Tab("📚  Borrowing", rulesPanel),
                new Tab("📧  Email",     emailPanel),
                new Tab("💾  Storage",   storagePanel)
        );
        pane.setContent(tabs);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:600; -fx-font-size:14px; " +
                "-fx-background-radius:10px; -fx-padding:10 24;");

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
                    smtpPortSpin.getValue(),
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
        l.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#0F172A;");
        return l;
    }
    private static Label panelDesc(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:#64748B;");
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
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#374151;");
        return l;
    }
    private static TextField inputTF(String id, String val, String prompt) {
        TextField f = new TextField(val);
        f.setId(id); f.setPromptText(prompt);
        f.setStyle(inputStyle());
        return f;
    }
    private static String inputStyle() {
        return "-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:9 12; -fx-font-size:14px;";
    }
    private static Button browseBtn(String title, TextField target, Stage owner) {
        Button b = new Button("Browse");
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

    // ─── Record ──────────────────────────────────────────────────

    public record ConfigData(
            int maxBorrowLimit, int loanDays, double finePerDay,
            String exportDirectory, String currencySymbol, String currencyCode,
            String smtpHost, int smtpPort,
            String smtpUsername, String smtpPassword, String fromAddress,
            boolean smtpAuth, boolean startTlsEnabled,
            String libraryName, String branchName) {}
}
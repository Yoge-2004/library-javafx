package com.example.application.ui;

import com.example.entities.AppConfiguration;
import com.example.services.AppConfigurationService;
import com.example.services.BookService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.function.Consumer;

public class LibraryConfigurationView extends BorderPane {

    private final AppConfiguration config;
    private final Consumer<String> onSave;
    
    private Spinner<Integer> maxBorrowSpin;
    private Spinner<Integer> loanDaysSpin;
    private Spinner<Integer> renewalSpin;
    private Spinner<Double> fineSpin;
    private TextField currSymbolField;
    private TextField currCodeField;
    
    private TextField smtpHostField;
    private ComboBox<Integer> smtpPortCombo;
    private TextField smtpUserField;
    private PasswordField smtpPassField;
    private TextField fromField;
    private CheckBox authCheck;
    private CheckBox tlsCheck;
    
    private TextField exportDirField;
    private ComboBox<String> storageFormatCombo;

    public LibraryConfigurationView(AppConfiguration config, Consumer<String> onSave) {
        this.config = config;
        this.onSave = onSave;
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: " + pageBackground() + ";");
        
        VBox header = new VBox(8);
        header.setPadding(new Insets(28, 28, 20, 28));
        header.setStyle("-fx-background-color: #0F172A;");
        Label title = new Label("Library Configuration");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label sub = new Label("Global system settings, borrowing rules, and email configuration");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        header.getChildren().addAll(title, sub);
        setTop(header);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("settings-tab-pane");

        tabs.getTabs().addAll(
            new Tab("Rules & Currency", buildRulesPanel()),
            new Tab("Email / SMTP", buildEmailPanel()),
            new Tab("Storage & Data", buildStoragePanel())
        );

        VBox content = new VBox(20, tabs);
        content.setPadding(new Insets(20));
        
        Button saveBtn = new Button("Save Configuration");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setPrefHeight(40);
        saveBtn.setOnAction(e -> handleSave());
        
        VBox footer = new VBox(saveBtn);
        footer.setPadding(new Insets(20));
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        VBox main = new VBox(tabs, footer);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        
        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        setCenter(scroll);
    }

    private VBox buildRulesPanel() {
        VBox p = panelContainer();
        GridPane g = grid();
        
        maxBorrowSpin = spinner(1, 100, BookService.getMaxBorrowLimit());
        loanDaysSpin = spinner(1, 365, BookService.getLoanPeriodDays());
        renewalSpin = spinner(0, 10, 2);
        fineSpin = doubleSpinner(0.0, 1000.0, BookService.getFinePerDay());
        
        currSymbolField = textField(config.getCurrencySymbol());
        currCodeField = textField(config.getCurrencyCode());

        g.addRow(0, label("Max Books per User:"), maxBorrowSpin);
        g.addRow(1, label("Loan Period (days):"), loanDaysSpin);
        g.addRow(2, label("Max Renewals:"), renewalSpin);
        g.addRow(3, label("Fine per Day:"), fineSpin);
        g.addRow(4, label("Currency Symbol:"), currSymbolField);
        g.addRow(5, label("Currency Code:"), currCodeField);
        
        p.getChildren().addAll(sectionHeader("BORROWING & FINES"), g);
        return p;
    }

    private VBox buildEmailPanel() {
        VBox p = panelContainer();
        GridPane g = grid();

        smtpHostField = textField(config.getSmtpHost());
        smtpPortCombo = new ComboBox<>(FXCollections.observableArrayList(25, 465, 587, 2525));
        smtpPortCombo.setValue(config.getSmtpPort());
        smtpPortCombo.setEditable(true);
        
        smtpUserField = textField(config.getSmtpUsername());
        smtpPassField = new PasswordField();
        smtpPassField.setText(config.getSmtpPassword());
        smtpPassField.setStyle(inputStyle());
        
        fromField = textField(config.getFromAddress());
        authCheck = new CheckBox("Enable SMTP Authentication");
        authCheck.setSelected(config.isSmtpAuth());
        tlsCheck = new CheckBox("Enable STARTTLS");
        tlsCheck.setSelected(config.isStartTlsEnabled());

        g.addRow(0, label("SMTP Host:"), smtpHostField);
        g.addRow(1, label("SMTP Port:"), smtpPortCombo);
        g.addRow(2, label("Username:"), smtpUserField);
        g.addRow(3, label("Password:"), smtpPassField);
        g.addRow(4, label("From Address:"), fromField);
        g.add(authCheck, 0, 5, 2, 1);
        g.add(tlsCheck, 0, 6, 2, 1);

        p.getChildren().addAll(sectionHeader("SMTP CONFIGURATION"), g);
        return p;
    }

    private VBox buildStoragePanel() {
        VBox p = panelContainer();
        GridPane g = grid();

        exportDirField = textField(config.getExportDirectory());
        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(getScene().getWindow());
            if (f != null) exportDirField.setText(f.getAbsolutePath());
        });
        HBox dirBox = new HBox(8, exportDirField, browseBtn);
        HBox.setHgrow(exportDirField, Priority.ALWAYS);

        storageFormatCombo = new ComboBox<>(FXCollections.observableArrayList("JSON", "BINARY", "XML"));
        storageFormatCombo.setValue("JSON");

        g.addRow(0, label("Data Directory:"), dirBox);
        g.addRow(1, label("Storage Format:"), storageFormatCombo);

        p.getChildren().addAll(sectionHeader("STORAGE SETTINGS"), g);
        return p;
    }

    private void handleSave() {
        try {
            config.setSmtpHost(smtpHostField.getText());
            config.setSmtpPort(smtpPortCombo.getValue());
            config.setSmtpUsername(smtpUserField.getText());
            config.setSmtpPassword(smtpPassField.getText());
            config.setFromAddress(fromField.getText());
            config.setSmtpAuth(authCheck.isSelected());
            config.setStartTlsEnabled(tlsCheck.isSelected());
            config.setCurrencySymbol(currSymbolField.getText());
            config.setCurrencyCode(currCodeField.getText());
            config.setExportDirectory(exportDirField.getText());
            
            AppConfigurationService.updateConfiguration(config);
            BookService.updateLibraryConfiguration(
                    maxBorrowSpin.getValue(),
                    loanDaysSpin.getValue(),
                    fineSpin.getValue()
            );
            
            if (onSave != null) onSave.accept("Configuration saved successfully.");
        } catch (Exception e) {
            // Error handling handled by caller or via toast
        }
    }

    // --- UI Helpers ---
    private VBox panelContainer() {
        VBox v = new VBox(20);
        v.setPadding(new Insets(24));
        return v;
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(20); g.setVgap(14);
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private Label sectionHeader(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #14B8A6; -fx-letter-spacing: 1px;");
        return l;
    }

    private Label label(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary() + ";");
        return l;
    }

    private TextField textField(String v) {
        TextField f = new TextField(v != null ? v : "");
        f.setStyle(inputStyle());
        return f;
    }

    private Spinner<Integer> spinner(int min, int max, int val) {
        Spinner<Integer> s = new Spinner<>(min, max, val);
        s.setEditable(true);
        s.setMaxWidth(Double.MAX_VALUE);
        s.getStyleClass().add("themed-spinner");
        return s;
    }

    private Spinner<Double> doubleSpinner(double min, double max, double val) {
        Spinner<Double> s = new Spinner<>(min, max, val, 0.5);
        s.setEditable(true);
        s.setMaxWidth(Double.MAX_VALUE);
        s.getStyleClass().add("themed-spinner");
        return s;
    }

    private String inputStyle() {
        return "-fx-background-color: " + (AppTheme.darkMode ? "#1E293B" : "#FFFFFF") + "; " +
               "-fx-border-color: " + (AppTheme.darkMode ? "#334155" : "#E2E8F0") + "; " +
               "-fx-border-width: 1.5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12;";
    }

    private String pageBackground() { return AppTheme.darkMode ? "#0F172A" : "#F8FAFC"; }
    private String textPrimary() { return AppTheme.darkMode ? "#F1F5F9" : "#1E293B"; }
}

package com.example.application.ui;

import com.example.entities.BooksDB;
import com.example.services.BookService;
import com.example.services.ReportExportService;
import com.example.services.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data management dialog for backups, exports, and system statistics.
 */
public class DataManagementDialog {

    public static void show(Stage owner, Snapshot snapshot) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Data Management");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        AppTheme.applyTheme(dialogPane);
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(500);

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        // Header
        Label title = new Label("Data Management");
        title.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 24px; " +
                "-fx-font-weight: 700; -fx-text-fill: #0F172A;");

        // Statistics cards
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(16);

        statsGrid.add(createStatCard("📚", "Books", String.valueOf(snapshot.totalBooks())), 0, 0);
        statsGrid.add(createStatCard("📖", "Total Copies", String.valueOf(snapshot.totalCopies())), 1, 0);
        statsGrid.add(createStatCard("✓", "Available", String.valueOf(snapshot.availableCopies())), 2, 0);
        statsGrid.add(createStatCard("🔄", "Issued", String.valueOf(snapshot.issuedCopies())), 0, 1);
        statsGrid.add(createStatCard("👥", "Users", String.valueOf(snapshot.totalUsers())), 1, 1);
        statsGrid.add(createStatCard("⚠", "Overdue", String.valueOf(snapshot.overdueBooks())), 2, 1);

        // Actions section
        Label actionsLabel = new Label("Actions");
        actionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #374151;");

        VBox actionsBox = new VBox(12);
        actionsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        actionsBox.setPadding(new Insets(20));

        // Export reports button
        Button exportBtn = createActionButton("📊", "Export Reports",
                "Generate overdue, issued books & requests as CSV", () -> exportReports(snapshot));

        // Backup button
        Button backupBtn = createActionButton("💾", "Create Backup",
                "Snapshot all data files to a timestamped backup", () -> createBackup(owner));

        // Restore button
        Button restoreBtn = createActionButton("🔄", "Restore from Backup",
                "Load a previously saved backup file", () -> restoreBackup(owner));

        actionsBox.getChildren().addAll(exportBtn, new Separator(), backupBtn, new Separator(), restoreBtn);

        // Email configuration status
        HBox emailStatus = new HBox(12);
        emailStatus.setAlignment(Pos.CENTER_LEFT);
        emailStatus.setStyle("-fx-background-color: " + (snapshot.emailConfigured() ? "#F0FDF4" : "#FEF3C7") + "; " +
                "-fx-background-radius: 8px; -fx-padding: 12 16;");

        Label emailIcon = new Label(snapshot.emailConfigured() ? "✓" : "⚠");
        emailIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: " + (snapshot.emailConfigured() ? "#16A34A" : "#D97706") + ";");

        Label emailText = new Label(snapshot.emailConfigured()
                ? "Email notifications are configured"
                : "Email notifications are not configured");
        emailText.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (snapshot.emailConfigured() ? "#166534" : "#92400E") + ";");

        emailStatus.getChildren().addAll(emailIcon, emailText);

        content.getChildren().addAll(title, statsGrid, actionsLabel, actionsBox, emailStatus);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        dialogPane.setContent(scrollPane);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private static VBox createStatCard(String icon, String label, String value) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 24px; " +
                "-fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        card.getChildren().addAll(iconLabel, valueLabel, labelLabel);
        return card;
    }

    private static Button createActionButton(String icon, String title, String description, Runnable action) {
        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #1E293B;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        textBox.getChildren().addAll(titleLabel, descLabel);

        Label arrowLabel = new Label("›");
        arrowLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #94A3B8;");

        content.getChildren().addAll(iconLabel, textBox, arrowLabel);
        btn.setGraphic(content);

        btn.setOnAction(e -> action.run());

        return btn;
    }

    private static void exportReports(Snapshot snapshot) {
        try {
            Path overduePath  = ReportExportService.exportOverdueReportCsv(BookService.getAllOverdueBooks());
            Path issuedPath   = ReportExportService.exportIssuedBooksCsv(BookService.getAllActiveIssueRecords());
            Path requestsPath = ReportExportService.exportBorrowRequestsCsv(BookService.getAllBorrowRequests());

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Export Complete");
            a.setHeaderText("Reports exported successfully");
            a.setContentText("Saved to: " + snapshot.exportDirectory() +
                    "\n• " + overduePath.getFileName() +
                    "\n• " + issuedPath.getFileName() +
                    "\n• " + requestsPath.getFileName());
            a.showAndWait();
        } catch (Exception e) {
            error("Export Failed", e.getMessage());
        }
    }

    private static void createBackup(Stage owner) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupDir = Paths.get("backups", ts);
            Files.createDirectories(backupDir);

            // Copy all .ser files
            Path dataDir = Paths.get("data");
            if (Files.exists(dataDir)) {
                try (var stream = Files.list(dataDir)) {
                    stream.filter(p -> p.toString().endsWith(".ser"))
                            .forEach(p -> {
                                try { Files.copy(p, backupDir.resolve(p.getFileName()),
                                        StandardCopyOption.REPLACE_EXISTING); }
                                catch (Exception ignored) {}
                            });
                }
            }

            // Persist latest in-memory state
            BookService.persistBooksDatabase();
            UserService.persistDatabase();

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Backup Complete");
            a.setHeaderText("Backup created successfully");
            a.setContentText("Location: " + backupDir.toAbsolutePath());
            a.showAndWait();
        } catch (Exception e) {
            error("Backup Failed", e.getMessage());
        }
    }

    private static void restoreBackup(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Backup Folder — choose any .ser file inside it");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized data", "*.ser"));
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Backup");
        confirm.setHeaderText("Restore from: " + chosen.getParentFile().getName() + "?");
        confirm.setContentText("This will overwrite current data. The app should be restarted after restore.");
        confirm.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                Path backupFolder = chosen.getParentFile().toPath();
                Path dataDir = Paths.get("data");
                Files.createDirectories(dataDir);

                try (var stream = Files.list(backupFolder)) {
                    stream.filter(p -> p.toString().endsWith(".ser"))
                            .forEach(p -> {
                                try { Files.copy(p, dataDir.resolve(p.getFileName()),
                                        StandardCopyOption.REPLACE_EXISTING); }
                                catch (Exception ignored) {}
                            });
                }
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Restore Complete");
                done.setHeaderText("Data restored from backup.");
                done.setContentText("Please restart Library OS for changes to take effect.");
                done.showAndWait();
            } catch (Exception e) {
                error("Restore Failed", e.getMessage());
            }
        });
    }

    private static void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        a.showAndWait();
    }

    public record Snapshot(int totalBooks, int totalCopies, int availableCopies,
                           int issuedCopies, int overdueBooks, int totalUsers,
                           int pendingRequests, double totalFines,
                           String exportDirectory, boolean emailConfigured) {}
}
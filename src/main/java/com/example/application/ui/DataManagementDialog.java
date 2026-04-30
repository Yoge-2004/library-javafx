package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.services.BookService;
import com.example.services.ReportExportService;
import com.example.services.UserService;
import com.example.storage.AppPaths;
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

    public static void show(Stage owner, Snapshot snapshot, ToastDisplay toastDisplay) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Data Management");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        AppTheme.applyTheme(dialogPane);
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(500);

        dialog.setOnShown(evt -> {
            if (dialogPane.getScene() != null && dialogPane.getScene().getWindow() instanceof Stage st) {
                AppTheme.applyWindowIcon(st);
                st.setMinWidth(560);
                st.setMinHeight(480);
                st.sizeToScene();
                st.centerOnScreen();
            }
        });

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        // Header
        Label title = new Label("Data Management");
        title.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 24px; " +
                "-fx-font-weight: 700; -fx-text-fill: " + textPrimary() + ";");

        // Statistics cards
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(16);

        statsGrid.add(createStatCard(AppTheme.ICON_BOOK, "#0D9488",
                "Books", String.valueOf(snapshot.totalBooks())), 0, 0);
        statsGrid.add(createStatCard(AppTheme.ICON_LIBRARY, "#3B82F6",
                "Total Copies", String.valueOf(snapshot.totalCopies())), 1, 0);
        statsGrid.add(createStatCard(AppTheme.ICON_CHECK, "#16A34A",
                "Available", String.valueOf(snapshot.availableCopies())), 2, 0);
        statsGrid.add(createStatCard(AppTheme.ICON_SYNC, "#0EA5E9",
                "Issued", String.valueOf(snapshot.issuedCopies())), 0, 1);
        statsGrid.add(createStatCard(AppTheme.ICON_USER, "#8B5CF6",
                "Users", String.valueOf(snapshot.totalUsers())), 1, 1);
        statsGrid.add(createStatCard(AppTheme.ICON_WARNING, "#D97706",
                "Overdue", String.valueOf(snapshot.overdueBooks())), 2, 1);

        // Actions section
        Label actionsLabel = new Label("Actions");
        actionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + textSecondary() + ";");

        FlowPane actionsPane = new FlowPane();
        actionsPane.setHgap(12);
        actionsPane.setVgap(12);
        actionsPane.setPrefWrapLength(520);

        // Export reports button
        Button exportBtn = createActionButton(AppTheme.ICON_DASHBOARD, "#0D9488", "Export Reports",
                "Generate overdue, issued books & requests as CSV", () -> exportReports(snapshot, toastDisplay));

        // Backup button
        Button backupBtn = createActionButton(AppTheme.ICON_SAVE, "#3B82F6", "Create Backup",
                "Snapshot all data files to a timestamped backup", () -> createBackup(owner, toastDisplay));

        // Restore button
        Button restoreBtn = createActionButton(AppTheme.ICON_SYNC, "#F59E0B", "Restore from Backup",
                "Load a previously saved backup file", () -> restoreBackup(owner, toastDisplay));

        actionsPane.getChildren().addAll(exportBtn, backupBtn, restoreBtn);

        // Email configuration status
        HBox emailStatus = new HBox(12);
        emailStatus.setAlignment(Pos.CENTER_LEFT);
        emailStatus.setStyle("-fx-background-color: " + (snapshot.emailConfigured() ? surfaceTint("#16A34A") : surfaceTint("#D97706")) + "; " +
                "-fx-background-radius: 8px; -fx-padding: 12 16;");

        StackPane emailIcon = createIconBubble(
                snapshot.emailConfigured() ? AppTheme.ICON_CHECK : AppTheme.ICON_WARNING,
                snapshot.emailConfigured() ? "#16A34A" : "#D97706");

        Label emailText = new Label(snapshot.emailConfigured()
                ? "Email notifications are configured"
                : "Email notifications are not configured");
        emailText.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textSecondary() + ";");

        emailStatus.getChildren().addAll(emailIcon, emailText);

        content.getChildren().addAll(title, statsGrid, actionsLabel, actionsPane, emailStatus);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");

        dialogPane.setContent(scrollPane);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        styleSecondaryButton((Button) dialogPane.lookupButton(ButtonType.CLOSE), "Close");

        dialog.showAndWait();
    }

    private static VBox createStatCard(String iconPath, String accentColor, String label, String value) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setStyle(cardStyle());

        StackPane iconBubble = createIconBubble(iconPath, accentColor);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 24px; " +
                "-fx-font-weight: 700; -fx-text-fill: " + textPrimary() + ";");

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");

        card.getChildren().addAll(iconBubble, valueLabel, labelLabel);
        return card;
    }

    private static Button createActionButton(String iconPath, String accentColor,
                                             String title, String description, Runnable action) {
        Button btn = new Button();
        btn.setStyle(cardStyle() + "-fx-cursor: hand;");
        btn.setPrefWidth(170);
        btn.setMinWidth(160);
        btn.setMaxWidth(220);
        btn.setMinHeight(120);
        btn.setPrefHeight(120);
        btn.setAlignment(Pos.TOP_LEFT);

        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(18));

        StackPane iconBubble = createIconBubble(iconPath, accentColor);

        VBox textBox = new VBox(4);
        textBox.setFillWidth(true);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary() + ";");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textMuted() + ";");
        descLabel.setWrapText(true);

        textBox.getChildren().addAll(titleLabel, descLabel);
        content.getChildren().addAll(iconBubble, textBox);
        btn.setGraphic(content);

        btn.setOnAction(e -> action.run());

        return btn;
    }

    private static void exportReports(Snapshot snapshot, ToastDisplay toastDisplay) {
        try {
            Path overduePath  = ReportExportService.exportOverdueReportCsv(BookService.getAllOverdueBooks());
            Path issuedPath   = ReportExportService.exportIssuedBooksCsv(BookService.getAllActiveIssueRecords());
            Path requestsPath = ReportExportService.exportBorrowRequestsCsv(BookService.getAllBorrowRequests());
            notifySuccess(toastDisplay, "Reports exported to " + snapshot.exportDirectory() +
                    " (" + overduePath.getFileName() + ", " + issuedPath.getFileName() + ", " + requestsPath.getFileName() + ")");
        } catch (Exception e) {
            notifyError(toastDisplay, "Export failed: " + e.getMessage());
        }
    }

    private static void createBackup(Stage owner, ToastDisplay toastDisplay) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupDir = AppPaths.backupDirectory().resolve(ts);
            Files.createDirectories(backupDir);

            // Copy all .ser files
            Path dataDir = AppPaths.resolveDataDirectory();
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
            notifySuccess(toastDisplay, "Backup created at " + backupDir.toAbsolutePath());
        } catch (Exception e) {
            notifyError(toastDisplay, "Backup failed: " + e.getMessage());
        }
    }

    private static void restoreBackup(Stage owner, ToastDisplay toastDisplay) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Backup Folder — choose any .ser file inside it");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Serialized data", "*.ser"));
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        AppTheme.applyTheme(confirm.getDialogPane());
        confirm.setTitle("Restore Backup");
        confirm.setHeaderText("Restore from: " + chosen.getParentFile().getName() + "?");
        confirm.setContentText("This will overwrite current data. The app should be restarted after restore.");
        confirm.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                Path backupFolder = chosen.getParentFile().toPath();
                Path dataDir = AppPaths.resolveDataDirectory();
                Files.createDirectories(dataDir);

                try (var stream = Files.list(backupFolder)) {
                    stream.filter(p -> p.toString().endsWith(".ser"))
                            .forEach(p -> {
                                try { Files.copy(p, dataDir.resolve(p.getFileName()),
                                        StandardCopyOption.REPLACE_EXISTING); }
                                catch (Exception ignored) {}
                            });
                }
                notifySuccess(toastDisplay, "Backup restored. Restart Library OS to load the restored data.");
            } catch (Exception e) {
                notifyError(toastDisplay, "Restore failed: " + e.getMessage());
            }
        });
    }

    private static void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        AppTheme.applyTheme(a.getDialogPane());
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        a.showAndWait();
    }

    private static void notifySuccess(ToastDisplay toastDisplay, String message) {
        if (toastDisplay != null) {
            toastDisplay.showSuccess(message);
            return;
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        AppTheme.applyTheme(a.getDialogPane());
        a.setTitle("Completed");
        a.setHeaderText("Completed");
        a.setContentText(message);
        a.showAndWait();
    }

    private static void notifyError(ToastDisplay toastDisplay, String message) {
        if (toastDisplay != null) {
            toastDisplay.showError(message);
            return;
        }
        error("Operation Failed", message);
    }

    private static StackPane createIconBubble(String iconPath, String accentColor) {
        StackPane bubble = new StackPane(AppTheme.createIcon(iconPath, 18));
        bubble.setMinSize(40, 40);
        bubble.setPrefSize(40, 40);
        bubble.setStyle("-fx-background-color:" + surfaceTint(accentColor) + "; -fx-background-radius:12px;");
        return bubble;
    }

    private static void styleSecondaryButton(Button button, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#334155" : "#E5E7EB") + ";" +
                "-fx-text-fill:" + (AppTheme.darkMode ? "#F8FAFC" : "#1F2937") + ";" +
                "-fx-font-weight:600; -fx-font-size:13px; -fx-background-radius:10px; -fx-padding:9 18;");
    }

    private static String cardStyle() {
        return "-fx-background-color:" + (AppTheme.darkMode ? "#1E293B" : "white") + "; " +
                "-fx-background-radius: 12px; -fx-border-radius: 12px; " +
                "-fx-border-color: " + (AppTheme.darkMode ? "#334155" : "#E2E8F0") + "; -fx-border-width: 1;";
    }

    private static String surfaceTint(String color) {
        return color + "22";
    }

    private static String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#0F172A";
    }

    private static String textSecondary() {
        return AppTheme.darkMode ? "#E2E8F0" : "#374151";
    }

    private static String textMuted() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    public record Snapshot(int totalBooks, int totalCopies, int availableCopies,
                           int issuedCopies, int overdueBooks, int totalUsers,
                           int pendingRequests, double totalFines,
                           String exportDirectory, boolean emailConfigured) {}
}

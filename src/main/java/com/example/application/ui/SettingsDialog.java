package com.example.application.ui;

import com.example.entities.UserRole;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Settings dialog with icons on every item and functional callbacks.
 */
public class SettingsDialog {

    public interface Actions {
        void openProfile();
        void openPassword();
        void openUserManagement();
        void openLibraryConfiguration();
        void openDataManagement();
        void openAnalytics();
    }

    public static void show(Stage owner, UserRole userRole, Actions actions) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(480);
        pane.setMinWidth(420);
        pane.setPrefHeight(560);

        VBox root = new VBox(0);
        root.setFillWidth(true);

        // Header
        VBox header = new VBox(6);
        header.setPadding(new Insets(28, 28, 20, 28));
        header.setStyle("-fx-background-color:#0F172A;");
        Label titleLbl = new Label("⚙  Settings");
        titleLbl.setStyle("-fx-font-size:22px; -fx-font-weight:800; -fx-text-fill:white;");
        Label subLbl = new Label("Account and system preferences");
        subLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#94A3B8;");
        header.getChildren().addAll(titleLbl, subLbl);

        // Content
        VBox content = new VBox(6);
        content.setPadding(new Insets(20, 20, 8, 20));

        // ── Account section ──────────────────────────────────────
        content.getChildren().add(sectionLabel("ACCOUNT"));

        content.getChildren().addAll(
                settingItem("👤", "Profile",
                        "Update your personal information and display name",
                        "#0D9488", () -> { dialog.close(); actions.openProfile(); }),

                settingItem("🔒", "Password",
                        "Change your account password",
                        "#3B82F6", () -> { dialog.close(); actions.openPassword(); })
        );

        // ── Administration section (staff only) ──────────────────
        if (userRole.isStaff()) {
            content.getChildren().add(sectionLabel("ADMINISTRATION"));

            content.getChildren().addAll(
                    settingItem("👥", "User Management",
                            "Add, remove or modify user accounts and roles",
                            "#8B5CF6", () -> { dialog.close(); actions.openUserManagement(); }),

                    settingItem("📚", "Library Configuration",
                            "Borrowing rules, fine rates, loan periods, email SMTP",
                            "#F59E0B", () -> { dialog.close(); actions.openLibraryConfiguration(); }),

                    settingItem("💾", "Data Management",
                            "Backup data, import/export, view system statistics",
                            "#16A34A", () -> { dialog.close(); actions.openDataManagement(); })
            );
        }

        // ── About ────────────────────────────────────────────────
        content.getChildren().add(sectionLabel("ABOUT"));
        HBox aboutCard = new HBox(16);
        aboutCard.setPadding(new Insets(14, 16, 14, 16));
        aboutCard.setStyle("-fx-background-color:white; -fx-background-radius:12px; " +
                "-fx-border-radius:12px; -fx-border-color:#E2E8F0; -fx-border-width:1;");
        aboutCard.setAlignment(Pos.CENTER_LEFT);

        Label aboutIcon = new Label("ℹ");
        aboutIcon.setStyle("-fx-font-size:22px;");

        VBox aboutTxt = new VBox(2);
        Label aboutTitle = new Label("Library OS  v3.1");
        aboutTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
        Label aboutSub = new Label("Modern library management . JavaFX 21 . Java 26");
        aboutSub.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");
        aboutTxt.getChildren().addAll(aboutTitle, aboutSub);
        aboutCard.getChildren().addAll(aboutIcon, aboutTxt);
        content.getChildren().add(aboutCard);

        root.getChildren().addAll(header, content);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        pane.setContent(scroll);
        pane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#94A3B8; " +
                "-fx-padding:16 0 6 4;");
        return l;
    }

    private static HBox settingItem(String emoji, String title, String desc,
                                    String accentColor, Runnable action) {
        HBox item = new HBox(14);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 16, 14, 16));
        item.setStyle(itemStyle(false));
        item.setCursor(javafx.scene.Cursor.HAND);

        // Colored icon bubble
        Label bubble = new Label(emoji);
        bubble.setStyle("-fx-font-size:20px; -fx-background-color:" + accentColor + "22; " +
                "-fx-background-radius:10px; -fx-padding:8;");
        bubble.setMinWidth(44);

        VBox txt = new VBox(3);
        HBox.setHgrow(txt, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:600; -fx-text-fill:#1E293B;");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");
        descLbl.setWrapText(true);
        txt.getChildren().addAll(titleLbl, descLbl);

        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size:22px; -fx-text-fill:#CBD5E1;");

        item.getChildren().addAll(bubble, txt, arrow);

        item.setOnMouseEntered(e -> item.setStyle(itemStyle(true)));
        item.setOnMouseExited(e  -> item.setStyle(itemStyle(false)));
        item.setOnMouseClicked(e -> action.run());

        return item;
    }

    private static String itemStyle(boolean hovered) {
        return "-fx-background-color:" + (hovered ? "#F8FAFC" : "white") + "; " +
                "-fx-background-radius:12px; -fx-border-radius:12px; " +
                "-fx-border-color:" + (hovered ? "#CBD5E1" : "#E2E8F0") + "; " +
                "-fx-border-width:1;" +
                (hovered ? " -fx-effect:dropshadow(gaussian,rgba(15,23,42,0.06),8,0,0,2);" : "");
    }
}
package com.example.application.ui;

import com.example.entities.UserRole;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Settings dialog with icons on every item and functional callbacks.
 */
public class SettingsView extends ScrollPane {

    public interface Actions {
        void openProfile();
        void openPassword();
        void openUserManagement();
        void openLibraryConfiguration();
        void openDataManagement();
        void openAnalytics();
    }

    public SettingsView(UserRole userRole, Actions actions) {
        setFitToWidth(true);
        setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        VBox root = new VBox(0);
        root.setFillWidth(true);

        // Header
        VBox header = new VBox(6);
        header.setPadding(new Insets(28, 28, 20, 28));
        header.setStyle("-fx-background-color:#0F172A;");
        Label titleLbl = new Label("Settings");
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
                settingItem(AppTheme.ICON_USER, "Profile",
                        "Update your personal information and display name",
                        "#0D9488", () -> openFromSettings(actions::openProfile)),

                settingItem(AppTheme.ICON_LOCK, "Password",
                        "Change your account password",
                        "#3B82F6", () -> openFromSettings(actions::openPassword))
        );

        // ── Administration section (staff only) ──────────────────
        if (userRole.isStaff()) {
            content.getChildren().add(sectionLabel("ADMINISTRATION"));

            content.getChildren().addAll(
                    settingItem(AppTheme.ICON_USER, "User Management",
                            "Add, remove or modify user accounts and roles",
                            "#8B5CF6", () -> openFromSettings(actions::openUserManagement)),

                    settingItem(AppTheme.ICON_LIBRARY, "Library Configuration",
                            "Borrowing rules, fines, email, storage, and the database tab",
                            "#F59E0B", () -> openFromSettings(actions::openLibraryConfiguration)),

                    settingItem(AppTheme.ICON_SAVE, "Data Management",
                            "Backup data, import/export, view system statistics",
                            "#16A34A", () -> openFromSettings(actions::openDataManagement))
            );
        }

        // ── About ────────────────────────────────────────────────
        content.getChildren().add(sectionLabel("ABOUT"));
        HBox aboutCard = new HBox(16);
        aboutCard.setPadding(new Insets(14, 16, 14, 16));
        aboutCard.setStyle(itemStyle(false));
        aboutCard.setAlignment(Pos.CENTER_LEFT);

        StackPane aboutIcon = createIconBubble(AppTheme.ICON_HELP, "#64748B");

        VBox aboutTxt = new VBox(2);
        Label aboutTitle = new Label("Library OS  v3.1");
        aboutTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:" + textPrimary() + ";");
        Label aboutSub = new Label("Modern library management . JavaFX 26 . Java 26");
        aboutSub.setStyle("-fx-font-size:12px; -fx-text-fill:" + textMuted() + ";");
        aboutTxt.getChildren().addAll(aboutTitle, aboutSub);
        aboutCard.getChildren().addAll(aboutIcon, aboutTxt);
        content.getChildren().add(aboutCard);

        root.getChildren().addAll(header, content);

        setContent(root);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#94A3B8; " +
                "-fx-padding:16 0 6 4;");
        return l;
    }

    private static HBox settingItem(String iconPath, String title, String desc,
                                    String accentColor, Runnable action) {
        HBox item = new HBox(14);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 16, 14, 16));
        item.setStyle(itemStyle(false));
        item.setCursor(javafx.scene.Cursor.HAND);

        StackPane bubble = createIconBubble(iconPath, accentColor);

        VBox txt = new VBox(3);
        HBox.setHgrow(txt, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:600; -fx-text-fill:" + textPrimary() + ";");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + textMuted() + ";");
        descLbl.setWrapText(true);
        txt.getChildren().addAll(titleLbl, descLbl);

        StackPane arrow = new StackPane(AppTheme.createIcon(AppTheme.ICON_CHEVRON_RIGHT, 14));

        item.getChildren().addAll(bubble, txt, arrow);

        item.setOnMouseEntered(e -> item.setStyle(itemStyle(true)));
        item.setOnMouseExited(e  -> item.setStyle(itemStyle(false)));
        item.setOnMouseClicked(e -> action.run());

        return item;
    }

    private static String itemStyle(boolean hovered) {
        String surface = AppTheme.darkMode
                ? (hovered ? "#0F172A" : "transparent")
                : (hovered ? "#F8FAFC" : "transparent");
        return "-fx-background-color:" + surface + "; " +
                "-fx-background-radius:8px; " +
                (hovered ? " -fx-effect:dropshadow(gaussian,rgba(15,23,42,0.04),4,0,0,1);" : "");
    }

    private static StackPane createIconBubble(String iconPath, String accentColor) {
        StackPane bubble = new StackPane(AppTheme.createIcon(iconPath, 18));
        bubble.setMinSize(44, 44);
        bubble.setPrefSize(44, 44);
        bubble.setMaxSize(44, 44);
        bubble.setStyle("-fx-background-color:" + accentColor + "22; -fx-background-radius:10px;");
        return bubble;
    }

    private static String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#1E293B";
    }

    private static String textMuted() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    private static void openFromSettings(Runnable action) {
        Platform.runLater(action);
    }

}

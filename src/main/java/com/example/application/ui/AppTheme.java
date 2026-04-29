package com.example.application.ui;

import com.example.entities.AppConfiguration;
import com.example.services.AppConfigurationService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modern UI theme system with comprehensive component library,
 * smooth animations, and accessibility features.
 */
public final class AppTheme {

    // === ICON LIBRARY (Material Design SVG Paths) ===
    public static final String ICON_ADD = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    public static final String ICON_EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String ICON_DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    public static final String ICON_REFRESH = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    public static final String ICON_LOGOUT = "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";
    public static final String ICON_VISIBILITY = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    public static final String ICON_VISIBILITY_OFF = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";
    public static final String ICON_SETTINGS = "M19.43 12.98c.04-.32.07-.64.07-.98s-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.23-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49-.12-.64l2.11 1.65c-.04.32-.07.65-.07.98s.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.23.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z";
    public static final String ICON_BOOK = "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z";
    public static final String ICON_WARNING = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    public static final String ICON_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    public static final String ICON_CLOSE = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
    public static final String ICON_USER = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
    public static final String ICON_RETURN = "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z";
    public static final String ICON_MAIL = "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z";
    public static final String ICON_SEARCH = "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    public static final String ICON_DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    public static final String ICON_LIBRARY = "M12 11.55C9.64 9.35 6.48 8 3 8v11c3.48 0 6.64 1.35 9 3.55 2.36-2.19 5.52-3.55 9-3.55V8c-3.48 0-6.64 1.35-9 3.55zM12 8c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3z";
    public static final String ICON_SYNC = "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z";
    public static final String ICON_FILTER = "M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z";
    public static final String ICON_MORE = "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";
    public static final String ICON_DOWNLOAD = "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z";
    public static final String ICON_UPLOAD = "M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z";
    public static final String ICON_PRINT = "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z";
    public static final String ICON_SAVE = "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z";
    public static final String ICON_CANCEL = "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z";
    public static final String ICON_ARROW_BACK = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";
    public static final String ICON_ARROW_FORWARD = "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z";
    public static final String ICON_TRENDING_UP = "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z";
    public static final String ICON_TRENDING_DOWN = "M16 18l2.29-2.29-4.88-4.88-4 4L2 7.41 3.41 6l6 6 4-4 6.3 6.29L22 12v6z";
    public static final String ICON_SCHEDULE = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
    public static final String ICON_NOTIFICATION = "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z";
    public static final String ICON_HELP = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z";
    public static final String ICON_LOCK = "M12 17a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm6-8h-1V7a5 5 0 0 0-10 0v2H6a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-9a2 2 0 0 0-2-2zm-6 8a4 4 0 0 1-1-7.87V7a1 1 0 1 1 2 0v2.13A4 4 0 0 1 12 17zm3-8H9V7a3 3 0 0 1 6 0v2z";
    public static final String ICON_SUN = "M6.76 4.84 5.34 3.42 3.93 4.83l1.41 1.41 1.42-1.4zM1 13h3v-2H1v2zm10 9h2v-3h-2v3zm9-9h3v-2h-3v2zm-1.34 6.17 1.41 1.41 1.41-1.41-1.41-1.41-1.41 1.41zM17.24 4.84l1.41 1.41 1.41-1.42-1.41-1.41-1.41 1.42zM12 6a6 6 0 1 0 0 12 6 6 0 0 0 0-12zm0 10a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm-8.07 4.24 1.41 1.41 1.42-1.41-1.42-1.41-1.41 1.41zM11 1h2v3h-2V1z";
    public static final String ICON_MOON = "M12.74 2a9 9 0 1 0 9.26 9.26A7 7 0 0 1 12.74 2zm-.74 18a7 7 0 0 1-6.95-7.86 9 9 0 0 0 9.81 9.81A6.96 6.96 0 0 1 12 20z";
    public static final String ICON_CHEVRON_RIGHT = "M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z";

    // === BUTTON STYLES ===
    public enum ButtonStyle {
        PRIMARY("btn-primary", Color.web("#0D9488")),
        SECONDARY("btn-secondary", Color.web("#1E293B")),
        SUCCESS("btn-success", Color.web("#16A34A")),
        WARNING("btn-warning", Color.web("#D97706")),
        DANGER("btn-danger", Color.web("#DC2626")),
        GHOST("btn-ghost", Color.TRANSPARENT),
        OUTLINE("btn-outline", Color.TRANSPARENT);

        private final String styleClass;
        private final Color baseColor;

        ButtonStyle(String styleClass, Color baseColor) {
            this.styleClass = styleClass;
            this.baseColor = baseColor;
        }

        public String getStyleClass() { return styleClass; }
        public Color getBaseColor() { return baseColor; }
    }

    // === CHIP STYLES ===
    public enum ChipStyle {
        NEUTRAL("chip-neutral"),
        PRIMARY("chip-primary"),
        SUCCESS("chip-success"),
        WARNING("chip-warning"),
        ERROR("chip-error"),
        INFO("chip-info");

        private final String styleClass;
        ChipStyle(String styleClass) { this.styleClass = styleClass; }
        public String getStyleClass() { return styleClass; }
    }

    private static final String THEME_PATH = "/theme.css";
    private static String cachedStylesheet;
    private static Image cachedLargeIcon;
    private static Image cachedMediumIcon;
    private static Image cachedSmallIcon;
    private static Image cachedTinyIcon;
    /** Set by LibraryApp when dark mode toggles; used to apply dark-mode to dialogs. */
    public static boolean darkMode = false;

    private AppTheme() {}

    // === SCENE CREATION ===
    public static Scene createScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        applyTheme(scene);
        return scene;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        String stylesheet = getStylesheetUrl();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    public static void applyTheme(DialogPane pane) {
        if (pane == null) return;
        String stylesheet = getStylesheetUrl();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
        if (!pane.getStyleClass().contains("dialog-pane-modern"))
            pane.getStyleClass().add("dialog-pane-modern");
        // Apply dark mode so dialogs match the main window
        if (darkMode && !pane.getStyleClass().contains("dark-mode"))
            pane.getStyleClass().add("dark-mode");
        else if (!darkMode)
            pane.getStyleClass().remove("dark-mode");
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof Stage stage) {
                applyWindowIcon(stage);
            }
        });
        if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage stage) {
            applyWindowIcon(stage);
        }
    }

    private static String getStylesheetUrl() {
        if (cachedStylesheet == null) {
            URL resource = AppTheme.class.getResource(THEME_PATH);
            if (resource == null) {
                throw new IllegalStateException("Theme stylesheet not found: " + THEME_PATH);
            }
            cachedStylesheet = resource.toExternalForm();
        }
        return cachedStylesheet;
    }

    // === STAGE UTILITIES ===
    public static Stage createModalStage(Stage owner, String title) {
        return createModalStage(owner, title, 400, 300);
    }

    public static Stage createModalStage(Stage owner, String title, double minWidth, double minHeight) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        applyWindowIcon(stage);
        return stage;
    }

    // === SVG ICON CREATION ===
    public static SVGPath createIcon(String pathContent) {
        return createIcon(pathContent, 20);
    }

    public static SVGPath createIcon(String pathContent, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathContent);
        svg.getStyleClass().add("icon-svg");
        svg.setFill(Color.web("#64748B"));
        double scale = size <= 0 ? 1.0 : size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    public static SVGPath createIcon(String pathContent, Color color) {
        SVGPath svg = createIcon(pathContent);
        svg.setFill(color);
        return svg;
    }

    public static void applyWindowIcon(Stage stage) {
        if (stage == null) {
            return;
        }
        // Always use programmatically-generated canvas icons.
        // JavaFX's Image class cannot decode SVG natively; attempting to load
        // an SVG as a window icon silently fails and leaves cachedLargeIcon null,
        // so subsequent dialog stages never receive icons either.
        if (cachedLargeIcon == null) {
            cachedLargeIcon  = createAppIcon(256);
            cachedMediumIcon = createAppIcon(128);
            cachedSmallIcon  = createAppIcon(64);
            cachedTinyIcon   = createAppIcon(32);
        }
        stage.getIcons().setAll(cachedLargeIcon, cachedMediumIcon, cachedSmallIcon, cachedTinyIcon);
    }

    // === BUTTON CREATION ===
    public static Button createButton(String text, ButtonStyle style) {
        Button button = new Button(text);
        button.getStyleClass().addAll("app-button", style.getStyleClass());
        installButtonAnimation(button);
        return button;
    }

    public static Button createButton(String text, ButtonStyle style, Runnable action) {
        Button button = createButton(text, style);
        button.setOnAction(e -> action.run());
        return button;
    }

    public static Button createIconButton(String iconPath, String tooltip, ButtonStyle style) {
        Button button = new Button();
        button.setGraphic(createIcon(iconPath));
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setTooltip(new Tooltip(tooltip));
        }
        button.getStyleClass().addAll("app-button", "icon-button", style.getStyleClass());
        installButtonAnimation(button);
        return button;
    }

    public static Button createIconTextButton(String text, String iconPath, ButtonStyle style) {
        Button button = new Button(text);
        button.setGraphic(createIcon(iconPath));
        button.getStyleClass().addAll("app-button", "icon-text-button", style.getStyleClass());
        button.setGraphicTextGap(10);
        installButtonAnimation(button);
        return button;
    }

    // === CHIP CREATION ===
    public static Label createChip(String text, ChipStyle style) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("chip", style.getStyleClass());
        return chip;
    }

    public static Label createStatusBadge(String text, ChipStyle style) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("status-badge", style.getStyleClass());
        return badge;
    }

    // === FORM COMPONENTS ===
    public static HBox createPasswordField(String prompt, StringProperty boundProperty) {
        PasswordField hiddenField = new PasswordField();
        hiddenField.setPromptText(prompt);

        TextField visibleField = new TextField();
        visibleField.setPromptText(prompt);
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());
        boundProperty.bind(hiddenField.textProperty());

        StackPane fieldStack = new StackPane(hiddenField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        Button toggleBtn = createIconButton(ICON_VISIBILITY, "Toggle visibility", ButtonStyle.GHOST);
        toggleBtn.getStyleClass().add("password-toggle");

        toggleBtn.setOnAction(e -> {
            boolean isHidden = hiddenField.isVisible();
            hiddenField.setVisible(!isHidden);
            hiddenField.setManaged(!isHidden);
            visibleField.setVisible(isHidden);
            visibleField.setManaged(isHidden);
            toggleBtn.setGraphic(createIcon(isHidden ? ICON_VISIBILITY_OFF : ICON_VISIBILITY));
        });

        HBox wrapper = new HBox(fieldStack, toggleBtn);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.getStyleClass().add("input-with-icon");
        wrapper.setSpacing(4);

        return wrapper;
    }

    public static TextField createSearchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("search-field");
        return field;
    }

    // === LAYOUT COMPONENTS ===
    public static VBox createHeaderBlock(String kicker, String title, String subtitle) {
        VBox block = new VBox(8);
        block.setFillWidth(true);

        if (kicker != null && !kicker.isBlank()) {
            Label kickerLabel = new Label(kicker);
            kickerLabel.getStyleClass().add("label-tiny");
            block.getChildren().add(kickerLabel);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-4");
        titleLabel.setWrapText(true);
        block.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("body-regular");
            subtitleLabel.setWrapText(true);
            block.getChildren().add(subtitleLabel);
        }

        return block;
    }

    public static VBox createMetricCard(String label, String value, String change, boolean positive) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(20));

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value-large");

        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("metric-label");

        card.getChildren().addAll(valueLabel, labelLabel);

        if (change != null && !change.isEmpty()) {
            Label changeLabel = new Label(change);
            changeLabel.getStyleClass().add(positive ? "metric-change-positive" : "metric-change-negative");
            card.getChildren().add(changeLabel);
        }

        installCardHoverEffect(card);
        return card;
    }

    // === ANIMATIONS ===
    public static void fadeIn(Node node, double delayMillis) {
        if (node == null) return;
        node.setOpacity(0);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMillis));
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setToValue(1);

        delay.setOnFinished(e -> fade.play());
        delay.play();
    }

    public static void slideUp(Node node, double delayMillis) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(20);

        PauseTransition delay = new PauseTransition(Duration.millis(delayMillis));

        FadeTransition fade = new FadeTransition(Duration.millis(350), node);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), node);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);

        delay.setOnFinished(e -> parallel.play());
        delay.play();
    }

    public static void staggeredEntrance(Collection<? extends Node> nodes, double initialDelay, double stepDelay) {
        double delay = initialDelay;
        for (Node node : nodes) {
            slideUp(node, delay);
            delay += stepDelay;
        }
    }

    public static void pulse(Node node, int cycles) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), node);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        SequentialTransition pulse = new SequentialTransition(scaleUp, scaleDown);
        pulse.setCycleCount(cycles);
        pulse.play();
    }

    public static void shake(Node node) {
        TranslateTransition left = new TranslateTransition(Duration.millis(50), node);
        left.setToX(-10);

        TranslateTransition right = new TranslateTransition(Duration.millis(50), node);
        right.setToX(10);

        TranslateTransition center = new TranslateTransition(Duration.millis(50), node);
        center.setToX(0);

        SequentialTransition shake = new SequentialTransition(left, right, left, right, center);
        shake.play();
    }

    private static void installButtonAnimation(Button button) {
        button.hoverProperty().addListener((obs, wasHover, isHover) -> {
            if (isHover) {
                animateScale(button, 1.02, 150);
            } else {
                animateScale(button, 1.0, 150);
            }
        });

        button.pressedProperty().addListener((obs, wasPressed, isPressed) -> {
            if (isPressed) {
                animateScale(button, 0.98, 80);
            } else {
                animateScale(button, button.isHover() ? 1.02 : 1.0, 80);
            }
        });
    }

    private static void installCardHoverEffect(Region card) {
        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setColor(Color.web("#0F172A", 0.12));
        hoverShadow.setRadius(15);
        hoverShadow.setOffsetY(5);

        card.hoverProperty().addListener((obs, wasHover, isHover) -> {
            if (isHover) {
                animateScale(card, 1.01, 200);
                card.setEffect(hoverShadow);
            } else {
                animateScale(card, 1.0, 200);
                card.setEffect(null);
            }
        });
    }

    private static void animateScale(Node node, double scale, int durationMillis) {
        node.setCache(true);
        node.setCacheHint(CacheHint.SCALE);

        ScaleTransition transition = new ScaleTransition(Duration.millis(durationMillis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setOnFinished(e -> node.setCache(false));
        transition.play();
    }

    // === UTILITY METHODS ===
    public static void runLater(Runnable action) {
        Platform.runLater(action);
    }

    public static void runLater(Runnable action, double delayMillis) {
        PauseTransition delay = new PauseTransition(Duration.millis(delayMillis));
        delay.setOnFinished(e -> action.run());
        delay.play();
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public static String formatCurrency(double amount) {
        try {
            AppConfiguration configuration = AppConfigurationService.getConfiguration();
            return configuration.formatAmount(amount);
        } catch (Exception ignored) {
            return String.format("$%,.2f", amount);
        }
    }

    public static void showTemporaryMessage(Label label, String message, String styleClass, double durationSeconds) {
        label.setText(message);
        label.getStyleClass().setAll("validation-message", styleClass);
        label.setVisible(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(durationSeconds));
        delay.setOnFinished(e -> {
            label.setVisible(false);
            label.setText("");
        });
        delay.play();
    }

    private static Image createAppIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        double arc = size * 0.28;
        graphics.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0F172A")),
                new Stop(0.55, Color.web("#134E4A")),
                new Stop(1, Color.web("#14B8A6"))));
        graphics.fillRoundRect(0, 0, size, size, arc, arc);

        graphics.setFill(Color.web("#FFFFFF", 0.12));
        graphics.fillOval(size * 0.10, size * 0.08, size * 0.66, size * 0.66);

        graphics.setFill(Color.WHITE);
        double bookX = size * 0.26;
        double bookY = size * 0.18;
        double bookW = size * 0.48;
        double bookH = size * 0.58;
        graphics.fillRoundRect(bookX, bookY, bookW, bookH, size * 0.08, size * 0.08);

        graphics.setFill(Color.web("#CCFBF1"));
        graphics.fillRoundRect(bookX + bookW * 0.10, bookY + bookH * 0.14,
                bookW * 0.18, bookH * 0.72, size * 0.04, size * 0.04);

        graphics.setStroke(Color.web("#0F766E", 0.85));
        graphics.setLineWidth(Math.max(2.0, size * 0.03));
        graphics.strokeLine(bookX + bookW * 0.36, bookY + bookH * 0.26, bookX + bookW * 0.78, bookY + bookH * 0.26);
        graphics.strokeLine(bookX + bookW * 0.36, bookY + bookH * 0.48, bookX + bookW * 0.78, bookY + bookH * 0.48);
        graphics.strokeLine(bookX + bookW * 0.36, bookY + bookH * 0.70, bookX + bookW * 0.66, bookY + bookH * 0.70);

        graphics.setFill(Color.web("#99F6E4"));
        graphics.fillRoundRect(size * 0.30, size * 0.74, size * 0.40, size * 0.08,
                size * 0.03, size * 0.03);

        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(new SnapshotParameters(), image);
        return image;
    }
}
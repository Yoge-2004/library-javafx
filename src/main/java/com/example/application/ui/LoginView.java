package com.example.application.ui;

import com.example.entities.AppConfiguration;
import com.example.entities.User;
import com.example.services.AppConfigurationService;
import com.example.services.ReminderService;
import com.example.services.UserService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern, visually stunning login view with smooth animations,
 * intuitive UX, and professional design.
 */
public class LoginView extends StackPane {

    private final Consumer<String> onLoginSuccess;
    private final Runnable onRegisterRequested;
    private final StringProperty passwordProperty = new SimpleStringProperty("");

    private TextField usernameField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private Button togglePasswordBtn;
    private Label errorLabel;
    private Button loginButton;
    private ProgressIndicator loadingIndicator;
    private VBox loginForm;
    private ComboBox<String> librarySelector;
    private final List<String> availableLibraries = new ArrayList<>();
    private boolean syncingLibraryItems;

    public LoginView(Consumer<String> onLoginSuccess, Runnable onRegisterRequested) {
        this.onLoginSuccess = onLoginSuccess;
        this.onRegisterRequested = onRegisterRequested;

        initializeUI();
        setupAnimations();
    }

    private void initializeUI() {
        // Main container with gradient background
        setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #0F172A, #1E293B 50%, #134E4A);");
        setPadding(new Insets(40));

        // Create split layout: Hero section + Login card
        HBox mainLayout = new HBox(60);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setMaxWidth(1200);

        // Left side - Hero content
        VBox heroSection = createHeroSection();
        HBox.setHgrow(heroSection, Priority.ALWAYS);

        // Right side - Login card
        VBox loginCard = createLoginCard();

        mainLayout.getChildren().addAll(heroSection, loginCard);

        // Scroll pane for small screens
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        getChildren().add(scrollPane);
    }

    private VBox createHeroSection() {
        VBox hero = new VBox(32);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMaxWidth(500);
        hero.setPadding(new Insets(40));

        // Logo/Brand
        Label brandLabel = new Label("LIBRARY OS");
        brandLabel.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 18px; " +
                "-fx-font-weight: 800; -fx-text-fill: #14B8A6; -fx-letter-spacing: 0.2em;");

        // Main headline
        Label headline = new Label("Manage Your Library\nwith Intelligence");
        headline.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 48px; " +
                "-fx-font-weight: 800; -fx-text-fill: white; -fx-line-spacing: 8px;");
        headline.setWrapText(true);

        // Subtitle
        Label subtitle = new Label("A modern, comprehensive solution for library circulation, " +
                "catalog management, and user administration.");
        subtitle.setStyle("-fx-font-size: 18px; -fx-text-fill: #94A3B8; -fx-line-spacing: 4px;");
        subtitle.setWrapText(true);

        // Feature list
        VBox features = new VBox(16);
        features.getChildren().addAll(
                createFeatureItem(AppTheme.ICON_CHECK, "Streamlined book circulation"),
                createFeatureItem(AppTheme.ICON_CHECK, "Real-time analytics dashboard"),
                createFeatureItem(AppTheme.ICON_CHECK, "Automated overdue notifications"),
                createFeatureItem(AppTheme.ICON_CHECK, "Multi-role user management")
        );

        hero.getChildren().addAll(brandLabel, headline, subtitle, features);
        return hero;
    }

    private HBox createFeatureItem(String iconPath, String text) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBadge = new StackPane(AppTheme.createIcon(iconPath, 16));
        iconBadge.setMinSize(28, 28);
        iconBadge.setPrefSize(28, 28);
        iconBadge.setMaxSize(28, 28);
        iconBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.15); -fx-background-radius: 14px;");

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-text-fill: #CBD5E1;");

        item.getChildren().addAll(iconBadge, label);
        return item;
    }

    private VBox createLoginCard() {
        loginForm = new VBox(24);
        loginForm.setAlignment(Pos.TOP_CENTER);
        loginForm.setPadding(new Insets(48));
        loginForm.setMaxWidth(420);
        loginForm.setStyle("-fx-background-color: white; -fx-background-radius: 24px;");

        // Card shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.2));
        shadow.setRadius(30);
        shadow.setOffsetY(10);
        loginForm.setEffect(shadow);

        // Card header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label welcomeLabel = new Label("Welcome back");
        welcomeLabel.setStyle("-fx-font-family: 'Plus Jakarta Sans'; -fx-font-size: 28px; " +
                "-fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Label signInLabel = new Label("Sign in to your account");
        signInLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748B;");

        header.getChildren().addAll(welcomeLabel, signInLabel);

        // Form fields
        VBox formFields = new VBox(20);
        formFields.setFillWidth(true);

        VBox libraryBox = createLibraryBox();

        // Username field
        VBox usernameBox = new VBox(6);
        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle(getInputStyle());
        usernameField.setPrefHeight(48);
        HBox.setHgrow(usernameField, Priority.ALWAYS);

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password field with toggle
        VBox passwordBox = new VBox(6);
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox passwordContainer = createPasswordContainer();

        passwordBox.getChildren().addAll(passwordLabel, passwordContainer);

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        formFields.getChildren().addAll(libraryBox, usernameBox, passwordBox, errorLabel);

        // Login button
        loginButton = new Button("Sign In");
        loginButton.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-font-weight: 600; -fx-background-radius: 12px; " +
                "-fx-cursor: hand;");
        loginButton.setPrefHeight(52);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin());

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        loadingIndicator.setMaxSize(24, 24);

        StackPane buttonStack = new StackPane(loginButton, loadingIndicator);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);
        HBox.setHgrow(buttonStack, Priority.ALWAYS);

        HBox buttonContainer = new HBox(buttonStack);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setMaxWidth(Double.MAX_VALUE);

        // Register link
        HBox registerBox = new HBox(6);
        registerBox.setAlignment(Pos.CENTER);

        Label noAccountLabel = new Label("Don't have an account?");
        noAccountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        Hyperlink registerLink = new Hyperlink("Create one");
        registerLink.setStyle("-fx-font-size: 14px; -fx-text-fill: #0D9488; -fx-font-weight: 600;");
        registerLink.setOnAction(e -> {
            if (onRegisterRequested != null) {
                onRegisterRequested.run();
            }
        });

        registerBox.getChildren().addAll(noAccountLabel, registerLink);

        // Forgot password link
        Hyperlink forgotPassLink = new Hyperlink("Forgot password?");
        forgotPassLink.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        forgotPassLink.setOnAction(e -> showForgotPasswordDialog());

        VBox footerBox = new VBox(8, registerBox, forgotPassLink);
        footerBox.setAlignment(Pos.CENTER);

        // Add enter key handler - only on explicit Enter press, not on every action
        librarySelector.setOnAction(e -> {
            // Only move focus if a value is actually selected (not during text editing)
            if (librarySelector.getValue() != null && !librarySelector.getValue().isEmpty()) {
                usernameField.requestFocus();
            }
        });
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        visiblePasswordField.setOnAction(e -> handleLogin());

        loginForm.getChildren().addAll(header, formFields, buttonContainer, footerBox);

        return loginForm;
    }

    private HBox createPasswordContainer() {
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle("-fx-background-color:transparent; -fx-border-color:transparent; " +
                "-fx-font-size:15px; -fx-text-fill:#111827; -fx-prompt-text-fill:#9CA3AF; " +
                "-fx-padding:10 14; -fx-font-family:'Noto Sans','Liberation Sans','DejaVu Sans',sans-serif;");
        passwordField.setPrefHeight(48);

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Enter your password");
        visiblePasswordField.setStyle("-fx-background-color:transparent; -fx-border-color:transparent; " +
                "-fx-font-size:15px; -fx-text-fill:#111827; -fx-prompt-text-fill:#9CA3AF; " +
                "-fx-padding:10 14; -fx-font-family:'Noto Sans','Liberation Sans','DejaVu Sans',sans-serif;");
        visiblePasswordField.setPrefHeight(48);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        // Bind fields
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordProperty.bind(passwordField.textProperty());

        // Toggle button
        togglePasswordBtn = new Button();
        togglePasswordBtn.setPrefHeight(48);
        togglePasswordBtn.setPrefWidth(48);
        togglePasswordBtn.setGraphic(AppTheme.createIcon(AppTheme.ICON_VISIBILITY, 16));
        togglePasswordBtn.getStyleClass().addAll("app-button", "btn-ghost", "password-toggle", "auth-password-toggle");
        togglePasswordBtn.setFocusTraversable(false);

        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());

        StackPane fieldStack = new StackPane(passwordField, visiblePasswordField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        HBox container = new HBox(fieldStack, togglePasswordBtn);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("input-with-icon");
        return container;
    }

    private VBox createLibraryBox() {
        AppConfiguration configuration = AppConfigurationService.getConfiguration();
        availableLibraries.clear();
        availableLibraries.addAll(configuration.getKnownLibraries());
        if (availableLibraries.isEmpty()) {
            availableLibraries.add(configuration.getCurrentLibraryDisplayName());
        }

        VBox libraryBox = new VBox(6);
        Label libraryLabel = new Label("Library");
        libraryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        librarySelector = new ComboBox<>(FXCollections.observableArrayList(availableLibraries));
        librarySelector.setEditable(true);
        librarySelector.setMaxWidth(Double.MAX_VALUE);
        librarySelector.setPrefHeight(48);
        librarySelector.setVisibleRowCount(Math.min(6, availableLibraries.size()));
        librarySelector.setPromptText("Select your library");
        librarySelector.setValue(configuration.getCurrentLibraryDisplayName());
        librarySelector.getEditor().setText(configuration.getCurrentLibraryDisplayName());
        librarySelector.getStyleClass().add("auth-combo-box");
        librarySelector.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #D1D5DB; " +
                "-fx-border-width: 1.5; -fx-border-radius: 12px; -fx-background-radius: 12px;");
        librarySelector.getEditor().setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 15px; -fx-text-fill: #111827; -fx-prompt-text-fill: #9CA3AF; " +
                "-fx-padding: 11 6 11 14;");
        librarySelector.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (syncingLibraryItems) {
                return;
            }
            filterLibraries(newValue);
        });
        librarySelector.setOnShowing(event -> filterLibraries(librarySelector.getEditor().getText()));
        librarySelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!syncingLibraryItems && newValue != null) {
                librarySelector.getEditor().setText(newValue);
            }
        });

        Label helper = new Label("Start typing to filter the available libraries.");
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        helper.setWrapText(true);

        libraryBox.getChildren().addAll(libraryLabel, librarySelector, helper);
        return libraryBox;
    }

    private String getInputStyle() {
        return "-fx-background-color: #F9FAFB; -fx-border-color: #D1D5DB; " +
                "-fx-border-width: 1.5; -fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; -fx-font-size: 15px; " +
                "-fx-text-fill: #111827; -fx-prompt-text-fill: #9CA3AF; " +
                "-fx-padding: 12 16;";
    }

    private void togglePasswordVisibility() {
        boolean isHidden = passwordField.isVisible();
        passwordField.setVisible(!isHidden);
        passwordField.setManaged(!isHidden);
        visiblePasswordField.setVisible(isHidden);
        visiblePasswordField.setManaged(isHidden);
        togglePasswordBtn.setGraphic(AppTheme.createIcon(
                isHidden ? AppTheme.ICON_VISIBILITY_OFF : AppTheme.ICON_VISIBILITY, 16));
    }

    private void handleLogin() {
        String selectedLibrary = resolveSelectedLibrary();
        String username = usernameField.getText().trim();
        String password = passwordProperty.getValue();

        // Validation
        if (selectedLibrary == null) {
            showError("Select a library from the list before signing in");
            shakeForm();
            return;
        }

        if (username.isEmpty()) {
            showError("Please enter your username");
            shakeForm();
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            shakeForm();
            return;
        }

        // Show loading state
        setLoading(true);

        // Perform login asynchronously
        new Thread(() -> {
            try {
                boolean success = UserService.login(username, password);

                Platform.runLater(() -> {
                    setLoading(false);
                    if (success) {
                        showSuccessAnimation();
                        if (onLoginSuccess != null) {
                            onLoginSuccess.accept(username);
                        }
                    } else {
                        showError("Invalid username or password");
                        shakeForm();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Login failed: " + ex.getMessage());
                    shakeForm();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        loginButton.setVisible(!loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        togglePasswordBtn.setDisable(loading);
        librarySelector.setDisable(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626; -fx-padding: 8 0 0 0; -fx-wrap-text: true;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
    }

    private void filterLibraries(String query) {
        if (librarySelector == null) {
            return;
        }

        String currentText = query == null ? "" : query;
        int caretPosition = Math.max(0, librarySelector.getEditor().getCaretPosition());
        String normalized = query == null ? "" : query.trim().toLowerCase();
        List<String> filtered = availableLibraries.stream()
                .filter(value -> normalized.isEmpty() || value.toLowerCase().contains(normalized))
                .toList();

        syncingLibraryItems = true;
        try {
            // Store the focused state and selection before updating items
            boolean wasEditorFocused = librarySelector.getEditor().isFocused();
            
            librarySelector.setItems(FXCollections.observableArrayList(filtered));
            librarySelector.setVisibleRowCount(Math.max(1, Math.min(6, filtered.size())));
            librarySelector.getEditor().setText(currentText);
            
            // Immediately restore caret position after setting text
            if (caretPosition <= currentText.length()) {
                librarySelector.getEditor().positionCaret(caretPosition);
            }
            
            // Restore focus to editor if it was focused before
            if (wasEditorFocused) {
                librarySelector.getEditor().requestFocus();
            }
        } finally {
            syncingLibraryItems = false;
        }

        // Show dropdown if editor is focused and there are filtered results
        if ((librarySelector.isFocused() || librarySelector.getEditor().isFocused()) && !librarySelector.isShowing() && !filtered.isEmpty()) {
            librarySelector.show();
        }
    }

    private String resolveSelectedLibrary() {
        if (librarySelector == null) {
            return null;
        }

        String typedValue = librarySelector.getEditor().getText() != null
                ? librarySelector.getEditor().getText().trim()
                : "";
        if (typedValue.isEmpty()) {
            return null;
        }

        return availableLibraries.stream()
                .filter(value -> value.equalsIgnoreCase(typedValue))
                .findFirst()
                .orElse(null);
    }

    private void shakeForm() {
        TranslateTransition left = new TranslateTransition(Duration.millis(50), loginForm);
        left.setToX(-8);

        TranslateTransition right = new TranslateTransition(Duration.millis(50), loginForm);
        right.setToX(8);

        TranslateTransition center = new TranslateTransition(Duration.millis(50), loginForm);
        center.setToX(0);

        SequentialTransition shake = new SequentialTransition(left, right, left, right, center);
        shake.play();
    }

    private void showSuccessAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), loginForm);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            loginForm.setVisible(false);
        });
        fade.play();
    }

    private void setupAnimations() {
        // Staggered entrance animation
        Platform.runLater(() -> {
            loginForm.setOpacity(0);
            loginForm.setTranslateY(30);

            FadeTransition fade = new FadeTransition(Duration.millis(600), loginForm);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(600), loginForm);
            slide.setToY(0);
            slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            ParallelTransition entrance = new ParallelTransition(fade, slide);
            entrance.setDelay(Duration.millis(200));
            entrance.play();
        });
    }

    private void showForgotPasswordDialog() {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Forgot Password");
        dlg.setHeaderText("Reset Your Password");

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(500);
        pane.setMinWidth(420);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label info = new Label("Enter your username and Library OS will email a temporary password to the address saved on your account.");
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        TextField usernameResetField = new TextField();
        usernameResetField.setPromptText("Enter your username");
        usernameResetField.setStyle(getInputStyle());
        usernameResetField.setPrefHeight(40);

        Label statusLabel = new Label();
        statusLabel.setVisible(false);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626; -fx-padding: 8 0 0 0;");

        content.getChildren().addAll(info, new Label("Username:"), usernameResetField, statusLabel);
        pane.setContent(content);

        ButtonType sendType = new ButtonType("Send Email", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, sendType);
        Button okBtn = (Button) pane.lookupButton(sendType);
        okBtn.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; -fx-font-weight:600;");

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String username = usernameResetField.getText().trim();
            if (username.isEmpty()) {
                statusLabel.setText("Enter your username first.");
                statusLabel.setVisible(true);
                event.consume();
                return;
            }
            if (!UserService.userExists(username)) {
                statusLabel.setText("No account was found for that username.");
                statusLabel.setVisible(true);
                event.consume();
                return;
            }
            statusLabel.setVisible(false);
        });

        dlg.setResultConverter(bt -> bt == sendType ? usernameResetField.getText().trim() : null);
        dlg.showAndWait().ifPresent(this::dispatchForgotPasswordEmail);
    }

    private void dispatchForgotPasswordEmail(String username) {
        new Thread(() -> {
            try {
                User user = UserService.getUserById(username);
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new IllegalStateException("This account does not have an email address on file.");
                }

                String originalPassword = user.getPassword();
                String temporaryPassword = buildTemporaryPassword();

                user.setPassword(temporaryPassword);
                UserService.updateUser(user);
                UserService.persistDatabase();

                try {
                    ReminderService.sendTemporaryPassword(user, temporaryPassword);
                } catch (Exception mailError) {
                    user.setPassword(originalPassword);
                    UserService.updateUser(user);
                    UserService.persistDatabase();
                    throw mailError;
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    AppTheme.applyTheme(alert.getDialogPane());
                    alert.setTitle("Password Reset Email Sent");
                    alert.setHeaderText("Temporary password sent");
                    alert.setContentText("A temporary password was emailed to " + user.getEmail() + ".");
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Forgot password failed: " + ex.getMessage()));
            }
        }, "forgot-password-email").start();
    }

    private String buildTemporaryPassword() {
        String seed = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "LIB" + seed.substring(0, 8);
    }
}

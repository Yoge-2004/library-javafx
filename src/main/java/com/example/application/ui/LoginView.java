package com.example.application.ui;

import com.example.entities.UserRole;
import com.example.services.UserService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

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

        Label icon = new Label("✓");
        icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #14B8A6; -fx-font-weight: bold;");

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-text-fill: #CBD5E1;");

        item.getChildren().addAll(icon, label);
        return item;
    }

    private VBox createLoginCard() {
        loginForm = new VBox(24);
        loginForm.setAlignment(Pos.CENTER);
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

        formFields.getChildren().addAll(usernameBox, passwordBox, errorLabel);

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
        loadingIndicator.setMaxSize(24, 24);

        HBox buttonContainer = new HBox(12);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(loginButton, loadingIndicator);

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

        // Add enter key handler
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        visiblePasswordField.setOnAction(e -> handleLogin());

        loginForm.getChildren().addAll(header, formFields, buttonContainer, registerBox);

        return loginForm;
    }

    private HBox createPasswordContainer() {
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle(getInputStyle() + "-fx-background-radius: 12 0 0 12;");
        passwordField.setPrefHeight(48);

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Enter your password");
        visiblePasswordField.setStyle(getInputStyle() + "-fx-background-radius: 12 0 0 12;");
        visiblePasswordField.setPrefHeight(48);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        // Bind fields
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordProperty.bind(passwordField.textProperty());

        // Toggle button
        togglePasswordBtn = new Button("👁");
        togglePasswordBtn.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 0 12 12 0; " +
                "-fx-border-color: #D1D5DB; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 12 12 0; " +
                "-fx-cursor: hand; -fx-font-size: 14px;");
        togglePasswordBtn.setPrefHeight(48);
        togglePasswordBtn.setPrefWidth(48);

        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());

        StackPane fieldStack = new StackPane(passwordField, visiblePasswordField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        HBox container = new HBox(fieldStack, togglePasswordBtn);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
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
        togglePasswordBtn.setText(isHidden ? "🙈" : "👁");
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordProperty.getValue();

        // Validation
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
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        visiblePasswordField.setDisable(loading);
        togglePasswordBtn.setDisable(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626; -fx-padding: 8 0 0 0;");
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
}
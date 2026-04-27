package com.example.application.ui;

import com.example.entities.AppConfiguration;
import com.example.entities.User;
import com.example.entities.UserRole;
import com.example.services.AppConfigurationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Registration dialog.
 * - First user -> auto-admin (no choice shown).
 * - ADMIN role is never in the dropdown.
 * - LIBRARIAN selection creates an inactive account pending admin approval.
 * - Real-time: password strength, username validation, show/hide password.
 */
public class RegistrationDialog {

    private record PasswordRow(PasswordField hiddenField, TextField visibleField,
                               Button toggleButton, HBox container) {}

    public static Optional<RegistrationRequest> show(Stage owner, boolean isFirstUser) {
        Dialog<RegistrationRequest> dialog = new Dialog<>();
        dialog.setTitle(isFirstUser ? "Create Administrator Account" : "Create Account");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(540);
        pane.setMinWidth(500);
        pane.setPrefHeight(700);

        AppConfiguration cfg = AppConfigurationService.getConfiguration();

        VBox root = new VBox(0);
        root.setFillWidth(true);

        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(28, 28, 20, 28));
        headerBox.setStyle("-fx-background-color: #0F172A;");

        StackPane iconBadge = new StackPane(AppTheme.createIcon(
                isFirstUser ? AppTheme.ICON_LIBRARY : AppTheme.ICON_USER, 22));
        iconBadge.setPrefSize(52, 52);
        iconBadge.setMaxSize(52, 52);
        iconBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.18); -fx-background-radius: 26px;");

        Label titleLbl = new Label(isFirstUser ? "Welcome to Library OS" : "Create Account");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label libLbl = new Label(cfg.getCurrentLibraryDisplayName());
        libLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #14B8A6; -fx-font-weight: 700;");

        Label subLbl = new Label(isFirstUser
                ? "You are the first user — your account will be Administrator."
                : "Fill in your details to create an account.");
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        subLbl.setWrapText(true);

        headerBox.getChildren().addAll(iconBadge, titleLbl, libLbl, subLbl);

        // Form
        VBox formBox = new VBox(12);
        formBox.setPadding(new Insets(24, 28, 12, 28));

        // Username
        Label uLbl = fieldLabel("Username");
        TextField usernameField = inputField("letters, numbers, . _ - (min 3 chars)");
        Label uFeedback = new Label();
        uFeedback.setStyle("-fx-font-size: 11px;");
        usernameField.textProperty().addListener((o, old, v) -> checkUsername(uFeedback, v));

        // Password
        Label pLbl = fieldLabel("Password");
        PasswordRow passwordRow = createPasswordRow("At least 4 characters");
        PasswordField passField = passwordRow.hiddenField();

        Label strengthLbl = new Label();
        strengthLbl.setStyle("-fx-font-size: 11px;");

        // Confirm password
        Label cLbl = fieldLabel("Confirm Password");
        PasswordRow confirmPasswordRow = createPasswordRow("Re-enter your password");
        PasswordField confirmField = confirmPasswordRow.hiddenField();
        Label confirmFeedback = new Label();
        confirmFeedback.setStyle("-fx-font-size: 11px;");
        passField.textProperty().addListener((o, old, v) -> {
            updateStrength(strengthLbl, v);
            checkMatch(confirmFeedback, v, confirmField.getText());
        });
        confirmField.textProperty().addListener((o, old, v) ->
                checkMatch(confirmFeedback, passField.getText(), v));

        // Email
        Label eLbl = fieldLabel("Email Address");
        TextField emailField = inputField("your.email@example.com");
        Label emailFeedback = new Label();
        emailFeedback.setStyle("-fx-font-size: 11px;");
        emailField.textProperty().addListener((o, old, v) -> checkEmail(emailFeedback, v));

        // Phone Number
        Label phLbl = fieldLabel("Phone Number");
        TextField phoneField = inputField("+1 (555) 000-0000");
        Label phoneFeedback = new Label();
        phoneFeedback.setStyle("-fx-font-size: 11px;");
        phoneField.textProperty().addListener((o, old, v) -> checkPhone(phoneFeedback, v));

        // Role
        ToggleGroup roleGroup = new ToggleGroup();
        VBox roleBox = null;
        Label librarianNotice = new Label();

        if (!isFirstUser) {
            Label rLbl = fieldLabel("Account Type");
            RadioButton userRb = new RadioButton();
            userRb.setToggleGroup(roleGroup);
            userRb.setUserData(UserRole.USER);
            userRb.setSelected(true);
            VBox userOption = roleOption(
                    userRb,
                    "Library User",
                    "Borrow books, track due dates, and manage your requests.",
                    false);

            RadioButton libRb  = new RadioButton();
            libRb.setToggleGroup(roleGroup);
            libRb.setUserData(UserRole.LIBRARIAN);
            VBox librarianOption = roleOption(
                    libRb,
                    "Librarian",
                    "Manage circulation and catalog tasks after administrator approval.",
                    false);

            VBox radioRow = new VBox(10, userOption, librarianOption);
            radioRow.setAlignment(Pos.CENTER_LEFT);

            librarianNotice.setText("Librarian accounts start as inactive. " +
                    "An admin must approve before you can log in.");
            librarianNotice.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400E; " +
                    "-fx-background-color: #FEF3C7; -fx-background-radius: 8px; " +
                    "-fx-padding: 10 14; -fx-wrap-text: true;");
            librarianNotice.setWrapText(true);
            librarianNotice.setVisible(false);
            librarianNotice.setManaged(false);

            roleGroup.selectedToggleProperty().addListener((o, old, nw) -> {
                boolean lib = nw != null && nw.getUserData() == UserRole.LIBRARIAN;
                librarianNotice.setVisible(lib);
                librarianNotice.setManaged(lib);
                userOption.setStyle(roleOptionStyle(userRb.isSelected()));
                librarianOption.setStyle(roleOptionStyle(libRb.isSelected()));
            });
            userOption.setStyle(roleOptionStyle(true));
            librarianOption.setStyle(roleOptionStyle(false));

            roleBox = new VBox(8, rLbl, radioRow, librarianNotice);
        }

        // Error
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626;");
        errorLbl.setVisible(false);
        errorLbl.setWrapText(true);

        formBox.getChildren().addAll(
                uLbl, usernameField, uFeedback,
                pLbl, passwordRow.container(), strengthLbl,
                cLbl, confirmPasswordRow.container(), confirmFeedback,
                eLbl, emailField, emailFeedback,
                phLbl, phoneField, phoneFeedback
        );
        if (roleBox != null) formBox.getChildren().add(roleBox);
        formBox.getChildren().add(errorLbl);

        root.getChildren().addAll(headerBox, formBox);

        // Wrap in ScrollPane so content never hides the button bar
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        pane.setContent(scroll);

        // Buttons
        ButtonType createBt = new ButtonType("Create Account", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, createBt);
        Button okBtn = (Button) pane.lookupButton(createBt);
        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        stylePrimaryButton(okBtn, "Create Account");
        styleSecondaryButton(cancelBtn, "Cancel");

        dialog.setOnShown(event -> {
            if (pane.getScene() == null) {
                return;
            }
            Scene scene = pane.getScene();
            if (scene.getWindow() instanceof Stage stage) {
                stage.setMinHeight(720);
                stage.setMinWidth(520);
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        });

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String user = usernameField.getText().trim();
            String pass = passField.getText();
            String conf = confirmField.getText();

            if (user.isEmpty())        { err(errorLbl, "Username is required.");           ev.consume(); return; }
            if (user.length() < 3)     { err(errorLbl, "Username needs >= 3 characters.");  ev.consume(); return; }
            if (!user.matches("^[a-zA-Z0-9._-]+$"))
            { err(errorLbl, "Username: letters/numbers/. _ - only."); ev.consume(); return; }
            if (pass.isEmpty())        { err(errorLbl, "Password is required.");           ev.consume(); return; }
            if (pass.length() < 4)     { err(errorLbl, "Password needs >= 4 characters.");  ev.consume(); return; }
            if (!pass.equals(conf))    { err(errorLbl, "Passwords do not match.");         ev.consume(); return; }
            if (emailField.getText().trim().isEmpty()) {
                err(errorLbl, "Email address is required."); ev.consume(); return;
            }
            if (!User.isValidEmail(emailField.getText())) {
                err(errorLbl, "Enter a valid email address."); ev.consume(); return;
            }
            if (phoneField.getText().trim().isEmpty()) {
                err(errorLbl, "Mobile number is required."); ev.consume(); return;
            }
            if (!User.isValidContactNumber(phoneField.getText())) {
                err(errorLbl, "Enter a valid mobile number (10-15 digits)."); ev.consume(); return;
            }
            errorLbl.setVisible(false);
        });

        dialog.setResultConverter(bt -> {
            if (bt == createBt) {
                UserRole role = isFirstUser ? UserRole.ADMIN
                        : (roleGroup == null ? UserRole.USER
                           : (UserRole) roleGroup.getSelectedToggle().getUserData());
                return new RegistrationRequest(
                        usernameField.getText().trim(),
                        passField.getText(),
                        role,
                        role == UserRole.LIBRARIAN,
                        emailField.getText().trim(),
                        phoneField.getText().trim()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ─── Helpers
    private static Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary() + ";");
        return l;
    }
    private static TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: " + inputSurface() + "; -fx-border-color: " + borderColor() + "; " +
                "-fx-border-width: 1.5; -fx-border-radius: 10px; -fx-background-radius: 10px; " +
                "-fx-padding: 10 14; -fx-font-size: 14px; -fx-text-fill: " + textPrimary() + ";");
        return f;
    }
    private static PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 14px; -fx-padding: 10 14; -fx-text-fill: " + textPrimary() + ";");
        return f;
    }

    private static TextField visiblePassField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 14px; -fx-padding: 10 14; -fx-text-fill: " + textPrimary() + ";");
        return f;
    }

    private static PasswordRow createPasswordRow(String prompt) {
        PasswordField hiddenField = passField(prompt);
        TextField visibleField = visiblePassField(prompt);
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());

        Button toggleButton = new Button();
        toggleButton.setGraphic(AppTheme.createIcon(AppTheme.ICON_VISIBILITY, 16));
        toggleButton.getStyleClass().addAll("app-button", "btn-ghost", "password-toggle", "auth-password-toggle");
        toggleButton.setFocusTraversable(false);

        StackPane fieldStack = new StackPane(hiddenField, visibleField);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);

        HBox container = new HBox(fieldStack, toggleButton);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().addAll("input-with-icon", "password-split-container");

        PasswordRow row = new PasswordRow(hiddenField, visibleField, toggleButton, container);
        toggleButton.setOnAction(e -> togglePass(row));
        return row;
    }

    private static void togglePass(PasswordRow row) {
        boolean shown = row.visibleField().isVisible();
        row.hiddenField().setVisible(shown);
        row.hiddenField().setManaged(shown);
        row.visibleField().setVisible(!shown);
        row.visibleField().setManaged(!shown);
        row.toggleButton().setGraphic(AppTheme.createIcon(
                shown ? AppTheme.ICON_VISIBILITY : AppTheme.ICON_VISIBILITY_OFF, 16));
    }
    private static void updateStrength(Label lbl, String p) {
        if (p == null || p.isEmpty()) { lbl.setText(""); return; }
        int s = 0;
        if (p.length() >= 8)               s++;
        if (p.matches(".*[A-Z].*"))        s++;
        if (p.matches(".*[0-9].*"))        s++;
        if (p.matches(".*[^a-zA-Z0-9].*")) s++;
        String[] t = {"Weak","Fair","Good","Strong"};
        String[] c = {"#DC2626","#D97706","#2563EB","#16A34A"};
        int i = Math.min(s, t.length - 1);
        lbl.setText("Strength: " + t[i]);
        lbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + c[i] + "; -fx-font-weight:600;");
    }
    private static void checkUsername(Label lbl, String v) {
        if (v == null || v.length() < 3) { lbl.setText(""); return; }
        if (!v.matches("^[a-zA-Z0-9._-]+$")) {
            lbl.setText("Invalid characters");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
        } else {
            lbl.setText("Valid");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
        }
    }
     private static void checkMatch(Label lbl, String pass, String confirm) {
         if (confirm == null || confirm.isEmpty()) { lbl.setText(""); return; }
         if (pass.equals(confirm)) {
             lbl.setText("Passwords match");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
         } else {
             lbl.setText("Passwords do not match");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
         }
     }
     private static void checkEmail(Label lbl, String email) {
         if (email == null || email.isEmpty()) { lbl.setText(""); return; }
         if (User.isValidEmail(email)) {
             lbl.setText("Valid email");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
         } else {
             lbl.setText("Invalid email format");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
         }
     }
     private static void checkPhone(Label lbl, String phone) {
         if (phone == null || phone.isEmpty()) { lbl.setText(""); return; }
         if (User.isValidContactNumber(phone)) {
             lbl.setText("Valid phone number");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
         } else {
             lbl.setText("Invalid phone format (10-15 digits)");
             lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
         }
     }

    private static VBox roleOption(RadioButton radioButton, String title, String description, boolean selected) {
        radioButton.setStyle("-fx-padding: 2 0 0 0; -fx-font-size: 14px; " +
                "-fx-border-color: transparent; -fx-background-color: transparent;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + textPrimary() + ";");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textMuted() + ";");

        VBox textBox = new VBox(4, titleLabel, descLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(12, radioButton, textBox);
        row.setAlignment(Pos.TOP_LEFT);

        VBox wrapper = new VBox(row);
        wrapper.setPadding(new Insets(14));
        wrapper.setStyle(roleOptionStyle(selected));
        wrapper.setOnMouseClicked(event -> radioButton.setSelected(true));
        return wrapper;
    }

    private static String roleOptionStyle(boolean selected) {
        String border = selected
                ? (AppTheme.darkMode ? "#14B8A6" : "#0D9488")
                : borderColor();
        String background = selected
                ? (AppTheme.darkMode ? "rgba(20,184,166,0.10)" : "rgba(13,148,136,0.08)")
                : inputSurface();
        return "-fx-background-color: " + background + "; " +
                "-fx-background-radius: 12px; -fx-border-radius: 12px; " +
                "-fx-border-color: " + border + "; -fx-border-width: 1.5;";
    }
    private static void err(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true);
    }

    private static String inputSurface() {
        return AppTheme.darkMode ? "#1E293B" : "#F9FAFB";
    }

    private static String borderColor() {
        return AppTheme.darkMode ? "#334155" : "#D1D5DB";
    }

    private static String textPrimary() {
        return AppTheme.darkMode ? "#E2E8F0" : "#374151";
    }

    private static String textMuted() {
        return AppTheme.darkMode ? "#94A3B8" : "#64748B";
    }

    private static void stylePrimaryButton(Button button, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.setStyle("-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:600; -fx-font-size:14px; -fx-background-radius:10px; -fx-padding:10 24;");
    }

    private static void styleSecondaryButton(Button button, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#334155" : "#E5E7EB") + "; " +
                "-fx-text-fill:" + (AppTheme.darkMode ? "#F8FAFC" : "#1F2937") + "; " +
                "-fx-font-weight:600; -fx-font-size:14px; -fx-background-radius:10px; -fx-padding:10 22;");
    }

    public record RegistrationRequest(
            String username, String password, UserRole role, boolean pendingApproval,
            String email, String phoneNumber) {
        /** Convenience factory - not pending approval */
        public static RegistrationRequest of(String username, String password, UserRole role) {
            return new RegistrationRequest(username, password, role, false, null, null);
        }
    }
}

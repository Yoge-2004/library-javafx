package com.example.application.ui;

import com.example.entities.AppConfiguration;
import com.example.entities.UserRole;
import com.example.services.AppConfigurationService;
import com.example.services.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    public static Optional<RegistrationRequest> show(Stage owner, boolean isFirstUser) {
        Dialog<RegistrationRequest> dialog = new Dialog<>();
        dialog.setTitle(isFirstUser ? "Create Administrator Account" : "Sign Up");
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(500);
        pane.setMinWidth(460);

        // Use Platform.runLater to correctly size dialog to content
        javafx.application.Platform.runLater(() -> {
            if (dialog.getDialogPane().getScene() != null && dialog.getDialogPane().getScene().getWindow() != null) {
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });

        AppConfiguration cfg = AppConfigurationService.getConfiguration();

        VBox root = new VBox(0);
        root.setFillWidth(true);

        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(28, 28, 20, 28));
        headerBox.setStyle("-fx-background-color: #0F172A;");

        Label emoji = new Label(isFirstUser ? "🏛️" : "👤");
        emoji.setStyle("-fx-font-size: 32px;");

        Label titleLbl = new Label(isFirstUser ? "Welcome to Library OS" : "Sign Up");
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label libLbl = new Label(cfg.getLibraryName() + " . " + cfg.getBranchName());
        libLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #14B8A6; -fx-font-weight: 700;");

        Label subLbl = new Label(isFirstUser
                ? "You are the first user — your account will be Administrator."
                : "Fill in your details to create an account.");
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        subLbl.setWrapText(true);

        headerBox.getChildren().addAll(emoji, titleLbl, libLbl, subLbl);

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
        PasswordField passField    = passField("At least 4 characters");
        TextField     passVisible  = inputField("At least 4 characters");
        passVisible.setVisible(false); passVisible.setManaged(false);
        passVisible.textProperty().bindBidirectional(passField.textProperty());
        Button showHidePass = new Button("👁");
        showHidePass.setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #D1D5DB; " +
                "-fx-border-width: 0 0 0 1; -fx-cursor: hand; -fx-background-radius: 0 10 10 0; " +
                "-fx-border-radius: 0; -fx-font-size: 14px; -fx-padding: 0 10;");
        showHidePass.setOnAction(e -> togglePass(passField, passVisible, showHidePass));
        StackPane passStack = new StackPane(passField, passVisible);
        HBox.setHgrow(passStack, Priority.ALWAYS);
        HBox passRow = new HBox(passStack, showHidePass);
        passRow.setStyle("-fx-border-color: #D1D5DB; -fx-border-width: 1.5; " +
                "-fx-border-radius: 10px; -fx-background-radius: 10px; -fx-background-color: #F9FAFB;");

        Label strengthLbl = new Label();
        strengthLbl.setStyle("-fx-font-size: 11px;");
        passField.textProperty().addListener((o, old, v) -> updateStrength(strengthLbl, v));

        // Confirm password
        Label cLbl = fieldLabel("Confirm Password");
        PasswordField confirmField = passField("Re-enter your password");
        Label confirmFeedback = new Label();
        confirmFeedback.setStyle("-fx-font-size: 11px;");
        confirmField.textProperty().addListener((o, old, v) ->
                checkMatch(confirmFeedback, passField.getText(), v));

        // Role
        ToggleGroup roleGroup = new ToggleGroup();
        VBox roleBox = null;
        Label librarianNotice = new Label();

        if (!isFirstUser) {
            Label rLbl = fieldLabel("Account Type");
            RadioButton userRb = new RadioButton("Library User");
            userRb.setToggleGroup(roleGroup);
            userRb.setUserData(UserRole.USER);
            userRb.setSelected(true);
            userRb.setStyle("-fx-font-size: 14px;");

            RadioButton libRb  = new RadioButton("Librarian  (requires admin approval)");
            libRb.setToggleGroup(roleGroup);
            libRb.setUserData(UserRole.LIBRARIAN);
            libRb.setStyle("-fx-font-size: 14px;");

            HBox radioRow = new HBox(20, userRb, libRb);
            radioRow.setAlignment(Pos.CENTER_LEFT);

            librarianNotice.setText("ℹ  Librarian accounts start as inactive. " +
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
            });

            roleBox = new VBox(8, rLbl, radioRow, librarianNotice);
        }

        // Error
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #DC2626;");
        errorLbl.setVisible(false);
        errorLbl.setWrapText(true);

        formBox.getChildren().addAll(
                uLbl, usernameField, uFeedback,
                pLbl, passRow, strengthLbl,
                cLbl, confirmField, confirmFeedback
        );
        if (roleBox != null) formBox.getChildren().add(roleBox);
        formBox.getChildren().add(errorLbl);

        root.getChildren().addAll(headerBox, formBox);
        pane.setContent(root);

        // Buttons
        ButtonType createBt = new ButtonType("Sign Up", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, createBt);
        Button okBtn = (Button) pane.lookupButton(createBt);
        okBtn.setStyle("-fx-background-color: #0D9488; -fx-text-fill: white; " +
                "-fx-font-weight: 600; -fx-font-size: 14px; " +
                "-fx-background-radius: 10px; -fx-padding: 10 24;");

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String user = usernameField.getText().trim();
            String pass = passField.getText();
            String conf = confirmField.getText();

            if (user.isEmpty())        { err(errorLbl, "Username is required.");           ev.consume(); return; }
            if (user.length() < 3)     { err(errorLbl, "Username needs ≥ 3 characters.");  ev.consume(); return; }
            if (!user.matches("^[a-zA-Z0-9._-]+$"))
            { err(errorLbl, "Username: letters/numbers/. _ - only."); ev.consume(); return; }
            if (pass.isEmpty())        { err(errorLbl, "Password is required.");           ev.consume(); return; }
            if (pass.length() < 4)     { err(errorLbl, "Password needs ≥ 4 characters.");  ev.consume(); return; }
            if (!pass.equals(conf))    { err(errorLbl, "Passwords do not match.");         ev.consume(); return; }
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
                        role == UserRole.LIBRARIAN
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ─── Helpers
    private static Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #374151;");
        return l;
    }
    private static TextField inputField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #D1D5DB; " +
                "-fx-border-width: 1.5; -fx-border-radius: 10px; -fx-background-radius: 10px; " +
                "-fx-padding: 10 14; -fx-font-size: 14px;");
        return f;
    }
    private static PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                "-fx-font-size: 14px; -fx-padding: 10 14;");
        return f;
    }
    private static void togglePass(PasswordField pf, TextField tf, Button btn) {
        boolean shown = tf.isVisible();
        pf.setVisible(shown); pf.setManaged(shown);
        tf.setVisible(!shown); tf.setManaged(!shown);
        btn.setText(shown ? "👁" : "🙈");
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
            lbl.setText("✕ Invalid characters");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
        } else {
            lbl.setText("✓ Valid");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
        }
    }
    private static void checkMatch(Label lbl, String pass, String confirm) {
        if (confirm == null || confirm.isEmpty()) { lbl.setText(""); return; }
        if (pass.equals(confirm)) {
            lbl.setText("✓ Passwords match");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#16A34A;");
        } else {
            lbl.setText("✕ Passwords do not match");
            lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626;");
        }
    }
    private static void err(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true);
    }

    public record RegistrationRequest(
            String username, String password, UserRole role, boolean pendingApproval) {
        /** Convenience factory - not pending approval */
        public static RegistrationRequest of(String username, String password, UserRole role) {
            return new RegistrationRequest(username, password, role, false);
        }
    }
}
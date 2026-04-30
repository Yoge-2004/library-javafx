package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.entities.User;
import com.example.entities.UserRole;
import com.example.exceptions.UserException;
import com.example.services.UserService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * User account management dialogs.
 *
 * Uses GridPane directly as DialogPane content (same pattern as original)
 * to avoid blank-dialog rendering issues with VBox wrappers.
 */
public class UserAccountDialogs {

    // ── Profile editor (available to ALL users) ───────────────────

    public static boolean showProfileEditor(Stage owner, String userId) {
        User user;
        try { user = UserService.getUserById(userId); }
        catch (Exception e) { return false; }

        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Edit Profile");
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(460);
        pane.setPrefHeight(380);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField firstField   = field(user.getFirstName());
        TextField lastField    = field(user.getLastName());
        TextField emailField   = field(user.getEmail());
        TextField contactField = field(user.getContactNumber());

        grid.add(bold("First Name"),  0, 0); grid.add(firstField,   1, 0);
        grid.add(bold("Last Name"),   0, 1); grid.add(lastField,    1, 1);
        grid.add(bold("Email"),       0, 2); grid.add(emailField,   1, 2);
        grid.add(bold("Contact"),     0, 3); grid.add(contactField, 1, 3);

        Label err = errorLabel();
        grid.add(err, 0, 4, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(120);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        pane.setContent(grid);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        styleOkBtn(pane, "Save Changes");
        styleSecondaryBtn(pane, ButtonType.CANCEL, "Cancel");

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return false;
            try {
                user.setFirstName(firstField.getText());
                user.setLastName(lastField.getText());
                user.setEmail(emailField.getText());
                user.setContactNumber(contactField.getText());
                UserService.updateUser(user);
                return true;
            } catch (Exception e) {
                err.setText(e.getMessage()); err.setVisible(true);
                return false;
            }
        });
        return dlg.showAndWait().orElse(false);
    }

    // ── Password editor (available to ALL users) ──────────────────

    public static boolean showPasswordEditor(Stage owner, String userId) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Change Password");
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(460);
        pane.setPrefHeight(340);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        PasswordField curField  = passField("Current password");
        PasswordField newField  = passField("New password (min 4 chars)");
        PasswordField confField = passField("Re-enter new password");
        Label err = errorLabel();

        grid.add(bold("Current Password"), 0, 0); grid.add(curField,  1, 0);
        grid.add(bold("New Password"),     0, 1); grid.add(newField,  1, 1);
        grid.add(bold("Confirm"),          0, 2); grid.add(confField, 1, 2);
        grid.add(err,                      0, 3, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(140);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        pane.setContent(grid);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button ok = styleOkBtn(pane, "Update Password");
        styleSecondaryBtn(pane, ButtonType.CANCEL, "Cancel");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            try {
                User u = UserService.getUserById(userId);
                if (!u.getPassword().equals(curField.getText())) {
                    err.setText("Current password is incorrect"); err.setVisible(true); ev.consume(); return;
                }
                if (newField.getText().length() < 4) {
                    err.setText("New password needs 4+ characters"); err.setVisible(true); ev.consume(); return;
                }
                if (!newField.getText().equals(confField.getText())) {
                    err.setText("Passwords do not match"); err.setVisible(true); ev.consume(); return;
                }
            } catch (Exception e) { err.setText(e.getMessage()); err.setVisible(true); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return false;
            try {
                User u = UserService.getUserById(userId);
                u.setPassword(newField.getText());
                UserService.updateUser(u);
                return true;
            } catch (Exception e) { return false; }
        });
        return dlg.showAndWait().orElse(false);
    }

    // ── User Management (admin/librarian) ────────────────────────

    public static void showUserManagement(Stage owner, String currentUserId, ToastDisplay toastDisplay) {
        boolean isAdmin = UserService.isAdmin(currentUserId);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("User Management");
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(840);
        pane.setPrefHeight(560);

        // Header + Add button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 12, 0));
        Label heading = new Label("User Management");
        heading.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:" +
                (AppTheme.darkMode ? "#F8FAFC" : "#0F172A") + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = new Button("+ Add User");
        addBtn.setStyle("-fx-background-color:#0D9488;-fx-text-fill:white;" +
                "-fx-font-weight:600;-fx-background-radius:8px;-fx-padding:8 18;-fx-cursor:hand;");
        topBar.getChildren().addAll(heading, sp, addBtn);

        // Table
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(400);

        TableColumn<User, String> uCol = col("Username",  u -> u.getUserId(), 130);
        TableColumn<User, String> nCol = col("Name",      u -> u.getFullName(), 160);
        TableColumn<User, String> rCol = col("Role",      u -> u.getRole().getDisplayName(), 110);

        TableColumn<User, Void> sCol = new TableColumn<>("Status");
        sCol.setPrefWidth(140);
        sCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                { setGraphic(null); return; }
                User u = getTableRow().getItem();
                String txt = u.getUserId().equals(currentUserId) ? "Active (You)"
                        : u.isActive() ? "Active" : "Pending Approval";
                Label chip = new Label(txt);
                chip.setStyle("-fx-background-color:" + (u.isActive() ? "#16A34A" : "#D97706")
                        + ";-fx-text-fill:white;-fx-background-radius:20px;-fx-padding:3 10;-fx-font-size:12px;");
                setGraphic(chip); setText(null);
            }
        });

        TableColumn<User, Void> aCol = new TableColumn<>("Actions");
        aCol.setMinWidth(108); aCol.setPrefWidth(108); aCol.setMaxWidth(108);
        aCol.setCellFactory(c -> new TableCell<>() {
            final Button apprBtn = actionIconBtn(AppTheme.ICON_CHECK, "Approve account", "#16A34A");
            final Button editBtn = actionIconBtn(AppTheme.ICON_EDIT, "Edit user", "#3B82F6");
            final Button delBtn  = actionIconBtn(AppTheme.ICON_DELETE, "Delete user", "#DC2626");
            {
                apprBtn.setOnAction(e -> {
                    User u = getTableRow().getItem(); if (u == null) return;
                    u.setActive(true);
                    try {
                        UserService.updateUser(u);
                        reload(table);
                        notifySuccess(toastDisplay, "User approved: " + u.getUserId());
                    } catch (Exception ex) {
                        notifyError(toastDisplay, ex.getMessage());
                    }
                });
                editBtn.setOnAction(e -> {
                    User u = getTableRow().getItem(); if (u == null) return;
                    editUser(owner, u, currentUserId, isAdmin);
                    reload(table);
                });
                delBtn.setOnAction(e -> {
                    User u = getTableRow().getItem();
                    if (u == null || u.getUserId().equals(currentUserId)) return;
                    if (!isAdmin && u.getRole().isAdmin()) {
                        notifyError(toastDisplay, "Only administrators can delete admin accounts."); return;
                    }
                    Alert conf = new Alert(Alert.AlertType.WARNING,
                            "Delete \"" + u.getUserId() + "\"?",
                            new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE),
                            ButtonType.CANCEL);
                    conf.setTitle("Delete User");
                    AppTheme.applyTheme(conf.getDialogPane());
                    conf.showAndWait()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .ifPresent(bt -> {
                                try {
                                    UserService.deleteUser(u.getUserId());
                                    reload(table);
                                    notifySuccess(toastDisplay, "User deleted: " + u.getUserId());
                                } catch (Exception ex) {
                                    notifyError(toastDisplay, ex.getMessage());
                                }
                            });
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                { setGraphic(null); return; }
                User u = getTableRow().getItem();
                HBox box = new HBox(2);
                if (!u.isActive()) box.getChildren().add(apprBtn);
                box.getChildren().add(editBtn);
                if (!u.getUserId().equals(currentUserId)
                        && (isAdmin || !u.getRole().isAdmin()))
                    box.getChildren().add(delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        table.getColumns().addAll(uCol, nCol, rCol, sCol, aCol);
        reload(table);

        addBtn.setOnAction(e -> {
            RegistrationDialog.show(owner, false).ifPresent(req -> {
                try {
                    if (UserService.userExists(req.username())) {
                        notifyError(toastDisplay, "Username already taken."); return;
                    }
                    if (UserService.emailExists(req.email())) {
                        notifyError(toastDisplay, "Email address already in use."); return;
                    }
                    UserService.createUser(req.username(), req.password(), req.role());
                    User created = UserService.getUserById(req.username());
                    created.setEmail(req.email());
                    created.setContactNumber(req.phoneNumber());
                    created.setActive(!req.pendingApproval());
                    UserService.updateUser(created);
                    reload(table);
                    notifySuccess(toastDisplay, "User created: " + req.username());
                } catch (Exception ex) { notifyError(toastDisplay, ex.getMessage()); }
            });
        });

        VBox content = new VBox(topBar, table);
        content.setPadding(new Insets(20));
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        styleSecondaryBtn(pane, ButtonType.CLOSE, "Close");
        dlg.showAndWait();
    }

    // ── Edit user (admin/librarian) ───────────────────────────────

    private static void editUser(Stage owner, User user, String currentUserId, boolean isAdmin) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Edit: " + user.getUserId());
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(460);
        pane.setPrefHeight(420);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField firstField   = field(user.getFirstName());
        TextField lastField    = field(user.getLastName());
        TextField emailField   = field(user.getEmail());
        TextField contactField = field(user.getContactNumber());
        boolean isSelf = user.getUserId().equals(currentUserId);

        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(isAdmin
                ? new UserRole[]{UserRole.USER, UserRole.LIBRARIAN, UserRole.ADMIN}
                : new UserRole[]{UserRole.USER, UserRole.LIBRARIAN});
        roleBox.setValue(user.getRole());
        roleBox.setMaxWidth(Double.MAX_VALUE);

        CheckBox activeCheck = new CheckBox("Account is active");
        activeCheck.setSelected(user.isActive());
        activeCheck.setStyle("-fx-text-fill:" + (AppTheme.darkMode ? "#E2E8F0" : "#374151") + ";");

        Label err = errorLabel();

        int row = 0;
        grid.add(bold("First Name"), 0, row); grid.add(firstField,   1, row++);
        grid.add(bold("Last Name"),  0, row); grid.add(lastField,    1, row++);
        grid.add(bold("Email"),      0, row); grid.add(emailField,   1, row++);
        grid.add(bold("Contact"),    0, row); grid.add(contactField, 1, row++);
        if (!isSelf) {
            grid.add(bold("Role"),   0, row); grid.add(roleBox,      1, row++);
            grid.add(activeCheck,    0, row, 2, 1); row++;
        }
        grid.add(err, 0, row, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(120);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        pane.setContent(grid);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        styleOkBtn(pane, "Save");
        styleSecondaryBtn(pane, ButtonType.CANCEL, "Cancel");

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return false;
            try {
                user.setFirstName(firstField.getText());
                user.setLastName(lastField.getText());
                user.setEmail(emailField.getText());
                user.setContactNumber(contactField.getText());
                if (!isSelf) { user.setRole(roleBox.getValue()); user.setActive(activeCheck.isSelected()); }
                UserService.updateUser(user);
                return true;
            } catch (Exception e) { err.setText(e.getMessage()); return false; }
        });
        dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static void reload(TableView<User> t) {
        t.setItems(FXCollections.observableArrayList(UserService.getAllUsers()));
    }

    private static TableColumn<User, String> col(String name,
                                                 java.util.function.Function<User, String> fn, double w) {
        TableColumn<User, String> c = new TableColumn<>(name);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private static Button actionIconBtn(String iconPath, String tooltip, String color) {
        Button b = new Button();
        var icon = AppTheme.createIcon(iconPath, 14);
        icon.setStyle("-fx-fill:white;");
        b.setGraphic(icon);
        b.setTooltip(AppTheme.createTooltip(tooltip));
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-cursor:hand;" +
                "-fx-background-radius:8px; -fx-padding:5; -fx-min-width:26px; -fx-pref-width:26px; " +
                "-fx-max-width:26px; -fx-min-height:26px; -fx-pref-height:26px; -fx-max-height:26px;");
        return b;
    }

    private static Label bold(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:#374151;");
        return l;
    }

    private static TextField field(String val) {
        TextField f = new TextField(val != null ? val : "");
        f.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#1E293B" : "#F9FAFB") +
                ";-fx-border-color:" + (AppTheme.darkMode ? "#334155" : "#D1D5DB") + ";" +
                "-fx-border-width:1.5;-fx-border-radius:8px;-fx-background-radius:8px;" +
                "-fx-padding:9 12;-fx-font-size:14px;-fx-text-fill:" +
                (AppTheme.darkMode ? "#E2E8F0" : "#111827") + ";");
        return f;
    }

    private static PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#1E293B" : "#F9FAFB") +
                ";-fx-border-color:" + (AppTheme.darkMode ? "#334155" : "#D1D5DB") + ";" +
                "-fx-border-width:1.5;-fx-border-radius:8px;-fx-background-radius:8px;" +
                "-fx-padding:9 12;-fx-font-size:14px;-fx-text-fill:" +
                (AppTheme.darkMode ? "#E2E8F0" : "#111827") + ";");
        return f;
    }

    private static Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12px;");
        l.setVisible(false);
        return l;
    }

    private static Button styleOkBtn(DialogPane pane, String text) {
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.setText(text);
            ok.setStyle("-fx-background-color:#0D9488;-fx-text-fill:white;" +
                    "-fx-font-weight:600;-fx-font-size:14px;" +
                    "-fx-background-radius:8px;-fx-padding:9 22;");
        }
        return ok;
    }

    private static Button styleSecondaryBtn(DialogPane pane, ButtonType buttonType, String text) {
        Button button = (Button) pane.lookupButton(buttonType);
        if (button != null) {
            button.setText(text);
            button.setStyle("-fx-background-color:" + (AppTheme.darkMode ? "#334155" : "#E5E7EB") + ";" +
                    "-fx-text-fill:" + (AppTheme.darkMode ? "#F8FAFC" : "#1F2937") + ";" +
                    "-fx-font-weight:600; -fx-font-size:14px; -fx-background-radius:8px; -fx-padding:9 18;");
        }
        return button;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        AppTheme.applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    private static void notifySuccess(ToastDisplay toastDisplay, String message) {
        if (toastDisplay != null) {
            toastDisplay.showSuccess(message);
        }
    }

    private static void notifyError(ToastDisplay toastDisplay, String message) {
        if (toastDisplay != null) {
            toastDisplay.showError(message);
            return;
        }
        showAlert(message);
    }
}

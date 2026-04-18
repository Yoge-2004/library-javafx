package com.example.application.ui;

import com.example.entities.User;
import com.example.entities.UserRole;
import com.example.exceptions.UserException;
import com.example.services.UserService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * User account management dialogs.
 * - Profile/Password editing for all users.
 * - Admin-only: User Management with approve/edit/delete.
 * - Librarians cannot delete admins.
 */
public class UserAccountDialogs {

    // ── Profile editor (all users) ────────────────────────────────

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
        pane.setPrefWidth(440);
        pane.setPrefHeight(360);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));

        Label heading = new Label("Edit Profile");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#0F172A;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        TextField firstField   = field(user.getFirstName());
        TextField lastField    = field(user.getLastName());
        TextField emailField   = field(user.getEmail());
        TextField contactField = field(user.getContactNumber());

        grid.addRow(0, lbl("First Name"),     firstField);
        grid.addRow(1, lbl("Last Name"),      lastField);
        grid.addRow(2, lbl("Email"),          emailField);
        grid.addRow(3, lbl("Contact Number"), contactField);

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12px;");
        errLbl.setVisible(false);
        grid.add(errLbl, 0, 4, 2, 1);

        root.getChildren().addAll(heading, grid);
        pane.setContent(root);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        ok.setStyle(primaryStyle());
        ok.setText("Save Changes");

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
                errLbl.setText(e.getMessage());
                errLbl.setVisible(true);
                return false;
            }
        });
        return dlg.showAndWait().orElse(false);
    }

    // ── Password editor (all users) ──────────────────────────────

    public static boolean showPasswordEditor(Stage owner, String userId) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Change Password");
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(440);
        pane.setPrefHeight(320);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));

        Label heading = new Label("Change Password");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#0F172A;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        PasswordField currentField = passField("Current password");
        PasswordField newField     = passField("New password (min 4 chars)");
        PasswordField confirmField = passField("Re-enter new password");

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12px;");
        errLbl.setVisible(false);

        grid.addRow(0, lbl("Current Password"), currentField);
        grid.addRow(1, lbl("New Password"),      newField);
        grid.addRow(2, lbl("Confirm Password"),  confirmField);
        grid.add(errLbl, 0, 3, 2, 1);

        root.getChildren().addAll(heading, grid);
        pane.setContent(root);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        ok.setStyle(primaryStyle());
        ok.setText("Update Password");

        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            try {
                User u = UserService.getUserById(userId);
                if (!u.getPassword().equals(currentField.getText())) {
                    errLbl.setText("Current password is incorrect"); errLbl.setVisible(true); ev.consume(); return;
                }
                if (newField.getText().length() < 4) {
                    errLbl.setText("New password must be at least 4 characters"); errLbl.setVisible(true); ev.consume(); return;
                }
                if (!newField.getText().equals(confirmField.getText())) {
                    errLbl.setText("Passwords do not match"); errLbl.setVisible(true); ev.consume(); return;
                }
            } catch (Exception e) {
                errLbl.setText(e.getMessage()); errLbl.setVisible(true); ev.consume();
            }
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

    public static void showUserManagement(Stage owner, String currentUserId) {
        UserRole currentRole = UserService.getUserRole(currentUserId);
        boolean isAdmin = currentRole != null && currentRole.isAdmin();

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("User Management");
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(820);
        pane.setPrefHeight(540);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));

        // Header + Add button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label heading = new Label("User Management");
        heading.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#0F172A;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = AppTheme.createIconTextButton("Add User", AppTheme.ICON_ADD, AppTheme.ButtonStyle.PRIMARY);
        topBar.getChildren().addAll(heading, sp, addBtn);

        // Table
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<User, String> userCol   = strCol("Username", u -> u.getUserId(), 130);
        TableColumn<User, String> nameCol   = strCol("Name",     u -> u.getFullName(), 150);
        TableColumn<User, String> roleCol   = strCol("Role",     u -> u.getRole().getDisplayName(), 110);

        // Status column with chip
        TableColumn<User, Void> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(130);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                String text = u.getUserId().equals(currentUserId) ? "Active (You)"
                        : u.isActive() ? "Active" : "Pending Approval";
                String sc = u.isActive() ? "chip-success" : "chip-warning";
                Label chip = new Label(text);
                chip.getStyleClass().addAll("chip", sc);
                setGraphic(chip); setText(null);
            }
        });

        // Actions column - wider to avoid truncation
        TableColumn<User, Void> actCol = new TableColumn<>("Actions");
        actCol.setMinWidth(220);
        actCol.setPrefWidth(220);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button approveBtn = btn("Approve", "#16A34A");
            final Button editBtn    = btn("Edit",    "#3B82F6");
            final Button deleteBtn  = btn("Delete",  "#DC2626");

            {
                approveBtn.setOnAction(e -> {
                    User u = getTableRow().getItem();
                    if (u == null) return;
                    u.setActive(true);
                    // Set correct role if it was a librarian pending
                    try {
                        UserService.updateUser(u);
                        refreshTable(table);
                    } catch (Exception ex) { alert("Approve failed: " + ex.getMessage()); }
                });
                editBtn.setOnAction(e -> {
                    User u = getTableRow().getItem();
                    if (u == null) return;
                    showEditUserDialog(owner, u, currentUserId, isAdmin);
                    refreshTable(table);
                });
                deleteBtn.setOnAction(e -> {
                    User u = getTableRow().getItem();
                    if (u == null || u.getUserId().equals(currentUserId)) return;
                    // Librarians cannot delete admins
                    if (!isAdmin && u.getRole().isAdmin()) {
                        alert("Only administrators can delete admin accounts.");
                        return;
                    }
                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete user \"" + u.getUserId() + "\"? This cannot be undone.",
                            ButtonType.YES, ButtonType.NO);
                    conf.setTitle("Delete User");
                    conf.showAndWait().filter(bt -> bt == ButtonType.YES).ifPresent(bt -> {
                        try { UserService.deleteUser(u.getUserId()); refreshTable(table); }
                        catch (Exception ex) { alert(ex.getMessage()); }
                    });
                });
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                HBox box = new HBox(6);
                if (!u.isActive()) box.getChildren().add(approveBtn);
                box.getChildren().add(editBtn);
                if (!u.getUserId().equals(currentUserId)) {
                    // Librarians cannot delete admins
                    if (isAdmin || !u.getRole().isAdmin()) {
                        box.getChildren().add(deleteBtn);
                    }
                }
                setGraphic(box);
            }
        });

        table.getColumns().addAll(userCol, nameCol, roleCol, statusCol, actCol);
        refreshTable(table);

        addBtn.setOnAction(e -> {
            RegistrationDialog.show(owner, false).ifPresent(req -> {
                try {
                    // Check uniqueness first
                    if (UserService.userExists(req.username())) {
                        alert("Username \"" + req.username() + "\" is already taken.");
                        return;
                    }
                    UserService.createUser(req.username(), req.password(), req.role());
                    if (req.pendingApproval()) {
                        User created = UserService.getUserById(req.username());
                        created.setActive(false);
                        UserService.updateUser(created);
                    }
                    refreshTable(table);
                } catch (Exception ex) { alert(ex.getMessage()); }
            });
        });

        root.getChildren().addAll(topBar, table);
        pane.setContent(root);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    // ── Edit user dialog (admin/librarian) ──────────────────────

    private static void showEditUserDialog(Stage owner, User user,
                                           String currentUserId, boolean isAdmin) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Edit: " + user.getUserId());
        dlg.initOwner(owner);
        dlg.setResizable(true);

        DialogPane pane = dlg.getDialogPane();
        AppTheme.applyTheme(pane);
        pane.setPrefWidth(460);
        pane.setPrefHeight(420);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));

        Label heading = new Label("Edit User: " + user.getUserId());
        heading.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#0F172A;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        TextField firstField   = field(user.getFirstName());
        TextField lastField    = field(user.getLastName());
        TextField emailField   = field(user.getEmail());
        TextField contactField = field(user.getContactNumber());

        boolean isSelf = user.getUserId().equals(currentUserId);

        ComboBox<UserRole> roleBox = new ComboBox<>();
        if (isAdmin) {
            roleBox.getItems().addAll(UserRole.USER, UserRole.LIBRARIAN, UserRole.ADMIN);
        } else {
            roleBox.getItems().addAll(UserRole.USER, UserRole.LIBRARIAN);
        }
        roleBox.setValue(user.getRole());

        CheckBox activeCheck = new CheckBox("Account is active");
        activeCheck.setSelected(user.isActive());

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill:#DC2626; -fx-font-size:12px;");

        int row = 0;
        grid.addRow(row++, lbl("First Name"),   firstField);
        grid.addRow(row++, lbl("Last Name"),    lastField);
        grid.addRow(row++, lbl("Email"),        emailField);
        grid.addRow(row++, lbl("Contact"),      contactField);
        if (!isSelf) {
            grid.addRow(row++, lbl("Role"),     roleBox);
            grid.addRow(row++, lbl("Status"),   activeCheck);
        }
        grid.add(errLbl, 0, row, 2, 1);

        root.getChildren().addAll(heading, grid);
        pane.setContent(root);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        ok.setStyle(primaryStyle());
        ok.setText("Save");

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return false;
            try {
                user.setFirstName(firstField.getText());
                user.setLastName(lastField.getText());
                user.setEmail(emailField.getText());
                user.setContactNumber(contactField.getText());
                if (!isSelf) {
                    user.setRole(roleBox.getValue());
                    user.setActive(activeCheck.isSelected());
                }
                UserService.updateUser(user);
                return true;
            } catch (Exception e) {
                errLbl.setText(e.getMessage()); return false;
            }
        });
        dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static void refreshTable(TableView<User> table) {
        table.setItems(FXCollections.observableArrayList(UserService.getAllUsers()));
    }

    private static TableColumn<User, String> strCol(String name,
                                                    java.util.function.Function<User, String> fn, double w) {
        TableColumn<User, String> c = new TableColumn<>(name);
        c.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private static Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                "-fx-font-size:12px; -fx-background-radius:6px; " +
                "-fx-padding:4 10; -fx-cursor:hand; -fx-min-width:60px;");
        return b;
    }

    private static Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#374151;");
        return l;
    }

    private static TextField field(String val) {
        TextField f = new TextField(val != null ? val : "");
        f.setStyle("-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:9 12; -fx-font-size:14px;");
        return f;
    }

    private static PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:#F9FAFB; -fx-border-color:#D1D5DB; " +
                "-fx-border-width:1.5; -fx-border-radius:10px; -fx-background-radius:10px; " +
                "-fx-padding:9 12; -fx-font-size:14px;");
        return f;
    }

    private static String primaryStyle() {
        return "-fx-background-color:#0D9488; -fx-text-fill:white; " +
                "-fx-font-weight:600; -fx-font-size:14px; " +
                "-fx-background-radius:10px; -fx-padding:10 24;";
    }

    private static void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Error"); a.showAndWait();
    }
}
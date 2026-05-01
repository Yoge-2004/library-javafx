package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.entities.User;
import com.example.services.UserService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class UserManagementView extends BorderPane {

    private final String currentUserId;
    private final boolean isAdmin;
    private final ToastDisplay toast;
    private TableView<User> table;
    private TextField searchField;

    public UserManagementView(String currentUserId, ToastDisplay toast) {
        this.currentUserId = currentUserId;
        this.isAdmin = UserService.isAdmin(currentUserId);
        this.toast = toast;
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: " + pageBackground() + ";");
        setPadding(new Insets(24));

        VBox topBar = new VBox(20);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        // Title section
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconBadge = new StackPane(AppTheme.createIcon(AppTheme.ICON_USER, 20));
        iconBadge.setMinSize(40, 40);
        iconBadge.setStyle("-fx-background-color: #8B5CF622; -fx-background-radius: 12px;");
        
        VBox titleTxt = new VBox(2);
        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + textPrimary() + ";");
        Label sub = new Label("Manage access control, user roles and pending registrations");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        titleTxt.getChildren().addAll(title, sub);
        
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        
        Button addBtn = AppTheme.createIconTextButton("Add New User", AppTheme.ICON_ADD, AppTheme.ButtonStyle.PRIMARY);
        addBtn.setOnAction(e -> handleAddUser());
        
        titleRow.getChildren().addAll(iconBadge, titleTxt, sp1, addBtn);

        // Search bar
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        
        searchField = new TextField();
        searchField.setPromptText("Search by username, name or email...");
        searchField.setPrefWidth(350);
        searchField.setPrefHeight(40);
        searchField.setStyle(inputStyle());
        searchField.textProperty().addListener((o, old, v) -> reload());
        
        searchRow.getChildren().addAll(AppTheme.createIcon(AppTheme.ICON_SEARCH, 18), searchField);

        topBar.getChildren().addAll(titleRow, searchRow);
        setTop(topBar);

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("table-view");
        
        TableColumn<User, String> uCol = col("Username",  u -> u.getUserId(), 130);
        TableColumn<User, String> nCol = col("Full Name", u -> u.getFullName(), 180);
        TableColumn<User, String> rCol = col("Role",      u -> u.getRole().getDisplayName(), 110);
        
        TableColumn<User, Void> sCol = new TableColumn<>("Status");
        sCol.setPrefWidth(140);
        sCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                String txt = u.getUserId().equals(currentUserId) ? "Active (You)" : u.isActive() ? "Active" : "Pending";
                Label chip = new Label(txt);
                chip.getStyleClass().addAll("chip", u.isActive() ? "chip-success" : "chip-warning");
                setGraphic(chip);
            }
        });

        TableColumn<User, Void> aCol = new TableColumn<>("Actions");
        aCol.setPrefWidth(120);
        aCol.setCellFactory(c -> new TableCell<>() {
            final Button apprBtn = actionBtn(AppTheme.ICON_CHECK, "Approve", "#16A34A");
            final Button editBtn = actionBtn(AppTheme.ICON_EDIT, "Edit", "#3B82F6");
            final Button delBtn  = actionBtn(AppTheme.ICON_DELETE, "Delete", "#DC2626");
            
            {
                apprBtn.setOnAction(e -> approveUser(getTableRow().getItem()));
                editBtn.setOnAction(e -> editUser(getTableRow().getItem()));
                delBtn.setOnAction(e -> deleteUser(getTableRow().getItem()));
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER);
                if (!u.isActive()) box.getChildren().add(apprBtn);
                box.getChildren().add(editBtn);
                if (!u.getUserId().equals(currentUserId) && (isAdmin || !u.getRole().isAdmin())) box.getChildren().add(delBtn);
                setGraphic(box);
            }
        });

        table.getColumns().add(uCol);
        table.getColumns().add(nCol);
        table.getColumns().add(rCol);
        table.getColumns().add(sCol);
        table.getColumns().add(aCol);
        setCenter(table);
        reload();
    }

    private void reload() {
        String q = searchField.getText().trim().toLowerCase();
        List<User> list = UserService.getAllUsers().stream()
                .filter(u -> q.isEmpty() 
                        || u.getUserId().toLowerCase().contains(q)
                        || u.getFullName().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(list));
    }

    private void handleAddUser() {
        RegistrationDialog.show((Stage)getScene().getWindow(), false, true).ifPresent(req -> {
            try {
                if (UserService.userExists(req.username())) { toast.showError("Username taken."); return; }
                UserService.createUser(req.username(), req.password(), req.role());
                User u = UserService.getUserById(req.username());
                u.setEmail(req.email());
                u.setContactNumber(req.phoneNumber());
                u.setActive(!req.pendingApproval());
                UserService.updateUser(u);
                reload();
                toast.showSuccess("User created: " + u.getUserId());
            } catch (Exception ex) { toast.showError(ex.getMessage()); }
        });
    }

    private void approveUser(User u) {
        if (u == null) return;
        try {
            u.setActive(true);
            UserService.updateUser(u);
            reload();
            toast.showSuccess("User approved: " + u.getUserId());
        } catch (Exception ex) { toast.showError(ex.getMessage()); }
    }

    private void editUser(User u) {
        if (u == null) return;
        // Re-using the edit logic from UserAccountDialogs for now as it's quite complex
        UserAccountDialogs.editUser((Stage)getScene().getWindow(), u, currentUserId, isAdmin, toast);
        reload();
    }

    private void deleteUser(User u) {
        if (u == null) return;
        Alert conf = new Alert(Alert.AlertType.WARNING, "Delete user \"" + u.getUserId() + "\"? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirm Deletion");
        AppTheme.applyTheme(conf.getDialogPane());
        conf.showAndWait().filter(bt -> bt == ButtonType.YES).ifPresent(bt -> {
            try {
                UserService.deleteUser(u.getUserId());
                reload();
                toast.showSuccess("User deleted.");
            } catch (Exception ex) { toast.showError(ex.getMessage()); }
        });
    }

    private <T> TableColumn<T, String> col(String name, java.util.function.Function<T, String> fn, double w) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(w);
        c.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(fn.apply(data.getValue())));
        return c;
    }

    private Button actionBtn(String icon, String tip, String color) {
        Button b = AppTheme.createIconButton(icon, tip, AppTheme.ButtonStyle.GHOST);
        b.setStyle(b.getStyle() + "; -fx-text-fill: " + color + ";");
        return b;
    }

    private String inputStyle() {
        return "-fx-background-color: " + (AppTheme.darkMode ? "#1E293B" : "#FFFFFF") + "; " +
               "-fx-border-color: " + (AppTheme.darkMode ? "#334155" : "#E2E8F0") + "; " +
               "-fx-border-width: 1.5; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8 14;";
    }

    private String pageBackground() { return AppTheme.darkMode ? "#0F172A" : "#F8FAFC"; }
    private String textPrimary() { return AppTheme.darkMode ? "#F1F5F9" : "#1E293B"; }
}

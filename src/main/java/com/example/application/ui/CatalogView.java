package com.example.application.ui;

import com.example.application.ToastDisplay;
import com.example.entities.Book;
import com.example.exceptions.BooksException;
import com.example.services.BookService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Modern catalog view with book cards, search, filters,
 * and intuitive book management actions.
 */
public class CatalogView extends BorderPane {

    private final ObservableList<Book> booksList;
    private final boolean isStaff;
    private final String currentUser;
    private final Runnable onRefresh;
    private final ToastDisplay toastDisplay;

    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private FlowPane booksGrid;
    private Label resultCountLabel;
    private FilteredList<Book> filteredBooks;

    public CatalogView(ObservableList<Book> booksList, boolean isStaff, String currentUser,
                       Runnable onRefresh, ToastDisplay toastDisplay) {
        this.booksList = booksList;
        this.isStaff = isStaff;
        this.currentUser = currentUser;
        this.onRefresh = onRefresh;
        this.toastDisplay = toastDisplay;

        initializeUI();
        setupDataBinding();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: " + pageBackground() + ";");
        setPadding(new Insets(0));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: " + pageBackground() + ";");

        // Header section
        VBox header = createHeader();

        // Filter bar
        HBox filterBar = createFilterBar();

        // Results count
        resultCountLabel = new Label("Showing all books");
        resultCountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

        // Books grid
        booksGrid = createBooksGrid();

        content.getChildren().addAll(header, filterBar, resultCountLabel, booksGrid);
        scrollPane.setContent(content);
        setCenter(scrollPane);
    }

    private VBox createHeader() {
        VBox header = new VBox(8);

        Label title = new Label("Book Catalog");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Browse, search, and manage your library collection");
        subtitle.getStyleClass().add("page-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private HBox createFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("filter-bar");
        bar.setPadding(new Insets(12, 16, 12, 16));

        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search books by title, author, or ISBN...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(350);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());

        // Category filter
        categoryFilter = new ComboBox<>();
        refreshCategoryFilter();
        categoryFilter.setValue("All Categories");
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        // Add book button (staff only)
        if (isStaff) {
            Button addBookBtn = AppTheme.createIconTextButton("Add Book", AppTheme.ICON_ADD, AppTheme.ButtonStyle.PRIMARY);
            addBookBtn.setOnAction(e -> showAddBookDialog());
            bar.getChildren().addAll(searchField, new Separator(javafx.geometry.Orientation.VERTICAL),
                    categoryFilter, addBookBtn);
        } else {
            bar.getChildren().addAll(searchField, new Separator(javafx.geometry.Orientation.VERTICAL), categoryFilter);
        }

        return bar;
    }

    private FlowPane createBooksGrid() {
        FlowPane grid = new FlowPane(javafx.geometry.Orientation.HORIZONTAL);
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.TOP_LEFT);
        return grid;
    }

    private void setupDataBinding() {
        filteredBooks = new FilteredList<>(booksList, b -> true);
        updateBooksGrid();

        booksList.addListener((javafx.collections.ListChangeListener<Book>) c -> {
            Platform.runLater(() -> {
                refreshCategoryFilter();
                updateBooksGrid();
            });
        });
    }

    /** Rebuild category dropdown from live book data + "Add category..." option. */
    private void refreshCategoryFilter() {
        String current = categoryFilter.getValue();
        java.util.Set<String> cats = new java.util.TreeSet<>();
        cats.add("All Categories");
        booksList.forEach(b -> { if (b.getCategory() != null) cats.add(b.getCategory()); });
        cats.add("── Add Category...");
        categoryFilter.getItems().setAll(cats);
        categoryFilter.setValue(current != null && cats.contains(current) ? current : "All Categories");
        // Handle "Add Category..." selection
        categoryFilter.valueProperty().addListener((o, old, nv) -> {
            if ("── Add Category...".equals(nv)) {
                TextInputDialog td = new TextInputDialog();
                td.setTitle("New Category");
                td.setHeaderText("Enter a new category name:");
                td.setContentText("Category:");
                td.showAndWait().ifPresent(name -> {
                    if (!name.isBlank()) {
                        String trimmed = name.trim();
                        if (!categoryFilter.getItems().contains(trimmed)) {
                            categoryFilter.getItems().add(categoryFilter.getItems().size() - 1, trimmed);
                        }
                        categoryFilter.setValue(trimmed);
                    } else {
                        categoryFilter.setValue("All Categories");
                    }
                });
            }
        });
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String category = categoryFilter.getValue();
        // Ignore the separator/add-category sentinel
        if (category == null || category.startsWith("──")) return;

        filteredBooks.setPredicate(book -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                    book.getTitle().toLowerCase().contains(searchText) ||
                    book.getAuthor().toLowerCase().contains(searchText) ||
                    book.getIsbn().toLowerCase().contains(searchText);

            // Category filter
            boolean matchesCategory = "All Categories".equals(category) ||
                    book.getCategory().equalsIgnoreCase(category);

            return matchesSearch && matchesCategory;
        });

        updateBooksGrid();
        updateResultCount();
    }

    private void updateResultCount() {
        int count = filteredBooks.size();
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            resultCountLabel.setText("Showing " + count + " book" + (count != 1 ? "s" : ""));
        } else {
            resultCountLabel.setText("Found " + count + " result" + (count != 1 ? "s" : "") +
                    " for \"" + searchText + "\"");
        }
    }

    private void updateBooksGrid() {
        booksGrid.getChildren().clear();

        for (Book book : filteredBooks) {
            VBox card = createBookCard(book);
            booksGrid.getChildren().add(card);
        }

        if (filteredBooks.isEmpty()) {
            VBox emptyState = createEmptyState();
            booksGrid.getChildren().add(emptyState);
        }
    }

    private VBox createBookCard(Book book) {
        VBox card = new VBox(0);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);

        // Header with gradient
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24));
        header.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #0F766E, #14B8A6); " +
                "-fx-background-radius: 12 12 0 0;");

        StackPane iconLabel = new StackPane(AppTheme.createIcon(AppTheme.ICON_BOOK, 26));
        iconLabel.setPrefSize(72, 72);
        iconLabel.setMaxSize(72, 72);
        iconLabel.setStyle("-fx-background-color: rgba(255,255,255,0.16); -fx-background-radius: 36px;");

        Label categoryChip = new Label(book.getCategory());
        categoryChip.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20px; " +
                "-fx-padding: 4 12; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: white;");

        header.getChildren().addAll(iconLabel, categoryChip);

        // Body
        VBox body = new VBox(8);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: " + cardSurface() + ";");

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + textPrimary() + ";");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(240);

        Label authorLabel = new Label("by " + book.getAuthor());
        authorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textMuted() + ";");

        Label isbnLabel = new Label("ISBN: " + book.getFormattedIsbn());
        isbnLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textSoft() + ";");

        body.getChildren().addAll(titleLabel, authorLabel, isbnLabel);

        // Footer
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-background-color: " + cardSurface() + "; -fx-background-radius: 0 0 12 12; " +
                "-fx-border-color: " + dividerColor() + "; -fx-border-width: 1 0 0 0;");

        // Availability indicator
        boolean isAvailable = book.getQuantity() > 0;
        Label availabilityLabel = new Label(isAvailable ? "Available" : "Out of Stock");
        availabilityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " +
                (isAvailable ? "#16A34A" : "#DC2626") + ";");

        Label quantityLabel = new Label(book.getQuantity() + " copies");
        quantityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textSoft() + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(availabilityLabel, spacer, quantityLabel);

        // Action buttons (staff only)
        if (isStaff) {
            HBox actions = new HBox(8);
            actions.setPadding(new Insets(0, 20, 16, 20));
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setStyle("-fx-background-color: " + cardSurface() + ";");

            Button editBtn = AppTheme.createIconButton(AppTheme.ICON_EDIT, "Edit", AppTheme.ButtonStyle.GHOST);
            editBtn.setOnAction(e -> showEditBookDialog(book));

            Button deleteBtn = AppTheme.createIconButton(AppTheme.ICON_DELETE, "Delete", AppTheme.ButtonStyle.GHOST);
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DC2626;");
            deleteBtn.setOnAction(e -> showDeleteBookConfirmation(book));

            actions.getChildren().addAll(editBtn, deleteBtn);
            card.getChildren().addAll(header, body, footer, actions);
        } else {
            // Request button for regular users
            HBox actions = new HBox();
            actions.setPadding(new Insets(0, 20, 16, 20));
            actions.setAlignment(Pos.CENTER);
            actions.setStyle("-fx-background-color: " + cardSurface() + ";");

            Button requestBtn = AppTheme.createButton("Request Book", AppTheme.ButtonStyle.PRIMARY);
            requestBtn.setMaxWidth(Double.MAX_VALUE);
            requestBtn.setDisable(!isAvailable);
            requestBtn.setOnAction(e -> requestBook(book));

            actions.getChildren().add(requestBtn);
            card.getChildren().addAll(header, body, footer, actions);
        }

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: " + cardSurface() + "; -fx-background-radius: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.15), 20, 0, 0, 8); " +
                    "-fx-translate-y: -4px;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: " + cardSurface() + "; -fx-background-radius: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.06), 8, 0, 0, 2);");
        });

        return card;
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        empty.setPrefWidth(600);

        Label icon = new Label("Books");
        icon.setGraphic(AppTheme.createIcon(AppTheme.ICON_LIBRARY, 36));

        Label title = new Label("No books found");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: " + textPrimary() + ";");

        Label desc = new Label("Try adjusting your search or filters to find what you're looking for.");
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: " + textMuted() + ";");
        desc.setWrapText(true);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        empty.getChildren().addAll(icon, title, desc);
        return empty;
    }

    private void showAddBookDialog() {
        BookDialog.showAddDialog(getScene().getWindow()).ifPresent(bookData -> {
            try {
                Book newBook = new Book(
                        bookData.isbn(),
                        bookData.title(),
                        bookData.author(),
                        bookData.category(),
                        bookData.quantity()
                );
                BookService.addBook(newBook);

                if (onRefresh != null) {
                    onRefresh.run();
                }

                if (toastDisplay != null) {
                    toastDisplay.showSuccess("Book added successfully!");
                }
            } catch (BooksException e) {
                if (toastDisplay != null) {
                    toastDisplay.showError("Failed to add book: " + e.getMessage());
                }
            }
        });
    }

    private void showEditBookDialog(Book book) {
        BookDialog.showEditDialog(getScene().getWindow(), book).ifPresent(bookData -> {
            try {
                book.setTitle(bookData.title());
                book.setAuthor(bookData.author());
                book.setCategory(bookData.category());
                book.setQuantity(bookData.quantity());

                BookService.updateBook(book);

                if (onRefresh != null) {
                    onRefresh.run();
                }

                if (toastDisplay != null) {
                    toastDisplay.showSuccess("Book updated successfully!");
                }
            } catch (BooksException e) {
                if (toastDisplay != null) {
                    toastDisplay.showError("Failed to update book: " + e.getMessage());
                }
            }
        });
    }

    private void showDeleteBookConfirmation(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Book");
        alert.setHeaderText("Delete \"" + book.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone. The book will be permanently removed from the catalog.");
        alert.initOwner(getScene().getWindow());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                BookService.deleteBook(book.getIsbn());

                if (onRefresh != null) {
                    onRefresh.run();
                }

                if (toastDisplay != null) {
                    toastDisplay.showSuccess("Book deleted successfully!");
                }
            } catch (BooksException e) {
                if (toastDisplay != null) {
                    toastDisplay.showError("Failed to delete book: " + e.getMessage());
                }
            }
        }
    }

    private void requestBook(Book book) {
        try {
            BookService.requestBookForUser(book.getIsbn(), currentUser, 1);

            if (toastDisplay != null) {
                toastDisplay.showSuccess("Book request submitted!");
            }
        } catch (Exception e) {
            if (toastDisplay != null) {
                toastDisplay.showError("Failed to request book: " + e.getMessage());
            }
        }
    }

    private String pageBackground() {
        return AppTheme.darkMode ? "#0F172A" : "#F1F5F9";
    }

    private String cardSurface() {
        return AppTheme.darkMode ? "#1E293B" : "white";
    }

    private String dividerColor() {
        return AppTheme.darkMode ? "#334155" : "#F1F5F9";
    }

    private String textPrimary() {
        return AppTheme.darkMode ? "#F8FAFC" : "#1E293B";
    }

    private String textMuted() {
        return AppTheme.darkMode ? "#CBD5E1" : "#64748B";
    }

    private String textSoft() {
        return AppTheme.darkMode ? "#94A3B8" : "#94A3B8";
    }
}

/**
 * Dialog for adding/editing books.
 */
class BookDialog {

    public static Optional<BookData> showAddDialog(javafx.stage.Window owner) {
        return showDialog(owner, "Add New Book", null);
    }

    public static Optional<BookData> showEditDialog(javafx.stage.Window owner, Book book) {
        return showDialog(owner, "Edit Book", book);
    }

    private static Optional<BookData> showDialog(javafx.stage.Window owner, String title, Book existingBook) {
        Dialog<BookData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(owner);

        // Dialog pane styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(AppTheme.class.getResource("/theme.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane-modern");

        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField isbnField = new TextField();
        isbnField.setPromptText("Enter ISBN");
        isbnField.setDisable(existingBook != null);

        TextField titleField = new TextField();
        titleField.setPromptText("Enter book title");

        TextField authorField = new TextField();
        authorField.setPromptText("Enter author name");

        ComboBox<String> categoryField = new ComboBox<>();
        categoryField.setEditable(true);   // allow typing a new category
        categoryField.getItems().addAll("Fiction", "Non-Fiction", "Science", "Technology",
                "History", "Biography", "Literature", "Reference",
                "Philosophy", "Psychology", "Arts", "Mathematics", "Medicine", "Law");
        categoryField.setPromptText("Select or type a category");

        Spinner<Integer> quantityField = new Spinner<>(1, 1000, 1);
        quantityField.setEditable(true);

        // Pre-fill if editing
        if (existingBook != null) {
            isbnField.setText(existingBook.getIsbn());
            titleField.setText(existingBook.getTitle());
            authorField.setText(existingBook.getAuthor());
            categoryField.setValue(existingBook.getCategory());
            quantityField.getValueFactory().setValue(existingBook.getQuantity());
        }

        grid.addRow(0, new Label("ISBN:"), isbnField);
        grid.addRow(1, new Label("Title:"), titleField);
        grid.addRow(2, new Label("Author:"), authorField);
        grid.addRow(3, new Label("Category:"), categoryField);
        grid.addRow(4, new Label("Quantity:"), quantityField);

        dialogPane.setContent(grid);

        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new BookData(
                        isbnField.getText().trim(),
                        titleField.getText().trim(),
                        authorField.getText().trim(),
                        categoryField.getValue(),
                        quantityField.getValue()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public record BookData(String isbn, String title, String author, String category, int quantity) {}
}

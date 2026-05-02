package com.example.services;

import com.example.application.ToastDisplay;
import com.example.application.ui.AppTheme;
import com.example.entities.AppConfiguration;
import com.example.entities.BooksDB;
import com.example.entities.BooksDB.IssueRecord;
import com.example.entities.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class InvoiceService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void generateAndHandleInvoice(User user, IssueRecord record, double amount, ToastDisplay toast) {
        generateAndHandleInvoice(user, record, amount, toast, null);
    }

    public static void generateAndHandleInvoice(User user, IssueRecord record, double amount, ToastDisplay toast, Runnable onComplete) {
        Dialog<Double> dlg = new Dialog<>();
        dlg.setTitle("Invoice Management");
        
        DialogPane dp = dlg.getDialogPane();
        AppTheme.applyTheme(dp);
        dp.setPrefWidth(450);

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_CENTER);

        // Success Icon
        StackPane iconBadge = new StackPane(AppTheme.createIcon(AppTheme.ICON_CHECK, 28));
        iconBadge.setPrefSize(60, 60);
        iconBadge.setMaxSize(60, 60);
        iconBadge.setStyle("-fx-background-color: rgba(22, 163, 74, 0.15); -fx-background-radius: 30px;");

        Label title = new Label("Payment Success");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #16A34A;");

        VBox details = new VBox(8);
        details.setAlignment(Pos.CENTER);
        Label desc = new Label("Total fine for this item: " + AppTheme.formatCurrency(record.calculateFine()));
        Label alreadyPaid = new Label("Already paid: " + AppTheme.formatCurrency(record.getPaidAmount()));
        Label remaining = new Label("Balance: " + AppTheme.formatCurrency(record.calculateFine() - record.getPaidAmount()));
        desc.setStyle("-fx-text-fill: #64748B;");
        alreadyPaid.setStyle("-fx-text-fill: #64748B;");
        remaining.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
        details.getChildren().addAll(desc, alreadyPaid, remaining);

        Separator sep = new Separator();

        VBox inputArea = new VBox(8);
        Label payLbl = new Label("Payment Amount:");
        payLbl.setStyle("-fx-font-weight: 600;");
        
        // Ensure we always use the freshest calculation
        double latestRemaining = record.getRemainingFine();
        TextField amountField = new TextField(String.format("%.2f", latestRemaining));
        amountField.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-alignment: CENTER; -fx-padding: 10;");
        inputArea.getChildren().addAll(payLbl, amountField);

        root.getChildren().addAll(iconBadge, title, details, sep, inputArea);
        dp.setContent(root);

        ButtonType printType = new ButtonType("Print Receipt", ButtonBar.ButtonData.OK_DONE);
        ButtonType emailType = new ButtonType("Send Email", ButtonBar.ButtonData.OK_DONE);
        ButtonType doneType = new ButtonType("Done", ButtonBar.ButtonData.CANCEL_CLOSE);

        dp.getButtonTypes().addAll(printType, emailType, doneType);

        // Styling buttons properly
        Button printBtn = (Button) dp.lookupButton(printType);
        Button emailBtn = (Button) dp.lookupButton(emailType);
        Button closeBtn = (Button) dp.lookupButton(doneType);

        printBtn.getStyleClass().add("btn-primary");
        emailBtn.getStyleClass().add("btn-primary");
        closeBtn.getStyleClass().add("btn-secondary");

        // Prevent non-numeric/negative input as the user types
        amountField.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(old);
            }
        });

        dlg.setResultConverter(type -> {
            if (type == doneType) return null;
            try {
                String text = amountField.getText().trim();
                if (text.isEmpty()) return null;
                
                double val = Double.parseDouble(text);
                double max = record.calculateFine() - record.getPaidAmount();
                
                if (val <= 0) {
                    Platform.runLater(() -> {
                        if (toast != null) toast.showError("Amount must be greater than zero.");
                        AppTheme.flashError(amountField);
                    });
                    return null;
                }
                if (val > max + 0.05) { // allowance for rounding
                    Platform.runLater(() -> {
                        if (toast != null) toast.showError("Amount exceeds remaining balance.");
                        AppTheme.flashError(amountField);
                    });
                    return null;
                }
                return val;
            } catch (Exception e) {
                return null;
            }
        });

        dlg.showAndWait().ifPresent(finalAmount -> {
            // Update record
            record.setPaidAmount(record.getPaidAmount() + finalAmount);
            
            // Check if full payment was made based on the fine AT THIS MOMENT
            double currentAccruedFine = record.calculateFine();
            if (record.getPaidAmount() >= currentAccruedFine - 0.01) {
                if (!record.isReturned()) {
                    // Reset overdue status so it starts fresh from today.
                    // This moves it to 'Active Issues' and starts a new fine cycle tomorrow.
                    record.setDueDate(java.time.LocalDate.now());
                    record.setPaidAmount(0);
                    record.setFineAmount(0);
                    record.setFinePaid(false);
                } else {
                    record.setFineAmount(currentAccruedFine);
                    record.setFinePaid(true);
                }
            }
            
            // Persist
            BooksDB.getInstance().saveAllData();

            // Handle actions (Print/Email) with a single invoice ID
            String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Persist to history (7 days)
            BooksDB.getInstance().addInvoiceRecord(new BooksDB.InvoiceData(invoiceId, user.getUserId(), record.getIsbn(), record.getBookTitle(), finalAmount));
            
            processInvoiceActions(user, record, finalAmount, invoiceId, toast);
            if (onComplete != null) onComplete.run();
        });
        
        // Wait, the showAndWait result only returns the converted value. 
        // I need to know which button was clicked.
    }

    public static void processInvoiceActions(User user, IssueRecord record, double amount, String invoiceId, ToastDisplay toast) {
        // Custom Dialog for Choice
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Invoice Actions");
        DialogPane dp = dlg.getDialogPane();
        AppTheme.applyTheme(dp);
        dp.setMinWidth(650);
        dp.setPrefWidth(650);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        Label msg = new Label("Payment of " + AppTheme.formatCurrency(amount) + " processed.");
        msg.setStyle("-fx-font-weight: 700; -fx-font-size: 15px;");
        Label idLbl = new Label("Invoice: " + invoiceId);
        idLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        
        root.getChildren().addAll(msg, idLbl);
        dp.setContent(root);

        ButtonType printT = new ButtonType("Print Receipt");
        ButtonType emailT = new ButtonType("Send Email");
        ButtonType bothT  = new ButtonType("Both");
        ButtonType doneT  = new ButtonType("Done", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dp.getButtonTypes().setAll(printT, emailT, bothT, doneT);
        
        for (ButtonType bt : dp.getButtonTypes()) {
            Button b = (Button) dp.lookupButton(bt);
            if (b != null) {
                b.setMinWidth(130);
                if (bt == doneT) b.getStyleClass().add("btn-secondary");
                else b.getStyleClass().add("btn-primary");
            }
        }

        dlg.showAndWait().ifPresent(type -> {
            if (type == printT || type == bothT) {
                printInvoice(user, record, amount, invoiceId);
            }
            if (type == emailT || type == bothT) {
                emailInvoice(user, record, amount, invoiceId, toast);
            }
        });
    }

    private static void printInvoice(User user, IssueRecord record, double amount, String invoiceId) {
        VBox printable = createInvoiceNode(user, record, amount, invoiceId);
        
        Stage printStage = new Stage();
        printStage.initModality(Modality.APPLICATION_MODAL);
        printStage.setTitle("Print Preview - " + invoiceId);
        
        ScrollPane scroll = new ScrollPane(printable);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        
        Button btn = new Button("Print Now");
        btn.getStyleClass().add("btn-primary");
        btn.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(printStage)) {
                boolean success = job.printPage(printable);
                if (success) job.endJob();
                printStage.close();
            }
        });
        
        VBox layout = new VBox(20, scroll, btn);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: white;");
        
        Scene scene = new Scene(layout, 500, 750);
        AppTheme.applyTheme(scene);
        printStage.setScene(scene);
        printStage.show();
    }

    private static VBox createInvoiceNode(User user, IssueRecord record, double amount, String invoiceId) {
        VBox v = new VBox(15);
        v.setPadding(new Insets(40));
        v.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        v.setMinWidth(450);
        v.setMaxWidth(450);

        AppConfiguration cfg = AppConfigurationService.getConfiguration();

        Label libName = new Label(cfg.getLibraryName().toUpperCase());
        libName.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        
        Label receiptTitle = new Label("FINE PAYMENT RECEIPT");
        receiptTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #64748B;");
        
        Separator sep1 = new Separator();
        
        GridPane info = new GridPane();
        info.setHgap(20); info.setVgap(8);
        info.add(boldLabel("Invoice ID:"), 0, 0); info.add(new Label(invoiceId), 1, 0);
        info.add(boldLabel("Date:"),       0, 1); info.add(new Label(LocalDateTime.now().format(FMT)), 1, 1);
        info.add(boldLabel("Member:"),     0, 2); info.add(new Label(user.getDisplayName() + " (" + user.getUserId() + ")"), 1, 2);
        
        Separator sep2 = new Separator();
        
        VBox items = new VBox(10);
        items.getChildren().add(boldLabel("Details:"));
        items.getChildren().add(new Label("Book: " + record.getBookTitle()));
        items.getChildren().add(new Label("ISBN: " + record.getIsbn()));
        items.getChildren().add(new Label("Overdue Days: " + record.getDaysOverdue()));
        
        Separator sep3 = new Separator();
        
        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        Label totalLbl = new Label("AMOUNT PAID: ");
        totalLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 16px;");
        Label amtLbl = new Label(AppTheme.formatCurrency(amount));
        amtLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 20px; -fx-text-fill: #0D9488;");
        totalBox.getChildren().addAll(totalLbl, amtLbl);
        
        Label footer = new Label("Thank you for using Library OS.\nPlease keep this receipt for your records.");
        footer.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        footer.setPadding(new Insets(20, 0, 0, 0));
        footer.setAlignment(Pos.CENTER);
        footer.setWrapText(true);

        v.getChildren().addAll(libName, receiptTitle, sep1, info, sep2, items, sep3, totalBox, footer);
        
        // Ensure all labels in printable node are black
        v.lookupAll(".label").forEach(n -> {
            if (n instanceof Label l) l.setTextFill(javafx.scene.paint.Color.BLACK);
        });
        amtLbl.setTextFill(javafx.scene.paint.Color.web("#0D9488"));
        
        return v;
    }

    private static Label boldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-weight: 700; -fx-text-fill: black;");
        return l;
    }

    private static void emailInvoice(User user, IssueRecord record, double amount, String invoiceId, ToastDisplay toast) {
        if (toast != null) toast.showInfo("Sending invoice to " + user.getEmail() + "...");
        
        // Generate the invoice attachment (PNG snapshot)
        VBox node = createInvoiceNode(user, record, amount, invoiceId);
        new Scene(node); // Force layout
        WritableImage image = node.snapshot(new SnapshotParameters(), null);
        
        byte[] attachmentData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", bos);
            attachmentData = bos.toByteArray();
        } catch (Exception e) {
            attachmentData = null;
        }

        final byte[] finalAttachment = attachmentData;
        new Thread(() -> {
            try {
                ReminderService.sendPaymentInvoice(user, record, amount, invoiceId, finalAttachment, "Invoice_" + invoiceId + ".png");
                Platform.runLater(() -> {
                    if (toast != null) toast.showSuccess("Invoice emailed successfully.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (toast != null) toast.showError("Failed to email invoice: " + e.getMessage());
                });
            }
        }, "invoice-email").start();
    }
}

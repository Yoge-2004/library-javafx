package com.example.services;

import com.example.application.ToastDisplay;
import com.example.application.ui.AppTheme;
import com.example.entities.AppConfiguration;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class InvoiceService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void generateAndHandleInvoice(User user, IssueRecord record, double amount, ToastDisplay toast) {
        String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Mark as paid
        record.setFinePaid(true);

        // Show options to user: Print or Email
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Invoice Generated");
        a.setHeaderText("Invoice " + invoiceId);
        a.setContentText("Fine of " + AppTheme.formatCurrency(amount) + " processed.\nChoose an action:");
        
        ButtonType printBtn = new ButtonType("Print Receipt");
        ButtonType emailBtn = new ButtonType("Send Email");
        ButtonType bothBtn  = new ButtonType("Both");
        ButtonType closeBtn = new ButtonType("Done", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        a.getButtonTypes().setAll(printBtn, emailBtn, bothBtn, closeBtn);
        AppTheme.applyTheme(a.getDialogPane());
        
        a.showAndWait().ifPresent(type -> {
            if (type == printBtn || type == bothBtn) {
                printInvoice(user, record, amount, invoiceId);
            }
            if (type == emailBtn || type == bothBtn) {
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
        
        Scene scene = new Scene(layout, 500, 700);
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
        Label totalLbl = new Label("TOTAL PAID: ");
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
        
        v.lookupAll(".label").forEach(n -> n.setStyle(n.getStyle() + "; -fx-text-fill: black;"));
        amtLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 20px; -fx-text-fill: #0D9488;");
        
        return v;
    }

    private static Label boldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-weight: 700; -fx-text-fill: black;");
        return l;
    }

    private static void emailInvoice(User user, IssueRecord record, double amount, String invoiceId, ToastDisplay toast) {
        if (toast != null) toast.showInfo("Sending invoice to " + user.getEmail() + "...");
        
        new Thread(() -> {
            try {
                ReminderService.sendPaymentInvoice(user, record, amount, invoiceId);
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

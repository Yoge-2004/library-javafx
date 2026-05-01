package com.example.services;

import com.example.entities.AppConfiguration;
import com.example.entities.BooksDB.IssueRecord;
import com.example.entities.User;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FIXED: Added null safety for SMTP credentials to prevent NullPointerException.
 */
public final class ReminderService {
    private static final Logger LOGGER = Logger.getLogger(ReminderService.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String APP_ICON_SVG = loadAppIconSvg();

    private ReminderService() {
    }

    public static ReminderDispatchResult sendOverdueReminders(List<IssueRecord> overdueBooks) throws MessagingException {
        AppConfiguration config = AppConfigurationService.getConfiguration();
        validateConfiguration(config);

        Map<String, List<IssueRecord>> recordsByUser = new LinkedHashMap<>();
        for (IssueRecord record : overdueBooks) {
            recordsByUser.computeIfAbsent(record.getUserId(), key -> new ArrayList<>()).add(record);
        }

        ReminderDispatchResult result = new ReminderDispatchResult();
        for (Map.Entry<String, List<IssueRecord>> entry : recordsByUser.entrySet()) {
            User user = UserService.getUserById(entry.getKey());
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                result.incrementSkippedNoEmail();
                continue;
            }
            try {
                sendOverdueReminder(user, entry.getValue());
                result.incrementSent();
            } catch (MessagingException ex) {
                LOGGER.log(Level.WARNING, "Failed to send reminder to " + user.getUserId(), ex);
                result.addFailure(user.getUserId(), ex.getMessage());
            }
        }
        return result;
    }

    public static void sendOverdueReminder(User user, List<IssueRecord> records) throws MessagingException {
        if (user == null) {
            throw new MessagingException("User information is required to send a reminder.");
        }
        if (records == null || records.isEmpty()) {
            throw new MessagingException("No overdue items were provided for the reminder.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new MessagingException("The selected user does not have an email address on file.");
        }

        AppConfiguration config = AppConfigurationService.getConfiguration();
        validateConfiguration(config);
        Session session = createSession(config);
        sendMessage(session, config, user.getEmail(), "Library overdue reminder",
                buildOverdueBody(config, user, records),
                buildOverdueHtmlBody(config, user, records));
    }

    public static void sendTemporaryPassword(User user, String temporaryPassword) throws MessagingException {
        if (user == null) {
            throw new MessagingException("User information is required to send a password reset email.");
        }
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            throw new MessagingException("Temporary password cannot be blank.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new MessagingException("The selected user does not have an email address on file.");
        }

        AppConfiguration config = AppConfigurationService.getConfiguration();
        validateConfiguration(config);
        Session session = createSession(config);
        sendMessage(session, config, user.getEmail(), "Library OS password reset",
                buildTemporaryPasswordBody(config, user, temporaryPassword),
                buildTemporaryPasswordHtmlBody(config, user, temporaryPassword));
    }

    /**
     * FIXED: Added null checks for SMTP credentials to prevent NullPointerException
     * in PasswordAuthentication constructor.
     */
    private static Session createSession(AppConfiguration config) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.getSmtpHost());
        properties.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        properties.put("mail.smtp.auth", String.valueOf(config.isSmtpAuth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(config.isStartTlsEnabled()));
        properties.put("mail.smtp.connectiontimeout", "15000");
        properties.put("mail.smtp.timeout", "15000");
        properties.put("mail.smtp.writetimeout", "15000");

        if (config.isSmtpAuth()) {
            // FIXED: Null-safe credential retrieval
            final String username = config.getSmtpUsername() != null ? config.getSmtpUsername() : "";
            final String password = config.getSmtpPassword() != null ? config.getSmtpPassword() : "";

            // Only create authenticator if username is not empty
            if (!username.isEmpty()) {
                return Session.getInstance(properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                LOGGER.log(Level.WARNING, "SMTP auth enabled but username is null/empty");
            }
        }
        return Session.getInstance(properties);
    }

    private static void sendMessage(Session session, AppConfiguration config, String toAddress,
                                    String subject, String plainBody, String htmlBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getFromAddress()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
        message.setSubject(subject);
        message.setHeader("X-Mailer", "Library OS");
        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText(plainBody, StandardCharsets.UTF_8.name());

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(plainPart);
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
        try {
            Transport.send(message);
        } catch (MessagingException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof java.net.ConnectException
                    || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.net.NoRouteToHostException
                    || cause instanceof java.net.SocketException) {
                throw new MessagingException(
                        "No network connectivity — check your internet connection and try again.", ex);
            }
            if (cause instanceof java.net.SocketTimeoutException) {
                throw new MessagingException(
                        "Connection to mail server timed out — check your SMTP settings.", ex);
            }
            throw ex;
        }
    }

    private static String buildOverdueBody(AppConfiguration config, User user, List<IssueRecord> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello ").append(user.getFullName()).append(",\n\n");
        builder.append("The following library items are overdue:\n\n");
        double totalFine = 0.0;
        for (IssueRecord record : records) {
            double fine = record.calculateFine();
            totalFine += fine;
            builder.append("- ").append(record.getBookTitle())
                    .append(" | Due: ").append(record.getDueDate().format(DATE_FORMATTER))
                    .append(" | Days overdue: ").append(record.getDaysOverdue())
                    .append(" | Fine: ").append(config.formatAmount(fine))
                    .append('\n');
        }
        builder.append("\nTotal outstanding fine: ").append(config.formatAmount(totalFine)).append('\n');
        builder.append("Please return the item(s) or contact the library administrator.\n");
        return builder.toString();
    }

    private static String buildTemporaryPasswordBody(AppConfiguration config, User user, String temporaryPassword) {
        String libraryName = config.getCurrentLibraryDisplayName();
        return "Hello " + user.getFullName() + ",\n\n" +
                "A password reset was requested for your Library OS account at " + libraryName + ".\n\n" +
                "Temporary password: " + temporaryPassword + "\n\n" +
                "Sign in with this password and change it immediately from Settings > Change Password.\n" +
                "If you did not request this change, contact the library administrator.\n";
    }

    private static String buildOverdueHtmlBody(AppConfiguration config, User user, List<IssueRecord> records) {
        StringBuilder items = new StringBuilder();
        double totalFine = 0.0;
        for (IssueRecord record : records) {
            double fine = record.calculateFine();
            totalFine += fine;
            items.append("""
                    <tr class="stack-row">
                      <td class="stack-cell" data-label="Item" style="padding:12px 14px;border-bottom:1px solid #E2E8F0;font-weight:600;color:#0F172A;">%s</td>
                      <td class="stack-cell" data-label="Due Date" style="padding:12px 14px;border-bottom:1px solid #E2E8F0;color:#475569;">%s</td>
                      <td class="stack-cell" data-label="Overdue" style="padding:12px 14px;border-bottom:1px solid #E2E8F0;color:#B91C1C;">%d day(s)</td>
                      <td class="stack-cell" data-label="Fine" style="padding:12px 14px;border-bottom:1px solid #E2E8F0;color:#0F766E;font-weight:600;">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(record.getBookTitle()),
                    escapeHtml(record.getDueDate().format(DATE_FORMATTER)),
                    record.getDaysOverdue(),
                    escapeHtml(config.formatAmount(fine))));
        }

        String body = """
                <p style="margin:0 0 16px 0;color:#334155;font-size:15px;line-height:1.6;">Hello %s,</p>
                <p style="margin:0 0 20px 0;color:#334155;font-size:15px;line-height:1.6;">
                  The following library items are overdue. Please return them or contact the library team if you need help.
                </p>
                <div style="border:1px solid #E2E8F0;border-radius:16px;overflow:hidden;background:#FFFFFF;">
                  <table role="presentation" class="stack-table" style="width:100%%;border-collapse:collapse;table-layout:fixed;">
                    <thead>
                      <tr style="background:#F8FAFC;">
                        <th class="stack-head" style="text-align:left;padding:12px 14px;color:#64748B;font-size:12px;">Item</th>
                        <th class="stack-head" style="text-align:left;padding:12px 14px;color:#64748B;font-size:12px;">Due Date</th>
                        <th class="stack-head" style="text-align:left;padding:12px 14px;color:#64748B;font-size:12px;">Overdue</th>
                        <th class="stack-head" style="text-align:left;padding:12px 14px;color:#64748B;font-size:12px;">Fine</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </div>
                <div style="margin-top:20px;padding:16px 18px;background:#ECFDF5;border:1px solid #A7F3D0;border-radius:14px;">
                  <div style="font-size:13px;color:#065F46;text-transform:uppercase;font-weight:700;letter-spacing:0.04em;">Outstanding Fine</div>
                  <div style="margin-top:4px;font-size:24px;font-weight:800;color:#0F766E;">%s</div>
                </div>
                <p style="margin:20px 0 0 0;color:#475569;font-size:14px;line-height:1.6;">
                  Sign in to Library OS to review your circulation record or contact the library administrator for assistance.
                </p>
                """.formatted(
                escapeHtml(user.getFullName()),
                items,
                escapeHtml(config.formatAmount(totalFine)));

        return buildEmailShell(
                config,
                "Overdue Library Reminder",
                "Please review the items below and return them as soon as possible.",
                body);
    }

    private static String buildTemporaryPasswordHtmlBody(AppConfiguration config, User user, String temporaryPassword) {
        String body = """
                <p style="margin:0 0 16px 0;color:#334155;font-size:15px;line-height:1.6;">Hello %s,</p>
                <p style="margin:0 0 20px 0;color:#334155;font-size:15px;line-height:1.6;">
                  A password reset was requested for your Library OS account at %s.
                </p>
                <div style="padding:18px 20px;border-radius:16px;background:#0F172A;">
                  <div style="font-size:12px;color:#94A3B8;text-transform:uppercase;font-weight:700;letter-spacing:0.08em;">Temporary Password</div>
                  <div style="margin-top:8px;font-size:28px;font-weight:800;color:#F8FAFC;letter-spacing:0.08em;">%s</div>
                </div>
                <p style="margin:20px 0 0 0;color:#475569;font-size:14px;line-height:1.6;">
                  Sign in with this password and change it immediately from <strong>Settings &gt; Change Password</strong>.
                </p>
                <p style="margin:12px 0 0 0;color:#475569;font-size:14px;line-height:1.6;">
                  If you did not request this change, contact the library administrator right away.
                </p>
                """.formatted(
                escapeHtml(user.getFullName()),
                escapeHtml(config.getCurrentLibraryDisplayName()),
                escapeHtml(temporaryPassword));

        return buildEmailShell(
                config,
                "Password Reset",
                "Use the temporary password below to sign in and update your credentials.",
                body);
    }

    private static String buildEmailShell(AppConfiguration config, String heading, String subtitle, String body) {
        String logoBlock = APP_ICON_SVG.isBlank()
                ? "<div style=\"display:block;width:56px;height:56px;border-radius:18px;background:linear-gradient(135deg,#0F172A,#14B8A6);\"></div>"
                : "<div style=\"display:block;width:56px;height:56px;line-height:0;\">" + APP_ICON_SVG + "</div>";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    @media only screen and (max-width: 640px) {
                      .shell-body { padding: 12px !important; }
                      .shell-card { border-radius: 18px !important; }
                      .shell-hero { padding: 22px 18px !important; }
                      .shell-content { padding: 22px 18px !important; }
                      .shell-hero-row { display: block !important; }
                      .shell-hero-copy { margin-top: 14px !important; }
                      .stack-head { display: none !important; }
                      .stack-table,
                      .stack-table tbody,
                      .stack-row,
                      .stack-cell { display: block !important; width: 100%% !important; }
                      .stack-row { border-bottom: 1px solid #E2E8F0 !important; padding: 6px 0 !important; }
                      .stack-cell {
                        box-sizing: border-box !important;
                        border-bottom: none !important;
                        padding: 6px 14px 6px 120px !important;
                        position: relative !important;
                        word-break: break-word !important;
                      }
                      .stack-cell:before {
                        content: attr(data-label) !important;
                        position: absolute !important;
                        left: 14px !important;
                        top: 6px !important;
                        width: 92px !important;
                        color: #64748B !important;
                        font-size: 11px !important;
                        font-weight: 700 !important;
                        text-transform: uppercase !important;
                      }
                    }
                  </style>
                </head>
                <body class="shell-body" style="margin:0;padding:24px;background:#E2E8F0;font-family:Segoe UI,Arial,sans-serif;">
                  <div class="shell-card" style="max-width:680px;margin:0 auto;background:#FFFFFF;border-radius:24px;overflow:hidden;box-shadow:0 18px 40px rgba(15,23,42,0.12);">
                    <div class="shell-hero" style="padding:28px 32px;background:linear-gradient(135deg,#0F172A,#134E4A 58%%,#14B8A6);">
                      <div class="shell-hero-row" style="display:flex;align-items:center;gap:16px;">
                        %s
                        <div class="shell-hero-copy">
                          <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#99F6E4;">Library OS</div>
                          <div style="margin-top:6px;font-size:28px;font-weight:800;color:#F8FAFC;">%s</div>
                          <div style="margin-top:6px;font-size:14px;color:#CCFBF1;line-height:1.6;">%s</div>
                        </div>
                      </div>
                    </div>
                    <div class="shell-content" style="padding:28px 32px;">
                      <div style="margin-bottom:20px;font-size:14px;color:#64748B;">%s</div>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                logoBlock,
                escapeHtml(heading),
                escapeHtml(subtitle),
                escapeHtml(config.getCurrentLibraryDisplayName()),
                body);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String loadAppIconSvg() {
        try (InputStream stream = ReminderService.class.getResourceAsStream("/app-icon.svg")) {
            if (stream == null) {
                return "";
            }
            String svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return svg.replaceFirst("<svg ", "<svg width=\"56\" height=\"56\" ");
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Could not load SVG email logo", ex);
            return "";
        }
    }

    private static void validateConfiguration(AppConfiguration config) throws MessagingException {
        if (config == null || !config.isEmailConfigured()) {
            throw new MessagingException("Email is not configured. Update Library Configuration first.");
        }
    }

    public static String toUserMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return hasUsableNetworkConnection()
                        ? "Could not resolve the email server host. Check the SMTP host value."
                        : "No network connectivity detected. Connect to the internet and try again.";
            }
            if (current instanceof ConnectException || current instanceof SocketTimeoutException) {
                return hasUsableNetworkConnection()
                        ? "Could not connect to the email server. Check the SMTP host, port, and firewall settings."
                        : "No network connectivity detected. Connect to the internet and try again.";
            }
            if (current instanceof MessagingException messagingException && messagingException.getNextException() != null) {
                String nested = toUserMessage(messagingException.getNextException());
                if (nested != null && !nested.isBlank()) {
                    return nested;
                }
            }
            current = current.getCause();
        }

        String message = error != null ? error.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "Email sending failed.";
        }
        if (!hasUsableNetworkConnection() && (
                message.contains("Couldn't connect")
                        || message.contains("Could not connect")
                        || message.contains("Connection timed out")
                        || message.contains("Unknown host"))) {
            return "No network connectivity detected. Connect to the internet and try again.";
        }
        return message;
    }

    private static boolean hasUsableNetworkConnection() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return false;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual()) {
                    return true;
                }
            }
        } catch (SocketException ex) {
            LOGGER.log(Level.FINE, "Could not inspect network interfaces", ex);
        }
        return false;
    }

    public static final class ReminderDispatchResult {
        private int sentCount;
        private int skippedNoEmailCount;
        private final Map<String, String> failures = new LinkedHashMap<>();

        public int getSentCount() {
            return sentCount;
        }

        public int getSkippedNoEmailCount() {
            return skippedNoEmailCount;
        }

        public Map<String, String> getFailures() {
            return Map.copyOf(failures);
        }

        void incrementSent() {
            sentCount++;
        }

        void incrementSkippedNoEmail() {
            skippedNoEmailCount++;
        }

        void addFailure(String userId, String reason) {
            failures.put(userId, reason);
        }
    }
}
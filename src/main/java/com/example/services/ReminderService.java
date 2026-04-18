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
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private ReminderService() {
    }

    public static ReminderDispatchResult sendOverdueReminders(List<IssueRecord> overdueBooks) throws MessagingException {
        AppConfiguration config = AppConfigurationService.getConfiguration();
        if (!config.isEmailConfigured()) {
            throw new MessagingException("SMTP is not configured. Update Library Configuration first.");
        }

        Map<String, List<IssueRecord>> recordsByUser = new LinkedHashMap<>();
        for (IssueRecord record : overdueBooks) {
            recordsByUser.computeIfAbsent(record.getUserId(), key -> new ArrayList<>()).add(record);
        }

        Session session = createSession(config);
        ReminderDispatchResult result = new ReminderDispatchResult();
        for (Map.Entry<String, List<IssueRecord>> entry : recordsByUser.entrySet()) {
            User user = UserService.getUserById(entry.getKey());
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                result.incrementSkippedNoEmail();
                continue;
            }
            try {
                sendReminder(session, config, user, entry.getValue());
                result.incrementSent();
            } catch (MessagingException ex) {
                LOGGER.log(Level.WARNING, "Failed to send reminder to " + user.getUserId(), ex);
                result.addFailure(user.getUserId(), ex.getMessage());
            }
        }
        return result;
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

    private static void sendReminder(Session session, AppConfiguration config, User user,
                                     List<IssueRecord> records) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getFromAddress()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
        message.setSubject("Library overdue reminder");
        message.setText(buildBody(user, records));
        Transport.send(message);
    }

    private static String buildBody(User user, List<IssueRecord> records) {
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
                    .append(" | Fine: $").append(String.format("%.2f", fine))
                    .append('\n');
        }
        builder.append("\nTotal outstanding fine: $").append(String.format("%.2f", totalFine)).append('\n');
        builder.append("Please return the item(s) or contact the library administrator.\n");
        return builder.toString();
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
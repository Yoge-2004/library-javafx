package com.example.application;

import com.example.storage.AppPaths;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LoggingConfigurator {
    private static final Logger LOGGER = Logger.getLogger(LoggingConfigurator.class.getName());
    private static final String LOG_PATTERN = AppPaths.logDirectory().resolve("library-os.%g.log").toString();
    private static volatile boolean configured;

    private LoggingConfigurator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }

        try {
            Logger rootLogger = Logger.getLogger("");
            if (!hasFileHandler(rootLogger)) {
                FileHandler fileHandler = new FileHandler(LOG_PATTERN, 1_048_576, 5, true);
                fileHandler.setEncoding(StandardCharsets.UTF_8.name());
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new PlainLogFormatter());
                rootLogger.addHandler(fileHandler);
            }
            rootLogger.setLevel(Level.INFO);
            configured = true;
            LOGGER.log(Level.INFO, "Logging configured at {0}", AppPaths.logDirectory());
        } catch (IOException e) {
            configured = true;
            System.err.println("Failed to initialize LibraryOS file logging: " + e.getMessage());
        }
    }

    private static boolean hasFileHandler(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof FileHandler) {
                return true;
            }
        }
        return false;
    }

    private static final class PlainLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String thrown = "";
            if (record.getThrown() != null) {
                StringWriter writer = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(writer));
                thrown = System.lineSeparator() + writer;
            }
            return String.format("%1$tF %1$tT [%2$s] %3$s - %4$s%5$s%n",
                    record.getMillis(),
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    formatMessage(record),
                    thrown);
        }
    }
}

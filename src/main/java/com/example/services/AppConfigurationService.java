package com.example.services;

import com.example.entities.AppConfiguration;
import com.example.storage.AppPaths;
import com.example.storage.DataStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppConfigurationService {
    private static final Logger LOGGER = Logger.getLogger(AppConfigurationService.class.getName());
    private static final Path CONFIG_FILE = AppPaths.configFile();
    private static final Path LEGACY_CONFIG_FILE = Path.of("data", "app_config.ser").toAbsolutePath().normalize();
    private static final String[] LEGACY_DIRECTORIES = {"data", "exports"};

    private static AppConfiguration configuration = loadConfiguration();

    private AppConfigurationService() {
    }

    public static AppConfiguration getConfiguration() {
        configuration.normalize();
        return configuration;
    }

    public static void updateConfiguration(AppConfiguration updated) throws IOException {
        if (updated == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        updated.normalize();
        updated.rememberCurrentLibrary();
        configuration = updated;
        DataStorage.writeSerialized(CONFIG_FILE.toString(), configuration);
    }

    private static AppConfiguration loadConfiguration() {
        boolean loadedFromLegacy = false;
        try {
            AppConfiguration loaded = DataStorage.readSerialized(CONFIG_FILE.toString(), AppConfiguration.class);
            if (loaded == null && Files.exists(LEGACY_CONFIG_FILE)) {
                loadedFromLegacy = true;
                loaded = DataStorage.readSerialized(LEGACY_CONFIG_FILE.toString(), AppConfiguration.class);
            }

            AppConfiguration configuration = loaded != null ? loaded : new AppConfiguration();
            configuration.normalize();
            configuration.rememberCurrentLibrary();
            migrateLegacyStorage(configuration);
            persistSilently(configuration);
            return configuration;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load app configuration, using defaults", e);
            AppConfiguration configuration = new AppConfiguration();
            configuration.normalize();
            configuration.rememberCurrentLibrary();
            migrateLegacyStorage(configuration);
            persistSilently(configuration);
            return configuration;
        }
    }

    public static final java.util.List<String> DEFAULT_BOOK_CATEGORIES = java.util.List.of(
            "Arts",
            "Biography",
            "Fiction",
            "History",
            "Law",
            "Literature",
            "Mathematics",
            "Medicine",
            "Non-Fiction",
            "Philosophy",
            "Psychology",
            "Reference",
            "Science",
            "Technology"
    );

    public static java.util.List<String> getAvailableBookCategories(java.util.Collection<com.example.entities.Book> books) {
        java.util.TreeSet<String> categories = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        categories.addAll(DEFAULT_BOOK_CATEGORIES);
        if (books != null) {
            books.stream()
                    .map(com.example.entities.Book::getCategory)
                    .filter(category -> category != null && !category.isBlank())
                    .map(String::trim)
                    .forEach(categories::add);
        }
        getConfiguration().getSavedCategories().stream()
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .forEach(categories::add);
        return java.util.List.copyOf(categories);
    }

    public static void rememberBookCategory(String category) throws IOException {
        if (category == null || category.isBlank()) {
            return;
        }
        AppConfiguration updated = getConfiguration();
        updated.rememberCategory(category);
        updateConfiguration(updated);
    }

    public static void selectKnownLibrary(String displayName) throws IOException {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        AppConfiguration updated = getConfiguration();
        if (updated.selectKnownLibrary(displayName.trim())) {
            updateConfiguration(updated);
        }
    }

    private static void migrateLegacyStorage(AppConfiguration configuration) {
        if (configuration == null) {
            return;
        }
        AppPaths.migrateLegacyDirectoryIfNeeded(LEGACY_DIRECTORIES[0], Path.of(configuration.getDataDirectory()));
        AppPaths.migrateLegacyDirectoryIfNeeded(LEGACY_DIRECTORIES[1], Path.of(configuration.getExportDirectory()));
    }

    private static void persistSilently(AppConfiguration configuration) {
        try {
            updateConfiguration(configuration);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to persist normalized app configuration", e);
        }
    }
}

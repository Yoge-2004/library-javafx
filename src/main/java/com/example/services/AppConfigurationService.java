package com.example.services;

import com.example.entities.AppConfiguration;
import com.example.storage.DataStorage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppConfigurationService {
    private static final Logger LOGGER = Logger.getLogger(AppConfigurationService.class.getName());
    private static final String CONFIG_FILE = "data/app_config.ser";

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
        DataStorage.writeSerialized(CONFIG_FILE, configuration);
    }

    private static AppConfiguration loadConfiguration() {
        try {
            AppConfiguration loaded = DataStorage.readSerialized(CONFIG_FILE, AppConfiguration.class);
            AppConfiguration configuration = loaded != null ? loaded : new AppConfiguration();
            configuration.normalize();
            configuration.rememberCurrentLibrary();
            return configuration;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load app configuration, using defaults", e);
            AppConfiguration configuration = new AppConfiguration();
            configuration.normalize();
            configuration.rememberCurrentLibrary();
            return configuration;
        }
    }
}

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
        return configuration;
    }

    public static void updateConfiguration(AppConfiguration updated) throws IOException {
        if (updated == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        configuration = updated;
        DataStorage.writeSerialized(CONFIG_FILE, configuration);
    }

    private static AppConfiguration loadConfiguration() {
        try {
            AppConfiguration loaded = DataStorage.readSerialized(CONFIG_FILE, AppConfiguration.class);
            return loaded != null ? loaded : new AppConfiguration();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load app configuration, using defaults", e);
            return new AppConfiguration();
        }
    }
}
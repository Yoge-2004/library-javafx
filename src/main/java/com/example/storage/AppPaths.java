package com.example.storage;

import com.example.services.AppConfigurationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;

public final class AppPaths {
    public static final String APP_NAME = "LibraryOS";
    private static final String HOME_OVERRIDE = "libraryos.home";

    private AppPaths() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Path appHome() {
        String override = System.getProperty(HOME_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return ensureDirectory(Paths.get(override.trim()).toAbsolutePath().normalize());
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path baseDirectory;
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            baseDirectory = appData != null && !appData.isBlank()
                    ? Paths.get(appData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (osName.contains("mac")) {
            baseDirectory = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            baseDirectory = xdgDataHome != null && !xdgDataHome.isBlank()
                    ? Paths.get(xdgDataHome)
                    : Paths.get(System.getProperty("user.home"), ".local", "share");
        }

        return ensureDirectory(baseDirectory.resolve(APP_NAME).toAbsolutePath().normalize());
    }

    public static Path configDirectory() {
        return ensureDirectory(appHome().resolve("config"));
    }

    public static Path configFile() {
        return configDirectory().resolve("app_config.ser");
    }

    public static Path defaultDataDirectory() {
        return ensureDirectory(appHome().resolve("data"));
    }

    public static Path defaultExportDirectory() {
        return ensureDirectory(appHome().resolve("exports"));
    }

    public static Path backupDirectory() {
        return ensureDirectory(appHome().resolve("backups"));
    }

    public static Path logDirectory() {
        String override = System.getProperty(HOME_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return ensureDirectory(appHome().resolve("logs"));
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path logBaseDirectory;
        if (osName.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            logBaseDirectory = localAppData != null && !localAppData.isBlank()
                    ? Paths.get(localAppData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Local");
            return ensureDirectory(logBaseDirectory.resolve(APP_NAME).resolve("logs").toAbsolutePath().normalize());
        }
        if (osName.contains("mac")) {
            return ensureDirectory(Paths.get(System.getProperty("user.home"), "Library", "Logs", APP_NAME)
                    .toAbsolutePath().normalize());
        }

        String xdgStateHome = System.getenv("XDG_STATE_HOME");
        logBaseDirectory = xdgStateHome != null && !xdgStateHome.isBlank()
                ? Paths.get(xdgStateHome)
                : Paths.get(System.getProperty("user.home"), ".local", "state");
        return ensureDirectory(logBaseDirectory.resolve(APP_NAME).resolve("logs").toAbsolutePath().normalize());
    }

    public static Path resolveDataDirectory() {
        return resolveConfiguredDirectory(AppConfigurationService.getConfiguration().getDataDirectory(),
                defaultDataDirectory());
    }

    public static Path resolveExportDirectory() {
        return resolveConfiguredDirectory(AppConfigurationService.getConfiguration().getExportDirectory(),
                defaultExportDirectory());
    }

    public static Path resolveDataFile(String fileName) {
        return resolveDataDirectory().resolve(fileName).toAbsolutePath().normalize();
    }

    public static Path resolveConfiguredDirectory(String configuredDirectory, Path fallbackDirectory) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return ensureDirectory(fallbackDirectory.toAbsolutePath().normalize());
        }

        Path configuredPath = Paths.get(configuredDirectory.trim());
        if (!configuredPath.isAbsolute()) {
            configuredPath = appHome().resolve(configuredPath);
        }
        return ensureDirectory(configuredPath.toAbsolutePath().normalize());
    }

    public static void migrateLegacyDirectoryIfNeeded(String legacyDirectoryName, Path targetDirectory) {
        Path sourceDirectory = Paths.get(legacyDirectoryName).toAbsolutePath().normalize();
        Path resolvedTarget = ensureDirectory(targetDirectory.toAbsolutePath().normalize());

        if (sourceDirectory.equals(resolvedTarget) || !Files.isDirectory(sourceDirectory)) {
            return;
        }

        try (Stream<Path> stream = Files.list(sourceDirectory)) {
            stream.filter(Files::isRegularFile).forEach(sourceFile -> {
                Path targetFile = resolvedTarget.resolve(sourceFile.getFileName());
                if (Files.exists(targetFile)) {
                    return;
                }
                try {
                    Files.copy(sourceFile, targetFile);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static Path ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create application directory: " + directory, e);
        }
        return directory;
    }
}

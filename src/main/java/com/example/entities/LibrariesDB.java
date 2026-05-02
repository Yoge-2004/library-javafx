package com.example.entities;

import com.example.storage.AppPaths;
import com.example.storage.DataStorage;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent database for storing the list of known libraries.
 * This ensures that library names are decoupled from the main configuration
 * and can be managed independently.
 */
public final class LibrariesDB implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LibrariesDB.class.getName());
    private static final String FILE_NAME = "libraries_db.ser";

    private static volatile LibrariesDB instance;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<String> libraries = new ArrayList<>();

    private LibrariesDB() {}

    /**
     * Gets the singleton instance, loading from disk if available.
     */
    public static LibrariesDB getInstance() {
        if (instance == null) {
            synchronized (LibrariesDB.class) {
                if (instance == null) {
                    instance = loadFromFile();
                    if (instance == null) {
                        instance = new LibrariesDB();
                        LOGGER.info("Created new empty libraries database");
                    }
                }
            }
        }
        return instance;
    }

    private static LibrariesDB loadFromFile() {
        try {
            String path = AppPaths.configDirectory().resolve(FILE_NAME).toString();
            return DataStorage.readSerialized(path, LibrariesDB.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load libraries database from file", e);
            return null;
        }
    }

    /**
     * Forces a reload from the database snapshot if connected.
     */
    public void forceReload() {
        lock.writeLock().lock();
        try {
            byte[] snapshot = com.example.services.DatabaseConnectionService.loadSnapshot("libraries_db.ser");
            if (snapshot != null) {
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(snapshot);
                     java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bis)) {
                    LibrariesDB loaded = (LibrariesDB) ois.readObject();
                    if (loaded != null) {
                        this.libraries.clear();
                        this.libraries.addAll(loaded.libraries);
                        LOGGER.info("Libraries database synchronized from SQL snapshot (libraries_db.ser)");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reload libraries from database snapshot", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a copy of the known libraries list.
     */
    public List<String> getLibraries() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(libraries);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a new library to the database if it doesn't already exist.
     */
    public void addLibrary(String name) {
        if (name == null || name.isBlank()) return;
        lock.writeLock().lock();
        try {
            String trimmed = name.trim();
            if (!libraries.contains(trimmed)) {
                libraries.add(trimmed);
                save();
                LOGGER.log(Level.INFO, "New library branch added: {0}", trimmed);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a library from the database.
     */
    public void removeLibrary(String name) {
        lock.writeLock().lock();
        try {
            if (libraries.remove(name)) {
                save();
                LOGGER.log(Level.INFO, "Library branch removed: {0}", name);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void save() {
        try {
            String path = AppPaths.configDirectory().resolve(FILE_NAME).toString();
            DataStorage.writeSerialized(path, this);

            // Mirror to SQL database if connected
            try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                 java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
                oos.writeObject(this);
                com.example.services.DatabaseConnectionService.saveSnapshot("libraries_db", bos.toByteArray());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save libraries database", e);
        }
    }
}

package com.example.storage;

import com.example.services.DatabaseConnectionService;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe utility class for handling serialization operations with improved error handling,
 * atomic operations, and Windows compatibility.
 */
public final class DataStorage {
    private static final Logger LOGGER = Logger.getLogger(DataStorage.class.getName());

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Private constructor to prevent instantiation
    private DataStorage() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Reads a serialized object from file with type safety.
     *
     * @param <T> the type of object to read
     * @param filename the file path
     * @param clazz the class type for casting
     * @return the deserialized object or null if file doesn't exist
     * @throws IOException if read operation fails
     * @throws ClassNotFoundException if class cannot be found during deserialization
     */
    public static <T> T readSerialized(String filename, Class<T> clazz)
            throws IOException, ClassNotFoundException {

        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        if (clazz == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }

        Path filePath = Paths.get(filename);
        if (!Files.exists(filePath)) {
            byte[] snapshot = DatabaseConnectionService.loadSnapshot(snapshotKey(filePath));
            if (snapshot != null) {
                LOGGER.log(Level.INFO, "Recovered {0} from database snapshot", filename);
                return clazz.cast(deserialize(snapshot, clazz));
            }
            LOGGER.log(Level.INFO, "File does not exist: {0}", filename);
            return null;
        }

        lock.readLock().lock();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {

            Object obj = ois.readObject();
            LOGGER.log(Level.FINE, "Successfully read object from: {0}", filename);
            return clazz.cast(obj);

        } catch (ClassCastException e) {
            throw new IOException("Object in file cannot be cast to " + clazz.getSimpleName(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Writes an object to file using serialization with improved atomic operation handling.
     * FIXED: Removed unreachable FileAlreadyExistsException catch block.
     *
     * @param filename the file path
     * @param obj the object to serialize
     * @throws IOException if write operation fails
     */
    public static void writeSerialized(String filename, Object obj) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        if (obj == null) {
            throw new IllegalArgumentException("Object to serialize cannot be null");
        }

        Path filePath = Paths.get(filename);
        Path parentDir = filePath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        String tempFileName = filename + ".tmp." + System.currentTimeMillis() + "." + Thread.currentThread().threadId();
        Path tempPath = Paths.get(tempFileName);

        lock.writeLock().lock();
        try {
            // Write to temporary file first
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING)))) {

                oos.writeObject(obj);
                oos.flush();
            }

            // Attempt atomic move with different strategies
            boolean moveSuccessful = false;

            // Strategy 1: Try atomic move with replace
            try {
                Files.move(tempPath, filePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                moveSuccessful = true;
                LOGGER.log(Level.FINE, "Successfully wrote object to: {0} (atomic)", filename);

            } catch (AtomicMoveNotSupportedException e1) {
                // Strategy 2: Try non-atomic move with replace
                try {
                    Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    moveSuccessful = true;
                    LOGGER.log(Level.FINE, "Successfully wrote object to: {0} (non-atomic)", filename);

                } catch (IOException e2) {
                    // Strategy 3: Delete existing file first, then move
                    try {
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                        Files.move(tempPath, filePath);
                        moveSuccessful = true;
                        LOGGER.log(Level.FINE, "Successfully wrote object to: {0} (delete-first)", filename);

                    } catch (IOException e3) {
                        // Strategy 4: Copy and delete (last resort)
                        try {
                            Files.copy(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                            Files.delete(tempPath);
                            moveSuccessful = true;
                            LOGGER.log(Level.FINE, "Successfully wrote object to: {0} (copy-delete)", filename);

                        } catch (IOException e4) {
                            LOGGER.log(Level.SEVERE, "All write strategies failed for: {0}", filename);
                            throw new IOException("Failed to write file after trying all strategies. " +
                                    "Original error: " + e1.getMessage() +
                                    ". Final error: " + e4.getMessage(), e4);
                        }
                    }
                }
            }

            if (moveSuccessful) {
                mirrorSnapshot(filePath, obj);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write to temporary file: " + tempFileName, e);
            throw new IOException("Failed to create temporary file for writing", e);
        } finally {
            // Clean up temporary file if it still exists
            try {
                if (Files.exists(tempPath)) {
                    Files.delete(tempPath);
                }
            } catch (IOException cleanupException) {
                LOGGER.log(Level.WARNING, "Failed to clean up temporary file: " + tempPath, cleanupException);
            }
            lock.writeLock().unlock();
        }
    }

    /**
     * Reads a serialized map from file.
     *
     * @param filename the file path
     * @return the deserialized map or null if file doesn't exist
     * @throws IOException if read operation fails
     * @throws ClassNotFoundException if class cannot be found during deserialization
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> readSerializedMap(String filename)
            throws IOException, ClassNotFoundException {

        Object obj = readSerialized(filename, Object.class);
        if (obj == null) {
            return null;
        }

        try {
            return (Map<String, List<String>>) obj;
        } catch (ClassCastException e) {
            throw new IOException("File does not contain a valid Map<String, List<String>>", e);
        }
    }

    /**
     * Writes a map to file using serialization.
     *
     * @param filename the file path
     * @param map the map to serialize
     * @throws IOException if write operation fails
     */
    public static void writeSerializedMap(String filename, Map<String, List<String>> map)
            throws IOException {
        writeSerialized(filename, map);
    }

    /**
     * Reads a serialized nested map from file.
     *
     * @param filename the file path
     * @return the deserialized nested map or null if file doesn't exist
     * @throws IOException if read operation fails
     * @throws ClassNotFoundException if class cannot be found during deserialization
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Integer>> readSerializedNestedMap(String filename)
            throws IOException, ClassNotFoundException {

        Object obj = readSerialized(filename, Object.class);
        if (obj == null) {
            return null;
        }

        try {
            return (Map<String, Map<String, Integer>>) obj;
        } catch (ClassCastException e) {
            throw new IOException("File does not contain a valid Map<String, Map<String, Integer>>", e);
        }
    }

    /**
     * Writes a nested map to file using serialization.
     *
     * @param filename the file path
     * @param map the nested map to serialize
     * @throws IOException if write operation fails
     */
    public static void writeSerializedNestedMap(String filename, Map<String, Map<String, Integer>> map)
            throws IOException {
        writeSerialized(filename, map);
    }

    /**
     * Checks if a file exists and is readable.
     *
     * @param filename the file path to check
     * @return true if file exists and is readable
     */
    public static boolean fileExists(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        Path filePath = Paths.get(filename);
        return Files.exists(filePath) && Files.isReadable(filePath);
    }

    /**
     * Safely deletes a file if it exists.
     *
     * @param filename the file path to delete
     * @return true if file was deleted or didn't exist
     */
    public static boolean deleteFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        try {
            return Files.deleteIfExists(Paths.get(filename));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete file: " + filename, e);
            return false;
        }
    }

    /**
     * Creates a backup copy of a file before modification.
     *
     * @param filename the file to backup
     * @return true if backup was created successfully
     */
    public static boolean createBackup(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        Path originalPath = Paths.get(filename);
        if (!Files.exists(originalPath)) {
            return false;
        }

        try {
            String backupFileName = filename + ".backup." + System.currentTimeMillis();
            Path backupPath = Paths.get(backupFileName);
            Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Created backup: " + backupFileName);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create backup for: " + filename + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the size of a file in bytes.
     *
     * @param filename the file path
     * @return file size in bytes, or -1 if file doesn't exist or error occurs
     */
    public static long getFileSize(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return -1;
        }

        try {
            Path filePath = Paths.get(filename);
            return Files.exists(filePath) ? Files.size(filePath) : -1;
        } catch (IOException e) {
            System.err.println("Failed to get file size for: " + filename + " - " + e.getMessage());
            return -1;
        }
    }

    /**
     * Ensures a directory exists, creating it if necessary.
     *
     * @param directoryPath the directory path to ensure
     * @return true if directory exists or was created successfully
     */
    public static boolean ensureDirectoryExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return false;
        }

        try {
            Path dirPath = Paths.get(directoryPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Created directory: " + directoryPath);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + directoryPath + " - " + e.getMessage());
            return false;
        }
    }

    private static void mirrorSnapshot(Path filePath, Object obj) {
        try {
            DatabaseConnectionService.saveSnapshot(snapshotKey(filePath), serialize(obj));
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to serialize snapshot for database mirror: " + filePath, ex);
        }
    }

    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private static <T> T deserialize(byte[] payload, Class<T> clazz) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(payload);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return clazz.cast(ois.readObject());
        } catch (ClassCastException ex) {
            throw new IOException("Object in snapshot cannot be cast to " + clazz.getSimpleName(), ex);
        }
    }

    private static String snapshotKey(Path filePath) {
        Path fileName = filePath.getFileName();
        return fileName != null ? fileName.toString() : filePath.toString();
    }
}

package world.landfall.statecraft.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;
import org.bson.BsonDocument;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.api.queue.QueuedOperation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Persistent storage for the operation queue.
 *
 * <p>Queued operations are saved to disk so they survive server restarts.
 * This ensures no operations are lost if the server crashes while the
 * Statecraft API is unreachable.
 */
public final class OperationQueueStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String QUEUE_FILE = "operation_queue.json";

    private static Path queueFilePath;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private OperationQueueStore() {
        // Utility class
    }

    /**
     * Initializes the queue store with the world directory.
     *
     * @param worldPath The world directory path
     */
    public static void initialize(Path worldPath) {
        lock.writeLock().lock();
        try {
            Path statecraftDir = worldPath.resolve(StatecraftMod.MODID);
            Files.createDirectories(statecraftDir);
            queueFilePath = statecraftDir.resolve(QUEUE_FILE);

            LOGGER.atInfo().log("[OperationQueueStore] Initialized at: {}", queueFilePath);
        } catch (IOException e) {
            LOGGER.atSevere().log("[OperationQueueStore] Failed to initialize", e);
            throw new RuntimeException("Failed to initialize operation queue store", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves the operation queue to disk.
     *
     * @param operations The operations to save
     * @return True if successful
     */
    public static boolean save(List<QueuedOperation> operations) {
        if (queueFilePath == null) {
            LOGGER.atWarning().log("[OperationQueueStore] Not initialized, cannot save");
            return false;
        }

        lock.writeLock().lock();
        try {
            // Convert operations to JSON using Codec
            List<com.google.gson.JsonElement> jsonElements = new ArrayList<>();
            for (QueuedOperation op : operations) {
//                var result = QueuedOperation.CODEC.encodeStart(JsonOps.INSTANCE, op);
                var result = QueuedOperation.CODEC.encode(op, ExtraInfo.THREAD_LOCAL.get());
                if (!result.isEmpty()) {
                    jsonElements.add(JsonParser.parseString(result.toJson()));
                } else {
                    LOGGER.atSevere().log("[OperationQueueStore] Failed to encode operation: {}", op.id());
                }
            }

            try (Writer writer = Files.newBufferedWriter(queueFilePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(jsonElements, writer);
            }

            LOGGER.atFine().log("[OperationQueueStore] Saved {} operations to disk", operations.size());
            return true;

        } catch (Exception e) {
            LOGGER.atSevere().log("[OperationQueueStore] Failed to save operations", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads the operation queue from disk.
     *
     * @return The loaded operations, or empty list if none
     */
    public static List<QueuedOperation> load() {
        if (queueFilePath == null) {
            LOGGER.atWarning().log("[OperationQueueStore] Not initialized, cannot load");
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            if (!Files.exists(queueFilePath)) {
                return new ArrayList<>();
            }

            try (Reader reader = Files.newBufferedReader(queueFilePath)) {
                List<com.google.gson.JsonElement> jsonElements = GSON.fromJson(
                    reader,
                    new TypeToken<List<com.google.gson.JsonElement>>() {}.getType()
                );

                if (jsonElements == null) {
                    return new ArrayList<>();
                }

                List<QueuedOperation> operations = new ArrayList<>();
                for (com.google.gson.JsonElement element : jsonElements) {
//                    var result = QueuedOperation.CODEC.parse(JsonOps.INSTANCE, element);
                    var result = QueuedOperation.CODEC.decode(BsonDocument.parse(element.getAsString()), ExtraInfo.THREAD_LOCAL.get());
                    if (result != null) {
                        operations.add(result);
                    } else {
                        LOGGER.atSevere().log("[OperationQueueStore] Failed to decode operation: {}", element);
                    }
                }

                LOGGER.atInfo().log("[OperationQueueStore] Loaded {} operations from disk", operations.size());
                return operations;
            }

        } catch (Exception e) {
            LOGGER.atSevere().log("[OperationQueueStore] Failed to load operations", e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Deletes the queue file.
     *
     * @return True if deleted or didn't exist
     */
    public static boolean delete() {
        if (queueFilePath == null) {
            return true;
        }

        lock.writeLock().lock();
        try {
            return Files.deleteIfExists(queueFilePath);
        } catch (IOException e) {
            LOGGER.atSevere().log("[OperationQueueStore] Failed to delete queue file", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

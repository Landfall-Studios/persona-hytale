package world.landfall.statecraft.api.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import world.landfall.statecraft.api.ApiConnectionManager;
import world.landfall.statecraft.api.ApiResult;
import world.landfall.statecraft.api.StatecraftApi;
import world.landfall.statecraft.api.queue.QueuedOperation;
import world.landfall.statecraft.config.StatecraftConfig;
import world.landfall.statecraft.storage.OperationQueueStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Implementation of the API connection manager.
 *
 * <p>This handles:
 * <ul>
 *   <li>Periodic health checks</li>
 *   <li>Operation queueing when offline</li>
 *   <li>Queue processing when connection is restored</li>
 *   <li>State change notifications</li>
 * </ul>
 */
public class ApiConnectionManagerImpl implements ApiConnectionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final StatecraftApi api;
    private final ScheduledExecutorService scheduler;
    private final List<QueuedOperation> operationQueue;
    private final List<Consumer<ConnectionState>> stateListeners;

    private final AtomicReference<ConnectionState> currentState = new AtomicReference<>(ConnectionState.UNKNOWN);
    private final AtomicReference<HytaleServer> server = new AtomicReference<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);

    // Statistics
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong lastFailedCheck = new AtomicLong(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger totalQueued = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);

    public ApiConnectionManagerImpl(StatecraftApi api) {
        this.api = api;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("Statecraft-Connection-Manager");
            thread.setDaemon(true);
            return thread;
        });
        this.operationQueue = new CopyOnWriteArrayList<>();
        this.stateListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void start(HytaleServer server) {
        if (isRunning.get()) {
            LOGGER.atWarning().log("[ConnectionManager] Already running");
            return;
        }

        this.server.set(server);
        this.isRunning.set(true);

        // Load queued operations from disk
        List<QueuedOperation> savedOps = OperationQueueStore.load();
        if (!savedOps.isEmpty()) {
            operationQueue.addAll(savedOps);
            LOGGER.atInfo().log("[ConnectionManager] Loaded %d queued operations from disk", savedOps.size());
        }

        // Perform initial health check
        checkHealth();

        // Schedule periodic health checks
        int interval = StatecraftConfig.HEALTH_CHECK_INTERVAL_SECONDS.get();
        scheduler.scheduleAtFixedRate(
            this::performHealthCheck,
            interval, interval, TimeUnit.SECONDS
        );

        LOGGER.atInfo().log("[ConnectionManager] Started with %ds health check interval", interval);
    }

    @Override
    public void stop() {
        if (!isRunning.get()) {
            return;
        }

        isRunning.set(false);

        // Try to process queue before shutdown
        if (!operationQueue.isEmpty() && currentState.get() == ConnectionState.CONNECTED) {
            LOGGER.atInfo().log("[ConnectionManager] Processing %d queued operations before shutdown", operationQueue.size());
            processQueue();
        }

        // Save remaining queue to disk
        if (!operationQueue.isEmpty()) {
            OperationQueueStore.save(new ArrayList<>(operationQueue));
            LOGGER.atInfo().log("[ConnectionManager] Saved %d operations to disk", operationQueue.size());
        }

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.atInfo().log("[ConnectionManager] Stopped");
    }

    @Override
    public ConnectionState getState() {
        return currentState.get();
    }

    @Override
    public boolean checkHealth() {
        ApiResult<Boolean> result = api.testConnection();

        if (result.isSuccess()) {
            lastSuccessfulCheck.set(System.currentTimeMillis());
            consecutiveFailures.set(0);

            ConnectionState oldState = currentState.getAndSet(ConnectionState.CONNECTED);

            if (oldState != ConnectionState.CONNECTED) {
                LOGGER.atInfo().log("[ConnectionManager] Connection established to Statecraft API");
                notifyStateChange(ConnectionState.CONNECTED);

                // Process queue on reconnect - use atomic flag to prevent duplicate processing
                if (!operationQueue.isEmpty() && isProcessingQueue.compareAndSet(false, true)) {
                    scheduler.execute(() -> {
                        try {
                            processQueue();
                        } finally {
                            isProcessingQueue.set(false);
                        }
                    });
                }
            }

            return true;
        } else {
            lastFailedCheck.set(System.currentTimeMillis());
            int failures = consecutiveFailures.incrementAndGet();

            ConnectionState oldState = currentState.get();
            ConnectionState newState = oldState;

            if (oldState == ConnectionState.CONNECTED) {
                newState = ConnectionState.RECONNECTING;
                LOGGER.atWarning().log("[ConnectionManager] Lost connection to Statecraft API: %s",
                    result.getErrorMessage().orElse("Unknown error"));
            } else if (failures >= 3 && oldState != ConnectionState.DISCONNECTED) {
                newState = ConnectionState.DISCONNECTED;
            }

            if (newState != oldState && currentState.compareAndSet(oldState, newState)) {
                notifyStateChange(newState);
            }

            return false;
        }
    }

    private void performHealthCheck() {
        if (!isRunning.get()) return;

        try {
            checkHealth();
        } catch (Exception e) {
            LOGGER.atSevere().log("[ConnectionManager] Error during health check: %s", e);
        }
    }

    @Override
    public boolean queueOperation(QueuedOperation operation) {
        int maxSize = StatecraftConfig.QUEUE_MAX_SIZE.get();

        if (operationQueue.size() >= maxSize) {
            LOGGER.atWarning().log("[ConnectionManager] Queue is full (%d/%d), rejecting operation: %s",
                operationQueue.size(), maxSize, operation.getDescription());
            return false;
        }

        operationQueue.add(operation);
        totalQueued.incrementAndGet();

        // Persist to disk
        OperationQueueStore.save(new ArrayList<>(operationQueue));

        LOGGER.atInfo().log("[ConnectionManager] Queued operation: %s (queue size: %d)",
            operation.getDescription(), operationQueue.size());

        return true;
    }

    @Override
    public int getQueueSize() {
        return operationQueue.size();
    }

    @Override
    public List<QueuedOperation> getQueuedOperations() {
        return Collections.unmodifiableList(new ArrayList<>(operationQueue));
    }

    @Override
    public int processQueue() {
        if (currentState.get() != ConnectionState.CONNECTED) {
            LOGGER.atFine().log("[ConnectionManager] Cannot process queue - not connected");
            return 0;
        }

        if (operationQueue.isEmpty()) {
            return 0;
        }

        LOGGER.atInfo().log("[ConnectionManager] Processing %d queued operations", operationQueue.size());

        int processed = 0;
        int ttlHours = StatecraftConfig.QUEUE_OPERATION_TTL_HOURS.get();

        // Process operations in order
        List<QueuedOperation> toRemove = new ArrayList<>();

        for (QueuedOperation op : operationQueue) {
            // Check if expired
            if (op.isExpired(ttlHours)) {
                LOGGER.atWarning().log("[ConnectionManager] Operation expired, discarding: %s", op.getDescription());
                toRemove.add(op);
                totalFailed.incrementAndGet();
                continue;
            }

            // Try to execute
            boolean success = executeOperation(op);

            if (success) {
                toRemove.add(op);
                processed++;
                totalProcessed.incrementAndGet();
                LOGGER.atFine().log("[ConnectionManager] Successfully processed: %s", op.getDescription());
            } else if (op.canRetry()) {
                // Replace with retry version
                int index = operationQueue.indexOf(op);
                if (index >= 0) {
                    operationQueue.set(index, op.withRetry());
                }
                LOGGER.atFine().log("[ConnectionManager] Will retry: %s (attempt %d)",
                    op.getDescription(), op.retryCount() + 1);
            } else {
                toRemove.add(op);
                totalFailed.incrementAndGet();
                LOGGER.atSevere().log("[ConnectionManager] Max retries exceeded, discarding: %s", op.getDescription());
            }
        }

        operationQueue.removeAll(toRemove);

        // Save updated queue
        OperationQueueStore.save(new ArrayList<>(operationQueue));

        LOGGER.atInfo().log("[ConnectionManager] Processed {} operations, {} remaining",
            processed, operationQueue.size());

        return processed;
    }

    private boolean executeOperation(QueuedOperation op) {
        try {
            ApiResult<?> result = switch (op.type()) {
                case CREATE_CHARACTER -> api.recordCharacter(op.playerId(), op.characterName());
                case MARK_DECEASED -> api.markDeceased(op.characterId(), op.characterName());
                case SET_PASSWORD -> {
                    // Password operations don't have enough info to retry
                    LOGGER.atWarning().log("[ConnectionManager] Cannot retry password operation");
                    yield ApiResult.validationError("Cannot retry password operations");
                }
                default -> {
                    LOGGER.atWarning().log("[ConnectionManager] Unsupported operation type: %s", op.type());
                    yield ApiResult.validationError("Unsupported operation type");
                }
            };

            return result.isSuccess();
        } catch (Exception e) {
            LOGGER.atSevere().log("[ConnectionManager] Error executing operation: %s", op.getDescription(), e);
            return false;
        }
    }

    @Override
    public void clearQueue() {
        int size = operationQueue.size();
        operationQueue.clear();
        OperationQueueStore.delete();
        LOGGER.atWarning().log("[ConnectionManager] Cleared %d queued operations", size);
    }

    @Override
    public void addStateChangeListener(Consumer<ConnectionState> listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateChangeListener(Consumer<ConnectionState> listener) {
        stateListeners.remove(listener);
    }

    private void notifyStateChange(ConnectionState newState) {
        for (Consumer<ConnectionState> listener : stateListeners) {
            try {
                listener.accept(newState);
            } catch (Exception e) {
                LOGGER.atSevere().log("[ConnectionManager] Error notifying state change listener: %s", e);
            }
        }
    }

    @Override
    public ConnectionStats getStats() {
        return new ConnectionStats(
            currentState.get(),
            lastSuccessfulCheck.get(),
            lastFailedCheck.get(),
            consecutiveFailures.get(),
            totalQueued.get(),
            totalProcessed.get(),
            totalFailed.get()
        );
    }
}

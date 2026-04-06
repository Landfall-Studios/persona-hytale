package world.landfall.statecraft.api;

import com.hypixel.hytale.server.core.HytaleServer;
import world.landfall.statecraft.api.queue.QueuedOperation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the connection state to the Statecraft API.
 *
 * <p>This component is responsible for:
 * <ul>
 *   <li>Tracking connection health</li>
 *   <li>Queueing operations when offline</li>
 *   <li>Processing the queue when connection is restored</li>
 *   <li>Notifying listeners of connection state changes</li>
 * </ul>
 */
public interface ApiConnectionManager {

    /**
     * Connection states.
     */
    enum ConnectionState {
        /**
         * Connection has not been tested yet.
         */
        UNKNOWN,

        /**
         * API is reachable and responding.
         */
        CONNECTED,

        /**
         * API is not reachable.
         */
        DISCONNECTED,

        /**
         * Connection was lost but attempting to reconnect.
         */
        RECONNECTING
    }

    /**
     * Starts the connection manager.
     *
     * @param server The Minecraft server instance
     */
    void start(HytaleServer server);

    /**
     * Stops the connection manager and processes remaining queue.
     */
    void stop();

    /**
     * Gets the current connection state.
     *
     * @return The current state
     */
    ConnectionState getState();

    /**
     * Returns true if the API is currently reachable.
     */
    default boolean isConnected() {
        return getState() == ConnectionState.CONNECTED;
    }

    /**
     * Performs an immediate health check.
     *
     * @return True if the API is reachable
     */
    boolean checkHealth();

    /**
     * Adds an operation to the queue.
     *
     * <p>Operations are queued when the API is unreachable and
     * will be processed when connection is restored.
     *
     * @param operation The operation to queue
     * @return True if the operation was queued, false if queue is full
     */
    boolean queueOperation(QueuedOperation operation);

    /**
     * Gets the number of operations currently in the queue.
     *
     * @return The queue size
     */
    int getQueueSize();

    /**
     * Gets all queued operations (for display purposes).
     *
     * @return Unmodifiable list of queued operations
     */
    List<QueuedOperation> getQueuedOperations();

    /**
     * Processes the operation queue.
     *
     * <p>This is called automatically when connection is restored,
     * but can also be called manually.
     *
     * @return The number of operations successfully processed
     */
    int processQueue();

    /**
     * Clears all queued operations.
     *
     * <p>Use with caution - this will discard pending operations.
     */
    void clearQueue();

    /**
     * Registers a listener for connection state changes.
     *
     * @param listener The listener to register
     */
    void addStateChangeListener(Consumer<ConnectionState> listener);

    /**
     * Unregisters a state change listener.
     *
     * @param listener The listener to remove
     */
    void removeStateChangeListener(Consumer<ConnectionState> listener);

    /**
     * Gets statistics about the connection.
     *
     * @return Connection statistics
     */
    ConnectionStats getStats();

    /**
     * Statistics about the API connection.
     */
    record ConnectionStats(
        ConnectionState currentState,
        long lastSuccessfulCheck,
        long lastFailedCheck,
        int consecutiveFailures,
        int totalOperationsQueued,
        int totalOperationsProcessed,
        int totalOperationsFailed
    ) {}
}

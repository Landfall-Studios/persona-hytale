package world.landfall.statecraft.api.queue;


import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Represents an operation that has been queued for later execution.
 *
 * <p>Operations are queued when the Statecraft API is unreachable
 * and will be processed when connection is restored.
 */
public class QueuedOperation {
    String id;
    OperationType type;
    UUID playerId;
    Long characterId;
    String characterName;
    Instant queuedAt;
    int retryCount;

    public QueuedOperation(String id, OperationType type, UUID playerId, Long characterId, String characterName, Instant queuedAt, int retryCount) {
        this.id = id;
        this.type = type;
        this.playerId = playerId;
        this.characterId = characterId;
        this.characterName = characterName;
        this.queuedAt = queuedAt;
        this.retryCount = retryCount;
    }

    public String id() {
        return id;
    }
    public OperationType type() {
        return type;
    }
    public UUID playerId() {
        return playerId;
    }
    public Long characterId() {
        return characterId;
    }
    public String characterName() {
        return characterName;
    }
    public Instant queuedAt() {
        return queuedAt;
    }
    public int retryCount() {
        return retryCount;
    }

    /**
     * Maximum number of times an operation can be retried.
     */
    public static final int MAX_RETRIES = 5;

    /**
     * Codec for serialization to disk.
     * TODO fix for hytale
     */
//    public static final Codec<QueuedOperation> CODEC = RecordCodecBuilder.create(instance ->
//        instance.group(
//            Codec.STRING.fieldOf("id").forGetter(QueuedOperation::id),
//            Codec.STRING.fieldOf("type").xmap(OperationType::valueOf, OperationType::name).forGetter(QueuedOperation::type),
//            UUIDUtil.CODEC.fieldOf("playerId").forGetter(QueuedOperation::playerId),
//            Codec.LONG.optionalFieldOf("characterId", 0L).forGetter(QueuedOperation::characterId),
//            Codec.STRING.optionalFieldOf("characterName", "").forGetter(QueuedOperation::characterName),
//            Codec.LONG.fieldOf("queuedAt").xmap(Instant::ofEpochMilli, Instant::toEpochMilli).forGetter(QueuedOperation::queuedAt),
//            Codec.INT.fieldOf("retryCount").forGetter(QueuedOperation::retryCount)
//        ).apply(instance, QueuedOperation::new)
//    );
    public static final BuilderCodec<QueuedOperation> CODEC = BuilderCodec.builder(QueuedOperation.class, QueuedOperation::create)
            .append(new KeyedCodec<>("Id", BuilderCodec.STRING),
                    (data, value) -> data.id = value,
                    (data) -> data.id).add()
            .append(new KeyedCodec<>("Type", BuilderCodec.STRING),
                    (data, value) -> data.type = OperationType.valueOf(value),
                    (data) -> data.type.name()).add()
            .append(new KeyedCodec<>("PlayerID", BuilderCodec.UUID_BINARY),
                    (data, value) -> data.playerId = value,
                    (data) -> data.playerId).add()
            .append(new KeyedCodec<>("CharacterID", BuilderCodec.LONG),
                    (data, value) -> data.characterId = value,
                    (data) -> data.characterId).add()
            .append(new KeyedCodec<>("CharacterName", BuilderCodec.STRING),
                    (data, value) -> data.characterName = value,
                    (data) -> data.characterName).add()
            .append(new KeyedCodec<>("QueuedAt", BuilderCodec.INSTANT),
                    (data, value) -> data.queuedAt = value,
                    (data) -> data.queuedAt).add()
            .append(new KeyedCodec<>("RetryCount", BuilderCodec.INTEGER),
                    (data, value) -> data.retryCount = value,
                    (data) -> data.retryCount).add()
            .build();

    /**
     * Creates a new queued operation.
     */
    public static QueuedOperation create(
        OperationType type,
        UUID playerId,
        Long characterId,
        String characterName
    ) {
        return new QueuedOperation(
            UUID.randomUUID().toString(),
            type,
            playerId,
            characterId != null ? characterId : 0L,
            characterName != null ? characterName : "",
            Instant.now(),
            0
        );
    }
    /**
     * Creates an empty queued operation (should only be used for CODECs).
     */
    private static QueuedOperation create() {
        return new QueuedOperation("", OperationType.CREATE_CHARACTER, UUID.randomUUID(), 0L, "", Instant.EPOCH, 0);
    }
    /**
     * Creates a new operation with incremented retry count.
     */
    public QueuedOperation withRetry() {
        return new QueuedOperation(
            id, type, playerId, characterId, characterName, queuedAt, retryCount + 1
        );
    }

    /**
     * Checks if this operation can be retried.
     */
    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }

    /**
     * Checks if this operation has expired.
     *
     * @param ttlHours Time-to-live in hours
     */
    public boolean isExpired(int ttlHours) {
        Instant expiry = queuedAt.plusSeconds(ttlHours * 3600L);
        return Instant.now().isAfter(expiry);
    }

    /**
     * Gets a description of this operation for logging/display.
     */
    public String getDescription() {
        return switch (type) {
            case CREATE_CHARACTER -> "Create character '" + characterName + "' for " + playerId;
            case UPDATE_CHARACTER -> "Update character " + characterId + " ('" + characterName + "')";
            case DELETE_CHARACTER -> "Delete character " + characterId + " ('" + characterName + "')";
            case MARK_DECEASED -> "Mark character " + characterId + " ('" + characterName + "') as deceased";
            case SET_PASSWORD -> "Set password for " + playerId;
        };
    }

    /**
     * Types of operations that can be queued.
     */
    public enum OperationType {
        CREATE_CHARACTER,
        UPDATE_CHARACTER,
        DELETE_CHARACTER,
        MARK_DECEASED,
        SET_PASSWORD
    }
}

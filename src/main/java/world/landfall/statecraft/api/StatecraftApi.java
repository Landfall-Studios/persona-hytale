package world.landfall.statecraft.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for communicating with the Statecraft API.
 *
 * <p>All methods return {@link ApiResult} to provide detailed error information
 * and allow callers to handle failures appropriately.
 *
 * <p>Implementations should handle:
 * <ul>
 *   <li>Connection timeouts</li>
 *   <li>Retry logic with exponential backoff</li>
 *   <li>Proper error classification</li>
 * </ul>
 */
public interface StatecraftApi {

    /**
     * Tests if the API is reachable.
     *
     * @return Result indicating connection status
     */
    ApiResult<Boolean> testConnection();

    /**
     * Gets all characters belonging to a player.
     *
     * @param playerUuid The player's Minecraft UUID
     * @return List of characters, or empty list if player has none
     */
    ApiResult<List<StatecraftCharacter>> getCharactersByPlayer(UUID playerUuid);

    /**
     * Gets a specific character by ID.
     *
     * @param characterId The Statecraft character ID
     * @return The character, or empty if not found
     */
    ApiResult<StatecraftCharacter> getCharacterById(long characterId);

    /**
     * Gets all nations currently in the database.
     *
     * @return List of nations, or empty list if there are none
     */
    ApiResult<List<StatecraftNation>> getNations();

    /**
     * Gets all members of this nation.
     * @param nationId The Statecraft nation ID
     * @return List of nation members, or empty list if there are none
     */
    ApiResult<List<StatecraftNationMember>> getNationMembers(long nationId);

    /**
     * Gets the nation this character belongs to, if any.
     * @param characterId The Statecraft character ID
     * @return The nation, or empty if not found
     */
    ApiResult<Optional<StatecraftNation>> getNationOfCharacter(long characterId);

    /**
     * Creates a new character or updates an existing one.
     *
     * @param playerUuid  The player's Minecraft UUID
     * @param displayName The character's display name
     * @return The created/updated character with its assigned ID
     */
    ApiResult<StatecraftCharacter> recordCharacter(UUID playerUuid, String displayName);

    /**
     * Marks a character as deceased.
     *
     * @param characterId The character ID
     * @param displayName The character's display name (for logging/verification)
     * @return True if successful
     */
    ApiResult<Boolean> markDeceased(long characterId, String displayName);

    /**
     * Sets a player's web panel password.
     *
     * @param playerUuid   The player's Minecraft UUID
     * @param passwordHash The hashed password
     * @param salt         The salt used for hashing
     * @return True if successful
     */
    ApiResult<Boolean> setPassword(UUID playerUuid, String passwordHash, String salt);

    /**
     * Async version of {@link #getCharactersByPlayer(UUID)}.
     */
    default CompletableFuture<ApiResult<List<StatecraftCharacter>>> getCharactersByPlayerAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getCharactersByPlayer(playerUuid));
    }

    /**
     * Async version of {@link #recordCharacter(UUID, String)}.
     */
    default CompletableFuture<ApiResult<StatecraftCharacter>> recordCharacterAsync(UUID playerUuid, String displayName) {
        return CompletableFuture.supplyAsync(() -> recordCharacter(playerUuid, displayName));
    }
}

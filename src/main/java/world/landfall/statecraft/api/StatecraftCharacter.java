package world.landfall.statecraft.api;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a character as returned by the Statecraft API.
 *
 * <p>This is a data transfer object (DTO) that contains only the information
 * provided by the API. Local game state (inventory, location, etc.) is stored
 * separately in {@link world.landfall.statecraft.core.CharacterState}.
 */
public final class StatecraftCharacter {
    private long characterId;
    private String displayName;
    private UUID playerUuid;
    private boolean deceased;
    private Instant firstUsed;
    private Instant lastSeen;
    private String profilePictureUrl;
    private String biography;

    public static final BuilderCodec<StatecraftCharacter> CODEC =
            BuilderCodec.builder(StatecraftCharacter.class, StatecraftCharacter::create)
                    .append(new KeyedCodec<>("CharacterId", BuilderCodec.LONG),
                            (data, value) -> data.characterId = value,
                            data -> data.characterId).add()
                    .append(new KeyedCodec<>("DisplayName", BuilderCodec.STRING),
                            (data, value) -> data.displayName = value,
                            data -> data.displayName).add()
                    .append(new KeyedCodec<>("PlayerUUID", BuilderCodec.UUID_BINARY),
                            (data, value) -> data.playerUuid = value,
                            data -> data.playerUuid).add()
                    .append(new KeyedCodec<>("Deceased", BuilderCodec.BOOLEAN),
                            (data, value) -> data.deceased = value,
                            data -> data.deceased).add()
                    .append(new KeyedCodec<>("FirstUsed", BuilderCodec.INSTANT),
                            (data, value) -> data.firstUsed = value,
                            data -> data.firstUsed).add()
                    .append(new KeyedCodec<>("LastSeen", BuilderCodec.INSTANT),
                            (data, value) -> data.lastSeen = value,
                            data -> data.lastSeen).add()
                    .append(new KeyedCodec<>("ProfilePictureURL", BuilderCodec.STRING),
                            (data, value) -> data.profilePictureUrl = value,
                            data -> data.profilePictureUrl).add()
                    .append(new KeyedCodec<>("Biography", BuilderCodec.STRING),
                            (data, value) -> data.biography = value,
                            data -> data.biography).add()
                    .build();

    public StatecraftCharacter(long characterId, String displayName, UUID playerUuid) {
        this.characterId = characterId;
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.deceased = false;
        this.firstUsed = Instant.now();
        this.lastSeen = Instant.now();

    }

    public long getCharacterId() {
        return characterId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public Instant getFirstUsed() {
        return firstUsed;
    }

    public void setFirstUsed(Instant firstUsed) {
        this.firstUsed = firstUsed;
    }

    public void setFirstUsed(long epochMilli) {
        this.firstUsed = Instant.ofEpochMilli(epochMilli);
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setLastSeen(long epochMilli) {
        this.lastSeen = Instant.ofEpochMilli(epochMilli);
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public static StatecraftCharacter create() {
        return new StatecraftCharacter(0, "", UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatecraftCharacter that = (StatecraftCharacter) o;
        return characterId == that.characterId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(characterId);
    }

    @Override
    public String toString() {
        return "StatecraftCharacter{" +
               "id=" + characterId +
               ", name='" + displayName + '\'' +
               ", player=" + playerUuid +
               ", deceased=" + deceased +
               '}';
    }
}

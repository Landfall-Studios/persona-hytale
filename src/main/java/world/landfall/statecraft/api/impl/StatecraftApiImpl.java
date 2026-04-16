package world.landfall.statecraft.api.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import world.landfall.statecraft.api.*;
import world.landfall.statecraft.config.StatecraftConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the Statecraft API client.
 *
 * <p>This implementation handles:
 * <ul>
 *   <li>HTTP communication with proper timeouts</li>
 *   <li>Retry logic with exponential backoff</li>
 *   <li>Detailed error classification</li>
 *   <li>JSON parsing</li>
 * </ul>
 */
public class StatecraftApiImpl implements StatecraftApi {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;

    public StatecraftApiImpl() {
        // Note: Don't access config here - it's not loaded yet during mod construction
        // Timeout is set per-request instead
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(StatecraftConfig.CONNECTION_TIMEOUT_SECONDS.get()))
            .build();
    }

    private String getBaseUrl() {
        return StatecraftConfig.API_URL.get();
    }

    private String getApiKey() {
        return StatecraftConfig.API_KEY.get();
    }

    @Override
    public ApiResult<Boolean> testConnection() {
        String url = getBaseUrl() + "/players/uuid/00000000-0000-0000-0000-000000000000";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("X-API-Key", getApiKey())
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // ANY response (even errors) means the server is reachable and responding
            // This matches Persona behavior - connection test is about reachability, not health
            // A truly disconnected server would throw ConnectException, not return HTTP errors
            int status = response.statusCode();
            if (status > 0) {
                return ApiResult.success(true);
            }

            return ApiResult.serverError("No response from server");

        } catch (java.net.ConnectException e) {
            return ApiResult.networkError("Cannot connect to " + url + ": " + e.getMessage());
        } catch (IOException e) {
            return ApiResult.networkError("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApiResult.networkError("Request interrupted");
        } catch (Exception e) {
            return ApiResult.failure(ApiResult.ErrorType.UNKNOWN, "Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult<List<StatecraftCharacter>> getCharactersByPlayer(UUID playerUuid) {
        String endpoint = "/players/uuid/" + playerUuid.toString();

        return executeWithRetry("GET", endpoint, null, response -> {
            List<StatecraftCharacter> characters = new ArrayList<>();
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json.has("displayNames")) {
                JsonArray displayNames = json.getAsJsonArray("displayNames");

                for (JsonElement element : displayNames) {
                    JsonObject nameObj = element.getAsJsonObject();

                    if (nameObj.has("name")) {
                        String displayName = nameObj.get("name").getAsString();
                        boolean isDeceased = nameObj.has("isDeceased") && nameObj.get("isDeceased").getAsBoolean();

                        long characterId;
                        if (nameObj.has("characterId")) {
                            characterId = nameObj.get("characterId").getAsLong();
                        } else {
                            // Fallback: deterministic hash
                            String composite = displayName.toLowerCase() + playerUuid.toString();
                            characterId = Math.abs((long) composite.hashCode());
                            LOGGER.atWarning().log("[StatecraftApi] Character '{}' missing ID, using hash: {}", displayName, characterId);
                        }

                        StatecraftCharacter character = new StatecraftCharacter(characterId, displayName, playerUuid);
                        character.setDeceased(isDeceased);

                        if (nameObj.has("firstUsed")) {
                            character.setFirstUsed(nameObj.get("firstUsed").getAsLong());
                        }
                        if (nameObj.has("lastSeen")) {
                            character.setLastSeen(nameObj.get("lastSeen").getAsLong());
                        }

                        characters.add(character);
                    }
                }
            }

            LOGGER.atFine().log("[StatecraftApi] Found {} characters for player {}", characters.size(), playerUuid);
            return characters;
        });
    }

    @Override
    public ApiResult<StatecraftCharacter> getCharacterById(long characterId) {
        String endpoint = "/players/id/" + characterId;

        return executeWithRetry("GET", endpoint, null, response -> {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            return parseCharacter(json);
        });
    }

    @Override
    public ApiResult<List<StatecraftNation>> getNations() {
        return executeWithRetry("GET", "/nations", null, response -> {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            if (!json.has("nations"))
                return List.of();
            var nations = json.getAsJsonArray("nations");
            return nations.asList().stream().map(JsonElement::getAsJsonObject).map(this::parseNation).toList();
        });
    }

    @Override
    public ApiResult<List<StatecraftNationMember>> getNationMembers(long nationId) {
        return executeWithRetry("GET", "/nations/%s/members".formatted(nationId), null, response -> {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            if (!json.has("members"))
                return List.of();
            var members = json.getAsJsonArray("members");

            return members.asList().stream().map(JsonElement::getAsJsonObject).map(this::parseNationMember).toList();
        });
    }

    @Override
    public ApiResult<Optional<StatecraftNation>> getNationOfCharacter(long characterId) {
        var nationsResult = getNations();
        if (nationsResult.isFailure()) return ApiResult.failure(nationsResult.getErrorType().get(), nationsResult.getErrorMessage().get());
        var nations = nationsResult.getValue().get();
        for (var nation : nations) {
            var memberResult = getNationMembers(nation.id);
            if (memberResult.isFailure()) return ApiResult.notFound("Could not find members of a nation.");
            var members = memberResult.getValue().get();
            for (var member : members)
                if (member.id == characterId)
                    return ApiResult.success(Optional.of(nation));
        }
        return ApiResult.notFound("Character is not a member of a nation");
    }

    @Override
    public ApiResult<StatecraftCharacter> recordCharacter(UUID playerUuid, String displayName) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("uuid", playerUuid.toString());
        requestBody.addProperty("displayName", displayName);

        return executeWithRetry("POST", "/players/record-display-name", requestBody.toString(), response -> {
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json.has("characterId")) {
                long characterId = json.get("characterId").getAsLong();
                boolean isNew = json.has("isNew") && json.get("isNew").getAsBoolean();

                StatecraftCharacter character = new StatecraftCharacter(characterId, displayName, playerUuid);

                LOGGER.atInfo().log("[StatecraftApi] {} character '{}' with ID {} for player {}",
                    isNew ? "Created" : "Updated", displayName, characterId, playerUuid);

                return character;
            }

            throw new RuntimeException("Invalid response: missing characterId");
        });
    }

    @Override
    public ApiResult<Boolean> markDeceased(long characterId, String displayName) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("characterId", characterId);
        requestBody.addProperty("displayName", displayName);

        return executeWithRetry("POST", "/players/mark-deceased", requestBody.toString(), response -> {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            boolean success = json.has("success") && json.get("success").getAsBoolean();

            if (success) {
                LOGGER.atInfo().log("[StatecraftApi] Marked character {} ('{}') as deceased", characterId, displayName);
            }

            return success;
        });
    }

    @Override
    public ApiResult<Boolean> setPassword(UUID playerUuid, String passwordHash, String salt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("uuid", playerUuid.toString());
        requestBody.addProperty("passwordHash", passwordHash);
        requestBody.addProperty("salt", salt);

        return executeWithRetry("POST", "/players/set-password", requestBody.toString(), response -> {
            // Try to parse response and check for success field
            try {
                JsonObject json = GSON.fromJson(response, JsonObject.class);
                if (json.has("success")) {
                    boolean success = json.get("success").getAsBoolean();
                    if (success) {
                        LOGGER.atInfo().log("[StatecraftApi] Set password for player {}", playerUuid);
                        return true;
                    } else {
                        LOGGER.atWarning().log("[StatecraftApi] Failed to set password for player {}: API returned success=false", playerUuid);
                        return false;
                    }
                }
            } catch (Exception e) {
                LOGGER.atFine().log("[StatecraftApi] Could not parse success field from response: {}", response);
            }

            // If we got here, we received a 200 response but no success field
            // Assume it worked (matches Persona behavior)
            LOGGER.atInfo().log("[StatecraftApi] Set password for player {} (200 OK response)", playerUuid);
            return true;
        });
    }

    private <T> ApiResult<T> executeWithRetry(String method, String endpoint, String body, ResponseParser<T> parser) {
        String url = getBaseUrl() + endpoint;
        int maxRetries = StatecraftConfig.MAX_RETRY_ATTEMPTS.get();

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(StatecraftConfig.CONNECTION_TIMEOUT_SECONDS.get()))
                    .header("X-API-Key", getApiKey())
                    .header("Content-Type", "application/json");

                if ("POST".equals(method) && body != null) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.GET();
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                // Success
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return ApiResult.success(parser.parse(response.body()));
                }

                // Client errors - don't retry
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    return classifyClientError(response.statusCode(), response.body());
                }

                // Server error - retry with backoff
                LOGGER.atWarning().log("[StatecraftApi] Server error {} for {} (attempt {}/{})",
                    response.statusCode(), endpoint, attempt + 1, maxRetries);

                if (attempt < maxRetries - 1) {
                    int delay = 1000 * (int) Math.pow(2, attempt);
                    Thread.sleep(delay);
                }

            } catch (java.net.ConnectException e) {
                LOGGER.atFine().log("[StatecraftApi] Connection refused for {}", endpoint);
                return ApiResult.networkError("Cannot connect to Statecraft API");
            } catch (IOException e) {
                LOGGER.atWarning().log("[StatecraftApi] Network error for {} (attempt {}/{}): {}",
                    endpoint, attempt + 1, maxRetries, e.getMessage());

                if (attempt < maxRetries - 1) {
                    try {
                        int delay = 1000 * (int) Math.pow(2, attempt);
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return ApiResult.networkError("Request interrupted");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ApiResult.networkError("Request interrupted");
            } catch (Exception e) {
                LOGGER.atSevere().log("[StatecraftApi] Unexpected error for {}", endpoint, e);
                return ApiResult.failure(ApiResult.ErrorType.UNKNOWN, e.getMessage());
            }
        }

        return ApiResult.serverError("Max retries exceeded for " + endpoint);
    }

    private <T> ApiResult<T> classifyClientError(int statusCode, String body) {
        return switch (statusCode) {
            case 401, 403 -> {
                String keyHint = getApiKey().length() > 4
                    ? getApiKey().substring(0, 4) + "..."
                    : "NOT SET";
                LOGGER.atSevere().log("[StatecraftApi] Authentication failed. API key starts with: {}", keyHint);
                yield ApiResult.authError("Authentication failed. Check your API key.");
            }
            case 404 -> ApiResult.notFound("Resource not found");
            case 429 -> ApiResult.failure(ApiResult.ErrorType.RATE_LIMITED, "Rate limited. Try again later.");
            default -> ApiResult.validationError("Request failed: " + body);
        };
    }

    private StatecraftCharacter parseCharacter(JsonObject json) {
        long characterId = json.get("characterId").getAsLong();
        String displayName = json.get("displayName").getAsString();
        UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());

        StatecraftCharacter character = new StatecraftCharacter(characterId, displayName, playerUuid);

        if (json.has("isDeceased")) {
            character.setDeceased(json.get("isDeceased").getAsBoolean());
        }
        if (json.has("firstUsed")) {
            character.setFirstUsed(json.get("firstUsed").getAsLong());
        }
        if (json.has("lastSeen")) {
            character.setLastSeen(json.get("lastSeen").getAsLong());
        }
        if (json.has("profilePictureUrl") && !json.get("profilePictureUrl").isJsonNull()) {
            character.setProfilePictureUrl(json.get("profilePictureUrl").getAsString());
        }
        if (json.has("biography") && !json.get("biography").isJsonNull()) {
            character.setBiography(json.get("biography").getAsString());
        }

        return character;
    }
    private StatecraftNation parseNation(JsonObject json) {
        try {
            return new StatecraftNation(
                    json.get("abbreviation").getAsString(),
                    json.get("activeMemberCounts").getAsInt(),
                    json.get("activityPercentage").getAsInt(),
                    json.get("citizenCount").getAsInt(),
                    Instant.ofEpochSecond(json.get("creationTime").getAsLong()),
                    json.get("description").getAsString(),
                    json.get("id").getAsLong(),
                    json.get("isActive").getAsBoolean(),
                    json.get("name").getAsString(),
                    json.get("nationalCurrency").getAsString(),
                    json.get("officerCount").getAsInt(),
                    json.get("profilePictureUrl").getAsString(),
                    json.get("totalMembers").getAsInt()
            );
        } catch (Exception e) {
            return new StatecraftNation();
        }
    }
    private StatecraftNationMember parseNationMember(JsonObject json) {
        try {

            long characterId = json.get("id").getAsLong();
            boolean isDead = json.get("isDeceased").getAsBoolean();
            long joinDate = json.get("joinDate").getAsLong();
            String displayName = json.get("name").getAsString();
            String profilePic = json.get("profilePictureUrl").getAsString();
            String role = json.get("role").getAsString();
            return new StatecraftNationMember(
                    characterId, isDead, Instant.ofEpochSecond(joinDate), displayName, profilePic, role
            );
        } catch (Exception e) {
            return new StatecraftNationMember();
        }
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String response) throws Exception;
    }
}

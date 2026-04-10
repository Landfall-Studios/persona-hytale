package world.landfall.statecraft.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import world.landfall.statecraft.StatecraftMod;

import java.io.Serializable;
import java.lang.invoke.TypeDescriptor;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Configuration for Statecraft Client.
 *
 * <p>All configuration values are server-side to ensure consistency
 * across all connected clients.
 */
public class StatecraftConfig {

    enum ConfigType {
        STRING, INTEGER, BOOLEAN
    }
    private static final HashMap<String, ConfigType> CONFIG_ENTRIES = new HashMap<>();
    private static final HashMap<String, Serializable> CONFIG = new HashMap<>();
    public static BuilderCodec<StatecraftConfig> CODEC;

    // API Configuration
    public static final Supplier<String> API_URL = configValue("api_url", ConfigType.STRING, "https://api.statecraft.landfall.world/api/v1");
    public static final Supplier<String> API_KEY = configValue("api_key", ConfigType.STRING, "");
    public static final Supplier<Integer> SYNC_INTERVAL_SECONDS = configValue("sync_interval_seconds", ConfigType.INTEGER, 60);
    public static final Supplier<Integer> CONNECTION_TIMEOUT_SECONDS = configValue("connection_timeout_seconds", ConfigType.INTEGER, 60);
    public static final Supplier<Integer> MAX_RETRY_ATTEMPTS = configValue("max_retry_attempts", ConfigType.INTEGER, 5);

    // Character Configuration
    public static final Supplier<Integer> MAX_CHARACTERS_PER_PLAYER = configValue("max_characters_per_player", ConfigType.INTEGER, 5);
    public static final Supplier<String> NAME_VALIDATION_REGEX = configValue("name_validation_regex", ConfigType.STRING, "");
    public static final Supplier<Boolean> REQUIRE_CHARACTER_ON_JOIN = configValue("require_character_on_join", ConfigType.BOOLEAN, false);

    // Feature Toggles
    public static final Supplier<Boolean> ENABLE_INVENTORY_FEATURE = configValue("enable_inventory_feature", ConfigType.BOOLEAN, true);
    public static final Supplier<Boolean> ENABLE_LOCATION_FEATURE = configValue("enable_location_feature", ConfigType.BOOLEAN, true);
    public static final Supplier<Boolean> ENABLE_AGING_FEATURE = configValue("enable_aging_feature", ConfigType.BOOLEAN, true);

    // Display Name System Configuration
    public static final Supplier<Boolean> ENABLE_NAME_SYSTEM = configValue("enable_name_system", ConfigType.BOOLEAN, true);
//    public static final Supplier<Boolean> SHOW_USERNAME_IN_TABLIST = configValue("show_username_in_tablist"); TODO recreate this system for /list
//    public static final Supplier<String> TABLIST_NAME_COLOR = configValue("");

    // Aging System Configuration
    public static final Supplier<Integer> CHARACTER_MAX_AGE_YEARS = configValue("character_max_age_years", ConfigType.INTEGER, 100);
    public static final Supplier<Integer> DEATH_PENALTY_YEARS = configValue("death_penalty_years", ConfigType.INTEGER, 1);
    public static final Supplier<Integer> REAL_DAYS_PER_RP_YEAR = configValue("real_days_per_rp_year", ConfigType.INTEGER, 30);
    public static final Supplier<Integer> MIN_STARTING_AGE = configValue("min_starting_age", ConfigType.INTEGER, 16);
    public static final Supplier<Integer> MAX_STARTING_AGE = configValue("max_starting_age", ConfigType.INTEGER, 90);
    public static final Supplier<Integer> DEFAULT_STARTING_AGE = configValue("default_starting_age", ConfigType.INTEGER, 21);

    // Queue Configuration
    public static final Supplier<Integer> QUEUE_MAX_SIZE = configValue("queue_max_size", ConfigType.INTEGER, 5);
    public static final Supplier<Integer> QUEUE_OPERATION_TTL_HOURS = configValue("queue_operation_ttl_hours", ConfigType.INTEGER, 1);
    public static final Supplier<Integer> QUEUE_RETRY_DELAY_SECONDS = configValue("queue_retry_delay_seconds", ConfigType.INTEGER, 5);

    // Safety Configuration
    public static final Supplier<Boolean> ALLOW_OFFLINE_CHARACTER_SWITCH = configValue("allow_offline_character_switch", ConfigType.BOOLEAN, false);
    public static final Supplier<Boolean> ALLOW_OFFLINE_CHARACTER_CREATE = configValue("allow_offline_character_create", ConfigType.BOOLEAN, false);
    public static final Supplier<Integer> HEALTH_CHECK_INTERVAL_SECONDS = configValue("health_check_interval_seconds", ConfigType.INTEGER, 20);

    public static void setupConfig() {
        var codecBuilder = BuilderCodec.builder(StatecraftConfig.class, StatecraftConfig::new);
        for (var x : CONFIG_ENTRIES.entrySet()) {
            var oldKey = x.getKey();
            var val = x.getValue();

            var key = sanitizeFieldName(oldKey);
            switch (val) {
                case BOOLEAN ->
                        codecBuilder.append(new KeyedCodec<Boolean>(key, BuilderCodec.BOOLEAN), (_, value) -> CONFIG.put(key, value), _ -> (Boolean)CONFIG.get(key)).add();
                case STRING ->
                        codecBuilder.append(new KeyedCodec<String>(key, BuilderCodec.STRING), (_, value) -> CONFIG.put(key, value), _ -> (String)CONFIG.get(key)).add();
                case INTEGER ->
                        codecBuilder.append(new KeyedCodec<Integer>(key, BuilderCodec.INTEGER), (_, value) -> CONFIG.put(key, value), _ -> (Integer)CONFIG.get(key)).add();
            }
        }
        CODEC = codecBuilder.build();
    }

    private static <T extends Serializable> Supplier<T> configValue(String name, ConfigType type) {
        CONFIG_ENTRIES.put(name, type);
        return () -> {
            var result = CONFIG.get(sanitizeFieldName(name));
            try {
                return (T) result;
            } catch (Exception e) {
                return null;
            }
        };
    }
    private static <T extends Serializable> Supplier<T> configValue(String name, ConfigType type, T defaultValue) {
        CONFIG_ENTRIES.put(name, type);
        CONFIG.put(sanitizeFieldName(name), defaultValue);
        return () -> {
            var result = CONFIG.get(sanitizeFieldName(name));
            try {
                return (T) result;
            } catch (Exception e) {
                return null;
            }
        };
    }
    private static String sanitizeFieldName(String field) {
        var builder = new StringBuilder();
        builder.append("Statecraft");
        for (int i = 0; i < field.length(); i++) {
            if (i == 0) {
                builder.append(field.toUpperCase().charAt(i));
                continue;
            }
            char c = field.charAt(i);
            if (c == '_') {
                i++;
                if (i == field.length()) break;
                builder.append(field.toUpperCase().charAt(i));
            } else builder.append(field.charAt(i));
        }
        return builder.toString();
    }
}

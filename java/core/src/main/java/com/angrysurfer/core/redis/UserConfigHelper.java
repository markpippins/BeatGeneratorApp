package com.angrysurfer.core.redis;

import com.angrysurfer.core.Constants;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.util.ErrorHandler;
import com.angrysurfer.core.util.PlayerDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserConfigHelper {
    private static final Logger logger = LoggerFactory.getLogger(UserConfigHelper.class.getName());
    private static final String CONFIG_KEY_PREFIX = "userconfig:";
    private static final String CONFIG_IDS_KEY = "userconfig:ids";
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public UserConfigHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        configureObjectMapper();  // Add this line
    }

    private void configureObjectMapper() {
        // Register the Player deserializer
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Player.class, new PlayerDeserializer());
        this.objectMapper.registerModule(module);

        // Enable more tolerant deserialization
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    /**
     * Load user configuration from Redis by ID
     *
     * @param id The configuration ID
     * @return The loaded configuration, or null if not found
     */
    public UserConfig loadConfigFromRedis(Integer id) {
        if (id == null) {
            logger.warn("Attempted to load UserConfig with null ID");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(CONFIG_KEY_PREFIX + id);
            if (json != null) {
                UserConfig config = objectMapper.readValue(json, UserConfig.class);
                logger.debug("Loaded UserConfig with ID {}", id);
                logger.info(json);
                config.getInstruments().forEach(instrument -> {
                    logger.info("Loaded {} [device: {}, isDefault: {}]", instrument.getName(), instrument.getDeviceName(),
                            instrument.getIsDefault());
                });

                if (Constants.RESET_USER_CONFIG) {
                    config.setDefaultStrikes(new ArrayList<>());
                    config.setDefaultNotes(new ArrayList<>());
                    config.setInstruments(new ArrayList<>());
                    config.setHasDefaults(false);
                }

                return config;
            } else {
                logger.debug("No UserConfig found with ID {}", id);
                return null;
            }
        } catch (Exception e) {
            ErrorHandler.logError("UserConfigHelper", "Error loading config with ID " + id + ": " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Legacy method for backward compatibility
     * Loads the first available user configuration or creates a new one if none exists
     */
    public UserConfig loadConfigFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Try to get the first available config ID
            Integer firstId = findFirstConfigId();

            if (firstId != null) {
                return loadConfigFromRedis(firstId);
            } else {
                logger.info("No existing configuration found, creating new one");
                UserConfig newConfig = new UserConfig();
                newConfig.setId(1);
                newConfig.setName("Default Configuration");
                newConfig.setLastUpdated(new Date());
                saveConfig(newConfig);
                return newConfig;
            }
        } catch (Exception e) {
            ErrorHandler.logError("UserConfigHelper", "Error loading default config: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find all UserConfig IDs stored in Redis
     *
     * @return List of configuration IDs
     */
    public List<Integer> findAllConfigIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> members = jedis.smembers(CONFIG_IDS_KEY);
            if (members == null || members.isEmpty()) {
                return new ArrayList<>();
            }

            return members.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            ErrorHandler.logError("UserConfigHelper", "Error finding config IDs: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Find the first available UserConfig ID
     *
     * @return First ID or null if none exist
     */
    public Integer findFirstConfigId() {
        List<Integer> ids = findAllConfigIds();
        if (ids.isEmpty()) {
            return null;
        }

        // Sort IDs and return the first (lowest) one
        ids.sort(Integer::compareTo);
        return ids.get(0);
    }

    /**
     * Save user configuration to Redis with improved error handling
     */
    public void saveConfig(UserConfig config) {
        if (config == null) {
            logger.error("Cannot save null configuration");
            throw new IllegalArgumentException("Cannot save null configuration");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Ensure the configuration has an ID
            if (config.getId() == null) {
                config.setId(generateNewConfigId());
            }

            // Update last modified timestamp
            config.setLastUpdated(new Date());

            // Verify Redis connection is active
            if (!jedis.isConnected()) {
                logger.error("Redis connection is not active!");
                jedis.connect();
                if (!jedis.isConnected()) {
                    throw new RuntimeException("Unable to connect to Redis");
                }
            }

            // Serialize the configuration with pretty printing for debugging
            ObjectMapper debugMapper = objectMapper.copy();
            debugMapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Log a small sample of the JSON for debugging
            try {
                String debugJson = debugMapper.writeValueAsString(config);
                int maxLen = Math.min(500, debugJson.length());
                logger.debug("Saving UserConfig JSON (excerpt): {}", debugJson.substring(0, maxLen));
            } catch (Exception e) {
                logger.warn("Could not generate debug JSON", e);
            }

            // Use the regular mapper for actual storage
            String json = objectMapper.writeValueAsString(config);

            // Save the configuration under its ID key
            String result = jedis.set(CONFIG_KEY_PREFIX + config.getId(), json);
            if (!"OK".equals(result)) {
                logger.error("Redis returned non-OK response when saving config: {}", result);
                throw new RuntimeException("Redis returned: " + result);
            }

            // Add the ID to the set of all config IDs
            jedis.sadd(CONFIG_IDS_KEY, config.getId().toString());

            logger.info("Saved UserConfig with ID {} to Redis (size: {} bytes)",
                    config.getId(), json.length());
        } catch (Exception e) {
            logger.error("Error saving config to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save UserConfig", e);
        }
    }

    /**
     * Delete a user configuration from Redis
     *
     * @param id The ID of the configuration to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteConfig(Integer id) {
        if (id == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Remove the configuration
            long removed = jedis.del(CONFIG_KEY_PREFIX + id);

            // Remove the ID from the set of all config IDs
            jedis.srem(CONFIG_IDS_KEY, id.toString());

            if (removed > 0) {
                logger.info("Deleted UserConfig with ID {}", id);
                return true;
            } else {
                logger.warn("No UserConfig found with ID {} to delete", id);
                return false;
            }
        } catch (Exception e) {
            ErrorHandler.logError("UserConfigHelper", "Error deleting config: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate a new unique configuration ID
     *
     * @return A new unique ID
     */
    private Integer generateNewConfigId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> existingIds = jedis.smembers(CONFIG_IDS_KEY);

            if (existingIds == null || existingIds.isEmpty()) {
                return 1; // Start with ID 1 if no configs exist
            }

            // Find the maximum existing ID and increment
            int maxId = existingIds.stream()
                    .mapToInt(Integer::parseInt)
                    .max()
                    .orElse(0);

            return maxId + 1;
        } catch (Exception e) {
            ErrorHandler.logError("UserConfigHelper", "Error generating config ID: " + e.getMessage(), e);
            // Fallback to timestamp-based ID
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }
    }

    /**
     * Load user configuration from a JSON file
     *
     * @param filePath The path to the JSON file
     * @return The loaded configuration, or null if error
     */
    public UserConfig loadConfigFromJSON(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(filePath);

            if (!file.exists()) {
                ErrorHandler.logError("UserConfigHelper", "Configuration file not found: " + filePath);
                return null;
            }

            UserConfig config = mapper.readValue(file, UserConfig.class);

            for (Player player : config.getDefaultStrikes())
                player.setIsDefault(true);

            for (Player player : config.getDefaultNotes())
                player.setIsDefault(true);

            for (InstrumentWrapper wrapper : config.getInstruments())
                wrapper.setIsDefault(true);

            // Perform validation or migration if needed
            config = validateAndMigrate(config);

            logger.info("Configuration successfully loaded from: {}", filePath);
            return config;
        } catch (IOException e) {
            ErrorHandler.logError("UserConfigHelper", "Error loading configuration from file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate and migrate configuration if needed
     */
    private UserConfig validateAndMigrate(UserConfig config) {
        // Initialize any null fields
        if (config.getInstruments() == null) {
            config.setInstruments(new ArrayList<>());
        }

        if (config.getDefaultStrikes() == null) {
            config.setDefaultStrikes(new ArrayList<>());
        }

        if (config.getDefaultNotes() == null) {
            config.setDefaultNotes(new ArrayList<>());
        }

        // Ensure the configuration has an ID
        if (config.getId() == null) {
            config.setId(generateNewConfigId());
        }

        // Set timestamps if missing
        if (config.getLastUpdated() == null) {
            config.setLastUpdated(new Date());
        }

        // Set a name if missing
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            config.setName("Configuration " + config.getId());
        }

        return config;
    }

    /**
     * Save the user configuration to a JSON file
     *
     * @param config   The configuration to save
     * @param filePath The path where to save the file
     * @return True if successful, false otherwise
     */
    public boolean saveConfigToJSON(UserConfig config, String filePath) {
        if (config == null) {
            ErrorHandler.logError("UserConfigHelper", "Cannot save null configuration to file");
            return false;
        }

        try {
            // Ensure the configuration has an ID
            if (config.getId() == null) {
                config.setId(generateNewConfigId());
            }

            // Update timestamp
            config.setLastUpdated(new Date());

            ObjectMapper mapper = new ObjectMapper();
            // Enable pretty printing for readable JSON
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Create the file
            File file = new File(filePath);

            // Ensure parent directories exist
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // Write the config to file
            mapper.writeValue(file, config);
            logger.info("Configuration successfully saved to: {}", filePath);
            return true;
        } catch (IOException e) {
            ErrorHandler.logError("UserConfigHelper", "Error saving configuration to file: " + e.getMessage(), e);
            return false;
        }
    }
}
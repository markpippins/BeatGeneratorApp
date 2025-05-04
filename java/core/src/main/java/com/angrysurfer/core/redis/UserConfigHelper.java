package com.angrysurfer.core.redis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.swing.*;

@Getter
public class UserConfigHelper {
    private static final Logger logger = LoggerFactory.getLogger(UserConfigHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public UserConfigHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public UserConfig loadConfigFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("userconfig");
            if (json != null) {
                UserConfig config = loadAndMigrateFromJson(json);
                return config;
            }
        } catch (Exception e) {
            logger.error("Error loading config from Redis: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error loading config from Redis: " + e.getMessage());
        }
        return null;
    }

    /**
     * Load user configuration from a JSON file
     *
     * @param filePath The path to the JSON file
     * @return The loaded configuration, or null if error
     */
    public UserConfig loadConfigFromJSON(String filePath) {
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                logger.warn("Configuration file not found: {}", filePath);
                JOptionPane.showMessageDialog(null, "Configuration file not found: " + filePath);
                return null;
            }
            
            String json = objectMapper.readTree(file).toString();
            UserConfig config = loadAndMigrateFromJson(json);
            
            logger.info("Configuration successfully loaded from: {}", filePath);
            return config;
        } catch (IOException e) {
            logger.error("Error loading configuration from file: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error loading configuration from file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Load and migrate configuration from JSON string
     */
    private UserConfig loadAndMigrateFromJson(String json) throws IOException {
        // First, parse as JsonNode to check version and presence of configs
        JsonNode rootNode = objectMapper.readTree(json);
        int version = rootNode.has("configVersion") ? rootNode.get("configVersion").asInt() : 1;
        
        // If version < 2, we need to migrate from old format to new format
        if (version < 2 && rootNode.has("configs")) {
            logger.info("Migrating UserConfig from version {} to version 2", version);
            return migrateFromVersion1(rootNode);
        } else {
            // No migration needed, parse normally
            return objectMapper.readValue(json, UserConfig.class);
        }
    }
    
    /**
     * Migrate from version 1 (with InstrumentConfig) to version 2 (without InstrumentConfig)
     */
    private UserConfig migrateFromVersion1(JsonNode rootNode) throws IOException {
        // Create a modifiable copy of the root node
        ObjectNode mutableRoot = rootNode.deepCopy();
        
        // Get the configs array if it exists
        if (mutableRoot.has("configs") && mutableRoot.get("configs").isArray()) {
            ArrayNode configsArray = (ArrayNode) mutableRoot.get("configs");
            
            // Create instruments array if it doesn't exist
            if (!mutableRoot.has("instruments") || mutableRoot.get("instruments").isNull()) {
                mutableRoot.putArray("instruments");
            }
            
            // Get the existing instruments array
            ArrayNode instrumentsArray = (ArrayNode) mutableRoot.get("instruments");
            
            // Convert each config to an instrumentWrapper and add to instruments array
            for (JsonNode configNode : configsArray) {
                ObjectNode instrumentNode = objectMapper.createObjectNode();
                
                // Generate a unique ID
                instrumentNode.put("id", System.currentTimeMillis() + instrumentsArray.size());
                
                // Set name and deviceName from the device field
                String device = configNode.has("device") ? configNode.get("device").asText("") : "Unknown Device";
                instrumentNode.put("name", device);
                instrumentNode.put("deviceName", device);
                
                // Set other properties
                instrumentNode.put("channel", 9); // Default to GM drum channel (10, but zero-indexed as 9)
                instrumentNode.put("available", configNode.has("available") ? configNode.get("available").asBoolean(false) : false);
                
                // Port
                if (configNode.has("port")) {
                    instrumentNode.put("port", configNode.get("port").asText(""));
                }
                
                // Add the new instrument to the array
                instrumentsArray.add(instrumentNode);
            }
            
            // Remove the configs field
            mutableRoot.remove("configs");
            
            // Set the new version
            mutableRoot.put("configVersion", 2);
            
            // Set last updated to now
            mutableRoot.put("lastUpdated", new Date().getTime());
        }
        
        // Convert back to UserConfig
        return objectMapper.treeToValue(mutableRoot, UserConfig.class);
    }

    /**
     * Validate and migrate configuration if needed
     */
    private UserConfig validateAndMigrate(UserConfig config) {
        // Initialize any null fields
        if (config.getInstruments() == null) {
            config.setInstruments(new ArrayList<>());
        }
        
        // Set timestamps if missing
        if (config.getLastUpdated() == null) {
            config.setLastUpdated(new Date());
        }
        
        // Make sure config version is set to latest
        if (config.getConfigVersion() < 2) {
            config.setConfigVersion(2);
        }
        
        return config;
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Ensure config is validated before saving
            config = validateAndMigrate(config);
            
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
            logger.info("Saved UserConfig to Redis");
        } catch (Exception e) {
            logger.error("Error saving config to Redis: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error saving config to Redis: " + e.getMessage());
            throw new RuntimeException("Failed to save config", e);
        }
    }

    /**
     * Save the user configuration to a JSON file
     *
     * @param config The configuration to save
     * @param filePath The path where to save the file
     * @return True if successful, false otherwise
     */
    public boolean saveConfigToJSON(UserConfig config, String filePath) {
        if (config == null) {
            logger.warn("Cannot save null configuration to file");
            JOptionPane.showMessageDialog(null, "Cannot save null configuration to file");
            return false;
        }
        
        try {
            // Ensure config is validated before saving
            config = validateAndMigrate(config);
            
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
            logger.error("Error saving configuration to file: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error saving configuration to file: " + e.getMessage());
            return false;
        }
    }
}
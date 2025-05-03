package com.angrysurfer.core.redis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.config.UserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
                return objectMapper.readValue(json, UserConfig.class);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,  "Error loading config from Redis: " + e.getMessage());
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
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(filePath);
            
            if (!file.exists()) {
                JOptionPane.showMessageDialog(null,  "Configuration file not found: " + filePath);
                return null;
            }
            
            UserConfig config = mapper.readValue(file, UserConfig.class);
            
            // Perform validation or migration if needed
            config = validateAndMigrate(config);
            
            logger.info("Configuration successfully loaded from: {}", filePath);
            return config;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,  "Error loading configuration from file: "  + e.getMessage());
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
        
        if (config.getConfigs() == null) {
            config.setConfigs(new HashSet<>());
        }
        
        // Set timestamps if missing
        if (config.getLastUpdated() == null) {
            config.setLastUpdated(new Date());
        }
        
        return config;
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
            logger.info("Saved UserConfig to Redis");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,  "Error saving config to Redis: " + e.getMessage());
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
            JOptionPane.showMessageDialog(null,  "Cannot save null configuration to file");
            return false;
        }
        
        try {
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
            logger.info("Configuration successfully saved to:" + filePath);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,  "Error saving configuration to file: " + e.getMessage());
            return false;
        }
    }
}
package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Make UserConfigManager the single source of truth
public class UserConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(UserConfigManager.class.getName());
    private static UserConfigManager instance;
    private final UserConfigHelper configHelper;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean initialized = false;
    private boolean isInitializing = false;
    private UserConfig currentConfig = new UserConfig();

    // Make constructor private for singleton pattern
    private UserConfigManager() {
        isInitializing = true;
        RedisService redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
        try {
            loadConfiguration();
        } finally {
            isInitializing = false;
        }
        
        // Now that initialization is complete, we can safely notify listeners
        commandBus.publish(Commands.USER_CONFIG_LOADED, this, null);
        initializeInstruments();
    }

    // Static method to get singleton instance
    public static synchronized UserConfigManager getInstance() {
        if (instance == null) {
            instance = new UserConfigManager();
        }
        return instance;
    }

    public void loadConfiguration() {
        logger.info("Loading user configuration from Redis");
        try {
            this.currentConfig = configHelper.loadConfigFromRedis();
            initialized = this.currentConfig != null;
            if (initialized) {
                logger.info("User configuration loaded successfully");
                if (!isInitializing) {
                    commandBus.publish(Commands.USER_CONFIG_LOADED, this, this.currentConfig);
                }
            } else {
                this.currentConfig = new UserConfig();
                logger.error("No user configuration found in Redis");
            }
        } catch (Exception e) {
            logger.error("Error loading user configuration: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    public void loadConfigurationFromFile(String configPath) {
        logger.info("Loading configuration from file: " + configPath);
        try {
            UserConfig loadedConfig = configHelper.loadConfigFromJSON(configPath);
            if (loadedConfig != null) {
                currentConfig = loadedConfig;
                configHelper.saveConfig(currentConfig);
                commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
                logger.info("Configuration loaded and saved successfully");
            }
        } catch (Exception e) {
            logger.error("Error loading configuration from file: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    public void saveConfiguration(UserConfig config) {
        logger.info("Saving user configuration");
        try {
            configHelper.saveConfig(config);
            currentConfig = config;
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
        } catch (Exception e) {
            logger.error("Error saving user configuration: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    public List<InstrumentWrapper> getInstruments() {
        return currentConfig.getInstruments();
    }

    private void initializeInstruments() {
        if (this.currentConfig.getInstruments() == null || getCurrentConfig().getInstruments().isEmpty()) {
            // Load instruments from Redis if config doesn't have them
            try {
                List<InstrumentWrapper> instruments = RedisService.getInstance().findAllInstruments();
                if (instruments != null && !instruments.isEmpty()) {
                    getCurrentConfig().setInstruments(instruments);
                    saveConfiguration(getCurrentConfig());
                    logger.info("Loaded " + instruments.size() + " instruments from Redis");
                }
            } catch (Exception e) {
                logger.error("Error loading instruments: {}", e.getMessage());
                // Clean up resources
                // Notify UI of failure
            }
        }

        // Make sure Gervill is registered
        try {
            boolean hasGervill = false;
            
            // Check if Gervill is already in the config
            if (currentConfig.getInstruments() != null) {
                for (InstrumentWrapper instrument : currentConfig.getInstruments()) {
                    if (instrument.getDeviceName() != null && 
                        instrument.getDeviceName().contains("Gervill")) {
                        hasGervill = true;
                        break;
                    }
                }
            }
            
            // If Gervill is not in config, add it
            if (!hasGervill) {
                logger.info("Adding Gervill instrument to configuration");
                
                // Get Gervill from InternalSynthManager
                Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();
                if (synth != null) {
                    InstrumentWrapper gervillInstrument = new InstrumentWrapper(
                        "Gervill", 
                        synth,
                        InstrumentWrapper.ALL_CHANNELS // Make it available on all channels
                    );
                    gervillInstrument.setId(System.currentTimeMillis());
                    
                    // Add to config
                    currentConfig.getInstruments().add(gervillInstrument);
                    saveConfiguration(currentConfig);
                    
                    logger.info("Gervill instrument added to configuration");
                }
            }
        } catch (Exception e) {
            logger.error("Error registering Gervill instrument: {}", e.getMessage());
        }
    }

    // All instrument updates go through here
    public void updateInstrument(InstrumentWrapper instrument) {
        List<InstrumentWrapper> instruments = currentConfig.getInstruments();

        try {
            // Find and replace the existing instrument or add if not found
            boolean updated = false;
            if (instruments != null) {
                for (int i = 0; i < instruments.size(); i++) {
                    if (instruments.get(i).getId().equals(instrument.getId())) {
                        instruments.set(i, instrument);
                        updated = true;
                        break;
                    }
                }

                if (!updated) {
                    instruments.add(instrument);
                }

                // Save the updated config
                saveConfiguration(currentConfig);
                logger.info("Instrument updated: " + instrument.getName());
            }

            // Notify InstrumentManager via event
            commandBus.publish(Commands.INSTRUMENT_UPDATED, this, instrument);
        } catch (Exception e) {
            logger.error("Error updating instrument: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    // Add a method to remove an instrument from the config
    public void removeInstrument(Long instrumentId) {
        List<InstrumentWrapper> instruments = currentConfig.getInstruments();

        try {
            if (instruments != null) {
                boolean removed = instruments.removeIf(i -> i.getId().equals(instrumentId));

                if (removed) {
                    // Save the updated config
                    saveConfiguration(currentConfig);
                    logger.info("Instrument removed: ID " + instrumentId);
                }
            }
        } catch (Exception e) {
            logger.error("Error removing instrument: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    /**
     * Backup current configuration to a file
     * 
     * @param backupPath Path to save the backup
     * @return True if successful, false otherwise
     */
    public boolean backupConfiguration(String backupPath) {
        logger.info("Backing up configuration to: {}", backupPath);
        try {
            return configHelper.saveConfigToJSON(currentConfig, backupPath);
        } catch (Exception e) {
            logger.error("Error backing up configuration: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Restore configuration from a backup file
     * 
     * @param backupPath Path to the backup file
     * @return True if successful, false otherwise
     */
    public boolean restoreConfiguration(String backupPath) {
        logger.info("Restoring configuration from: {}", backupPath);
        try {
            UserConfig restoredConfig = configHelper.loadConfigFromJSON(backupPath);
            if (restoredConfig != null) {
                // Validate and migrate the restored config
                restoredConfig = migrateConfigIfNeeded(restoredConfig);
                if (validateConfig(restoredConfig)) {
                    // Save to persistent storage
                    configHelper.saveConfig(restoredConfig);
                    currentConfig = restoredConfig;
                    commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
                    return true;
                } else {
                    logger.warn("Restored configuration failed validation");
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error restoring configuration: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates the user configuration for completeness and consistency
     * 
     * @param config The configuration to validate
     * @return True if valid, false otherwise
     */
    private boolean validateConfig(UserConfig config) {
        if (config == null) {
            logger.error("Configuration is null");
            return false;
        }

        // Validate instruments
        if (config.getInstruments() == null) {
            logger.warn("Instruments list is null, initializing empty list");
            config.setInstruments(new ArrayList<>());
        }
        
        // Validate instruments have required fields
        boolean allValid = true;
        for (InstrumentWrapper instrument : config.getInstruments()) {
            if (instrument.getId() == null) {
                logger.warn("Instrument missing ID: {}", instrument.getName());
                instrument.setId(generateUniqueId());
                allValid = false;
            }
            
            if (instrument.getName() == null || instrument.getName().trim().isEmpty()) {
                logger.warn("Instrument has invalid name, ID: {}", instrument.getId());
                allValid = false;
            }
        }
        
        // Validate device configs
        if (config.getConfigs() == null) {
            logger.warn("Device configs set is null, initializing empty set");
            config.setConfigs(new HashSet<>());
        }
        
        return allValid;
    }

    /**
     * Check if configuration requires migration and perform if needed
     *
     * @param config The configuration to check
     * @return The migrated configuration
     */
    private UserConfig migrateConfigIfNeeded(UserConfig config) {
        if (config == null) return new UserConfig();

        // Check version and migrate as needed
        if (config.getConfigVersion() < 1) {
            // logger.info("Migrating configuration from legacy format");
            // Apply migration steps
            config.setConfigVersion(1);
        }

        // Add future version migrations here

        config.setLastUpdated(new Date());
        return config;
    }

    /**
     * Generate a unique ID for an instrument
     */
    private Long generateUniqueId() {
        return System.currentTimeMillis();
    }

    /**
     * Make multiple changes to the configuration in a single transaction
     * 
     * @param configUpdater A function that modifies the configuration
     * @return True if successful, false otherwise
     */
    public synchronized boolean updateConfigInTransaction(Function<UserConfig, Boolean> configUpdater) {
        // Create a temporary copy to work with
        UserConfig tempConfig = cloneConfig(currentConfig);
        
        try {
            // Apply the updates to the temporary copy
            boolean successful = configUpdater.apply(tempConfig);
            
            if (!successful) {
                logger.warn("Configuration update transaction failed (callback returned false)");
                return false;
            }
            
            // Validate the updated configuration
            if (!validateConfig(tempConfig)) {
                logger.warn("Configuration update failed validation");
                return false;
            }
            
            // Update last modified timestamp
            tempConfig.setLastUpdated(new Date());
            
            // Persist the updated configuration
            configHelper.saveConfig(tempConfig);
            
            // Update the in-memory configuration
            currentConfig = tempConfig;
            
            // Notify listeners
            commandBus.publish(Commands.USER_CONFIG_UPDATED, this, currentConfig);
            
            return true;
        } catch (Exception e) {
            logger.error("Error during configuration transaction: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a deep clone of the configuration
     */
    private UserConfig cloneConfig(UserConfig source) {
        try {
            // Use Jackson for deep cloning
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(mapper.writeValueAsString(source), UserConfig.class);
        } catch (Exception e) {
            logger.error("Error cloning configuration: {}", e.getMessage(), e);
            // Fallback to creating a new object
            return new UserConfig();
        }
    }

    /**
     * Add multiple instruments in a single transaction
     * 
     * @param instruments The instruments to add
     * @return True if successful
     */
    public boolean addInstruments(List<InstrumentWrapper> instruments) {
        return updateConfigInTransaction(config -> {
            try {
                // Add all instruments that don't already exist
                for (InstrumentWrapper instrument : instruments) {
                    // Check for duplicate by ID
                    boolean exists = config.getInstruments().stream()
                        .anyMatch(i -> i.getId().equals(instrument.getId()));
                    
                    if (!exists) {
                        config.getInstruments().add(instrument);
                        logger.info("Added instrument: {}", instrument.getName());
                    } else {
                        logger.warn("Skipping duplicate instrument: {}", instrument.getName());
                    }
                }
                return true;
            } catch (Exception e) {
                logger.error("Error adding instruments: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Find an instrument by ID with proper error handling
     * 
     * @param id The instrument ID
     * @return The instrument or null if not found
     */
    public InstrumentWrapper findInstrumentById(Long id) {
        if (id == null || currentConfig == null || currentConfig.getInstruments() == null) {
            return null;
        }
        
        return currentConfig.getInstruments().stream()
            .filter(i -> id.equals(i.getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find instruments by name (partial match)
     * 
     * @param nameFragment Name fragment to search for
     * @return List of matching instruments
     */
    public List<InstrumentWrapper> findInstrumentsByName(String nameFragment) {
        if (nameFragment == null || currentConfig == null || currentConfig.getInstruments() == null) {
            return new ArrayList<>();
        }
        
        return currentConfig.getInstruments().stream()
            .filter(i -> i.getName() != null && i.getName().toLowerCase().contains(nameFragment.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Handle Redis connection status changes
     */
    public void onRedisConnectionChanged(boolean connected) {
        logger.info("Redis connection status changed: {}", connected ? "Connected" : "Disconnected");
        
        if (connected) {
            // Try to load config on reconnect
            try {
                UserConfig redisConfig = configHelper.loadConfigFromRedis();
                if (redisConfig != null) {
                    // Handle merged changes if needed
                    handleReconnectionConfigUpdate(redisConfig);
                }
            } catch (Exception e) {
                logger.error("Error reloading config after reconnection: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Handle configuration merging when Redis reconnects
     * 
     * @param redisConfig The config loaded from Redis
     */
    private void handleReconnectionConfigUpdate(UserConfig redisConfig) {
        // Check if Redis config is newer
        if (redisConfig.getLastUpdated() != null && 
            currentConfig.getLastUpdated() != null &&
            redisConfig.getLastUpdated().after(currentConfig.getLastUpdated())) {
            
            logger.info("Redis has a newer configuration, updating local copy");
            currentConfig = redisConfig;
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
        } else if (currentConfig.getLastUpdated() != null &&
                   (redisConfig.getLastUpdated() == null || 
                    currentConfig.getLastUpdated().after(redisConfig.getLastUpdated()))) {
            
            logger.info("Local config is newer than Redis, updating Redis");
            try {
                configHelper.saveConfig(currentConfig);
            } catch (Exception e) {
                logger.error("Error saving newer config to Redis: {}", e.getMessage(), e);
            }
        }
    }
}
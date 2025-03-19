package com.angrysurfer.core.service;

import java.util.List;
import java.util.logging.Logger;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserConfigManager {

    private static final Logger logger = Logger.getLogger(UserConfigManager.class.getName());
    private static UserConfigManager instance;
    private final UserConfigHelper configHelper;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean initialized = false;
    private UserConfig currentConfig = new UserConfig();

    // Make constructor private for singleton pattern
    private UserConfigManager() {
        RedisService redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
        loadConfiguration();
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
        this.currentConfig = configHelper.loadConfigFromRedis();
        initialized = this.currentConfig != null;
        if (initialized) {
            logger.info("User configuration loaded successfully");
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, this.currentConfig);
        } else {
            this.currentConfig = new UserConfig();
            logger.warning("No user configuration found in Redis");
        }
    }

    public void loadConfigurationFromFile(String configPath) {
        logger.info("Loading configuration from file: " + configPath);
        UserConfig loadedConfig = configHelper.loadConfigFromJSON(configPath);
        if (loadedConfig != null) {
            currentConfig = loadedConfig;
            configHelper.saveConfig(currentConfig);
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
            logger.info("Configuration loaded and saved successfully");
        }
    }

    public void saveConfiguration(UserConfig config) {
        logger.info("Saving user configuration");
        configHelper.saveConfig(config);
        currentConfig = config;
        commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
    }

    public List<Instrument> getInstruments() {
        return currentConfig.getInstruments();
    }

    private void initializeInstruments() {
        if (this.currentConfig.getInstruments() == null || getCurrentConfig().getInstruments().isEmpty()) {
            // Load instruments from Redis if config doesn't have them
            try {
                List<Instrument> instruments = RedisService.getInstance().findAllInstruments();
                if (instruments != null && !instruments.isEmpty()) {
                    getCurrentConfig().setInstruments(instruments);
                    saveConfiguration(getCurrentConfig());
                    logger.info("Loaded " + instruments.size() + " instruments from Redis");
                }
            } catch (Exception e) {
                logger.warning("Error loading instruments: " + e.getMessage());
            }
        }
    }

    // Add a method to update an instrument in the config
    public void updateInstrument(Instrument instrument) {
        List<Instrument> instruments = currentConfig.getInstruments();

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
    }

    // Add a method to remove an instrument from the config
    public void removeInstrument(Long instrumentId) {
        List<Instrument> instruments = currentConfig.getInstruments();

        if (instruments != null) {
            boolean removed = instruments.removeIf(i -> i.getId().equals(instrumentId));

            if (removed) {
                // Save the updated config
                saveConfiguration(currentConfig);
                logger.info("Instrument removed: ID " + instrumentId);
            }
        }
    }
}
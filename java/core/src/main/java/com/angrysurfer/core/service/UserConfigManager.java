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

@Setter
@Getter
public class UserConfigManager {
    
    private static final Logger logger = Logger.getLogger(UserConfigManager.class.getName());
    // private static UserConfigurationEngine instance;
    private final UserConfigHelper configHelper;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean initialized = false;
    private UserConfig currentConfig = new UserConfig();

    public UserConfigManager() {
        RedisService redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
        loadConfiguration();
    }

    public void loadConfiguration() {
        logger.info("Loading user configuration from Redis");
        currentConfig = configHelper.loadConfigFromRedis();
        initialized = true;
        if (currentConfig != null) {
            logger.info("User configuration loaded successfully");
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
        } else {
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
}
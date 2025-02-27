package com.angrysurfer.core.service;

import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.RedisUserConfigurationHelper;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserConfigurationManager {
    private static final Logger logger = Logger.getLogger(UserConfigurationManager.class.getName());
    private static UserConfigurationManager instance;
    private final RedisUserConfigurationHelper configHelper;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean initialized = false;
    private UserConfig currentConfig = new UserConfig();

    private UserConfigurationManager() {
        RedisService redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
        loadConfiguration();
    }

    public static UserConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (UserConfigurationManager.class) {
                if (instance == null) {
                    instance = new UserConfigurationManager();
                }
            }
        }
        return instance;
    }

    private void loadConfiguration() {
        logger.info("Loading user configuration from Redis");
        currentConfig = configHelper.loadConfigFromRedis();
        initialized = true;
        if (currentConfig != null) {
            logger.info("User configuration loaded successfully");
            commandBus.publish(new Command(Commands.USER_CONFIG_LOADED, this, currentConfig));
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
            commandBus.publish(new Command(Commands.USER_CONFIG_LOADED, this, currentConfig));
            logger.info("Configuration loaded and saved successfully");
        }
    }

    public void saveConfiguration(UserConfig config) {
        logger.info("Saving user configuration");
        configHelper.saveConfig(config);
        currentConfig = config;
        commandBus.publish(new Command(Commands.USER_CONFIG_LOADED, this, currentConfig));
    }
}
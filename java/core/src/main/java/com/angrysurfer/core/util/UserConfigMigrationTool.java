package com.angrysurfer.core.util;

import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.SessionHelper;
import com.angrysurfer.core.redis.UserConfigHelper;
import com.angrysurfer.core.service.UserConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Migration utility to convert UserConfig.InstrumentConfig objects to InstrumentWrapper objects.
 * This preserves device information while moving to a more consistent model.
 */
public class UserConfigMigrationTool {
    private static final Logger logger = LoggerFactory.getLogger(UserConfigMigrationTool.class);
    
    /**
     * Main method to run the migration
     */
    public static void main(String[] args) {
        logger.info("Starting UserConfig to InstrumentWrapper migration...");
        
        try {
            // 1. Set up Redis connection
            logger.info("Connecting to Redis...");
            RedisService redisService = RedisService.getInstance();
            UserConfigHelper configHelper = redisService.getUserConfigHelper();
            SessionHelper sessionHelper = redisService.getSessionHelper();
            
            // 2. Load user configuration
            logger.info("Loading UserConfig...");
            UserConfig userConfig = configHelper.loadConfigFromRedis();
            
            if (userConfig == null) {
                logger.error("No UserConfig found in Redis. Creating a new one.");
                userConfig = new UserConfig();
                userConfig.setInstruments(new ArrayList<>());
                userConfig.setConfigs(new HashSet<>());
                userConfig.setLastUpdated(new Date());
            }
            
            // 3. Load the first session
            logger.info("Loading first session...");
            Session session = sessionHelper.findFirstValidSession();
            
            if (session == null) {
                logger.info("No valid session found. Creating a new session.");
                session = sessionHelper.newSession();
            }
            
            // 4. Convert InstrumentConfig objects to InstrumentWrapper objects
            logger.info("Converting InstrumentConfig objects to InstrumentWrapper objects...");
            List<InstrumentWrapper> convertedInstruments = convertConfigsToInstruments(userConfig);
            
            // 5. Add converted instruments to the existing instruments
            if (userConfig.getInstruments() == null) {
                userConfig.setInstruments(new ArrayList<>());
            }
            
            // Track how many were actually added (avoiding duplicates)
            int addedCount = 0;
            for (InstrumentWrapper newInstrument : convertedInstruments) {
                if (!containsInstrumentByDeviceName(userConfig.getInstruments(), newInstrument.getDeviceName())) {
                    userConfig.getInstruments().add(newInstrument);
                    addedCount++;
                }
            }
            
            logger.info("Added {} new instruments to UserConfig", addedCount);
            
            // 6. Save the updated UserConfig
            logger.info("Saving updated UserConfig...");
            userConfig.setLastUpdated(new Date());
            configHelper.saveConfig(userConfig);
            
            // 7. Save the session to ensure it's current
            logger.info("Updating session...");
            sessionHelper.saveSession(session);
            
            // 8. Verify the changes
            logger.info("Verifying changes...");
            UserConfig verifiedConfig = configHelper.loadConfigFromRedis();
            Session verifiedSession = sessionHelper.findSessionById(session.getId());
            
            if (verifiedConfig != null && verifiedConfig.getInstruments() != null) {
                logger.info("Verification successful. UserConfig now has {} instruments.",
                          verifiedConfig.getInstruments().size());
                
                // Log the instrument details
                logger.info("Instruments in UserConfig:");
                for (InstrumentWrapper instrument : verifiedConfig.getInstruments()) {
                    logger.info("  - {} (ID: {}, Device: {})", 
                              instrument.getName(), instrument.getId(), instrument.getDeviceName());
                }
            } else {
                logger.error("Verification failed. Could not load updated UserConfig.");
            }
            
            if (verifiedSession != null) {
                logger.info("Session verification successful. Session ID: {}", verifiedSession.getId());
            } else {
                logger.error("Session verification failed. Could not load session.");
            }
            
            logger.info("Migration complete!");
            
        } catch (Exception e) {
            logger.error("Error during migration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Convert UserConfig.InstrumentConfig objects to InstrumentWrapper objects
     */
    private static List<InstrumentWrapper> convertConfigsToInstruments(UserConfig userConfig) {
        List<InstrumentWrapper> convertedInstruments = new ArrayList<>();
        Set<UserConfig.InstrumentConfig> configs = userConfig.getConfigs();
        
        if (configs == null || configs.isEmpty()) {
            logger.info("No InstrumentConfig objects found to convert.");
            return convertedInstruments;
        }
        
        logger.info("Found {} InstrumentConfig objects to convert", configs.size());
        
        for (UserConfig.InstrumentConfig config : configs) {
            try {
                // Skip if missing essential data
                if (config.getDevice() == null || config.getDevice().trim().isEmpty()) {
                    logger.warn("Skipping InstrumentConfig with no device name");
                    continue;
                }
                
                // Create a new InstrumentWrapper
                InstrumentWrapper instrument = new InstrumentWrapper();
                
                // Generate a unique ID if needed
                instrument.setId(System.currentTimeMillis() + convertedInstruments.size());
                
                // Set the name based on the device, or create a descriptive name
                String deviceName = config.getDevice().trim();
                instrument.setDeviceName(deviceName);
                
                // Create a descriptive name
                String name = deviceName;
                if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
                    name += " (" + config.getPort().trim() + ")";
                }
                instrument.setName(name);
                
                // Set other properties
                instrument.setAvailable(config.isAvailable());
                
                // Map channels to something reasonable
                int channels = config.getChannels();
                if (channels <= 0) {
                    channels = 16; // Default to 16 channels
                }
                
                // Set default channel to GM drum channel (10, but zero-indexed as 9)
                instrument.setChannel(9);
                
                // Add to the result list
                convertedInstruments.add(instrument);
                logger.info("Converted '{}' to InstrumentWrapper '{}'", 
                          config.getDevice(), instrument.getName());
                
            } catch (Exception e) {
                logger.error("Error converting InstrumentConfig: {}", e.getMessage(), e);
            }
        }
        
        logger.info("Successfully converted {} InstrumentConfig objects to InstrumentWrapper objects", 
                  convertedInstruments.size());
        return convertedInstruments;
    }
    
    /**
     * Check if the list of instruments already contains an instrument with the same device name
     */
    private static boolean containsInstrumentByDeviceName(List<InstrumentWrapper> instruments, String deviceName) {
        if (deviceName == null || instruments == null) {
            return false;
        }
        
        for (InstrumentWrapper instrument : instruments) {
            if (deviceName.equals(instrument.getDeviceName())) {
                return true;
            }
        }
        
        return false;
    }
}
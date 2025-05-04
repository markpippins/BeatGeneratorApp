package com.angrysurfer.beats.diagnostic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;

/**
 * Utility to migrate from old UserConfig format (with InstrumentConfig) to new format
 */
public class UserConfigMigrationTool {
    private static final Logger logger = LoggerFactory.getLogger(UserConfigMigrationTool.class);
    
    /**
     * Main entry point for running the migration tool
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Show confirmation dialog
                int choice = JOptionPane.showConfirmDialog(
                    null,
                    "This will migrate your instrument configuration data to the new format.\n" +
                    "It will convert all InstrumentConfig entries to InstrumentWrapper objects.\n\n" +
                    "It's recommended to backup your configuration before proceeding.\n\n" +
                    "Do you want to continue?",
                    "Confirm Migration",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (choice != JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(null, "Migration cancelled.");
                    return;
                }
                
                // Run the migration
                boolean success = migrateUserConfig();
                
                if (success) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Migration completed successfully!\n\n" +
                        "All instrument configurations have been converted to the new format.",
                        "Migration Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        "Migration failed. Please check the logs for details.",
                        "Migration Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (Exception e) {
                logger.error("Error during migration: {}", e.getMessage(), e);
                JOptionPane.showMessageDialog(
                    null,
                    "An error occurred during migration: " + e.getMessage(),
                    "Migration Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
    
    /**
     * Perform the actual migration
     */
    public static boolean migrateUserConfig() {
        logger.info("Starting UserConfig migration");
        RedisService redisService = RedisService.getInstance();
        UserConfigHelper configHelper = redisService.getUserConfigHelper();
        
        try {
            // 1. Load current config
            UserConfig config = configHelper.loadConfigFromRedis();
            if (config == null) {
                logger.warn("No existing UserConfig found in Redis");
                return false;
            }
            
            logger.info("Loaded UserConfig, version: {}", config.getConfigVersion());
            
            // 2. Backup the original config
            String backupPath = System.getProperty("user.home") + "/userconfig_backup_" + 
                              System.currentTimeMillis() + ".json";
            boolean backupSuccess = configHelper.saveConfigToJSON(config, backupPath);
            
            if (backupSuccess) {
                logger.info("Created backup of original config at: {}", backupPath);
            } else {
                logger.warn("Failed to create backup, proceeding with caution");
            }
            
            // 3. Process InstrumentConfig objects if present
//            Set<?> configs = config.con
//            List<InstrumentWrapper> convertedInstruments = new ArrayList<>();
//
//            if (configs != null && !configs.isEmpty()) {
//                logger.info("Found {} InstrumentConfig objects to migrate", configs.size());
//
//                for (Object configObj : configs) {
//                    if (!(configObj instanceof UserConfig.InstrumentConfig)) {
//                        logger.warn("Found non-InstrumentConfig object in configs collection, skipping");
//                        continue;
//                    }
//
//                    try {
//                        // Since we've removed the class, we need to use reflection
//                        String device = getFieldValue(configObj, "device", String.class);
//                        String port = getFieldValue(configObj, "port", String.class);
//                        Boolean available = getFieldValue(configObj, "available", Boolean.class);
//                        Integer channels = getFieldValue(configObj, "channels", Integer.class);
//
//                        if (device == null || device.trim().isEmpty()) {
//                            logger.warn("Skipping InstrumentConfig with no device name");
//                            continue;
//                        }
//
//                        // Create new instrument
//                        InstrumentWrapper instrument = new InstrumentWrapper();
//                        instrument.setId(System.currentTimeMillis() + convertedInstruments.size());
//                        instrument.setDeviceName(device);
//
//                        // Create descriptive name
//                        String name = device;
//                        if (port != null && !port.trim().isEmpty()) {
//                            name += " (" + port.trim() + ")";
//                        }
//                        instrument.setName(name);
//
//                        // Set availability
//                        instrument.setAvailable(available != null ? available : false);
//
//                        // Default to GM drum channel
//                        instrument.setChannel(9);
//
//                        convertedInstruments.add(instrument);
//                        logger.info("Converted '{}' to InstrumentWrapper '{}'", device, name);
//                    } catch (Exception e) {
//                        logger.error("Error converting InstrumentConfig: {}", e.getMessage(), e);
//                    }
//                }
//
//                // 4. Add converted instruments to the config
//                for (InstrumentWrapper instrument : convertedInstruments) {
//                    if (!containsInstrumentByDeviceName(config.getInstruments(), instrument.getDeviceName())) {
//                        config.getInstruments().add(instrument);
//                        logger.info("Added new instrument: {}", instrument.getName());
//                    } else {
//                        logger.info("Skipped duplicate instrument: {}", instrument.getDeviceName());
//                    }
//                }
//
//                // 5. Clear the old configs collection (this won't remove it from the JSON, but the loader will ignore it)
//                configs.clear();
//            } else {
//                logger.info("No InstrumentConfig objects found, migration not needed");
//            }
            
            // 6. Update version and timestamp
            config.setConfigVersion(2);
            config.setLastUpdated(new Date());
            
            // 7. Save the updated config
            configHelper.saveConfig(config);
            logger.info("Migration completed successfully");
            
            return true;
        } catch (Exception e) {
            logger.error("Error during UserConfig migration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Utility method to get field value using reflection
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String fieldName, Class<T> expectedType) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value == null || expectedType.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            logger.debug("Error accessing field {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if an instrument with the given device name already exists
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
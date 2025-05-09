package com.angrysurfer.beats.diagnostic.helper;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;

import java.util.UUID;

/**
 * Helper class for UserConfig diagnostics
 */
public class UserConfigDiagnosticsHelper {
    
    private final RedisService redisService;
    private final UserConfigHelper configHelper;
    
    /**
     * Constructor
     */
    public UserConfigDiagnosticsHelper() {
        this.redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
    }
    
    /**
     * Run diagnostic tests for user configuration
     */
    public DiagnosticLogBuilder testUserConfigDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("User Configuration Diagnostics");
        
    //    try {
    //        // Test direct UserConfig object
    //        log.addSection("Test: UserConfig operations");

    //        // First, check existing config
    //        UserConfig existingConfig = configHelper.loadConfigFromRedis();
    //        if (existingConfig != null) {
    //            log.addIndentedLine("Found existing UserConfig in Redis", 1);
    //        } else {
    //            log.addIndentedLine("No existing UserConfig found in Redis", 1);
    //        }

    //        // Create a temporary test config
    //        UserConfig testConfig = existingConfig != null ? existingConfig : new UserConfig();

    //        // Create a test InstrumentConfig object
    //        UserConfig.InstrumentConfig testInstrumentConfig = new UserConfig.InstrumentConfig();
    //        // Since we don't have setName() and setId(), use the fields directly based on what's in UserConfig.java
    //        String testDeviceName = "TestDevice_" + UUID.randomUUID().toString().substring(0, 8);
    //        testInstrumentConfig.device = testDeviceName;
    //        testInstrumentConfig.port = "TEST_PORT";
    //        testInstrumentConfig.available = true;
    //        testInstrumentConfig.setChannels = 16;
    //        testInstrumentConfig.low = 0;
    //        testInstrumentConfig.high = 127;

    //        log.addIndentedLine("Adding test instrument config with device: " + testDeviceName, 1);

    //        // Add the test instrument config to the configs collection
    //        if (testConfig.getConfigs() == null) {
    //            testConfig.setConfigs(new java.util.HashSet<>());
    //        }
    //        testConfig.getConfigs().add(testInstrumentConfig);

    //        // Save it to Redis
    //        log.addIndentedLine("Saving test config to Redis", 1);
    //        configHelper.saveConfig(testConfig);

    //        // Verify retrieval
    //        UserConfig retrievedConfig = configHelper.loadConfigFromRedis();
    //        boolean found = false;

    //        if (retrievedConfig != null && retrievedConfig.getConfigs() != null) {
    //            for (UserConfig.InstrumentConfig config : retrievedConfig.getConfigs()) {
    //                // Since we don't have getName(), check the device field
    //                if (testDeviceName.equals(config.device)) {
    //                    found = true;
    //                    break;
    //                }
    //            }

    //            if (found) {
    //                log.addIndentedLine("Successfully verified test config in Redis", 1);
    //            } else {
    //                log.addError("Failed to retrieve test device from saved config");
    //            }
    //        } else {
    //            log.addError("Failed to retrieve config from Redis or configs collection is null");
    //        }

    //        // Test file operations (optional)
    //        log.addSection("Test: File operations");

    //        String tempFilePath = System.getProperty("java.io.tmpdir") + "/test_config_" +
    //                             System.currentTimeMillis() + ".json";
    //        log.addIndentedLine("Saving config to temporary file: " + tempFilePath, 1);

    //        boolean fileSaved = configHelper.saveConfigToJSON(testConfig, tempFilePath);
    //        if (fileSaved) {
    //            log.addIndentedLine("Successfully saved config to file", 1);

    //            // Try to load it back
    //            UserConfig fileConfig = configHelper.loadConfigFromJSON(tempFilePath);
    //            found = false;

    //            if (fileConfig != null && fileConfig.getConfigs() != null) {
    //                for (UserConfig.InstrumentConfig config : fileConfig.getConfigs()) {
    //                    // Check the device field instead of getName()
    //                    if (testDeviceName.equals(config.device)) {
    //                        found = true;
    //                        break;
    //                    }
    //                }

    //                if (found) {
    //                    log.addIndentedLine("Successfully loaded config from file", 1);
    //                } else {
    //                    log.addError("Failed to find test device in loaded config");
    //                }
    //            } else {
    //                log.addError("Failed to load config from file or configs collection is null");
    //            }

    //            // Clean up the test file
    //            try {
    //                java.io.File testFile = new java.io.File(tempFilePath);
    //                if (testFile.exists() && testFile.delete()) {
    //                    log.addIndentedLine("Cleaned up temporary test file", 1);
    //                }
    //            } catch (Exception e) {
    //                log.addIndentedLine("Note: Could not delete temporary file: " + e.getMessage(), 1);
    //            }
    //        } else {
    //            log.addError("Failed to save config to file");
    //        }

    //        // Restore original config if needed
    //        if (existingConfig != null && !existingConfig.equals(testConfig)) {
    //            configHelper.saveConfig(existingConfig);
    //            log.addIndentedLine("Restored original config", 1);
    //        }

    //    } catch (Exception e) {
    //        log.addException(e);
    //    }
        
        return log;
    }
}

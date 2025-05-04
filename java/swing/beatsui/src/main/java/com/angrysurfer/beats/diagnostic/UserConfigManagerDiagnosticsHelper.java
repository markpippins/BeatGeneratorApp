package com.angrysurfer.beats.diagnostic;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.UserConfigManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic helper for UserConfigManager
 */
public class UserConfigManagerDiagnosticsHelper {

    /**
     * Run diagnostics on UserConfigManager
     */
    public DiagnosticLogBuilder testUserConfigManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("UserConfigManager Diagnostics");
        
        try {
            UserConfigManager manager = UserConfigManager.getInstance();
            log.addLine("UserConfigManager instance obtained: " + (manager != null));
            
            // Get current configuration
            UserConfig config = manager.getCurrentConfig();
            log.addLine("Current configuration: " + (config != null ? "Available" : "Null"));
            
            if (config != null) {
                testConfig(config, log);
            } else {
                log.addError("No user configuration available");
                return log;
            }
            
            // Test instrument retrieval
            log.addSection("Instrument Retrieval Test");
            List<InstrumentWrapper> instruments = manager.getInstruments();
            log.addLine("Retrieved " + instruments.size() + " instruments");
            
            if (!instruments.isEmpty()) {
                InstrumentWrapper firstInstrument = instruments.get(0);
                log.addLine("Testing instrument lookup by ID: " + firstInstrument.getId());
                
                InstrumentWrapper found = manager.findInstrumentById(firstInstrument.getId());
                log.addLine("Lookup result: " + (found != null ? "Found" : "Not found"));
                
                if (found != null) {
                    boolean sameInstance = found == firstInstrument;
                    log.addLine("Same instance: " + sameInstance);
                    
                    if (!sameInstance) {
                        boolean equalContent = found.getId().equals(firstInstrument.getId()) &&
                                            found.getName().equals(firstInstrument.getName());
                        log.addLine("Equal content: " + equalContent);
                    }
                }
                
                // Test name lookup
                if (firstInstrument.getName() != null) {
                    String nameFragment = firstInstrument.getName().substring(0, 
                            Math.min(3, firstInstrument.getName().length()));
                    
                    log.addLine("Testing instrument lookup by name fragment: \"" + nameFragment + "\"");
                    List<InstrumentWrapper> foundByName = manager.findInstrumentsByName(nameFragment);
                    log.addLine("Found " + foundByName.size() + " instruments containing \"" + nameFragment + "\"");
                }
            }
            
            // Test config backup and restore
            log.addSection("Configuration Backup Test");
            
            try {
                // Create a temp directory for testing
                Path tempDir = Files.createTempDirectory("config_test");
                String backupPath = tempDir.resolve("config_backup_test.json").toString();
                
                log.addLine("Testing backup to: " + backupPath);
                boolean backupSuccess = manager.backupConfiguration(backupPath);
                log.addLine("Backup result: " + (backupSuccess ? "Success" : "Failed"));
                
                if (backupSuccess) {
                    File backupFile = new File(backupPath);
                    log.addLine("Backup file exists: " + backupFile.exists());
                    log.addLine("Backup file size: " + backupFile.length() + " bytes");
                    
                    // Don't actually restore as it would change the application state
                    log.addLine("Restore test skipped to avoid modifying application state");
                    
                    // Clean up
                    boolean deleted = backupFile.delete();
                    log.addLine("Temporary backup file deleted: " + deleted);
                }
                
                // Clean up temp directory
                Files.delete(tempDir);
            } catch (Exception e) {
                log.addWarning("Error during backup test: " + e.getMessage());
            }
            
            // Test command bus registration
            log.addSection("Event System Registration");
            CommandBus commandBus = CommandBus.getInstance();
            // log.addLine("UserConfigManager receives events: " + commandBus.isRegistered(manager));
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test configuration integrity
     */
    private void testConfig(UserConfig config, DiagnosticLogBuilder log) {
        log.addSection("Configuration Details");
        log.addLine("Config version: " + config.getConfigVersion());
        log.addLine("Last updated: " + (config.getLastUpdated() != null ? 
                config.getLastUpdated().toString() : "Not set"));
        
        // Test instrument configuration
        List<InstrumentWrapper> instruments = config.getInstruments();
        log.addLine("Instruments: " + (instruments != null ? instruments.size() : "None"));
        
        if (instruments != null && !instruments.isEmpty()) {
            // Count device types
            Map<String, Integer> deviceCount = new HashMap<>();
            int internalInstruments = 0;
            int missingNames = 0;
            int missingIds = 0;
            
            for (InstrumentWrapper instrument : instruments) {
                // Count by device type
                String deviceType = instrument.getDeviceName() != null ? instrument.getDeviceName() : "Unknown";
                deviceCount.put(deviceType, deviceCount.getOrDefault(deviceType, 0) + 1);
                
                // Count internal instruments
                if (deviceType.contains("Gervill") || deviceType.contains("Internal")) {
                    internalInstruments++;
                }
                
                // Check for missing data
                if (instrument.getName() == null || instrument.getName().trim().isEmpty()) {
                    missingNames++;
                }
                
                if (instrument.getId() == null) {
                    missingIds++;
                }
            }
            
            log.addIndentedLine("Internal instruments: " + internalInstruments, 1);
            log.addIndentedLine("Instruments by device type:", 1);
            
            for (Map.Entry<String, Integer> entry : deviceCount.entrySet()) {
                log.addIndentedLine(entry.getKey() + ": " + entry.getValue(), 2);
            }
            
            if (missingNames > 0 || missingIds > 0) {
                log.addWarning("Found incomplete instrument records:");
                if (missingNames > 0) log.addIndentedLine("Missing names: " + missingNames, 1);
                if (missingIds > 0) log.addIndentedLine("Missing IDs: " + missingIds, 1);
            }
            
            // Check for duplicate instrument IDs
            Map<Long, List<String>> idToNames = new HashMap<>();
            
            for (InstrumentWrapper instrument : instruments) {
                if (instrument.getId() != null) {
                    idToNames.computeIfAbsent(instrument.getId(), id -> new ArrayList<>())
                            .add(instrument.getName() != null ? instrument.getName() : "Unknown");
                }
            }
            
            List<Long> duplicateIds = new ArrayList<>();
            for (Map.Entry<Long, List<String>> entry : idToNames.entrySet()) {
                if (entry.getValue().size() > 1) {
                    duplicateIds.add(entry.getKey());
                }
            }
            
            if (!duplicateIds.isEmpty()) {
                log.addWarning("Found " + duplicateIds.size() + " duplicate instrument IDs:");
                for (Long id : duplicateIds) {
                    log.addIndentedLine("ID " + id + ": " + String.join(", ", idToNames.get(id)), 1);
                }
            }
        }
        
        // Test device configs
        if (config.getConfigs() != null) {
            log.addLine("Device configs: " + config.getConfigs().size());
        } else {
            log.addLine("Device configs: None");
        }
        
        // Test Redis connectivity for user config
        log.addSection("Redis Connectivity Test");
        try {
            RedisService redisService = RedisService.getInstance();
            
            if (redisService != null && redisService.getUserConfigHelper() != null) {
                log.addLine("UserConfigHelper available: Yes");
                
                // Try loading config from Redis
                UserConfig redisConfig = redisService.getUserConfigHelper().loadConfigFromRedis();
                log.addLine("Config loaded from Redis: " + (redisConfig != null ? "Yes" : "No"));
                
                if (redisConfig != null) {
                    boolean sameConfig = redisConfig == config;
                    log.addLine("Same instance as current config: " + sameConfig);
                    
                    if (!sameConfig) {
                        // Compare basic attributes
                        boolean sameVersion = redisConfig.getConfigVersion() == config.getConfigVersion();
                        boolean sameInstrumentCount = 
                            redisConfig.getInstruments() != null && 
                            config.getInstruments() != null &&
                            redisConfig.getInstruments().size() == config.getInstruments().size();
                        
                        log.addLine("Same version: " + sameVersion);
                        log.addLine("Same instrument count: " + sameInstrumentCount);
                    }
                }
            } else {
                log.addWarning("UserConfigHelper not available, can't test Redis connectivity");
            }
        } catch (Exception e) {
            log.addError("Error testing Redis connectivity: " + e.getMessage());
        }
    }
    
    /**
     * Test config transaction support
     */
    public DiagnosticLogBuilder testConfigTransactions() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("UserConfig Transaction Diagnostics");
        
        try {
            UserConfigManager manager = UserConfigManager.getInstance();
            log.addLine("Testing configuration transaction support");
            
            // Get current state for comparison
            int initialInstrumentCount = manager.getInstruments().size();
            log.addLine("Initial instrument count: " + initialInstrumentCount);
            
            // Test transaction that makes no changes
            log.addSection("Read-Only Transaction Test");
            boolean readOnlyResult = manager.updateConfigInTransaction(config -> {
                // Just inspect the config without changing it
                int instrumentCount = config.getInstruments().size();
                log.addIndentedLine("Transaction sees " + instrumentCount + " instruments", 1);
                return true; // Success
            });
            
            log.addLine("Read-only transaction result: " + readOnlyResult);
            
            // Verify no changes occurred
            int afterReadOnlyCount = manager.getInstruments().size();
            log.addLine("Instrument count after read-only transaction: " + afterReadOnlyCount);
            log.addLine("Count unchanged: " + (initialInstrumentCount == afterReadOnlyCount));
            
            // Test transaction that makes then reverts changes
            log.addSection("Self-Reverting Transaction Test");
            boolean revertingResult = manager.updateConfigInTransaction(config -> {
                // Add a temporary instrument
                InstrumentWrapper tempInstrument = new InstrumentWrapper();
                tempInstrument.setId(System.currentTimeMillis());
                tempInstrument.setName("Temporary Test Instrument");
                
                // Add to config
                config.getInstruments().add(tempInstrument);
                log.addIndentedLine("Added temporary instrument, count now: " + 
                                  config.getInstruments().size(), 1);
                
                // Then remove it
                config.getInstruments().remove(tempInstrument);
                log.addIndentedLine("Removed temporary instrument, count now: " + 
                                  config.getInstruments().size(), 1);
                
                return true; // Success
            });
            
            log.addLine("Self-reverting transaction result: " + revertingResult);
            
            // Verify no changes occurred
            int afterRevertingCount = manager.getInstruments().size();
            log.addLine("Instrument count after self-reverting transaction: " + afterRevertingCount);
            log.addLine("Count unchanged: " + (initialInstrumentCount == afterRevertingCount));
            
            // Test transaction that would fail validation
            log.addSection("Invalid Transaction Test");
            boolean invalidResult = manager.updateConfigInTransaction(config -> {
                // Add an invalid instrument (no ID)
                InstrumentWrapper invalidInstrument = new InstrumentWrapper();
                invalidInstrument.setName("Invalid Instrument");
                
                // Add to config
                config.getInstruments().add(invalidInstrument);
                log.addIndentedLine("Added invalid instrument without ID", 1);
                
                return true; // We return success but validation should fail
            });
            
            log.addLine("Invalid transaction result: " + invalidResult);
            
            // Verify no changes occurred
            int afterInvalidCount = manager.getInstruments().size();
            log.addLine("Instrument count after invalid transaction: " + afterInvalidCount);
            log.addLine("Count unchanged: " + (initialInstrumentCount == afterInvalidCount));
            
            // Test rejected transaction
            log.addSection("Rejected Transaction Test");
            boolean rejectedResult = manager.updateConfigInTransaction(config -> {
                // Simulate a transaction that detects a problem and rejects itself
                log.addIndentedLine("Transaction detected a problem and is rejecting itself", 1);
                return false; // Reject the transaction
            });
            
            log.addLine("Rejected transaction result: " + rejectedResult);
            
            // Verify final state matches initial state
            int finalCount = manager.getInstruments().size();
            log.addLine("Final instrument count: " + finalCount);
            log.addLine("Count matches initial state: " + (initialInstrumentCount == finalCount));
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
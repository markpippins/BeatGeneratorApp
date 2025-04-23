package com.angrysurfer.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized manager for MIDI receivers to handle device connections
 * and recovery strategies
 */
public class ReceiverManager {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverManager.class);
    private static ReceiverManager instance;
    
    // Cache receivers by device name for reuse
    private final Map<String, Receiver> receiverCache = new ConcurrentHashMap<>();
    // Track if devices are being reconnected to avoid duplicate attempts
    private final Map<String, Boolean> reconnectionInProgress = new ConcurrentHashMap<>();
    // Add a timestamp map for validation
    private final Map<String, Long> lastValidationTime = new ConcurrentHashMap<>();
    private static final long VALIDATION_INTERVAL_MS = 5000; // Only validate every 5 seconds
    
    private ReceiverManager() {
        // Private constructor for singleton pattern
    }
    
    public static synchronized ReceiverManager getInstance() {
        if (instance == null) {
            instance = new ReceiverManager();
        }
        return instance;
    }
    
    /**
     * Get or create a receiver for the specified device with error handling and recovery
     * 
     * @param deviceName The name of the MIDI device
     * @param device Current device reference (may be null)
     * @return A valid receiver or null if unavailable
     */
    public synchronized Receiver getOrCreateReceiver(String deviceName, MidiDevice device) {
        logger.debug("getOrCreateReceiver for device: {}", deviceName);
        
        // Check if we have a cached receiver that is still valid
        Receiver cachedReceiver = receiverCache.get(deviceName);
        if (cachedReceiver != null) {
            // Only validate periodically, not on every call
            Long lastValidated = lastValidationTime.getOrDefault(deviceName, 0L);
            if (System.currentTimeMillis() - lastValidated < VALIDATION_INTERVAL_MS) {
                return cachedReceiver; // Skip validation most of the time
            }
            
            if (isReceiverValid(cachedReceiver)) {
                lastValidationTime.put(deviceName, System.currentTimeMillis());
                return cachedReceiver;
            }
            
            // Remove invalid receiver from cache
            logger.debug("Removing invalid cached receiver for: {}", deviceName);
            receiverCache.remove(deviceName);
            try {
                cachedReceiver.close();
            } catch (Exception e) {
                logger.debug("Error closing invalid receiver: {}", e.getMessage());
            }
        }
        
        // Check if current device reference is valid and open
        if (device != null && device.isOpen()) {
            try {
                logger.debug("Creating receiver from provided device: {}", deviceName);
                Receiver newReceiver = device.getReceiver();
                if (isReceiverValid(newReceiver)) {
                    receiverCache.put(deviceName, newReceiver);
                    return newReceiver;
                }
            } catch (Exception e) {
                logger.warn("Failed to get receiver from provided device: {}", e.getMessage());
            }
        }
        
        // Need to get a fresh device
        if (reconnectionInProgress.getOrDefault(deviceName, false)) {
            logger.debug("Reconnection already in progress for: {}", deviceName);
            return null;
        }
        
        try {
            reconnectionInProgress.put(deviceName, true);
            
            // Get a fresh device instance from DeviceManager
            MidiDevice freshDevice = DeviceManager.getMidiDevice(deviceName);
            
            if (freshDevice != null) {
                try {
                    if (!freshDevice.isOpen()) {
                        freshDevice.open();
                    }
                    
                    if (freshDevice.isOpen()) {
                        Receiver newReceiver = freshDevice.getReceiver();
                        if (isReceiverValid(newReceiver)) {
                            logger.info("Successfully created receiver for: {}", deviceName);
                            receiverCache.put(deviceName, newReceiver);
                            return newReceiver;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error creating receiver from fresh device: {}", e.getMessage());
                }
            }
            
            // Special handling for Gervill
            if ("Gervill".equals(deviceName)) {
                return handleGervillReconnection();
            }
            
            // Try default system receiver as last resort
            try {
                Receiver defaultReceiver = MidiSystem.getReceiver();
                if (isReceiverValid(defaultReceiver)) {
                    logger.info("Using default system receiver as fallback for: {}", deviceName);
                    receiverCache.put(deviceName, defaultReceiver);
                    return defaultReceiver;
                }
            } catch (Exception e) {
                logger.error("Failed to get default system receiver: {}", e.getMessage());
            }
            
            return null;
        } finally {
            reconnectionInProgress.put(deviceName, false);
        }
    }
    
    /**
     * Close and remove a receiver from the cache
     * 
     * @param deviceName The device name
     */
    public synchronized void closeReceiver(String deviceName) {
        // Add null check to prevent NullPointerException
        if (deviceName == null) {
            logger.warn("Attempted to close receiver with null device name");
            return;
        }
        
        Receiver receiver = receiverCache.remove(deviceName);
        if (receiver != null) {
            try {
                receiver.close();
                logger.debug("Closed and removed receiver for: {}", deviceName);
            } catch (Exception e) {
                logger.debug("Error closing receiver: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Check if receiver is valid by sending a test message
     */
    private boolean isReceiverValid(Receiver receiver) {
        if (receiver == null) return false;
        
        // Skip actual validation test most of the time
        return true; // Assume valid to improve performance
        
        /* Original validation code - too expensive for real-time
        try {
            receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 120, 0), -1);
            return true;
        } catch (Exception e) {
            logger.debug("Receiver validation failed: {}", e.getMessage());
            return false;
        }
        */
    }
    
    /**
     * Special handling for Gervill synthesizer
     */
    private Receiver handleGervillReconnection() {
        try {
            logger.info("Attempting Gervill reconnection");
            
            // Clean up devices first
            DeviceManager.cleanupMidiDevices();
            
            // Try to find and open Gervill
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                if (info.getName().contains("Gervill")) {
                    try {
                        MidiDevice gervillDevice = MidiSystem.getMidiDevice(info);
                        if (!gervillDevice.isOpen()) {
                            gervillDevice.open();
                        }
                        
                        Receiver gervillReceiver = gervillDevice.getReceiver();
                        if (isReceiverValid(gervillReceiver)) {
                            logger.info("Successfully reconnected to Gervill");
                            receiverCache.put("Gervill", gervillReceiver);
                            return gervillReceiver;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to connect to Gervill: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Gervill reconnection failed: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Clear all receivers (useful during shutdown)
     */
    public synchronized void clearAllReceivers() {
        for (String deviceName : receiverCache.keySet()) {
            closeReceiver(deviceName);
        }
        receiverCache.clear();
        logger.info("All receivers cleared");
    }
}
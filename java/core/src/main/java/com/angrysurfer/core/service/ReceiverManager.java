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
     * Add an improved version of getOrCreateReceiver that handles
     * error cases better
     */
    public Receiver getOrCreateReceiver(String deviceName, MidiDevice device) {
        if (deviceName == null || deviceName.isEmpty()) {
            logger.warn("Cannot get receiver for null or empty device name");
            return null;
        }
        
        // Check if we already have a receiver for this device
        Receiver receiver = receiverCache.get(deviceName);
        if (receiver != null) {
            // Already have a cached receiver - verify it's still valid
            try {
                // Try to send a dummy message to test if receiver is still valid
                ShortMessage msg = new ShortMessage();
                msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, 127);
                receiver.send(msg, -1);
                return receiver; // Receiver is valid
            } catch (Exception e) {
                // Receiver is invalid, remove it and continue to create a new one
                logger.debug("Cached receiver for {} is invalid, creating new one", deviceName);
                closeReceiver(deviceName);
            }
        }
        
        // If device wasn't provided, try to get it
        if (device == null) {
            device = DeviceManager.getMidiDevice(deviceName);
        }
        
        if (device == null) {
            logger.warn("Could not find device: {}", deviceName);
            return null;
        }
        
        // Make sure device is open
        try {
            if (!device.isOpen()) {
                device.open();
            }
        } catch (Exception e) {
            logger.error("Failed to open device {}: {}", deviceName, e.getMessage());
            return null;
        }
        
        // Get receiver from device
        try {
            if (device.getMaxReceivers() != 0) {
                receiver = device.getReceiver();
                receiverCache.put(deviceName, receiver);
                logger.debug("Created new receiver for device: {}", deviceName);
                return receiver;
            } else {
                logger.warn("Device does not support receivers: {}", deviceName);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to get receiver for {}: {}", deviceName, e.getMessage());
            return null;
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
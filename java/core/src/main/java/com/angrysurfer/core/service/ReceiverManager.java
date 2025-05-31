package com.angrysurfer.core.service;

import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized manager for MIDI receivers to handle device connections
 * and recovery strategies
 */
public class ReceiverManager {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverManager.class);
    private static final long VALIDATION_INTERVAL_MS = 5000; // Only validate every 5 seconds
    private static ReceiverManager instance;
    private final DeviceManager deviceManager; // Added DeviceManager instance
    // Cache receivers by device name for reuse
    private final Map<String, Receiver> receiverCache = new ConcurrentHashMap<>();
    // Add a timestamp map for validation
    private final Map<String, Long> lastValidationTime = new ConcurrentHashMap<>();

    private ReceiverManager() {
        // Private constructor for singleton pattern
        this.deviceManager = DeviceManager.getInstance(); // Initialize DeviceManager
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
    public Receiver getOrCreateReceiver(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) {
            logger.warn("Cannot get receiver for null or empty device name");
            return null;
        }

        // Check cache first
        Receiver receiver = receiverCache.get(deviceName);
        if (isReceiverValid(receiver, deviceName)) {
            return receiver; // Cached receiver is valid
        }

        // If in cache but invalid, remove it
        if (receiver != null) {
            logger.warn("Cached receiver for {} is invalid. Attempting to recreate.", deviceName);
            closeReceiver(deviceName); // Close and remove the invalid one
        }

        // Attempt to acquire the device and get a new receiver
        MidiDevice device = deviceManager.acquireDevice(deviceName);

        if (device == null) {
            logger.warn("Could not acquire MIDI device: {}", deviceName);
            // Special handling for Gervill if it's the requested device
            if (SequencerConstants.GERVILL.equalsIgnoreCase(deviceName)) {
                logger.info("Attempting to ensure Gervill is available as fallback.");
                // Try to use the specific Gervill reconnection logic which might involve more steps
                return handleGervillReconnection(); // Use the dedicated method
            }
            // If not Gervill or Gervill ensure failed, return null
            return null;
        }

        try {
            if (!device.isOpen()) { // Should be open if acquired successfully, but double check
                logger.warn("Device {} was acquired but is not open. Attempting to open.", deviceName);
                device.open(); // This might be redundant if acquireDevice guarantees an open device
            }

            if (device.getMaxReceivers() != 0) { // Check if it can provide receivers
                receiver = device.getReceiver();
                if (receiver != null) {
                    receiverCache.put(deviceName, receiver);
                    logger.info("Successfully created and cached new receiver for device: {}", deviceName);
                    return receiver;
                } else {
                    logger.error("Device {} returned a null receiver.", deviceName);
                }
            } else {
                logger.warn("Device {} does not support receivers (maxReceivers is 0).", deviceName);
            }
        } catch (Exception e) {
            logger.error("Failed to get receiver for device {}: {}", deviceName, e.getMessage(), e);
        }
        // If we reach here, receiver creation failed. Release the device if we acquired it.
        deviceManager.releaseDevice(deviceName);
        return null;
    }

    /**
     * Close and remove a receiver from the cache
     *
     * @param deviceName The device name
     */
    public synchronized void closeReceiver(String deviceName) {
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
                logger.warn("Error closing receiver for {}: {}", deviceName, e.getMessage());
            }
        }
        lastValidationTime.remove(deviceName); // Also remove from validation tracking
    }

    /**
     * Check if receiver is valid by sending a test message
     */
    private boolean isReceiverValid(Receiver receiver, String deviceName) {
        if (receiver == null) return false;

        // Throttle validation to avoid performance issues
        long currentTime = System.currentTimeMillis();
        if (lastValidationTime.getOrDefault(deviceName, 0L) + VALIDATION_INTERVAL_MS > currentTime) {
            return true; // Assume valid if recently validated
        }

        try {
            // A lightweight way to check might be to send a NO-OP or a benign CC message
            // For example, a Channel Mode Message like All Notes Off on a high, unused channel
            // Or a System Reset message if appropriate, though that can be disruptive.
            // A simple Note On/Off on a high note/channel could also work if it's quickly followed by Note Off.
            // For now, let's use a benign Control Change (e.g., undefined CC 119 on channel 15)
            ShortMessage testMsg = new ShortMessage();
            // Using a common, generally harmless CC like Channel Volume on a high channel (e.g., 15)
            // Or a System Reset message if appropriate, though that can be disruptive.
            // A simple Note On/Off on a high note/channel could also work if it's quickly followed by Note Off.
            // For now, let's use a benign Control Change (e.g., undefined CC 119 on channel 15)
            testMsg.setMessage(ShortMessage.CONTROL_CHANGE, 15, 119, 0); // Channel 15, CC 119, value 0
            receiver.send(testMsg, -1); // -1 timestamp for immediate send
            lastValidationTime.put(deviceName, currentTime);
            logger.trace("Receiver for {} validated successfully.", deviceName);
            return true;
        } catch (Exception e) {
            // IllegalStateException is often thrown if the device/receiver is closed or disconnected
            logger.warn("Receiver validation failed for device {}: {}. Marking as invalid.", deviceName, e.getMessage());
            return false;
        }
    }

    /**
     * Special handling for Gervill synthesizer reconnection
     */
    private Receiver handleGervillReconnection() {
        logger.info("Attempting Gervill reconnection through DeviceManager");
        if (deviceManager.ensureGervillAvailable()) {
            MidiDevice gervillDevice = deviceManager.acquireDevice(SequencerConstants.GERVILL);
            if (gervillDevice != null) {
                try {
                    Receiver gervillReceiver = gervillDevice.getReceiver();
                    if (gervillReceiver != null) {
                        receiverCache.put(SequencerConstants.GERVILL, gervillReceiver);
                        logger.info("Successfully reconnected to Gervill and obtained receiver.");
                        return gervillReceiver;
                    }
                } catch (Exception e) {
                    logger.error("Failed to get receiver from re-acquired Gervill device: {}", e.getMessage());
                    deviceManager.releaseDevice(SequencerConstants.GERVILL); // Release if we can't get receiver
                }
            } else {
                logger.error("Failed to acquire Gervill device even after ensuring availability.");
            }
        } else {
            logger.error("Gervill could not be made available by DeviceManager.");
        }
        return null;
    }

    /**
     * Clear all receivers (useful during shutdown)
     */
    public synchronized void clearAllReceivers() {
        for (String deviceName : receiverCache.keySet()) {
            // Use a copy of the keySet to avoid ConcurrentModificationException if closeReceiver modifies the cache
            new ConcurrentHashMap<>(receiverCache).keySet().forEach(this::closeReceiver);
        }
        receiverCache.clear(); // Should be empty now but clear just in case
        lastValidationTime.clear();
        logger.info("All receivers cleared");
    }
}
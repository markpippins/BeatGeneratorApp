package com.angrysurfer.core.service;

import com.angrysurfer.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages MIDI channel assignment to ensure proper distribution
 */
public class ChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(ChannelManager.class);
    private static ChannelManager instance;
    
    // Track which channels are in use (true = in use)
    private final boolean[] channelsInUse = new boolean[16]; // For all MIDI channels 0-15
    
    private ChannelManager() {
        // Private constructor for singleton
        // Reserve channel 9 (drums) automatically
        channelsInUse[9] = true;
        logger.info("ChannelManager initialized, channel 9 reserved for drums");
    }
    
    public static synchronized ChannelManager getInstance() {
        if (instance == null) {
            instance = new ChannelManager();
        }
        return instance;
    }
    
    /**
     * Get next available melodic channel (0-15, avoiding 9)
     * @return Next available channel, or 0 if all are in use
     */
    public synchronized int getNextAvailableMelodicChannel() {
        for (int i = 0; i < channelsInUse.length; i++) {
            // Skip drum channel (9)
            if (i == Constants.MIDI_DRUM_CHANNEL) continue;
            
            if (!channelsInUse[i]) {
                channelsInUse[i] = true;
                logger.info("Assigning melodic channel {}", i);
                return i;
            }
        }
        
        // If all channels are in use, default to channel 0
        logger.warn("All melodic channels in use, defaulting to channel 0");
        return 0;
    }
    
    /**
     * Get a specific channel for a sequencer
     * @param sequencerIndex The index of the sequencer (0-7)
     * @return Channel assigned to this sequencer (avoiding channel 9)
     */
    public synchronized int getChannelForSequencerIndex(Integer sequencerIndex) {
        // Map sequencer indices 0-7 to channels 0-8,10-15
        int channel = sequencerIndex;
        if (channel >= Constants.MIDI_DRUM_CHANNEL) {
            // Skip drum channel 9
            channel++;
        }
        
        // Ensure channel is within range
        if (channel >= 16) {
            logger.warn("Sequencer index {} is too high, wrapping to available channels", sequencerIndex);
            channel = channel % 16;
            if (channel == Constants.MIDI_DRUM_CHANNEL) channel = 0; // Avoid drum channel 9
        }
        
        // Mark channel as in use
        channelsInUse[channel] = true;
        logger.info("Assigned channel {} to sequencer index {}", channel, sequencerIndex);
        
        return channel;
    }
    
    /**
     * Release a channel when no longer needed
     * @param channel The channel to release
     */
    public synchronized void releaseChannel(int channel) {
        if (channel >= 0 && channel < channelsInUse.length && channel != Constants.MIDI_DRUM_CHANNEL) {
            channelsInUse[channel] = false;
            logger.info("Released melodic channel {}", channel);
        }
    }
    
    /**
     * Check if a channel is already in use
     * @param channel The channel to check
     * @return true if the channel is in use
     */
    public synchronized boolean isChannelInUse(int channel) {
        return channel >= 0 && channel < channelsInUse.length && channelsInUse[channel];
    }
    
    /**
     * Reserve a specific channel if available
     * @param channel The channel to reserve
     * @return true if successful, false if channel already in use
     */
    public synchronized boolean reserveChannel(int channel) {
        if (channel >= 0 && channel < channelsInUse.length && channel != Constants.MIDI_DRUM_CHANNEL && !channelsInUse[channel]) {
            channelsInUse[channel] = true;
            logger.info("Reserved melodic channel {}", channel);
            return true;
        }
        return false;
    }
}
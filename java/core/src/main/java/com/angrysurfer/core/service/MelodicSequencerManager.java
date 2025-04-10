package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.sequencer.MelodicSequencer;

/**
 * Manager for MelodicSequencer instances.
 * Maintains a collection of sequencers and provides methods to create and access them.
 */
public class MelodicSequencerManager {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerManager.class);
    
    private static MelodicSequencerManager instance;
    
    // Store sequencers in an ArrayList for indexed access
    private final List<MelodicSequencer> sequencers;
    
    // Private constructor for singleton pattern
    private MelodicSequencerManager() {
        sequencers = new ArrayList<>();
    }
    
    // Singleton access method
    public static synchronized MelodicSequencerManager getInstance() {
        if (instance == null) {
            instance = new MelodicSequencerManager();
        }
        return instance;
    }
    
    /**
     * Create a new MelodicSequencer with the specified MIDI channel
     * and add it to the manager.
     *
     * @param midiChannel The MIDI channel for the new sequencer
     * @return The newly created MelodicSequencer
     */
    public synchronized MelodicSequencer newSequencer(int midiChannel) {
        MelodicSequencer sequencer = new MelodicSequencer(midiChannel);
        sequencers.add(sequencer);
        logger.info("Created new melodic sequencer with MIDI channel {} (index: {})", 
                midiChannel, sequencers.size() - 1);
        return sequencer;
    }
    
    /**
     * Get a sequencer by its index in the collection.
     *
     * @param index The index of the sequencer
     * @return The MelodicSequencer at the specified index, or null if not found
     */
    public synchronized MelodicSequencer getSequencer(int index) {
        if (index >= 0 && index < sequencers.size()) {
            return sequencers.get(index);
        }
        logger.warn("Sequencer with index {} not found", index);
        return null;
    }
    
    /**
     * Get the number of sequencers currently managed.
     *
     * @return The number of sequencers
     */
    public synchronized int getSequencerCount() {
        return sequencers.size();
    }
    
    /**
     * Get an unmodifiable view of all sequencers.
     *
     * @return Unmodifiable list of all sequencers
     */
    public synchronized List<MelodicSequencer> getAllSequencers() {
        return Collections.unmodifiableList(sequencers);
    }
    
    /**
     * Remove a sequencer from the manager.
     *
     * @param index The index of the sequencer to remove
     * @return true if removed successfully, false otherwise
     */
    public synchronized boolean removeSequencer(int index) {
        if (index >= 0 && index < sequencers.size()) {
            sequencers.remove(index);
            logger.info("Removed melodic sequencer at index {}", index);
            return true;
        }
        logger.warn("Failed to remove sequencer at index {}: not found", index);
        return false;
    }
}
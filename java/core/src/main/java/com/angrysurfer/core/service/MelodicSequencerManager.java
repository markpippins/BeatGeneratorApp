package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
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
//    public synchronized MelodicSequencer newSequencer(int midiChannel) {
//        MelodicSequencer sequencer = new MelodicSequencer(midiChannel);
//        sequencer.setId(sequencers.size() + 1); // Set a unique ID for the sequencer
//        sequencer.setChannel(midiChannel);
//        sequencers.add(sequencer);
//        logger.info("Created new melodic sequencer with MIDI channel {} (index: {})",
//                midiChannel, sequencers.size() - 1);
//
//        // Notify listeners that a sequencer was added
//        CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCER_ADDED, this, sequencer);
//
//        return sequencer;
//    }

    /**
     * Create a new sequencer with specified ID and channel
     * 
     * @param id The sequencer ID
     * @param channel The MIDI channel to use
     * @return A new MelodicSequencer instance
     */
    public MelodicSequencer newSequencer(int id, Integer channel) {
        // Create a new sequencer with the specified ID and channel
        MelodicSequencer sequencer = new MelodicSequencer();
        sequencer.setSequenceData(new MelodicSequenceData());
        // Set id and channel after creation
        sequencer.setId(id);
        sequencer.setChannel(channel);
        // Register with the manager
        sequencers.add(sequencer);
        
        CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCER_ADDED, this, sequencer);

        logger.info("Created new melodic sequencer with ID {} on channel {}", id, channel);
        return sequencer;
    }

    /**
     * Create a new sequencer with specified ID only (for backward compatibility)
     * 
     * @param id The sequencer ID
     * @return A new MelodicSequencer instance
     */
    public MelodicSequencer newSequencer(Integer id) {
        // Use the new method, with a default channel based on ID
        // Avoid channel 9 (drums)
        int channel =  id.intValue() % 16;
        if (channel == 9) channel = 15;
        
        return newSequencer(id, channel);
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
            MelodicSequencer removed = sequencers.remove(index);
            logger.info("Removed melodic sequencer at index {}", index);
            
            // Notify listeners that a sequencer was removed
            CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCER_REMOVED, this, removed);
            
            return true;
        }
        logger.warn("Failed to remove sequencer at index {}: not found", index);
        return false;
    }

    /**
     * Load a sequence into the given sequencer
     *
     * @param id The sequence ID to load
     * @param sequencer The sequencer to load into
     * @return true if successful, false otherwise
     */
    public boolean loadSequence(Long id, MelodicSequencer sequencer) {
        try {
            if (sequencer == null || sequencer.getId() == null) {
                logger.warn("Cannot load sequence - sequencer is null or has no ID");
                return false;
            }
            
            MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(id, sequencer.getId());
            if (data != null) {
                RedisService.getInstance().applyMelodicSequenceToSequencer(data, sequencer);
                logger.info("Loaded melodic sequence {} into sequencer {}", id, sequencer.getId());
                return true;
            }
            logger.warn("Melodic sequence {} not found for sequencer {}", id, sequencer.getId());
            return false;
        } catch (Exception e) {
            logger.error("Error loading melodic sequence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Load the first available pattern into the specified sequencer
     * 
     * @param sequencer The sequencer to update
     * @return The ID of the first pattern, or null if none available
     */
    public Long loadFirstPattern(MelodicSequencer sequencer) {
        if (sequencer == null || sequencer.getId() == null) {
            logger.warn("Cannot load first pattern - sequencer is null or has no ID");
            return null;
        }
        
        // Get the first sequence ID from RedisService
        Long firstId = RedisService.getInstance().getMinimumMelodicSequenceId(sequencer.getId());
        
        if (firstId != null) {
            // Load the sequence data
            MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(firstId, sequencer.getId());
            if (data != null) {
                // Apply to sequencer
                RedisService.getInstance().applyMelodicSequenceToSequencer(data, sequencer);
                logger.info("Loaded first melodic sequence (ID: {}) for sequencer {}", data.getId(), sequencer.getId());
                return data.getId();
            }
        }
        
        // Create a default sequence if none exists
        logger.info("No melodic sequences found for sequencer {}, creating default", sequencer.getId());
        MelodicSequenceData newData = RedisService.getInstance().newMelodicSequence(sequencer.getId());
        RedisService.getInstance().applyMelodicSequenceToSequencer(newData, sequencer);
        return newData.getId();
    }

    /**
     * Check if there are any sequences available for a specific sequencer
     *
     * @param sequencerId The sequencer ID to check for
     * @return true if sequences exist, false otherwise
     */
    public boolean hasSequences(Integer sequencerId) {
        List<Long> ids = RedisService.getInstance().getAllMelodicSequenceIds(sequencerId);
        return ids != null && !ids.isEmpty();
    }

    /**
     * Get the first sequence ID for a specific sequencer
     *
     * @param sequencerId The sequencer ID
     * @return The first ID or null if none
     */
    public Long getFirstSequenceId(Integer sequencerId) {
        return RedisService.getInstance().getMinimumMelodicSequenceId(sequencerId);
    }

    /**
     * Get the last sequence ID for a specific sequencer
     *
     * @param sequencerId The sequencer ID
     * @return The last ID or null if none
     */
    public Long getLastSequenceId(Integer sequencerId) {
        return RedisService.getInstance().getMaximumMelodicSequenceId(sequencerId);
    }

    /**
     * Get the previous sequence ID
     *
     * @param sequencerId The sequencer ID
     * @param currentId The current sequence ID
     * @return The previous ID or null if none
     */
    public Long getPreviousSequenceId(Integer sequencerId, Long currentId) {
        return RedisService.getInstance().getPreviousMelodicSequenceId(sequencerId, currentId);
    }

    /**
     * Get the next sequence ID
     *
     * @param sequencerId The sequencer ID
     * @param currentId The current sequence ID
     * @return The next ID or null if none
     */
    public Long getNextSequenceId(Integer sequencerId, Long currentId) {
        return RedisService.getInstance().getNextMelodicSequenceId(sequencerId, currentId);
    }

    /**
     * Create a new melodic sequence for a specific sequencer
     *
     * @param sequencerId The sequencer ID
     * @return The new sequence data
     */
    public MelodicSequenceData createNewSequence(Integer sequencerId) {
        try {
            return RedisService.getInstance().newMelodicSequence(sequencerId);
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Save a melodic sequence
     *
     * @param sequencer The sequencer containing pattern data
     * @return The ID of the saved sequence
     */
    public Long saveSequence(MelodicSequencer sequencer) {
        try {
            if (sequencer == null || sequencer.getId() == null) {
                logger.warn("Cannot save sequence - sequencer is null or has no ID");
                return null;
            }
            
            RedisService.getInstance().saveMelodicSequence(sequencer);
            logger.info("Saved melodic sequence with ID: {} for sequencer {}", 
                sequencer.getSequenceData().getId(), sequencer.getId());
            return sequencer.getSequenceData().getId();
        } catch (Exception e) {
            logger.error("Error saving melodic sequence: " + e.getMessage(), e);
            return null;
        }
    }

    public List<Long> getAllMelodicSequenceIds(Integer id) {
       return RedisService.getInstance().getAllMelodicSequenceIds(id);
    }

    /**
     * Updates tempo settings on all managed sequencers
     * 
     * @param tempoInBPM The new tempo in BPM
     * @param ticksPerBeat The new ticks per beat value
     */
    public synchronized void updateTempoSettings(float tempoInBPM, int ticksPerBeat) {
        for (MelodicSequencer sequencer : sequencers) {
//            sequencer.set setTempoInBPM(tempoInBPM);
            sequencer.setMasterTempo(ticksPerBeat);
//            setTicksPerBeat(ticksPerBeat);
        }
        logger.info("Updated tempo settings on {} melodic sequencers: {} BPM, {} ticks per beat", 
                sequencers.size(), tempoInBPM, ticksPerBeat);
    }

    // Add a method to get the currently active sequencer
    public MelodicSequencer getActiveSequencer() {
        // If we have sequencers, return the first one (or implement more sophisticated logic)
        if (!sequencers.isEmpty()) {
            return sequencers.get(0);
        }
        return null;
    }
}
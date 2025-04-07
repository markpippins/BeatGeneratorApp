package com.angrysurfer.core.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Manager for drum sequences persistence and operations
 */
public class DrumSequencerManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerManager.class);
    private static DrumSequencerManager instance;
    private final RedisService redisService;
    private final CommandBus commandBus;
    
    private DrumSequencerManager() {
        this.redisService = RedisService.getInstance();
        this.commandBus = CommandBus.getInstance();
        commandBus.register(this);
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized DrumSequencerManager getInstance() {
        if (instance == null) {
            instance = new DrumSequencerManager();
        }
        return instance;
    }
    
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        switch (action.getCommand()) {
            case Commands.SAVE_PATTERN -> {
                if (action.getData() instanceof DrumSequencer sequencer) {
                    saveSequence(sequencer);
                }
            }
            case Commands.LOAD_PATTERN -> {
                if (action.getData() instanceof Long id) {
                    loadSequenceById(id);
                }
            }
        }
    }
    
    /**
     * Save a drum sequence
     * 
     * @param sequencer The sequencer containing pattern data
     * @return The ID of the saved sequence
     */
    public Long saveSequence(DrumSequencer sequencer) {
        try {
            redisService.saveDrumSequence(sequencer);
            logger.info("Saved drum sequence with ID: {}", sequencer.getDrumSequenceId());
            return sequencer.getDrumSequenceId();
        } catch (Exception e) {
            logger.error("Error saving drum sequence: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Load a drum sequence by ID
     * 
     * @param id The sequence ID to load
     * @return The loaded sequence data or null if not found
     */
    public DrumSequenceData loadSequenceById(Long id) {
        try {
            return redisService.findDrumSequenceById(id);
        } catch (Exception e) {
            logger.error("Error loading drum sequence {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Load a sequence into the given sequencer
     * 
     * @param id The sequence ID to load
     * @param sequencer The sequencer to load into
     * @return true if successful, false otherwise
     */
    public boolean loadSequence(Long id, DrumSequencer sequencer) {
        try {
            DrumSequenceData data = loadSequenceById(id);
            if (data != null) {
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                logger.info("Loaded drum sequence {} into sequencer", id);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error applying drum sequence to sequencer: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create a new drum sequence
     * 
     * @return The new sequence data
     */
    public DrumSequenceData createNewSequence() {
        try {
            return redisService.newDrumSequence();
        } catch (Exception e) {
            logger.error("Error creating new drum sequence: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Initialize a new sequence in the given sequencer
     * 
     * @param sequencer The sequencer to initialize
     * @return The ID of the new sequence
     */
    public Long initializeNewSequence(DrumSequencer sequencer) {
        try {
            DrumSequenceData data = createNewSequence();
            if (data != null) {
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                return data.getId();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error initializing new sequence: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get the previous sequence ID
     * 
     * @param currentId The current sequence ID
     * @return The previous ID or null if none
     */
    public Long getPreviousSequenceId(Long currentId) {
        return redisService.getPreviousDrumSequenceId(currentId);
    }
    
    /**
     * Get the next sequence ID
     * 
     * @param currentId The current sequence ID
     * @return The next ID or null if none
     */
    public Long getNextSequenceId(Long currentId) {
        return redisService.getNextDrumSequenceId(currentId);
    }
    
    /**
     * Get the first sequence ID
     * 
     * @return The first ID or null if none
     */
    public Long getFirstSequenceId() {
        return redisService.getMinimumDrumSequenceId();
    }
    
    /**
     * Get the last sequence ID
     * 
     * @return The last ID or null if none
     */
    public Long getLastSequenceId() {
        return redisService.getMaximumDrumSequenceId();
    }
    
    /**
     * Check if there are any sequences available
     * 
     * @return true if sequences exist, false otherwise
     */
    public boolean hasSequences() {
        List<Long> ids = redisService.getAllDrumSequenceIds();
        return ids != null && !ids.isEmpty();
    }
    
    /**
     * Refresh the internal list of sequences from the database
     */
    public void refreshSequenceList() {
        try {
            // Force a refresh of the sequence ID list from Redis
            List<Long> sequenceIds = redisService.getAllDrumSequenceIds();
            logger.info("Refreshed drum sequence list, found {} sequences", 
                       sequenceIds != null ? sequenceIds.size() : 0);
        } catch (Exception e) {
            logger.error("Error refreshing sequence list: " + e.getMessage(), e);
        }
    }
}
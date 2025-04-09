package com.angrysurfer.core.service;

import java.util.ArrayList;
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

import lombok.Getter;

/**
 * Singleton manager for the DrumSequencer instance
 */
@Getter
public class DrumSequencerManager implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerManager.class);
    private static DrumSequencerManager instance;
    private final CommandBus commandBus = CommandBus.getInstance();
    
    // The sequencer is now lazily initialized
    private DrumSequencer sequencer;
    
    // Cache sequence IDs for faster navigation
    private List<Long> cachedSequenceIds = new ArrayList<>();
    private boolean sequenceListDirty = true;
    
    /**
     * Private constructor for singleton pattern
     */
    private DrumSequencerManager() {
        // DON'T create the sequencer in the constructor - breaks circular dependency
        commandBus.register(this);
        logger.info("DrumSequencerManager initialized");
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
    
    /**
     * Get the sequencer instance, lazily initializing it if needed
     */
    public synchronized DrumSequencer getSequencer() {
        if (sequencer == null) {
            logger.info("Creating new DrumSequencer instance");
            sequencer = new DrumSequencer(false); // Don't load sequence in constructor
            
            // Now that sequencer is fully initialized, try to load first sequence
            Long firstId = RedisService.getInstance().getMinimumDrumSequenceId();
            if (firstId != null) {
                loadSequence(firstId);
            }
        }
        return sequencer;
    }
    
    /**
     * Check if any sequences exist in storage
     */
    public boolean hasSequences() {
        return RedisService.getInstance().getMinimumDrumSequenceId() != null;
    }
    
    /**
     * Get the ID of the first available sequence
     */
    public Long getFirstSequenceId() {
        return RedisService.getInstance().getMinimumDrumSequenceId();
    }
    
    /**
     * Get the ID of the last available sequence
     */
    public Long getLastSequenceId() {
        return RedisService.getInstance().getMaximumDrumSequenceId();
    }
    
    /**
     * Get the ID of the previous sequence before the given sequence
     */
    public Long getPreviousSequenceId(long currentId) {
        return RedisService.getInstance().getPreviousDrumSequenceId(currentId);
    }
    
    /**
     * Get the ID of the next sequence after the given sequence
     */
    public Long getNextSequenceId(long currentId) {
        return RedisService.getInstance().getNextDrumSequenceId(currentId);
    }
    
    /**
     * Load a drum sequence into the sequencer
     */
    public void loadSequence(Long sequenceId, DrumSequencer targetSequencer) {
        try {
            DrumSequenceData data = RedisService.getInstance().findDrumSequenceById(sequenceId);
            if (data != null) {
                RedisService.getInstance().applyDrumSequenceToSequencer(data, targetSequencer);
                logger.info("Loaded drum sequence {}", sequenceId);
                
                // Notify listeners
                commandBus.publish(Commands.DRUM_SEQUENCE_LOADED, this, sequenceId);
            } else {
                logger.warn("Drum sequence {} not found", sequenceId);
            }
        } catch (Exception e) {
            logger.error("Error loading drum sequence: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Load a drum sequence into the central sequencer
     */
    public void loadSequence(Long sequenceId) {
        loadSequence(sequenceId, getSequencer());
    }
    
    /**
     * Save the current drum sequence
     */
    public void saveSequence(DrumSequencer targetSequencer) {
        try {
            RedisService.getInstance().saveDrumSequence(targetSequencer);
            sequenceListDirty = true;
            logger.info("Saved drum sequence {}", targetSequencer.getDrumSequenceId());
            
            // Notify listeners
            commandBus.publish(Commands.DRUM_SEQUENCE_SAVED, this, targetSequencer.getDrumSequenceId());
        } catch (Exception e) {
            logger.error("Error saving drum sequence: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Save the central drum sequence
     */
    public void saveSequence() {
        saveSequence(getSequencer());
    }
    
    /**
     * Create a new drum sequence
     */
    public void createNewSequence(DrumSequencer targetSequencer) {
        try {
            DrumSequenceData data = RedisService.getInstance().newDrumSequence();
            RedisService.getInstance().applyDrumSequenceToSequencer(data, targetSequencer);
            sequenceListDirty = true;
            logger.info("Created new drum sequence {}", data.getId());
            
            // Notify listeners
            commandBus.publish(Commands.DRUM_SEQUENCE_CREATED, this, data.getId());
        } catch (Exception e) {
            logger.error("Error creating new drum sequence: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a new drum sequence in the central sequencer
     */
    public void createNewSequence() {
        createNewSequence(getSequencer());
    }
    
    /**
     * Delete a drum sequence
     */
    public void deleteSequence(Long sequenceId) {
        try {
            RedisService.getInstance().deleteDrumSequence(sequenceId);
            sequenceListDirty = true;
            logger.info("Deleted drum sequence {}", sequenceId);
            
            // Notify listeners
            commandBus.publish(Commands.DRUM_SEQUENCE_DELETED, this, sequenceId);
        } catch (Exception e) {
            logger.error("Error deleting drum sequence: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Refresh the list of available drum sequences
     */
    public void refreshSequenceList() {
        if (sequenceListDirty) {
            cachedSequenceIds = RedisService.getInstance().getAllDrumSequenceIds();
            sequenceListDirty = false;
        }
    }
    
    /**
     * Get the list of all drum sequence IDs
     */
    public List<Long> getAllSequenceIds() {
        refreshSequenceList();
        return new ArrayList<>(cachedSequenceIds);
    }
    
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_SAVED, Commands.DRUM_SEQUENCE_CREATED, 
                 Commands.DRUM_SEQUENCE_DELETED -> {
                // Mark sequence list as dirty when changes occur
                sequenceListDirty = true;
            }
            case Commands.TRANSPORT_START -> {
                getSequencer().play();
            }
            case Commands.TRANSPORT_STOP -> {
                getSequencer().stop();
            }
        }
    }
}
package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Manager for DrumSequencer instances.
 * Maintains a collection of sequencers and provides methods to create and access them.
 */
public class DrumSequencerManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerManager.class);

    private static DrumSequencerManager instance;

    private final RedisService redisService;
    private final CommandBus commandBus;

    // Store sequencers in an ArrayList for indexed access
    private final List<DrumSequencer> sequencers;

    private int selectedPadIndex = 0; // Default to first pad

    // Private constructor for singleton pattern
    private DrumSequencerManager() {
        this.redisService = RedisService.getInstance();
        this.commandBus = CommandBus.getInstance();
        this.sequencers = new ArrayList<>();
        commandBus.register(this);
    }

    // Singleton access method
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
            case Commands.SAVE_DRUM_SEQUENCE -> {
                if (action.getData() instanceof DrumSequencer sequencer) {
                    saveSequence(sequencer);
                }
            }
            case Commands.LOAD_DRUM_SEQUENCE -> {
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
            logger.info("Saved drum sequence with ID: {}", sequencer.getData().getId());
            return sequencer.getData().getId();
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

    /**
     * Create a new DrumSequencer and add it to the manager.
     *
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer() {
        DrumSequencer sequencer = new DrumSequencer();
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer (index: {})", sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Create a new DrumSequencer with a note event listener and add it to the manager.
     *
     * @param noteEventListener Callback for when a note should be played
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer(Consumer<NoteEvent> noteEventListener) {
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setNoteEventListener(noteEventListener);
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer with note event listener (index: {})",
                sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Create a new DrumSequencer with note event and step update listeners.
     *
     * @param noteEventListener Callback for when a note should be played
     * @param stepUpdateListener Callback for step updates during playback
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer(
            Consumer<NoteEvent> noteEventListener,
            Consumer<DrumStepUpdateEvent> stepUpdateListener) {  // Changed from StepUpdateEvent to DrumStepUpdateEvent
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setNoteEventListener(noteEventListener);
        sequencer.setStepUpdateListener(stepUpdateListener);
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer with note event and step update listeners (index: {})",
                sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Get a sequencer by its index in the collection.
     *
     * @param index The index of the sequencer
     * @return The DrumSequencer at the specified index, or null if not found
     */
    public synchronized DrumSequencer getSequencer(int index) {
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
    public synchronized List<DrumSequencer> getAllSequencers() {
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
            logger.info("Removed drum sequencer at index {}", index);
            return true;
        }
        logger.warn("Failed to remove sequencer at index {}: not found", index);
        return false;
    }

    /**
     * Load the first available pattern into the specified sequencer
     * 
     * @param sequencer The sequencer to update
     * @return The ID of the first pattern, or null if none available
     */
    public Long loadFirstPattern(DrumSequencer sequencer) {
        // Get the first sequence ID from RedisService
        Long firstId = redisService.getMinimumDrumSequenceId();
        
        if (firstId != null) {
            // Load the sequence data
            DrumSequenceData data = redisService.findDrumSequenceById(firstId);
            if (data != null) {
                // Apply to sequencer
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                logger.info("Loaded first drum sequence (ID: {})", data.getId());
                return data.getId();
            }
        }
        
        // Create a default sequence if none exists
        logger.info("No drum sequences found, creating default");
        DrumSequenceData newData = redisService.newDrumSequence();
        redisService.applyDrumSequenceToSequencer(newData, sequencer);
        return newData.getId();
    }

    /**
     * Load the next pattern for a specific sequencer
     * 
     * @param sequencer The sequencer to update
     * @param currentId The current pattern ID
     * @return The ID of the next pattern, or the same ID if at the end
     */
    public Long loadNextPattern(DrumSequencer sequencer, Long currentId) {
        // Find the next sequence ID after the current one
        Long nextId = redisService.getNextDrumSequenceId(currentId);
        
        if (nextId != null) {
            // Load the sequence data
            DrumSequenceData data = redisService.findDrumSequenceById(nextId);
            if (data != null) {
                // Apply to sequencer
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                logger.info("Loaded next drum sequence (ID: {})", data.getId());
                return data.getId();
            }
        }
        
        // No next sequence, stay on current
        logger.info("No next drum sequence available after ID {}", currentId);
        return currentId;
    }

    /**
     * Load the last pattern for a specific sequencer
     * 
     * @param sequencer The sequencer to update
     * @return The ID of the last pattern, or null if none available
     */
    public Long loadLastPattern(DrumSequencer sequencer) {
        // Get the last sequence ID from RedisService
        Long lastId = redisService.getMaximumDrumSequenceId();
        
        if (lastId != null) {
            // Load the sequence data
            DrumSequenceData data = redisService.findDrumSequenceById(lastId);
            if (data != null) {
                // Apply to sequencer
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                logger.info("Loaded last drum sequence (ID: {})", data.getId());
                return data.getId();
            }
        }
        
        // Create a default sequence if none exists
        logger.info("No drum sequences found, creating default");
        DrumSequenceData newData = redisService.newDrumSequence();
        redisService.applyDrumSequenceToSequencer(newData, sequencer);
        return newData.getId();
    }

    public List<Long> getAllDrumSequenceIds() {
        return redisService.getAllDrumSequenceIds();
    }

    /**
     * Get the currently selected drum pad index
     * 
     * @return The selected pad index
     */
    public synchronized int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    /**
     * Set the currently selected drum pad index
     * 
     * @param index The new selected pad index
     */
    public synchronized void setSelectedPadIndex(int index) {
        // Validate the index first
        if (index >= 0 && index < DrumSequenceData.DRUM_PAD_COUNT) {
            selectedPadIndex = index;
            
            // Also update the selected pad index in sequencers
            for (DrumSequencer seq : sequencers) {
                seq.setSelectedPadIndex(index);
            }
            
            logger.info("Selected pad index set to: {}", index);
        } else {
            logger.warn("Attempted to set invalid pad index: {}", index);
        }
    }

    /**
     * Updates tempo settings on all managed sequencers
     * 
     * @param tempoInBPM The new tempo in BPM
     * @param ticksPerBeat The new ticks per beat value
     */
    public synchronized void updateTempoSettings(float tempoInBPM, int ticksPerBeat) {
        for (DrumSequencer sequencer : sequencers) {
            // sequencer.getData().setTempoInBPM(tempoInBPM);
            // sequencer.getData().setTicksPerBeat(ticksPerBeat);
            sequencer.getData().setMasterTempo(ticksPerBeat);
        }
        logger.info("Updated tempo settings on {} drum sequencers: {} BPM, {} ticks per beat", 
                sequencers.size(), tempoInBPM, ticksPerBeat);
    }

    /**
     * Get the currently active sequencer
     * 
     * @return The active DrumSequencer, or null if none available
     */
    public DrumSequencer getActiveSequencer() {
        // If we have sequencers, return the first one (or implement more sophisticated logic)
        if (!sequencers.isEmpty()) {
            return sequencers.get(0);
        }
        return null;
    }
}
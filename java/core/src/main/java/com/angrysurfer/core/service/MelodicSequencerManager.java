package com.angrysurfer.core.service;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for MelodicSequencer instances.
 * Maintains a collection of sequencers and provides methods to create and
 * access them.
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
     * Create a new sequencer with specified ID and channel
     *
     * @param id The sequencer ID
     * @return A new MelodicSequencer instance
     */
    public MelodicSequencer newSequencer(int id) {
        // Create a new sequencer with the specified ID and channel
        MelodicSequencer sequencer = new MelodicSequencer(id);
        sequencer.setSequenceData(new MelodicSequenceData());
        // sequencer.getPlayer().noteOn(60, 100);
        sequencers.add(sequencer);

        CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCER_ADDED, this, sequencer);

        logger.info("Created new melodic sequencer with ID {} on channel {}", id,
                SequencerConstants.SEQUENCER_CHANNELS[id % SequencerConstants.SEQUENCER_CHANNELS.length]);
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
     * @param currentId   The current sequence ID
     * @return The previous ID or null if none
     */
    public Long getPreviousSequenceId(Integer sequencerId, Long currentId) {
        return RedisService.getInstance().getPreviousMelodicSequenceId(sequencerId, currentId);
    }

    /**
     * Get the next sequence ID
     *
     * @param sequencerId The sequencer ID
     * @param currentId   The current sequence ID
     * @return The next ID or null if none
     */
    public Long getNextSequenceId(Integer sequencerId, Long currentId) {
        return RedisService.getInstance().getNextMelodicSequenceId(sequencerId, currentId);
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

            // Update instrument settings in the sequence data
            sequencer.updateInstrumentSettingsInSequenceData();

            // Now save
            RedisService.getInstance().saveMelodicSequence(sequencer);
            logger.info("Saved melodic sequence with ID: {} for sequencer {}",
                    sequencer.getSequenceData().getId(), sequencer.getId());
            return sequencer.getSequenceData().getId();
        } catch (Exception e) {
            logger.error("Error saving melodic sequence: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<Long> getAllMelodicSequenceIds(Integer id) {
        return RedisService.getInstance().getAllMelodicSequenceIds(id);
    }

    /**
     * Updates tempo settings on all managed sequencers
     *
     * @param tempoInBPM   The new tempo in BPM
     * @param ticksPerBeat The new ticks per beat value
     */
    public synchronized void updateTempoSettings(float tempoInBPM, int ticksPerBeat) {
        for (MelodicSequencer sequencer : sequencers) {
            // sequencer.set setTempoInBPM(tempoInBPM);
            sequencer.setMasterTempo(ticksPerBeat);
            // setTicksPerBeat(ticksPerBeat);
        }
        logger.info("Updated tempo settings on {} melodic sequencers: {} BPM, {} ticks per beat",
                sequencers.size(), tempoInBPM, ticksPerBeat);
    }

    // Add a method to get the currently active sequencer
    public MelodicSequencer getActiveSequencer() {
        // If we have sequencers, return the first one (or implement more sophisticated
        // logic)
        if (!sequencers.isEmpty()) {
            return sequencers.getFirst();
        }
        return null;
    }


}
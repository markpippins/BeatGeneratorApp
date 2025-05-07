package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

import com.angrysurfer.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

/**
 * Core sequencer engine that handles drum pattern sequencing and playback with
 * individual parameters per drum pad.
 */
@Getter
public class DrumSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencer.class);

    // Reference to the data container
    @Setter
    private final DrumSequenceData data;

    private Player[] players;

    // Event handling
    private Consumer<DrumStepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;
    private Consumer<NoteEvent> noteEventPublisher;

    // Add as static fields in both sequencer classes
    private static final ScheduledExecutorService SHARED_NOTE_SCHEDULER = Executors.newScheduledThreadPool(2);

    // Add field to track if we're using internal synth
    private boolean usingInternalSynth = false;
    private InternalSynthManager internalSynthManager = null;

    // 1. Create a reusable message object as a class field
    private final javax.sound.midi.ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    /**
     * Creates a new drum sequencer with per-drum parameters
     */
    public DrumSequencer() {

        players = new Player[DrumSequenceData.DRUM_PAD_COUNT];
        // Initialize the data container
        this.data = new DrumSequenceData();

        // Save reference to synth manager for future use
        internalSynthManager = InternalSynthManager.getInstance();

        // Make sure we have a working synthesizer
        if (!internalSynthManager.checkInternalSynthAvailable()) {
            logger.info("Initializing internal synthesizer for drum sequencer");
            internalSynthManager.initializeSynthesizer();
            usingInternalSynth = true;
        }

        // Initialize players array
        initializePlayers();

        // Register with command bus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Load first saved sequence (if available) instead of default pattern
        loadFirstSequence();
    }

    /**
     * Initialize all player instances with proper MIDI setup
     */
    private void initializePlayers() {
        // First try to get default MIDI device to reuse for all drum pads
        MidiDevice defaultDevice = DeviceManager.getInstance().getDefaultOutputDevice();
        if (defaultDevice == null) {
            logger.warn("No default MIDI output device available, attempting to get Gervill");
            defaultDevice = DeviceManager.getInstance().getMidiDevice("Gervill");
            if (defaultDevice != null && !defaultDevice.isOpen()) {
                try {
                    defaultDevice.open();
                    logger.info("Opened Gervill device for drum sequencer");
                } catch (Exception e) {
                    logger.error("Could not open Gervill device: {}", e.getMessage());
                }
            }
        }

        logger.info("Creating drum players with active connections");
        for (int i = 0; i < DrumSequenceData.DRUM_PAD_COUNT; i++) {
            players[i] = RedisService.getInstance().newStrike();
            players[i].setOwner(this);
            players[i].setChannel(DrumSequenceData.MIDI_DRUM_CHANNEL);
            players[i].setRootNote(DrumSequenceData.MIDI_DRUM_NOTE_OFFSET + i);
            players[i].setName(InternalSynthManager.getInstance().getDrumName(DrumSequenceData.MIDI_DRUM_NOTE_OFFSET + i));

            // Use PlayerManager to initialize the instrument - it will use our enhanced API
            PlayerManager.getInstance().initializeInternalInstrument(players[i], false);

            // Store references in data object

            // Initialize device connections
            initializeDrumPadConnections(i, defaultDevice);

            SessionManager.getInstance().getActiveSession().getPlayers().add(players[i]);
            SessionManager.getInstance().saveActiveSession();

            logger.debug("Initialized drum pad {} with note {}", i, DrumSequenceData.MIDI_DRUM_NOTE_OFFSET + i);
        }
    }

    /**
     * Initialize a drum pad with proper device connections
     * 
     * @param drumIndex     The index of the drum pad to initialize
     * @param defaultDevice The default MIDI device to use if a specific one isn't
     *                      available
     */
    private void initializeDrumPadConnections(int drumIndex, MidiDevice defaultDevice) {
        try {
            Player player = players[drumIndex];
            if (player == null || player.getInstrument() == null) {
                logger.warn("Cannot initialize connections for drum {} - no player or instrument", drumIndex);
                return;
            }

            InstrumentWrapper instrument = player.getInstrument();

            // Set the channel
            instrument.setChannel(DrumSequenceData.MIDI_DRUM_CHANNEL);
            instrument.setReceivedChannels(new Integer[] { DrumSequenceData.MIDI_DRUM_CHANNEL });

            // Try to get a device - first check if instrument already has one specified
            MidiDevice device = null;
            String deviceName = instrument.getDeviceName();

            if (deviceName != null && !deviceName.isEmpty()) {
                try {
                    device = DeviceManager.getInstance().acquireDevice(deviceName);
                    if (device != null && !device.isOpen()) {
                        device.open();
                        logger.debug("Opened specified device {} for drum {}", deviceName, drumIndex);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to open specified device {} for drum {}: {}", 
                        deviceName, drumIndex, e.getMessage());
                    device = null;
                }
            }

            // If no device specified or couldn't open it, try the default
            if (device == null && defaultDevice != null) {
                device = defaultDevice;
                instrument.setDeviceName(defaultDevice.getDeviceInfo().getName());
                logger.debug("Using default device for drum {}: {}", drumIndex,
                        defaultDevice.getDeviceInfo().getName());
            }

            // If still no device, try Gervill specifically
            if (device == null) {
                try {
                    device = DeviceManager.getInstance().getMidiDevice("Gervill");
                    if (device != null) {
                        if (!device.isOpen()) {
                            device.open();
                        }
                        instrument.setDeviceName("Gervill");
                        logger.debug("Using Gervill synthesizer for drum {}", drumIndex);
                    }
                } catch (Exception e) {
                    logger.warn("Could not use Gervill for drum {}: {}", drumIndex, e.getMessage());
                }
            }

            // If we have a device, set it on the instrument
            if (device != null) {
                instrument.setDevice(device);

                // Also ensure the instrument has a receiver
                try {
                    if (instrument.getReceiver() == null) {
                        Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(
                                instrument.getDeviceName(),
                                device);

                        if (receiver != null) {
                            // UPDATED: Now directly set the receiver (no AtomicReference)
                            instrument.setReceiver(receiver);
                            logger.debug("Set receiver for drum {}", drumIndex);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not get receiver for drum {}: {}", drumIndex, e.getMessage());
                }

                // Initialize the instrument's sound
                try {
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                    logger.debug("Applied preset for drum {}", drumIndex);
                } catch (Exception e) {
                    logger.warn("Could not apply preset for drum {}", drumIndex, e.getMessage());
                }
            } else {
                logger.warn("No valid device found for drum {}", drumIndex);
            }
        } catch (Exception e) {
            logger.error("Error initializing connections for drum {}: {}", drumIndex, e.getMessage());
        }
    }

    /**
     * Sets the global swing percentage
     * 
     * @param percentage Value from 50 (no swing) to 75 (maximum swing)
     */
    public void setSwingPercentage(int percentage) {
        // Limit to valid range
        int value = Math.max(DrumSequenceData.MIN_SWING, Math.min(DrumSequenceData.MAX_SWING, percentage));
        data.setSwingPercentage(value);
        logger.info("Swing percentage set to: {}", value);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, -1 // -1 indicates global
                                                                                         // parameter
        );
    }

    public int getSwingPercentage() {
        return data.getSwingPercentage();
    }

    public void setSwingEnabled(boolean enabled) {
        data.setSwingEnabled(enabled);
        logger.info("Swing enabled: {}", enabled);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, -1);
    }

    public boolean isSwingEnabled() {
        return data.isSwingEnabled();
    }

    public int getAbsoluteStep() {
        return data.getAbsoluteStep();
    }

    public void setAbsoluteStep(int step) {
        data.setAbsoluteStep(step);
    }

    /**
     * Load a sequence while preserving playback position if sequencer is running
     * 
     * @param sequenceId The ID of the sequence to load
     * @return true if sequence loaded successfully
     */
    public boolean loadSequence(long sequenceId) {
        // Don't do anything if trying to load the currently active sequence
        if (sequenceId == data.getId()) {
            logger.info("Sequence {} already loaded", sequenceId);
            return true;
        }

        // Store current playback state
        boolean wasPlaying = data.isPlaying();

        // Get the manager
        DrumSequencerManager manager = DrumSequencerManager.getInstance();

        // Load the sequence
        boolean loaded = manager.loadSequence(sequenceId, this);

        if (loaded) {
            logger.info("Loaded drum sequence: {}", sequenceId);
            data.setId(sequenceId);

            // Immediately update visual indicators without resetting
            if (stepUpdateListener != null) {
                for (int drumIndex = 0; drumIndex < DrumSequenceData.DRUM_PAD_COUNT; drumIndex++) {
                    // Force an update with the current positions
                    stepUpdateListener
                            .accept(new DrumStepUpdateEvent(drumIndex, -1, data.getCurrentStep()[drumIndex]));
                }
            }

            // Publish event to notify UI components
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_LOADED, this, data.getId());

            // Preserve playing state (don't stop if we were playing)
            data.setPlaying(wasPlaying);

            return true;
        } else {
            logger.warn("Failed to load drum sequence {}", sequenceId);
            return false;
        }
    }

    /**
     * Load the first available sequence
     */
    private void loadFirstSequence() {
        try {
            // Get the manager
            DrumSequencerManager manager = DrumSequencerManager.getInstance();

            // Get the first sequence ID
            Long firstId = manager.getFirstSequenceId();

            if (firstId != null) {
                // Use our loadSequence method
                loadSequence(firstId);
            } else {
                logger.info("No saved drum sequences found, using empty pattern");
            }
        } catch (Exception e) {
            logger.error("Error loading initial drum sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset the sequencer state
     * 
     * @param preservePositions whether to preserve current positions
     */
    public void reset(boolean preservePositions) {
        // Use the data object's reset method
        data.reset(preservePositions);

        // Force the sequencer to generate an event to update visual indicators
        if (stepUpdateListener != null) {
            for (int drumIndex = 0; drumIndex < DrumSequenceData.DRUM_PAD_COUNT; drumIndex++) {
                stepUpdateListener
                        .accept(new DrumStepUpdateEvent(drumIndex, -1, data.getCurrentStep()[drumIndex]));
            }
        }

        logger.debug("Sequencer reset - preservePositions={}", preservePositions);
    }

    /**
     * Reset with default behavior
     */
    public void reset() {
        reset(false);
    }

    /**
     * Process a timing tick
     *
     * @param tick The current tick count
     */
    public void processTick(long tick) {
        if (!data.isPlaying()) {
            return;
        }

        data.setTickCounter(tick);

        // Use the standard timing to determine global step changes
        int standardTicksPerStep = TimingDivision.NORMAL.getTicksPerBeat();

        // Update absoluteStep based on the tick count - for the global timing
        if (tick % standardTicksPerStep == 0) {
            // Increment the absoluteStep (cycle through the maximum pattern length)
            int newStep = (data.getAbsoluteStep() + 1) % data.getMaxPatternLength();
            data.setAbsoluteStep(newStep);
            logger.debug("Absolute step: {}", newStep);
        }

        // Reset pattern completion flag at the start of processing
        data.setPatternJustCompleted(false);

        // Process each drum separately
        for (int drumIndex = 0; drumIndex < DrumSequenceData.DRUM_PAD_COUNT; drumIndex++) {
            // Skip if no Player configured
            if (players[drumIndex] == null) {
                continue;
            }

            // Use each drum's timing division
            TimingDivision division = data.getTimingDivisions()[drumIndex];
            int drumTicksPerStep = division.getTicksPerBeat();

            // Make sure we have a valid minimum value
            if (drumTicksPerStep <= 0) {
                drumTicksPerStep = 24; // Emergency fallback
            }

            // Use modulo for stability
            if (tick % drumTicksPerStep == 0) {
                // Reset pattern completion flag if we're looping
                if (data.getPatternCompleted()[drumIndex] && data.getLoopingFlags()[drumIndex]) {
                    data.getPatternCompleted()[drumIndex] = false;
                }

                // Skip if pattern is completed and not looping
                if (data.getPatternCompleted()[drumIndex] && !data.getLoopingFlags()[drumIndex]) {
                    continue;
                }

                // Process the current step for this drum
                processStep(drumIndex);

                logger.debug("Drum {} step processed at tick {} (timing: {})", drumIndex, tick,
                        division.getDisplayName());
            }
        }

        // Check for pattern completion
        if (data.areAllPatternsCompleted() && data.getNextPatternId() != null) {
            data.setPatternJustCompleted(true);

            // Switch to next pattern
            Long currentId = data.getId();
            loadSequence(data.getNextPatternId());

            // Notify about pattern switch
            CommandBus.getInstance().publish(Commands.DRUM_PATTERN_SWITCHED, this,
                    new PatternSwitchEvent(currentId, data.getNextPatternId()));

            // Clear the next pattern ID (one-shot behavior)
            data.setNextPatternId(null);
        }
    }

    /**
     * Set the next pattern to automatically switch to when the current pattern
     * completes
     * 
     * @param patternId The ID of the next pattern, or null to disable automatic
     *                  switching
     */
    public void setNextPatternId(Long patternId) {
        data.setNextPatternId(patternId);
        logger.info("Set next drum pattern ID: {}", patternId);
    }

    /**
     * Get the next pattern ID
     * 
     * @return The next pattern ID or null if no pattern is queued
     */
    public Long getNextPatternId() {
        return data.getNextPatternId();
    }

    /**
     * Process the current step for a drum
     *
     * @param drumIndex The drum to process
     */
    private void processStep(int drumIndex) {
        // Get the current step for this drum
        int step = data.getCurrentStep()[drumIndex];

        // Notify listeners of step update BEFORE playing the sound
        if (stepUpdateListener != null) {
            DrumStepUpdateEvent event = new DrumStepUpdateEvent(drumIndex, getPreviousStep(drumIndex), step);
            stepUpdateListener.accept(event);
        }

        // Trigger the drum step
        triggerDrumStep(drumIndex, step);

        // Calculate next step - store previous step for UI updates
        data.calculateNextStep(drumIndex);
    }

    /**
     * Calculate the previous step based on current direction
     */
    private int getPreviousStep(int drumIndex) {
        Direction direction = data.getDirections()[drumIndex];
        int currentPos = data.getCurrentStep()[drumIndex];
        int length = data.getPatternLengths()[drumIndex];

        return switch (direction) {
            case FORWARD -> (currentPos + length - 1) % length;
            case BACKWARD -> (currentPos + 1) % length;
            case BOUNCE -> {
                // For bounce, it depends on the current bounce direction
                if (data.getBounceDirections()[drumIndex] > 0) {
                    yield currentPos > 0 ? currentPos - 1 : 0;
                } else {
                    yield currentPos < length - 1 ? currentPos + 1 : length - 1;
                }
            }
            case RANDOM -> currentPos; // For random, just use current position
        };
    }

    /**
     * Trigger a drum step with per-step parameters
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     */
    private void triggerDrumStep(int drumIndex, int stepIndex) {
        // Skip if step is inactive
        if (!data.isStepActive(drumIndex, stepIndex)) {
            return;
        }

        // Get all step parameters
        int velocity = data.getStepVelocities()[drumIndex][stepIndex];
        int probability = data.getStepProbabilities()[drumIndex][stepIndex];
        int decay = data.getStepDecays()[drumIndex][stepIndex];
        int nudge = data.getStepNudges()[drumIndex][stepIndex];

        // Check probability
        if (Math.random() * 100 >= probability) {
            return;
        }

        // Apply velocity scaling
        int finalVelocity = (int) (velocity * (data.getVelocities()[drumIndex] / 127.0));
        if (finalVelocity <= 0) {
            return;
        }

        // Get player
        Player player = players[drumIndex];
        if (player == null) {
            return;
        }

        // Set instrument if needed
        if (player.getInstrument() == null && player.getInstrumentId() != null) {
            player.setInstrument(InstrumentManager.getInstance().getInstrumentById(player.getInstrumentId()));
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(DrumSequenceData.MIDI_DRUM_CHANNEL);
                player.getInstrument().setReceivedChannels(new Integer[] { DrumSequenceData.MIDI_DRUM_CHANNEL });
            }
        }

        // Process and send effects before playing the note
        processEffects(drumIndex, stepIndex, player);

        // Apply swing if needed
        if (data.isSwingEnabled() && stepIndex % 2 == 1) {
            nudge += calculateSwingAmount(drumIndex);
        }

        // Now trigger the note
        final int finalNoteNumber = player.getRootNote();
        final int finalVelocityCopy = finalVelocity;
        final int finalDecay = decay;
        final int finalDrumIndex = drumIndex;

        if (nudge > 0) {
            // Delayed note
            SHARED_NOTE_SCHEDULER.schedule(() -> {
                player.noteOn(finalNoteNumber, finalVelocityCopy, finalDecay);
                publishNoteEvent(finalDrumIndex, finalVelocityCopy, finalDecay);
            }, nudge, TimeUnit.MILLISECONDS);
        } else {
            // Immediate note
            player.noteOn(finalNoteNumber, finalVelocity, decay);
            publishNoteEvent(drumIndex, finalVelocity, decay);
        }
    }

    /**
     * Publish a note event to listeners
     */
    private void publishNoteEvent(int drumIndex, int velocity, int durationMs) {
        if (noteEventPublisher != null) {
            // Convert drum index back to MIDI note (36=kick, etc.)
            int midiNote = drumIndex + DrumSequenceData.MIDI_DRUM_NOTE_OFFSET;
            NoteEvent event = new NoteEvent(midiNote, velocity, durationMs);
            noteEventPublisher.accept(event);
        }
    }

    /**
     * Calculate swing amount in milliseconds based on current tempo and timing
     * division
     */
    private int calculateSwingAmount(int drumIndex) {
        // Get session BPM
        float bpm = SessionManager.getInstance().getActiveSession().getTempoInBPM();
        if (bpm <= 0)
            bpm = 120; // Default fallback

        // Calculate step duration in milliseconds
        TimingDivision division = data.getTimingDivisions()[drumIndex];
        float stepDurationMs = 60000f / bpm; // Duration of quarter note in ms

        // Adjust for timing division based on actual enum values
        switch (division) {
            case NORMAL -> stepDurationMs *= 1; // No change for normal timing
            case DOUBLE -> stepDurationMs /= 2; // Double time (faster)
            case HALF -> stepDurationMs *= 2; // Half time (slower)
            case QUARTER -> stepDurationMs *= 4; // Quarter time (very slow)
            case TRIPLET -> stepDurationMs *= 2.0f / 3.0f; // Triplet feel
            case QUARTER_TRIPLET -> stepDurationMs *= 4.0f / 3.0f; // Quarter note triplets
            case EIGHTH_TRIPLET -> stepDurationMs *= 1.0f / 3.0f; // Eighth note triplets
            case SIXTEENTH -> stepDurationMs *= 1.0f / 4.0f; // Sixteenth notes
            case SIXTEENTH_TRIPLET -> stepDurationMs *= 1.0f / 6.0f; // Sixteenth note triplets
            case BEBOP -> stepDurationMs *= 1; // Same as normal for swing calculations
            case FIVE_FOUR -> stepDurationMs *= 5.0f / 4.0f; // 5/4 time
            case SEVEN_EIGHT -> stepDurationMs *= 7.0f / 8.0f; // 7/8 time
            case NINE_EIGHT -> stepDurationMs *= 9.0f / 8.0f; // 9/8 time
            case TWELVE_EIGHT -> stepDurationMs *= 12.0f / 8.0f; // 12/8 time
            case SIX_FOUR -> stepDurationMs *= 6.0f / 4.0f; // 6/4 time
        }

        // Calculate swing percentage (convert from 50-75% to 0-25%)
        float swingFactor = (data.getSwingPercentage() - 50) / 100f;

        // Return swing amount in milliseconds
        return (int) (stepDurationMs * swingFactor);
    }

    /**
     * Calculate ticks per step based on timing division
     */
    private int calculateTicksPerStep(TimingDivision timing) {
        // Add safety check to prevent division by zero
        int masterTempo = data.getMasterTempo();
        if (masterTempo <= 0) {
            logger.warn("Invalid masterTempo value ({}), using default of {}", masterTempo,
                    DrumSequenceData.DEFAULT_MASTER_TEMPO);
            masterTempo = DrumSequenceData.DEFAULT_MASTER_TEMPO; // Emergency fallback
        }

        double ticksPerBeat = timing.getTicksPerBeat();
        if (ticksPerBeat <= 0) {
            logger.warn("Invalid ticksPerBeat value ({}), using default of {}", ticksPerBeat,
                    DrumSequenceData.DEFAULT_TICKS_PER_BEAT);
            ticksPerBeat = DrumSequenceData.DEFAULT_TICKS_PER_BEAT; // Emergency fallback
        }

        // Simplified calculation that works consistently
        int result = (int) (masterTempo / (ticksPerBeat / 24.0));

        // Add safety check for the final result
        if (result <= 0) {
            logger.warn("Calculated invalid ticksPerStep ({}), using default of 24", result);
            result = 24; // Emergency fallback for extreme values
        }

        return result;
    }

    /**
     * Update master tempo from session
     */
    public void updateMasterTempo(int sessionTicksPerBeat) {
        data.setMasterTempo(sessionTicksPerBeat);
        logger.info("Updated master tempo to {}", sessionTicksPerBeat);

        // Recalculate all next step timings based on new tempo
        for (int drumIndex = 0; drumIndex < DrumSequenceData.DRUM_PAD_COUNT; drumIndex++) {
            if (data.getTimingDivisions()[drumIndex] != null) {
                int calculatedTicksPerStep = calculateTicksPerStep(data.getTimingDivisions()[drumIndex]);
                data.getNextStepTick()[drumIndex] = data.getTickCounter() + calculatedTicksPerStep;
            }
        }
    }

    /**
     * Start playback
     */
    public void play() {
        data.setPlaying(true);

        // Reset step positions to ensure consistent playback
        for (int i = 0; i < DrumSequenceData.DRUM_PAD_COUNT; i++) {
            // Set all next step ticks to the current tick to trigger immediately
            data.getNextStepTick()[i] = data.getTickCounter();

            // Reset pattern completion flags
            data.getPatternCompleted()[i] = false;
        }

        logger.info("DrumSequencer playback started at tick {}", data.getTickCounter());
    }

    /**
     * Start playback with reset
     */
    public void start() {
        if (!data.isPlaying()) {
            reset();
            data.setPlaying(true);
        }
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (data.isPlaying()) {
            data.setPlaying(false);
            reset();
        }
    }

    /**
     * Get whether the sequencer is currently playing
     */
    public boolean isPlaying() {
        return data.isPlaying();
    }

    /**
     * Toggle a step on/off for a specific drum and step
     * @param drumIndex The drum pad index
     * @param stepIndex The step index to toggle
     * @return The new state of the step (true=active, false=inactive)
     */
    public boolean toggleStep(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT &&
                stepIndex >= 0 && stepIndex < data.getMaxPatternLength()) {

            // Toggle the step
            boolean[][] patterns = data.getPatterns();
            patterns[drumIndex][stepIndex] = !patterns[drumIndex][stepIndex];
            boolean newState = patterns[drumIndex][stepIndex];

            // Create event data - FIX: Convert boolean to int
            DrumStepUpdateEvent event = new DrumStepUpdateEvent(
                    drumIndex,
                    stepIndex,
                    newState ? 1 : 0  // Convert boolean to int
            );

            // If we have a direct listener, notify it
            if (stepUpdateListener != null) {
                stepUpdateListener.accept(event);
            }

            // Also publish on command bus for other listeners
            CommandBus.getInstance().publish(Commands.DRUM_STEP_UPDATED, this, event);

            logger.debug("Toggled step for drum {} at position {} to {}",
                    drumIndex, stepIndex, newState);

            return newState;
        }

        logger.warn("Invalid drum/step indices: {}/{}", drumIndex, stepIndex);
        return false;
    }

    /**
     * Get the pattern length for a drum
     */
    public int getPatternLength(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getPatternLength", drumIndex);
            return data.getDefaultPatternLength();
        }
        return data.getPatternLengths()[drumIndex];
    }

    /**
     * Set the pattern length for a drum
     */
    public void setPatternLength(int drumIndex, int length) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && length > 0
                && length <= data.getMaxPatternLength()) {
            logger.info("Setting pattern length for drum {} to {}", drumIndex, length);
            data.getPatternLengths()[drumIndex] = length;

            // Ensure the current step is within bounds
            if (data.getCurrentStep()[drumIndex] >= length) {
                data.getCurrentStep()[drumIndex] = 0;
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);

            // Reset this drum if we're changing length while playing
            if (data.isPlaying()) {
                resetDrum(drumIndex);
            }
        } else {
            logger.warn("Invalid pattern length: {} for drum {} (must be 1-{})", length, drumIndex,
                    data.getMaxPatternLength());
        }
    }

    /**
     * Reset a drum to the beginning of its pattern
     */
    private void resetDrum(int drumIndex) {
        // Reset current step based on playback direction
        switch (data.getDirections()[drumIndex]) {
            case FORWARD:
                data.getCurrentStep()[drumIndex] = 0;
                break;
            case BACKWARD:
                data.getCurrentStep()[drumIndex] = data.getPatternLengths()[drumIndex] - 1;
                break;
            case BOUNCE:
                data.getBounceDirections()[drumIndex] = 1; // Start forward
                data.getCurrentStep()[drumIndex] = 0;
                break;
            case RANDOM:
                data.getCurrentStep()[drumIndex] = (int) (Math.random() * data.getPatternLengths()[drumIndex]);
                break;
        }

        // Reset pattern completion flag
        data.getPatternCompleted()[drumIndex] = false;

        // If playing, also reset the next step time
        if (data.isPlaying()) {
            // Calculate appropriate step timing based on timing division
            int stepTiming = calculateTicksPerStep(data.getTimingDivisions()[drumIndex]);
            data.getNextStepTick()[drumIndex] = data.getTickCounter() + stepTiming;
        }
    }

    /**
     * Get the direction for a drum
     */
    public Direction getDirection(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getDirection", drumIndex);
            return Direction.FORWARD;
        }
        return data.getDirections()[drumIndex];
    }

    /**
     * Set the direction for a drum
     */
    public void setDirection(int drumIndex, Direction direction) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            data.getDirections()[drumIndex] = direction;

            // If playing in bounce mode, make sure bounce direction is set correctly
            if (direction == Direction.BOUNCE) {
                // Initialize bounce direction if needed
                if (data.getBounceDirections()[drumIndex] == 0) {
                    data.getBounceDirections()[drumIndex] = 1; // Start forward
                }
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the timing division for a drum
     */
    public TimingDivision getTimingDivision(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getTimingDivision", drumIndex);
            return TimingDivision.NORMAL;
        }
        return data.getTimingDivisions()[drumIndex];
    }

    /**
     * Set the timing division for a drum
     */
    public void setTimingDivision(int drumIndex, TimingDivision division) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            data.getTimingDivisions()[drumIndex] = division;

            // Reset the drum's next step time to apply the new timing
            if (data.isPlaying()) {
                resetDrum(drumIndex);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get whether a drum is looping
     */
    public boolean isLooping(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for isLooping", drumIndex);
            return true;
        }
        return data.getLoopingFlags()[drumIndex];
    }

    /**
     * Set whether a drum should loop
     */
    public void setLooping(int drumIndex, boolean loop) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            data.getLoopingFlags()[drumIndex] = loop;

            // If we're re-enabling looping for a stopped pattern, reset it
            if (loop && data.getPatternCompleted()[drumIndex] && data.isPlaying()) {
                data.getPatternCompleted()[drumIndex] = false;
                resetDrum(drumIndex);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the velocity for a drum
     */
    public int getVelocity(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            return data.getVelocities()[drumIndex];
        }
        return DrumSequenceData.DEFAULT_VELOCITY;
    }

    /**
     * Set the velocity for a drum
     */
    public void setVelocity(int drumIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            // Constrain to valid MIDI range
            velocity = Math.max(0, Math.min(DrumSequenceData.MAX_MIDI_VELOCITY, velocity));
            data.getVelocities()[drumIndex] = velocity;

            // If we have a Player object for this drum, update its level
            Player player = getPlayer(drumIndex);
            if (player != null) {
                player.setLevel(velocity);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the velocity for a specific step
     */
    public int getStepVelocity(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepVelocities()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the velocity for a specific step
     */
    public void setStepVelocity(int drumIndex, int stepIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepVelocities()[drumIndex][stepIndex] = velocity;
        }
    }

    /**
     * Get the decay for a specific step
     */
    public int getStepDecay(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepDecays()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the decay for a specific step
     */
    public void setStepDecay(int drumIndex, int stepIndex, int decay) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepDecays()[drumIndex][stepIndex] = decay;
        }
    }

    /**
     * Get the probability for a specific step
     */
    public int getStepProbability(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepProbabilities()[drumIndex][stepIndex];
        }
        return DrumSequenceData.DEFAULT_PROBABILITY;
    }

    /**
     * Set the probability for a specific step
     */
    public void setStepProbability(int drumIndex, int stepIndex, int probability) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            // Clamp value between 0-100
            data.getStepProbabilities()[drumIndex][stepIndex] = Math.max(0, Math.min(100, probability));
        }
    }

    /**
     * Get the nudge for a specific step
     */
    public int getStepNudge(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepNudges()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the nudge for a specific step
     */
    public void setStepNudge(int drumIndex, int stepIndex, int nudge) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepNudges()[drumIndex][stepIndex] = nudge;
        }
    }

    /**
     * Get the pan position for a specific step
     */
    public int getStepPan(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepPans()[drumIndex][stepIndex];
        }
        return DrumSequenceData.DEFAULT_PAN;
    }

    /**
     * Set the pan position for a specific step
     */
    public void setStepPan(int drumIndex, int stepIndex, int pan) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepPans()[drumIndex][stepIndex] = Math.max(0, Math.min(127, pan));
        }
    }

    /**
     * Get the chorus amount for a specific step
     */
    public int getStepChorus(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepChorus()[drumIndex][stepIndex];
        }
        return DrumSequenceData.DEFAULT_CHORUS;
    }

    /**
     * Set the chorus amount for a specific step
     */
    public void setStepChorus(int drumIndex, int stepIndex, int chorus) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepChorus()[drumIndex][stepIndex] = Math.max(0, Math.min(100, chorus));
        }
    }

    /**
     * Get the reverb amount for a specific step
     */
    public int getStepReverb(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            return data.getStepReverb()[drumIndex][stepIndex];
        }
        return DrumSequenceData.DEFAULT_REVERB;
    }

    /**
     * Set the reverb amount for a specific step
     */
    public void setStepReverb(int drumIndex, int stepIndex, int reverb) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < data.getMaxPatternLength()) {
            data.getStepReverb()[drumIndex][stepIndex] = Math.max(0, Math.min(100, reverb));
        }
    }

    /**
     * Get the currently selected drum pad index
     */
    public int getSelectedPadIndex() {
        return data.getSelectedPadIndex();
    }

    /**
     * Set the currently selected drum pad index
     */
    public void setSelectedPadIndex(int index) {
        if (index >= 0 && index < DrumSequenceData.DRUM_PAD_COUNT) {
            // Store old selection
            int oldSelection = data.getSelectedPadIndex();

            // Set new selection
            data.setSelectedPadIndex(index);

            // Notify listeners of selection change
            CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED, this,
                    new DrumPadSelectionEvent(oldSelection, index));

            logger.info("Selected drum pad index updated to: {}", index);
        } else {
            logger.warn("Invalid drum pad index: {}", index);
        }
    }

    /**
     * Set pattern length for the currently selected drum pad
     */
    public void setPatternLength(int length) {
        setPatternLength(data.getSelectedPadIndex(), length);
    }

    /**
     * Set direction for the currently selected drum pad
     */
    public void setDirection(Direction direction) {
        setDirection(data.getSelectedPadIndex(), direction);
    }

    /**
     * Set timing division for the currently selected drum pad
     */
    public void setTimingDivision(TimingDivision division) {
        setTimingDivision(data.getSelectedPadIndex(), division);
    }

    /**
     * Set looping for the currently selected drum pad
     */
    public void setLooping(boolean loop) {
        setLooping(data.getSelectedPadIndex(), loop);
    }

    /**
     * Get pattern length for the currently selected drum pad
     */
    public int getSelectPatternLength() {
        return getPatternLength(data.getSelectedPadIndex());
    }

    /**
     * Get the default pattern length
     */
    public int getDefaultPatternLength() {
        return data.getDefaultPatternLength();
    }

    /**
     * Get the maximum pattern length
     */
    public int getMaxPatternLength() {
        return data.getMaxPatternLength();
    }

    /**
     * Set default pattern length
     */
    public void setDefaultPatternLength(int length) {
        data.setDefaultPatternLength(length);
    }

    /**
     * Set maximum pattern length
     */
    public void setMaxPatternLength(int length) {
        if (length >= data.getDefaultPatternLength()) {
            data.setMaxPatternLength(length);
        }
    }

    /**
     * Select a drum pad and notify listeners
     */
    public void selectDrumPad(int padIndex) {
        setSelectedPadIndex(padIndex);
    }

    /**
     * Get the Player object for a specific drum pad
     */
    public Player getPlayer(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            return players[drumIndex];
        }
        return null;
    }

    /**
     * Set the Player object for a specific drum pad
     */
    public void setPlayer(int drumIndex, Player player) {
        if (drumIndex >= 0 && drumIndex < DrumSequenceData.DRUM_PAD_COUNT) {
            players[drumIndex] = player;
        }
    }

    /**
     * Check if a specific step is active for a drum
     */
    public boolean isStepActive(int drumIndex, int stepIndex) {
        return data.isStepActive(drumIndex, stepIndex);
    }

    /**
     * Clear all patterns
     */
    public void clearPattern() {
        data.clearPatterns();
        logger.info("All patterns cleared");

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, null);
    }

    /**
     * Generate a simple pattern for the specified drum
     */
    public void generatePattern(int density) {
        // Generate pattern for selected drum pad
        int drumIndex = data.getSelectedPadIndex();
        data.generatePattern(drumIndex, density);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);

        logger.info("Generated pattern for drum {}", drumIndex);
    }

    /**
     * Initialize with a simple default pattern
     */
    private void initializeDefaultPattern() {
        // Set up a basic kick/snare pattern on first two drums
        if (DrumSequenceData.DRUM_PAD_COUNT > 0) {
            // Kick on quarters (every 4 steps)
            for (int i = 0; i < 16; i += 4) {
                data.getPatterns()[0][i] = true;
            }
        }

        if (DrumSequenceData.DRUM_PAD_COUNT > 1) {
            // Snare on 5 and 13
            data.getPatterns()[1][4] = true;
            data.getPatterns()[1][12] = true;
        }

        if (DrumSequenceData.DRUM_PAD_COUNT > 2) {
            // Hi-hat on even steps
            for (int i = 0; i < 16; i += 2) {
                data.getPatterns()[2][i] = true;
            }
        }

        logger.info("Default pattern initialized");
    }

    /**
     * Push the pattern forward by one step for the selected drum pad
     */
    public void pushForward() {
        int drumIndex = data.getSelectedPadIndex();
        data.pushPatternForward(drumIndex);
        logger.info("Pushed pattern forward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, null);
    }

    /**
     * Pull the pattern backward by one step for the selected drum pad
     */
    public void pullBackward() {
        int drumIndex = data.getSelectedPadIndex();
        data.pullPatternBackward(drumIndex);
        logger.info("Pulled pattern backward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, null);
    }

    /**
     * Set a step update listener
     */
    public void setStepUpdateListener(Consumer<DrumStepUpdateEvent> listener) {
        this.stepUpdateListener = listener;
    }

    /**
     * Set a note event listener
     */
    public void setNoteEventListener(Consumer<NoteEvent> listener) {
        this.noteEventListener = listener;
    }

    /**
     * Set a note event publisher
     */
    public void setNoteEventPublisher(Consumer<NoteEvent> publisher) {
        this.noteEventPublisher = publisher;
    }

    /**
     * Required by IBusListener interface
     */
    @Override
    public void onAction(Command cmd) {
        if (cmd == null || cmd.getCommand() == null) {
            return;
        }

        // Handle repair connections command
        if (Commands.REPAIR_MIDI_CONNECTIONS.equals(cmd.getCommand())) {
            repairMidiConnections();
            return;
        }

        // Process timing updates
        if (Commands.TIMING_UPDATE.equals(cmd.getCommand()) && cmd.getData() instanceof TimingUpdate) {
            processTick(((TimingUpdate) cmd.getData()).tickCount());
            return; // Process timing updates immediately and return
        }

        // Handle other commands
        switch (cmd.getCommand()) {
            case Commands.TRANSPORT_START -> {
                data.setPlaying(true);
                data.setMasterTempo(SessionManager.getInstance().getActiveSession().getTicksPerBeat());
                reset();
                ensureDeviceConnections();
            }
            case Commands.TRANSPORT_STOP -> {
                data.setPlaying(false);
                reset();
            }
            case Commands.UPDATE_TEMPO -> {
                if (cmd.getData() instanceof Integer ticksPerBeat) {
                    updateMasterTempo(ticksPerBeat);
                } else if (cmd.getData() instanceof Float) {
                    // If BPM is sent instead, get ticksPerBeat from session
                    int tpb = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                    updateMasterTempo(tpb);
                }
            }
        }
    }

    /**
     * Play a drum note using the Player for the specified drum pad
     */
    public void playDrumNote(int drumIndex, int velocity) {
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index: {}", drumIndex);
            return;
        }

        Player player = players[drumIndex];
        if (player == null) {
            logger.debug("No Player assigned to drum pad {}", drumIndex);
            return;
        }

        if (player.getInstrument() == null && player.getInstrumentId() != null) {
            player.setInstrument(InstrumentManager.getInstance().getInstrumentById(player.getInstrumentId()));
            player.setChannel(9);
        }

        if (player.getInstrument() == null) {
            logger.debug("No instrument assigned to Player for drum pad {}", drumIndex);
            return;
        }

        int noteNumber = player.getRootNote();
        try {
            // Use the Player's instrument directly to play the note
            player.noteOn(noteNumber, velocity);

            // Still notify listeners for UI updates
            if (noteEventListener != null) {
                noteEventListener.accept(new NoteEvent(noteNumber, velocity, 0));
            }
        } catch (Exception e) {
            logger.error("Error playing drum note: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a MIDI CC message to the instrument's device if it exists
     */
    private boolean sendMidiCC(Player player, int cc, int value) {
        if (player == null || player.getInstrument() == null) {
            return false;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();
            int channel = player.getChannel();

            // Reuse message object instead of creating new ones
            synchronized (reuseableMessage) {
                reuseableMessage.setMessage(javax.sound.midi.ShortMessage.CONTROL_CHANGE, channel, cc, value);
                instrument.getReceiver().send(reuseableMessage, -1);
            }
            return true;
        } catch (Exception e) {
            // Log only at debug level to reduce logging overhead
            logger.debug("Error sending MIDI CC: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process effects for a single step
     */
    private void processEffects(int drumIndex, int stepIndex, Player player) {
        // Skip if the step is inactive or player has no instrument
        if (!data.isStepActive(drumIndex, stepIndex) || player == null || player.getInstrument() == null) {
            return;
        }

        try {
            // Get current effect values
            int pan = data.getStepPans()[drumIndex][stepIndex];
            int reverb = data.getStepReverb()[drumIndex][stepIndex];
            int chorus = data.getStepChorus()[drumIndex][stepIndex];
            int decay = data.getStepDecays()[drumIndex][stepIndex];

            // Count how many effects need to be sent
            int effectCount = 0;

            // Only add effects that have changed
            if (pan != data.getLastPanValues()[drumIndex][stepIndex]) {
                data.getEffectControllers()[effectCount] = DrumSequenceData.CC_PAN;
                data.getEffectValues()[effectCount] = pan;
                data.getLastPanValues()[drumIndex][stepIndex] = pan;
                effectCount++;
            }

            if (reverb != data.getLastReverbValues()[drumIndex][stepIndex]) {
                data.getEffectControllers()[effectCount] = DrumSequenceData.CC_REVERB;
                data.getEffectValues()[effectCount] = reverb;
                data.getLastReverbValues()[drumIndex][stepIndex] = reverb;
                effectCount++;
            }

            if (chorus != data.getLastChorusValues()[drumIndex][stepIndex]) {
                data.getEffectControllers()[effectCount] = DrumSequenceData.CC_CHORUS;
                data.getEffectValues()[effectCount] = chorus;
                data.getLastChorusValues()[drumIndex][stepIndex] = chorus;
                effectCount++;
            }

            if (decay != data.getLastDecayValues()[drumIndex][stepIndex]) {
                data.getEffectControllers()[effectCount] = DrumSequenceData.CC_DELAY; // Using delay CC for decay
                data.getEffectValues()[effectCount] = decay;
                data.getLastDecayValues()[drumIndex][stepIndex] = decay;
                effectCount++;
            }

            // Send effects only if needed
            if (effectCount > 0) {
                int channel = player.getChannel();
                int[] controllers = Arrays.copyOf(data.getEffectControllers(), effectCount);
                int[] values = Arrays.copyOf(data.getEffectValues(), effectCount);

                player.getInstrument().sendBulkCC(controllers, values);
            }
        } catch (Exception e) {
            // Just ignore errors to avoid performance impact
        }
    }

    /**
     * Ensure all drum players have valid device connections
     */
    public void ensureDeviceConnections() {
        logger.info("Ensuring all drum pads have valid device connections");

        // Make sure the internal synth is initialized
        ensureInternalSynthAvailable();
        
        // Get a default device to use as fallback
        MidiDevice defaultDevice = getDefaultMidiDevice();
        
        // Connect each drum pad to a valid device
        int connectedCount = 0;
        for (int i = 0; i < DrumSequenceData.DRUM_PAD_COUNT; i++) {
            if (connectDrumPad(i, defaultDevice)) {
                connectedCount++;
            }
        }
        
        logger.info("Connected {}/{} drum pads to active devices", connectedCount, DrumSequenceData.DRUM_PAD_COUNT);
    }

    /**
     * Ensure internal synthesizer is available
     */
    private void ensureInternalSynthAvailable() {
        if (!internalSynthManager.checkInternalSynthAvailable()) {
            internalSynthManager.initializeSynthesizer();
            this.usingInternalSynth = true;
            logger.info("Initialized internal synth for drum sequencer");
        }
    }

    /**
     * Get a default MIDI device to use as fallback
     */
    private MidiDevice getDefaultMidiDevice() {
        // First try to get the default output device
        MidiDevice device = DeviceManager.getInstance().getDefaultOutputDevice();
        
        // If that fails, try to get Gervill specifically
        if (device == null) {
            logger.debug("No default output device available, trying Gervill");
            device = DeviceManager.getInstance().getMidiDevice("Gervill");
            
            // Make sure it's open
            if (device != null && !device.isOpen()) {
                try {
                    device.open();
                    logger.info("Opened Gervill device for drum sequencer");
                } catch (Exception e) {
                    logger.error("Could not open Gervill device: {}", e.getMessage());
                    device = null;
                }
            }
        }
        
        return device;
    }

    /**
     * Connect a specific drum pad to a valid MIDI device
     * @param drumIndex The index of the drum pad to connect
     * @param defaultDevice The default device to use as fallback
     * @return true if successfully connected, false otherwise
     */
    private boolean connectDrumPad(int drumIndex, MidiDevice defaultDevice) {
        Player player = players[drumIndex];
        if (player == null) {
            logger.debug("No player for drum pad {}", drumIndex);
            return false;
        }
        
        // Ensure channel is set correctly
        player.setChannel(DrumSequenceData.MIDI_DRUM_CHANNEL);
        
        // Ensure instrument is set
        if (player.getInstrument() == null && player.getInstrumentId() != null) {
            player.setInstrument(InstrumentManager.getInstance().getInstrumentById(player.getInstrumentId()));
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(DrumSequenceData.MIDI_DRUM_CHANNEL);
                player.getInstrument().setReceivedChannels(new Integer[] { DrumSequenceData.MIDI_DRUM_CHANNEL });
            } else {
                logger.warn("Failed to set instrument for drum {}", drumIndex);
                return false;
            }
        }
        
        InstrumentWrapper instrument = player.getInstrument();
        
        // Try to connect the instrument to a device
        return connectInstrumentToDevice(drumIndex, instrument, defaultDevice);
    }

    /**
     * Connect an instrument to a MIDI device
     * @param drumIndex The drum pad index (for logging)
     * @param instrument The instrument to connect
     * @param defaultDevice The default device to use if preferred device isn't available
     * @return true if successfully connected, false otherwise
     */
    private boolean connectInstrumentToDevice(int drumIndex, InstrumentWrapper instrument, MidiDevice defaultDevice) {
        // Skip if already connected
        if (instrument.getReceiver() != null) {
            logger.debug("Drum {} already has a valid receiver", drumIndex);
            return true;
        }
        
        try {
            // Strategy 1: Try to get device by name
            MidiDevice device = null;
            String deviceName = instrument.getDeviceName();
            
            if (deviceName != null && !deviceName.isEmpty()) {
                device = DeviceManager.getInstance().acquireDevice(deviceName);
                if (device != null && !device.isOpen()) {
                    try {
                        device.open();
                        logger.debug("Opened device {} for drum {}", deviceName, drumIndex);
                    } catch (Exception e) {
                        logger.warn("Could not open device {} for drum {}: {}", 
                                deviceName, drumIndex, e.getMessage());
                        device = null;
                    }
                }
            }
            
            // Strategy 2: Use default device if specific device not available
            if (device == null && defaultDevice != null) {
                device = defaultDevice;
                deviceName = defaultDevice.getDeviceInfo().getName();
                instrument.setDeviceName(deviceName);
                logger.debug("Using default device {} for drum {}", deviceName, drumIndex);
            }
            
            // Strategy 3: Try Gervill specifically as last resort
            if (device == null) {
                device = DeviceManager.getInstance().getMidiDevice("Gervill");
                if (device != null) {
                    if (!device.isOpen()) {
                        device.open();
                    }
                    deviceName = "Gervill";
                    instrument.setDeviceName(deviceName);
                    logger.debug("Using Gervill synthesizer for drum {}", drumIndex);
                }
            }
            
            // Now get a receiver for the device
            if (device != null) {
                instrument.setDevice(device);
                
                Receiver receiver = ReceiverManager.getInstance()
                        .getOrCreateReceiver(deviceName, device);
                
                if (receiver != null) {
                    // UPDATED: Now directly set the receiver (no AtomicReference)
                    instrument.setReceiver(receiver);
                    
                    // Initialize sound with proper program change
                    PlayerManager.getInstance().applyInstrumentPreset(players[drumIndex]);
                    
                    logger.info("Successfully connected drum {} to device {}", drumIndex, deviceName);
                    
                    // Save the player to persist these changes
                    PlayerManager.getInstance().savePlayerProperties(players[drumIndex]);



                    return true;
                } else {
                    logger.warn("Failed to get receiver for drum {} from device {}", drumIndex, deviceName);
                }
            } else {
                logger.warn("Could not find any valid device for drum {}", drumIndex);
            }
        } catch (Exception e) {
            logger.error("Error connecting drum {}: {}", drumIndex, e.getMessage());
        }
        
        return false;
    }

    /**
     * Attempts to repair MIDI connections if they have been lost
     */
    public void repairMidiConnections() {
        logger.info("Attempting to repair MIDI connections for drum players");

        // First clear all existing receivers
        ReceiverManager.getInstance().clearAllReceivers();

        // Then try to reconnect all devices
        for (int i = 0; i < DrumSequenceData.DRUM_PAD_COUNT; i++) {
            Player player = players[i];
            if (player != null && player.getInstrument() != null) {
                InstrumentWrapper instrument = player.getInstrument();
                
                String deviceName = instrument.getDeviceName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "Gervill";
                    instrument.setDeviceName(deviceName);
                }
                
                MidiDevice device = DeviceManager.getInstance().getMidiDevice(deviceName);
                if (device == null) {
                    device = DeviceManager.getInstance().getDefaultOutputDevice();
                    if (device != null) {
                        deviceName = device.getDeviceInfo().getName();
                        instrument.setDeviceName(deviceName);
                    }
                }
                
                if (device != null) {
                    if (!device.isOpen()) {
                        try {
                            device.open();
                        } catch (Exception e) {
                            logger.warn("Could not open device {} for drum {}: {}", 
                                    deviceName, i, e.getMessage());
                            continue;
                        }
                    }
                    
                    // Set device and get receiver
                    instrument.setDevice(device);
                    Receiver receiver = ReceiverManager.getInstance()
                            .getOrCreateReceiver(deviceName, device);
                    
                    if (receiver != null) {
                        // UPDATED: Now directly set the receiver (no AtomicReference)
                        instrument.setReceiver(receiver);
                        logger.info("Successfully reconnected drum {} to device {}", i, deviceName);
                    }
                }
            }
        }

        // Force update instrument settings
        for (int i = 0; i < DrumSequenceData.DRUM_PAD_COUNT; i++) {
            Player player = players[i];
            if (player != null && player.getInstrument() != null) {
                PlayerManager.getInstance().applyInstrumentPreset(player);
            }
        }

        logger.info("MIDI connection repair completed");
    }

    /**
     * Set instrument for a specific drum with all parameters
     * @param drumIndex The drum index
     * @param instrumentId The instrument ID (or null to create a new one)
     * @param deviceName The device name
     * @param soundbankName The soundbank name
     * @param preset The preset number
     * @param bankIndex The bank index
     * @return The instrument that was set
     */
    public InstrumentWrapper setDrumInstrument(int drumIndex, Long instrumentId, 
            String deviceName, String soundbankName, Integer preset, Integer bankIndex) {
        
        if (drumIndex < 0 || drumIndex >= DrumSequenceData.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index: {}", drumIndex);
            return null;
        }
        
        Player player = getPlayer(drumIndex);
        if (player == null) {
            logger.warn("No player for drum {}", drumIndex);
            return null;
        }
        
        // First try to get existing instrument by ID
        InstrumentWrapper instrument = null;
        if (instrumentId != null) {
            instrument = InstrumentManager.getInstance().getInstrumentById(instrumentId);
        }
        
        // If not found, create a new one
        if (instrument == null) {
            // Get device
            MidiDevice device = null;
            if (deviceName != null && !deviceName.isEmpty()) {
                device = DeviceManager.getInstance().acquireDevice(deviceName);
            }
            
            // Create new instrument
            instrument = new InstrumentWrapper(
                "Drum " + drumIndex,
                device,
                DrumSequenceData.MIDI_DRUM_CHANNEL
            );
            
            // Set additional parameters
            if (deviceName != null) {
                instrument.setDeviceName(deviceName);
            }
            
            if (soundbankName != null) {
                instrument.setSoundbankName(soundbankName);
            }
            
            if (preset != null) {
                instrument.setPreset(preset);
            }
            
            if (bankIndex != null) {
                instrument.setBankIndex(bankIndex);
            }
            
            // Save the instrument
            InstrumentManager.getInstance().updateInstrument(instrument);
        }
        
        // Set the instrument on the player
        player.setInstrument(instrument);
        player.setInstrumentId(instrument.getId());
        
        // Apply the instrument preset
        PlayerManager.getInstance().applyInstrumentPreset(player);
        
        // Update data for saving
        data.getInstrumentIds()[drumIndex] = instrument.getId();
        data.getSoundbankNames()[drumIndex] = instrument.getSoundbankName();
        data.getPresets()[drumIndex] = instrument.getPreset();
        data.getBankIndices()[drumIndex] = instrument.getBankIndex();
        data.getDeviceNames()[drumIndex] = instrument.getDeviceName();
        data.getInstrumentNames()[drumIndex] = instrument.getName();
        
        return instrument;
    }
}
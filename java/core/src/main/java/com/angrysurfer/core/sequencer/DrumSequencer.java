package com.angrysurfer.core.sequencer;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;

import com.angrysurfer.core.redis.MelodicSequenceDataHelper;
import com.angrysurfer.core.redis.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Core sequencer engine that handles drum pattern sequencing and playback with
 * individual parameters per drum pad.
 */
@Getter
@Setter
public class DrumSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencer.class);

    // Constants
    public static final int DRUM_PAD_COUNT = 16; // Number of drum pads

    // MIDI and music constants
    private static final int MIDI_DRUM_CHANNEL = 9; // Standard MIDI drum channel
    public static final int MIDI_DRUM_NOTE_OFFSET = 36; // First drum pad note number
    private static final int MAX_MIDI_VELOCITY = 127; // Maximum MIDI velocity

    // MIDI CC values
    private static final int CC_VOLUME = 7;
    private static final int CC_PAN = 10;
    private static final int CC_REVERB = 91;
    private static final int CC_CHORUS = 93;
    private static final int CC_DELAY = 94; // Use for decay

    // Default values for parameters
    public static final int DEFAULT_VELOCITY = 100; // Default note velocity
    public static final int DEFAULT_DECAY = 60; // Default note decay
    public static final int DEFAULT_PROBABILITY = 100; // Default step probability (%)
    private static final int DEFAULT_TICKS_PER_BEAT = 24; // Default timing fallback
    private static final int DEFAULT_MASTER_TEMPO = 96; // Default master tempo

    private int defaultPatternLength = 32; // Default pattern length
    private int maxPatternLength = 64; // Default pattern length

    // Swing parameters
    private static final int NO_SWING = 50; // Percentage value for no swing
    public static final int MAX_SWING = 99; // Maximum swing percentage
    public static final int MIN_SWING = 25; // Minimum swing percentage

    // Pattern generation parameters
    private static final int MAX_DENSITY = 10; // Maximum density for pattern generation

    // Button dimensions
    private static final int DRUM_PAD_SIZE = 28; // Standard drum pad button size

    // Global sequencing state
    private long tickCounter = 0; // Count ticks
    private int beatCounter = 0; // Count beats
    private int ticksPerStep = 24; // Base ticks per step
    private boolean isPlaying = false; // Global play state
    private int absoluteStep = 0; // Global step counter independent of individual drum steps

    private long drumSequenceId = -1;

    // Per-drum sequencing state
    private int[] currentStep; // Current step for each drum
    private boolean[] patternCompleted; // Pattern completion flag for each drum
    private long[] nextStepTick; // Next step trigger tick for each drum

    // Per-drum pattern parameters
    private int[] patternLengths; // Pattern length for each drum
    private Direction[] directions; // Direction for each drum
    private TimingDivision[] timingDivisions; // Timing for each drum
    private boolean[] loopingFlags; // Loop setting for each drum
    private int[] bounceDirections; // 1 for forward, -1 for backward (for bounce mode)
    private int[] velocities; // Velocity for each drum
    private int[] originalVelocities; // Saved original velocities for resetting

    // Pattern data storage
    private boolean[][] patterns; // [drumIndex][stepIndex]

    // Per-step parameter values for each drum
    private int[][] stepVelocities; // Velocity for each step of each drum [drumIndex][stepIndex]
    private int[][] stepDecays; // Decay (gate time) for each step of each drum [drumIndex][stepIndex]
    private int[][] stepProbabilities; // Probability for each step [drumIndex][stepIndex]
    private int[][] stepNudges; // Timing nudge for each step [drumIndex][stepIndex]

    // New effect parameters
    private int[][] stepPans; // Pan position (0-127) for each step [drumIndex][stepIndex]
    private int[][] stepChorus; // Chorus amount (0-100) for each step [drumIndex][stepIndex]
    private int[][] stepReverb; // Reverb amount (0-100) for each step [drumIndex][stepIndex]

    // Track last sent effect values to avoid redundant MIDI messages
    private int[][] lastSentPans;
    private int[][] lastSentChorus;
    private int[][] lastSentReverb;
    private int[][] lastSentDecays; // For delay effect

    // Reusable arrays for effects to avoid constant object creation
    private final int[] effectControllers = new int[4];
    private final int[] effectValues = new int[4];

    // These track the last sent values to avoid redundant messages
    private int[][] lastPanValues;
    private int[][] lastReverbValues;
    private int[][] lastChorusValues;
    private int[][] lastDecayValues;

    // Constants for default values
    public static final int DEFAULT_PAN = 64; // Default pan position (center)
    public static final int DEFAULT_CHORUS = 0; // Default chorus effect amount
    public static final int DEFAULT_REVERB = 0; // Default reverb effect amount

    // Player objects for each drum pad
    private Player[] players;
    private InstrumentWrapper[] instruments;

    // Selection state
    private int selectedPadIndex = 0; // Currently selected drum pad

    // Event handling
    private Consumer<DrumStepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    private int masterTempo;

    // Swing parameters
    private int swingPercentage = 50; // Default swing percentage (50 = no swing)
    private boolean swingEnabled = false; // Swing enabled flag

    private Consumer<NoteEvent> noteEventPublisher;

    // Add field for next pattern ID
    private Long nextPatternId = null;

    // Track pattern completion for switching
    private boolean patternJustCompleted = false;

    // 1. Create a reusable message object as a class field
    private final javax.sound.midi.ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    // Add as static fields in both sequencer classes
    private static final ScheduledExecutorService SHARED_NOTE_SCHEDULER = Executors.newScheduledThreadPool(2);

    // Add field to track if we're using internal synth
    private boolean usingInternalSynth = false;
    private InternalSynthManager internalSynthManager = null;

    public void setNoteEventPublisher(Consumer<NoteEvent> publisher) {
        this.noteEventPublisher = publisher;
    }

    /**
     * Get the currently selected drum pad index
     * 
     * @return The selected pad index
     */
    public int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    /**
     * Set the currently selected drum pad index and publish event
     * 
     * @param index The new selected pad index
     */
    public void setSelectedPadIndex(int index) {
        if (index >= 0 && index < DRUM_PAD_COUNT) {
            // Store old selection
            int oldSelection = selectedPadIndex;

            // Set new selection
            selectedPadIndex = index;

            // Notify listeners of selection change
            CommandBus.getInstance().publish(
                    Commands.DRUM_PAD_SELECTED,
                    this,
                    new DrumPadSelectionEvent(oldSelection, index));

            logger.info("Selected drum pad index updated to: {}", index);
        } else {
            logger.warn("Invalid drum pad index: {}", index);
        }
    }

    /**
     * Creates a new drum sequencer with per-drum parameters
     */
    public DrumSequencer() {
        // Initialize arrays
        currentStep = new int[DRUM_PAD_COUNT];
        patternCompleted = new boolean[DRUM_PAD_COUNT];
        nextStepTick = new long[DRUM_PAD_COUNT];

        patternLengths = new int[DRUM_PAD_COUNT];
        directions = new Direction[DRUM_PAD_COUNT];
        timingDivisions = new TimingDivision[DRUM_PAD_COUNT];
        loopingFlags = new boolean[DRUM_PAD_COUNT];
        bounceDirections = new int[DRUM_PAD_COUNT];

        // Initialize velocity arrays
        velocities = new int[DRUM_PAD_COUNT];
        originalVelocities = new int[DRUM_PAD_COUNT];
        Arrays.fill(velocities, DEFAULT_VELOCITY);
        Arrays.fill(originalVelocities, DEFAULT_VELOCITY);

        // Default values
        Arrays.fill(patternLengths, getDefaultPatternLength());
        Arrays.fill(directions, Direction.FORWARD); // Default to forward
        Arrays.fill(timingDivisions, TimingDivision.NORMAL); // Default timing
        Arrays.fill(loopingFlags, true); // Default to looping
        Arrays.fill(bounceDirections, 1); // Default to forward bounce

        // Initialize masterTempo with default value
        masterTempo = DEFAULT_MASTER_TEMPO;

        // Initialize patterns with max possible length
        patterns = new boolean[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepVelocities = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepDecays = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepProbabilities = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepNudges = new int[DRUM_PAD_COUNT][getMaxPatternLength()];

        // Initialize new arrays for effects
        stepPans = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepChorus = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        stepReverb = new int[DRUM_PAD_COUNT][getMaxPatternLength()];

        // Initialize last sent effect values
        lastSentPans = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastSentChorus = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastSentReverb = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastSentDecays = new int[DRUM_PAD_COUNT][getMaxPatternLength()];

        // Initialize effect tracking arrays
        lastPanValues = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastReverbValues = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastChorusValues = new int[DRUM_PAD_COUNT][getMaxPatternLength()];
        lastDecayValues = new int[DRUM_PAD_COUNT][getMaxPatternLength()];

        // Set initial values to -1 to ensure first message gets sent
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < getMaxPatternLength(); j++) {
                lastPanValues[i][j] = -1;
                lastReverbValues[i][j] = -1;
                lastChorusValues[i][j] = -1;
                lastDecayValues[i][j] = -1;
            }
        }

        // Set default values
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < getMaxPatternLength(); j++) {
                stepVelocities[i][j] = DEFAULT_VELOCITY;
                stepDecays[i][j] = DEFAULT_DECAY;
                stepProbabilities[i][j] = DEFAULT_PROBABILITY;
                stepNudges[i][j] = 0; // Default nudge at 0 (no offset)

                // Set defaults for new effect parameters
                stepPans[i][j] = DEFAULT_PAN; // Center pan
                stepChorus[i][j] = DEFAULT_CHORUS; // No chorus by default
                stepReverb[i][j] = DEFAULT_REVERB; // No reverb by default

                // Initialize last sent values to invalid defaults
                lastSentPans[i][j] = -1;
                lastSentChorus[i][j] = -1;
                lastSentReverb[i][j] = -1;
                lastSentDecays[i][j] = -1;
            }
        }

        // Initialize players array
        instruments = new InstrumentWrapper[DRUM_PAD_COUNT];
        players = new Player[DRUM_PAD_COUNT];

        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            players[i] = RedisService.getInstance().newStrike();
            players[i].setOwner(this);
            players[i].setChannel(MIDI_DRUM_CHANNEL);
            players[i].setRootNote(MIDI_DRUM_NOTE_OFFSET + i);
            players[i].setName(InternalSynthManager.getInstance().getDrumName(MIDI_DRUM_NOTE_OFFSET + i));

            // Use PlayerManager to initialize the instrument - it will use our enhanced API
            PlayerManager.getInstance().initializeInternalInstrument(players[i], false);

            // Store reference to instrument
            instruments[i] = players[i].getInstrument();
            logger.debug("Initialized drum pad {} with note {}", i, MIDI_DRUM_NOTE_OFFSET + i);
        }

        // Register with command bus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Load first saved sequence (if available) instead of default pattern
        loadFirstSequence();
    }

    /**
     * Sets the global swing percentage
     * 
     * @param percentage Value from 50 (no swing) to 75 (maximum swing)
     */
    public void setSwingPercentage(int percentage) {
        // Limit to valid range

        this.swingPercentage = Math.max(MIN_SWING, Math.min(MAX_SWING, percentage));
        logger.info("Swing percentage set to: {}", swingPercentage);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                this,
                -1 // -1 indicates global parameter
        );
    }

    public int getSwingPercentage() {
        return swingPercentage;
    }

    public void setSwingEnabled(boolean enabled) {
        this.swingEnabled = enabled;
        logger.info("Swing enabled: {}", enabled);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                this,
                -1);
    }

    public boolean isSwingEnabled() {
        return swingEnabled;
    }

    public int getAbsoluteStep() {
        return absoluteStep;
    }

    public void setAbsoluteStep(int step) {
        this.absoluteStep = step;
    }

    /**
     * Load a sequence while preserving playback position if sequencer is running
     * 
     * @param sequenceId The ID of the sequence to load
     * @return true if sequence loaded successfully
     */
    public boolean loadSequence(long sequenceId) {
        // Don't do anything if trying to load the currently active sequence
        if (sequenceId == drumSequenceId) {
            logger.info("Sequence {} already loaded", sequenceId);
            return true;
        }

        // Store current playback state
        boolean wasPlaying = isPlaying;

        // Get the manager
        DrumSequencerManager manager = DrumSequencerManager.getInstance();

        // Load the sequence
        boolean loaded = manager.loadSequence(sequenceId, this);

        if (loaded) {
            logger.info("Loaded drum sequence: {}", sequenceId);

            // Immediately update visual indicators without resetting
            if (stepUpdateListener != null) {
                for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                    // Force an update with the current positions
                    stepUpdateListener.accept(
                            new DrumStepUpdateEvent(drumIndex, -1, currentStep[drumIndex]));
                }
            }
            
            // Publish event to notify UI components
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_LOADED,
                    this,
                    drumSequenceId);

            // Preserve playing state (don't stop if we were playing)
            isPlaying = wasPlaying;

            return true;
        } else {
            logger.warn("Failed to load drum sequence {}", sequenceId);
            return false;
        }
    }

    /**
     * Modify this method to use our new loadSequence method
     */
    private void loadFirstSequence() {
        try {
            // Get the manager
            DrumSequencerManager manager = DrumSequencerManager.getInstance();

            // Get the first sequence ID
            Long firstId = manager.getFirstSequenceId();

            if (firstId != null) {
                // Use our new method instead of directly calling manager.loadSequence
                loadSequence(firstId);
            } else {
                logger.info("No saved drum sequences found, using empty pattern");
            }
        } catch (Exception e) {
            logger.error("Error loading initial drum sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Override the reset method to preserve positions when needed
     */
    public void reset(boolean preservePositions) {
        if (!preservePositions) {
            // Original full reset behavior
            Arrays.fill(currentStep, 0);
            Arrays.fill(patternCompleted, false);
            Arrays.fill(nextStepTick, 0);
            Arrays.fill(bounceDirections, 1);

            // Reset global counters
            tickCounter = 0;
            beatCounter = 0;
            absoluteStep = 0; // Reset the absolute step counter
        } else {
            // Just reset state flags but keep positions
            Arrays.fill(patternCompleted, false);
            Arrays.fill(currentStep, absoluteStep);

            // Don't reset absoluteStep when preserving positions

            // Recalculate next step times
            for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                if (timingDivisions[i] != null) {
                    int calculatedTicksPerStep = calculateTicksPerStep(timingDivisions[i]);
                    nextStepTick[i] = tickCounter + calculatedTicksPerStep;
                }
            }
        }

        // Force the sequencer to generate an event to update visual indicators
        if (stepUpdateListener != null) {
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                stepUpdateListener.accept(new DrumStepUpdateEvent(drumIndex, -1, currentStep[drumIndex]));
            }
        }

        logger.debug("Sequencer reset - preservePositions={}", preservePositions);
    }

    /**
     * Keep the original reset method for backward compatibility
     */
    public void reset() {
        // Call the new method with preservePositions=false
        reset(false);
    }

    /**
     * Process a timing tick - now handles each drum separately and checks for
     * pattern completion
     *
     * @param tick The current tick count
     */
    public void processTick(long tick) {
        if (!isPlaying) {
            return;
        }

        tickCounter = tick;

        // Use the standard timing (first drum's timing) to determine global step
        // changes
        int standardTicksPerStep = TimingDivision.NORMAL.getTicksPerBeat();

        // Update absoluteStep based on the tick count - for the global timing
        if (tick % standardTicksPerStep == 0) {
            // Increment the absoluteStep (cycle through the maximum pattern length)
            absoluteStep = (absoluteStep + 1) % getMaxPatternLength();
            // Log the absolute step for debugging
            logger.debug("Absolute step: {}", absoluteStep);
        }

        // Reset pattern completion flag at the start of processing
        patternJustCompleted = false;

        // Process each drum separately
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Skip if no Player configured
            if (players[drumIndex] == null) {
                continue;
            }

            // IMPORTANT: Use each drum's timing division instead of fixed value
            int drumTicksPerStep = timingDivisions[drumIndex].getTicksPerBeat();

            // Make sure we have a valid minimum value
            if (drumTicksPerStep <= 0) {
                drumTicksPerStep = 24; // Emergency fallback
            }

            // Use modulo instead of next tick calculations - more stable
            if (tick % drumTicksPerStep == 0) {
                // Reset pattern completion flag if we're looping
                if (patternCompleted[drumIndex] && loopingFlags[drumIndex]) {
                    patternCompleted[drumIndex] = false;
                }

                // Handle if pattern is completed and not looping
                if (patternCompleted[drumIndex] && !loopingFlags[drumIndex]) {
                    continue; // Skip this drum
                }

                // Process the current step for this drum
                processStep(drumIndex);

                // Debug log to track progression
                logger.debug("Drum {} step processed at tick {} (timing: {})",
                        drumIndex, tick, timingDivisions[drumIndex].getDisplayName());
            }
        }

        // Check for pattern completion - this happens when all drums have completed
        // their patterns at least once in the current cycle
        boolean allCompleted = true;
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            if (!patternCompleted[i] && loopingFlags[i]) {
                allCompleted = false;
                break;
            }
        }

        // If we completed a full cycle and have a next pattern queued
        if (allCompleted && nextPatternId != null) {
            patternJustCompleted = true;

            // Switch to next pattern
            Long currentId = drumSequenceId;
            loadSequence(nextPatternId);

            // Notify about pattern switch
            CommandBus.getInstance().publish(
                    Commands.DRUM_PATTERN_SWITCHED,
                    this,
                    new PatternSwitchEvent(currentId, nextPatternId));

            // Clear the next pattern ID (one-shot behavior)
            nextPatternId = null;
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
        this.nextPatternId = patternId;
        logger.info("Set next drum pattern ID: {}", patternId);
    }

    /**
     * Get the next pattern ID that will be loaded when the current pattern
     * completes
     * 
     * @return The next pattern ID or null if no pattern is queued
     */
    public Long getNextPatternId() {
        return nextPatternId;
    }

    /**
     * Process the current step for a drum
     *
     * @param drumIndex The drum to process
     */
    private void processStep(int drumIndex) {
        // Get the current step for this drum
        int step = currentStep[drumIndex];

        // Notify listeners of step update BEFORE playing the sound
        // This ensures visual indicators match the sound that's about to play
        if (stepUpdateListener != null) {
            // FIXED: Use the proper constructor with the required parameters
            // instead of trying to use setters on a new instance
            DrumStepUpdateEvent event = new DrumStepUpdateEvent(
                    drumIndex,
                    getPreviousStep(drumIndex),
                    step);
            stepUpdateListener.accept(event);
        }

        // Trigger the drum step
        triggerDrumStep(drumIndex, step);

        // Calculate next step
        calculateNextStep(drumIndex);
    }

    /**
     * Trigger a drum step with per-step parameters
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     */
    private void triggerDrumStep(int drumIndex, int stepIndex) {
        // Skip if step is inactive
        if (!patterns[drumIndex][stepIndex]) {
            return;
        }

        // Get all step parameters
        int velocity = stepVelocities[drumIndex][stepIndex];
        int probability = stepProbabilities[drumIndex][stepIndex];
        int decay = stepDecays[drumIndex][stepIndex];
        int nudge = stepNudges[drumIndex][stepIndex];

        // Check probability
        if (Math.random() * 100 >= probability) {
            return;
        }

        // Apply velocity scaling
        int finalVelocity = (int) (velocity * (velocities[drumIndex] / 127.0));
        if (finalVelocity <= 0) {
            return;
        }

        // Get player
        Player player = players[drumIndex];
        if (player == null) {
            return;
        }

        // Set instrument if needed
        if (player.getInstrument() == null) {
            player.setInstrument(instruments[drumIndex]);
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(MIDI_DRUM_CHANNEL);
                player.getInstrument().setReceivedChannels(new Integer[] { MIDI_DRUM_CHANNEL });
            }
        }

        // Process and send effects before playing the note
        processEffects(drumIndex, stepIndex, player);

        // Apply swing if needed
        if (swingEnabled && stepIndex % 2 == 1) {
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
     * Trigger a note and publish a NoteEvent
     *
     * @param drumIndex The drum pad index
     * @param velocity  The velocity of the note
     */
    private void publishNoteEvent(int drumIndex, int velocity, int durationMs) {

        // Add this at the end:
        if (noteEventPublisher != null) {
            // Convert drum index back to MIDI note (36=kick, etc.)
            int midiNote = drumIndex + MIDI_DRUM_NOTE_OFFSET;
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
        TimingDivision division = timingDivisions[drumIndex];
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
        float swingFactor = (swingPercentage - 50) / 100f;

        // Return swing amount in milliseconds
        return (int) (stepDurationMs * swingFactor);
    }

    /**
     * Calculate the previous step based on current direction
     */
    private int getPreviousStep(int drumIndex) {
        Direction direction = directions[drumIndex];
        int currentPos = currentStep[drumIndex];
        int length = patternLengths[drumIndex];

        return switch (direction) {
            case FORWARD ->
                (currentPos + length - 1) % length;
            case BACKWARD ->
                (currentPos + 1) % length;
            case BOUNCE -> {
                // For bounce, it depends on the current bounce direction
                if (bounceDirections[drumIndex] > 0) {
                    yield currentPos > 0 ? currentPos - 1 : 0;
                } else {
                    yield currentPos < length - 1 ? currentPos + 1 : length - 1;
                }
            }
            case RANDOM ->
                currentPos; // For random, just use current position
        };
    }

    /**
     * Calculate the next step for a drum based on its direction
     *
     * @param drumIndex The drum to calculate for
     */
    private void calculateNextStep(int drumIndex) {
        Direction direction = directions[drumIndex];
        int length = patternLengths[drumIndex];

        switch (direction) {
            case FORWARD -> {
                currentStep[drumIndex] = (currentStep[drumIndex] + 1) % length;

                // Check for pattern completion
                if (currentStep[drumIndex] == 0) {
                    patternCompleted[drumIndex] = true;
                }
            }
            case BACKWARD -> {
                currentStep[drumIndex] = (currentStep[drumIndex] - 1 + length) % length;

                // Check for pattern completion
                if (currentStep[drumIndex] == length - 1) {
                    patternCompleted[drumIndex] = true;
                }
            }
            case BOUNCE -> {
                // Get bounce direction (1 or -1)
                int bounce = bounceDirections[drumIndex];

                // Move step
                currentStep[drumIndex] += bounce;

                // Check bounds and reverse if needed
                if (currentStep[drumIndex] >= length) {
                    currentStep[drumIndex] = length - 2;
                    bounceDirections[drumIndex] = -1;
                    patternCompleted[drumIndex] = true;
                } else if (currentStep[drumIndex] < 0) {
                    currentStep[drumIndex] = 1;
                    bounceDirections[drumIndex] = 1;
                    patternCompleted[drumIndex] = true;
                }
            }
            case RANDOM -> {
                int oldStep = currentStep[drumIndex];
                // Generate a random step position
                currentStep[drumIndex] = (int) (Math.random() * length);

                // Ensure we don't get the same step twice in a row
                if (currentStep[drumIndex] == oldStep && length > 1) {
                    currentStep[drumIndex] = (currentStep[drumIndex] + 1) % length;
                }

                // Random is considered complete after each step
                patternCompleted[drumIndex] = true;
            }
        }
    }

    /**
     * Advances the step for a specific drum based on its direction
     * 
     * @param drumIndex The drum to advance
     */
    private void advanceStepForDrum(int drumIndex) {
        // Use existing calculateNextStep method which already updates currentStep
        calculateNextStep(drumIndex);
    }

    /**
     * Calculates when the next step should occur for a drum
     * 
     * @param drumIndex The drum to calculate for
     */
    private void calculateNextStepTime(int drumIndex) {
        if (timingDivisions[drumIndex] != null) {
            int calculatedTicksPerStep = calculateTicksPerStep(timingDivisions[drumIndex]);
            nextStepTick[drumIndex] = tickCounter + calculatedTicksPerStep;
        }
    }

    /**
     * Calculate ticks per step based on timing division Fixed to prevent
     * division by zero or very small values
     */
    private int calculateTicksPerStep(TimingDivision timing) {
        // CRITICAL FIX: Add safety check to prevent division by zero
        if (masterTempo <= 0) {
            logger.warn("Invalid masterTempo value ({}), using default of {}", masterTempo, DEFAULT_MASTER_TEMPO);
            masterTempo = DEFAULT_MASTER_TEMPO; // Emergency fallback
        }

        double ticksPerBeat = timing.getTicksPerBeat();
        if (ticksPerBeat <= 0) {
            logger.warn("Invalid ticksPerBeat value ({}), using default of {}", ticksPerBeat, DEFAULT_TICKS_PER_BEAT);
            ticksPerBeat = DEFAULT_TICKS_PER_BEAT; // Emergency fallback
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
        this.masterTempo = sessionTicksPerBeat;
        logger.info("Updated master tempo to {}", masterTempo);

        // Recalculate all next step timings based on new tempo
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            if (timingDivisions[drumIndex] != null) {
                int calculatedTicksPerStep = calculateTicksPerStep(timingDivisions[drumIndex]);
                nextStepTick[drumIndex] = tickCounter + calculatedTicksPerStep;
            }
        }
    }

    /**
     * Start playback with proper initialization
     */
    public void play() {
        isPlaying = true;

        // CRITICAL FIX: Reset step positions to ensure consistent playback
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            // Set all next step ticks to the current tick to trigger immediately
            nextStepTick[i] = tickCounter;

            // Reset pattern completion flags
            patternCompleted[i] = false;
        }

        logger.info("DrumSequencer playback started at tick {}", tickCounter);
    }

    /**
     * Stop playback
     */
    public void start() {
        if (!isPlaying) {
            reset();
            isPlaying = true;
        }
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            reset();
        }
    }

    /**
     * Toggle a pattern step for a drum pad
     */
    public void toggleStep(int drumIndex, int step) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT
                && step >= 0 && step < getMaxPatternLength()) {
            patterns[drumIndex][step] = !patterns[drumIndex][step];
        }
    }

    // Getter and setter methods for per-drum parameters
    public int getPatternLength(int drumIndex) {
        // Add bounds check
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getPatternLength", drumIndex);
            return getDefaultPatternLength(); // Return default
        }
        return patternLengths[drumIndex];
    }

    public void setPatternLength(int drumIndex, int length) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT
                && length > 0 && length <= getMaxPatternLength()) {
            logger.info("Setting pattern length for drum {} to {}", drumIndex, length);
            patternLengths[drumIndex] = length;

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex);
        }
    }

    public Direction getDirection(int drumIndex) {
        // Add bounds check
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getDirection", drumIndex);
            return Direction.FORWARD; // Return default
        }
        return directions[drumIndex];
    }

    public void setDirection(int drumIndex, Direction direction) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            directions[drumIndex] = direction;

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex);
        }
    }

    public TimingDivision getTimingDivision(int drumIndex) {
        // Add bounds check
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getTimingDivision", drumIndex);
            return TimingDivision.NORMAL; // Return default
        }
        return timingDivisions[drumIndex];
    }

    public void setTimingDivision(int drumIndex, TimingDivision division) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            timingDivisions[drumIndex] = division;

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex);
        }
    }

    public boolean isLooping(int drumIndex) {
        // Add bounds check
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for isLooping", drumIndex);
            return true; // Return default
        }
        return loopingFlags[drumIndex];
    }

    public void setLooping(int drumIndex, boolean loop) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            loopingFlags[drumIndex] = loop;

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex);
        }
    }

    public int getVelocity(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            return velocities[drumIndex];
        }
        return DEFAULT_VELOCITY; // Default value
    }

    public void setVelocity(int drumIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            // Constrain to valid MIDI range
            velocity = Math.max(0, Math.min(MAX_MIDI_VELOCITY, velocity));
            velocities[drumIndex] = velocity;

            // If we have a Player object for this drum, update its level
            Player Player = getPlayer(drumIndex);
            if (Player != null) {
                Player.setLevel(velocity);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex);
        }
    }

    public int getStepVelocity(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepVelocities[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepVelocity(int drumIndex, int stepIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepVelocities[drumIndex][stepIndex] = velocity;
        }
    }

    public int getStepDecay(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepDecays[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepDecay(int drumIndex, int stepIndex, int decay) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepDecays[drumIndex][stepIndex] = decay;
        }
    }

    public int getStepProbability(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepProbabilities[drumIndex][stepIndex];
        }
        return DEFAULT_PROBABILITY; // Default to 100% if out of bounds
    }

    public void setStepProbability(int drumIndex, int stepIndex, int probability) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            // Clamp value between 0-100
            stepProbabilities[drumIndex][stepIndex] = Math.max(0, Math.min(100, probability));
        }
    }

    public int getStepNudge(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepNudges[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepNudge(int drumIndex, int stepIndex, int nudge) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepNudges[drumIndex][stepIndex] = nudge;
        }
    }

    /**
     * Get the pan position for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return Pan position (0-127, 64 is center)
     */
    public int getStepPan(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepPans[drumIndex][stepIndex];
        }
        return DEFAULT_PAN; // Default center pan
    }

    /**
     * Set the pan position for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @param pan       Pan position (0-127, 64 is center)
     */
    public void setStepPan(int drumIndex, int stepIndex, int pan) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepPans[drumIndex][stepIndex] = Math.max(0, Math.min(127, pan));
        }
    }

    /**
     * Get the chorus amount for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return Chorus amount (0-100)
     */
    public int getStepChorus(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepChorus[drumIndex][stepIndex];
        }
        return DEFAULT_CHORUS;
    }

    /**
     * Set the chorus amount for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @param chorus    Chorus amount (0-100)
     */
    public void setStepChorus(int drumIndex, int stepIndex, int chorus) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepChorus[drumIndex][stepIndex] = Math.max(0, Math.min(100, chorus));
        }
    }

    /**
     * Get the reverb amount for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return Reverb amount (0-100)
     */
    public int getStepReverb(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return stepReverb[drumIndex][stepIndex];
        }
        return DEFAULT_REVERB;
    }

    /**
     * Set the reverb amount for a specific step
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @param reverb    Reverb amount (0-100)
     */
    public void setStepReverb(int drumIndex, int stepIndex, int reverb) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            stepReverb[drumIndex][stepIndex] = Math.max(0, Math.min(100, reverb));
        }
    }

    /**
     * Set pattern length for the currently selected drum pad
     *
     * @param length The new pattern length (1-64)
     */
    public void setPatternLength(int length) {
        logger.info("Setting pattern length for drum {} to {}", selectedPadIndex, length);
        setPatternLength(selectedPadIndex, length);
    }

    /**
     * Set direction for the currently selected drum pad
     *
     * @param direction The new direction
     */
    public void setDirection(Direction direction) {
        setDirection(selectedPadIndex, direction);
    }

    /**
     * Set timing division for the currently selected drum pad
     *
     * @param division The new timing division
     */
    public void setTimingDivision(TimingDivision division) {
        setTimingDivision(selectedPadIndex, division);
    }

    /**
     * Set looping for the currently selected drum pad
     *
     * @param loop Whether the pattern should loop
     */
    public void setLooping(boolean loop) {
        setLooping(selectedPadIndex, loop);
    }

    /**
     * Get pattern length for the currently selected drum pad
     *
     * @return The pattern length
     */
    public int getSelectPatternLength() {
        return getPatternLength(selectedPadIndex);
    }

    /**
     * Select a drum pad and notify listeners
     *
     * @param padIndex The index of the drum pad to select (0-15)
     */
    public void selectDrumPad(int padIndex) {
        // Store old selection
        int oldSelection = selectedPadIndex;

        // Update selected pad (ensure it's within bounds)
        if (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) {
            selectedPadIndex = padIndex;
            logger.info("Drum pad selected: {} -> {}", oldSelection, selectedPadIndex);

            // Publish selection event on the command bus
            CommandBus.getInstance().publish(
                    Commands.DRUM_PAD_SELECTED,
                    this,
                    new DrumPadSelectionEvent(oldSelection, selectedPadIndex));
        } else {
            logger.warn("Invalid drum pad index: {}", padIndex);
        }
    }

    /**
     * Get the Player object for a specific drum pad
     *
     * @param drumIndex The index of the drum pad (0-15)
     * @return The Player object or null if not set
     */
    public Player getPlayer(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            return players[drumIndex];
        }
        return null;
    }

    /**
     * Set the Player object for a specific drum pad
     *
     * @param drumIndex The index of the drum pad (0-15)
     * @param Player    The Player object to associate with the drum pad
     */
    public void setPlayer(int drumIndex, Player Player) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            players[drumIndex] = Player;
        }
    }

    /**
     * Check if a specific step is active for a drum
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return true if the step is active
     */
    public boolean isStepActive(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT
                && stepIndex >= 0 && stepIndex < getMaxPatternLength()) {
            return patterns[drumIndex][stepIndex];
        }
        return false;
    }

    /**
     * Clear the pattern for all drum pads
     */
    public void clearPattern() {
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < getMaxPatternLength(); step++) {
                patterns[drumIndex][step] = false;
            }
        }
        logger.info("All patterns cleared");
    }

    /**
     * Generate a simple pattern for the specified drum
     *
     * @param density The index of the drum pad to generate a pattern for
     */
    public void generatePattern(int density) {
        // Generate pattern for selected drum pad
        int drumIndex = selectedPadIndex;
        int length = patternLengths[drumIndex];

        // Clear existing pattern
        for (int step = 0; step < length; step++) {
            patterns[drumIndex][step] = false;
        }

        // Generate new pattern based on density (1-10)
        int hitsToAdd = Math.max(1, Math.min(MAX_DENSITY, density)) * length / MAX_DENSITY;

        // Always add a hit on the first beat
        patterns[drumIndex][0] = true;
        hitsToAdd--;

        // Randomly distribute remaining hits
        while (hitsToAdd > 0) {
            int step = (int) (Math.random() * length);
            if (!patterns[drumIndex][step]) {
                patterns[drumIndex][step] = true;
                hitsToAdd--;
            }
        }

        // Notify UI of pattern change
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                this,
                selectedPadIndex);

        logger.info("Generated pattern for drum {}", selectedPadIndex);
    }

    /**
     * Initialize with a simple default pattern
     */
    private void initializeDefaultPattern() {
        // Set up a basic kick/snare pattern on first two drums
        if (DRUM_PAD_COUNT > 0) {
            // Kick on quarters (every 4 steps)
            for (int i = 0; i < 16; i += 4) {
                patterns[0][i] = true;
            }
        }

        if (DRUM_PAD_COUNT > 1) {
            // Snare on 5 and 13
            patterns[1][4] = true;
            patterns[1][12] = true;
        }

        if (DRUM_PAD_COUNT > 2) {
            // Hi-hat on even steps
            for (int i = 0; i < 16; i += 2) {
                patterns[2][i] = true;
            }
        }

        logger.info("Default pattern initialized");
    }

    /**
     * Push the pattern forward by one step for the selected drum pad,
     * wrapping the last step to the first position
     */
    public void pushForward() {
        int drumIndex = selectedPadIndex;
        int length = patternLengths[drumIndex];

        if (length <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate main pattern (trigger states)
        boolean lastTrigger = patterns[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            patterns[drumIndex][i] = patterns[drumIndex][i - 1];
        }
        patterns[drumIndex][0] = lastTrigger;

        // Rotate step velocities
        int lastVelocity = stepVelocities[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepVelocities[drumIndex][i] = stepVelocities[drumIndex][i - 1];
        }
        stepVelocities[drumIndex][0] = lastVelocity;

        // Rotate step decays
        int lastDecay = stepDecays[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepDecays[drumIndex][i] = stepDecays[drumIndex][i - 1];
        }
        stepDecays[drumIndex][0] = lastDecay;

        // Rotate step probabilities
        int lastProbability = stepProbabilities[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepProbabilities[drumIndex][i] = stepProbabilities[drumIndex][i - 1];
        }
        stepProbabilities[drumIndex][0] = lastProbability;

        // Rotate step nudges
        int lastNudge = stepNudges[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepNudges[drumIndex][i] = stepNudges[drumIndex][i - 1];
        }
        stepNudges[drumIndex][0] = lastNudge;

        // Rotate step pans
        int lastPan = stepPans[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepPans[drumIndex][i] = stepPans[drumIndex][i - 1];
        }
        stepPans[drumIndex][0] = lastPan;

        // Rotate step chorus
        int lastChorus = stepChorus[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepChorus[drumIndex][i] = stepChorus[drumIndex][i - 1];
        }
        stepChorus[drumIndex][0] = lastChorus;

        // Rotate step reverb
        int lastReverb = stepReverb[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepReverb[drumIndex][i] = stepReverb[drumIndex][i - 1];
        }
        stepReverb[drumIndex][0] = lastReverb;

        logger.info("Pushed pattern forward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_UPDATED,
                this,
                null);
    }

    /**
     * Pull the pattern backward by one step for the selected drum pad,
     * wrapping the first step to the last position
     */
    public void pullBackward() {
        int drumIndex = selectedPadIndex;
        int length = patternLengths[drumIndex];

        if (length <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate main pattern (trigger states)
        boolean firstTrigger = patterns[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            patterns[drumIndex][i] = patterns[drumIndex][i + 1];
        }
        patterns[drumIndex][length - 1] = firstTrigger;

        // Rotate step velocities
        int firstVelocity = stepVelocities[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepVelocities[drumIndex][i] = stepVelocities[drumIndex][i + 1];
        }
        stepVelocities[drumIndex][length - 1] = firstVelocity;

        // Rotate step decays
        int firstDecay = stepDecays[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepDecays[drumIndex][i] = stepDecays[drumIndex][i + 1];
        }
        stepDecays[drumIndex][length - 1] = firstDecay;

        // Rotate step probabilities
        int firstProbability = stepProbabilities[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepProbabilities[drumIndex][i] = stepProbabilities[drumIndex][i + 1];
        }
        stepProbabilities[drumIndex][length - 1] = firstProbability;

        // Rotate step nudges
        int firstNudge = stepNudges[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepNudges[drumIndex][i] = stepNudges[drumIndex][i + 1];
        }
        stepNudges[drumIndex][length - 1] = firstNudge;

        // Rotate step pans
        int firstPan = stepPans[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepPans[drumIndex][i] = stepPans[drumIndex][i + 1];
        }
        stepPans[drumIndex][length - 1] = firstPan;

        // Rotate step chorus
        int firstChorus = stepChorus[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepChorus[drumIndex][i] = stepChorus[drumIndex][i + 1];
        }
        stepChorus[drumIndex][length - 1] = firstChorus;

        // Rotate step reverb
        int firstReverb = stepReverb[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepReverb[drumIndex][i] = stepReverb[drumIndex][i + 1];
        }
        stepReverb[drumIndex][length - 1] = firstReverb;

        logger.info("Pulled pattern backward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_UPDATED,
                this,
                null);
    }

    /**
     * Required by IBusListener interface
     */
    @Override
    public void onAction(Command cmd) {
        if (cmd == null || cmd.getCommand() == null) {
            return;
        }

        // Add this before any other checks - ensure timing updates are processed
        if (Commands.TIMING_UPDATE.equals(cmd.getCommand()) && cmd.getData() instanceof TimingUpdate) {
            // CRITICAL: Process the tick without additional filtering
            processTick(((TimingUpdate) cmd.getData()).tickCount());
            return; // Process timing updates immediately and return
        }

        // Rest of method remains unchanged
        switch (cmd.getCommand()) {
            case Commands.TRANSPORT_START -> {
                isPlaying = true;
                masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                reset();
            }
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
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
     *
     * @param drumIndex The drum pad index to play
     * @param velocity  The velocity to play the note with
     */
    public void playDrumNote(int drumIndex, int velocity) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index: {}", drumIndex);
            return;
        }

        Player Player = players[drumIndex];
        if (Player == null) {
            logger.debug("No Player assigned to drum pad {}", drumIndex);
            return;
        }

        if (Player.getInstrument() == null) {
            Player.setInstrument(instruments[drumIndex]);
            Player.setChannel(9);
        }

        if (Player.getInstrument() == null) {
            logger.debug("No instrument assigned to Player for drum pad {}", drumIndex);
            return;
        }

        int noteNumber = Player.getRootNote();
        try {
            // Use the Player's instrument directly to play the note
            Player.noteOn(noteNumber, velocity);

            // Still notify listeners for UI updates
            if (noteEventListener != null) {
                noteEventListener.accept(new NoteEvent(noteNumber, velocity, 0));
            }
        } catch (Exception e) {
            logger.error("Error playing drum note: {}", e.getMessage(), e);
        }
    }

    /**
     * Process timing updates and trigger notes with nudge delays
     *
     * @param update The timing update
     */
    private void processTimingUpdate(TimingUpdate update) {
        // For each drum
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Skip if we're past the next step tick for this drum
            if (update.tickCount() >= nextStepTick[drumIndex]) {
                // Get the current step for this drum
                int stepIndex = currentStep[drumIndex];

                // Check if the step is active in the pattern
                if (patterns[drumIndex][stepIndex]) {
                    // Get step parameters
                    int velocity = stepVelocities[drumIndex][stepIndex];
                    int probability = stepProbabilities[drumIndex][stepIndex];
                    int decay = stepDecays[drumIndex][stepIndex];
                    int nudge = stepNudges[drumIndex][stepIndex];

                    // Apply probability - only play note if random value is below probability
                    // percentage
                    if (Math.random() * 100 < probability) {
                        // Apply velocity scaling using the drum's overall velocity
                        int finalVelocity = (int) (velocity * (velocities[drumIndex] / 127.0));

                        // Get the player for this drum
                        Player player = players[drumIndex];

                        // Only play if player exists, has instrument, and velocity is > 0
                        if (player != null && player.getInstrument() != null && finalVelocity > 0) {
                            // Get the note number
                            int noteNumber = drumIndex + MIDI_DRUM_NOTE_OFFSET; // Default MIDI mapping, adjust as
                                                                                // needed

                            // Apply nudge delay if specified
                            if (nudge > 0) {
                                // Schedule delayed note using executor service
                                SHARED_NOTE_SCHEDULER.schedule(() -> {
                                    // Trigger the note with specified parameters after delay
                                    player.noteOn(noteNumber, finalVelocity, decay);

                                    // Log for debugging
                                    // logger.debug("Triggered delayed drum {}: step={}, nudge={}ms, vel={},
                                    // decay={}",
                                    // drumIndex, stepIndex, nudge, finalVelocity, decay);
                                }, nudge, java.util.concurrent.TimeUnit.MILLISECONDS);
                            } else {
                                // No nudge, play immediately
                                player.noteOn(noteNumber, finalVelocity, decay);

                                // Log for debugging
                                logger.debug("Triggered drum {}: step={}, vel={}, decay={}",
                                        drumIndex, stepIndex, finalVelocity, decay);
                            }
                        }
                    } else {
                        // Note skipped due to probability
                        logger.debug("Drum {} step {} skipped due to probability: {}/100",
                                drumIndex, stepIndex, probability);
                    }
                }

                // Update step counter for this drum
                advanceStepForDrum(drumIndex);

                // Calculate next step time
                calculateNextStepTime(drumIndex);
            }
        }
    }

    /**
     * Send a MIDI CC message to the instrument's device if it exists
     * 
     * @param player The player whose instrument will receive the message
     * @param cc     The MIDI CC number
     * @param value  The value to send (0-127)
     * @return true if successful
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
                reuseableMessage.setMessage(
                        javax.sound.midi.ShortMessage.CONTROL_CHANGE,
                        channel,
                        cc,
                        value);
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
     * Batch effect messages to send only when needed
     * 
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @param player    The player object
     */
    private void sendEffectsIfNeeded(int drumIndex, int stepIndex, Player player) {
        // Only send effects for active steps to reduce overhead
        if (!patterns[drumIndex][stepIndex]) {
            return;
        }

        // Get all effect values
        int pan = stepPans[drumIndex][stepIndex];
        int chorus = stepChorus[drumIndex][stepIndex];
        int reverb = stepReverb[drumIndex][stepIndex];
        int decay = stepDecays[drumIndex][stepIndex];

        // Only check global "effect needs sending" flag to reduce comparisons
        boolean shouldSendEffects = false;

        // Compare against previous values more efficiently
        if (pan != lastSentPans[drumIndex][stepIndex]) {
            shouldSendEffects = true;
            lastSentPans[drumIndex][stepIndex] = pan;
        }

        // Only call sendMidiCC once if needed
        if (shouldSendEffects && player.getInstrument() != null) {
            // Send just the pan effect since that's the primary one
            sendMidiCC(player, CC_PAN, pan);

            // Only send these additional effects when absolutely necessary
            // For now, disable these to reduce MIDI traffic
            // sendMidiCC(player, CC_CHORUS, chorus);
            // sendMidiCC(player, CC_REVERB, reverb);
            // sendMidiCC(player, CC_DELAY, decay);
        }
    }

    // This method processes effects for a single step
    private void processEffects(int drumIndex, int stepIndex, Player player) {
        // Skip if the step is inactive or player has no instrument
        if (!patterns[drumIndex][stepIndex] || player == null ||
                player.getInstrument() == null) {
            return;
        }

        try {
            // Get current effect values
            int pan = stepPans[drumIndex][stepIndex];
            int reverb = stepReverb[drumIndex][stepIndex];
            int chorus = stepChorus[drumIndex][stepIndex];
            int decay = stepDecays[drumIndex][stepIndex];

            // Count how many effects need to be sent
            int effectCount = 0;

            // Only add effects that have changed
            if (pan != lastPanValues[drumIndex][stepIndex]) {
                effectControllers[effectCount] = CC_PAN;
                effectValues[effectCount] = pan;
                lastPanValues[drumIndex][stepIndex] = pan;
                effectCount++;
            }

            if (reverb != lastReverbValues[drumIndex][stepIndex]) {
                effectControllers[effectCount] = CC_REVERB;
                effectValues[effectCount] = reverb;
                lastReverbValues[drumIndex][stepIndex] = reverb;
                effectCount++;
            }

            if (chorus != lastChorusValues[drumIndex][stepIndex]) {
                effectControllers[effectCount] = CC_CHORUS;
                effectValues[effectCount] = chorus;
                lastChorusValues[drumIndex][stepIndex] = chorus;
                effectCount++;
            }

            if (decay != lastDecayValues[drumIndex][stepIndex]) {
                effectControllers[effectCount] = CC_DELAY; // Using delay CC for decay
                effectValues[effectCount] = decay;
                lastDecayValues[drumIndex][stepIndex] = decay;
                effectCount++;
            }

            // Send effects only if needed
            if (effectCount > 0) {
                int channel = player.getChannel();
                int[] controllers = Arrays.copyOf(effectControllers, effectCount);
                int[] values = Arrays.copyOf(effectValues, effectCount);

                player.getInstrument().sendBulkCC(controllers, values);
            }
        } catch (Exception e) {
            // Just ignore errors to avoid performance impact
        }
    }

}
package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.angrysurfer.core.service.InternalSynthManager;
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
    private static final int MAX_STEPS = 64;      // Maximum pattern length

    // Global sequencing state
    private long tickCounter = 0;            // Count ticks
    private int beatCounter = 0;            // Count beats  
    private int ticksPerStep = 24;          // Base ticks per step
    private boolean isPlaying = false;      // Global play state

    private long drumSequenceId = -1;

    // Per-drum sequencing state
    private int[] currentStep;              // Current step for each drum
    private boolean[] patternCompleted;     // Pattern completion flag for each drum
    private long[] nextStepTick;             // Next step trigger tick for each drum

    // Per-drum pattern parameters
    private int[] patternLengths;           // Pattern length for each drum
    private Direction[] directions;         // Direction for each drum
    private TimingDivision[] timingDivisions; // Timing for each drum
    private boolean[] loopingFlags;         // Loop setting for each drum
    private int[] bounceDirections;         // 1 for forward, -1 for backward (for bounce mode)
    private int[] velocities;               // Velocity for each drum
    private int[] originalVelocities;       // Saved original velocities for resetting

    // Pattern data storage
    private boolean[][] patterns;           // [drumIndex][stepIndex]

    // Per-step parameter values for each drum
    private int[][] stepVelocities;      // Velocity for each step of each drum [drumIndex][stepIndex]
    private int[][] stepDecays;          // Decay (gate time) for each step [drumIndex][stepIndex]
    private int[][] stepProbabilities;   // Probability for each step [drumIndex][stepIndex]
    private int[][] stepNudges;          // Timing nudge for each step [drumIndex][stepIndex]

    // Strike objects for each drum pad
    private Strike[] players;
    private InstrumentWrapper[] instruments;

    // Selection state
    private int selectedPadIndex = 0;       // Currently selected drum pad

    // Event handling
    private Consumer<DrumStepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    private int masterTempo;

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
        Arrays.fill(velocities, 100);  // Default to 100
        Arrays.fill(originalVelocities, 100);

        // Default values
        Arrays.fill(patternLengths, 16);      // Default to 16 steps
        Arrays.fill(directions, Direction.FORWARD);  // Default to forward
        Arrays.fill(timingDivisions, TimingDivision.NORMAL); // Default timing
        Arrays.fill(loopingFlags, true);      // Default to looping
        Arrays.fill(bounceDirections, 1);     // Default to forward bounce

        // Initialize masterTempo with default value
        masterTempo = 96; // Default PPQN if not set by session

        // Initialize patterns with max possible length
        patterns = new boolean[DRUM_PAD_COUNT][MAX_STEPS];
        stepVelocities = new int[DRUM_PAD_COUNT][MAX_STEPS];
        stepDecays = new int[DRUM_PAD_COUNT][MAX_STEPS];
        stepProbabilities = new int[DRUM_PAD_COUNT][MAX_STEPS];
        stepNudges = new int[DRUM_PAD_COUNT][MAX_STEPS];

        // Set default values
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < MAX_STEPS; j++) {
                stepVelocities[i][j] = 100;  // Default velocity at 100
                stepDecays[i][j] = 60;       // Default decay at 60
                stepProbabilities[i][j] = 100; // Default probability at 100% (always play)
                stepNudges[i][j] = 0;        // Default nudge at 0 (no offset)
            }
        }

        // Get internal synthesizer from InternalSynthManager
        javax.sound.midi.Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();

        // Initialize strikes with proper configuration
        instruments = new InstrumentWrapper[DRUM_PAD_COUNT];
        players = new Strike[DRUM_PAD_COUNT];

        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            players[i] = new Strike();
            players[i].setName("Drum " + (i + 1));
            // Set default root notes - standard GM drum map starting points
            players[i].setRootNote(36 + i); // Start from MIDI note 36 (C1)
            // Configure to use drum channel (9)
            players[i].setChannel(9);

            // Create an InstrumentWrapper for the internal synth
            instruments[i] = new InstrumentWrapper(players[i].getName(), synth);
            instruments[i].setDeviceName(synth.getDeviceInfo().getName());
            instruments[i].setChannels(new Integer[]{9}); // Drum channel
            instruments[i].setInternal(true);

            logger.info("Created internal synth instrument for drum sequencer: {}", instruments[i].getName());

            // Assign the internal synth instrument
            players[i].setInstrument(instruments[i]);

            logger.debug("Initialized drum pad {} with note {}", i, 36 + i);
        }

        // Register with command bus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Load first saved sequence (if available) instead of default pattern
        loadFirstSequence();
    }

    /**
     * Load the first available sequence from storage
     */
    private void loadFirstSequence() {
        try {
            // Get the manager
            DrumSequencerManager manager = DrumSequencerManager.getInstance();

            // Get the first sequence ID
            Long firstId = manager.getFirstSequenceId();

            if (firstId != null) {
                // Load the sequence
                boolean loaded = manager.loadSequence(firstId, this);
                if (loaded) {
                    logger.info("Loaded initial drum sequence: {}", firstId);
                    // Publish event to notify UI components
                    CommandBus.getInstance().publish(
                            Commands.DRUM_SEQUENCE_LOADED,
                            this,
                            drumSequenceId
                    );
                } else {
                    logger.warn("Failed to load initial drum sequence");
                }
            } else {
                logger.info("No saved drum sequences found, using empty pattern");
            }
        } catch (Exception e) {
            logger.error("Error loading initial drum sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a timing tick - now handles each drum separately
     *
     * @param tick The current tick count
     */
    public void processTick(long tick) {
        if (!isPlaying) {
            return;
        }

        tickCounter = tick;

        // Process each drum separately
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Skip if no strike configured
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
                    step
            );
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
        // First check if the step is active
        if (patterns[drumIndex][stepIndex]) {
            // Get the per-step parameters
            int velocity = stepVelocities[drumIndex][stepIndex];
            int probability = stepProbabilities[drumIndex][stepIndex];

            // Check probability - only play if random number is less than probability
            if (Math.random() * 100 < probability) {
                // Apply global velocity scaling
                int finalVelocity = (int) (velocity * (velocities[drumIndex] / 127.0));

                // Add these safety checks before playing the note
                if (finalVelocity > 0 && drumIndex >= 0 && drumIndex < players.length) {
                    Strike player = players[drumIndex];

                    // Check if player exists
                    if (player != null) {
                        // Check if player has a valid instrument before trying to play
                        if (player.getInstrument() == null) {
                            player.setInstrument(instruments[drumIndex]);
                            player.setChannel(9);
                        }

                        if (player.getInstrument() != null) {
                            // Now safe to trigger the note
                            int decay = stepDecays[drumIndex][stepIndex];
                            int nudge = stepNudges[drumIndex][stepIndex];

                            // Apply nudge delay if specified
                            if (nudge > 0) {
                                // Create final copies of variables used in lambda
                                final int finalNoteNumber = player.getRootNote();
                                final int finalVelocityCopy = finalVelocity;
                                final int finalDecay = decay;
                                final int finalDrumIndex = drumIndex;
                                final int finalStepIndex = stepIndex;
                                final int finalNudge = nudge;

                                // Schedule delayed note using executor service
                                java.util.concurrent.ScheduledExecutorService scheduler =
                                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

                                scheduler.schedule(() -> {
                                    // Use final copies inside lambda
                                    player.noteOn(finalNoteNumber, finalVelocityCopy, finalDecay);

                                    // Log for debugging
                                    logger.debug("Triggered delayed drum {}: step={}, nudge={}ms, vel={}, decay={}",
                                            finalDrumIndex, finalStepIndex, finalNudge, finalVelocityCopy, finalDecay);

                                    // Shutdown the scheduler since we're done with it
                                    scheduler.shutdown();
                                }, nudge, java.util.concurrent.TimeUnit.MILLISECONDS);
                            } else {
                                player.noteOn(player.getRootNote(), finalVelocity, decay);
                            }
                        } else {
                            // Log warning about missing instrument
                            logger.warn("No instrument assigned to player for drum " + drumIndex);
                        }
                    } else {
                        // Log warning about missing player
                        logger.warn("No player assigned for drum " + drumIndex);
                    }
                }
            }
        }
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
            logger.warn("Invalid masterTempo value ({}), using default of 96", masterTempo);
            masterTempo = 96;  // Emergency fallback
        }

        double ticksPerBeat = timing.getTicksPerBeat();
        if (ticksPerBeat <= 0) {
            logger.warn("Invalid ticksPerBeat value ({}), using default of 24", ticksPerBeat);
            ticksPerBeat = 24.0;  // Emergency fallback
        }

        // Simplified calculation that works consistently
        int result = (int) (masterTempo / (ticksPerBeat / 24.0));

        // Add safety check for the final result
        if (result <= 0) {
            logger.warn("Calculated invalid ticksPerStep ({}), using default of 24", result);
            result = 24;  // Emergency fallback for extreme values
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
     * Reset the sequencer to start position
     */
    public void reset() {
        // Reset all step tracking arrays
        Arrays.fill(currentStep, 0);
        Arrays.fill(patternCompleted, false);
        Arrays.fill(nextStepTick, 0);
        Arrays.fill(bounceDirections, 1);

        // Reset global counters
        tickCounter = 0;
        beatCounter = 0;

        // Force the sequencer to generate an event to update visual indicators
        if (stepUpdateListener != null) {
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                stepUpdateListener.accept(new DrumStepUpdateEvent(drumIndex, -1, currentStep[drumIndex]));
            }
        }

        logger.debug("Sequencer fully reset - all counters and indicators cleared");
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
    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            reset();
            CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this);
        }
    }

    /**
     * Toggle a pattern step for a drum pad
     */
    public void toggleStep(int drumIndex, int step) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT
                && step >= 0 && step < MAX_STEPS) {
            patterns[drumIndex][step] = !patterns[drumIndex][step];
        }
    }

    // Getter and setter methods for per-drum parameters
    public int getPatternLength(int drumIndex) {
        // Add bounds check
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getPatternLength", drumIndex);
            return 16; // Return default
        }
        return patternLengths[drumIndex];
    }

    public void setPatternLength(int drumIndex, int length) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT
                && length > 0 && length <= MAX_STEPS) {
            logger.info("Setting pattern length for drum {} to {}", drumIndex, length);
            patternLengths[drumIndex] = length;

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex
            );
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
                    drumIndex
            );
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
                    drumIndex
            );
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
                    drumIndex
            );
        }
    }

    public int getVelocity(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            return velocities[drumIndex];
        }
        return 100; // Default value
    }

    public void setVelocity(int drumIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            // Constrain to valid MIDI range
            velocity = Math.max(0, Math.min(127, velocity));
            velocities[drumIndex] = velocity;

            // If we have a Strike object for this drum, update its level
            Strike strike = getStrike(drumIndex);
            if (strike != null) {
                strike.setLevel(velocity);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_PARAMS_CHANGED,
                    this,
                    drumIndex
            );
        }
    }

    public int getStepVelocity(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            return stepVelocities[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepVelocity(int drumIndex, int stepIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            stepVelocities[drumIndex][stepIndex] = velocity;
        }
    }

    public int getStepDecay(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            return stepDecays[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepDecay(int drumIndex, int stepIndex, int decay) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            stepDecays[drumIndex][stepIndex] = decay;
        }
    }

    public int getStepProbability(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            return stepProbabilities[drumIndex][stepIndex];
        }
        return 100; // Default to 100% if out of bounds
    }

    public void setStepProbability(int drumIndex, int stepIndex, int probability) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            // Clamp value between 0-100
            stepProbabilities[drumIndex][stepIndex] = Math.max(0, Math.min(100, probability));
        }
    }

    public int getStepNudge(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            return stepNudges[drumIndex][stepIndex];
        }
        return 0;
    }

    public void setStepNudge(int drumIndex, int stepIndex, int nudge) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            stepNudges[drumIndex][stepIndex] = nudge;
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
    public int getPatternLength() {
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
                    new DrumPadSelectionEvent(oldSelection, selectedPadIndex)
            );
        } else {
            logger.warn("Invalid drum pad index: {}", padIndex);
        }
    }

    /**
     * Get the Strike object for a specific drum pad
     *
     * @param drumIndex The index of the drum pad (0-15)
     * @return The Strike object or null if not set
     */
    public Strike getStrike(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            return players[drumIndex];
        }
        return null;
    }

    /**
     * Set the Strike object for a specific drum pad
     *
     * @param drumIndex The index of the drum pad (0-15)
     * @param strike The Strike object to associate with the drum pad
     */
    public void setStrike(int drumIndex, Strike strike) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            players[drumIndex] = strike;
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
                && stepIndex >= 0 && stepIndex < MAX_STEPS) {
            return patterns[drumIndex][stepIndex];
        }
        return false;
    }

    /**
     * Clear the pattern for all drum pads
     */
    public void clearPattern() {
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < MAX_STEPS; step++) {
                patterns[drumIndex][step] = false;
            }
        }
        logger.info("All patterns cleared");
    }

    /**
     * Generate a simple pattern for the specified drum
     *
     * @param drumIndex The index of the drum pad to generate a pattern for
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
        int hitsToAdd = Math.max(1, Math.min(10, density)) * length / 10;

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
                selectedPadIndex
        );

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
     * Required by IBusListener interface
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                isPlaying = true;
                masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                reset();
            }
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                reset();
            }
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    if (update.tick() != null) {
                        processTick(update.tick());
                    }
                }
            }
            case Commands.UPDATE_TEMPO -> {
                if (action.getData() instanceof Integer ticksPerBeat) {
                    updateMasterTempo(ticksPerBeat);
                } else if (action.getData() instanceof Float) {
                    // If BPM is sent instead, get ticksPerBeat from session
                    int tpb = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                    updateMasterTempo(tpb);
                }
            }
        }
    }

    /**
     * Play a drum note using the Strike for the specified drum pad
     *
     * @param drumIndex The drum pad index to play
     * @param velocity The velocity to play the note with
     */
    public void playDrumNote(int drumIndex, int velocity) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index: {}", drumIndex);
            return;
        }

        Strike strike = players[drumIndex];
        if (strike == null) {
            logger.debug("No Strike assigned to drum pad {}", drumIndex);
            return;
        }

        if (strike.getInstrument() == null) {
            strike.setInstrument(instruments[drumIndex]);
            strike.setChannel(9);
        }

        if (strike.getInstrument() == null) {
            logger.debug("No instrument assigned to Strike for drum pad {}", drumIndex);
            return;
        }

        int noteNumber = strike.getRootNote();
        try {
            // Use the Strike's instrument directly to play the note
            strike.noteOn(noteNumber, velocity);

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

                    // Apply probability - only play note if random value is below probability percentage
                    if (Math.random() * 100 < probability) {
                        // Apply velocity scaling using the drum's overall velocity
                        int finalVelocity = (int) (velocity * (velocities[drumIndex] / 127.0));

                        // Get the player for this drum
                        Strike player = players[drumIndex];

                        // Only play if player exists, has instrument, and velocity is > 0
                        if (player != null && player.getInstrument() != null && finalVelocity > 0) {
                            // Get the note number
                            int noteNumber = drumIndex + 36; // Default MIDI mapping, adjust as needed

                            // Apply nudge delay if specified
                            if (nudge > 0) {
                                // Schedule delayed note using executor service
                                java.util.concurrent.ScheduledExecutorService scheduler =
                                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

                                scheduler.schedule(() -> {
                                    // Trigger the note with specified parameters after delay
                                    player.noteOn(noteNumber, finalVelocity, decay);

                                    // Log for debugging
                                    // logger.debug("Triggered delayed drum {}: step={}, nudge={}ms, vel={}, decay={}",
                                    //         drumIndex, stepIndex, nudge, finalVelocity, decay);

                                    // Shutdown the scheduler since we're done with it
                                    scheduler.shutdown();
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
}

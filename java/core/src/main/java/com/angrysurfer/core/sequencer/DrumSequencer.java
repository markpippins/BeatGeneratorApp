package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;

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
    private static final int DRUM_PAD_COUNT = 16; // Number of drum pads
    private static final int MAX_STEPS = 64;      // Maximum pattern length

    // Global sequencing state
    private int tickCounter = 0;            // Count ticks
    private int beatCounter = 0;            // Count beats  
    private int ticksPerStep = 24;          // Base ticks per step
    private boolean isPlaying = false;      // Global play state

    // Per-drum sequencing state
    private int[] currentStep;              // Current step for each drum
    private boolean[] patternCompleted;     // Pattern completion flag for each drum
    private int[] nextStepTick;             // Next step trigger tick for each drum

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

    // Strike objects for each drum pad
    private Strike[] strikes;

    // Selection state
    private int selectedPadIndex = 0;       // Currently selected drum pad

    // Event handling
    private Consumer<DrumStepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    /**
     * Creates a new drum sequencer with per-drum parameters
     */
    public DrumSequencer() {
        // Initialize arrays
        currentStep = new int[DRUM_PAD_COUNT];
        patternCompleted = new boolean[DRUM_PAD_COUNT];
        nextStepTick = new int[DRUM_PAD_COUNT];

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

        // Initialize patterns with max possible length
        patterns = new boolean[DRUM_PAD_COUNT][MAX_STEPS];

        // Initialize strikes
        strikes = new Strike[DRUM_PAD_COUNT];

        // Register with command bus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Initialize default pattern
        initializeDefaultPattern();
    }

    /**
     * Process a timing tick - now handles each drum separately
     *
     * @param tick The current tick count
     */
    public void processTick(int tick) {
        if (!isPlaying) {
            return;
        }

        tickCounter = tick;

        // Process each drum separately
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Skip if no strike configured
            if (strikes[drumIndex] == null) {
                continue;
            }

            // Check if it's time for next step for this drum
            if (tick >= nextStepTick[drumIndex]) {
                // Calculate ticks for this drum's timing division
                int drumTicksPerStep = calculateTicksPerStep(timingDivisions[drumIndex]);

                // Set next step tick
                nextStepTick[drumIndex] = tick + drumTicksPerStep;

                // Handle if pattern is completed and not looping
                if (patternCompleted[drumIndex] && !loopingFlags[drumIndex]) {
                    continue; // Skip this drum
                }

                // Process the current step for this drum
                processStep(drumIndex);
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
        
        // Check if this step is active
        if (patterns[drumIndex][step]) {
            // Get the Strike for this drum
            Strike strike = strikes[drumIndex];
            
            // If we have a valid Strike and note event listener, trigger the note
            if (strike != null && noteEventListener != null) {
                int note = strike.getRootNote();
                
                // Use the velocity from the velocities array
                int velocity = velocities[drumIndex];
                
                // Create a note event with the note and velocity
                NoteEvent event = new NoteEvent(note, velocity, 100);
                noteEventListener.accept(event);
            }
        }
        
        // Calculate next step
        calculateNextStep(drumIndex);
        
        // Notify listeners of step update
        if (stepUpdateListener != null) {
            int oldStep = step;
            int newStep = currentStep[drumIndex];
            stepUpdateListener.accept(new DrumStepUpdateEvent(drumIndex, oldStep, newStep));
        }
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
     * Calculate ticks per step based on timing division
     */
    private int calculateTicksPerStep(TimingDivision timing) {
        // Use the ticksPerBeat value directly from the enum
        return (ticksPerStep * 4) / timing.getTicksPerBeat();
    }

    /**
     * Reset the sequencer to start position
     */
    public void reset() {
        Arrays.fill(currentStep, 0);
        Arrays.fill(patternCompleted, false);
        Arrays.fill(nextStepTick, 0);
        Arrays.fill(bounceDirections, 1);
        tickCounter = 0;
        beatCounter = 0;
    }

    /**
     * Start playback
     */
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            reset();
            CommandBus.getInstance().publish(Commands.TRANSPORT_START, this);
        }
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
                    Commands.PATTERN_PARAMS_CHANGED,
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
                    Commands.PATTERN_PARAMS_CHANGED,
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
                    Commands.PATTERN_PARAMS_CHANGED,
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
                    Commands.PATTERN_PARAMS_CHANGED,
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
                    Commands.PATTERN_PARAMS_CHANGED,
                    this,
                    drumIndex
            );
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
            return strikes[drumIndex];
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
            strikes[drumIndex] = strike;
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
                Commands.PATTERN_PARAMS_CHANGED,
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
        // Handle commands as needed
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                isPlaying = true;
                reset();
            }
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                reset();
            }
            case Commands.TIMING_UPDATE -> {
                // Process timing update
                if (isPlaying && action.getData() instanceof TimingUpdate timingUpdate) {
                    if (timingUpdate.tick() != null) {
                        // Process the tick
                        processTick(timingUpdate.tickCount().intValue());

                        if (timingUpdate.beat() > beatCounter) {
                            // Notify UI of status update every 4 ticks
                            int step = currentStep[selectedPadIndex];
                            int patternLength = patternLengths[selectedPadIndex];
                            logger.debug("Status update: Step {} of {}", step + 1, patternLength);

                            // Publish status update to command bus
                            CommandBus.getInstance().publish(Commands.STATUS_UPDATE, this,
                                    new StatusUpdate("Step: " + (step + 1) + " of " + patternLength));

                            beatCounter++;
                        }

                    }
                }
            }
        }
    }
}

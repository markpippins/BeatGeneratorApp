package com.angrysurfer.core.sequencer;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.MelodicSequencer.StepUpdateEvent;

import lombok.Getter;
import lombok.Setter;

/**
 * Core sequencer engine that handles drum pattern sequencing and playback. This
 * class is responsible for managing patterns, timing, and triggering notes.
 */
@Getter
@Setter
public class DrumSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencer.class);

    // Sequencing state
    private int currentStep = 0;            // Current step being played
    private int beat = 0;                   // Current beat counter
    private int tickCounter = 0;            // Count ticks within current step
    private int ticksPerStep = 24;          // How many ticks make one step
    private int nextStepTick = 0;           // When to trigger the next step
    private boolean patternCompleted = false; // Flag for pattern completion

    // Pattern parameters
    private int patternLength = 16;         // Number of steps in pattern (default 16)
    private boolean isLooping = true;       // Whether pattern should loop
    private Direction direction = Direction.FORWARD; // Playback direction
    private boolean bounceForward = true;   // Used for bounce direction

    // Constants
    private static final int DRUM_PAD_COUNT = 16; // Number of drum pads

    // Timing parameters
    private TimingDivision timingDivision = TimingDivision.NORMAL;
    private boolean isPlaying = false;

    // Pattern data storage - for each pad, store pattern steps
    private boolean[][] patterns = new boolean[DRUM_PAD_COUNT][16]; // Default 16 steps

    // Strike objects for each drum pad
    private Strike[] strikes = new Strike[DRUM_PAD_COUNT];

    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    // Selected pad tracking
    private int selectedPadIndex = 0;

    /**
     * Creates a new drum sequencer
     */
    public DrumSequencer() {
        // Initialize strikes array with null values
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            strikes[i] = null;
        }

        // Register with CommandBus to receive all commands including timing updates
        CommandBus.getInstance().register(this);

        // ALSO register with TimingBus for timing updates
        TimingBus.getInstance().register(this);

        // Initialize default pattern
        initializeDefaultPattern();

        logger.info("DrumSequencer initialized and registered with CommandBus and TimingBus");
    }

    /**
     * Initialize with a simple default pattern
     */
    private void initializeDefaultPattern() {
        // Clear all patterns
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < patternLength; step++) {
                patterns[drumIndex][step] = false;
            }
        }

        // Set up a basic drum pattern
        // Kick drum on every quarter note (steps 0, 4, 8, 12)
        patterns[0][0] = true;
        patterns[0][4] = true;
        patterns[0][8] = true;
        patterns[0][12] = true;

        // Snare on beats 2 and 4 (steps 4 and 12)
        patterns[1][4] = true;
        patterns[1][12] = true;

        // Closed hi-hat on every eighth note (even steps)
        for (int i = 0; i < patternLength; i += 2) {
            patterns[2][i] = true;
        }

        // Log that we've created a default pattern
        logger.info("Created default drum pattern");
    }

    /**
     * Process commands from the command bus
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    // Process tick for step sequencing
                    tickCounter++;

                    // Add debug logging to see if we're getting ticks
                    if (tickCounter % 24 == 0) {
                        logger.debug("Drum sequencer tick: {}, nextStepTick: {}", tickCounter, nextStepTick);
                    }

                    if (tickCounter >= nextStepTick) {
                        processStepAdvance();
                    }
                }
            }
            case Commands.TRANSPORT_START -> {
                resetSequence();
                isPlaying = true;
                logger.info("Drum sequencer started");
            }
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                // Notify UI that we stopped (with -1 as new step to indicate no active step)
                notifyStepUpdate(currentStep, -1);
                logger.info("Drum sequencer stopped");
            }
        }
    }

    /**
     * Process step advance - calculate next step and trigger notes if needed
     */
    private void processStepAdvance() {
        // Remember current step for highlighting
        int oldStep = currentStep;

        // Calculate the next step based on direction
        int nextStep = calculateNextStep(currentStep, patternLength, direction);

        // Update step counter
        currentStep = nextStep;

        // Add debug logging
        logger.debug("Drum sequencer advancing step: {} -> {}", oldStep, currentStep);

        // Notify UI of step change
        notifyStepUpdate(oldStep, currentStep);

        // Check if notes should be triggered for this step
        triggerNotesForStep(currentStep);

        // Reset tick counter and calculate next step time
        tickCounter = 0;
        nextStepTick = ticksPerStep;
    }

    /**
     * Trigger notes for the current step
     *
     * @param step Current step index
     */
    private void triggerNotesForStep(int step) {
        // Trigger all active drum pads for this step
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            if (step < patternLength && patterns[drumIndex][step]) {
                Strike strike = strikes[drumIndex];
                if (strike != null) {
                    // Create note event
                    NoteEvent noteEvent = new NoteEvent(
                            strike.getRootNote(),
                            127, // Full velocity 
                            250 // Standard decay time
                    );

                    // Trigger note via listener
                    if (noteEventListener != null) {
                        noteEventListener.accept(noteEvent);
                    }
                }
            }
        }
    }

    /**
     * Reset the sequence to initial state
     */
    public void resetSequence() {
        currentStep = 0;
        beat = 0;
        tickCounter = 0;
        patternCompleted = false;
        nextStepTick = ticksPerStep;
    }

    /**
     * Notifies listeners about step updates
     */
    private void notifyStepUpdate(int oldStep, int newStep) {
        // First, notify any direct listeners
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(oldStep, newStep));
        }

        // Then publish on the command bus for any other components
        CommandBus.getInstance().publish(
                Commands.SEQUENCER_STEP_UPDATE,
                this,
                new StepUpdateEvent(oldStep, newStep)
        );
    }

    /**
     * Calculate the next step based on the current direction
     */
    private int calculateNextStep(int currentStep, int patternLength, Direction direction) {
        switch (direction) {
            case FORWARD:
                return (currentStep + 1) % patternLength;

            case BACKWARD:
                return (currentStep - 1 + patternLength) % patternLength;

            case BOUNCE:
                if (bounceForward) {
                    int nextStep = currentStep + 1;
                    if (nextStep >= patternLength - 1) {
                        bounceForward = false;
                        return Math.max(0, patternLength - 1);
                    }
                    return nextStep;
                } else {
                    int nextStep = currentStep - 1;
                    if (nextStep <= 0) {
                        bounceForward = true;
                        return 0;
                    }
                    return nextStep;
                }

            case RANDOM:
                return (int) (Math.random() * patternLength);

            default:
                return (currentStep + 1) % patternLength;
        }
    }

    /**
     * Check if the pattern has ended based on current and next step
     */
    private boolean hasPatternEnded(int currentStep, int nextStep, int patternLength, Direction direction) {
        switch (direction) {
            case FORWARD:
                return currentStep == patternLength - 1 && nextStep == 0;

            case BACKWARD:
                return currentStep == 0 && nextStep == patternLength - 1;

            case BOUNCE:
                // In bounce mode, we consider the pattern ended when
                // we reach either end and change direction
                if (bounceForward) {
                    return currentStep == patternLength - 2 && nextStep == patternLength - 1;
                } else {
                    return currentStep == 1 && nextStep == 0;
                }

            case RANDOM:
                // For random mode, pattern only ends at the last step index
                return currentStep == patternLength - 1;

            default:
                return false;
        }
    }

    /**
     * Set the state of a specific step for a drum pad
     *
     * @param drumPadIndex The drum pad index (0-15)
     * @param stepIndex The step index (0-15)
     * @param active Whether the step should be active
     */
    public void setStep(int drumPadIndex, int stepIndex, boolean active) {
        if (drumPadIndex >= 0 && drumPadIndex < DRUM_PAD_COUNT
                && stepIndex >= 0 && stepIndex < patternLength) {
            patterns[drumPadIndex][stepIndex] = active;
        }
    }

    /**
     * Check if a step is active for a specific drum pad
     *
     * @param drumPadIndex The drum pad index (0-15)
     * @param stepIndex The step index (0-15)
     * @return true if the step is active, false otherwise
     */
    public boolean isStepActive(int drumPadIndex, int stepIndex) {
        if (drumPadIndex >= 0 && drumPadIndex < DRUM_PAD_COUNT
                && stepIndex >= 0 && stepIndex < patternLength) {
            return patterns[drumPadIndex][stepIndex];
        }
        return false;
    }

    /**
     * Get the pattern data for all drum pads
     *
     * @return 2D boolean array of pattern data
     */
    public boolean[][] getPatterns() {
        return patterns;
    }

    /**
     * Clear the entire pattern for all drum pads
     */
    public void clearPattern() {
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < patternLength; step++) {
                patterns[drumIndex][step] = false;
            }
        }
        logger.info("Pattern cleared");
    }

    /**
     * Generate a random pattern with given density
     *
     * @param density Percentage (0-100) of steps to activate
     */
    public void generatePattern(int density) {
        // Clear existing pattern
        clearPattern();

        logger.info("Generating pattern with {}% density", density);

        // Generate for each drum and step
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Skip if this drum doesn't have a Strike
            if (strikes[drumIndex] == null) {
                continue;
            }

            // Different probability for different drums
            int drumDensity = density;
            if (drumIndex == 0) {
                drumDensity = Math.min(50, density); // Less kicks

                        }if (drumIndex == 1) {
                drumDensity = Math.min(40, density); // Less snares

                        }if (drumIndex == 2) {
                drumDensity = Math.min(70, density); // More hi-hats
            }
            for (int step = 0; step < patternLength; step++) {
                // Determine if step should be active based on density (0-100)
                boolean activateStep = Math.random() * 100 < drumDensity;
                patterns[drumIndex][step] = activateStep;

                // Always put kick on downbeat for first drum
                if (drumIndex == 0 && (step == 0 || step == 8)) {
                    patterns[drumIndex][step] = true;
                }

                // Always put snare on beats 5 and 13 if it's the second drum
                if (drumIndex == 1 && (step == 4 || step == 12)) {
                    patterns[drumIndex][step] = true;
                }
            }
        }
    }

    /**
     * Set a strike object for a specific drum pad
     *
     * @param drumPadIndex The drum pad index (0-15)
     * @param strike The Strike object representing this drum sound
     */
    public void setStrike(int drumPadIndex, Strike strike) {
        if (drumPadIndex >= 0 && drumPadIndex < DRUM_PAD_COUNT) {
            strikes[drumPadIndex] = strike;
        }
    }

    /**
     * Get the strike object for a specific drum pad
     *
     * @param drumPadIndex The drum pad index (0-15)
     * @return The Strike object or null if not set
     */
    public Strike getStrike(int drumPadIndex) {
        if (drumPadIndex >= 0 && drumPadIndex < DRUM_PAD_COUNT) {
            return strikes[drumPadIndex];
        }
        return null;
    }

    /**
     * Set step update listener
     *
     * @param listener The listener to call when steps change
     */
    public void setStepUpdateListener(Consumer<StepUpdateEvent> listener) {
        this.stepUpdateListener = listener;
    }

    /**
     * Set note event listener
     *
     * @param listener The listener to call when notes should be played
     */
    public void setNoteEventListener(Consumer<NoteEvent> listener) {
        this.noteEventListener = listener;
    }

    /**
     * Select a drum pad and notify listeners
     *
     * @param drumPadIndex The drum pad index to select (0-15)
     */
    public void selectDrumPad(int drumPadIndex) {
        if (drumPadIndex >= 0 && drumPadIndex < DRUM_PAD_COUNT) {
            int oldSelection = selectedPadIndex;
            selectedPadIndex = drumPadIndex;

            // Log that we're selecting a new pad
            logger.debug("DrumSequencer selecting pad: {} (was: {})", drumPadIndex, oldSelection);

            // Create the event
            DrumPadSelectionEvent event = new DrumPadSelectionEvent(oldSelection, selectedPadIndex);

            // Notify about drum selection change
            CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED, this, event);
        }
    }
}

package com.angrysurfer.core.sequencer;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.DrumStepParametersEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for modifying drum sequences without UI dependencies
 */
public class DrumSequenceModifier {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceModifier.class);
    private static final Random random = new Random();

    /**
     * Applies an Euclidean pattern to the specified drum
     *
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @param pattern   The boolean array representing the pattern
     * @return True if pattern was applied successfully, false otherwise
     */
    public static boolean applyEuclideanPattern(DrumSequencer sequencer, int drumIndex, boolean[] pattern) {
        if (pattern == null || pattern.length == 0) {
            logger.warn("Cannot apply null or empty Euclidean pattern");
            return false;
        }

        try {
            // First clear the track properly - this also resets all parameters
            clearDrumTrack(sequencer, drumIndex);

            // Set the pattern length if needed
            int newLength = pattern.length;
            sequencer.setPatternLength(drumIndex, newLength);

            // Apply pattern values (activate steps where pattern is true)
            for (int step = 0; step < pattern.length; step++) {
                if (pattern[step]) {
                    // Toggle the step to make it active
                    sequencer.toggleStep(drumIndex, step);

                    // Always set default parameters for active steps
                    sequencer.setStepVelocity(drumIndex, step, SequencerConstants.DEFAULT_VELOCITY);
                    sequencer.setStepDecay(drumIndex, step, SequencerConstants.DEFAULT_DECAY);
                    sequencer.setStepProbability(drumIndex, step, SequencerConstants.DEFAULT_PROBABILITY);

                    // Set default effect parameters too
                    sequencer.setStepPan(drumIndex, step, 64); // Center
                    sequencer.setStepChorus(drumIndex, step, 0);
                    sequencer.setStepReverb(drumIndex, step, 0);
                }
            }

            // Use our helper method to notify pattern changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied Euclidean pattern to drum {}, pattern length: {}", drumIndex, pattern.length);
            return true;
        } catch (Exception e) {
            logger.error("Error applying Euclidean pattern", e);
            return false;
        }
    }

    /**
     * Clears all steps for a specific drum track
     */
    public static boolean clearDrumTrack(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    sequencer.toggleStep(drumIndex, step);
                }

                // Reset all parameters to defaults (important for clean state)
                sequencer.setStepVelocity(drumIndex, step, SequencerConstants.DEFAULT_VELOCITY);
                sequencer.setStepDecay(drumIndex, step, SequencerConstants.DEFAULT_DECAY);
                sequencer.setStepProbability(drumIndex, step, SequencerConstants.DEFAULT_PROBABILITY);
                sequencer.setStepNudge(drumIndex, step, 0);
                sequencer.setStepPan(drumIndex, step, 64); // Center
                sequencer.setStepChorus(drumIndex, step, 0);
                sequencer.setStepReverb(drumIndex, step, 0);
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Cleared track for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error clearing drum track {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Sets maximum pattern length and truncates any patterns that exceed it
     *
     * @param sequencer The drum sequencer to modify
     * @param maxLength The new maximum pattern length
     * @return List of drum indices that were modified
     */
    public static List<Integer> applyMaxPatternLength(DrumSequencer sequencer, int maxLength) {
        List<Integer> updatedDrums = new ArrayList<>();

        try {
            // Update lengths for all drums
            for (int drumIndex = 0; drumIndex < SequencerConstants.DRUM_PAD_COUNT; drumIndex++) {
                int currentLength = sequencer.getPatternLength(drumIndex);

                // Only update drums that exceed the new maximum
                if (currentLength > maxLength) {
                    sequencer.setPatternLength(drumIndex, maxLength);
                    updatedDrums.add(drumIndex);
                    logger.debug("Updated drum {} pattern length from {} to {}",
                            drumIndex, currentLength, maxLength);
                }
            }

            logger.info("Applied max pattern length {} to {} drums", maxLength, updatedDrums.size());
        } catch (Exception e) {
            logger.error("Error applying max pattern length {}", maxLength, e);
        }

        return updatedDrums;
    }

    /**
     * Applies a pattern that activates every Nth step
     *
     * @param sequencer    The drum sequencer to modify
     * @param drumIndex    The index of the drum to update
     * @param stepInterval The interval between active steps (2 = every other step)
     * @return True if pattern was applied successfully
     */
    public static boolean applyPatternEveryN(DrumSequencer sequencer, int drumIndex, int stepInterval) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Clear existing pattern first
            clearDrumTrack(sequencer, drumIndex);

            // Set every Nth step
            for (int i = 0; i < patternLength; i += stepInterval) {
                if (!sequencer.isStepActive(drumIndex, i)) {
                    sequencer.toggleStep(drumIndex, i);
                }
            }

            logger.info("Applied 1/{} pattern to drum {}", stepInterval, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying pattern every {} steps to drum {}", stepInterval, drumIndex, e);
            return false;
        }
    }

    /**
     * Shifts a pattern forward/right by one step with parameter preservation
     */
    public static boolean pushPatternForward(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            boolean[] originalPattern = new boolean[patternLength];

            // Arrays to store all step parameters
            int[] originalVelocities = new int[patternLength];
            int[] originalDecays = new int[patternLength];
            int[] originalProbabilities = new int[patternLength];
            int[] originalNudges = new int[patternLength];
            int[] originalPans = new int[patternLength];
            int[] originalChorus = new int[patternLength];
            int[] originalReverb = new int[patternLength];

            // Save original pattern with all parameters
            for (int i = 0; i < patternLength; i++) {
                originalPattern[i] = sequencer.isStepActive(drumIndex, i);
                originalVelocities[i] = sequencer.getStepVelocity(drumIndex, i);
                originalDecays[i] = sequencer.getStepDecay(drumIndex, i);
                originalProbabilities[i] = sequencer.getStepProbability(drumIndex, i);
                originalNudges[i] = sequencer.getStepNudge(drumIndex, i);
                originalPans[i] = sequencer.getStepPan(drumIndex, i);
                originalChorus[i] = sequencer.getStepChorus(drumIndex, i);
                originalReverb[i] = sequencer.getStepReverb(drumIndex, i);
            }

            // Shift pattern right (wrap around for last step)
            boolean lastStepState = originalPattern[patternLength - 1];
            int lastVelocity = originalVelocities[patternLength - 1];
            int lastDecay = originalDecays[patternLength - 1];
            int lastProbability = originalProbabilities[patternLength - 1];
            int lastNudge = originalNudges[patternLength - 1];
            int lastPan = originalPans[patternLength - 1];
            int lastChorus = originalChorus[patternLength - 1];
            int lastReverb = originalReverb[patternLength - 1];

            // Clear current pattern
            clearDrumTrack(sequencer, drumIndex);

            // Apply shifted pattern
            for (int i = 0; i < patternLength - 1; i++) {
                if (originalPattern[i]) {
                    sequencer.toggleStep(drumIndex, i + 1);
                }

                // Copy parameters regardless of step state
                sequencer.setStepVelocity(drumIndex, i + 1, originalVelocities[i]);
                sequencer.setStepDecay(drumIndex, i + 1, originalDecays[i]);
                sequencer.setStepProbability(drumIndex, i + 1, originalProbabilities[i]);
                sequencer.setStepNudge(drumIndex, i + 1, originalNudges[i]);
                sequencer.setStepPan(drumIndex, i + 1, originalPans[i]);
                sequencer.setStepChorus(drumIndex, i + 1, originalChorus[i]);
                sequencer.setStepReverb(drumIndex, i + 1, originalReverb[i]);
            }

            // Wrap last step to first position
            if (lastStepState) {
                sequencer.toggleStep(drumIndex, 0);
            }

            // Wrap last step parameters to first position
            sequencer.setStepVelocity(drumIndex, 0, lastVelocity);
            sequencer.setStepDecay(drumIndex, 0, lastDecay);
            sequencer.setStepProbability(drumIndex, 0, lastProbability);
            sequencer.setStepNudge(drumIndex, 0, lastNudge);
            sequencer.setStepPan(drumIndex, 0, lastPan);
            sequencer.setStepChorus(drumIndex, 0, lastChorus);
            sequencer.setStepReverb(drumIndex, 0, lastReverb);

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Pushed pattern forward for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error pushing pattern forward for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Shifts a pattern backward/left by one step with parameter preservation
     */
    public static boolean pullPatternBackward(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            boolean[] originalPattern = new boolean[patternLength];

            // Arrays to store all step parameters
            int[] originalVelocities = new int[patternLength];
            int[] originalDecays = new int[patternLength];
            int[] originalProbabilities = new int[patternLength];
            int[] originalNudges = new int[patternLength];
            int[] originalPans = new int[patternLength];
            int[] originalChorus = new int[patternLength];
            int[] originalReverb = new int[patternLength];

            // Save original pattern with all parameters
            for (int i = 0; i < patternLength; i++) {
                originalPattern[i] = sequencer.isStepActive(drumIndex, i);
                originalVelocities[i] = sequencer.getStepVelocity(drumIndex, i);
                originalDecays[i] = sequencer.getStepDecay(drumIndex, i);
                originalProbabilities[i] = sequencer.getStepProbability(drumIndex, i);
                originalNudges[i] = sequencer.getStepNudge(drumIndex, i);
                originalPans[i] = sequencer.getStepPan(drumIndex, i);
                originalChorus[i] = sequencer.getStepChorus(drumIndex, i);
                originalReverb[i] = sequencer.getStepReverb(drumIndex, i);
            }

            // Shift pattern left (wrap around for first step)
            boolean firstStepState = originalPattern[0];
            int firstVelocity = originalVelocities[0];
            int firstDecay = originalDecays[0];
            int firstProbability = originalProbabilities[0];
            int firstNudge = originalNudges[0];
            int firstPan = originalPans[0];
            int firstChorus = originalChorus[0];
            int firstReverb = originalReverb[0];

            // Clear current pattern
            clearDrumTrack(sequencer, drumIndex);

            // Apply shifted pattern
            for (int i = 1; i < patternLength; i++) {
                if (originalPattern[i]) {
                    sequencer.toggleStep(drumIndex, i - 1);
                }

                // Copy parameters regardless of step state
                sequencer.setStepVelocity(drumIndex, i - 1, originalVelocities[i]);
                sequencer.setStepDecay(drumIndex, i - 1, originalDecays[i]);
                sequencer.setStepProbability(drumIndex, i - 1, originalProbabilities[i]);
                sequencer.setStepNudge(drumIndex, i - 1, originalNudges[i]);
                sequencer.setStepPan(drumIndex, i - 1, originalPans[i]);
                sequencer.setStepChorus(drumIndex, i - 1, originalChorus[i]);
                sequencer.setStepReverb(drumIndex, i - 1, originalReverb[i]);
            }

            // Wrap first step to last position
            if (firstStepState) {
                sequencer.toggleStep(drumIndex, patternLength - 1);
            }

            // Wrap first step parameters to last position
            sequencer.setStepVelocity(drumIndex, patternLength - 1, firstVelocity);
            sequencer.setStepDecay(drumIndex, patternLength - 1, firstDecay);
            sequencer.setStepProbability(drumIndex, patternLength - 1, firstProbability);
            sequencer.setStepNudge(drumIndex, patternLength - 1, firstNudge);
            sequencer.setStepPan(drumIndex, patternLength - 1, firstPan);
            sequencer.setStepChorus(drumIndex, patternLength - 1, firstChorus);
            sequencer.setStepReverb(drumIndex, patternLength - 1, firstReverb);

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Pulled pattern backward for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error pulling pattern backward for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Generates a random pattern with specified density
     */
    public static boolean generateRandomPattern(DrumSequencer sequencer, int drumIndex, int density) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Clear existing pattern
            clearDrumTrack(sequencer, drumIndex);

            // Calculate how many steps should be active
            int activeSteps = (int) Math.round((density / 100.0) * patternLength);

            // Generate random steps until we have enough active steps
            int currentActive = 0;
            while (currentActive < activeSteps) {
                int step = random.nextInt(patternLength);
                if (!sequencer.isStepActive(drumIndex, step)) {
                    sequencer.toggleStep(drumIndex, step);

                    // Set slightly randomized parameters for variety
                    sequencer.setStepVelocity(drumIndex, step,
                            SequencerConstants.DEFAULT_VELOCITY - random.nextInt(20));
                    sequencer.setStepDecay(drumIndex, step,
                            SequencerConstants.DEFAULT_DECAY - random.nextInt(10));
                    sequencer.setStepProbability(drumIndex, step,
                            SequencerConstants.DEFAULT_PROBABILITY - random.nextInt(15));

                    currentActive++;
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Generated random pattern with {}% density for drum {}", density, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error generating random pattern for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Applies a fill pattern starting from a specific step
     */
    public static boolean applyFillPattern(DrumSequencer sequencer, int drumIndex, int startStep, String fillType) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Clear steps from start position
            for (int i = startStep; i < patternLength; i++) {
                if (sequencer.isStepActive(drumIndex, i)) {
                    sequencer.toggleStep(drumIndex, i);
                }
            }

            // Apply the fill pattern
            for (int i = startStep; i < patternLength; i++) {
                boolean shouldActivate = false;
                int velocity = SequencerConstants.DEFAULT_VELOCITY;
                int decay = SequencerConstants.DEFAULT_DECAY;
                int probability = SequencerConstants.DEFAULT_PROBABILITY;

                switch (fillType) {
                    case "all" -> {
                        shouldActivate = true;
                    }
                    case "everyOther" -> {
                        shouldActivate = ((i - startStep) % 2) == 0;
                    }
                    case "every4th" -> {
                        shouldActivate = ((i - startStep) % 4) == 0;
                    }
                    case "decay" -> {
                        shouldActivate = true;
                        // Set decreasing velocity for decay pattern
                        velocity = Math.max(SequencerConstants.DEFAULT_VELOCITY / 2,
                                SequencerConstants.DEFAULT_VELOCITY - ((i - startStep) * 8));
                        // Also reduce decay for later hits
                        decay = Math.max(SequencerConstants.DEFAULT_DECAY / 2,
                                SequencerConstants.DEFAULT_DECAY - ((i - startStep) * 4));
                    }
                }

                if (shouldActivate) {
                    sequencer.toggleStep(drumIndex, i);
                    // Always set parameters - even if step was already active
                    sequencer.setStepVelocity(drumIndex, i, velocity);
                    sequencer.setStepDecay(drumIndex, i, decay);
                    sequencer.setStepProbability(drumIndex, i, probability);
                }
            }

            // Notify that the pattern has changed - crucial for UI update
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied {} fill pattern from step {} for drum {}", fillType, startStep, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying fill pattern for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Randomizes velocities for all active steps in a pattern
     *
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @return True if velocities were randomized successfully
     */
    public static boolean randomizeVelocities(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Go through each step in the pattern
            for (int step = 0; step < patternLength; step++) {
                // Only modify active steps
                if (sequencer.isStepActive(drumIndex, step)) {
                    // Generate random velocity between 50-127 for better musical results
                    int randomVelocity = 50 + random.nextInt(78);
                    sequencer.setStepVelocity(drumIndex, step, randomVelocity);
                }
            }

            // Publish parameter-specific event to update dials
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    // Publish change event for each active step
                    CommandBus.getInstance().publish(
                        Commands.DRUM_STEP_PARAMETERS_CHANGED,
                        DrumSequenceModifier.class,
                        new DrumStepParametersEvent(sequencer, drumIndex, step)
                    );
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Randomized velocities for active steps in drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error randomizing velocities for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Randomizes nudge values for all active steps in a pattern
     */
    public static boolean randomizeNudgeValues(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Go through each step in the pattern
            for (int step = 0; step < patternLength; step++) {
                // Only modify active steps
                if (sequencer.isStepActive(drumIndex, step)) {
                    // Generate random nudge between -25 and 25
                    int randomNudge = random.nextInt(51) - 25;
                    sequencer.setStepNudge(drumIndex, step, randomNudge);
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Randomized nudge values for active steps in drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error randomizing nudge values for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Randomizes probability values for all active steps in a pattern
     */
    public static boolean randomizeProbabilities(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Go through each step in the pattern
            for (int step = 0; step < patternLength; step++) {
                // Only modify active steps
                if (sequencer.isStepActive(drumIndex, step)) {
                    // Generate random probability between 50 and 100 (for musical results)
                    int randomProb = 50 + random.nextInt(51);
                    sequencer.setStepProbability(drumIndex, step, randomProb);
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Randomized probabilities for active steps in drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error randomizing probabilities for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Sets ascending velocity pattern for active steps
     */
    public static boolean setAscendingVelocities(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Count active steps to calculate velocity range
            int activeCount = 0;
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    activeCount++;
                }
            }

            if (activeCount == 0) {
                logger.warn("No active steps to apply ascending velocities to");
                return false;
            }

            // Calculate velocity increment
            int velocityStart = 50;
            int velocityRange = 77; // Up to 127
            int increment = velocityRange / Math.max(1, activeCount - 1);

            // Apply ascending velocities to active steps
            int currentVelocity = velocityStart;
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    sequencer.setStepVelocity(drumIndex, step, currentVelocity);
                    currentVelocity = Math.min(127, currentVelocity + increment);
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied ascending velocities for active steps in drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying ascending velocities for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Sets descending velocity pattern for active steps
     */
    public static boolean setDescendingVelocities(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Count active steps to calculate velocity range
            int activeCount = 0;
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    activeCount++;
                }
            }

            if (activeCount == 0) {
                logger.warn("No active steps to apply descending velocities to");
                return false;
            }

            // Calculate velocity decrement
            int velocityStart = 127;
            int velocityRange = 77; // Down to 50
            int decrement = velocityRange / Math.max(1, activeCount - 1);

            // Apply descending velocities to active steps
            int currentVelocity = velocityStart;
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    sequencer.setStepVelocity(drumIndex, step, currentVelocity);
                    currentVelocity = Math.max(50, currentVelocity - decrement);
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied descending velocities for active steps in drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying descending velocities for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Applies a pattern from a specific start step using the every-Nth approach
     */
    public static boolean applyPatternEveryNFromStep(DrumSequencer sequencer, int drumIndex, int startStep, int stepInterval) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Clear steps from start position
            for (int i = startStep; i < patternLength; i++) {
                if (sequencer.isStepActive(drumIndex, i)) {
                    sequencer.toggleStep(drumIndex, i);
                }
            }

            // Apply every-N pattern from start step
            for (int i = startStep; i < patternLength; i += stepInterval) {
                sequencer.toggleStep(drumIndex, i);

                // Set default parameters
                sequencer.setStepVelocity(drumIndex, i, SequencerConstants.DEFAULT_VELOCITY);
                sequencer.setStepDecay(drumIndex, i, SequencerConstants.DEFAULT_DECAY);
                sequencer.setStepProbability(drumIndex, i, SequencerConstants.DEFAULT_PROBABILITY);
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied 1/{} pattern from step {} for drum {}", stepInterval, startStep, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying pattern every {} steps from step {} for drum {}",
                    stepInterval, startStep, drumIndex, e);
            return false;
        }
    }

    /**
     * Creates a bouncing pattern by alternating direction
     */
    public static boolean createBouncePattern(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            boolean[] originalPattern = new boolean[patternLength];
            int[] originalVelocities = new int[patternLength];
            int[] originalDecays = new int[patternLength];
            int[] originalProbabilities = new int[patternLength];

            // Save original pattern with parameters
            for (int i = 0; i < patternLength; i++) {
                originalPattern[i] = sequencer.isStepActive(drumIndex, i);
                originalVelocities[i] = sequencer.getStepVelocity(drumIndex, i);
                originalDecays[i] = sequencer.getStepDecay(drumIndex, i);
                originalProbabilities[i] = sequencer.getStepProbability(drumIndex, i);
            }

            // Clear and create new pattern with double length if possible
            int newLength = Math.min(sequencer.getMaxPatternLength(), patternLength * 2);
            clearDrumTrack(sequencer, drumIndex);
            sequencer.setPatternLength(drumIndex, newLength);

            // First copy original pattern
            for (int i = 0; i < patternLength; i++) {
                if (originalPattern[i]) {
                    sequencer.toggleStep(drumIndex, i);
                    sequencer.setStepVelocity(drumIndex, i, originalVelocities[i]);
                    sequencer.setStepDecay(drumIndex, i, originalDecays[i]);
                    sequencer.setStepProbability(drumIndex, i, originalProbabilities[i]);
                }
            }

            // Then add reversed pattern
            for (int i = 0; i < patternLength && (patternLength + i) < newLength; i++) {
                int srcIndex = patternLength - 1 - i;
                int destIndex = patternLength + i;

                if (srcIndex >= 0 && originalPattern[srcIndex]) {
                    sequencer.toggleStep(drumIndex, destIndex);
                    sequencer.setStepVelocity(drumIndex, destIndex, originalVelocities[srcIndex]);
                    sequencer.setStepDecay(drumIndex, destIndex, originalDecays[srcIndex]);
                    sequencer.setStepProbability(drumIndex, destIndex, originalProbabilities[srcIndex]);
                }
            }

            // Notify that the pattern has changed
            notifyPatternChanged(sequencer, drumIndex);

            logger.info("Created bounce pattern for drum {} with new length {}", drumIndex, newLength);
            return true;
        } catch (Exception e) {
            logger.error("Error creating bounce pattern for drum {}", drumIndex, e);
            return false;
        }
    }

    /**
     * Make this method public so it can be called from other classes
     */
    public static void notifyPatternChanged(DrumSequencer sequencer, int drumIndex) {
        // Send event to update UI
        CommandBus.getInstance().publish(
                Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED,
                DrumSequenceModifier.class,
                drumIndex
        );

        // Also publish a pattern updated event
        CommandBus.getInstance().publish(
                Commands.PATTERN_UPDATED,
                DrumSequenceModifier.class,
                sequencer.getSequenceData()
        );

        // Add a direct UI update event
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_UPDATED,
                DrumSequenceModifier.class,
                drumIndex
        );
    }
}

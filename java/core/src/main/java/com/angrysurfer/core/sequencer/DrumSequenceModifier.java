package com.angrysurfer.core.sequencer;

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
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @param pattern The boolean array representing the pattern
     * @return True if pattern was applied successfully, false otherwise
     */
    public static boolean applyEuclideanPattern(DrumSequencer sequencer, int drumIndex, boolean[] pattern) {
        if (pattern == null || pattern.length == 0) {
            logger.warn("Cannot apply null or empty Euclidean pattern");
            return false;
        }
        
        try {
            // Clear existing pattern for this drum
            for (int step = 0; step < sequencer.getPatternLength(drumIndex); step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    sequencer.toggleStep(drumIndex, step);
                }
            }
            
            // Set the pattern length if needed
            int newLength = pattern.length;
            sequencer.setPatternLength(drumIndex, newLength);
            
            // Apply pattern values (activate steps where pattern is true)
            for (int step = 0; step < pattern.length; step++) {
                if (pattern[step]) {
                    // Toggle the step to make it active
                    if (!sequencer.isStepActive(drumIndex, step)) {
                        sequencer.toggleStep(drumIndex, step);
                    }
                    
                    // Set default parameters for this step
                    sequencer.setStepVelocity(drumIndex, step, DrumSequenceData.DEFAULT_VELOCITY);
                    sequencer.setStepDecay(drumIndex, step, DrumSequenceData.DEFAULT_DECAY);
                    sequencer.setStepProbability(drumIndex, step, DrumSequenceData.DEFAULT_PROBABILITY);
                    sequencer.setStepNudge(drumIndex, step, 0);
                }
            }
            
            logger.info("Applied Euclidean pattern to drum {}, pattern length: {}", drumIndex, pattern.length);
            return true;
        } catch (Exception e) {
            logger.error("Error applying Euclidean pattern", e);
            return false;
        }
    }
    
    /**
     * Clears all steps for a specific drum track
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to clear
     * @return True if operation was successful
     */
    public static boolean clearDrumTrack(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            for (int step = 0; step < patternLength; step++) {
                if (sequencer.isStepActive(drumIndex, step)) {
                    sequencer.toggleStep(drumIndex, step);
                }
            }
            logger.info("Cleared track for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error clearing drum track {}", drumIndex, e);
            return false;
        }
    }
    
    /**
     * Sets maximum pattern length and truncates any patterns that exceed it
     * @param sequencer The drum sequencer to modify
     * @param maxLength The new maximum pattern length
     * @return List of drum indices that were modified
     */
    public static List<Integer> applyMaxPatternLength(DrumSequencer sequencer, int maxLength) {
        List<Integer> updatedDrums = new ArrayList<>();
        
        try {
            // Update lengths for all drums
            for (int drumIndex = 0; drumIndex < DrumSequenceData.DRUM_PAD_COUNT; drumIndex++) {
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
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
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
     * Shifts a pattern forward/right by one step
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @return True if operation was successful
     */
    public static boolean pushPatternForward(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            boolean[] originalPattern = new boolean[patternLength];
            
            // Save original pattern
            for (int i = 0; i < patternLength; i++) {
                originalPattern[i] = sequencer.isStepActive(drumIndex, i);
            }
            
            // Shift pattern right (wrap around for last step)
            boolean lastStepState = originalPattern[patternLength - 1];
            
            // Clear current pattern
            clearDrumTrack(sequencer, drumIndex);
            
            // Apply shifted pattern
            for (int i = 0; i < patternLength - 1; i++) {
                if (originalPattern[i]) {
                    sequencer.toggleStep(drumIndex, i + 1);
                }
            }
            
            // Wrap last step to first position
            if (lastStepState) {
                sequencer.toggleStep(drumIndex, 0);
            }
            
            logger.info("Pushed pattern forward for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error pushing pattern forward for drum {}", drumIndex, e);
            return false;
        }
    }
    
    /**
     * Shifts a pattern backward/left by one step
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @return True if operation was successful
     */
    public static boolean pullPatternBackward(DrumSequencer sequencer, int drumIndex) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            boolean[] originalPattern = new boolean[patternLength];
            
            // Save original pattern
            for (int i = 0; i < patternLength; i++) {
                originalPattern[i] = sequencer.isStepActive(drumIndex, i);
            }
            
            // Shift pattern left (wrap around for first step)
            boolean firstStepState = originalPattern[0];
            
            // Clear current pattern
            clearDrumTrack(sequencer, drumIndex);
            
            // Apply shifted pattern
            for (int i = 1; i < patternLength; i++) {
                if (originalPattern[i]) {
                    sequencer.toggleStep(drumIndex, i - 1);
                }
            }
            
            // Wrap first step to last position
            if (firstStepState) {
                sequencer.toggleStep(drumIndex, patternLength - 1);
            }
            
            logger.info("Pulled pattern backward for drum {}", drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error pulling pattern backward for drum {}", drumIndex, e);
            return false;
        }
    }
    
    /**
     * Generates a random pattern with specified density
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @param density Percentage of steps that should be active (0-100)
     * @return True if pattern was generated successfully
     */
    public static boolean generateRandomPattern(DrumSequencer sequencer, int drumIndex, int density) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);
            
            // Clear existing pattern
            clearDrumTrack(sequencer, drumIndex);
            
            // Calculate how many steps should be active
            int activeSteps = (int)Math.round((density / 100.0) * patternLength);
            
            // Generate random steps until we have enough active steps
            int currentActive = 0;
            while (currentActive < activeSteps) {
                int step = random.nextInt(patternLength);
                if (!sequencer.isStepActive(drumIndex, step)) {
                    sequencer.toggleStep(drumIndex, step);
                    currentActive++;
                }
            }
            
            logger.info("Generated random pattern with {}% density for drum {}", density, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error generating random pattern for drum {}", drumIndex, e);
            return false;
        }
    }
    
    /**
     * Applies a fill pattern starting from a specific step
     * @param sequencer The drum sequencer to modify
     * @param drumIndex The index of the drum to update
     * @param startStep The step to start the fill from
     * @param fillType The type of fill to apply (all, everyOther, every4th, decay)
     * @return True if fill was applied successfully
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
                
                switch (fillType) {
                    case "all" -> shouldActivate = true;
                    case "everyOther" -> shouldActivate = ((i - startStep) % 2) == 0;
                    case "every4th" -> shouldActivate = ((i - startStep) % 4) == 0;
                    case "decay" -> {
                        shouldActivate = true;
                        // Set decreasing velocity for decay pattern
                        int velocity = Math.max(DrumSequenceData.DEFAULT_VELOCITY / 2,
                                         DrumSequenceData.DEFAULT_VELOCITY - ((i - startStep) * 8));
                        sequencer.setStepVelocity(drumIndex, i, velocity);
                    }
                }
                
                if (shouldActivate) {
                    sequencer.toggleStep(drumIndex, i);
                }
            }
            
            logger.info("Applied {} fill pattern from step {} for drum {}", fillType, startStep, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying fill pattern for drum {}", drumIndex, e);
            return false;
        }
    }
}

package com.angrysurfer.core.sequencer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrumSequenceModifier {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceModifier.class);

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
                    sequencer.setStepVelocity(drumIndex, step, DrumSequencer.DEFAULT_VELOCITY);
                    sequencer.setStepDecay(drumIndex, step, DrumSequencer.DEFAULT_DECAY);
                    sequencer.setStepProbability(drumIndex, step, DrumSequencer.DEFAULT_PROBABILITY);
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
}

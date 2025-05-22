package com.angrysurfer.core.event;

import com.angrysurfer.core.sequencer.DrumSequencer;
import lombok.Getter;

/**
 * Event class for drum step parameter changes
 */
@Getter
public class DrumStepParametersEvent {
    private final int drumIndex;
    private final int stepIndex;
    private final int velocity;
    private final int decay;
    private final int probability;
    private final int nudge;

    /**
     * Create a new drum step parameters event
     * 
     * @param sequencer The drum sequencer containing the step data
     * @param drumIndex The index of the drum
     * @param stepIndex The index of the step
     */
    public DrumStepParametersEvent(DrumSequencer sequencer, int drumIndex, int stepIndex) {
        this.drumIndex = drumIndex;
        this.stepIndex = stepIndex;
        
        // Extract all values directly from the sequencer
        this.velocity = sequencer.getStepVelocity(drumIndex, stepIndex);
        this.decay = sequencer.getStepDecay(drumIndex, stepIndex);
        this.probability = sequencer.getStepProbability(drumIndex, stepIndex);
        this.nudge = sequencer.getStepNudge(drumIndex, stepIndex);
    }
}
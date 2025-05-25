package com.angrysurfer.core.event;

import com.angrysurfer.core.sequencer.DrumSequencer;
import lombok.Getter;
import lombok.Setter;

/**
 * Event class for drum step parameter changes
 */
@Getter
@Setter
public class DrumStepParametersEvent {
    private int drumIndex;
    private int stepIndex;
    private int velocity;
    private int decay;
    private int probability;
    private int nudge;
    private boolean accented;
    private boolean active;

    public DrumStepParametersEvent() {

    }

    /**
     * Create a new drum step parameters event
     *
     * @param sequencer The drum sequencer containing the step data
     * @param drumIndex The index of the drum
     * @param stepIndex The index of the step
     */
    public DrumStepParametersEvent(DrumSequencer sequencer, int drumIndex, int stepIndex) {
        this.drumIndex = drumIndex;
        this.active = sequencer.isStepActive(drumIndex, stepIndex);
        this.stepIndex = stepIndex;
        this.accented = sequencer.isStepAccented(drumIndex, stepIndex);
        this.velocity = sequencer.getStepVelocity(drumIndex, stepIndex);
        this.decay = sequencer.getStepDecay(drumIndex, stepIndex);
        this.probability = sequencer.getStepProbability(drumIndex, stepIndex);
        this.nudge = sequencer.getStepNudge(drumIndex, stepIndex);
    }
}
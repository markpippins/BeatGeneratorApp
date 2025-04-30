package com.angrysurfer.core.event;

import lombok.Getter;

/**
 * Updated StepUpdateEvent to include drum index
 */
@Getter
public class DrumStepUpdateEvent {

    private final int drumIndex;
    private final int oldStep;
    private final int newStep;

    public DrumStepUpdateEvent(int drumIndex, int oldStep, int newStep) {
        this.drumIndex = drumIndex;
        this.oldStep = oldStep;
        this.newStep = newStep;
    }
}
package com.angrysurfer.core.event;

/**
 * Event class for step updates
 */
public class StepUpdateEvent {
    private final int oldStep;
    private final int newStep;
    
    public StepUpdateEvent(int oldStep, int newStep) {
        this.oldStep = oldStep;
        this.newStep = newStep;
    }
    
    public int getOldStep() {
        return oldStep;
    }
    
    public int getNewStep() {
        return newStep;
    }
}
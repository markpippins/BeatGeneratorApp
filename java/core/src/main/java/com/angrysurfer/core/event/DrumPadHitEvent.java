package com.angrysurfer.core.event;

import lombok.Getter;

/**
 * Event class for drum pad hit notifications
 */
@Getter
public class DrumPadHitEvent {
    private final int padIndex;
    private final int velocity;
    
    public DrumPadHitEvent(int padIndex, int velocity) {
        this.padIndex = padIndex;
        this.velocity = velocity;
    }
}
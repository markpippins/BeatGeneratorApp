package com.angrysurfer.core.sequencer;

/**
 * Event class for pattern switching notifications
 */
public class PatternSwitchEvent {
    private final Long previousPatternId;
    private final Long newPatternId;
    
    public PatternSwitchEvent(Long previousPatternId, Long newPatternId) {
        this.previousPatternId = previousPatternId;
        this.newPatternId = newPatternId;
    }
    
    public Long getPreviousPatternId() {
        return previousPatternId;
    }
    
    public Long getNewPatternId() {
        return newPatternId;
    }
}
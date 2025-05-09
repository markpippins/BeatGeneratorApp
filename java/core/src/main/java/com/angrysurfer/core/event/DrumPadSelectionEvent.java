package com.angrysurfer.core.event;

/**
 * Event data for drum pad selection changes
 */
public class DrumPadSelectionEvent {
    private final int oldSelection;
    private final int newSelection;
    
    public DrumPadSelectionEvent(int oldSelection, int newSelection) {
        this.oldSelection = oldSelection;
        this.newSelection = newSelection;
    }
    
    public int getOldSelection() {
        return oldSelection;
    }
    
    public int getNewSelection() {
        return newSelection;
    }
}
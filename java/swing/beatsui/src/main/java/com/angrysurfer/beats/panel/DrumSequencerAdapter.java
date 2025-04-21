package com.angrysurfer.beats.panel;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.TimingDivision;

/**
 * Adapter class that makes DrumParamsSequencerPanel compatible with components 
 * expecting a DrumSequencerPanel
 */
public class DrumSequencerAdapter extends DrumSequencerPanel {
    
    private final DrumParamsSequencerPanel adaptee;
    
    public DrumSequencerAdapter(DrumParamsSequencerPanel panel) {
        super(null); // Not actually used
        this.adaptee = panel;
    }
    
    @Override
    public int getSelectedPadIndex() {
        return adaptee.getSelectedPadIndex();
    }
    
    @Override
    public void updateStepButtonsForDrum(int index) {
        adaptee.refreshTriggerButtonsForPad(index);
    }
    
    @Override
    public void clearRow(int index) {
        // Implement using adaptee's functionality
        for (int i = 0; i < 16; i++) {
            if (adaptee.getSequencer().isStepActive(index, i)) {
                adaptee.getSequencer().toggleStep(index, i);
            }
        }
        adaptee.refreshTriggerButtonsForPad(index);
    }
    
    @Override
    public void refreshGridUI() {
        adaptee.refreshGridUI();
    }
    
//    @Override
//    public Direction getCurrentDirection() {
//        return adaptee.getCurrentDirection();
//    }
//
//    @Override
//    public TimingDivision getTimingDivision() {
//        return adaptee.getTimingDivision();
//    }
//
//    @Override
//    public int getPatternLength() {
//        return adaptee.getPatternLength();
//    }
//
//    @Override
//    public boolean isLooping() {
//        return adaptee.isLooping();
//    }
}
package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class DrumPatternVisualization extends LockHandler implements IVisualizationHandler {

    private int seqPosition = 0;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        seqPosition = (seqPosition + 1) % buttons[0].length;

        // Create drum pattern visualization
        for (int row = 0; row < buttons.length; row++) {
            int pattern = switch (row) {
                case 0 -> 0b1000100010001000; // Kick
                case 1 -> 0b0000100000001000; // Snare
                case 2 -> 0b1010101010101010; // Hi-hat
                case 3 -> 0b0010001000100010; // Percussion
                default -> 0;
            };

            for (int col = 0; col < buttons[0].length; col++) {
                boolean isHit = (pattern & (1 << (col % 16))) != 0;
                if (isHit) {
                    buttons[row][col].setBackground(
                        col == seqPosition ? Color.WHITE : Color.ORANGE);
                } else if (col == seqPosition) {
                    buttons[row][col].setBackground(Color.DARK_GRAY);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Drum Pattern";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

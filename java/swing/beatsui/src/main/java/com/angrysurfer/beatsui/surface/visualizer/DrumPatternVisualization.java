package com.angrysurfer.beatsui.surface.visualizer;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class DrumPatternVisualization implements Visualization {
    private int seqPosition = 0;

    @Override
    public void update(GridButton[][] buttons) {
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
}

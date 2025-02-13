package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class ArpeggiatorVisualization implements Visualization {
    private double phase = 0.0;
    private final int[] notes = {0, 4, 7, 12, 7, 4}; // Major triad up and down

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        int position = (int)(phase * 8) % notes.length;
        int division = buttons[0].length / notes.length;

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                int noteIndex = col / division;
                if (noteIndex < notes.length && row == notes[noteIndex] % buttons.length) {
                    boolean isActive = noteIndex == position;
                    Color color = isActive ? Color.WHITE : 
                                 noteIndex == (position + 1) % notes.length ? Color.BLUE : 
                                 Color.DARK_GRAY;
                    buttons[row][col].setBackground(color);
                }
            }
        }
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Arpeggiator";
    }
}

package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class PolyrhythmVisualization implements Visualization {
    private double phase = 0.0;
    private final int[] rhythms = {3, 4, 5, 7, 8, 9, 11, 13}; // Different polyrhythm divisions
    private final Color[] colors = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, 
        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK
    };

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int row = 0; row < Math.min(buttons.length, rhythms.length); row++) {
            int rhythm = rhythms[row];
            double rhythmPhase = phase * rhythm;
            
            // Calculate current position in this rhythm
            int position = (int)(rhythmPhase) % buttons[0].length;
            int nextBeat = (position + buttons[0].length/rhythm) % buttons[0].length;

            // Draw rhythm line
            for (int col = 0; col < buttons[0].length; col++) {
                if (col == position) {
                    buttons[row][col].setBackground(Color.WHITE);
                } else if (col % (buttons[0].length/rhythm) == 0) {
                    buttons[row][col].setBackground(colors[row % colors.length]);
                }

                // Draw moving highlight
                double distance = Math.abs(col - position);
                if (distance < 2) {
                    int brightness = (int)(255 * (2 - distance));
                    buttons[row][col].setBackground(new Color(
                        brightness, brightness, brightness));
                }
            }
        }
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Polyrhythm";
    }
}

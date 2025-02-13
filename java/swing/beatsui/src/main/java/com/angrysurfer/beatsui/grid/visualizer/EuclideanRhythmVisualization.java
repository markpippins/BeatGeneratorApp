package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class EuclideanRhythmVisualization implements Visualization {
    private int position = 0;
    private final int[] steps = {8, 16, 12, 10, 7, 5, 6, 9};
    private final int[] pulses = {3, 7, 5, 4, 3, 2, 4, 5};

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int row = 0; row < Math.min(buttons.length, steps.length); row++) {
            int step = steps[row];
            int pulse = pulses[row];
            boolean[] pattern = generateEuclideanRhythm(step, pulse);

            int stepsPerCol = buttons[0].length / step;
            for (int i = 0; i < step; i++) {
                if (pattern[i]) {
                    int startCol = i * stepsPerCol;
                    Color color = (i == position % step) ? Color.WHITE : 
                                Color.getHSBColor((float)row / buttons.length, 0.8f, 1.0f);
                    
                    for (int col = startCol; col < startCol + stepsPerCol && col < buttons[0].length; col++) {
                        buttons[row][col].setBackground(color);
                    }
                }
            }
        }
        position++;
    }

    private boolean[] generateEuclideanRhythm(int steps, int pulses) {
        boolean[] pattern = new boolean[steps];
        double spacing = (double)steps / pulses;
        
        // Generate Euclidean rhythm pattern
        for (int i = 0; i < pulses; i++) {
            pattern[(int)(i * spacing)] = true;
        }
        
        return pattern;
    }

    @Override
    public String getName() {
        return "Euclidean Rhythm";
    }
}

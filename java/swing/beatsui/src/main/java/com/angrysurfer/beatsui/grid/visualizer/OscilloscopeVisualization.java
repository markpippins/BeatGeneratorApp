package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class OscilloscopeVisualization implements Visualization {
    private double phase = 0.0;

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Create Lissajous pattern
        for (int t = 0; t < 50; t++) {
            double angle = t * 0.1 + phase;
            int x = (int) ((Math.sin(angle * 3) + 1) * (buttons[0].length - 1) / 2);
            int y = (int) ((Math.sin(angle * 2) + 1) * (buttons.length - 1) / 2);
            
            if (x >= 0 && x < buttons[0].length && y >= 0 && y < buttons.length) {
                buttons[y][x].setBackground(Color.GREEN);
            }
        }
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Oscilloscope";
    }
}

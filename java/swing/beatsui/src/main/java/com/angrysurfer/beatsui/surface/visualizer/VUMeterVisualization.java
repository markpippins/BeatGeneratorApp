package com.angrysurfer.beatsui.surface.visualizer;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class VUMeterVisualization implements Visualization {
    private double phase = 0.0;

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        int meterWidth = buttons[0].length / 4;
        for (int meter = 0; meter < 4; meter++) {
            double level = Math.abs(Math.sin(phase + meter * 0.5));
            int height = (int) (level * buttons.length);

            for (int row = buttons.length - 1; row >= buttons.length - height; row--) {
                for (int col = meter * meterWidth; col < (meter + 1) * meterWidth - 1; col++) {
                    double rowPercent = (double) row / buttons.length;
                    Color color = rowPercent < 0.2 ? Color.RED :
                                 rowPercent < 0.4 ? Color.YELLOW : 
                                 Color.GREEN;
                    buttons[row][col].setBackground(color);
                }
            }
        }
        phase += 0.1;
    }

    @Override
    public String getName() {
        return "VU Meters";
    }
}

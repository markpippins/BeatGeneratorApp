package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class VUMeterVisualization implements IVisualizationHandler {
    
    private double phase = 0.0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());

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

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

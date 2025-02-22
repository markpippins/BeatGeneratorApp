package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class FrequencyBandsVisualization implements IVisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double phase = 0.0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        // Simulate frequency band analysis
        int bandWidth = 3;
        for (int band = 0; band < buttons[0].length / bandWidth; band++) {
            double freq = band * 0.5;
            int height = (int) (Math.abs(Math.sin(phase * freq)) * buttons.length);

            for (int col = band * bandWidth; col < (band + 1) * bandWidth; col++) {
                for (int row = buttons.length - 1; row >= buttons.length - height; row--) {
                    buttons[row][col].setBackground(new Color(
                        100 + (155 * band / (buttons[0].length / bandWidth)),
                        255 - (200 * band / (buttons[0].length / bandWidth)),
                        255));
                }
            }
        }
        phase += 0.1;
    }

    @Override
    public String getName() {
        return "Frequency Bands";
    }
}

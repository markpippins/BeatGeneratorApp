package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class WaveVisualization implements IVisualizationHandler {

    private double phase = 0.0;
    private final double frequency = 2.0;
    private final double amplitude = 2.0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        // Calculate wave parameters
        int centerY = buttons.length / 2;

        // Draw sine wave
        for (int col = 0; col < buttons[0].length; col++) {
            double x = col * (2 * Math.PI) / buttons[0].length;
            double y = Math.sin(x * frequency + phase) * amplitude;
            int row = centerY + (int) y;

            // Draw wave with gradient trail
            for (int offset = -2; offset <= 2; offset++) {
                int drawRow = row + offset;
                if (drawRow >= 0 && drawRow < buttons.length) {
                    int intensity = 255 - Math.abs(offset) * 60;
                    buttons[drawRow][col].setBackground(new Color(0, intensity, intensity));
                }
            }
        }

        phase += 0.1;
    }

    @Override
    public String getName() {
        return "Wave";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

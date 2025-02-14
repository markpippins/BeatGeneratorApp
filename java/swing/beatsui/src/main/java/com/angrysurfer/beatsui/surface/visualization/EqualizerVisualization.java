package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationCategory;
import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class EqualizerVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private final Random random = new Random();
    private int[] levels;

    @Override
    public void update(GridButton[][] buttons) {
        if (levels == null) {
            levels = new int[buttons[0].length];
        }
        
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update levels
        for (int col = 0; col < buttons[0].length; col++) {
            // Randomly adjust levels
            if (random.nextInt(100) < 30) {
                levels[col] += random.nextInt(3) - 1;
                levels[col] = Math.max(0, Math.min(buttons.length - 1, levels[col]));
            }

            // Draw columns
            for (int row = buttons.length - 1; row >= buttons.length - levels[col] && row >= 0; row--) {
                Color color = new Color(
                    50 + (205 * (buttons.length - row) / (buttons.length - 1)),
                    255 - (205 * (buttons.length - row) / (buttons.length - 1)),
                    0);
                buttons[row][col].setBackground(color);
            }
        }
    }

    @Override
    public String getName() {
        return "Equalizer";
    }
}

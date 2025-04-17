package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class EqualizerVisualization extends LockHandler implements IVisualizationHandler {
    
    private final Random random = new Random();
    private int[] levels;

    @Override
    public void update(JButton[][] buttons) {
        
       

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

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

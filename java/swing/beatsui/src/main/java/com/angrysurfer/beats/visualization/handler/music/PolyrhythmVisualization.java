package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class PolyrhythmVisualization extends LockHandler implements IVisualizationHandler {

    
    private double[] phases;
    private final double[] rhythms = {3, 4, 5, 7, 8, 9, 11, 13};
    private double baseSpeed = 0.1;

    @Override
    public void update(GridButton[][] buttons) {
        if (phases == null) {
            phases = new double[buttons.length];
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw each rhythm line
        for (int row = 0; row < buttons.length; row++) {
            double rhythm = rhythms[row % rhythms.length];
            phases[row] = (phases[row] + baseSpeed * rhythm) % (2 * Math.PI);

            for (int col = 0; col < buttons[0].length; col++) {
                double value = Math.sin(phases[row] + (col * 2 * Math.PI / buttons[0].length));
                value = (value + 1) / 2; // Normalize to 0-1 range

                // Ensure color values are within valid range (0-255)
                int intensity = (int)(value * 255);
                intensity = Math.max(0, Math.min(255, intensity));

                // Create colors using the bounded intensity value
                Color color = new Color(
                    intensity, 
                    Math.max(0, Math.min(255, intensity / 2)), 
                    Math.max(0, Math.min(255, 255 - intensity))
                );
                
                buttons[row][col].setBackground(color);
            }
        }
    }

    @Override
    public String getName() {
        return "Polyrhythm";
    } 

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PolyphonicVisualization extends LockHandler implements IVisualizationHandler {

    
    private double phase = 0.0;
    private final Color[] voiceColors = {Color.RED, Color.BLUE, Color.GREEN};

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Create multiple interweaving sine waves
        for (int voice = 0; voice < voiceColors.length; voice++) {
            double freq = 1.0 + voice * 0.5;
            
            for (int col = 0; col < buttons[0].length; col++) {
                double t = phase * freq + col * 0.2;
                int row = (int)((Math.sin(t) + 1) * (buttons.length - 1) / 2);
                
                if (row >= 0 && row < buttons.length) {
                    // Blend with existing color if multiple voices overlap
                    Color existing = buttons[row][col].getBackground();
                    if (!existing.equals(buttons[0][0].getParent().getBackground())) {
                        int r = (existing.getRed() + voiceColors[voice].getRed()) / 2;
                        int g = (existing.getGreen() + voiceColors[voice].getGreen()) / 2;
                        int b = (existing.getBlue() + voiceColors[voice].getBlue()) / 2;
                        buttons[row][col].setBackground(new Color(r, g, b));
                    } else {
                        buttons[row][col].setBackground(voiceColors[voice]);
                    }
                }
            }
        }
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Polyphonic Lines";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PhaseShiftVisualization extends LockHandler implements IVisualizationHandler {
    
    private double phase = 0.0;
    private final double[] phaseOffsets = {0, Math.PI/6, Math.PI/4, Math.PI/3, Math.PI/2, 2*Math.PI/3, 3*Math.PI/4, Math.PI};

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int row = 0; row < buttons.length; row++) {
            double phaseOffset = phaseOffsets[row % phaseOffsets.length];
            for (int col = 0; col < buttons[0].length; col++) {
                double value = Math.sin(phase + phaseOffset + col * 0.2);
                if (value > 0) {
                    int brightness = (int)(value * 255);
                    Color color = new Color(0, brightness, brightness);
                    buttons[row][col].setBackground(color);
                }
            }
        }
        phase += 0.1;
    }

    @Override
    public String getName() {
        return "Phase Shift";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

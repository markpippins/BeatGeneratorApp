package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class ModularCVVisualization implements IVisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double[] phases;
    private final double[] frequencies = {0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0};
    private final double[] amplitudes = {1.0, 0.8, 0.6, 0.7, 0.9, 0.5, 0.4, 0.3};

    @Override
    public void update(GridButton[][] buttons) {
        if (phases == null) {
            phases = new double[buttons.length];
        }

        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        // Generate multiple CV waveforms
        for (int row = 0; row < buttons.length; row++) {
            double freq = frequencies[row % frequencies.length];
            double amp = amplitudes[row % amplitudes.length];
            phases[row] += freq * 0.1;

            // Draw row of CV values
            for (int col = 0; col < buttons[0].length; col++) {
                // Generate modulated CV signal
                double cv = Math.sin(phases[row] + col * 0.1) * amp;
                cv = (cv + 1) / 2; // Normalize to 0-1 range

                // Create color gradient based on CV value
                int red = (int)(cv * 255);
                int blue = (int)((1 - cv) * 255);
                buttons[row][col].setBackground(new Color(red, 0, blue));

                // Add patch points at regular intervals
                if (col % 4 == 0) {
                    buttons[row][col].setBackground(Color.WHITE);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Modular CV";
    }
}

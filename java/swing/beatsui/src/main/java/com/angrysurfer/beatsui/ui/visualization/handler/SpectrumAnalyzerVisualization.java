package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class SpectrumAnalyzerVisualization implements IVisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double phase = 0.0;
    private double[] spectrumData;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        if (spectrumData == null || spectrumData.length != buttons[0].length) {
            spectrumData = new double[buttons[0].length];
        }

        // Simulate spectrum data
        for (int col = 0; col < buttons[0].length; col++) {
            spectrumData[col] = Math.abs(Math.sin(phase + col * 0.2)) *
                    Math.abs(Math.cos(phase * 0.7 + col * 0.1));
            int height = (int) (spectrumData[col] * buttons.length);

            for (int row = buttons.length - 1; row >= buttons.length - height; row--) {
                float hue = (float) col / buttons[0].length;
                buttons[row][col].setBackground(Color.getHSBColor(hue, 0.8f, 1.0f));
            }
        }
        phase += 0.1;
    }

    @Override
    public String getName() {
        return "Spectrum Analyzer";
    }
}

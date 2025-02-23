package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class LFOMatrixVisualization extends LockHandler implements IVisualizationHandler {

    private double phase = 0.0;
    private double[][] lfoValues;
    private final double[] lfoFreqs = {1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5};

    @Override
    public void update(GridButton[][] buttons) {
        if (lfoValues == null) {
            lfoValues = new double[buttons.length][buttons[0].length];
        }

        // Update LFO values
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double lfo1 = Math.sin(phase * lfoFreqs[row % lfoFreqs.length]);
                double lfo2 = Math.cos(phase * lfoFreqs[col % lfoFreqs.length]);
                lfoValues[row][col] = (lfo1 + lfo2) / 2;

                int intensity = (int)((lfoValues[row][col] + 1) * 127);
                buttons[row][col].setBackground(new Color(intensity, 0, intensity));
            }
        }
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "LFO Matrix";
    }   

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

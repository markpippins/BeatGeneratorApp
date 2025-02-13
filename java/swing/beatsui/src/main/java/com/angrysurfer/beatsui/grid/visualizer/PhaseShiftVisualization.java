package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class PhaseShiftVisualization implements Visualization {
    private double phase = 0.0;
    private final double[] phaseOffsets = {0, Math.PI/6, Math.PI/4, Math.PI/3, Math.PI/2, 2*Math.PI/3, 3*Math.PI/4, Math.PI};

    @Override
    public void update(GridButton[][] buttons) {
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
}

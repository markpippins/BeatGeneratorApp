package com.angrysurfer.beatsui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.visualization.VisualizationHandler;
import com.angrysurfer.beatsui.visualization.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class TimeDivisionVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double phase = 0.0;
    private final int[] divisions = {1, 2, 3, 4, 6, 8, 12, 16};

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int row = 0; row < Math.min(buttons.length, divisions.length); row++) {
            int division = divisions[row];
            double rowPhase = phase * division;
            
            // Calculate positions for this division
            for (int d = 0; d < division; d++) {
                int pos = (int)((d + rowPhase) * buttons[0].length / division) % buttons[0].length;
                
                // Draw division marker
                Color color = d == 0 ? Color.WHITE : 
                            d % 2 == 0 ? Color.CYAN : Color.BLUE;
                            
                // Draw markers with fade
                for (int i = -1; i <= 1; i++) {
                    int drawPos = (pos + i + buttons[0].length) % buttons[0].length;
                    if (i == 0) {
                        buttons[row][drawPos].setBackground(color);
                    } else {
                        Color fadeColor = new Color(
                            color.getRed() / 4,
                            color.getGreen() / 4,
                            color.getBlue() / 4
                        );
                        buttons[row][drawPos].setBackground(fadeColor);
                    }
                }
            }
        }
        phase += 0.02;
    }

    @Override
    public String getName() {
        return "Time Division";
    }
}

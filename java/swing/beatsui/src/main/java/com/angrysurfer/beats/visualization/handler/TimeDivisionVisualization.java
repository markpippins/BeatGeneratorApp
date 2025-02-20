package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class TimeDivisionVisualization implements IVisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double phase = 0.0;
    // Expand divisions to cover more rows, repeating pattern if needed
    private final int[] divisions = {1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64};

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());
        int totalRows = buttons.length;

        // Calculate how many times to repeat the pattern to fill all rows
        int patternRepeats = (totalRows + divisions.length - 1) / divisions.length;

        for (int row = 0; row < totalRows; row++) {
            // Get division value by wrapping around the divisions array
            int division = divisions[row % divisions.length];
            double rowPhase = phase * division;
            
            // Calculate positions for this division
            for (int d = 0; d < division; d++) {
                int pos = (int)((d + rowPhase) * buttons[0].length / division) % buttons[0].length;
                
                // Vary colors based on row position in pattern
                Color baseColor = getColorForRow(row, divisions.length);
                
                // Draw division marker with fade
                for (int i = -1; i <= 1; i++) {
                    int drawPos = (pos + i + buttons[0].length) % buttons[0].length;
                    Color color = i == 0 ? baseColor : new Color(
                        baseColor.getRed() / 4,
                        baseColor.getGreen() / 4,
                        baseColor.getBlue() / 4
                    );
                    buttons[row][drawPos].setBackground(color);
                }
            }
        }
        phase += 0.02;
    }

    private Color getColorForRow(int row, int patternLength) {
        // Calculate position within pattern
        int patternPosition = row % patternLength;
        float hue = (float) patternPosition / patternLength;
        
        // Base color - use white for first division in pattern
        if (patternPosition == 0) {
            return Color.WHITE;
        }
        
        // For other positions, use HSB color with full saturation and brightness
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    @Override
    public String getName() {
        return "Time Division";
    }
}

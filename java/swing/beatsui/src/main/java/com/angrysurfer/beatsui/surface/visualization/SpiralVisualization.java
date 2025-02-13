package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class SpiralVisualization implements VisualizationHandler {
    private double spiralAngle = 0;
    private double spiralRadius = 0;
    private final int centerX;
    private final int centerY;

    public SpiralVisualization() {
        this.centerX = 36 / 2; // GRID_COLS / 2
        this.centerY = 8 / 2;  // GRID_ROWS / 2
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        spiralAngle += 0.2;
        spiralRadius = (spiralAngle % 10) / 2;
        
        int x = (int) (centerX + Math.cos(spiralAngle) * spiralRadius);
        int y = (int) (centerY + Math.sin(spiralAngle) * spiralRadius);
        
        if (y >= 0 && y < buttons.length && x >= 0 && x < buttons[0].length) {
            buttons[y][x].setBackground(Color.MAGENTA);
        }
    }

    @Override
    public String getName() {
        return "Spiral";
    }
}

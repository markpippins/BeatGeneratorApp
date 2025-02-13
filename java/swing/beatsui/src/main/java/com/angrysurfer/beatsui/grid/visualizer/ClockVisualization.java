package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class ClockVisualization implements Visualization {
    private void drawClockHand(GridButton[][] buttons, int length, double angle, Color color) {
        int centerX = buttons[0].length / 2;
        int centerY = 2;
        int endX = centerX + (int) (Math.sin(angle) * length);
        int endY = centerY + (int) (Math.cos(angle) * length);
        
        if (endY >= 0 && endY < buttons.length && endX >= 0 && endX < buttons[0].length) {
            buttons[endY][endX].setBackground(color);
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        double time = System.currentTimeMillis() / 1000.0;
        // Hour hand
        drawClockHand(buttons, 2, time / 3600 % 12 * Math.PI / 6, Color.RED);
        // Minute hand
        drawClockHand(buttons, 3, time / 60 % 60 * Math.PI / 30, Color.GREEN);
        // Second hand
        drawClockHand(buttons, 3, time % 60 * Math.PI / 30, Color.BLUE);
    }

    @Override
    public String getName() {
        return "Clock";
    }
}

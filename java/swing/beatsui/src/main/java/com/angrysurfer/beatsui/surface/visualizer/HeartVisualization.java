package com.angrysurfer.beatsui.surface.visualizer;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class HeartVisualization implements Visualization {
    private double heartBeat = 0;

    private boolean isHeart(double x, double y) {
        x = Math.abs(x);
        return Math.pow((x * x + y * y - 1), 3) <= x * x * y * y * y;
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        int centerX = buttons[0].length / 2;
        int centerY = 2;
        heartBeat += 0.1;
        double size = 1 + Math.sin(heartBeat) * 0.5;

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double dx = (col - centerX) / size;
                double dy = (row - centerY) / size;
                if (isHeart(dx, dy)) {
                    buttons[row][col].setBackground(Color.RED);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Heart Beat";
    }
}

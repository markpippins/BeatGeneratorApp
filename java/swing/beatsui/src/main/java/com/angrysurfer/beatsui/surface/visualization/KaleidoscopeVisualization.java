package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.widget.GridButton;

public class KaleidoscopeVisualization implements VisualizationHandler {
    private double t = 0.0;

    private void setSymmetricPixels(GridButton[][] buttons, int x, int y, Color color) {
        int centerX = buttons[0].length / 2;
        int centerY = buttons.length / 2;

        if (y + centerY < buttons.length && x + centerX < buttons[0].length)
            buttons[y + centerY][x + centerX].setBackground(color);
        if (y + centerY < buttons.length && centerX - x - 1 >= 0)
            buttons[y + centerY][centerX - x - 1].setBackground(color);
        if (centerY - y - 1 >= 0 && x + centerX < buttons[0].length)
            buttons[centerY - y - 1][x + centerX].setBackground(color);
        if (centerY - y - 1 >= 0 && centerX - x - 1 >= 0)
            buttons[centerY - y - 1][centerX - x - 1].setBackground(color);
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length / 2; col++) {
                if (row < buttons.length / 2) {
                    double angle = t + Math.sqrt(col * col + row * row) * 0.2;
                    int red = (int) (Math.sin(angle) * 127 + 128);
                    int green = (int) (Math.sin(angle + 2 * Math.PI / 3) * 127 + 128);
                    int blue = (int) (Math.sin(angle + 4 * Math.PI / 3) * 127 + 128);
                    setSymmetricPixels(buttons, col, row, new Color(red, green, blue));
                }
            }
        }
        t += 0.1;
    }

    @Override
    public String getName() {
        return "Kaleidoscope";
    }
}

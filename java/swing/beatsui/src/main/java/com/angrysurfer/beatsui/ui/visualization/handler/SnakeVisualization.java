package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class SnakeVisualization implements IVisualizationHandler {
    private double angle = 0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());
        
        int row = 2 + (int) (Math.sin(angle) * 1.5);
        int col = (int) (angle * 2) % buttons[0].length;
        if (row >= 0 && row < buttons.length && col >= 0 && col < buttons[0].length) {
            buttons[row][col].setBackground(Color.GREEN);
        }
        angle += 0.2;
    }

    @Override
    public String getName() {
        return "Snake";
    }
}

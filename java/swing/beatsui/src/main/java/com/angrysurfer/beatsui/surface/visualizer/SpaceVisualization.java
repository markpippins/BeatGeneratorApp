package com.angrysurfer.beatsui.surface.visualizer;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class SpaceVisualization implements Visualization {
    private final Random random = new Random();

    @Override
    public void update(GridButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (random.nextInt(100) < 2) {
                    buttons[row][col].setBackground(Color.WHITE);
                } else {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Space";
    }
}

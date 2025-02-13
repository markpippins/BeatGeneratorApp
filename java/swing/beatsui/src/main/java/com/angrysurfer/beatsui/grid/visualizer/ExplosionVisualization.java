package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import java.util.Random;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class ExplosionVisualization implements Visualization {
    private final Random random = new Random();

    @Override
    public void update(GridButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(new Color(
                            random.nextInt(156) + 100,
                            random.nextInt(50),
                            0));
                } else {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Explosion";
    }
}

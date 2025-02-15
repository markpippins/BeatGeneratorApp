package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class ConfettiVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(GridButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (random.nextInt(100) < 5) {
                    buttons[row][col].setBackground(Utils.RAINBOW_COLORS[
                        random.nextInt(Utils.RAINBOW_COLORS.length)]);
                } else if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Confetti";
    }
}

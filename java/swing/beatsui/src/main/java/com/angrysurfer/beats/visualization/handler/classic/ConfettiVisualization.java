package com.angrysurfer.beats.visualization.handler.classic;

import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

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

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}

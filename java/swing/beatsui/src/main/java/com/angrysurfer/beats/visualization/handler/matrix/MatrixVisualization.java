package com.angrysurfer.beats.visualization.handler.matrix;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.GridButton;

public class MatrixVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(GridButton[][] buttons) {
        // Shift everything down
        for (int row = buttons.length - 1; row > 0; row--) {
            for (int col = 0; col < buttons[0].length; col++) {
                Color above = buttons[row - 1][col].getBackground();
                if (above.equals(ColorUtils.fadedLime)) {
                    buttons[row][col].setBackground(ColorUtils.mutedOlive);
                } else if (!above.equals(buttons[0][0].getParent().getBackground())) {
                    buttons[row][col].setBackground(new Color(0,
                            Math.max(above.getGreen() - 20, 0), 0));
                } else {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }

        // New matrix symbols at top
        for (int col = 0; col < buttons[0].length; col++) {
            if (random.nextInt(100) < 15) {
                buttons[0][col].setBackground(ColorUtils.fadedLime);
            }
        }
    }

    @Override
    public String getName() {
        return "Matrix";
    }
   
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MATRIX;
    }
}

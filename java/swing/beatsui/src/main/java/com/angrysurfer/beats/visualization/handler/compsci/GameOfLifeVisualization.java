package com.angrysurfer.beats.visualization.handler.compsci;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;

import javax.swing.JButton;

public class GameOfLifeVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        boolean[][] nextGen = new boolean[buttons.length][buttons[0].length];
        Color parentBg = buttons[0][0].getParent().getBackground();

        // Calculate next generation
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                int neighbors = countLiveNeighbors(buttons, row, col);
                boolean isAlive = !buttons[row][col].getBackground().equals(parentBg);

                if (isAlive && (neighbors == 2 || neighbors == 3)) {
                    nextGen[row][col] = true;
                } else if (!isAlive && neighbors == 3) {
                    nextGen[row][col] = true;
                }
            }
        }

        // Update grid
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].setBackground(nextGen[row][col] ? UIUtils.fadedLime : parentBg);
            }
        }

        // Randomly seed new cells
        if (random.nextInt(100) < 5) {
            int row = random.nextInt(buttons.length);
            int col = random.nextInt(buttons[0].length);
            buttons[row][col].setBackground(UIUtils.fadedLime);
        }
    }

    private int countLiveNeighbors(JButton[][] buttons, int row, int col) {
        int count = 0;
        Color parentBg = buttons[0][0].getParent().getBackground();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue;
                int r = (row + i + buttons.length) % buttons.length;
                int c = (col + j + buttons[0].length) % buttons[0].length;
                if (!buttons[r][c].getBackground().equals(parentBg)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public String getName() {
        return "Game of Life";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.COMPSCI;
    }
}

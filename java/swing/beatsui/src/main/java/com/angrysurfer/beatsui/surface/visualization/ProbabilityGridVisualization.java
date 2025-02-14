package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationCategory;
import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class ProbabilityGridVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private final Random random = new Random();
    private double[][] probabilities;
    private boolean[][] activeStates;
    private int seqPosition = 0;

    @Override
    public void update(GridButton[][] buttons) {
        if (probabilities == null) {
            initializeGrid(buttons.length, buttons[0].length);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update sequence position
        seqPosition = (seqPosition + 1) % buttons[0].length;

        // Update active states based on probabilities
        for (int row = 0; row < buttons.length; row++) {
            activeStates[row][seqPosition] = random.nextDouble() < probabilities[row][seqPosition];
        }

        // Draw grid
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (activeStates[row][col]) {
                    Color color = col == seqPosition ? Color.WHITE : Color.MAGENTA;
                    buttons[row][col].setBackground(color);
                } else {
                    int probability = (int)(probabilities[row][col] * 255);
                    buttons[row][col].setBackground(new Color(0, probability, probability));
                }
            }
        }

        // Slowly evolve probabilities
        evolveProbabilities();
    }

    private void initializeGrid(int rows, int cols) {
        probabilities = new double[rows][cols];
        activeStates = new boolean[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                probabilities[row][col] = random.nextDouble() * 0.5;
            }
        }
    }

    private void evolveProbabilities() {
        for (int row = 0; row < probabilities.length; row++) {
            for (int col = 0; col < probabilities[0].length; col++) {
                if (random.nextInt(100) < 5) {
                    probabilities[row][col] += (random.nextDouble() - 0.5) * 0.1;
                    probabilities[row][col] = Math.min(1.0, Math.max(0.0, probabilities[row][col]));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Probability Grid";
    }
}

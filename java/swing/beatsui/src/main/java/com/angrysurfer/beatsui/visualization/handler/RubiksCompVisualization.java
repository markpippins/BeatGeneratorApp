package com.angrysurfer.beatsui.visualization.handler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.beatsui.visualization.VisualizationHandler;
import com.angrysurfer.beatsui.widget.GridButton;

public class RubiksCompVisualization implements VisualizationHandler {
    private List<RubiksCuber> cubers = new ArrayList<>();
    private long competitionStartTime;
    private boolean hasWinner = false;
    private Random random = new Random();
    private static final int NUM_COMPETITORS = 4;

    public RubiksCompVisualization() {
        resetCompetition();
    }

    private void resetCompetition() {
        cubers.clear();
        hasWinner = false;
        competitionStartTime = System.currentTimeMillis();
        
        // Create competitors spread across the grid
        for (int i = 0; i < NUM_COMPETITORS; i++) {
            cubers.add(new RubiksCuber(i * 8 + 4, 4));
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        // Clear the display
        for (GridButton[] row : buttons) {
            for (GridButton button : row) {
                button.setBackground(Color.BLACK);
            }
        }

        // Update and draw competitors
        boolean someoneWon = false;
        for (RubiksCuber cuber : cubers) {
            cuber.update();
            if (cuber.isWinner) {
                someoneWon = true;
            }
            
            // Draw the cuber and their progress bar
            int progressWidth = (int)(cuber.progress * 6);
            for (int i = 0; i < 6; i++) {
                Color color = i < progressWidth ? cuber.getCurrentColor() : Color.DARK_GRAY;
                buttons[cuber.y][cuber.x + i].setBackground(color);
            }
        }

        // Reset if someone won or after a timeout
        if (someoneWon || System.currentTimeMillis() - competitionStartTime > 10000) {
            resetCompetition();
        }
    }

    @Override
    public String getName() {
        return "Rubik's Competition";
    }

    private class RubiksCuber {
        int x, y;
        double progress;
        boolean isWinner;
        Color[] colors;

        RubiksCuber(int x, int y) {
            this.x = x;
            this.y = y;
            this.progress = 0.0;
            this.isWinner = false;
            this.colors = new Color[] {
                Color.RED, Color.ORANGE, Color.YELLOW,
                Color.GREEN, Color.BLUE, Color.WHITE
            };
        }

        void update() {
            if (!isWinner) {
                progress += random.nextDouble() * 0.03;
                if (progress >= 1.0) {
                    isWinner = true;
                }
            }
        }

        Color getCurrentColor() {
            int colorIndex = (int)(progress * colors.length);
            return colors[Math.min(colorIndex, colors.length - 1)];
        }
    }
}

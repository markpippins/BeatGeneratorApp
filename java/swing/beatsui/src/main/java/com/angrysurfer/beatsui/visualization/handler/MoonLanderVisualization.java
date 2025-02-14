package com.angrysurfer.beatsui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.visualization.VisualizationHandler;
import com.angrysurfer.beatsui.visualization.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class MoonLanderVisualization implements VisualizationHandler {
    private double shipX = 0.5; // Ship position (0.0 to 1.0)
    private double shipY = 0.2; // Ship starts near top
    private double velocityY = 0.0;
    private double velocityX = 0.0;
    private double thrust = 0.001;
    private double gravity = 0.0005;
    private int[] terrain; // Terrain height at each x position
    private boolean gameOver = false;
    private boolean landed = false;

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GAME;
    }

    private void initTerrain(int width) {
        if (terrain == null || terrain.length != width) {
            terrain = new int[width];
            // Generate random terrain with a flat landing pad
            int padStart = width / 3;
            int padWidth = width / 6;
            int baseHeight = width / 2;

            for (int i = 0; i < width; i++) {
                if (i >= padStart && i <= padStart + padWidth) {
                    terrain[i] = baseHeight; // Landing pad
                } else {
                    terrain[i] = baseHeight + (int) (Math.random() * 4) - 2;
                }
            }
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        int width = buttons[0].length;
        int height = buttons.length;

        initTerrain(width);
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        if (!gameOver && !landed) {
            // Update physics
            velocityY += gravity;
            shipY += velocityY;
            shipX += velocityX;

            // Bounce off walls
            if (shipX < 0) {
                shipX = 0;
                velocityX = Math.abs(velocityX) * 0.5;
            }
            if (shipX > 1) {
                shipX = 1;
                velocityX = -Math.abs(velocityX) * 0.5;
            }

            // Random thrust adjustments
            if (Math.random() < 0.1) {
                velocityY -= thrust; // Apply upward thrust
                velocityX += (Math.random() - 0.5) * thrust; // Random lateral adjustment
            }
        }

        // Convert ship position to grid coordinates
        int shipGridX = (int) (shipX * (width - 1));
        int shipGridY = (int) (shipY * (height - 1));

        // Check for collision with terrain
        if (shipGridY >= height - terrain[shipGridX]) {
            shipGridY = height - terrain[shipGridX];
            landed = true;
            // Check if landing was successful (low velocity)
            gameOver = Math.abs(velocityY) > 0.01 || Math.abs(velocityX) > 0.01;
        }

        // Draw terrain
        for (int x = 0; x < width; x++) {
            for (int y = height - terrain[x]; y < height; y++) {
                buttons[y][x].setBackground(Color.GRAY);
            }
        }

        // Draw ship
        Color shipColor = gameOver ? Color.RED : (landed ? Color.GREEN : Color.WHITE);
        if (shipGridY >= 0 && shipGridY < height && shipGridX >= 0 && shipGridX < width) {
            buttons[shipGridY][shipGridX].setBackground(shipColor);
            // Draw thrust trail
            if (!gameOver && !landed && Math.random() < 0.5) {
                int trailY = shipGridY + 1;
                if (trailY < height) {
                    buttons[trailY][shipGridX].setBackground(Color.ORANGE);
                }
            }
        }

        // Reset game if needed
        if ((gameOver || landed) && Math.random() < 0.02) {
            resetGame();
        }
    }

    private void resetGame() {
        shipX = Math.random(); // Random starting X position
        shipY = 0.2; // Start near top
        velocityY = 0.0;
        velocityX = 0.0;
        gameOver = false;
        landed = false;
    }

    @Override
    public String getName() {
        return "Moon Lander";
    }
}
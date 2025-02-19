package com.angrysurfer.beats.visualization.handler.arcade;

import com.angrysurfer.beats.visualization.DisplayType;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DescendingForceVisualizer implements IVisualizationHandler {
    private final Random random = new Random();
    private final List<Invader> invaders = new ArrayList<>();
    private int frameCount = 0;
    
    // Classic Space Invader patterns (8x8)
    private static final int[][] INVADER_PATTERN = {
        {0,0,1,1,1,1,0,0},
        {0,1,1,1,1,1,1,0},
        {1,1,1,1,1,1,1,1},
        {1,1,0,1,1,0,1,1},
        {1,1,1,1,1,1,1,1},
        {0,0,1,0,0,1,0,0},
        {0,1,0,1,1,0,1,0},
        {1,0,1,0,0,1,0,1}
    };

    private static final Color[] COLORS = {
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.ORANGE,
        Color.PINK
    };

    private class Invader {
        double y;           // Using double for smooth movement
        int x;
        double speed;
        Color color;
        boolean isRainbow;
        
        Invader() {
            reset();
        }
        
        void reset() {
            y = -INVADER_PATTERN.length;  // Start above viewport
            x = random.nextInt(40);       // Random horizontal position
            speed = 0.1 + random.nextDouble() * 0.2;  // Random speed
            isRainbow = random.nextDouble() < 0.1; // 10% chance of rainbow
            if (!isRainbow) {
                color = COLORS[random.nextInt(COLORS.length)];
            }
        }
        
        void update() {
            y += speed;
            if (y > 8) {  // Reset when fully past viewport
                reset();
            }
        }
        
        void draw(GridButton[][] buttons) {
            int startY = (int)y;
            // Only draw visible portions of the invader
            for (int row = 0; row < INVADER_PATTERN.length; row++) {
                int gridY = startY + row;
                if (gridY >= 0 && gridY < buttons.length) {
                    for (int col = 0; col < INVADER_PATTERN[0].length; col++) {
                        int gridX = x + col;
                        if (gridX >= 0 && gridX < buttons[0].length && INVADER_PATTERN[row][col] == 1) {
                            GridButton button = buttons[gridY][gridX];
                            if (isRainbow) {
                                // For rainbow invaders, use different colors for each row
                                button.setBackground(COLORS[(row + frameCount) % COLORS.length]);
                            } else {
                                button.setBackground(color);
                            }
                            button.setOn(true);
                        }
                    }
                }
            }
        }
    }

    public DescendingForceVisualizer() {
        // Create initial invaders
        for (int i = 0; i < 3; i++) {
            Invader invader = new Invader();
            // Stagger initial positions
            invader.y = -INVADER_PATTERN.length - (i * (INVADER_PATTERN.length + 4));
            invaders.add(invader);
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        // Clear the grid
        for (GridButton[] row : buttons) {
            for (GridButton button : row) {
                button.setOn(false);
            }
        }

        // Update and draw all invaders
        for (Invader invader : invaders) {
            invader.update();
            invader.draw(buttons);
        }

        frameCount++;
    }

    @Override
    public String getName() {
        return "Descending Force";
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.GAME;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GAME;
    }
}

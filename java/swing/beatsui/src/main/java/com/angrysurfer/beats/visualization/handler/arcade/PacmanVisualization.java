package com.angrysurfer.beats.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class PacmanVisualization implements IVisualizationHandler {
    private Point pacman = new Point(1, 4);
    private List<Point> dots = new ArrayList<>();
    private List<Point> ghosts = new ArrayList<>();
    private final int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}}; // right, down, left, up
    private int currentDir = 0;
    private double phase = 0;
    private final Random random = new Random();

    @Override
    public void update(GridButton[][] buttons) {
        if (dots.isEmpty()) {
            initializeGame(buttons);
        }

        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        // Move Pacman
        pacman.x = Math.floorMod(pacman.x + directions[currentDir][0], buttons[0].length);
        pacman.y = Math.floorMod(pacman.y + directions[currentDir][1], buttons.length);

        // AI direction change
        if (random.nextInt(20) == 0) {
            currentDir = random.nextInt(4);
        }

        // Collect dots
        dots.removeIf(dot -> dot.x == pacman.x && dot.y == pacman.y);

        // Move ghosts
        for (Point ghost : ghosts) {
            // Simple ghost AI - move toward Pacman
            if (random.nextInt(2) == 0) {
                if (ghost.x < pacman.x) ghost.x++;
                else if (ghost.x > pacman.x) ghost.x--;
            } else {
                if (ghost.y < pacman.y) ghost.y++;
                else if (ghost.y > pacman.y) ghost.y--;
            }
            
            // Wrap around screen
            ghost.x = Math.floorMod(ghost.x, buttons[0].length);
            ghost.y = Math.floorMod(ghost.y, buttons.length);
        }

        // Draw dots
        for (Point dot : dots) {
            buttons[dot.y][dot.x].setBackground(Color.WHITE);
        }

        // Draw ghosts
        for (Point ghost : ghosts) {
            buttons[ghost.y][ghost.x].setBackground(Color.RED);
        }

        // Draw Pacman with animation
        boolean mouthOpen = Math.sin(phase) > 0;
        buttons[pacman.y][pacman.x].setBackground(mouthOpen ? Color.YELLOW : Color.WHITE);

        phase += 0.3;

        // Reset if all dots collected
        if (dots.isEmpty()) {
            initializeGame(buttons);
        }
    }

    private void initializeGame(GridButton[][] buttons) {
        // Create dots
        for (int row = 1; row < buttons.length - 1; row += 2) {
            for (int col = 1; col < buttons[0].length - 1; col += 2) {
                dots.add(new Point(col, row));
            }
        }

        // Create ghosts
        ghosts.clear();
        for (int i = 0; i < 4; i++) {
            ghosts.add(new Point(
                buttons[0].length - 2 + random.nextInt(3),
                1 + random.nextInt(buttons.length - 2)
            ));
        }

        // Reset Pacman
        pacman.x = 1;
        pacman.y = buttons.length / 2;
        currentDir = 0;
    }

    @Override
    public String getName() {
        return "Pac-Man";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GAME;
    }
}

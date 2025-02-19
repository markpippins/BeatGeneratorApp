package com.angrysurfer.beats.visualization.handler.compsci;

import java.awt.Color;
import java.awt.Point;
import java.util.*;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class LifeSoupVisualization implements IVisualizationHandler {
    private Map<Point, Integer> cells = new HashMap<>();
    private final Random random = new Random();
    private final int[][] directions = {
        {-1,-1}, {-1,0}, {-1,1},
        {0,-1},          {0,1},
        {1,-1},  {1,0},  {1,1}
    };
    private List<Point> patterns;
    private double phase = 0;

    public LifeSoupVisualization() {
        patterns = Arrays.asList(
            // Glider
            new Point(0,0), new Point(1,0), new Point(2,0),
            new Point(2,1), new Point(1,2),
            // Blinker
            new Point(0,0), new Point(1,0), new Point(2,0),
            // Block
            new Point(0,0), new Point(0,1),
            new Point(1,0), new Point(1,1)
        );
    }

    @Override
    public void update(GridButton[][] buttons) {
        if (cells.isEmpty()) {
            initializeSoup(buttons);
        }

        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update life soup
        Map<Point, Integer> newCells = new HashMap<>();
        Set<Point> checkPoints = new HashSet<>(cells.keySet());
        
        // Add neighbor points to check
        for (Point p : cells.keySet()) {
            for (int[] dir : directions) {
                checkPoints.add(new Point(
                    Math.floorMod(p.x + dir[0], buttons[0].length),
                    Math.floorMod(p.y + dir[1], buttons.length)
                ));
            }
        }

        // Apply Game of Life rules with age
        for (Point p : checkPoints) {
            int neighbors = countNeighbors(p, buttons);
            if (cells.containsKey(p)) {
                if (neighbors == 2 || neighbors == 3) {
                    newCells.put(p, cells.get(p) + 1);
                }
            } else if (neighbors == 3) {
                newCells.put(p, 1);
            }
        }

        cells = newCells;

        // Draw cells with color based on age
        for (Map.Entry<Point, Integer> entry : cells.entrySet()) {
            Point p = entry.getKey();
            if (p.x >= 0 && p.x < buttons[0].length && p.y >= 0 && p.y < buttons.length) {
                double age = Math.min(entry.getValue() / 20.0, 1.0);
                float hue = (float)((age + phase) % 1.0);
                buttons[p.y][p.x].setBackground(Color.getHSBColor(hue, 0.8f, 1.0f));
            }
        }

        // Randomly spawn new patterns
        if (random.nextInt(20) == 0) {
            spawnPattern(buttons);
        }

        phase += 0.02;
    }

    private int countNeighbors(Point p, GridButton[][] buttons) {
        int count = 0;
        for (int[] dir : directions) {
            Point neighbor = new Point(
                Math.floorMod(p.x + dir[0], buttons[0].length),
                Math.floorMod(p.y + dir[1], buttons.length)
            );
            if (cells.containsKey(neighbor)) count++;
        }
        return count;
    }

    private void spawnPattern(GridButton[][] buttons) {
        int startIdx = random.nextInt(patterns.size() / 4) * 4;
        int x = random.nextInt(buttons[0].length);
        int y = random.nextInt(buttons.length);
        
        for (int i = 0; i < 4 && startIdx + i < patterns.size(); i++) {
            Point p = patterns.get(startIdx + i);
            Point newPoint = new Point(
                Math.floorMod(x + p.x, buttons[0].length),
                Math.floorMod(y + p.y, buttons.length)
            );
            cells.put(newPoint, 1);
        }
    }

    private void initializeSoup(GridButton[][] buttons) {
        cells.clear();
        for (int i = 0; i < 3; i++) {
            spawnPattern(buttons);
        }
    }

    @Override
    public String getName() {
        return "Life Soup";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        // TODO Auto-generated method stub
        return IVisualizationHandler.super.getVisualizationCategory();
    }
}

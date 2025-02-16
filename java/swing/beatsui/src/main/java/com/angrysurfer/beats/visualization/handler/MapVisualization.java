package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;
import java.awt.Point;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.widget.GridButton;

public class MapVisualization implements IVisualizationHandler {
    private final Random random = new Random();
    private Point viewportPosition = new Point(0, 0);
    private int scrollSpeed = 1;
    
    // Colors for different "terrain" types
    private final Color WATER = new Color(0, 100, 255);
    private final Color LAND = new Color(34, 139, 34);
    private final Color MOUNTAIN = new Color(139, 137, 137);
    private final Color DESERT = new Color(238, 214, 175);

    @Override
    public void update(GridButton[][] buttons) {
        // Move viewport
        viewportPosition.x = (viewportPosition.x + scrollSpeed) % 1000;
        
        for (int y = 0; y < buttons.length; y++) {
            for (int x = 0; x < buttons[0].length; x++) {
                // Use noise-like function to generate terrain
                double noiseValue = Math.sin((x + viewportPosition.x) * 0.1) * 
                                  Math.cos((y + viewportPosition.x) * 0.1) +
                                  Math.sin((x + viewportPosition.x) * 0.05) * 
                                  Math.cos((y + viewportPosition.x) * 0.05);
                
                // Assign colors based on "height"
                if (noiseValue < -0.5) {
                    buttons[y][x].setBackground(WATER);
                } else if (noiseValue < 0.2) {
                    buttons[y][x].setBackground(LAND);
                } else if (noiseValue < 0.5) {
                    buttons[y][x].setBackground(MOUNTAIN);
                } else {
                    buttons[y][x].setBackground(DESERT);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Map";
    }
}
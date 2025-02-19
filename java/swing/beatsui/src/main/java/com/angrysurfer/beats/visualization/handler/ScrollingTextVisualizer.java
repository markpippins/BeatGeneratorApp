package com.angrysurfer.beats.visualization.handler;

import com.angrysurfer.beats.visualization.DisplayType;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class ScrollingTextVisualizer implements IVisualizationHandler {
    private int position = 0;
    private static final String MESSAGE = "Hello, World!";
    private static final int CHAR_WIDTH = 4;
    private static final int CHAR_HEIGHT = 5;
    private static final int PADDING = 1;  // One row padding top and bottom
    
    // LED font definitions - 1 represents lit pixel, 0 represents unlit
    private static final int[][][] FONT = {
        // H
        {{1,0,0,1},
         {1,0,0,1},
         {1,1,1,1},
         {1,0,0,1},
         {1,0,0,1}},
        // e
        {{0,1,1,0},
         {1,0,0,1},
         {1,1,1,1},
         {1,0,0,0},
         {0,1,1,1}},
        // l
        {{1,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {1,1,1,0}},
        // o
        {{0,1,1,0},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {0,1,1,0}},
        // space
        {{0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,0,0,0}},
        // W
        {{1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,1,1},
         {1,1,0,1}},
        // r
        {{1,1,1,0},
         {1,0,0,1},
         {1,0,0,0},
         {1,0,0,0},
         {1,0,0,0}},
        // d
        {{0,0,1,1},
         {0,0,0,1},
         {0,1,1,1},
         {1,0,0,1},
         {0,1,1,1}},
        // !
        {{0,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,0,0,0},
         {0,1,0,0}}
    };

    private static final int[][] getCharacter(char c) {
        // Simple mapping for our demo message
        switch (Character.toLowerCase(c)) {
            case 'h': return FONT[0];
            case 'e': return FONT[1];
            case 'l': return FONT[2];
            case 'o': return FONT[3];
            case ' ': return FONT[4];
            case 'w': return FONT[5];
            case 'r': return FONT[6];
            case 'd': return FONT[7];
            case '!': return FONT[8];
            default: return FONT[4]; // space for unknown characters
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

        // Calculate the total width of the message
        int messageWidth = MESSAGE.length() * (CHAR_WIDTH + 1) - 1;
        
        // Draw each character
        for (int charIndex = 0; charIndex < MESSAGE.length(); charIndex++) {
            int[][] charPattern = getCharacter(MESSAGE.charAt(charIndex));
            int startX = charIndex * (CHAR_WIDTH + 1) - position;
            
            // Skip if the character is completely off screen
            if (startX + CHAR_WIDTH <= 0 || startX >= buttons[0].length) continue;
            
            // Draw the character
            for (int y = 0; y < CHAR_HEIGHT; y++) {
                for (int x = 0; x < CHAR_WIDTH; x++) {
                    int gridX = startX + x;
                    int gridY = y + PADDING;
                    
                    if (gridX >= 0 && gridX < buttons[0].length) {
                        buttons[gridY][gridX].setOn(charPattern[y][x] == 1);
                    }
                }
            }
        }

        // Update position for next frame
        position = (position + 1) % (messageWidth + buttons[0].length);
    }

    @Override
    public String getName() {
        return "Scrolling Text";
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }
}

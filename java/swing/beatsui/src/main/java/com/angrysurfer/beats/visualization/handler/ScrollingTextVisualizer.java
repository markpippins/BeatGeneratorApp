package com.angrysurfer.beats.visualization.handler;

import com.angrysurfer.beats.visualization.DisplayType;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class ScrollingTextVisualizer implements IVisualizationHandler {
    private int position = 0;
    private static final String MESSAGE = "Your beats suck!";
    private static final int CHAR_WIDTH = 4;
    private static final int CHAR_HEIGHT = 5;
    private static final int PADDING = 1;  // One row padding top and bottom
    
    // LED font definitions - 1 represents lit pixel, 0 represents unlit
    private static final int[][][] FONT = {
        // A
        {{0,1,1,0},
         {1,0,0,1},
         {1,1,1,1},
         {1,0,0,1},
         {1,0,0,1}},
        // B
        {{1,1,1,0},
         {1,0,0,1},
         {1,1,1,0},
         {1,0,0,1},
         {1,1,1,0}},
        // C
        {{0,1,1,1},
         {1,0,0,0},
         {1,0,0,0},
         {1,0,0,0},
         {0,1,1,1}},
        // D
        {{1,1,1,0},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {1,1,1,0}},
        // E
        {{1,1,1,1},
         {1,0,0,0},
         {1,1,1,0},
         {1,0,0,0},
         {1,1,1,1}},
        // F
        {{1,1,1,1},
         {1,0,0,0},
         {1,1,1,0},
         {1,0,0,0},
         {1,0,0,0}},
        // G
        {{0,1,1,1},
         {1,0,0,0},
         {1,0,1,1},
         {1,0,0,1},
         {0,1,1,1}},
        // H
        {{1,0,0,1},
         {1,0,0,1},
         {1,1,1,1},
         {1,0,0,1},
         {1,0,0,1}},
        // I
        {{1,1,1,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {1,1,1,0}},
        // J
        {{0,0,1,1},
         {0,0,0,1},
         {0,0,0,1},
         {1,0,0,1},
         {0,1,1,0}},
        // K
        {{1,0,0,1},
         {1,0,1,0},
         {1,1,0,0},
         {1,0,1,0},
         {1,0,0,1}},
        // L
        {{1,0,0,0},
         {1,0,0,0},
         {1,0,0,0},
         {1,0,0,0},
         {1,1,1,1}},
        // M
        {{1,0,0,1},
         {1,1,1,1},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,0,1}},
        // N
        {{1,0,0,1},
         {1,1,0,1},
         {1,0,1,1},
         {1,0,0,1},
         {1,0,0,1}},
        // O
        {{0,1,1,0},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {0,1,1,0}},
        // P
        {{1,1,1,0},
         {1,0,0,1},
         {1,1,1,0},
         {1,0,0,0},
         {1,0,0,0}},
        // Q
        {{0,1,1,0},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,1,0},
         {0,1,0,1}},
        // R
        {{1,1,1,0},
         {1,0,0,1},
         {1,1,1,0},
         {1,0,1,0},
         {1,0,0,1}},
        // S
        {{0,1,1,1},
         {1,0,0,0},
         {0,1,1,0},
         {0,0,0,1},
         {1,1,1,0}},
        // T
        {{1,1,1,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,1,0,0}},
        // U
        {{1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {0,1,1,0}},
        // V
        {{1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {0,1,1,0},
         {0,0,1,0}},
        // W
        {{1,0,0,1},
         {1,0,0,1},
         {1,0,0,1},
         {1,1,1,1},
         {0,1,1,0}},
        // X
        {{1,0,0,1},
         {0,1,1,0},
         {0,1,1,0},
         {0,1,1,0},
         {1,0,0,1}},
        // Y
        {{1,0,0,1},
         {0,1,1,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,1,0,0}},
        // Z
        {{1,1,1,1},
         {0,0,1,0},
         {0,1,0,0},
         {1,0,0,0},
         {1,1,1,1}},
        // 0
        {{0,1,1,0},
         {1,0,1,1},
         {1,1,0,1},
         {1,0,0,1},
         {0,1,1,0}},
        // 1
        {{0,1,0,0},
         {1,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {1,1,1,0}},
        // 2
        {{0,1,1,0},
         {1,0,0,1},
         {0,0,1,0},
         {0,1,0,0},
         {1,1,1,1}},
        // 3
        {{1,1,1,0},
         {0,0,0,1},
         {0,1,1,0},
         {0,0,0,1},
         {1,1,1,0}},
        // 4
        {{1,0,0,1},
         {1,0,0,1},
         {1,1,1,1},
         {0,0,0,1},
         {0,0,0,1}},
        // 5
        {{1,1,1,1},
         {1,0,0,0},
         {1,1,1,0},
         {0,0,0,1},
         {1,1,1,0}},
        // 6
        {{0,1,1,0},
         {1,0,0,0},
         {1,1,1,0},
         {1,0,0,1},
         {0,1,1,0}},
        // 7
        {{1,1,1,1},
         {0,0,0,1},
         {0,0,1,0},
         {0,1,0,0},
         {0,1,0,0}},
        // 8
        {{0,1,1,0},
         {1,0,0,1},
         {0,1,1,0},
         {1,0,0,1},
         {0,1,1,0}},
        // 9
        {{0,1,1,0},
         {1,0,0,1},
         {0,1,1,1},
         {0,0,0,1},
         {0,1,1,0}},
        // space
        {{0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,0,0,0}},
        // .
        {{0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,0,0,0},
         {0,1,0,0}},
        // !
        {{0,1,0,0},
         {0,1,0,0},
         {0,1,0,0},
         {0,0,0,0},
         {0,1,0,0}},
        // ?
        {{0,1,1,0},
         {1,0,0,1},
         {0,0,1,0},
         {0,0,0,0},
         {0,0,1,0}}
    };

    private static final int[][] getCharacter(char c) {
        if (Character.isWhitespace(c)) return FONT[36];  // space
        
        if (Character.isLetter(c)) {
            int index = Character.toLowerCase(c) - 'a';
            if (index >= 0 && index < 26) return FONT[index];
        }
        
        if (Character.isDigit(c)) {
            int index = (c - '0') + 26;
            if (index >= 26 && index < 36) return FONT[index];
        }
        
        // Special characters
        return switch (c) {
            case '.' -> FONT[37];
            case '!' -> FONT[38];
            case '?' -> FONT[39];
            default -> FONT[36];  // space for unknown characters
        };
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

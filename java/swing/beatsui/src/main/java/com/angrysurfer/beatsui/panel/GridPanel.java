package com.angrysurfer.beatsui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

public class GridPanel extends JPanel {

    public GridPanel() {
        super(new GridLayout(5, 33, 2, 2));
        setup();
    }

    static int BUTTON_SIZE = 25;
    static int GRID_ROWS = 5;
    static int GRID_COLS = 36;

    private void setup() {

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create 5x33 grid of buttons with varying colors
        Color[] colors = {
                new Color(255, 200, 200), // Light red
                new Color(200, 255, 200), // Light green
                new Color(200, 200, 255), // Light blue
                new Color(255, 255, 200), // Light yellow
                new Color(255, 200, 255), // Light purple
                new Color(200, 255, 255) // Light cyan
        };

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE)); // Make buttons square

                // Vary colors based on position
                int colorIndex = (row * col) % colors.length;
                button.setBackground(colors[colorIndex]);
                button.setOpaque(true);
                button.setBorderPainted(true);

                // Optional: Add tooltip showing position
                button.setToolTipText(String.format("Row: %d, Col: %d", row + 1, col + 1));

                add(button);
            }
        }
    }
}

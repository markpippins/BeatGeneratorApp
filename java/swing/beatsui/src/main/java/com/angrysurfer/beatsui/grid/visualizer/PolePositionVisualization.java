package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;
import java.util.Random;

public class PolePositionVisualization implements Visualization {
    private double roadX = 0;
    private double curvePhase = 0;
    private double speed = 1.0;
    private int carPosition = 18;
    private final Random random = new Random();
    
    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update road position
        roadX += Math.sin(curvePhase) * 0.1 * speed;
        curvePhase += 0.02 * speed;

        // Draw road
        for (int y = 0; y < buttons.length; y++) {
            // Calculate perspective and road curvature
            double perspective = (y + 1.0) / buttons.length;
            double roadWidth = 20 * perspective;
            double xOffset = roadX * (1 - perspective) * 10;
            
            int centerX = buttons[0].length / 2 + (int)xOffset;
            int leftEdge = centerX - (int)(roadWidth / 2);
            int rightEdge = centerX + (int)(roadWidth / 2);
            
            // Draw road and stripes
            for (int x = 0; x < buttons[0].length; x++) {
                if (x >= leftEdge && x <= rightEdge) {
                    buttons[y][x].setBackground(Color.DARK_GRAY);
                    
                    // Draw center line
                    if (Math.abs(x - centerX) < 1 && (y + (int)(speed * 10)) % 2 == 0) {
                        buttons[y][x].setBackground(Color.YELLOW);
                    }
                } else {
                    // Draw grass
                    buttons[y][x].setBackground(new Color(0, 100, 0));
                }
            }
        }

        // AI steering
        double targetX = -Math.sin(curvePhase + 0.5) * 5;
        double diff = targetX - roadX;
        if (Math.abs(diff) > 0.1) {
            carPosition += Math.signum(diff);
            carPosition = Math.max(0, Math.min(buttons[0].length - 1, carPosition));
        }

        // Draw car
        int carY = buttons.length - 2;
        buttons[carY][carPosition].setBackground(Color.RED);
        if (carPosition > 0) buttons[carY][carPosition - 1].setBackground(Color.RED);
        if (carPosition < buttons[0].length - 1) buttons[carY][carPosition + 1].setBackground(Color.RED);
        buttons[carY - 1][carPosition].setBackground(Color.RED);

        // Vary speed
        if (random.nextInt(30) == 0) {
            speed = 0.5 + random.nextDouble();
        }
    }

    @Override
    public String getName() {
        return "Pole Position";
    }
}

package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationCategory;
import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class LoopPulseVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private final Random random = new Random();
    private int[] loopStates;
    private double phase = 0.0;
    private final double[] speeds = {1.0, 0.5, 0.25, 2.0, 1.5, 0.75, 0.33, 1.25};

    @Override
    public void update(GridButton[][] buttons) {
        if (loopStates == null) {
            loopStates = new int[buttons.length];
            for (int i = 0; i < loopStates.length; i++) {
                loopStates[i] = random.nextInt(buttons[0].length);
            }
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw loop positions
        for (int row = 0; row < buttons.length; row++) {
            int pos = (int)(phase * speeds[row % speeds.length]) % buttons[0].length;
            Color rowColor = Color.getHSBColor((float)row / buttons.length, 0.8f, 1.0f);
            
            // Draw main pulse
            buttons[row][pos].setBackground(Color.WHITE);
            
            // Draw trail
            for (int i = 1; i <= 3; i++) {
                int trailPos = (pos - i + buttons[0].length) % buttons[0].length;
                int brightness = 255 - (i * 60);
                buttons[row][trailPos].setBackground(new Color(
                    blend(rowColor.getRed(), brightness),
                    blend(rowColor.getGreen(), brightness),
                    blend(rowColor.getBlue(), brightness)
                ));
            }
        }
        phase += 0.2;
    }

    private int blend(int color, int brightness) {
        return Math.max(0, Math.min(255, (color * brightness) / 255));
    }

    @Override
    public String getName() {
        return "Loop Pulse";
    }
}

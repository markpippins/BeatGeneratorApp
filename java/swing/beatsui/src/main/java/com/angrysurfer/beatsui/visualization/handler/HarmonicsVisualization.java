package com.angrysurfer.beatsui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.visualization.VisualizationCategory;
import com.angrysurfer.beatsui.visualization.VisualizationHandler;
import com.angrysurfer.beatsui.visualization.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class HarmonicsVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private double phase = 0.0;
    private final Color[] harmonicColors = {
            Color.RED, // Fundamental
            Color.ORANGE, // 2nd harmonic
            Color.YELLOW, // 3rd harmonic
            Color.GREEN, // 4th harmonic
            Color.CYAN, // 5th harmonic
            Color.BLUE, // 6th harmonic
            Color.MAGENTA, // 7th harmonic
            Color.PINK // 8th harmonic
    };

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        double baseFreq = 1.0;
        for (int harmonic = 1; harmonic <= buttons.length; harmonic++) {
            double freq = baseFreq * harmonic;
            Color harmonicColor = harmonicColors[(harmonic - 1) % harmonicColors.length];

            for (int col = 0; col < buttons[0].length; col++) {
                double amplitude = Math.sin(phase * freq + col * 0.1);
                if (amplitude > 0.7) {
                    int brightness = (int) ((amplitude - 0.7) * 850);
                    Color color = new Color(
                            blend(0, harmonicColor.getRed(), brightness),
                            blend(0, harmonicColor.getGreen(), brightness),
                            blend(0, harmonicColor.getBlue(), brightness));
                    buttons[harmonic - 1][col].setBackground(color);
                }
            }
        }
        phase += 0.1;
    }

    private int blend(int c1, int c2, int amount) {
        return Math.min(255, Math.max(0, (c1 * (255 - amount) + c2 * amount) / 255));
    }

    @Override
    public String getName() {
        return "Harmonic Series";
    }
}

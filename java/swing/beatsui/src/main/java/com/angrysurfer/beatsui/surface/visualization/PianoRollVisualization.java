package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationCategory;
import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class PianoRollVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private final Random random = new Random();
    private int seqPosition = 0;

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        seqPosition = (seqPosition + 1) % buttons[0].length;

        // Create piano roll style notes
        for (int row = 0; row < buttons.length; row++) {
            // Different note lengths for variety
            int noteLength = random.nextInt(4) + 1;
            for (int col = 0; col < buttons[0].length; col++) {
                if (col % (8 * noteLength) == 0) {
                    // Draw a note of random length
                    Color noteColor = row % 2 == 0 ? Color.MAGENTA : Color.CYAN;
                    for (int i = 0; i < noteLength && col + i < buttons[0].length; i++) {
                        buttons[row][col + i].setBackground(
                            col + i == seqPosition ? Color.WHITE : noteColor);
                    }
                } else if (col == seqPosition) {
                    buttons[row][col].setBackground(Color.DARK_GRAY);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Piano Roll";
    }
}

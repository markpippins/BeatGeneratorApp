package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.Utils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class PianoRollVisualization implements IVisualizationHandler {

    private final Random random = new Random();
    private int seqPosition = 0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());
        
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

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

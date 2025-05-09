package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class MidiGridVisualization extends LockHandler implements IVisualizationHandler {
    private final Random random = new Random();
    private int seqPosition = 0;
    private boolean[][] midiGrid;

    @Override
    public void update(JButton[][] buttons) {

       

        if (midiGrid == null) {
            midiGrid = new boolean[buttons.length][buttons[0].length];
        }

        // Randomly trigger new notes
        if (random.nextInt(100) < 10) {
            midiGrid[random.nextInt(buttons.length)][random.nextInt(buttons[0].length)] = true;
        }

        // Update sequence position
        seqPosition = (seqPosition + 1) % buttons[0].length;

        // Draw grid
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (midiGrid[row][col]) {
                    buttons[row][col].setBackground(
                            col == seqPosition ? Color.WHITE : Color.BLUE);
                    // Fade out notes
                    if (random.nextInt(100) < 5) {
                        midiGrid[row][col] = false;
                    }
                } else if (col == seqPosition) {
                    buttons[row][col].setBackground(Color.DARK_GRAY);
                } else {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "MIDI Grid";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

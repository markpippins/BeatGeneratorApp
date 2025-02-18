package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class StepSequencerVisualization implements IVisualizationHandler {
    
    private final Random random = new Random();
    private int seqPosition = 0;
    private boolean[][] sequencerGrid;

    @Override
    public void update(GridButton[][] buttons) {
        if (sequencerGrid == null) {
            sequencerGrid = new boolean[buttons.length][buttons[0].length];
            randomizePattern();
        }

        seqPosition = (seqPosition + 1) % 16; // 16-step sequence
        int stepWidth = buttons[0].length / 16;

        // Draw 4 instrument tracks
        for (int track = 0; track < buttons.length; track++) {
            for (int step = 0; step < 16; step++) {
                Color trackColor = switch (track) {
                    case 0 -> Color.RED;    // Kick
                    case 1 -> Color.YELLOW; // Snare
                    case 2 -> Color.CYAN;   // Hi-hat
                    case 3 -> Color.GREEN;  // Percussion
                    default -> Color.GRAY;
                };

                for (int x = step * stepWidth; x < (step + 1) * stepWidth; x++) {
                    buttons[track][x].setBackground(
                        step == seqPosition ? Color.WHITE : 
                        sequencerGrid[track][step] ? trackColor : 
                        buttons[0][0].getParent().getBackground()
                    );
                }
            }
        }
    }

    private void randomizePattern() {
        for (int track = 0; track < sequencerGrid.length; track++) {
            for (int step = 0; step < 16; step++) {
                sequencerGrid[track][step] = random.nextInt(100) < 20;
            }
        }
    }

    @Override
    public String getName() {
        return "Step Sequencer";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

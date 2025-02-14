package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationCategory;
import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.surface.VisualizationUtils;
import com.angrysurfer.beatsui.widget.GridButton;

public class GateSequencerVisualization implements VisualizationHandler {

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
    
    private final Random random = new Random();
    private int seqPosition = 0;
    private final int[] gateLengths = new int[8];
    private final boolean[][] gateStates;

    public GateSequencerVisualization() {
        gateStates = new boolean[8][16];
        initializeGates();
    }

    private void initializeGates() {
        for (int i = 0; i < gateLengths.length; i++) {
            gateLengths[i] = random.nextInt(4) + 1;
            for (int step = 0; step < 16; step++) {
                gateStates[i][step] = random.nextInt(100) < 30;
            }
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        int stepWidth = buttons[0].length / 16;
        seqPosition = (seqPosition + 1) % 16;

        // Draw gate patterns
        for (int row = 0; row < Math.min(buttons.length, gateLengths.length); row++) {
            for (int step = 0; step < 16; step++) {
                if (gateStates[row][step]) {
                    Color gateColor = (step % 2 == 0) ? Color.RED : Color.ORANGE;
                    for (int x = step * stepWidth; x < (step + 1) * stepWidth; x++) {
                        buttons[row][x].setBackground(
                            step == seqPosition ? Color.WHITE : gateColor
                        );
                    }
                } else if (step == seqPosition) {
                    for (int x = step * stepWidth; x < (step + 1) * stepWidth; x++) {
                        buttons[row][x].setBackground(Color.DARK_GRAY);
                    }
                }
            }
        }

        // Occasionally modify patterns
        if (random.nextInt(100) < 5) {
            int row = random.nextInt(gateLengths.length);
            int step = random.nextInt(16);
            gateStates[row][step] = !gateStates[row][step];
        }
    }

    @Override
    public String getName() {
        return "Gate Sequencer";
    }
}

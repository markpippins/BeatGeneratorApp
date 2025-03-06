package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;
import com.angrysurfer.beats.visualization.*;
import com.angrysurfer.beats.widget.GridButton;

public class GateSequencerVisualization extends LockHandler implements IVisualizationHandler {
    private int currentStep = 0;
    private boolean[][] gates;
    private static final int NUM_CHANNELS = 8; // Increased from 4 to use more vertical space
    private static final Color[] CHANNEL_COLORS = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK
    };

    public GateSequencerVisualization() {
        gates = new boolean[NUM_CHANNELS][48]; // Match grid width
        // Initialize some random patterns
        for (int channel = 0; channel < NUM_CHANNELS; channel++) {
            for (int step = 0; step < gates[0].length; step++) {
                gates[channel][step] = Math.random() > 0.75;
            }
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        int gridHeight = buttons.length;
        int channelSpacing = gridHeight / (NUM_CHANNELS + 1); // Distribute channels evenly
        
        // Draw gates for each channel
        for (int channel = 0; channel < NUM_CHANNELS; channel++) {
            int row = (channel + 1) * channelSpacing;
            Color channelColor = CHANNEL_COLORS[channel];
            
            for (int col = 0; col < gates[0].length; col++) {
                if (gates[channel][col]) {
                    // Draw taller gates
                    for (int gateHeight = -1; gateHeight <= 1; gateHeight++) {
                        int displayRow = row + gateHeight;
                        if (displayRow >= 0 && displayRow < buttons.length) {
                            buttons[displayRow][col].setBackground(channelColor);
                        }
                    }
                }
                
                // Highlight current step
                if (col == currentStep) {
                    for (int i = 0; i < buttons.length; i++) {
                        if (buttons[i][col].getBackground() == Color.BLACK) {
                            buttons[i][col].setBackground(Color.DARK_GRAY);
                        }
                    }
                }
            }
        }
        
        currentStep = (currentStep + 1) % gates[0].length;
    }

    @Override
    public String getName() {
        return "Gate Sequencer";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

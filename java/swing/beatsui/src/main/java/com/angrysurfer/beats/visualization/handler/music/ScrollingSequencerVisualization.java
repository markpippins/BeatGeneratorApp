package com.angrysurfer.beats.visualization.handler.music;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class ScrollingSequencerVisualization extends LockHandler implements IVisualizationHandler {

    private static final int BEATS_PER_BAR = 4;
    private static final int TOTAL_BARS = 4;
    private static final Color BEAT_MARKER_COLOR = new Color(40, 40, 40);
    private static final Color BAR_MARKER_COLOR = new Color(60, 60, 60);
    private static final Color POSITION_INDICATOR = Color.WHITE;
    
    private int position = 0;

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Draw beat and bar markers first (background)
        for (int col = 0; col < buttons[0].length; col++) {
            if (col % (BEATS_PER_BAR * 4) == 0) {
                for (int row = 0; row < buttons.length; row++) {
                    buttons[row][col].setBackground(BAR_MARKER_COLOR);
                }
            }
            else if (col % 4 == 0) {
                for (int row = 0; row < buttons.length; row++) {
                    buttons[row][col].setBackground(BEAT_MARKER_COLOR);
                }
            }
        }

        position = (position + 1) % buttons[0].length;

        // Draw vertical position indicator
        for (int row = 0; row < buttons.length; row++) {
            buttons[row][position].setBackground(POSITION_INDICATOR);
        }
    }

    @Override
    public String getName() {
        return "Scrolling Sequencer";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }
}

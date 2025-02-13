package com.angrysurfer.beatsui.surface.visualizer;

import java.awt.Color;

import com.angrysurfer.beatsui.surface.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class BounceVisualization implements Visualization {
    private int bouncePos = 1;
    private boolean bounceUp = true;

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        for (int col = 0; col < buttons[0].length; col++) {
            buttons[bouncePos][col].setBackground(Color.YELLOW);
        }

        if (bounceUp) {
            bouncePos--;
            if (bouncePos < 0) {
                bouncePos = 0;
                bounceUp = false;
            }
        } else {
            bouncePos++;
            if (bouncePos >= buttons.length) {
                bouncePos = buttons.length - 1;
                bounceUp = true;
            }
        }
    }

    @Override
    public String getName() {
        return "Bounce";
    }
}

package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class BounceVisualization implements IVisualizationHandler {
    private int bouncePos = 1;
    private boolean bounceUp = true;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());
        
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

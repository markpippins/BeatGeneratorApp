package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.visualization.Utils;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class DNAVisualization implements IVisualizationHandler {
    private double angle = 0;

    @Override
    public void update(GridButton[][] buttons) {
        Utils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int col = 0; col < buttons[0].length; col++) {
            double offset = angle + col * 0.3;
            int row1 = (int) (2 + Math.sin(offset) * 1.2);
            int row2 = (int) (2 + Math.sin(offset + Math.PI) * 1.2);

            if (row1 >= 0 && row1 < buttons.length) {
                buttons[row1][col].setBackground(Color.BLUE);
            }
            if (row2 >= 0 && row2 < buttons.length) {
                buttons[row2][col].setBackground(Color.RED);
            }
            
            if (col % 4 == 0) {
                int middle = (row1 + row2) / 2;
                if (middle >= 0 && middle < buttons.length) {
                    buttons[middle][col].setBackground(Color.GREEN);
                }
            }
        }
        angle += 0.1;
    }

    @Override
    public String getName() {
        return "DNA Helix";
    }
}

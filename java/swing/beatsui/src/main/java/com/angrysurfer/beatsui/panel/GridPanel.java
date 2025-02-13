package com.angrysurfer.beatsui.panel;

import java.awt.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.angrysurfer.beatsui.widget.GridButton;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.grid.Display;

public class GridPanel extends StatusProviderPanel {

    private GridButton[][] buttons;
    private Display gridSaver;

    public GridPanel() {
        this(null);
    }

    public GridPanel(StatusConsumer statusConsumer) {
        super(new GridLayout(Display.GRID_ROWS, Display.GRID_COLS, 2, 2), statusConsumer);
        setup();
        gridSaver = new Display(this, statusConsumer, buttons);
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons = new GridButton[Display.GRID_ROWS][Display.GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gridSaver.isScreensaverMode()) {
                    gridSaver.stopScreensaver();
                }
            }
        };

        for (int row = 0; row < Display.GRID_ROWS; row++) {
            for (int col = 0; col < Display.GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                buttons[row][col].addMouseListener(mouseHandler);
                add(buttons[row][col]);
            }
        }
    }
}

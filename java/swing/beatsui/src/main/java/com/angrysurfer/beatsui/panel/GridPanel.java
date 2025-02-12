package com.angrysurfer.beatsui.panel;

import java.awt.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.angrysurfer.beatsui.widget.GridButton;
import com.angrysurfer.beatsui.api.StatusConsumer;

public class GridPanel extends StatusProviderPanel {

    private static final int GRID_ROWS = 5;
    private static final int GRID_COLS = 36;

    private GridButton[][] buttons;
    private GridSaver gridSaver;

    public GridPanel() {
        this(null);
    }

    public GridPanel(StatusConsumer statusConsumer) {
        super(new GridLayout(GRID_ROWS, GRID_COLS, 2, 2), statusConsumer);
        setup();
        gridSaver = new GridSaver(statusConsumer, buttons);
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons = new GridButton[GRID_ROWS][GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gridSaver.isScreensaverMode()) {
                    gridSaver.stopScreensaver();
                }
            }
        };

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                buttons[row][col].addMouseListener(mouseHandler);
                add(buttons[row][col]);
            }
        }
    }
}

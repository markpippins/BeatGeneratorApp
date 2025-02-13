package com.angrysurfer.beatsui.panel;

import java.awt.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.angrysurfer.beatsui.widget.GridButton;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.screensaver.GridSaver;

public class GridPanel extends StatusProviderPanel {

    private GridButton[][] buttons;
    private GridSaver gridSaver;

    public GridPanel() {
        this(null);
    }

    public GridPanel(StatusConsumer statusConsumer) {
        super(new GridLayout(GridSaver.GRID_ROWS, GridSaver.GRID_COLS, 2, 2), statusConsumer);
        setup();
        gridSaver = new GridSaver(this, statusConsumer, buttons);
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons = new GridButton[GridSaver.GRID_ROWS][GridSaver.GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gridSaver.isScreensaverMode()) {
                    gridSaver.stopScreensaver();
                }
            }
        };

        for (int row = 0; row < GridSaver.GRID_ROWS; row++) {
            for (int col = 0; col < GridSaver.GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                buttons[row][col].addMouseListener(mouseHandler);
                add(buttons[row][col]);
            }
        }
    }
}

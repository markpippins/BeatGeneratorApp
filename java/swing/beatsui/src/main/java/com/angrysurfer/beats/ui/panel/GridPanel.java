package com.angrysurfer.beats.ui.panel;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;

import com.angrysurfer.beats.ui.visualization.Visualizer;
import com.angrysurfer.beats.ui.widget.GridButton;
import com.angrysurfer.core.api.StatusConsumer;

public class GridPanel extends StatusProviderPanel {

    private GridButton[][] buttons;
    private Visualizer gridSaver;

    static int GRID_ROWS = 8;
    static int GRID_COLS = 24;

    public GridPanel() {
        this(null);
    }

    public GridPanel(StatusConsumer statusConsumer) {
        super(new GridLayout(GRID_ROWS, GRID_COLS, 2, 2), statusConsumer);
        setup();
        gridSaver = new Visualizer(this, statusConsumer, buttons);
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons = new GridButton[GRID_ROWS][GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gridSaver.isVisualizationMode()) {
                    gridSaver.stopVisualizer();
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

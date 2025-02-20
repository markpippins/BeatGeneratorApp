package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Random;

import javax.swing.JButton;

public class GridButton extends JButton {

    public static final int BUTTON_SIZE = 15;

    Color[] colors = {
            new Color(255, 200, 200), // Light red
            new Color(200, 255, 200), // Light green
            new Color(200, 200, 255), // Light blue
            new Color(255, 255, 200), // Light yellow
            new Color(255, 200, 255), // Light purple
            new Color(200, 255, 255) // Light cyan
    };

    private final Random rand = new Random();

    private int row;
    private int col;
    private boolean on;

    public GridButton() {
        super();
        setup();
    }

    public GridButton(int row, int col) {
        this.row = row;
        this.col = col;
        setup();
    }

    public void clear() {
        setText("");
        setToolTipText("");
        setBackground(getParent().getBackground());
    }

    public void reset() {
        setText("");
        setToolTipText("");
        setBackground(getParent().getBackground());
    }

    public void randomize() {
        setText("");
        setToolTipText("");
        setBackground(colors[rand.nextInt(colors.length)]);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setOn(boolean on) {
        this.on = on;
        if (!on) {
            setBackground(Color.RED);
        }
        repaint();
    }

    private void setup() {
        setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setBackground(colors[rand.nextInt(colors.length)]);
        setOpaque(true);
        setBorderPainted(true);
    }
}

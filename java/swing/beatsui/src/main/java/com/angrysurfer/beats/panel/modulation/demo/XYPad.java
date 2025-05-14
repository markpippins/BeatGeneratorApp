package com.angrysurfer.beats.panel.modulation.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class XYPad extends JPanel {
    private XYPadState state;
    private final int padSize = 150;

    public XYPad(XYPadState state) {
        this.state = state;
        setPreferredSize(new Dimension(padSize, padSize));
        setBackground(Color.DARK_GRAY);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { updateState(e); }
            @Override
            public void mouseDragged(MouseEvent e) { updateState(e); }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void updateState(MouseEvent e) {
        int x = Math.max(0, Math.min(e.getX(), getWidth()));
        int y = Math.max(0, Math.min(e.getY(), getHeight()));
        state.x = x / (float) getWidth();
        state.y = y / (float) getHeight();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int knobX = (int) (state.x * getWidth());
        int knobY = (int) (state.y * getHeight());

        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        g.setColor(Color.RED);
        g.fillOval(knobX - 5, knobY - 5, 10, 10);
    }
}


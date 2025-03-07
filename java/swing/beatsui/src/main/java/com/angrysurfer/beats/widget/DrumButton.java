package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;

public class DrumButton extends JButton {

    // private int row;
    // private int col;

    // public DrumButton(int row, int col) {
    // this.row = row;
    // this.col = col;
    // }

    // public int getRow() {
    // return row;
    // }

    // public int getCol() {
    // return col;
    // }

    public DrumButton() {
        super();
        // setUI(new BasicButtonUI());
        // setContentAreaFilled(false);
        // setFocusable(false);
        // setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // setBorderPainted(false);
        // setOpaque(true);
        setup();
    }

    private void setup() {
        Color baseColor = new Color(50, 130, 200); // A vibrant, cool blue
        // new Color(60, 60, 60); // Dark grey base
        Color flashColor = new Color(160, 160, 160); // Lighter grey for flash
        final boolean[] isFlashing = { false };

        addActionListener(e -> {
            isFlashing[0] = true;
            repaint();

            Timer timer = new Timer(100, evt -> {
                isFlashing[0] = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                if (isFlashing[0]) {
                    g2d.setColor(flashColor);
                } else {
                    g2d.setColor(baseColor);
                }

                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                g2d.dispose();
            }
        });

        setPreferredSize(new Dimension(40, 40));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

}

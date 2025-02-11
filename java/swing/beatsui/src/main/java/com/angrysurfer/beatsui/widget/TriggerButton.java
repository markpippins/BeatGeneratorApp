package com.angrysurfer.beatsui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class TriggerButton extends JButton {

    public TriggerButton() {
        super();
        setup();
    }

    private void setup() {
        // setFocusPainted(false);
        // setBorderPainted(false);
        // setContentAreaFilled(false);
        setPreferredSize(new Dimension(30, 20));
        setMinimumSize(new Dimension(30, 20));
        setMaximumSize(new Dimension(30, 20));

        final boolean[] isActive = { false };

        // Orange for inactive state
        Color topColorInactive = new Color(255, 140, 0); // Bright orange
        Color bottomColorInactive = new Color(200, 110, 0); // Darker orange

        // Green for active state
        Color topColorActive = new Color(50, 255, 50); // Bright green
        Color bottomColorActive = new Color(40, 200, 40); // Darker green

        addActionListener(e -> {
            isActive[0] = !isActive[0];
            repaint();
        });

        setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Choose colors based on state
                Color topColor = isActive[0] ? topColorActive : topColorInactive;
                Color bottomColor = isActive[0] ? bottomColorActive : bottomColorInactive;

                GradientPaint gp = new GradientPaint(
                        0, 0, topColor,
                        0, h, bottomColor);

                g2d.setPaint(gp);
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

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        // setToolTipText("Step " + (index + 1));
    }

}

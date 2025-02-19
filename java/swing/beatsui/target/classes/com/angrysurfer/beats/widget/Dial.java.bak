package com.angrysurfer.beatsui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import com.angrysurfer.beatsui.Utils;

// Simple JDial implementation for knobs
public class Dial extends JComponent {
    private int value = 64;
    private boolean isDragging = false;
    private int lastY;

    public Dial() {
        setPreferredSize(new Dimension(50, 50));

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                isDragging = true;
                lastY = e.getY();
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (isDragging) {
                    int delta = lastY - e.getY();
                    value = Math.min(127, Math.max(0, value + delta));
                    lastY = e.getY();
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int min = Math.min(w, h);

        // Draw knob body with blue color
        // g2d.setColor(new Color(30, 100, 255));
        g2d.fillOval(0, 0, min - 1, min - 1);

        // Add a subtle gradient for 3D effect
        // GradientPaint gp = new GradientPaint(
        // 0, 0, new Color(60, 130, 255),
        // 0, h, new Color(20, 80, 200));

        GradientPaint gp = new GradientPaint(
                0, 0, Utils.deepNavy,
                0, h, Utils.warmOffWhite);

        g2d.setPaint(gp);
        g2d.fillOval(2, 2, min - 4, min - 4);

        // Draw white indicator line
        g2d.setColor(Color.WHITE);
        // g2d.setStroke(new BasicStroke(2.0f));
        double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);
        int centerX = min / 2;
        int centerY = min / 2;
        int radius = min / 2 - 6;

        g2d.drawLine(centerX, centerY,
                centerX + (int) (Math.cos(angle) * radius),
                centerY + (int) (Math.sin(angle) * radius));

        // Add highlight for 3D effect
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.fillOval(5, 3, min / 2 - 5, min / 2 - 5);

        g2d.dispose();
    }

    public void setValue(int newValue) {
        value = Math.min(127, Math.max(0, newValue));
        repaint();
    }

    public int getValue() {
        return value;
    }
}
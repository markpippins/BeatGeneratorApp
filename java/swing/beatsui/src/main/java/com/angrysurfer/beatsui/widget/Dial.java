package com.angrysurfer.beatsui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;

import javax.swing.JComponent;

import com.angrysurfer.beatsui.Utils;

// Simple JDial implementation for knobs
public class Dial extends JComponent {
    private int value = 64;
    private boolean isDragging = false;
    private int lastY;

    public Dial() {
        setPreferredSize(new Dimension(60, 60));  // Increased size
        setMinimumSize(new Dimension(60, 60));    // Added minimum size

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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int min = Math.min(w, h);

        // Draw border
        g2d.setColor(Utils.charcoalGray);
        g2d.fillOval(0, 0, min - 1, min - 1);

        // Draw knob body
        GradientPaint gp = new GradientPaint(
                0, 0, Utils.deepNavy,
                0, h, Utils.warmOffWhite.darker());
        g2d.setPaint(gp);
        g2d.fillOval(4, 4, min - 8, min - 8);

        // Draw indicator line
        g2d.setColor(Utils.warmOffWhite);
        g2d.setStroke(new BasicStroke(2.0f));
        double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);
        int centerX = min / 2;
        int centerY = min / 2;
        int radius = min / 2 - 6;

        g2d.drawLine(centerX, centerY,
                centerX + (int) (Math.cos(angle) * radius),
                centerY + (int) (Math.sin(angle) * radius));

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
package com.angrysurfer.beats.ui.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Window;

import com.angrysurfer.beats.ui.Utils;

public class Dial extends JComponent {
    private static final int MIN_SIZE = 50;
    private static final int MAX_SIZE = 70;
    private static final int BASE_WINDOW_WIDTH = 1200;
    private static final int BASE_WINDOW_HEIGHT = 800;
    private int value = 64;
    private boolean isDragging = false;
    private int lastY;

    public Dial() {
        updateSize();
        
        // Register for ancestor window resize events
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                Window window = SwingUtilities.getWindowAncestor(Dial.this);
                if (window != null) {
                    window.addComponentListener(new java.awt.event.ComponentAdapter() {
                        public void componentResized(java.awt.event.ComponentEvent e) {
                            updateSize();
                        }
                    });
                }
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });

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

    private void updateSize() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            // Calculate size based on window dimensions
            double widthRatio = (double) window.getWidth() / BASE_WINDOW_WIDTH;
            double heightRatio = (double) window.getHeight() / BASE_WINDOW_HEIGHT;
            double scaleFactor = Math.min(Math.max(widthRatio, heightRatio), MAX_SIZE / (double) MIN_SIZE);
            
            int size = Math.min(MAX_SIZE, (int) (MIN_SIZE * scaleFactor));
            
            Dimension newSize = new Dimension(size, size);
            setPreferredSize(newSize);
            setMinimumSize(newSize);
            revalidate();
        } else {
            // Default size when no window is available
            setPreferredSize(new Dimension(MIN_SIZE, MIN_SIZE));
            setMinimumSize(new Dimension(MIN_SIZE, MIN_SIZE));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int min = Math.min(w, h);

        // Draw outer rim (brushed aluminum effect)
        g2d.setPaint(new GradientPaint(
            0, 0,
            new Color(180, 180, 180),
            0, h,
            new Color(120, 120, 120)
        ));
        g2d.fillOval(0, 0, min - 1, min - 1);

        // Draw main knob body (vintage bakelite look)
        g2d.setPaint(new GradientPaint(
            min/4f, min/4f,
            new Color(40, 40, 40),
            min/2f, min/2f,
            new Color(20, 20, 20)
        ));
        g2d.fillOval(4, 4, min - 8, min - 8);

        // Add highlight reflection
        g2d.setPaint(new GradientPaint(
            min/4f, min/4f,
            new Color(255, 255, 255, 30),
            min/2f, min/2f,
            new Color(255, 255, 255, 0)
        ));
        g2d.fillOval(6, 6, min - 12, min - 12);

        // Draw indicator groove
        g2d.setColor(new Color(200, 200, 200, 60));
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);
        int centerX = min / 2;
        int centerY = min / 2;
        int radius = min / 2 - 6;

        // Draw indicator line with metallic effect
        GradientPaint lineGradient = new GradientPaint(
            centerX, centerY,
            new Color(220, 220, 220),
            centerX + (float)(Math.cos(angle) * radius),
            centerY + (float)(Math.sin(angle) * radius),
            new Color(180, 180, 180)
        );
        g2d.setPaint(lineGradient);
        g2d.drawLine(centerX, centerY,
                centerX + (int) (Math.cos(angle) * radius),
                centerY + (int) (Math.sin(angle) * radius));

        // Add center dot
        g2d.setPaint(new GradientPaint(
            centerX - 2, centerY - 2,
            new Color(100, 100, 100),
            centerX + 2, centerY + 2,
            new Color(60, 60, 60)
        ));
        g2d.fillOval(centerX - 2, centerY - 2, 4, 4);

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
package com.angrysurfer.beats.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.angrysurfer.core.api.CommandBus;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Dial extends JComponent {
    private static final int MIN_SIZE = 50;
    private static final int MAX_SIZE = 70;
    private static final Color KNOB_COLOR = new Color(30, 100, 255);
    private static final Color KNOB_GRADIENT_START = new Color(60, 130, 255);
    private static final Color KNOB_GRADIENT_END = new Color(20, 80, 200);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 60);
    private static final int BASE_WINDOW_WIDTH = 1200;
    private static final int BASE_WINDOW_HEIGHT = 800;
    private int value = 64;
    private boolean isDragging = false;
    private int lastY;
    private int min = 0;
    private int max = 127;
    private String command;
    private List<ChangeListener> changeListeners = new ArrayList<>();

    public Dial() {
        this.command = null;
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

            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
            }

            public void ancestorMoved(javax.swing.event.AncestorEvent e) {
            }
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
                    int newValue = Math.min(max, Math.max(min, value + delta));
                    setValue(newValue);
                    lastY = e.getY();
                }
            }
        });
    }

    public void setCommand(String command) {
        this.command = command;
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
        int centerX = min / 2;
        int centerY = min / 2;
        int radius = min / 2 - 6;

        // Draw knob body with blue color
        g2d.setColor(KNOB_COLOR);
        g2d.fillOval(0, 0, min - 1, min - 1);

        // Add a subtle gradient for 3D effect
        GradientPaint gp = new GradientPaint(
                0, 0, KNOB_GRADIENT_START,
                0, h, KNOB_GRADIENT_END);
        g2d.setPaint(gp);
        g2d.fillOval(2, 2, min - 4, min - 4);

        // Draw indicator line with thicker stroke
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2.0f));
        double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);

        g2d.drawLine(centerX, centerY,
                centerX + (int) (Math.cos(angle) * radius),
                centerY + (int) (Math.sin(angle) * radius));

        // Add highlight for 3D effect
        g2d.setColor(HIGHLIGHT_COLOR);
        g2d.fillOval(5, 3, min / 2 - 5, min / 2 - 5);

        g2d.dispose();
    }

    public void setValue(int newValue) {
        if (value != newValue) {
            value = Math.min(max, Math.max(min, newValue));
            repaint();
            fireStateChanged();
            if (command != null) {
                CommandBus.getInstance().publish(command, this, value);
            }
        }
    }

    public int getValue() {
        return value;
    }

    public int getMinimum() {
        return min;
    }

    public void setMinimum(int min) {
        this.min = min;
        repaint();
    }

    public int getMaximum() {
        return max;
    }

    public void setMaximum(int max) {
        this.max = max;
        repaint();
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    protected void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(event);
        }
    }
}
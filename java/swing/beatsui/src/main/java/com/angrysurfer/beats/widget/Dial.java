package com.angrysurfer.beats.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.angrysurfer.core.api.CommandBus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dial extends JComponent {

    private static final int MIN_SIZE = 40;
    private static final int MAX_SIZE = 60;
    private static final Color KNOB_COLOR = new Color(30, 100, 255);
    private static final Color KNOB_GRADIENT_START = new Color(60, 130, 255);
    private static final Color KNOB_GRADIENT_END = new Color(20, 80, 200);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 60);
    private static final int BASE_WINDOW_WIDTH = 1200;
    private static final int BASE_WINDOW_HEIGHT = 800;
    private int value = 64;
    private boolean isDragging = false;
    private int lastY;
    private int minimum = 0;
    private int maximum = 127;
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
                    int newValue = Math.min(maximum, Math.max(minimum, value + delta));
                    setValue(newValue, true);
                    lastY = e.getY();
                }
            }
        });

        // Add mouse wheel support
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                if (!isEnabled())
                    return;
                    
                // Get the wheel rotation (negative for up, positive for down)
                int wheelRotation = e.getWheelRotation();
                
                // Calculate value change - make it proportional to the value range
                // A smaller range means finer control
                int range = maximum - minimum;
                int changeAmount = Math.max(1, range / 100);
                
                // Invert the direction: scroll up to increase, down to decrease
                int delta = -wheelRotation * changeAmount;
                
                // Calculate new value with bounds checking
                int newValue = Math.min(maximum, Math.max(minimum, value + delta));
                
                // Update the value
                setValue(newValue, true);
            }
        });
    }

    public Dial(String command) {
        this();
        setCommand(command);
    }

    private boolean updateOnResize = false;

    private void updateSize() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            // Calculate size based on window dimensions
            double widthRatio = (double) window.getWidth() / BASE_WINDOW_WIDTH;
            double heightRatio = (double) window.getHeight() / BASE_WINDOW_HEIGHT;
            double scaleFactor = updateOnResize ? 1
                    : Math.min(Math.max(widthRatio, heightRatio), MAX_SIZE / (double) MIN_SIZE);

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

        // Calculate normalized value between 0 and 1 based on min/max range
        double normalizedValue = (value - minimum) / (double) (maximum - minimum);

        // Draw indicator line with thicker stroke
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2.0f));

        // Use normalized value for angle calculation
        // Start at 7:30 (0.75π) and rotate 1.5π radians (270 degrees)
        double angle = Math.PI * 0.75 + (Math.PI * 1.5 * normalizedValue);

        g2d.drawLine(centerX, centerY,
                centerX + (int) (Math.cos(angle) * radius),
                centerY + (int) (Math.sin(angle) * radius));

        // Add highlight for 3D effect
        g2d.setColor(HIGHLIGHT_COLOR);
        g2d.fillOval(5, 3, min / 2 - 5, min / 2 - 5);

        g2d.dispose();
    }

    public void setValue(int newValue, boolean notify) {
        if (value != newValue) {
            value = Math.min(maximum, Math.max(minimum, newValue));
            repaint();
            if (notify)
                fireStateChanged();
            if (command != null && notify) {
                CommandBus.getInstance().publish(command, this, value);
            }
        }
    }

    public void setMinimum(int min) {
        this.minimum = min;
        repaint();
    }

    public void setMaximum(int max) {
        this.maximum = max;
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
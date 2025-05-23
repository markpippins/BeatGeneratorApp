package com.angrysurfer.beats.widget;

import com.angrysurfer.core.api.CommandBus;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DoubleDial extends JComponent {

    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 60);
    private static final int BASE_WINDOW_WIDTH = 1200;
    private static final int BASE_WINDOW_HEIGHT = 800;
    private double value = 64;
    private boolean isDragging = false;
    private int lastY;
    private double minimum = 0;
    private double maximum = 127;
    private String command;
    private String label;
    private List<ChangeListener> changeListeners = new ArrayList<>();

    private int minSize = 50;
    private int maxSize = 70;

    private double stepSize = 1.0;

    private Color gradientStartColor = new Color(60, 130, 255);
    private Color gradientEndColor = new Color(20, 80, 200);
    private Color knobColor = new Color(30, 100, 255);
    private boolean updateOnResize = false;

    public DoubleDial() {
        this.command = null;
        updateSize();

        // Register for ancestor window resize events
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                Window window = SwingUtilities.getWindowAncestor(DoubleDial.this);
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

                    // Calculate how many steps this drag represents
                    double range = maximum - minimum;
                    double pixelsPerStep = 100.0 / range;
                    double steps = delta / pixelsPerStep;

                    // Apply the stepSize
                    double valueChange = steps * stepSize;

                    // Round to nearest step to avoid floating point errors
                    double newValue = value + valueChange;
                    newValue = minimum + Math.round((newValue - minimum) / stepSize) * stepSize;

                    // Ensure within bounds
                    newValue = Math.min(maximum, Math.max(minimum, newValue));

                    setValue(newValue, true);
                    lastY = e.getY();
                }
            }
        });

        // Add mouse wheel support
        addMouseWheelListener(e -> {
            if (!isEnabled()) {
                return;
            }

            // Get the wheel rotation (negative for up, positive for down)
            int wheelRotation = e.getWheelRotation();

            // Use stepSize for wheel changes
            double delta = -wheelRotation * stepSize;

            // Calculate new value with bounds checking
            double newValue = value + delta;

            // Round to nearest step to avoid floating point errors
            newValue = minimum + Math.round((newValue - minimum) / stepSize) * stepSize;

            // Apply bounds
            newValue = Math.min(maximum, Math.max(minimum, newValue));

            // Update the value
            setValue(newValue, true);
        });

    }

    public DoubleDial(String command) {
        this();
        setCommand(command);
    }

    public void setKnobColor(Color color) {
        this.knobColor = color;
        this.gradientStartColor = color.brighter();
        this.gradientEndColor = color.darker();
    }

    protected void updateSize() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            // Calculate size based on window dimensions
            double widthRatio = (double) window.getWidth() / BASE_WINDOW_WIDTH;
            double heightRatio = (double) window.getHeight() / BASE_WINDOW_HEIGHT;
            double scaleFactor = updateOnResize ? 1
                    : Math.min(Math.max(widthRatio, heightRatio), getMaxSize() / (double) getMinSize());

            int size = Math.min(getMaxSize(), (int) (getMinSize() * scaleFactor));

            Dimension newSize = new Dimension(size, size);
            setPreferredSize(newSize);
            setMinimumSize(newSize);
            revalidate();
        } else {
            // Default size when no window is available
            setPreferredSize(new Dimension(getMinSize(), getMinSize()));
            setMinimumSize(new Dimension(getMinSize(), getMinSize()));
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
        g2d.setColor(knobColor);
        g2d.fillOval(0, 0, min - 1, min - 1);

        // Add a subtle gradient for 3D effect
        GradientPaint gp = new GradientPaint(
                0, 0, gradientStartColor,
                0, h, gradientEndColor);
        g2d.setPaint(gp);
        g2d.fillOval(2, 2, min - 4, min - 4);

        // Calculate normalized value between 0 and 1 based on min/max range
        double normalizedValue = (value - minimum) / (maximum - minimum);

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

    public void setValue(double newValue, boolean notify) {
        if (value != newValue) {
            value = Math.min(maximum, Math.max(minimum, newValue));
            repaint();
            if (notify) {
                fireStateChanged();
            }
            if (command != null && notify) {
                CommandBus.getInstance().publish(command, this, value);
            }
        }
    }

    public void setMinimum(double min) {
        this.minimum = min;
        repaint();
    }

    public void setMaximum(double max) {
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

    public void setLabel(String label) {
        this.label = label;
        repaint();
    }

    public void setStepSize(double size) {
        this.stepSize = size;
        repaint();
    }
}

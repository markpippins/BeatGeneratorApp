package com.angrysurfer.beats.widget;

import com.angrysurfer.beats.util.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AccentButton extends JToggleButton {
    private static final Dimension BUTTON_SIZE = new Dimension(20, 20);
    private static final Color defaultColor = UIHelper.coolBlue;
    private static final Color selectedColor = Color.GREEN;
    private Color baseColor;
    private Color activeColor;
    private boolean highlighted = false;
    private boolean toggleable = false;
    private Color highlightColor = UIHelper.fadedOrange; // Default highlight color

    public AccentButton(String text) {
        super(text);
        setup();
    }

    private void setup() {
        baseColor = UIHelper.deepOrange;
        activeColor = Color.GREEN;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);

        // Set fixed size
        setMaximumSize(BUTTON_SIZE);
        setPreferredSize(BUTTON_SIZE);
        setMinimumSize(BUTTON_SIZE);

        // Only try to remove listeners if they exist
        ActionListener[] listeners = getActionListeners();
        if (listeners != null && listeners.length > 0) {
            removeActionListener(listeners[0]);
        }
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
        setEnabled(toggleable); // Only enable the button if it's toggleable
    }

    public boolean isActive() {
        return isSelected();
    }

    /**
     * Get whether this step is currently highlighted
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Set highlighted state (current playback position)
     */
    public void setHighlighted(boolean highlighted) {
        if (this.highlighted != highlighted) {
            this.highlighted = highlighted;
            repaint(); // Force visual update
        }
    }

    /**
     * Set custom highlight color
     */
    public void setHighlightColor(Color color) {
        this.highlightColor = color;
    }

    /**
     * Override paint component to properly show both selected and highlighted states
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Call parent for basic rendering
        super.paintComponent(g);

        // Add custom painting based on state
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Use highlight color if step is active
        if (isHighlighted()) {
            g2d.setColor(highlightColor);
        } else if (isSelected()) {
            g2d.setColor(selectedColor);
        } else {
            g2d.setColor(defaultColor);
        }

        // Draw main button body
        g2d.setPaint(new GradientPaint(
                0, 0,
                g2d.getColor().brighter(),
                0, height,
                g2d.getColor().darker()
        ));
        g2d.fillRoundRect(0, 0, width, height, 10, 10);

        // Draw highlight for 3D effect
        g2d.setPaint(new GradientPaint(
                0, 0,
                new Color(255, 255, 255, 100),
                0, height / 2,
                new Color(255, 255, 255, 0)
        ));
        g2d.fillRoundRect(0, 0, width, height, 10, 10);

        // Draw border
        g2d.setColor(isSelected() ? activeColor.darker() : baseColor.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);

        // Draw text with shadow for depth
        String text = getText();
        FontMetrics metrics = g2d.getFontMetrics(getFont());
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        // Draw text shadow
        g2d.setColor(isSelected() ? activeColor.darker().darker() : baseColor.darker().darker());
        g2d.drawString(text, x + 1, y + 1);

        // Draw main text
        g2d.setColor(isSelected() ? baseColor : activeColor);
        g2d.drawString(text, x, y);

        // Add pressed effect when active
        if (toggleable && isSelected()) {
            g2d.setPaint(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(0, 0, width, height, 10, 10);
        }

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return BUTTON_SIZE;
    }
}

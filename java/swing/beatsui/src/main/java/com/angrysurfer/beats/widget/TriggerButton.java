package com.angrysurfer.beats.widget;

import java.awt.*;
import javax.swing.JToggleButton;
import java.awt.event.ActionListener;

public class TriggerButton extends JToggleButton {
    private Color baseColor;
    private Color activeColor;
    private boolean highlighted = false;
    private boolean toggleable = false;
    private static final Dimension BUTTON_SIZE = new Dimension(30, 20);

    public TriggerButton(String text) {
        super(text);
        setup();
    }

    private void setup() {
        baseColor = ColorUtils.deepOrange;
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

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
        setEnabled(toggleable); // Only enable the button if it's toggleable
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public boolean isActive() {
        return isSelected();
    }

    public void setHighlighted(boolean highlighted) {
        if (this.highlighted != highlighted) {
            this.highlighted = highlighted;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        
        // Use highlight color if step is active
        Color currentColor = highlighted ? Color.WHITE : 
                           isSelected() ? activeColor : baseColor;

        // Draw main button body
        g2d.setPaint(new GradientPaint(
            0, 0, 
            currentColor.brighter(), 
            0, height, 
            currentColor.darker()
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

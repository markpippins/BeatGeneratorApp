package com.angrysurfer.beats.ui.widget;

import java.awt.*;
import javax.swing.JButton;

import com.angrysurfer.beats.ui.Utils;

public class TriggerButton extends JButton {
    private Color baseColor;
    private Color activeColor;
    private boolean isActive = false;
    private static final Dimension BUTTON_SIZE = new Dimension(30, 20);

    public TriggerButton(String text) {
        super(text);
        setup();
    }

    private void setup() {
        baseColor = Utils.deepOrange;
        activeColor = Color.GREEN;
        
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        
        // Set fixed size
        setMaximumSize(BUTTON_SIZE);
        setPreferredSize(BUTTON_SIZE);
        setMinimumSize(BUTTON_SIZE);

        addActionListener(e -> toggle());
    }

    public void toggle() {
        isActive = !isActive;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background with current color
        g2d.setColor(isActive ? activeColor : baseColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // Draw text in contrasting color
        g2d.setColor(isActive ? baseColor : activeColor);
        FontMetrics metrics = g2d.getFontMetrics(getFont());
        int x = (getWidth() - metrics.stringWidth(getText())) / 2;
        int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
        g2d.drawString(getText(), x, y);

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return BUTTON_SIZE;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            repaint();
        }
    }
}

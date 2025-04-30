package com.angrysurfer.beats.widget;

import javax.swing.*;

import java.awt.*;

import lombok.Getter;
import lombok.Setter;

/**
 * A simple LED indicator that can be turned on/off and display a label
 */
@Getter
@Setter
public class LEDIndicator extends JPanel {
    private boolean isOn = false;
    private Color onColor = new Color(0, 255, 0); // Default to green
    private Color offColor = new Color(50, 50, 50);
    private final String label;
    
    public LEDIndicator(Color onColor, String label) {
        this.onColor = onColor;
        this.label = label;
        // setPreferredSize(new Dimension(30, 16));
        setOpaque(false);
    }
    
    public void setOn(boolean on) {
        this.isOn = on;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw LED
        int ledSize = 8;
        int x = 2;
        int y = (getHeight() - ledSize) / 2;
        
        // Draw LED background/shadow
        g2d.setColor(new Color(20, 20, 20));
        g2d.fillOval(x+1, y+1, ledSize, ledSize);
        
        // Draw LED
        g2d.setColor(isOn ? onColor : offColor);
        g2d.fillOval(x, y, ledSize, ledSize);
        
        // Draw highlight to give 3D effect
        if (isOn) {
            g2d.setColor(new Color(255, 255, 255, 120));
            g2d.fillOval(x+2, y+2, ledSize-4, ledSize-4);
        }
        
        // Draw label
        g2d.setColor(isOn ? onColor.darker() : Color.GRAY);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + ledSize + 3;
        int textY = y + (ledSize + metrics.getAscent() - metrics.getDescent()) / 2;
        g2d.drawString(label, textX, textY);
        
        g2d.dispose();
    }
}

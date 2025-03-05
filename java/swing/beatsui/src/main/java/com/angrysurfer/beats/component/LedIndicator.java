package com.angrysurfer.beats.component;

import javax.swing.*;
import java.awt.*;

public class LedIndicator extends JComponent {
    private boolean lit = false;
    private final Color offColor = new Color(50, 50, 50);
    private final Color onColor;
    private static final int SIZE = 8;

    public LedIndicator(Color onColor) {
        this.onColor = onColor;
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
    }

    public void setLit(boolean lit) {
        this.lit = lit;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw LED
        g2.setColor(lit ? onColor : offColor);
        g2.fillOval(0, 0, SIZE-1, SIZE-1);
        
        // Draw highlight
        if (lit) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval(2, 2, SIZE/2, SIZE/2);
        }

        g2.dispose();
    }
}

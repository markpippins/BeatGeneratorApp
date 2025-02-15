package com.angrysurfer.beatsui.ui.widget;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.Timer;

import com.angrysurfer.beatsui.ui.Utils;

public class TriggerButton extends JButton {
    private Color baseColor;
    private Color activeColor;
    private boolean isTriggered = false;
    private Timer fadeTimer;
    private float alpha = 1.0f;

    public TriggerButton(String text) {
        super(text);
        setup();
    }

    private void setup() {
        baseColor = Utils.deepNavy;
        activeColor = Utils.warmOffWhite;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(40, 40));

        // Create fade timer
        fadeTimer = new Timer(50, e -> {
            alpha *= 0.8f;
            if (alpha < 0.1f) {
                alpha = 1.0f;
                isTriggered = false;
                fadeTimer.stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                trigger();
            }
        });
    }

    public void trigger() {
        isTriggered = true;
        alpha = 1.0f;
        fadeTimer.restart();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(baseColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // Draw trigger effect
        if (isTriggered) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(activeColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }

        // Draw text
        FontMetrics metrics = g2d.getFontMetrics(getFont());
        int x = (getWidth() - metrics.stringWidth(getText())) / 2;
        int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
        
        g2d.setColor(isTriggered ? baseColor : activeColor);
        g2d.drawString(getText(), x, y);

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int max = Math.max(size.width, size.height);
        return new Dimension(max, max);
    }
}

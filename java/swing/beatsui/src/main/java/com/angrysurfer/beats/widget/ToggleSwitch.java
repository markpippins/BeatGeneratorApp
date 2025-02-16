package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import com.angrysurfer.beats.Utils;

public class ToggleSwitch extends JComponent {
    private boolean selected = false;
    private final int width = 60;
    private final int height = 30;

    public ToggleSwitch() {
        setPreferredSize(new Dimension(width, height));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = !selected;
                repaint();
                firePropertyChange("selected", !selected, selected);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(selected ? Utils.deepNavy : Color.DARK_GRAY);
        g2d.fillRoundRect(0, 0, width, height, height, height);

        // Draw toggle circle
        g2d.setColor(Utils.warmOffWhite);
        int toggleX = selected ? width - height + 2 : 2;
        g2d.fillOval(toggleX, 2, height - 4, height - 4);

        g2d.dispose();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        boolean oldValue = this.selected;
        this.selected = selected;
        firePropertyChange("selected", oldValue, selected);
        repaint();
    }
}

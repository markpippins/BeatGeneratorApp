package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.angrysurfer.beats.util.UIHelper;

public class ToggleSwitch extends JComponent {
    private boolean selected = false;
    private final int width = 60;
    private final int height = 30;

    // Add ActionListener support
    private final List<ActionListener> actionListeners = new ArrayList<>();

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    protected void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "stateChanged");
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(event);
        }
    }

    public ToggleSwitch() {
        setPreferredSize(new Dimension(width, height));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean wasSelected = selected;
                selected = !selected;
                repaint();
                firePropertyChange("selected", !selected, selected);
                if (isSelected() != wasSelected) {
                    fireActionPerformed();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(selected ? UIHelper.deepNavy : Color.DARK_GRAY);
        g2d.fillRoundRect(0, 0, width, height, height, height);

        // Draw toggle circle
        g2d.setColor(UIHelper.warmOffWhite);
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

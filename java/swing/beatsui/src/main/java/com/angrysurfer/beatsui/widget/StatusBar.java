package com.angrysurfer.beatsui.widget;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

import com.angrysurfer.beatsui.api.StatusConsumer;

public class StatusBar extends JPanel implements StatusConsumer {
    private final JLabel label;

    public StatusBar() {
        super(new BorderLayout());
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        label = new JLabel(" ");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label, BorderLayout.CENTER);
    }

    @Override
    public void setStatus(String text) {
        label.setText(text);
        repaint();
    }

    @Override
    public void clearStatus() {
        label.setText(" ");
    }
}

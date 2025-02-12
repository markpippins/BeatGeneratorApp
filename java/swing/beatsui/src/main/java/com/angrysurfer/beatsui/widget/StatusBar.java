package com.angrysurfer.beatsui.widget;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.angrysurfer.beatsui.api.StatusConsumer;

public class StatusBar extends JPanel implements StatusConsumer {
    private final JLabel label;

    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 6,8 , 6));
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

package com.angrysurfer.beatsui.widget;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatusBar extends JPanel implements IStatus {

    private JLabel statusLabel;

    public StatusBar() {
        super(new BorderLayout());
        setup();
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.WEST);
    }

    @Override
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    @Override
    public void clearStatus() {
        statusLabel.setText("");
    }

}

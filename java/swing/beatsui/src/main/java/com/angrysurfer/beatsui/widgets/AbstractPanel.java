package com.angrysurfer.beatsui.widgets;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public abstract class AbstractPanel extends JPanel {

    public AbstractPanel() {
        super(new BorderLayout());
        add(createContent(), BorderLayout.CENTER);
    }

    public abstract JPanel createContent();
}

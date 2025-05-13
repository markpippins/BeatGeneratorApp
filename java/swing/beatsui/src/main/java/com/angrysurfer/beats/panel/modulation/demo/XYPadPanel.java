package com.angrysurfer.beats.panel.modulation.demo;

import javax.swing.*;
import java.awt.*;

public class XYPadPanel extends JPanel {
    public XYPadState[] states = new XYPadState[4];

    public XYPadPanel() {
        setLayout(new GridLayout(2, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < 4; i++) {
            states[i] = new XYPadState(0.5f, 0.5f); // start centered
            add(new XYPad(states[i]));
        }
    }

    public XYPadState getPadState(int index) {
        return states[index];
    }
}


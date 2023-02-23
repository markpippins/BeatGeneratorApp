package com.angrysurfer.midi.swing;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

public class FaderPanel extends JPanel {


    public FaderPanel() {
        super();
        add(new JLabel("Fader Panel"), BorderLayout.NORTH);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JPanel faderPad = new JPanel(new GridLayout(4, 16));
        for (int i = 0; i < 64; i++) {
            JSlider slider = new JSlider();
            slider.setToolTipText(Integer.toString(i));
            faderPad.add(slider);
            add(faderPad, BorderLayout.CENTER);
            setMaximumSize(new Dimension(800, 400));
        }
    }
}

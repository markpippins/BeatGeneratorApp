package com.angrysurfer.midi.swing;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.DimensionUIResource;
import java.awt.*;

public class SliderPanel extends JPanel {


    public SliderPanel() {
        super();
        add(new JLabel("Fader Panel"), BorderLayout.NORTH);
        setupUI();
    }

    private void setupUI() {
        setMaximumSize(new DimensionUIResource(400, 200));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JPanel sliderPad = new JPanel(new GridLayout(4, 16));
        for (int i = 0; i < 32; i++) {
            JSlider slider = new JSlider();
            slider.setOrientation(JSlider.VERTICAL);
            slider.setToolTipText(Integer.toString(i));
            slider.setMaximumSize(new Dimension(50, 50));
            sliderPad.add(slider);
            add(sliderPad, BorderLayout.CENTER);
        }
    }
}

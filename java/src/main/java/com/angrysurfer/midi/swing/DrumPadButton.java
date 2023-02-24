package com.angrysurfer.midi.swing;

import javax.swing.*;
import java.awt.*;

public class DrumPadButton extends JButton {
    static Dimension MINIMUM_SIZE = new Dimension(50, 50);

    public DrumPadButton() {
    }
    public DrumPadButton(String text) {
        super(text, null);
    }

    @Override
    public Dimension getMinimumSize() {
        return MINIMUM_SIZE;
    }

    @Override
    public Dimension getMaximumSize() {
        return MINIMUM_SIZE;
    }
}

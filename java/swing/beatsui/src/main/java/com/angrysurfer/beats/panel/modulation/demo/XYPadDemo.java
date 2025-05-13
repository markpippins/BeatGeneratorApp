package com.angrysurfer.beats.panel.modulation.demo;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class XYPadDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("4 XY Pads");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new XYPadPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}


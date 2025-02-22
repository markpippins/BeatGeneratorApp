package com.angrysurfer.beatsui.ui.visualization.handler;

import java.awt.Color;

import com.angrysurfer.beatsui.ui.visualization.IVisualizationHandler;
import com.angrysurfer.beatsui.ui.widget.GridButton;

public class RippleVisualization implements IVisualizationHandler {
    private double t = 0.0;

    @Override
    public void update(GridButton[][] buttons) {
        double cx = buttons[0].length / 2.0;
        double cy = buttons.length / 2.0;
        
        for (int y = 0; y < buttons.length; y++) {
            for (int x = 0; x < buttons[0].length; x++) {
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double val = Math.sin(dist - t);
                int rgb = (int) ((val + 1) * 127);
                buttons[y][x].setBackground(new Color(0, rgb, rgb));
            }
        }
        t += 0.1;
    }

    @Override
    public String getName() {
        return "Ripple";
    }
}

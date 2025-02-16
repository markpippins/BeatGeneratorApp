package com.angrysurfer.beats.ui.state;

import java.awt.Dimension;
import java.awt.Point;
import lombok.Data;

@Data
public class FrameState {
    private Point location = new Point(100, 100);
    private Dimension size = new Dimension(800, 600);
    private boolean maximized = false;
    private int dividerLocation = 200;
    
    // Default constructor uses default values
    public FrameState() {}
    
    // Constructor with all fields
    public FrameState(Point location, Dimension size, boolean maximized, int dividerLocation) {
        this.location = location;
        this.size = size;
        this.maximized = maximized;
        this.dividerLocation = dividerLocation;
    }
}

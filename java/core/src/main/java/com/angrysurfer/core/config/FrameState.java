package com.angrysurfer.core.config;

import java.awt.Dimension;
import java.awt.Point;
import lombok.Data;

@Data
public class FrameState {
    private Point location = new Point(100, 100);
    private Dimension size = new Dimension(800, 600);
    private boolean maximized = false;
    private int dividerLocation = 200;
    private int selectedTab = 0;
    private int frameSizeX = 800;
    private int frameSizeY = 600;
    private int framePosX = 100;
    private int framePosY = 100;
    private String lookAndFeelClassName;
    
    // Default constructor uses default values
    public FrameState() {}
    
    // Constructor with window state
    public FrameState(Point location, Dimension size, boolean maximized, int dividerLocation) {
        this.location = location;
        this.size = size;
        this.maximized = maximized;
        this.dividerLocation = dividerLocation;
    }

    // Constructor with individual components
    public FrameState(int selectedTab, int sizeX, int sizeY, int posX, int posY, String lookAndFeel) {
        this.selectedTab = selectedTab;
        this.frameSizeX = sizeX;
        this.frameSizeY = sizeY;
        this.framePosX = posX;
        this.framePosY = posY;
        this.lookAndFeelClassName = lookAndFeel;
    }
}

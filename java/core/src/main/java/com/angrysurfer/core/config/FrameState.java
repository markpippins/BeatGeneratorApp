package com.angrysurfer.core.config;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class FrameState {
    private Point location = new Point(100, 100);
    private Dimension size = new Dimension(800, 600);
    private boolean maximized = false;
    private boolean minimized = false;
    private int dividerLocation = 200;
    private int selectedTab = 0;
    private int frameSizeX = 800;
    private int frameSizeY = 600;
    private int framePosX = 100;
    private int framePosY = 100;
    private String lookAndFeelClassName;
    private List<String> columnOrder;

    // Default constructor uses default values
    public FrameState() {
        this.columnOrder = new ArrayList<>();
    }

    // Constructor with window state
    public FrameState(Point location, Dimension size, boolean maximized, boolean minimized, int dividerLocation) {
        this.location = location;
        this.size = size;
        this.maximized = maximized;
        this.minimized = minimized;
        this.dividerLocation = dividerLocation;
        this.columnOrder = new ArrayList<>();
    }

    // Constructor with individual components
    public FrameState(int selectedTab, int sizeX, int sizeY, int posX, int posY, String lookAndFeel) {
        this.selectedTab = selectedTab;
        this.frameSizeX = sizeX;
        this.frameSizeY = sizeY;
        this.framePosX = posX;
        this.framePosY = posY;
        this.lookAndFeelClassName = lookAndFeel;
        this.columnOrder = new ArrayList<>();
    }

    // Add helper method
    public void setPlayerColumnOrderFromSet(Set<String> columns) {
        if (columns != null) {
            this.columnOrder = new ArrayList<>(columns);
        } else {
            this.columnOrder = new ArrayList<>();
        }
    }

    public Set<String> getPlayerColumnOrderAsSet() {
        if (columnOrder == null) {
            columnOrder = new ArrayList<>();
        }
        return new LinkedHashSet<>(columnOrder);
    }

    public void setColumnOrder(List<String> columnOrder) {
        if (columnOrder == null) {
            this.columnOrder = new ArrayList<>();
        } else {
            this.columnOrder = new ArrayList<>(columnOrder);
        }
    }

    public List<String> getColumnOrder() {
        if (columnOrder == null) {
            columnOrder = new ArrayList<>();
        }
        return new ArrayList<>(columnOrder); // Return a copy to prevent modification
    }
}

package com.angrysurfer.beatsui.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FrameState {
    public int selectedTab;
    public int frameSizeX;
    public int frameSizeY;
    public int framePosX;
    public int framePosY;

    private String lookAndFeelClassName;

    public FrameState() {
        this(0, 1200, 800, 0, 0);
    }

    public FrameState(int selectedTab, int frameSizeX, int frameSizeY, int framePosX, int framePosY) {
        this.selectedTab = selectedTab;
        this.frameSizeX = frameSizeX;
        this.frameSizeY = frameSizeY;
        this.framePosX = framePosX;
        this.framePosY = framePosY;
        this.lookAndFeelClassName = com.formdev.flatlaf.FlatLightLaf.class.getName(); // default LAF
    }

    public FrameState(int selectedTab, int frameSizeX, int frameSizeY, int framePosX, int framePosY, String lookAndFeelClassName) {
        this(selectedTab, frameSizeX, frameSizeY, framePosX, framePosY);
        this.lookAndFeelClassName = lookAndFeelClassName;
    }
}
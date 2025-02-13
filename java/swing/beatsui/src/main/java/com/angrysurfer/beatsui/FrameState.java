package com.angrysurfer.beatsui;

public class FrameState {
    public int selectedTab;
    public int frameSizeX;
    public int frameSizeY;
    public int framePosX;
    public int framePosY;

    public FrameState() {
        this(0, 1200, 800, 0, 0);
    }

    public FrameState(int selectedTab, int frameSizeX, int frameSizeY, int framePosX, int framePosY) {
        this.selectedTab = selectedTab;
        this.frameSizeX = frameSizeX;
        this.frameSizeY = frameSizeY;
        this.framePosX = framePosX;
        this.framePosY = framePosY;
    }
}
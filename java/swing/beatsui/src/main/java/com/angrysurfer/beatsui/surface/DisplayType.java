package com.angrysurfer.beatsui.surface;

public enum DisplayType {
    VISUALIZER("Visualizer"),
    CONTROL("Control");

    private final String label;

    DisplayType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
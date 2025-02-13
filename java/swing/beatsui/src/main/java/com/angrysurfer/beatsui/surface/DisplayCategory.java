package com.angrysurfer.beatsui.surface;

public enum DisplayCategory {
    DEFAULT("Default"),
    CONTROL("Control");

    private final String label;

    DisplayCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
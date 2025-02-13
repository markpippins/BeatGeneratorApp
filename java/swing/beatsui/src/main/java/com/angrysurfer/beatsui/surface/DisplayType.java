package com.angrysurfer.beatsui.surface;

public enum DisplayType {
    SIZZLE("Sizzle"),
    CONTROL("Control");

    private final String label;

    DisplayType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
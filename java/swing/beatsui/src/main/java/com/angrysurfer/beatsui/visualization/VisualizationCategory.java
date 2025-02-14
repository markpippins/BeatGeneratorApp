package com.angrysurfer.beatsui.visualization;

public enum VisualizationCategory {
    DEFAULT("Default"),
    GAME("Games"),
    MUSIC("Music");

    private final String label;

    VisualizationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
package com.angrysurfer.beats.visualization;

public enum VisualizationCategory {
    DEFAULT("Default"),
    COMPSCI("Computer Science"),
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
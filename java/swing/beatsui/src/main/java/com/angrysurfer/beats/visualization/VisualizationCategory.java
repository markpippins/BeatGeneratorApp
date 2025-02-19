package com.angrysurfer.beats.visualization;

public enum VisualizationCategory {
    DEFAULT("Default"),
    ARCADE("Arcade Games"),
    CLASSIC("Classic Visualizations"),
    COMPSCI("Computer Science"),
    GAME("Games"),
    MATH("Math"),
    MATRIX("Matrix"),
    MUSIC("Music"),
    SCIENCE("Science");

    private final String label;

    VisualizationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
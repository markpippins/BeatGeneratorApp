package com.angrysurfer.core.model;

// Helper class to represent presets in the combo box
public class PresetItem {

    private final int number;
    private final String name;

    public PresetItem(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " - " + number;
    }
}
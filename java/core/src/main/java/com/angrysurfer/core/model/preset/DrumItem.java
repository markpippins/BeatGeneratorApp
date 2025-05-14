package com.angrysurfer.core.model.preset;

// Add a helper class for drum items
public class DrumItem {

    private final int noteNumber;
    private final String name;

    public DrumItem(int noteNumber, String name) {
        this.noteNumber = noteNumber;
        this.name = name;
    }

    public int getNoteNumber() {
        return noteNumber;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
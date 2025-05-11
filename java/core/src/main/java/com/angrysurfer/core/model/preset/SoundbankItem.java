package com.angrysurfer.core.model.preset;

/**
 * Inner class for Soundbank ComboBox items
 */
public class SoundbankItem {
    private final String name;

    public SoundbankItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

package com.angrysurfer.core.model.preset;

/**
 * Inner class for Bank ComboBox items
 */
public class BankItem {
    private final int index;
    private final String name;

    public BankItem(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (" + index + ")";
    }
}

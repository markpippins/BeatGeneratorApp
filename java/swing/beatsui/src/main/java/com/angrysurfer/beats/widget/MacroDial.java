package com.angrysurfer.beats.widget;

import java.util.HashMap;

public class MacroDial extends Dial {

    private HashMap<Dial, Integer> children = new HashMap<Dial, Integer>();

    public MacroDial() {
        super();
    }

    public void addChild(Dial dial, int weight) {
        children.put(dial, weight);
    }

    public void removeChild(Dial dial) {
        children.remove(dial);
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        for (Dial dial : children.keySet())
            dial.setValue(value * children.get(dial));
    }
}

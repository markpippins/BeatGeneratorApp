package com.angrysurfer.beatsui.grid;

import com.angrysurfer.beatsui.widget.GridButton;

public interface Visualization {

    void update(GridButton[][] buttons);

    String getName();

}
package com.angrysurfer.beatsui.surface;

import com.angrysurfer.beatsui.widget.GridButton;

public interface Visualization {

    void update(GridButton[][] buttons);

    String getName();

}
package com.angrysurfer.beatsui.surface;

import com.angrysurfer.beatsui.widget.GridButton;

public interface VisualizationHandler {

    void update(GridButton[][] buttons);

    String getName();

    default DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    default VisualizationEnum getEnum() {
        return VisualizationEnum.fromLabel(getName());
    }
}
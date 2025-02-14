package com.angrysurfer.beatsui.visualization;

import com.angrysurfer.beatsui.widget.GridButton;

public interface VisualizationHandler {

    void update(GridButton[][] buttons);

    String getName();

    default DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    default VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }

    default VisualizationEnum getEnum() {
        return VisualizationEnum.fromLabel(getName());
    }
}
package com.angrysurfer.beats.visualization;

import com.angrysurfer.beats.widget.GridButton;

public interface IVisualizationHandler {

    void update(GridButton[][] buttons);

    String getName();

    default DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    default VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }

    // default VisualizationEnum getEnum() {
    //     return VisualizationEnum.fromLabel(getName());
    // }
}
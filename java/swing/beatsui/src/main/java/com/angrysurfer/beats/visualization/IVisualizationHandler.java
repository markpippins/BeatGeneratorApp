package com.angrysurfer.beats.visualization;

import com.angrysurfer.beats.widget.GridButton;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

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
    // return VisualizationEnum.fromLabel(getName());
    // }

    default void lockDisplay() {
        CommandBus.getInstance().publish(Commands.LOCK_CURRENT_VISUALIZATION);
    }

    default void unlockDisplay() {
        CommandBus.getInstance().publish(Commands.UNLOCK_CURRENT_VISUALIZATION);
    }
}
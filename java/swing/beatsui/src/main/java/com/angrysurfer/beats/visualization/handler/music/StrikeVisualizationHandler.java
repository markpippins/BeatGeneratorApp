package com.angrysurfer.beats.visualization.handler.music;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.widget.GridButton;

public class StrikeVisualizationHandler extends LockHandler implements IVisualizationHandler {

    @Override
    public String getName() {
        return "Strike Details";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }

    @Override
    public void update(GridButton[][] buttons) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

}

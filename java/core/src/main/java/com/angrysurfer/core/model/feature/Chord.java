package com.angrysurfer.core.model.feature;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Chord extends Player {

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

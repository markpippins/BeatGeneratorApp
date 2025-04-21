package com.angrysurfer.core.model.feature;

import java.util.ArrayList;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sample extends Player {

    private boolean started = false;

    public Sample() {
        super();
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onTick'");
    }

}

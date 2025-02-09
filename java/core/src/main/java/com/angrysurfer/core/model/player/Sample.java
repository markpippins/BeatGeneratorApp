package com.angrysurfer.core.model.player;

import java.util.ArrayList;

import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sample extends Strike {

    private boolean started = false;

    public Sample(String name, Ticker ticker, Instrument instrument, int low, int high) {
        super(name, ticker, instrument, low, new ArrayList<>(instrument.getAssignments().keySet()));
        // setHighNote(high);
    }

    @Override
    public void onTick(long tick, long bar) {

    }
}

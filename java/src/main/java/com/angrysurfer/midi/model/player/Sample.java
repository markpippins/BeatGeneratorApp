package com.angrysurfer.midi.model.player;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.model.midi.MidiInstrument;

@Getter
@Setter
public class Sample extends Strike {

    private boolean started = false;

    public Sample(String name, Ticker ticker, MidiInstrument instrument, int low, int high) {
        super(name, ticker, instrument, low, new ArrayList<>(instrument.getAssignments().keySet()));
        // setHighNote(high);
    }

    @Override
    public void onTick(long tick, long bar) {

    }
}

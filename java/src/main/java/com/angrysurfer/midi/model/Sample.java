package com.angrysurfer.midi.model;

import com.angrysurfer.midi.service.MidiInstrument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sample extends Strike {

    private int highNote;

    private boolean started = false;

    public Sample(String name, Ticker ticker, MidiInstrument instrument, int low, int high) {
        super(name, ticker, instrument, low, instrument.getAssignments().keySet().stream().toList());
        setHighNote(high);
    }

    @Override
    public void onTick(long tick, int bar) {

    }
}

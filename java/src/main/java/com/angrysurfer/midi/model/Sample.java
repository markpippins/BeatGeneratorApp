package com.angrysurfer.midi.model;

import com.angrysurfer.midi.service.MidiInstrument;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
public class Sample extends Strike {

    private int highNote;

    private boolean started = false;

    public Sample(String name, Ticker ticker, MidiInstrument instrument, int low, int high) {
        super(name, ticker, instrument, low, new HashSet<>(instrument.getAssignments().keySet()));
        setHighNote(high);
    }

    @Override
    public void onTick(long tick, int bar) {

    }
}

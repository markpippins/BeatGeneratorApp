package com.angrysurfer.midi.util;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.angrysurfer.midi.model.MidiInstrument;

@Getter
@Setter
public class MidiInstrumentList implements Serializable {
    List<MidiInstrument> instruments = new ArrayList<>();
    public MidiInstrumentList() {
    }
}

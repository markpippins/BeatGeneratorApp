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

    // public MidiInstrumentList(List<Player> players) {
    //     Map<String, MidiInstrument> defs = new HashMap<>();
    //     players.forEach(player -> defs.put(player.getInstrumentName(), new MidiInstrument(player)));
    //     getInstruments().addAll(defs.values());
    // }
}

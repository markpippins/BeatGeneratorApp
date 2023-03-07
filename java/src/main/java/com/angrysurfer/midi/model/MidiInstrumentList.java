package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MidiInstrumentList implements Serializable {
    List<MidiInstrumentInfo> instruments = new ArrayList<>();
    public MidiInstrumentList() {
    }

    public MidiInstrumentList(List<Player> players) {
        Map<String, MidiInstrumentInfo> defs = new HashMap<>();
        players.forEach(player -> defs.put(player.getInstrumentName(), new MidiInstrumentInfo(player)));
        getInstruments().addAll(defs.values());
    }
}

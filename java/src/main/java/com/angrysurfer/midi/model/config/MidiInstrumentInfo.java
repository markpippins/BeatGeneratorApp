package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Player;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class MidiInstrumentInfo implements Serializable {
    
    public MidiInstrumentInfo() {
        
    }
    public MidiInstrumentInfo(Player player) {
        setName(player.getInstrumentName());
        setAssignments(player.getInstrument().getAssignments());
        setHighestPreset(player.getInstrument().getHighestPreset());
        setChannel(player.getChannel());
        setBoundaries(player.getInstrument().getBoundaries());
        setHighestNote(player.getInstrument().getHighestNote());
        setHighestPreset((player.getInstrument().getHighestPreset()));
        setLowestNote(player.getInstrument().getLowestNote());
        setPreferredPreset(player.getInstrument().getPreferredPreset());
        setDeviceName(player.getInstrument().getDevice().getDeviceInfo().getName());
    }
    private Map<Integer, String> assignments = new HashMap<>();
    private Map<Integer, Integer[]> boundaries = new HashMap<>();
    private String deviceName;
    private String name;
    private int channel;
    private int lowestNote;
    private int highestNote;
    private int highestPreset;
    private int preferredPreset;
}


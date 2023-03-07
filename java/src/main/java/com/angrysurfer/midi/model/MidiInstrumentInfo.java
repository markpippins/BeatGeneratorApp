package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.ControlCode;
import com.angrysurfer.midi.model.Player;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Entity
public class MidiInstrumentInfo implements Serializable {
    @Transient
    boolean hasAssignments;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    @Transient
    private Map<Integer, String> assignments = new HashMap<>();
    @Transient
    private Map<Integer, Integer[]> boundaries = new HashMap<>();
    private String deviceName;
    private String name;
    private int channel;
    private int lowestNote;
    private int highestNote;
    private int highestPreset;
    private int preferredPreset;
    private int pads;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "instrument_control_code", joinColumns = {@JoinColumn(name = "instrument_id")}, inverseJoinColumns = {
            @JoinColumn(name = "control_code_id")})
    private List<ControlCode> controlCodes = new ArrayList<>();


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
        setHasAssignments(player.getInstrument().getAssignments().size() > 0);
    }
}


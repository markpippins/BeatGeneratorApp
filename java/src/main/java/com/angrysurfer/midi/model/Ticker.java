package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public class Ticker implements Serializable {

    @Transient
    @JsonIgnore
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());

    @Transient
    @JsonIgnore
    private Set<Strike> addList = new HashSet<>();

    @Transient
    @JsonIgnore
    private Set<Strike> removeList = new HashSet<>();

    @Transient
    private int bar = 1;

    @Transient
    private Long tick = 1L;

    @Transient
    private boolean done = false;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Transient
    private Set<Strike> players = new HashSet<>();

    @Transient
    private double beat = 1;

    @Transient
    private double granularBeat = 1.0;

    private int beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private double beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private int partLength = Constants.DEFAULT_PART_LENGTH;
    private int maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private int songLength = Constants.DEFAULT_MAX_TRACKS;
    private int swing = Constants.DEFAULT_SWING;
    private int ticksPerBeat = Constants.DEFAULT_PPQ;
    private float tempoInBPM = Float.valueOf(120);
    
    @Transient
    private boolean playing = false;
    
    @Transient
    private boolean paused = false;

    @Transient
    @JsonIgnore
    private MuteGroupList muteGroups = new MuteGroupList();

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
    }

    public Strike getPlayer(Long playerId) {
        return getPlayers().stream().filter(p -> playerId == p.getId()).findFirst().orElseThrow();
    }

    public void reset() {
        setId(null);
        setTick(0L);
        setBar(1);
        setBeat(1);
        getPlayers().clear();
        setPaused(false);
        setDone(false);
        setSwing(Constants.DEFAULT_SWING);
        setMaxTracks(Constants.DEFAULT_MAX_TRACKS);
        setPartLength(Constants.DEFAULT_PART_LENGTH);
        setSongLength(Constants.DEFAULT_SONG_LENGTH);
        setTempoInBPM(Constants.DEFAULT_BPM);
        setBeatDivider(Constants.DEFAULT_BEAT_DIVIDER);
        setBeatsPerBar(Constants.DEFAULT_BEATS_PER_BAR);
        setGranularBeat(1.0);
    }

    public void onBarChange(int bar) {
    
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }
        
        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }    
    }

    private void clearMuteGroups() {
    }

    private void createMuteGroups() {
    }

    private void updateMuteGroups() {
    }

    public double getBeatDivision() {
        return 1.0 / beatDivider;
    }



}


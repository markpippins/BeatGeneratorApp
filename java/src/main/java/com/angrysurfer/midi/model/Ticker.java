package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.midi.util.Constants;
import com.angrysurfer.midi.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    // @Transient
    // private AtomicInteger atomicBar = new AtomicInteger(1);


    @Transient
    private Cycler beatCycler = new Cycler();

    @Transient
    private Cycler barCycler = new Cycler();

    @Transient
    private AtomicInteger atomicPart = new AtomicInteger(1);

    @Transient
    private AtomicLong atomicTick = new AtomicLong(1L);

    @Transient
    private boolean done = false;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @Transient
    private Set<Strike> players = new HashSet<>();

    @Transient 
    Set<Long> activePlayerIds = new HashSet<>();

    @Transient
    private double granularBeat = 1.0;

    private int beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private double beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private int partLength = Constants.DEFAULT_PART_LENGTH;
    private int maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private int songLength = Constants.DEFAULT_MAX_TRACKS;
    private int swing = Constants.DEFAULT_SWING;
    private int ticksPerBeat = Constants.DEFAULT_PPQ;
    private float tempoInBPM = Constants.DEFAULT_BPM;
    private int loopCount  = Constants.DEFAULT_LOOP_COUNT;
    private int parts = 4;

    @Transient
    private boolean playing = false;
    
    @Transient
    private boolean paused = false;

    @Transient
    @JsonIgnore
    private Set<MuteGroup> muteGroups = new HashSet<>();

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
    }

    public Strike getPlayer(Long playerId) {
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public void beforeTick() {
    }

    public void afterTick() {
        setTick(getTick() + 1);
        if (getTick() % getTicksPerBeat() == 0) {
            getBeatCycler().advance();
            if (getBeatCycler().get() % getBeatsPerBar() == 0) 
                onBarChange();
        }
    }

    public double getBeat() {
        return getBeatCycler().get();
    }

    public void setTick(long tick) {
        atomicTick.set(tick);
    }

    public Long getTick() {
        return atomicTick.get();
    }

    // public void setBar(int bar) {
    //     atomicBar.set(bar);
    // }

    public int getBar() {
        return getBarCycler().get();
    }

    private void setPart(int part) {
        atomicPart.set(part);
    }

    public int getPart() {
        return atomicPart.get();
    }

    public void reset() {
        setId(null);
        setTick(0L);
        getBeatCycler().reset();
        getBarCycler().reset();
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

    public void onBarChange() {

        getBarCycler().advance();

        if (getBar() % getPartLength() == 0)
            onPartChange();
    
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }
        
        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }    
    }

    public void onPartChange() {
        setPart(getPart() < getParts() ? getPart() + 1 : 1);    
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

    public void afterEnd() {
        setPlaying(false);
        setTick(1L);
        getBeatCycler().reset();
        getBarCycler().reset();
    }

    public void beforeStart() {
        getBeatCycler().setLength(getBeatsPerBar());
        getBarCycler().setLength(getPartLength() * getBeatsPerBar());

        setPlaying(true);
        setTick(1L);
    }

    public void onStop() {
        setPlaying(false);
        setTick(1L);
        getBarCycler().reset();
        getBeatCycler().reset();
    }


}


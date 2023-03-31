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
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

@Getter
@Setter
@Entity
public class Ticker implements Serializable {

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());

    @JsonIgnore
    @Transient
    private Set<Player> addList = new HashSet<>();

    @JsonIgnore
    @Transient
    private Set<Player> removeList = new HashSet<>();

    @JsonIgnore
    @Transient
    private Cycler beatCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler beatCounter = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler barCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler barCounter = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler partCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler partCounter = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler tickCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler tickCounter = new Cycler();

    @Transient
    private boolean done = false;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Transient
    private Set<Player> players = new HashSet<>();

    @Transient
    Set<Long> activePlayerIds = new HashSet<>();

    @Transient
    private double granularBeat = 0.0;

    private Integer bars = Constants.DEFAULT_BAR_COUNT;
    private Integer beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private Double beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private Integer partLength = Constants.DEFAULT_PART_LENGTH;
    private Integer maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private Integer songLength = Constants.DEFAULT_MAX_TRACKS;
    private Integer swing = Constants.DEFAULT_SWING;
    private Integer ticksPerBeat = Constants.DEFAULT_PPQ;
    private Float tempoInBPM = Constants.DEFAULT_BPM;
    private Integer loopCount = Constants.DEFAULT_LOOP_COUNT;
    private Integer parts = Constants.DEFAULT_PART_COUNT;

    @Transient
    private boolean paused = false;

    @Transient
    @JsonIgnore
    private Set<MuteGroup> muteGroups = new HashSet<>();

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
    }

    public Player getPlayer(Long playerId) {
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public double getBeat() {
        logger.info(String.format("beat: %s", getBeatCycler().get()));
        return getBeatCycler().get();
    }

    public Long getBeatCount() {
        logger.info(String.format("beat count: %s",getBeatCounter().get()));
        return getBeatCounter().get();
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        getBeatCycler().setLength(beatsPerBar);
    }

    public Long getTick() {
        return getTickCycler().get();
    }

    public Long getTickCount() {
        return getTickCounter().get();
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        this.ticksPerBeat = ticksPerBeat;
        getTickCycler().setLength(ticksPerBeat);
    }

    public Long getBar() {
        return getBarCycler().get();
    }

    public Long getBarCount() {
        return getBarCounter().get();
    }

    public void setBars(int bars) {
        this.bars = bars;
        getBarCycler().setLength(bars);
    }

    public Long getPart() {
        return getPartCycler().get();
    }

    public Long getPartCount() {
        return getPartCounter().get();
    }

    public void setParts(int parts) {
        this.parts = parts;
        this.partCycler.setLength(parts);
    }

    public void setPartLength(int partLength) {
        this.partLength = partLength;
    }

    public void reset() {
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();

        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();

        getAddList().clear();
        getRemoveList().forEach(r -> getPlayers().remove(r));
        getRemoveList().clear();
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
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();

        IntStream.range(0, 127).forEach(note -> {
            getPlayers().forEach(p -> {
                try {
                    p.getInstrument().noteOff(note, 0);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }
            });
        });
    }

    public void beforeStart() {
        getTickCycler().setLength(getTicksPerBeat());
        getBarCycler().setLength(getBars());
        getBeatCycler().setLength(getBeatsPerBar());
        getPartCycler().setLength(getPartLength());
        getPlayers().forEach(p -> p.getSkipCycler().setLength(p.getSkips()));
    }

    public void onStart() {
        getBeatCycler().getListeners().forEach(l -> l.starting());
        getBarCycler().getListeners().forEach(l -> l.starting());
    }

    public void onStop() {
        getBeatCycler().reset();
    }

    public void beforeTick() {
    }

    public void afterTick() {
        granularBeat += getTicksPerBeat() / getBeatsPerBar();
        
        if (getTick() % getTicksPerBeat() == 0)
            onBeatChange();

        getTickCycler().advance();
        getTickCounter().advance();
    }

    public void onBeatChange() {
        setGranularBeat(0.0);
        getBeatCycler().advance();
        getBeatCounter().advance();

        if (getBeat() % getBeatsPerBar() == 0)
            onBarChange();
    }

    public void onBarChange() {
        updatePlayerConfig();
        if (getBar() % getPartLength() == 0)
            onPartChange();

        getBarCycler().advance();
        getBarCounter().advance();
    }

    public void onPartChange() {
        getPartCycler().advance();
        getPartCounter().advance();
    }

    private void updatePlayerConfig() {
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }

        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }
    }
}

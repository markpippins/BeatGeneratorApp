package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.IPlayer;
import com.angrysurfer.core.api.ITicker;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Ticker implements Serializable, ITicker {

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());

    @JsonIgnore
    @Transient
    ClockSource clockSource;

    @JsonIgnore
    @Transient
    private Set<IPlayer> removeList = new HashSet<>();

    @JsonIgnore
    @Transient
    private Cycler beatCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler beatCounter = new Cycler(0);

    @JsonIgnore
    @Transient
    private Cycler barCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler barCounter = new Cycler(0);

    @JsonIgnore
    @Transient
    private Cycler partCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler partCounter = new Cycler(0);

    @JsonIgnore
    @Transient
    private Cycler tickCycler = new Cycler();

    @JsonIgnore
    @Transient
    private Cycler tickCounter = new Cycler(0);

    @Transient
    private boolean done = false;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Transient
    private Set<IPlayer> players = new HashSet<>();

    @Transient
    Set<Long> activePlayerIds = new HashSet<>();

    @Transient
    private double granularBeat = 0.0;

    private Integer bars = Constants.DEFAULT_BAR_COUNT;
    private Integer beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private Integer beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private Long partLength = Constants.DEFAULT_PART_LENGTH;
    private Integer maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private Long songLength = Constants.DEFAULT_SONG_LENGTH;
    private Long swing = Constants.DEFAULT_SWING;
    private Integer ticksPerBeat = Constants.DEFAULT_PPQ;
    private Float tempoInBPM = Constants.DEFAULT_BPM;
    private Integer loopCount = Constants.DEFAULT_LOOP_COUNT;
    private Integer parts = Constants.DEFAULT_PART_COUNT;
    private Double noteOffset = 0.0;

    @Transient
    private boolean paused = false;

    @Transient
    @JsonIgnore
    private Set<MuteGroup> muteGroups = new HashSet<>();

    public Ticker() {
        setSongLength(Long.MAX_VALUE);
    }

    @Override
    public IPlayer getPlayer(Long playerId) {
        logger.info("getPlayer() - playerId: {}", playerId);
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    @Override
    public double getBeat() {
        // // logger.debug("getBeat() - current beat: {}", getBeatCycler().get());
        return getBeatCycler().get();
    }

    @Override
    public Long getBeatCount() {
        // // logger.debug("getBeatCount() - current count: {}",
        // getBeatCounter().get());
        return getBeatCounter().get();
    }

    @Override
    public void setBeatsPerBar(int beatsPerBar) {
        logger.info("setBeatsPerBar() - new value: {}", beatsPerBar);
        this.beatsPerBar = beatsPerBar;
        getBeatCycler().setLength((long) beatsPerBar);
    }

    @Override
    public Long getTick() {
        // logger.debug("getTick() - current tick: {}", getTickCycler().get());
        return getTickCycler().get();
    }

    @Override
    public Long getTickCount() {
        // logger.debug("getTickCount() - current count: {}", getTickCounter().get());
        return getTickCounter().get();
    }

    @Override
    public void setTicksPerBeat(int ticksPerBeat) {
        logger.info("setTicksPerBeat() - new value: {}", ticksPerBeat);
        this.ticksPerBeat = ticksPerBeat;
        getTickCycler().setLength((long) ticksPerBeat);
    }

    @Override
    public Long getBar() {
        // logger.debug("getBar() - current bar: {}", getBarCycler().get());
        return getBarCycler().get();
    }

    @Override
    public Long getBarCount() {
        // logger.debug("getBarCount() - current count: {}", getBarCounter().get());
        return getBarCounter().get();
    }

    @Override
    public void setBars(int bars) {
        logger.info("setBars() - new value: {}", bars);
        this.bars = bars;
        getBarCycler().setLength((long) bars);
    }

    @Override
    public Long getPart() {
        // logger.debug("getPart() - current part: {}", getPartCycler().get());
        return getPartCycler().get();
    }

    @Override
    public Long getPartCount() {
        // logger.debug("getPartCount() - current count: {}", getPartCounter().get());
        return getPartCounter().get();
    }

    @Override
    public void setParts(int parts) {
        logger.info("setParts() - new value: {}", parts);
        this.parts = parts;
        this.partCycler.setLength((long) parts);
    }

    @Override
    public void setPartLength(long partLength) {
        this.partLength = partLength;
    }

    @Override
    public void reset() {
        logger.info("reset() - resetting all cyclers and counters");
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();

        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();

        // getAddList().clear();
        getRemoveList().forEach(r -> getPlayers().remove(r));
        getRemoveList().clear();
    }

    private void clearMuteGroups() {
    }

    private void createMuteGroups() {
    }

    private void updateMuteGroups() {
    }

    @Override
    public double getBeatDivision() {
        return 1.0 / beatDivider;
    }

    @Override
    public void afterEnd() {
        logger.info("afterEnd() - resetting cyclers and stopping all notes");
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();

        IntStream.range(0, 127).forEach(note -> {
            getPlayers().forEach(p -> {
                try {
                    p.getInstrument().noteOff(p.getChannel(), note, 0);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error("Error stopping note {} on channel {}: {}",
                            note, p.getChannel(), e.getMessage(), e);
                }
            });
        });
    }

    @Override
    public void beforeStart() {
        logger.info("beforeStart() - initializing cycler lengths");
        getTickCycler().setLength((long) getTicksPerBeat());
        getBarCycler().setLength((long) getBars());
        getBeatCycler().setLength((long) getBeatsPerBar());
        getPartCycler().setLength((long) getPartLength());
        getPlayers().forEach(p -> p.getSkipCycler().setLength(p.getSkips()));
    }

    @Override
    public void onStart() {
        logger.info("onStart() - notifying beat and bar listeners");
        getBeatCycler().getListeners().forEach(l -> l.starting());
        getBarCycler().getListeners().forEach(l -> l.starting());
    }

    @Override
    public void onStop() {
        logger.info("onStop() - resetting beat cycler");
        getBeatCycler().reset();
    }

    @Override
    public void beforeTick() {
    }

    @Override
    public void afterTick() {
        logger.debug("afterTick() - granularBeat: {}", granularBeat);
        granularBeat += 1.0 / getTicksPerBeat();

        if (getTick() % getTicksPerBeat() == 0)
            onBeatChange();

        getTickCycler().advance();
        getTickCounter().advance();
    }

    @Override
    public void onBeatChange() {
        logger.debug("onBeatChange() - current beat: {}", getBeat());
        setGranularBeat(0.0);
        getBeatCycler().advance();
        getBeatCounter().advance();

        if (getBeat() % getBeatsPerBar() == 0)
            onBarChange();
    }

    @Override
    public void onBarChange() {
        logger.debug("onBarChange() - current bar: {}", getBar());
        updatePlayerConfig();
        if (getBar() % getPartLength() == 0)
            onPartChange();

        getBarCycler().advance();
        getBarCounter().advance();
    }

    @Override
    public void onPartChange() {
        logger.debug("onPartChange() - current part: {}", getPart());
        getPartCycler().advance();
        getPartCounter().advance();
    }

    private void updatePlayerConfig() {
        // logger.debug("updatePlayerConfig() - removing {} players, adding {} players",
        // getRemoveList().size(), getAddList().size());

        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }

        // if (!getAddList().isEmpty()) {
        // getPlayers().addAll(getAddList());
        // getAddList().clear();
        // }
    }

    @Override
    public Float getBeatDuration() {
        // logger.debug("getBeatDuration() - calculated duration: {}",
        // 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar());
        return 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar();
    }

    @Override
    public double getInterval() {
        // logger.debug("getInterval() - calculated interval: {}",
        // 60000 / getTempoInBPM() / getTicksPerBeat());
        return 60000 / getTempoInBPM() / getTicksPerBeat();
    }

    @Override
    public Boolean hasSolos() {
        boolean result = this.getPlayers().stream().anyMatch(player -> player.isSolo());
        // logger.debug("hasSolos() - result: {}", result);
        return result;
    }

    @Override
    public boolean isRunning() {
        return (Objects.nonNull(clockSource) && clockSource.isRunning());
    }
}

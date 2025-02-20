package com.angrysurfer.core.proxy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.engine.MIDIEngine;
import com.angrysurfer.core.model.ITicker;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProxyTicker implements Serializable, ITicker {

    @Override
    public List<Callable<Boolean>> getCallables() {
        // Implement the method as required
        return getPlayers().stream()
                .map(player -> (Callable<Boolean>) () -> player.call())
                .collect(Collectors.toList());
    }

    @Override
    public void setParts(Integer parts) {
        this.parts = parts;
        this.partCycler.setLength((long) parts);
    }

    @Override
    public void setBars(Integer bars) {
        this.bars = bars;
        this.barCycler.setLength((long) bars);
    }

    @Override
    public void setBeatsPerBar(Integer beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        this.beatCycler.setLength((long) beatsPerBar);
    }

    private Long id;

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(ProxyTicker.class.getCanonicalName());

    @JsonIgnore
    @Transient
    public boolean isFirst = false;

    @JsonIgnore
    @Transient
    public boolean isLast = false;

    @JsonIgnore
    @Transient
    ClockSource clockSource;

    @JsonIgnore
    @Transient
    private Set<IProxyPlayer> addList = new HashSet<>();

    @JsonIgnore
    @Transient
    private Set<IProxyPlayer> removeList = new HashSet<>();

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

    @JsonIgnore
    private transient Set<IProxyPlayer> players = new HashSet<>();

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
    private Set<ProxyMuteGroup> muteGroups = new HashSet<>();

    public ProxyTicker() {
        setSongLength(Long.MAX_VALUE);
    }

    public ProxyTicker(float tempoInBPM, int bars, int beatsPerBar, int ticksPerBeat, int parts, long partLength) {
        this.tempoInBPM = tempoInBPM;
        this.bars = bars;
        this.beatsPerBar = beatsPerBar;
        this.ticksPerBeat = ticksPerBeat;
        this.parts = parts;
        this.partLength = partLength;
    }

    public IProxyPlayer getPlayer(Long playerId) {
        logger.info("getPlayer() - playerId: {}", playerId);
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public double getBeat() {
        // // logger.debug("getBeat() - current beat: {}", getBeatCycler().get());
        return getBeatCycler().get();
    }

    public Long getBeatCount() {
        // // logger.debug("getBeatCount() - current count: {}",
        // getBeatCounter().get());
        return getBeatCounter().get();
    }

    public void setBeatsPerBar(int beatsPerBar) {
        logger.info("setBeatsPerBar() - new value: {}", beatsPerBar);
        this.beatsPerBar = beatsPerBar;
        getBeatCycler().setLength((long) beatsPerBar);
    }

    public Long getTick() {
        // logger.debug("getTick() - current tick: {}", getTickCycler().get());
        return getTickCycler().get();
    }

    public Long getTickCount() {
        // logger.debug("getTickCount() - current count: {}", getTickCounter().get());
        return getTickCounter().get();
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        logger.info("setTicksPerBeat() - new value: {}", ticksPerBeat);
        this.ticksPerBeat = ticksPerBeat;
        getTickCycler().setLength((long) ticksPerBeat);
    }

    public Long getBar() {
        // logger.debug("getBar() - current bar: {}", getBarCycler().get());
        return getBarCycler().get();
    }

    public Long getBarCount() {
        // logger.debug("getBarCount() - current count: {}", getBarCounter().get());
        return getBarCounter().get();
    }

    public void setBars(int bars) {
        logger.info("setBars() - new value: {}", bars);
        this.bars = bars;
        getBarCycler().setLength((long) bars);
    }

    public Long getPart() {
        // logger.debug("getPart() - current part: {}", getPartCycler().get());
        return getPartCycler().get();
    }

    public Long getPartCount() {
        // logger.debug("getPartCount() - current count: {}", getPartCounter().get());
        return getPartCounter().get();
    }

    public void setParts(int parts) {
        logger.info("setParts() - new value: {}", parts);
        this.parts = parts;
        this.partCycler.setLength((long) parts);
    }

    public void setPartLength(long partLength) {
        this.partLength = partLength;
    }

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

    public void beforeStart() {
        logger.info("beforeStart() - initializing cycler lengths");
        getTickCycler().setLength((long) getTicksPerBeat());
        getBarCycler().setLength((long) getBars());
        getBeatCycler().setLength((long) getBeatsPerBar());
        getPartCycler().setLength((long) getPartLength());
        getPlayers().forEach(p -> p.getSkipCycler().setLength(p.getSkips()));
    }

    public void onStart() {
        logger.info("onStart() - notifying beat and bar listeners");
        getBeatCycler().getListeners().forEach(l -> l.starting());
        getBarCycler().getListeners().forEach(l -> l.starting());
    }

    public void onStop() {
        logger.info("onStop() - resetting beat cycler");
        getBeatCycler().reset();
    }

    public void beforeTick() {
    }

    public void afterTick() {
        logger.debug("afterTick() - granularBeat: {}", granularBeat);
        granularBeat += 1.0 / getTicksPerBeat();

        if (getTick() % getTicksPerBeat() == 0)
            onBeatChange();

        getTickCycler().advance();
        getTickCounter().advance();
    }

    public void onBeatChange() {
        logger.debug("onBeatChange() - current beat: {}", getBeat());
        setGranularBeat(0.0);
        getBeatCycler().advance();
        getBeatCounter().advance();

        if (getBeat() % getBeatsPerBar() == 0)
            onBarChange();
    }

    public void onBarChange() {
        logger.debug("onBarChange() - current bar: {}", getBar());
        updatePlayerConfig();
        if (getBar() % getPartLength() == 0)
            onPartChange();

        getBarCycler().advance();
        getBarCounter().advance();
    }

    public void onPartChange() {
        logger.debug("onPartChange() - current part: {}", getPart());
        getPartCycler().advance();
        getPartCounter().advance();
    }

    private void updatePlayerConfig() {
        logger.debug("updatePlayerConfig() - removing {} players, adding {} players",
                getRemoveList().size(), getAddList().size());
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }

        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }
    }

    public Float getBeatDuration() {
        // logger.debug("getBeatDuration() - calculated duration: {}",
        // 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar());
        return 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar();
    }

    public double getInterval() {
        // logger.debug("getInterval() - calculated interval: {}",
        // 60000 / getTempoInBPM() / getTicksPerBeat());
        return 60000 / getTempoInBPM() / getTicksPerBeat();
    }

    public Boolean hasSolos() {
        boolean result = this.getPlayers().stream().anyMatch(player -> player.isSolo());
        // logger.debug("hasSolos() - result: {}", result);
        return result;
    }

    public boolean isRunning() {
        return (Objects.nonNull(clockSource) && clockSource.isRunning());
    }

    public Set<IProxyPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(Set<IProxyPlayer> players) {
        this.players = players;
    }

    public synchronized boolean isValid() {
        return (Objects.nonNull(getPlayers()) && !getPlayers().isEmpty()
                && getPlayers().stream().anyMatch(p -> p.isValid()));
    }

    public void play() {

        stopRunningClocks();

        List<MidiDevice> devices = MIDIEngine.getMidiOutDevices();

        getPlayers().forEach(p -> {

            ProxyInstrument instrument = p.getInstrument();
            if (Objects.nonNull(instrument)) {
                Optional<MidiDevice> device = devices.stream()
                        .filter(d -> d.getDeviceInfo().getName().equals(instrument.getDeviceName())).findFirst();

                if (device.isPresent() && !device.get().isOpen())
                    try {
                        device.get().open();
                        instrument.setDevice(device.get());
                    } catch (MidiUnavailableException e) {
                        logger.error(e.getMessage(), e);
                    }

                else
                    logger.error(instrument.getDeviceName() + " not initialized");
            } else
                logger.error("Instrument not initialized");
        });

        // new Thread(newClockSource(this)).start();

        getPlayers().forEach(p -> {
            try {
                if (p.getPreset() > -1)
                    p.getInstrument().programChange(p.getChannel(), p.getPreset(), 0);
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                logger.error(e.getMessage(), e);
            }
        });

    }

    private ClockSource newClockSource() {
        stopRunningClocks();
        // clockSource = new ClockSource(this);
        // clocks.add(clockSource);
        setClockSource(clockSource);
        return clockSource;
    }

    private void stopRunningClocks() {
        // clocks.forEach(sr -> sr.stop());
        // clocks.clear();
    }

    public void stop() {
        stopRunningClocks();
        setPaused(false);
        getBeatCycler().reset();
        getBarCycler().reset();
        setDone(false);
        reset();
    }

}

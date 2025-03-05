package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.service.MIDIDeviceManager;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ticker implements Serializable, CommandListener {

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());

    private Long id;

    @JsonIgnore
    @Transient
    public boolean isActive = false;

    @JsonIgnore
    @Transient
    ClockSource clockSource;

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
    private transient Set<Player> players = new HashSet<>();

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

    private CommandBus commandBus = CommandBus.getInstance();
    private TimingBus timingBus = TimingBus.getInstance(); // Add this

    public Ticker() {
        setSongLength(Long.MAX_VALUE);
        commandBus.register(this);
        timingBus.register(this);  // Add this registration
    }

    public Ticker(float tempoInBPM, int bars, int beatsPerBar, int ticksPerBeat, int parts, long partLength) {
        this.tempoInBPM = tempoInBPM;
        this.bars = bars;
        this.beatsPerBar = beatsPerBar;
        this.ticksPerBeat = ticksPerBeat;
        this.parts = parts;
        this.partLength = partLength;
    }

    public void addPlayer(Player player) {
        logger.info("addPlayer() - adding player: {}", player);
        if (isRunning())
            synchronized (getAddList()) {
                getAddList().add(player);
            }
        else
            getPlayers().add(player);

        player.setTicker(this);
        commandBus.publish(Commands.PLAYER_ADDED, this, player);
    }

    public void removePlayer(Player player) {
        logger.info("addPlayer() - removing player: {}", player);
        if (isRunning())
            synchronized (getRemoveList()) {
                getRemoveList().add(player);
            }
        else
            getPlayers().remove(player);

        player.setTicker(null);
        commandBus.publish(Commands.PLAYER_ADDED, this, player);
    }

    public Player getPlayer(Long playerId) {
        logger.info("getPlayer() - playerId: {}", playerId);
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public void setParts(Integer parts) {
        this.parts = parts;
        this.partCycler.setLength((long) parts);
    }

    public void setBars(Integer bars) {
        this.bars = bars;
        this.barCycler.setLength((long) bars);
    }

    public void setBeatsPerBar(Integer beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        this.beatCycler.setLength((long) beatsPerBar);
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
        for (Player player : getPlayers())
            player.setTicker(this);
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

    public Set<Player> getPlayers() {
        return players;
    }

    public void setPlayers(Set<Player> players) {
        this.players = players;
    }

    public synchronized boolean isValid() {
        return (Objects.nonNull(getPlayers()) && !getPlayers().isEmpty()
                && getPlayers().stream().anyMatch(p -> Objects.nonNull(p.getRules()) && p.getRules().size() > 0));
    }

    public void play() {
        stopRunningClocks();
        initializeDevices();
        commandBus.publish(Commands.TRANSPORT_PLAY, this);
    }

    private void initializeDevices() {
        List<MidiDevice> devices = MIDIDeviceManager.getMidiOutDevices();
        
        getPlayers().forEach(p -> {
            initializePlayerDevice(p, devices);
            initializePlayerPreset(p);
            p.setTicker(this);
        });
    }

    private void initializePlayerDevice(Player player, List<MidiDevice> devices) {
        Instrument instrument = player.getInstrument();
        if (instrument != null) {
            devices.stream()
                  .filter(d -> d.getDeviceInfo().getName().equals(instrument.getDeviceName()))
                  .findFirst()
                  .ifPresent(device -> {
                      try {
                          if (!device.isOpen()) {
                              device.open();
                          }
                          instrument.setDevice(device);
                      } catch (MidiUnavailableException e) {
                          logger.error("Failed to open device: " + e.getMessage(), e);
                      }
                  });
        }
    }

    private void initializePlayerPreset(Player player) {
        try {
            if (player.getPreset() > -1) {
                player.getInstrument().programChange(
                    player.getChannel(), 
                    player.getPreset(), 
                    0
                );
            }
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Failed to set preset: " + e.getMessage(), e);
        }
    }

    private void stopRunningClocks() {
        if (clockSource != null) {
            clockSource.stop();
            clockSource = null;
        }
    }

    public void stop() {
        stopRunningClocks();
        setPaused(false);
        getBeatCycler().reset();
        getBarCycler().reset();
        setDone(false);
        reset();
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.BASIC_TIMING_TICK -> {
                beforeTick();
                // Players now handle their own timing
                afterTick();
            }
            case Commands.TRANSPORT_PLAY -> {
                beforeStart();
                if (clockSource == null) {
                    clockSource = new ClockSource();
                }
                clockSource.start();
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
            }
            case Commands.TRANSPORT_STOP -> {
                if (clockSource != null) {
                    clockSource.stop();
                }
                stop();
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
            }
            case Commands.TRANSPORT_PAUSE -> {
                if (clockSource != null) {
                    clockSource.pause();
                }
                setPaused(true);
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
            }
            case Commands.BASIC_TIMING_BEAT -> {
                onBeatChange();
            }
        }
    }

}
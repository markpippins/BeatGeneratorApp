package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.MidiClockSource;
import com.angrysurfer.core.util.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Session implements Serializable, IBusListener {

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(Session.class.getCanonicalName());

    private Long id;

    @JsonIgnore
    @Transient
    public boolean isActive = false;

    @JsonIgnore
    @Transient
    private Set<Player> addList = new HashSet<>();

    @JsonIgnore
    @Transient
    private Set<Player> removeList = new HashSet<>();

    // Replace Cyclers with simple variables initialized to 1 instead of 0
    @JsonIgnore
    @Transient
    private long tick = 1;

    @JsonIgnore
    @Transient
    private double beat = 1.0;

    @JsonIgnore
    @Transient
    private long bar = 1;

    @JsonIgnore
    @Transient
    private long part = 1;

    @JsonIgnore
    @Transient
    private long tickCount = 0;

    @JsonIgnore
    @Transient
    private long beatCount = 0;

    @JsonIgnore
    @Transient
    private long barCount = 0;

    @JsonIgnore
    @Transient
    private long partCount = 0;

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

    @JsonIgnore
    @Transient
    private CommandBus commandBus = CommandBus.getInstance();

    @JsonIgnore
    @Transient
    private TimingBus timingBus = TimingBus.getInstance(); // Add this

    @JsonIgnore
    @Transient
    private final MidiClockSource sequencerManager = new MidiClockSource();

    // Variables to store lengths (replacing Cycler lengths)
    private long tickLength;
    private long barLength;

    // Consumer for tick callbacks (replacing CyclerListener)
    @JsonIgnore
    @Transient
    private Consumer<Long> tickListener;

    // FindAll<ControlCodeCaption> getCaptionFindAll();

    public Session() {
        setSongLength(Long.MAX_VALUE);
        commandBus.register(this);
        timingBus.register(this); // Add this registration
    }

    public Session(float tempoInBPM, int bars, int beatsPerBar, int ticksPerBeat, int parts, long partLength) {
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

        player.setSession(this);
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

        player.setSession(null);
        commandBus.publish(Commands.PLAYER_ADDED, this, player);
    }

    public Player getPlayer(Long playerId) {
        logger.info("getPlayer() - playerId: {}", playerId);
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public Player getPlayerById(Long id) {
        if (players == null || id == null)
            return null;

        for (Player player : players) {
            if (id.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

    public void setParts(Integer parts) {
        this.parts = parts;
    }

    public void setBars(Integer bars) {
        this.bars = bars;
    }

    public void setBeatsPerBar(Integer beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
    }

    public double getBeat() {
        return beat;
    }

    public Long getBeatCount() {
        return beatCount;
    }

    public Long getTick() {
        return tick;
    }

    public Long getTickCount() {
        return tickCount;
    }

    public Long getBar() {
        return bar;
    }

    public Long getBarCount() {
        return barCount;
    }

    public Long getPart() {
        return part;
    }

    public Long getPartCount() {
        return partCount;
    }

    public void setPartLength(long partLength) {
        this.partLength = partLength;
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    /**
     * Make this a complete reset of all state
     */
    public void reset() {
        System.out.println("Session: Resetting session state");

        // Reset all position variables to 1 instead of 0
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;
        
        // Counters still start at 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;
        System.out.println("Session: All counters reset");

        // Reset player state
        if (getPlayers() != null) {
            System.out.println("Session: Resetting " + getPlayers().size() + " players");
            getPlayers().forEach(p -> {
                if (p.getSkipCycler() != null) {
                    p.getSkipCycler().reset();
                }
                p.setEnabled(false);
                System.out.println("Session: Reset player " + p.getId());
            });
        }

        // Clear lists
        if (addList != null) {
            addList.clear();
            System.out.println("Session: Add list cleared");
        }
        if (removeList != null) {
            removeList.clear();
            System.out.println("Session: Remove list cleared");
        }

        // Reset state flags
        done = false;
        isActive = false;
        paused = false;
        System.out.println("Session: State flags reset");

        // Clear mute groups
        clearMuteGroups();
        System.out.println("Session: Mute groups cleared");

        System.out.println("Session: Reset complete");
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

    /**
     * Clean up at the end of playback
     */
    public void afterEnd() {
        logger.info("afterEnd() - resetting position and stopping all notes");

        // Reset all position variables to 1
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;
        
        // Reset counters to 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;

        // Reset granular beat tracking
        setGranularBeat(0.0);

        // Stop all notes
        stopAllNotes();
    }

    /**
     * Initialize all timing variables before session playback starts
     */
    public void beforeStart() {
        System.out.println("Session: Preparing to start session " + getId());

        // Reset state
        reset();
        System.out.println("Session: State reset");

        // Initialize lengths
        tickLength = ticksPerBeat;
        barLength = bars;
        System.out.println("Session: Timing variables initialized:");
        System.out.println("  - Ticks per beat: " + ticksPerBeat);
        System.out.println("  - Beats per bar: " + beatsPerBar);
        System.out.println("  - Bars: " + bars);
        System.out.println("  - Part length: " + partLength);

        // Add tick listener
        setupTickListener();

        // Set up players
        if (getPlayers() != null && !getPlayers().isEmpty()) {
            System.out.println("Session: Setting up " + getPlayers().size() + " players");
            getPlayers().forEach(p -> {
                System.out.println("Session: Setting up player " + p.getId() + " (" + p.getName() + ")");
                p.setEnabled(true);
                if (p.getRules() != null) {
                    System.out.println("  - Player has " + p.getRules().size() + " rules");
                }
            });
        } else {
            System.out.println("Session: No players to set up");
        }

        // Register with timing bus
        timingBus.register(this);
        System.out.println("Session: Registered with timing bus");

        // Set active state
        isActive = true;
        System.out.println("Session: Session marked as active");

        // Publish session starting event
        commandBus.publish(Commands.SESSION_STARTING, this);
        System.out.println("Session: Published SESSION_STARTING event");
    }

    /**
     * Helper method to stop all active notes
     */
    private void stopAllNotes() {
        IntStream.range(0, 127).forEach(note -> {
            getPlayers().forEach(p -> {
                try {
                    if (p.getInstrument() != null) {
                        p.getInstrument().noteOff(p.getChannel(), note, 0);
                    }
                } catch (Exception e) {
                    logger.error("Error stopping note {} on channel {}: {}", note, p.getChannel(), e.getMessage(), e);
                }
            });
        });
    }

    private void updatePlayerConfig() {
        logger.debug("updatePlayerConfig() - removing {} players, adding {} players", getRemoveList().size(),
                getAddList().size());
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
        logger.debug("getBeatDuration() - calculated duration: {}",
                60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar());
        return 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar();
    }

    public double getInterval() {
        logger.debug("getInterval() - calculated interval: {}", 60000 / getTempoInBPM() / getTicksPerBeat());
        return 60000 / getTempoInBPM() / getTicksPerBeat();
    }

    private final Object soloLock = new Object();

    public Boolean hasSolos() {
        synchronized (soloLock) {
            for (Player player : players) {
                if (player.isSolo()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isRunning() {
        boolean running = sequencerManager != null && sequencerManager.isRunning();
        System.out.println("Session.isRunning returning: " + running);
        return running;
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
        System.out.println("Session: Starting play sequence for session " + getId());

        System.out.println("Session cyclers before play:");
        System.out.println("  - Tick cycler: " + tick + " (length: " + tickLength + ")");
        System.out.println("  - Beat cycler: " + beat + " (length: " + beatsPerBar + ")");
        System.out.println("  - Bar cycler: " + bar + " (length: " + barLength + ")");
        System.out.println("  - Part cycler: " + part + " (length: " + partLength + ")");

        reset();

        initializeDevices();

        System.out.println("Session: Registering " + players.size() + " players with timing bus");
        for (Player player : players) {
            timingBus.register(player);
            System.out.println("  - Registered player: " + player.getName());
        }

        sequencerManager.startSequence();

        System.out.println("Session: Play sequence completed");
    }

    public void initializeDevices() {
        System.out.println("Session: Initializing devices for session " + getId());
        List<MidiDevice> devices = DeviceManager.getMidiOutDevices();
        System.out.println("Session: Found " + devices.size() + " MIDI output devices");

        if (getPlayers() == null || getPlayers().isEmpty()) {
            System.out.println("Session: No players to initialize!");
            return;
        }

        System.out.println("Session: Initializing " + getPlayers().size() + " players");
        getPlayers().forEach(p -> {
            System.out.println("Session: Initializing player " + p.getId() + " (" + p.getName() + ")");
            initializePlayerDevice(p, devices);
            initializePlayerPreset(p);
            p.setSession(this);
            p.setEnabled(true);
            System.out.println("Session: Player " + p.getId() + " initialized and enabled");
        });
        System.out.println("Session: Device initialization complete");
    }

    private void initializePlayerDevice(Player p, List<MidiDevice> devices) {
        System.out.println("Session: Initializing device for player " + p.getName());

        if (p.getInstrument() == null) {
            System.out.println("WARNING: Player " + p.getName() + " has no instrument!");
            return;
        }

        try {
            System.out.println(
                    "Session: Player " + p.getName() + " initialized with instrument " + p.getInstrument().getName());
        } catch (Exception e) {
            System.err.println("Error initializing player device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializePlayerPreset(Player player) {
        System.out.println("Session: Setting preset for player " + player.getId());
        try {
            if (player.getPreset() > -1) {
                System.out.println(
                        "Session: Setting preset " + player.getPreset() + " on channel " + player.getChannel());
                player.getInstrument().programChange(player.getChannel(), player.getPreset(), 0);
                System.out.println("Session: Preset set successfully");
            } else {
                System.out.println("Session: No preset configured for player");
            }
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            System.err.println("Session: Failed to set preset: " + e.getMessage());
        }
    }

    public void stop() {

        sequencerManager.stop();

        getPlayers().forEach(p -> p.setEnabled(false));

        logger.info("onStop() - resetting all cyclers and counters");

        tickListener = null;

        // Reset to 1 instead of 0
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;
        
        // Reset counters to 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;

        setGranularBeat(0.0);

        sequencerManager.cleanup();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() != null) {
            System.out.println("Session received action: " + action.getCommand());
            switch (action.getCommand()) {
            case Commands.TIMING_PARAMETERS_CHANGED -> sequencerManager.updateTimingParameters(getTempoInBPM(),
                    getTicksPerBeat(), getBeatsPerBar());
            }
        }
    }

    public void onTick() {
        // Cycle from 1 to tickLength
        tick = tick % tickLength + 1;
        tickCount++;

        System.out.println("Session.onTick() - tick: " + tick + "/" + tickLength);

        if (tickListener != null) {
            tickListener.accept(tick);
        }

        if (tick % ticksPerBeat == 1) {  // Changed from 0 to 1
            onBeatChange();
        }
        
        timingBus.publish(Commands.TIME_TICK, this, tick);
    }

    private void onBeatChange() {
        // Cycle from 1 to beatsPerBar
        beat = beat % beatsPerBar + 1.0;
        beatCount++;

        TimingBus.getInstance().publish(Commands.TIME_BEAT, this, beat);

        if (beat == 1.0) {  // Changed from 0.0 to 1.0
            onBarChange();
        }
    }

    private void onBarChange() {
        // Cycle from 1 to barLength
        bar = bar % barLength + 1;
        barCount++;

        TimingBus.getInstance().publish(Commands.TIME_BAR, this, bar);

        if (bar % partLength == 1) {  // Changed from 0 to 1
            onPartChange();
        }
    }

    public void onPartChange() {
        // Cycle from 1 to parts
        part = part % parts + 1;
        partCount++;

        TimingBus.getInstance().publish(Commands.TIME_PART, this, part);
    }

    private void setupTickListener() {
        tickListener = (tickPosition) -> {
        };
    }

    private void removeTickListener() {
        tickListener = null;
    }

    public void syncToSequencer() {
        sequencerManager.updateTimingParameters(getTempoInBPM(), getTicksPerBeat(), getBeatsPerBar());
    }

    public void setTempoInBPM(float tempoInBPM) {
        this.tempoInBPM = tempoInBPM;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        this.ticksPerBeat = ticksPerBeat;
        this.tickLength = ticksPerBeat;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setBars(int bars) {
        this.bars = bars;
        this.barLength = bars;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setParts(int parts) {
        this.parts = parts;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void updatePlayer(Player updatedPlayer) {
        if (updatedPlayer == null || updatedPlayer.getId() == null) {
            return;
        }

        if (players == null) {
            players = new LinkedHashSet<>();
        }

        players.removeIf(p -> p.getId().equals(updatedPlayer.getId()));

        updatedPlayer.setSession(this);
        players.add(updatedPlayer);
    }
}
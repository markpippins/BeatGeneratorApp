package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import com.angrysurfer.core.util.Cycler;
import com.angrysurfer.core.util.CyclerListener;
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

    @JsonIgnore
    @Transient
    private CommandBus commandBus = CommandBus.getInstance();

    @JsonIgnore
    @Transient
    private TimingBus timingBus = TimingBus.getInstance(); // Add this

    @JsonIgnore
    @Transient
    private final MidiClockSource sequencerManager = new MidiClockSource();

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
        // logger.debug("getBeat() - current beat: {}", getBeatCycler().get());
        return getBeatCycler().get();
    }

    public Long getBeatCount() {
        // logger.debug("getBeatCount() - current count: {}",
        // getBeatCounter().get());
        return getBeatCounter().get();
    }

    public Long getTick() {
        logger.debug("getTick() - current tick: {}", getTickCycler().get());
        return getTickCycler().get();
    }

    public Long getTickCount() {
        logger.debug("getTickCount() - current count: {}", getTickCounter().get());
        return getTickCounter().get();
    }

    public Long getBar() {
        logger.debug("getBar() - current bar: {}", getBarCycler().get());
        return getBarCycler().get();
    }

    public Long getBarCount() {
        logger.debug("getBarCount() - current count: {}", getBarCounter().get());
        return getBarCounter().get();
    }

    public void setBars(int bars) {
        logger.info("setBars() - new value: {}", bars);
        this.bars = bars;
        getBarCycler().setLength((long) bars);
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public Long getPart() {
        logger.debug("getPart() - current part: {}", getPartCycler().get());
        return getPartCycler().get();
    }

    public Long getPartCount() {
        logger.debug("getPartCount() - current count: {}", getPartCounter().get());
        return getPartCounter().get();
    }

    public void setParts(int parts) {
        logger.info("setParts() - new value: {}", parts);
        this.parts = parts;
        this.partCycler.setLength((long) parts);
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
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

        // Reset all cyclers
        if (tickCycler != null) {
            tickCycler.reset();
            System.out.println("Session: Tick cycler reset");
        }
        if (beatCycler != null) {
            beatCycler.reset();
            System.out.println("Session: Beat cycler reset");
        }
        if (barCycler != null) {
            barCycler.reset();
            System.out.println("Session: Bar cycler reset");
        }
        if (partCycler != null) {
            partCycler.reset();
            System.out.println("Session: Part cycler reset");
        }

        // Reset all counters
        if (tickCounter != null) {
            tickCounter.reset();
            System.out.println("Session: Tick counter reset");
        }
        if (beatCounter != null) {
            beatCounter.reset();
            System.out.println("Session: Beat counter reset");
        }
        if (barCounter != null) {
            barCounter.reset();
            System.out.println("Session: Bar counter reset");
        }
        if (partCounter != null) {
            partCounter.reset();
            System.out.println("Session: Part counter reset");
        }

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
        logger.info("afterEnd() - resetting cyclers and stopping all notes");

        // Reset all primary cyclers
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();

        // Also reset counter cyclers
        // Comment if you want to keep count across stop/start
        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();

        // Reset granular beat tracking
        setGranularBeat(0.0);

        // Stop all notes
        stopAllNotes();
    }

    /**
     * Initialize all cyclers with appropriate lengths before session playback
     * starts
     */
    public void beforeStart() {
        System.out.println("Session: Preparing to start session " + getId());

        // Reset state
        reset();
        System.out.println("Session: State reset");

        // Initialize cycler lengths
        tickCycler.setLength(Long.valueOf(ticksPerBeat));
        beatCycler.setLength(Long.valueOf(beatsPerBar));
        barCycler.setLength(Long.valueOf(bars));
        partCycler.setLength(Long.valueOf(partLength));
        System.out.println("Session: Cycler lengths initialized:");
        System.out.println("  - Ticks per beat: " + ticksPerBeat);
        System.out.println("  - Beats per bar: " + beatsPerBar);
        System.out.println("  - Bars: " + bars);
        System.out.println("  - Part length: " + partLength);

        // Initialize counters
        tickCounter.setLength(0L);
        beatCounter.setLength(0L);
        barCounter.setLength(0L);
        partCounter.setLength(0L);
        System.out.println("Session: Counters reset");

        // add tick listener
        addTickListener();

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
        // Delegate to the SequencerManager since it knows the real state
        // SequencerManager sequencerManager = SequencerManager.getInstance();
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

    // Update play() method for better integration with SessionManager
    public void play() {
        System.out.println("Session: Starting play sequence for session " + getId());

        // Debug the cycler state before starting
        System.out.println("Session cyclers before play:");
        System.out.println("  - Tick cycler: " + tickCycler.get() + " (length: " + tickCycler.getLength() + ")");
        System.out.println("  - Beat cycler: " + beatCycler.get() + " (length: " + beatCycler.getLength() + ")");
        System.out.println("  - Bar cycler: " + barCycler.get() + " (length: " + barCycler.getLength() + ")");
        System.out.println("  - Part cycler: " + partCycler.get() + " (length: " + partCycler.getLength() + ")");

        // Reset state
        reset();

        // Initialize players
        initializeDevices();

        // Register all players with the timing bus
        System.out.println("Session: Registering " + players.size() + " players with timing bus");
        for (Player player : players) {
            timingBus.register(player);
            System.out.println("  - Registered player: " + player.getName());
        }

        // Start sequencer
        sequencerManager.startSequence();

        System.out.println("Session: Play sequence completed");
    }

    // Change from private to public
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
            // Your existing code
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

        // Disable all players when stopping
        getPlayers().forEach(p -> p.setEnabled(false));

        logger.info("onStop() - resetting all cyclers and counters");

        getTickCycler().getListeners().clear();

        // Reset all primary cyclers
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();

        // Also reset counter cyclers
        // Comment if you want to keep count across stop/start
        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();

        // Reset granular beat tracking
        setGranularBeat(0.0);

        sequencerManager.cleanup();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() != null)
            switch (action.getCommand()) {
            case Commands.TIME_TICK -> onTick();
            case Commands.TIMING_PARAMETERS_CHANGED -> sequencerManager.updateTimingParameters(getTempoInBPM(),
                    getTicksPerBeat(), getBeatsPerBar());
            }
    }

    private void onTick() {
        getTickCycler().advance();
    }

    // Add a method to sync Session parameters to SequencerManager
    public void syncToSequencer() {
        // SequencerManager sequencerManager = SequencerManager.getInstance();
        sequencerManager.updateTimingParameters(getTempoInBPM(), getTicksPerBeat(), getBeatsPerBar());
    }

    // Add setters that automatically sync when modified during playback
    public void setTempoInBPM(float tempoInBPM) {
        this.tempoInBPM = tempoInBPM;

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        this.ticksPerBeat = ticksPerBeat;
        getTickCycler().setLength((long) ticksPerBeat);

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        getBeatCycler().setLength((long) beatsPerBar);

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    /**
     * Updates an existing player in the session or adds it if not present
     * 
     * @param updatedPlayer The player with updated details
     */
    public void updatePlayer(Player updatedPlayer) {
        if (updatedPlayer == null || updatedPlayer.getId() == null) {
            return;
        }

        // Initialize players set if needed
        if (players == null) {
            players = new LinkedHashSet<>();
        }

        // Remove existing player with same ID
        players.removeIf(p -> p.getId().equals(updatedPlayer.getId()));

        // Add the updated player
        updatedPlayer.setSession(this);
        players.add(updatedPlayer);
    }

    /**
     * Handle beat changes - occurs every ticksPerBeat ticks
     */
    private void onBeatChange() {
        logger.debug("onBeatChange() - current beat: {}", getBeatCycler().get());

        // Advance the beat cycler - THIS IS CRITICAL
        getBeatCycler().advance();
        getBeatCounter().advance();

        // Publish standard beat event
        TimingBus.getInstance().publish(Commands.TIME_BEAT, this, getBeatCycler().get());

        // Check if we've completed a bar
        if (getBeat() % getBeatsPerBar() == 0)
            onBarChange();

    }

    /**
     * Handle bar changes - occurs every beatsPerBar beats
     */
    private void onBarChange() {
        logger.debug("onBarChange() - current bar: {}", getBarCycler().get());

        // Advance the bar cycler - ALSO CRITICAL
        getBarCycler().advance();
        getBarCounter().advance();

        // Publish standard bar event
        TimingBus.getInstance().publish(Commands.TIME_BAR, this, getBarCycler().get());

        // Check if we've completed a part
        if (getBar() % getPartLength() == 0)
            onPartChange();
    }

    /**
     * Handle part changes - occurs every bars per part
     */
    public void onPartChange() {
        logger.debug("onPartChange() - current part: {}", getPartCycler().get());

        // Advance the part cycler - CRITICAL TOO
        getPartCycler().advance();
        getPartCounter().advance();

        // Publish standard part event
        TimingBus.getInstance().publish(Commands.TIME_PART, this, getPartCycler().get());
    }

    private void addTickListener() {
        // Add tick listener
        getTickCycler().addListener(new CyclerListener() {
            @Override
            public void advanced(long position) {
                if (getTick() % getTicksPerBeat() == 0)
                    onBeatChange();

                // if (getBeat() % getBeatsPerBar() == 0)
                //     onBarChange();

                // if (getBar() % getPartLength() == 0)
                //     onPartChange();
            }

            @Override
            public void cycleComplete() {

            }

            @Override
            public void starting() {
                // No action needed
            }
        });
    }

    private void removeTickListener() {
        // Remove tick listener
        getTickCycler().getListeners().clear();
    }
}
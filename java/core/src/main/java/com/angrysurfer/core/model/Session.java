package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.SequencerManager;
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

    private CommandBus commandBus = CommandBus.getInstance();
    private TimingBus timingBus = TimingBus.getInstance(); // Add this

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
        if (players == null || id == null) return null;
        
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
        // // logger.debug("getBeat() - current beat: {}", getBeatCycler().get());
        return getBeatCycler().get();
    }

    public Long getBeatCount() {
        // // logger.debug("getBeatCount() - current count: {}",
        // getBeatCounter().get());
        return getBeatCounter().get();
    }

    public Long getTick() {
        // logger.debug("getTick() - current tick: {}", getTickCycler().get());
        return getTickCycler().get();
    }

    public Long getTickCount() {
        // logger.debug("getTickCount() - current count: {}", getTickCounter().get());
        return getTickCounter().get();
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

    /**
     * Make this a complete reset of all state
     */
    public void reset() {
        logger.info("reset() - resetting all cyclers, counters, and player lists");
        
        // Reset all primary cyclers
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();
    
        // Reset all counter cyclers
        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();
    
        // Reset all player management lists
        getAddList().clear();
        getRemoveList().forEach(r -> getPlayers().remove(r));
        getRemoveList().clear();
        
        // Reset granular beat tracking
        setGranularBeat(0.0);
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
     * Initialize all cyclers with appropriate lengths before session playback starts
     */
    public void beforeStart() {
        logger.info("beforeStart() - initializing all cyclers and counters");
        
        // Log current settings
        logger.info("Timing settings: ticksPerBeat={}, beatsPerBar={}, bars={}, parts={}", 
                    ticksPerBeat, beatsPerBar, bars, parts);
        
        // Set primary cycler lengths
        getTickCycler().setLength((long) getTicksPerBeat());
        getBeatCycler().setLength((long) getBeatsPerBar());
        getBarCycler().setLength((long) getBars());
        getPartCycler().setLength((long) getParts());
        
        // Log the configured cycler lengths
        logger.info("Cycler lengths: tick={}, beat={}, bar={}, part={}",
                    getTickCycler().getLength(), getBeatCycler().getLength(),
                    getBarCycler().getLength(), getPartCycler().getLength());
        
        // Also set counter cyclers to max value (they just increment and don't cycle)
        getTickCounter().setLength(Long.MAX_VALUE);
        getBeatCounter().setLength(Long.MAX_VALUE);
        getBarCounter().setLength(Long.MAX_VALUE);
        getPartCounter().setLength(Long.MAX_VALUE);
        
        // Set player cyclers
        getPlayers().forEach(p -> {
            if (p.getSkipCycler() != null) {
                p.getSkipCycler().setLength(p.getSkips());
            }
        });
        
        // Reset all cycler positions to ensure clean start
        getTickCycler().reset();
        getBeatCycler().reset();
        getBarCycler().reset();
        getPartCycler().reset();
        getTickCounter().reset();
        getBeatCounter().reset();
        getBarCounter().reset();
        getPartCounter().reset();
        
        // Notify listeners that we're starting
        TimingBus.getInstance().publish(Commands.SESSION_STARTING, this, null);
    }

    /**
     * Set up listeners for cyclers to publish events
     */
    private void setupCyclerListeners() {
        // Clear any existing listeners to avoid duplicates
        getTickCycler().getListeners().clear();
        getBeatCycler().getListeners().clear();
        getBarCycler().getListeners().clear();
        getPartCycler().getListeners().clear();
        
        // Add tick cycler listener
        getTickCycler().addListener(new CyclerListener() {
            @Override
            public void advanced(long position) {
                // This is handled via onTick() already
            }
            
            @Override
            public void cycleComplete() {
                // This is handled via onBeatChange() already
            }
            
            @Override
            public void starting() {
                // No action needed
            }
        });
        
        // Similar patterns for other cyclers...
    }

    /**
     * Reset all cyclers to default state when stopping
     */
    public void onStop() {
        logger.info("onStop() - resetting all cyclers and counters");
        
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
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error("Error stopping note {} on channel {}: {}",
                            note, p.getChannel(), e.getMessage(), e);
                }
            });
        });
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
        // Query the SequencerManager directly instead of checking CommandBus
        return SequencerManager.getInstance().isRunning();
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
        // Initialize devices - keep this here since it's private
        initializeDevices();
        
        // Let SessionManager handle the actual transport control through CommandBus
        // We don't need to publish the command here - that's handled by UI or other components
    }

    // Change from private to public
    public void initializeDevices() {
        List<MidiDevice> devices = DeviceManager.getMidiOutDevices();

        getPlayers().forEach(p -> {
            initializePlayerDevice(p, devices);
            initializePlayerPreset(p);
            p.setSession(this);
            p.setEnabled(true);
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
                        0);
            }
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Failed to set preset: " + e.getMessage(), e);
        }
    }

    public void stop() {
        // Reset state
        setPaused(false);
        getBeatCycler().reset();
        
        // Disable all players when stopping
        getPlayers().forEach(p -> p.setEnabled(false));

        // Delegate stop command to SequencerManager
        commandBus.publish(Commands.TRANSPORT_STOP, this);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) 
            return;
        
        String cmd = action.getCommand();
        try {
            switch (cmd) {
                // Have Session respond to tick events from SequencerManager
                case Commands.BASIC_TIMING_TICK -> {
                    if (action.getData() instanceof Long tick) {
                        // Forward tick to our handler
                        onTick(tick);
                    }
                }
                
                // Other existing cases...
            }
        } catch (Exception e) {
            logger.error("Error processing command {}: {}", cmd, e.getMessage(), e);
        }
    }

    // Add a method to sync Session parameters to SequencerManager
    public void syncToSequencer() {
        SequencerManager sequencerManager = SequencerManager.getInstance();
        sequencerManager.updateTimingParameters(
                getTempoInBPM(),
                getTicksPerBeat(),
                getBeatsPerBar());
    }

    // Add setters that automatically sync when modified during playback
    public void setTempoInBPM(float tempoInBPM) {
        this.tempoInBPM = tempoInBPM;

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        this.ticksPerBeat = ticksPerBeat;
        getTickCycler().setLength((long) ticksPerBeat);

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;
        getBeatCycler().setLength((long) beatsPerBar);

        // If currently playing, update the sequencer
        if (isRunning()) {
            syncToSequencer();
        }
    }

    /**
     * Updates an existing player in the session or adds it if not present
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
     * Handles the tick processing from the sequencer
     * @param tick current tick position
     */
    public void onTick(Long tick) {
        // First process before tick event
        beforeTick(tick);
        
        // Update granular beat tracking
        granularBeat += 1.0 / getTicksPerBeat();
        
        // Advance the tick cycler - THIS IS THE KEY PART WE WERE MISSING
        getTickCycler().advance();
        getTickCounter().advance();
        
        // Check if we've completed a beat based on our CYCLER
        // This is the critical change - use our cycler's state, not the raw tick value
        if (getTickCycler().atStart()) {
            logger.info("Beat boundary detected at tick {}", tick);
            onBeatChange();
        }
        
        // Process after tick event
        afterTick();
        
        // Publish the actual tick so UI can update
        TimingBus.getInstance().publish(Commands.BASIC_TIMING_TICK, this, getTickCycler().get());
    }

    /**
     * Called before a tick is processed
     */
    private void beforeTick(Long tick) {
        // Publish an event that we're about to process a tick
        TimingBus.getInstance().publish(Commands.BEFORE_TICK, this, tick);
    }

    /**
     * Called after a tick is processed
     */
    private void afterTick() {
        // Any cleanup or notifications after a tick
        TimingBus.getInstance().publish(Commands.AFTER_TICK, this, getTickCycler().get());
    }

    /**
     * Handle beat changes - occurs every ticksPerBeat ticks
     */
    private void onBeatChange() {
        logger.debug("onBeatChange() - current beat: {}", getBeatCycler().get());
        
        // First publish before beat event
        TimingBus.getInstance().publish(Commands.BEFORE_BEAT, this, getBeatCycler().get());
        
        // Advance the beat cycler - THIS IS CRITICAL
        getBeatCycler().advance();
        getBeatCounter().advance();
        
        // Publish standard beat event
        TimingBus.getInstance().publish(Commands.BASIC_TIMING_BEAT, this, getBeatCycler().get());
        
        // Check if we've completed a bar
        if (getBeatCycler().atStart()) {
            onBarChange();
        }
        
        // After processing
        TimingBus.getInstance().publish(Commands.AFTER_BEAT, this, getBeatCycler().get());
    }

    /**
     * Handle bar changes - occurs every beatsPerBar beats
     */
    private void onBarChange() {
        logger.debug("onBarChange() - current bar: {}", getBarCycler().get());
        
        // First publish before bar event
        TimingBus.getInstance().publish(Commands.BEFORE_BAR, this, getBarCycler().get());
        
        // Advance the bar cycler - ALSO CRITICAL
        getBarCycler().advance();
        getBarCounter().advance();
        
        // Publish standard bar event
        TimingBus.getInstance().publish(Commands.BASIC_TIMING_BAR, this, getBarCycler().get());
        
        // Check if we've completed a part
        if (getBarCycler().atStart()) {
            onPartChange();
        }
        
        // After processing
        TimingBus.getInstance().publish(Commands.AFTER_BAR, this, getBarCycler().get());
    }

    /**
     * Handle part changes - occurs every bars per part
     */
    public void onPartChange() {
        logger.debug("onPartChange() - current part: {}", getPartCycler().get());
        
        // First publish before part event
        TimingBus.getInstance().publish(Commands.BEFORE_PART, this, getPartCycler().get());
        
        // Advance the part cycler - CRITICAL TOO
        getPartCycler().advance();
        getPartCounter().advance();
        
        // Publish standard part event
        TimingBus.getInstance().publish(Commands.BASIC_TIMING_PART, this, getPartCycler().get());
        
        // After processing
        TimingBus.getInstance().publish(Commands.AFTER_PART, this, getPartCycler().get());
    }
}
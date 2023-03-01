package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.config.PlayerInfo;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public abstract class Ticker implements Runnable, Serializable {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());
    static boolean initialized = false;
    static Sequencer sequencer;

    static {
        try {
            sequencer = MidiSystem.getSequencer();
            initialized = true;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private final AtomicInteger bar = new AtomicInteger(0);

    private final AtomicLong tick = new AtomicLong(0);
    
    public boolean done = false;
    
    private List<Player> players = new ArrayList<>();

    private ExecutorService executor;
    private int ticksPerBeat = 96;
    private double beat = 0;
    private double granularBeat = 1.0;
    private int beatsPerBar = 4;
    private double beatDivider = 4.0;
    private int tempoInBPM = 120;
    private int partLength = 16;
    private int maxTracks = 128;
    private int songLength = Integer.MAX_VALUE;
    private int swing = 25;
    
    private boolean playing = false;
    
    private boolean stopped = false;
    
    private boolean paused = false;
    private MuteGroupList muteGroups = new MuteGroupList();

    public Ticker(int songLength) {
        setSongLength(songLength);
        setExecutor(Executors.newFixedThreadPool(getMaxTracks()));
    }

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
        setExecutor(Executors.newFixedThreadPool(getMaxTracks()));
    }

    private void reset() {
        setDone(false);
        setPlaying(false);
        setStopped(false);
        synchronized (tick) {
            tick.set(1);
        }
        synchronized (bar) {
            bar.set(1);
        }
        beat = 1;
    }

    private void onTickChange() {
        setBeat(getBeat() + (1.0 / getTicksPerBeat()));
        if (this.beat == getBeatsPerBar()) {
            this.bar.set(this.bar.get() + 1);
            onBarChange(this.bar.get());
        }

        if (getBeat() > getBeatsPerBar()) {
            setBeat(0);
            this.bar.set(this.bar.get() + 1);
        }
    }

    public abstract void onBarChange(int bar);

    public abstract void onBeatChange(long beat);

    @Override
    public void run() {
        reset();
        createMuteGroups();
        setPlaying(true);
        setDone(false);
        setStopped(false);
        if (initialized)
            new Thread(new Runnable() {
                @Override
                public void run() {

                    Sequence sequence = null;
                    try {
                        sequence = new Sequence(Sequence.PPQ, getTicksPerBeat());
                        Track track = sequence.createTrack();
                        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 34, 127), 1000));
                        sequencer.setTempoInBPM(getTempoInBPM());
                        sequencer.setSequence(sequence);
                        sequencer.setLoopCount(100);
                        sequencer.open();
                        sequencer.start();
                    } catch (InvalidMidiDataException | MidiUnavailableException e) {
                        throw new RuntimeException(e);
                    }

                    while (sequencer.isRunning() && !isStopped() && isPlaying()) {
                        if (sequencer.getTickPosition() != getTick())
                            try {
                                tick.set(sequencer.getTickPosition());
                                onTickChange();
                                getExecutor().invokeAll(getPlayers());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }).start();
    }

    private void createMuteGroups() {
    }

    
    
    public synchronized int getBar() {
        return bar.get();
    }

    
    
    public synchronized long getTick() {
        return tick.get();
    }

    
    public double getBeatDivision() {
        return 1.0 / beatDivider;
    }

    public void stop() {
        setPlaying(false);
        setStopped(true);
        reset();
    }

    public void pause() {
        if (isPaused() || isPlaying()) {
            setPlaying(!isPlaying());
            setPaused(!isPaused());
            setStopped(isPaused());
        }
    }

//    public abstract PlayerInfo addPlayer(Player player);


    public void clearEventSources() {
        synchronized (this.getPlayers()) {
            this.getPlayers().clear();
        }
    }
}


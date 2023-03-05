package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public abstract class Ticker implements Runnable, Serializable {
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
    private Long id;
    private List<Player> players = new ArrayList<>();
    private ExecutorService executor;
    private int ticksPerBeat = 24;
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

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
        setExecutor(Executors.newFixedThreadPool(getMaxTracks()));
    }

    public void reset() {
        setPaused(false);
        setDone(false);
        setPlaying(false);
        setStopped(false);
        setId(null);
        setSwing(50);
        setMaxTracks(24);
        setPartLength(64);
        setSongLength(Integer.MAX_VALUE);
        setBeat(1.0);
        setTempoInBPM(120);
        setBeatDivider(4.0);
        setBeatsPerBar(4);
        setTicksPerBeat(24);
        setGranularBeat(1.0);

        synchronized (tick) {
            tick.set(1);
        }
        synchronized (bar) {
            bar.set(1);
        }
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
                    try {
                        Sequence sequence = new Sequence(Sequence.PPQ, getTicksPerBeat());
                        Track track = sequence.createTrack();
                        track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0), 1000));
                        sequencer.setTempoInBPM(getTempoInBPM());
                        sequencer.setSequence(sequence);
                        sequencer.setLoopCount(Integer.MAX_VALUE);
                        sequencer.open();
                        sequencer.start();
                        while (sequencer.isRunning() && !isStopped() && isPlaying()) {
                            while (sequencer.getTickPosition() > getTick()) {
                                tick.incrementAndGet();
                                onTickChange();
                                getExecutor().invokeAll(getPlayers());
                            }
                            Thread.sleep(5);
                        }
                        sequencer.close();
                    } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
                        throw new RuntimeException(e);
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


package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.config.PlayerInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

@Getter
@Setter
public abstract class Ticker implements Runnable, Serializable {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());
    @JsonIgnore
    private final AtomicInteger bar = new AtomicInteger(0);
    @JsonIgnore
    private final AtomicInteger tick = new AtomicInteger(0);
    @JsonIgnore
    public boolean done = false;
    @JsonIgnore
    private List<Player> players = new ArrayList<>();
    @JsonIgnore
    private ExecutorService executor;
    private int beatLengthInTicks = 16; //16384;
    private int beat;
    private int beatsPerBar = 4;
    private double beatDivider = 4.0;
    private int delay = 65;
    private int partLength = 64;
    private int maxTracks = 128;
    private int songLength = 64;
    private int swing = 25;
    private boolean playing = false;
    private boolean stopped = false;

    private boolean paused = false;
    private MuteGroupList muteGroups = new MuteGroupList();
    private List<Player> waitList = new ArrayList<>();

    public Ticker(int songLength) {
        setSongLength(songLength);
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

    private void incrementTick() {
        synchronized (tick) {
            tick.incrementAndGet();
        }

        if (tick.get() % (getBeatLengthInTicks() / getBeatsPerBar()) == 0) {
            if (getBeat() == getBeatsPerBar())
                beat = 1;
            else beat += 1;
            onBeatChange(bar.get());
        }

        if (tick.get() % getBeatLengthInTicks() == 0) {
            synchronized (bar) {
                bar.set(bar.get() + 1);
                if (bar.get() >= getPartLength())
                    bar.set(0);
                onBarChange(bar.get());
            }
        }
    }

    public void onBarChange(int bar) {
    }

    public void onBeatChange(int beat) {

    }

    @Override
    public void run() {
        reset();
        setPlaying(true);
        setDone(false);
        setStopped(false);

        try {
            Sequencer base = MidiSystem.getSequencer();

            Sequence sequence = new Sequence(Sequence.PPQ, getBeatLengthInTicks());
            Track track = sequence.createTrack();
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 34, 127), 1000));
            base.setTempoInBPM(120);
            base.setSequence(sequence);
            base.setLoopCount(100);
            base.open();
            base.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (base.isRunning() && !isStopped() && isPlaying()) {
                        if (base.getTickPosition() > getTick())
                            try {
                                incrementTick();
                                getExecutor().invokeAll(getPlayers());
//                                    Thread.sleep(getDelay());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
//            else if (!isStopped() && isPaused()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }).start();
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }

//        IntStream.range(0, getSongLength()).forEach(i -> {
//            if (!isStopped() && isPlaying())
//                try {
//                    incrementTick();
//                    getExecutor().invokeAll(getPlayers());
//                    Thread.sleep(getDelay());
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            else if (!isStopped() && isPaused()) {
//                try {
//                    Thread.sleep(2500);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });

//        setStopped(true);
//        setPlaying(false);
//        setDone(true);
    }

    @JsonIgnore
    public synchronized int getBar() {
        return bar.get();
    }

    @JsonIgnore
    public synchronized int getTick() {
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

    public abstract PlayerInfo addPlayer(String instrument);


    public void clearEventSources() {
        synchronized (this.getPlayers()) {
            this.getPlayers().clear();
        }
    }

}


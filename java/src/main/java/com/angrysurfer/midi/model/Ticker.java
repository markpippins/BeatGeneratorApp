package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Getter
@Setter
public class Ticker implements Runnable, Serializable {
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());
    static Sequencer sequencer;

    private List<Player> addList = new ArrayList<>();

    private List<Player> removeList = new ArrayList<>();


    static {
        try {
            sequencer = MidiSystem.getSequencer();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private int bar = 1;
    private Long tick = 1L;
    public boolean done = false;
    private Long id;
    private List<Player> players = new ArrayList<>();
    private ExecutorService executor;
    private double beat = 0;
    private double granularBeat = 1.0;
    private int beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private double beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private int partLength = Constants.DEFAULT_PART_LENGTH;
    private int maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private int songLength = Constants.DEFAULT_MAX_TRACKS;
    private int swing = Constants.DEFAULT_SWING;
    private int ticksPerBeat = Constants.DEFAULT_PPQ;

    private boolean paused = false;
    private MuteGroupList muteGroups = new MuteGroupList();

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
        setExecutor(Executors.newFixedThreadPool(getMaxTracks()));
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void reset() {
        setTick(1L);
        setBar(1);
        setBeat(1);
        getPlayers().clear();
        setPaused(false);
        setDone(false);
        setSwing(Constants.DEFAULT_SWING);
        setMaxTracks(Constants.DEFAULT_MAX_TRACKS);
        setPartLength(Constants.DEFAULT_PART_LENGTH);
        setSongLength(Constants.DEFAULT_SONG_LENGTH);
        setTempoInBPM(Constants.DEFAULT_BPM);
        setBeatDivider(Constants.DEFAULT_BEAT_DIVIDER);
        setBeatsPerBar(Constants.DEFAULT_BEATS_PER_BAR);
        setGranularBeat(1.0);
    }

    public float getTempoInBPM() {
        return sequencer.getTempoInBPM();
    }

    public void setTempoInBPM(Float i) {
        sequencer.setTempoInBPM(i);
    }

    private void onTickChange() {

        if (getBeat() == getBeatsPerBar()) {
            setBar(getBar() + 1);
            onBarChange(getBar());
        }

        setBeat(getBeat() + (1.0 / getTicksPerBeat()));

        if (getBeat() > getBeatsPerBar() + 1) {
            setBeat(1);
            // setBar(getBar() + 1);
        }
    }

    public void onBarChange(int bar) {
    
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }
        
        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }    
    }

    // public abstract void onBeatChange(long beat);

    @Override
    public void run() {
        // reset();
        createMuteGroups();
        setDone(false);
        if (Objects.nonNull(sequencer))
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Sequence sequence = new Sequence(Sequence.PPQ, getTicksPerBeat());
                        Track track = sequence.createTrack();
                        IntStream.range(1, 1000).forEach(i ->{
                            try {
                                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0), i * 1000));
                            } catch (InvalidMidiDataException e) {
                                e.printStackTrace();
                            }
                        });
                        sequencer.setSequence(sequence);
                        sequencer.setLoopCount(Integer.MAX_VALUE);
                        sequencer.open();
                        sequencer.start();
                        while (sequencer.isRunning() && !isStopped() && isPlaying()) {
                            if (sequencer.getTickPosition() == getTick() + 1) {
                                onTickChange();
                                getExecutor().invokeAll(getPlayers());
                                setTick(getTick() + 1);
                            }
                            Thread.sleep(10);
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

    public double getBeatDivision() {
        return 1.0 / beatDivider;
    }

    public void stop() {
        setPaused(false);
        setBeat(1);
        setTick(1L);
        setBar(1);
        setDone(false);
    }

    public void pause() {
        if (isPaused() || isPlaying()) {
            setPaused(!isPaused());
        }
    }

    public boolean isPlaying() {
        return sequencer.isRunning();
    }

    public boolean isStopped() {
        return !sequencer.isRunning();
    }

}


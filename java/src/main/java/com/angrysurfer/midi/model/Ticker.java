package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Getter
@Setter
@Entity
public class Ticker implements Runnable, Serializable {

    @Transient
    @JsonIgnore
    static Logger logger = LoggerFactory.getLogger(Ticker.class.getCanonicalName());

    @Transient
    @JsonIgnore
    private Set<Strike> addList = new HashSet<>();

    @Transient
    @JsonIgnore
    private Set<Strike> removeList = new HashSet<>();

    @Transient
    private int bar = 1;

    @Transient
    private Long tick = 1L;

    @Transient
    private boolean done = false;
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    
//    @OneToMany( fetch = FetchType.EAGER)
//	@JoinTable( name = "ticker_player", joinColumns = { @JoinColumn( name = "player_id" ) }, inverseJoinColumns = {
//			@JoinColumn( name = "ticker_id")})
    @Transient
    private Set<Strike> players = new HashSet<>();

    @Transient
    @JsonIgnore
    private ExecutorService executor;

    @Transient
    private double beat = 1;

    @Transient
    private double granularBeat = 1.0;

    private int beatsPerBar = Constants.DEFAULT_BEATS_PER_BAR;
    private double beatDivider = Constants.DEFAULT_BEAT_DIVIDER;
    private int partLength = Constants.DEFAULT_PART_LENGTH;
    private int maxTracks = Constants.DEFAULT_MAX_TRACKS;
    private int songLength = Constants.DEFAULT_MAX_TRACKS;
    private int swing = Constants.DEFAULT_SWING;
    private int ticksPerBeat = Constants.DEFAULT_PPQ;

    @Transient
    @JsonIgnore
    private Sequencer sequencer;

    @Transient
    private boolean paused = false;

    @Transient
    @JsonIgnore
    private MuteGroupList muteGroups = new MuteGroupList();

    public Ticker() {
        setSongLength(Integer.MAX_VALUE);
        setExecutor(Executors.newFixedThreadPool(getMaxTracks()));
    }

    public Strike getPlayer(Long playerId) {
        return getPlayers().stream().filter(p -> playerId == p.getId()).findFirst().orElseThrow();
    }

    public void reset() {
        setId(null);
        setTick(0L);
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
        if (Objects.nonNull(getSequencer()))
            return getSequencer().getTempoInBPM();

        return Float.NEGATIVE_INFINITY;
    }

    public void setTempoInBPM(Float i) {
        getSequencer().setTempoInBPM(i);
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
    
        
    @Override
    public void run() {
        // reset();
        createMuteGroups();
        setDone(false);

        if (Objects.nonNull(getSequencer()))
            new Thread(new Runnable() {
    
                void ensureDevicesOpen() {
                    getPlayers().stream().map(p -> p.getInstrument().getDevice()).distinct().forEach(device -> { 
                        if (!device.isOpen())
                            try {
                                device.open();
                            } catch (MidiUnavailableException e) {
                                logger.error(e.getMessage(), e);
                            }} 
                        );
                }

                Sequence getMasterSequence() throws InvalidMidiDataException {
                    Sequence sequence = null;

                        sequence = new Sequence(Sequence.PPQ, getTicksPerBeat());
                        Track track = sequence.createTrack();
                        IntStream.range(0, 100).forEach(i -> {
                            try {
                                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0), i * 1000));
                            } catch (InvalidMidiDataException e) {
                                logger.error(e.getMessage(), e);
                            }
                        });

                    return sequence;
                }
            
                void beforeStart() throws InvalidMidiDataException, MidiUnavailableException {
                    setTick(1L);
                    Sequence master = getMasterSequence();
                    getSequencer().setSequence(master);
                    getSequencer().setLoopCount(Integer.MAX_VALUE);
                    getSequencer().open();
                }

                void afterEnd() {
                    getSequencer().close();
                    setTick(1L);
                    setBar(1);
                    setBeat(1.0);
                }

                @Override
                public void run() {

                    ensureDevicesOpen();
    
                    try {
                        beforeStart();
                        getSequencer().start();
                        while (getSequencer().isRunning()) {
                            if (getSequencer().getTickPosition() > getTick()) {

                                if (getBeat() >= getBeatsPerBar() + 1) {
                                    setBeat(1.0);
                                    setBar(getBar() + 1);
                                    onBarChange(getBar());
                                }

                                getExecutor().invokeAll(getPlayers());

                                setBeat(getBeat() == getBeatsPerBar() ? 1 : getBeat() + (1.0 / getTicksPerBeat()));
                                setTick(getTick() + 1);
                            }
                            Thread.sleep(5);
                        }

                        afterEnd();
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
        if (Objects.nonNull(getSequencer()) && getSequencer().isRunning())
            getSequencer().stop();

        setPaused(false);
        setBeat(1);
        setTick(1L);
        setBar(1);
        setDone(false);
    }

    public void pause() {
        if (isPaused() || isPlaying())
            setPaused(!isPaused());
    }

    public boolean isPlaying() {
        return Objects.nonNull(getSequencer()) ? getSequencer().isRunning() : false;
    }

}


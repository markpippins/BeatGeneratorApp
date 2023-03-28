package com.angrysurfer.midi.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.service.MIDIService;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SequenceRunner implements Runnable {

    static Logger logger = LoggerFactory.getLogger(SequenceRunner.class.getCanonicalName());

    private ExecutorService executor; 

    static Sequencer sequencer;

    static boolean initialized;

    static Exception exception;

    static {
        try {
            sequencer = MidiSystem.getSequencer();
            initialized = true;
        } catch (MidiUnavailableException e) {
            initialized = false;
            exception = e;
            throw new RuntimeException(e);
        }
    }
    
    /**
     *
     */
    private final Ticker ticker;

    private boolean stopped = false;

    private int delay;

    private Set<CyclerListener> cycleListeners = new HashSet<>();
    /**
     * @param ticker
     */
    public SequenceRunner(Ticker ticker) {
        this.ticker = ticker;
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
    }

    public void ensureDevicesOpen() {
        getTicker().getPlayers().stream().map(p -> p.getInstrument().getDevice()).filter(d -> !d.isOpen()).distinct().forEach(d -> MIDIService.select(d));
    }

    public Sequence getMasterSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, getTicker().getTicksPerBeat());        
        Track track = sequence.createTrack();
        IntStream.range(0, 10).forEach(i -> {
            try {
                track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0), i * 1000));
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }); 
        return sequence;
    }

    public void beforeStart() throws InvalidMidiDataException, MidiUnavailableException {
        Sequence master = getMasterSequence();
        sequencer.setSequence(master);
        sequencer.setLoopCount(getTicker().getLoopCount());
        sequencer.setTempoInBPM(getTicker().getTempoInBPM());
        sequencer.open();
        getTicker().beforeStart();
    }

    public void afterEnd() {
        sequencer.close();
        getTicker().afterEnd();
        stopped = false;
    }

    @Override
    public void run() {

        boolean started = false;

        MIDIService.reset();
        ensureDevicesOpen();

        try {
            beforeStart();
            
            sequencer.start();
            while (sequencer.isRunning() && !isStopped()) {
                float delay = 60000 / getTicker().getTempoInBPM() / getTicker().getTicksPerBeat();
                getTicker().beforeTick();
                while (sequencer.getTickPosition() < getTicker().getTick() + 1)
                    Thread.sleep(1); 

                if (!started)                    
                    started = handleStarted();

                this.executor.invokeAll(getTicker().getPlayers());
                Thread.sleep((long) (delay * .8)); 
                getTicker().afterTick();
            }

            afterEnd();
        } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean handleStarted() {
        getTicker().onStart();
        getCycleListeners().forEach(c -> c.starting());
        return true;
    }

    public Ticker stop() {
        setStopped(true);
        if (Objects.nonNull(sequencer) && sequencer.isRunning())
            sequencer.stop();
        getTicker().setPaused(false);
        getTicker().getBeatCycler().reset();
        getTicker().getBarCycler().reset();
        getTicker().setDone(false);
        getTicker().reset();
        return getTicker();
    }

    public void pause() {
        if (getTicker().isPaused() || isPlaying())
            getTicker().setPaused(!getTicker().isPaused());
    }

    public boolean isPlaying() {
        return Objects.nonNull(sequencer) ? sequencer.isRunning() : false;
    }

    public Sequencer getSequencer() {
        return sequencer;
    }

}
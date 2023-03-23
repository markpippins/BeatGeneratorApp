package com.angrysurfer.midi.util;

import java.util.Objects;
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

    /**
     * @param ticker
     */
    public SequenceRunner(Ticker ticker) {
        this.ticker = ticker;
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
    }

    public void ensureDevicesOpen() {

        this.ticker.getPlayers().stream().map(p -> p.getInstrument().getDevice()).distinct().forEach(d -> {
            MIDIService.select(d);
        });

        // this.ticker.getPlayers().stream().map(p -> p.getInstrument().getDevice()).distinct().forEach(device -> { 
        //     if (!device.isOpen())
        //         try {
        //             device.open();
        //         } catch (MidiUnavailableException e) {
        //             logger.error(e.getMessage(), e);
        //         }} 
        //     );
    }

    public Sequence getMasterSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, this.ticker.getTicksPerBeat());        
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
        sequencer.setLoopCount(this.ticker.getLoopCount());
        sequencer.setTempoInBPM(this.ticker.getTempoInBPM());
        
        sequencer.open();
        this.ticker.beforeStart();
    }

    public void afterEnd() {
        sequencer.close();
        this.ticker.afterEnd();
    }

    @Override
    public void run() {

        ensureDevicesOpen();

        try {
            beforeStart();
            sequencer.start();
            while (sequencer.isRunning() && sequencer.isRunning()) {
                if (sequencer.getTickPosition() > this.ticker.getTick()) {
                    this.ticker.beforeTick();
                    this.executor.invokeAll(this.ticker.getPlayers());
                    this.ticker.afterTick();
                }
                Thread.sleep(5);
            }

            afterEnd();
        } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Ticker stop() {
        if (Objects.nonNull(sequencer) && sequencer.isRunning())
            sequencer.stop();

        this.ticker.setPaused(false);
        this.ticker.getBeatCycler().reset();
        this.ticker.getBarCycler().reset();
        this.ticker.setDone(false);
        this.ticker.reset();
        return this.ticker;
    }

    public void pause() {
        if (this.ticker.isPaused() || isPlaying())
            this.ticker.setPaused(!this.ticker.isPaused());
    }

    public boolean isPlaying() {
        return Objects.nonNull(sequencer) ? sequencer.isRunning() : false;
    }

    public Sequencer getSequencer() {
        return sequencer;
    }

}
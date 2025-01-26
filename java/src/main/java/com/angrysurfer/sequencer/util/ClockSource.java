package com.angrysurfer.sequencer.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.sequencer.model.Ticker;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.util.listener.CyclerListener;
import com.angrysurfer.sequencer.util.listener.ClockListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockSource implements Runnable { //, Receiver {

    static Logger logger = LoggerFactory.getLogger(ClockSource.class.getCanonicalName());

    private ExecutorService executor;

    static Sequencer sequencer;

    static boolean initialized;

    static Stack<Exception> exceptions = new Stack<>();

    private final Ticker ticker;

    private Boolean stopped = false;

    private static final AtomicBoolean playing = new AtomicBoolean(false);

    private List<ClockListener> listeners = new ArrayList<>();

    static {
        try {
            sequencer = MidiSystem.getSequencer();
            initialized = true;
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage(), e);
            exceptions.push(e);
            initialized = false;
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */

    private Set<CyclerListener> cycleListeners = new HashSet<>();

    /**
     * @param ticker
     */
    public ClockSource(Ticker ticker) {
        this.ticker = ticker;
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
    }

    public void ensureDevicesOpen() {
        getTicker().getPlayers().stream().map(p -> p.getInstrument().getDevice()).filter(d -> !d.isOpen()).distinct()
                .forEach(d -> MIDIService.select(d));
    }

    public Sequence getMasterSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, getTicker().getTicksPerBeat());
        sequence.createTrack().add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0),
                ticker.getTicksPerBeat() * ticker.getBeatsPerBar() * 4 * 1000));
        return sequence;
    }

    public void beforeStart() throws InvalidMidiDataException, MidiUnavailableException {
        Sequence master = getMasterSequence();
        sequencer.setSequence(master);
        sequencer.setLoopCount(getTicker().getLoopCount());
        sequencer.setTempoInBPM(getTicker().getTempoInBPM());
        sequencer.open();
        // sequencer.getTransmitter().setReceiver(this);
        getTicker().beforeStart();
    }

    public void afterEnd() {
        // sequencer.getReceivers().remove(this);
        getListeners().forEach(l -> l.onEnd());
        sequencer.close();
        getTicker().afterEnd();
        stopped = false;
    }

    @Override
    public void run() {
        MIDIService.reset();

        try {
            beforeStart();

            // Initialize PreciseTimer with ticker's parameters
            PreciseTimer timer = new PreciseTimer(
                    Math.round(ticker.getTempoInBPM()),
                    ticker.getTicksPerBeat());

            // Add tick listener to handle each tick
            timer.addTickListener(() -> {
                
                try {
                    if (!playing.get()) {
                        playing.set(handleStarted());
                    }

                    ticker.beforeTick();
                    getListeners().forEach(l -> l.onTick());
                    this.executor.invokeAll(ticker.getPlayers());
                    ticker.afterTick();

                    logger.info("Tick: {}", ticker.getTick());
                } catch (InterruptedException e) {
                    logger.error("Error in tick processing", e);
                }
            });

            sequencer.start();

            // Start the timer in a new thread
            Thread timerThread = new Thread(timer);
            timerThread.setPriority(Thread.MAX_PRIORITY);
            timerThread.start();

            // Wait while running
            while (sequencer.isRunning() && !stopped) {
                Thread.sleep(10);
            }

            // Cleanup
            timer.stop();
            timerThread.join();
            afterEnd();

        } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
            stop();
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

    double getDelay() {
        return 60000 / ticker.getTempoInBPM() / ticker.getTicksPerBeat();
    }

    double getDutyCycle() {
        return 0.5;
    }

    // public Sequencer getSequencer() {
    //     return sequencer;
    // }

        // private TempoCache tempoCache;

    // @Override
    // public void send(MidiMessage message, long timeStamp) {
    //     logger.info(com.angrysurfer.beatgenerator.model.midi.MidiMessage.lookupCommand(message.getStatus()));
    //     long tickPos = 0;
    //     if (tempoCache == null) {
    //         try {
    //             tempoCache = new TempoCache(getMasterSequence());
    //         } catch (InvalidMidiDataException e) {
    //             logger.error(e.getMessage(), e);
    //         }
    //     }
    //     // convert timeStamp to ticks
    //     if (timeStamp < 0) {
    //         tickPos = ticker.getTick();
    //     } else {
    //         synchronized (tempoCache) {
    //             try {
    //                 tickPos = MidiUtils.microsecond2tick(getMasterSequence(), timeStamp, tempoCache);
    //             } catch (InvalidMidiDataException e) {
    //                 logger.error(e.getMessage(), e);
    //             }
    //         }
    //     }
    // }

    // @Override
    // public void close() {
    //     getSequencer().getReceivers().remove(this);
    // }
}
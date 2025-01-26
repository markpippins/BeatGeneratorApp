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
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.sequencer.exception.MidiDeviceException;
import com.angrysurfer.sequencer.model.Ticker;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.util.listener.CyclerListener;
import com.angrysurfer.sequencer.util.listener.TickListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockSourceGen implements Runnable { // , Receiver {

    static Logger logger = LoggerFactory.getLogger(ClockSourceGen.class.getCanonicalName());

    private ExecutorService executor;

    static boolean initialized;

    static Stack<Exception> exceptions = new Stack<>();

    private final Ticker ticker;

    private Boolean stopped = false;

    private List<TickListener> playerWrappers = new ArrayList<>();

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private PreciseTimer timer;


    /**
     *
     */

    private Set<CyclerListener> cycleListeners = new HashSet<>();

    /**
     * @param ticker
     */
    public ClockSourceGen(Ticker ticker) {
        this.ticker = ticker;
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
    }

    //
    public Set<CyclerListener> getCycleListeners() {
        return cycleListeners;
    }

    public void ensureDevicesOpen() throws MidiDeviceException {
        MidiDeviceException[] errors = new MidiDeviceException[1];
        ticker.getPlayers().stream().map(p -> p.getInstrument().getDevice()).filter(d -> !d.isOpen()).distinct()
                .forEach(d -> {

                    try {
                        MIDIService.select(d);
                    } catch (MidiDeviceException e) {
                        errors[0] = e;
                    }
                });

        if (Objects.nonNull(errors[0]))
            throw errors[0];
    }

    public Sequence getMasterSequence() throws InvalidMidiDataException {
        InvalidMidiDataException[] errors = new InvalidMidiDataException[1];
        Sequence sequence = new Sequence(Sequence.PPQ, ticker.getTicksPerBeat());
        Track track = sequence.createTrack();

        try {
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0),
                    ticker.getTicksPerBeat() * ticker.getBeatsPerBar() * 4));
        } catch (InvalidMidiDataException e) {
            errors[0] = e;
        }

        if (Objects.nonNull(errors[0]))
            throw errors[0];

        return sequence;
    }

    public void beforeStart() throws InvalidMidiDataException, MidiUnavailableException {

        // ensureDevicesOpen();

        stopped = false;
        ticker.beforeStart();
    }

    public void afterEnd() {
        // sequencer.getReceivers().remove(this);
        playerWrappers.forEach(l -> {
            l.onEnd();
        });
        
        ticker.afterEnd();
        stopped = false;
    }

    double getDelay() {
        return 60000 / ticker.getTempoInBPM() / ticker.getTicksPerBeat();
    }

    double getDutyCycle() {
        return 0.5;
    }

    // public Sequence generateSequence() {

    // Sequence sequence = null;

    // try {
    // beforeStart();

    // for (int i = 0; i < ticker.getTicksPerBeat() * ticker.getBeatsPerBar() * 4;
    // i++) {

    // ticker.beforeTick();
    // playerWrappers.forEach(l -> l.onTick());
    // this.executor.invokeAll(ticker.getPlayers());
    // ticker.afterTick();
    // }

    // afterEnd();
    // } catch (InvalidMidiDataException | MidiUnavailableException |
    // InterruptedException e) {
    // throw new RuntimeException(e);
    // }

    // return sequence;
    // }

    @Override
    public void run() {
        MIDIService.reset();

        try {
            beforeStart();
            
            // Initialize PreciseTimer with ticker's parameters
            timer = new PreciseTimer(
                Math.round(ticker.getTempoInBPM()), 
                ticker.getTicksPerBeat()
            );
            
            // Add tick listener to handle each tick
            timer.addTickListener(() -> {
                try {
                    if (!running.get()) {
                        running.set(handleStarted());
                    }
                    
                    ticker.beforeTick();
                    playerWrappers.forEach(l -> l.onTick());
                    this.executor.invokeAll(ticker.getPlayers());
                    ticker.afterTick();
                    
                    Thread.sleep((long) (getDelay() * getDutyCycle()));
                } catch (InterruptedException e) {
                    logger.error("Error in tick processing", e);
                }
            });

            // Start the timer in a new thread
            Thread timerThread = new Thread(timer);
            timerThread.setPriority(Thread.MAX_PRIORITY);
            timerThread.start();

            // Wait while running
            while (running.get() && !stopped) {
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
        // initialize the cycler too much?
        ticker.onStart();
        cycleListeners.forEach(c -> c.starting());
        return true;
    }

    public Ticker stop() {
        setStopped(true);
        ticker.setPaused(false);
        ticker.getBeatCycler().reset();
        ticker.getBarCycler().reset();
        ticker.setDone(false);
        ticker.reset();
        return ticker;
    }

    public void pause() {
        if (ticker.isPaused() || isRunning())
            ticker.setPaused(!ticker.isPaused());
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

    public boolean isRunning() {
        return running.get();
    }
}
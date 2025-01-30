package com.angrysurfer.sequencer.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.sequencer.model.Ticker;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.util.listener.ClockListener;
import com.angrysurfer.sequencer.util.listener.CyclerListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockSource implements Runnable {

    static Logger logger = LoggerFactory.getLogger(ClockSource.class.getCanonicalName());

    private ExecutorService executor;

    static Stack<Exception> exceptions = new Stack<>();

    private final Ticker ticker;

    private Boolean stopped = false;

    private static final AtomicBoolean playing = new AtomicBoolean(false);

    private List<ClockListener> listeners = new ArrayList<>();

    private Set<CyclerListener> cycleListeners = new HashSet<>();

    private PreciseTimer timer;

    /**
     * @param ticker
     */
    public ClockSource(Ticker ticker) {
        this.ticker = ticker;
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
        timer = new PreciseTimer(
                Math.round(ticker.getTempoInBPM()),
                ticker.getTicksPerBeat());
    }

    public void afterEnd() {
        getListeners().forEach(l -> l.onEnd());
        getTicker().afterEnd();
        stopped = false;
    }

    @Override
    public void run() {
        MIDIService.reset();

        try {
            getTicker().beforeStart();

            // Initialize PreciseTimer with ticker's parameters

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

                    // logger.info("Tick: {}", ticker.getTick());
                } catch (InterruptedException e) {
                    logger.error("Error in tick processing", e);
                }
            });


            // Start the timer in a new thread
            Thread timerThread = new Thread(timer);
            timerThread.setPriority(Thread.MAX_PRIORITY);
            timerThread.start();

            // Wait while running
            while (!stopped) {
                Thread.sleep(10);
            }

            playing.set(false);
            
            // Cleanup
            timer.stop();
            timerThread.join();
            afterEnd();

        } catch (InterruptedException e) {
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

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
        return playing.get();
    }

    double getDelay() {
        return 60000 / ticker.getTempoInBPM() / ticker.getTicksPerBeat();
    }

    double getDutyCycle() {
        return 0.5;
    }

    public void setTempoInBPM(long updateValue) {
        timer.setBpm((int) updateValue);
    }
}
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

    private PreciseTimer timer = new PreciseTimer(130, 24);;

    private static final AtomicBoolean playing = new AtomicBoolean(false);

    private List<ClockListener> listeners = new ArrayList<>();

    private Set<CyclerListener> cycleListeners = new HashSet<>();

    /**
     * @param ticker
     */
    public ClockSource(Ticker ticker) {
        this.ticker = ticker;
        timer = new PreciseTimer(
                Math.round(ticker.getTempoInBPM()),
                ticker.getTicksPerBeat());
        executor = Executors.newFixedThreadPool(ticker.getMaxTracks());
    }

    public void afterEnd() {
        getListeners().forEach(l -> l.onEnd());
        getTicker().afterEnd();
        playing.set(false);
    }

    @Override
    public void run() {
        MIDIService.reset();

        try {
            getTicker().beforeStart();
            
            // Add tick listener to handle each tick
            timer.addTickListener(() -> {

                try {
                    if (!playing.get())
                        playing.set(handleStarted());

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
            Thread.sleep(10);
            while (playing.get()) {
                Thread.sleep(10);
            }

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

    public void stop() {
        playing.set(false);
    }

    public void pause() {
        if (getTicker().isPaused() || isPlaying())
            getTicker().setPaused(!getTicker().isPaused());
    }

    public boolean isPlaying() {
        return playing.get();
    }

    double getDutyCycle() {
        return 0.5;
    }

    public void setTempoInBPM(long updateValue) {
        if (Objects.nonNull(timer))
            timer.setBpm((int) updateValue);

    }

    public void setppq(long updateValue) {
        if (Objects.nonNull(timer))
            timer.setPpq((int) updateValue);
    }
}
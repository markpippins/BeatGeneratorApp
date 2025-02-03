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

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private List<ClockListener> listeners = new ArrayList<>();

    private Set<CyclerListener> cycleListeners = new HashSet<>();

    private Timer timer;

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

    public void onTick() {

        ticker.beforeTick();
        getListeners().forEach(l -> l.onTick());

        try {
            this.executor.invokeAll(ticker.getPlayers());
        } catch (InterruptedException e) {
            logger.error("Error in tick processing", e);
        }

        ticker.afterTick();
    }

    @Override
    public void run() {
        MIDIService.reset();

        try {
            getTicker().beforeStart();

            getTimer().addTickListener(() -> {
                if (!running.get()) {
                    running.set(handleStarted());
                }
                onTick();
            });

            // Start the timer in a new thread
            Thread timerThread = new Thread(timer);
            timerThread.setPriority(Thread.MAX_PRIORITY);
            timerThread.start();

            // Wait while running
            while (!stopped)
                Thread.sleep(10);

            running.set(false);

            // Cleanup
            getTimer().stop();
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
    }

    public void pause() {
        if (!getTicker().isPaused() || !isRunning())
            return;

        timer.stop();
        getTicker().setPaused(!getTicker().isPaused());
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setTempoInBPM(long updateValue) {
        timer.setBpm((int) updateValue);
    }

    // double getDelay() {
    // return 60000 / ticker.getTempoInBPM() / ticker.getTicksPerBeat();
    // }

    // double getDutyCycle() {
    // return 0.5;
    // }
}
package com.angrysurfer.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Ticker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockSource implements Runnable {

    static Logger logger = LoggerFactory.getLogger(ClockSource.class.getCanonicalName());

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

        timer = new PreciseTimer(
                Math.round(ticker.getTempoInBPM()),
                ticker.getTicksPerBeat());

        // Register single callbacks for tick and beat events
        timer.addTickCallback(this::handleTick);
        timer.addBeatCallback(this::handleBeat);
    }

    public void afterEnd() {
        getListeners().forEach(l -> l.onEnd());
        getTicker().afterEnd();
        stopped = false;
    }

    // public void onTick() {
    // logger.debug("onTick() - ticker: {}", ticker.getId());
    // ticker.beforeTick();
    // getListeners().forEach(l -> l.onTick());
    // ticker.afterTick();
    // }

    private void handleTick() {
        logger.debug("handleTick() - ticker: {}", ticker.getId());
        ticker.beforeTick();
        CommandBus.getInstance().publish(Commands.BASIC_TIMING_TICK, this);
        ticker.afterTick();
    }

    private void handleBeat() {
        logger.debug("handleBeat() - ticker: {}", ticker.getId());
        CommandBus.getInstance().publish(Commands.BASIC_TIMING_BEAT, this);
    }

    @Override
    public void run() {
        try {
            running.set(true);
            handleStarted();

            // Start the timer in high-priority thread
            Thread timerThread = new Thread(timer, "PreciseTimer-Main");
            timerThread.setPriority(Thread.MAX_PRIORITY);
            timerThread.start();

            while (!stopped) {
                Thread.sleep(1);
            }

            running.set(false);
            cleanup();
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
        timer.stop();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while stopping", e);
        }

        // getTicker().setPaused(false);
        // getTicker().getBeatCycler().reset();
        // getTicker().getBarCycler().reset();
        // getTicker().setDone(false);
        getTicker().reset();
    }

    public void pause() {
        // Only pause if we're currently running and not already paused
        if (isRunning()) // && !getTicker().isPaused()) {
            timer.stop();
        // getTicker().setPaused(true);
        // }
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

    private void cleanup() {
        getTimer().stop();
        // tickExecutor.shutdown();
        // beatExecutor.shutdown();
        // executor.shutdown();

        // try {
        // tickExecutor.awaitTermination(1, TimeUnit.SECONDS);
        // beatExecutor.awaitTermination(1, TimeUnit.SECONDS);
        // executor.awaitTermination(1, TimeUnit.SECONDS);
        // } catch (InterruptedException e) {
        // logger.error("Error shutting down executors", e);
        // Thread.currentThread().interrupt();
        // }
    }
}
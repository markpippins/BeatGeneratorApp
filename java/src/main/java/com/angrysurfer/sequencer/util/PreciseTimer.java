package com.angrysurfer.sequencer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PreciseTimer implements Runnable {
    private final int bpm;
    private final int ppq; // pulses per quarter note
    private final AtomicLong ticks = new AtomicLong(0);
    private volatile boolean running = true;
    private final List<Runnable> tickListeners = new ArrayList<>();
    private final List<Runnable> beatListeners = new ArrayList<>();

    public PreciseTimer(int bpm, int ppq) {
        this.bpm = bpm;
        this.ppq = ppq;
    }

    public void addTickListener(Runnable listener) {
        tickListeners.add(listener);
    }

    public void addBeatListener(Runnable listener) {
        beatListeners.add(listener);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        long intervalNanos = (long) ((60.0 / bpm / ppq) * 1_000_000_000);
        long nextTick = System.nanoTime();

        while (running) {
            long currentTime = System.nanoTime();
            if (currentTime >= nextTick) {
                long tickNum = ticks.incrementAndGet();
                
                // Notify tick listeners
                tickListeners.forEach(Runnable::run);
                
                // If we've reached a beat (ppq ticks), notify beat listeners
                if (tickNum % ppq == 0) {
                    beatListeners.forEach(Runnable::run);
                }
                
                nextTick += intervalNanos;
            }
            
            // Small yield to prevent busy-waiting
            Thread.yield();
        }
    }

    public long getCurrentTick() {
        return ticks.get();
    }
}
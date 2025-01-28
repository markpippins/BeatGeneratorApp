package com.angrysurfer.sequencer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PreciseTimer implements Runnable {
    private volatile int bpm;
    private volatile int ppq;
    private final AtomicInteger pendingBpm = new AtomicInteger(-1);
    private final AtomicInteger pendingPpq = new AtomicInteger(-1);
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

    public synchronized void setBpm(int newBpm) {
        pendingBpm.set(newBpm);
    }

    public synchronized void setPpq(int newPpq) {
        pendingPpq.set(newPpq);
    }

    public int getBpm() {
        return bpm;
    }

    public int getPpq() {
        return ppq;
    }

    @Override
    public void run() {
        long intervalNanos = (long) ((60.0 / bpm / ppq) * 1_000_000_000);
        long nextTick = System.nanoTime();

        while (running) {
            long currentTime = System.nanoTime();
            if (currentTime >= nextTick) {
                long tickNum = ticks.incrementAndGet();
                
                // Check for pending changes at beat boundaries
                if (tickNum % ppq == 0) {
                    int newBpm = pendingBpm.get();
                    if (newBpm > 0) {
                        bpm = newBpm;
                        intervalNanos = (long) ((60.0 / bpm / ppq) * 1_000_000_000);
                        pendingBpm.set(-1);
                    }
                    
                    int newPpq = pendingPpq.get();
                    if (newPpq > 0) {
                        ppq = newPpq;
                        intervalNanos = (long) ((60.0 / bpm / ppq) * 1_000_000_000);
                        pendingPpq.set(-1);
                    }
                    
                    beatListeners.forEach(Runnable::run);
                }
                
                tickListeners.forEach(Runnable::run);
                nextTick += intervalNanos;
            }
            
            Thread.yield();
        }
    }

    public long getCurrentTick() {
        return ticks.get();
    }
}
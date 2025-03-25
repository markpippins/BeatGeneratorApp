package com.angrysurfer.core.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.util.Constants;

public class LowLatencyMidiClock {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "MIDI-Clock");
            t.setPriority(Thread.MAX_PRIORITY);
            t.setDaemon(true);
            return t;
        }
    );
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Session session;
    private ScheduledFuture<?> clockTask;
    
    public LowLatencyMidiClock(Session session) {
        this.session = session;
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Calculate tick interval in nanoseconds for more precision
            // long intervalNanos = (long)(60_000_000_000L / 
            //     (session.getTempoInBPM() * session.getTicksPerBeat()));
            long intervalNanos = (long)(60_000_000_000L / 
                (session.getTempoInBPM() * Constants.DEFAULT_PPQ));
            
            clockTask = scheduler.scheduleAtFixedRate(
                this::tick, 
                0, 
                intervalNanos, 
                TimeUnit.NANOSECONDS
            );
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false) && clockTask != null) {
            clockTask.cancel(false);
        }
    }
    
    private void tick() {
        try {
            if (session != null) {
                session.onTick();
            }
        } catch (Exception e) {
            // Catch exceptions to prevent scheduler from stopping
            e.printStackTrace();
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
}
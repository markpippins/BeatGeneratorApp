package com.angrysurfer.core.api;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimingBus extends AbstractBus {
    private static TimingBus instance;

    // Use a lock-free concurrent structure for timing events
    private final ConcurrentLinkedQueue<IBusListener> timingListeners = new ConcurrentLinkedQueue<>();
    
    // Use a dedicated thread with high priority for timing events
    private final ExecutorService timingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TimingBus-Thread");
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    private TimingBus() {
        super();
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

    @Override
    public void publish(String command, Object source, Object data) {
        // Fast path - minimal processing for timing events
        for (IBusListener listener : timingListeners) {
            // Submit to executor to avoid blocking the MIDI thread
            timingExecutor.submit(() -> listener.onAction(new Command(command, source, data)));
        }
    }

    // Other methods...
}

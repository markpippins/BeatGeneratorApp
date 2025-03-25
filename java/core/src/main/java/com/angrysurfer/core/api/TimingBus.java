package com.angrysurfer.core.api;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimingBus extends AbstractBus {
    private static TimingBus instance;

    // Initialize field BEFORE constructor is called
    private final ConcurrentLinkedQueue<IBusListener> timingListeners = new ConcurrentLinkedQueue<>();
    
    private final ExecutorService timingExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TimingBus-Thread");
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    // Constructor must be after field initialization
    private TimingBus() {
        // We'll handle registration ourselves instead of relying on parent
        // Don't call super() which calls register() before fields are initialized
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
            try {
                // Use direct call for minimal latency
                listener.onAction(new Command(command, source, data));
            } catch (Exception e) {
                System.err.println("Error publishing timing event: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void register(IBusListener listener) {
        if (listener != null) {
            if (timingListeners == null) {
                System.err.println("TimingBus: timingListeners is null!");
                return;
            }
            
            if (!timingListeners.contains(listener)) {
                timingListeners.add(listener);
                System.out.println("TimingBus: Registered listener: " + 
                    (listener.getClass() != null ? listener.getClass().getSimpleName() : "null"));
            }
        }
    }
    
    @Override
    public void unregister(IBusListener listener) {
        if (listener != null && timingListeners != null) {
            timingListeners.remove(listener);
            System.out.println("TimingBus: Unregistered listener: " + 
                (listener.getClass() != null ? listener.getClass().getSimpleName() : "null"));
        }
    }
}

package com.angrysurfer.core.api;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;

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

    // Add this field for reusing the same Command object
    private final Command sharedCommand = new Command(null, null, null);

    // Diagnostic counter for timing events
    private int eventCount = 0;

    // Constructor must be after field initialization
    private TimingBus() {
        // We'll handle registration ourselves instead of relying on parent
        // Don't call super() which calls register() before fields are initialized

        // Add diagnostic message
        System.out.println("TimingBus initialized with " + timingListeners.size() + " listeners");

        // Start a diagnostic thread to monitor timing events
        new Thread(() -> {
            long lastReport = System.currentTimeMillis();
            int eventCount = 0;

            while (true) {
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                    long now = System.currentTimeMillis();
                    System.out.println("TimingBus health: " + eventCount + " events in " +
                                       ((now - lastReport)/1000) + " seconds");
                    lastReport = now;
                    eventCount = 0;
                } catch (Exception e) {
                    // Ignore
                }
            }
        }, "TimingBus-Monitor").start();
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

    @Override
    public void publish(String commandName, Object source, Object data) {
        // Add immediate diagnostic output
        System.out.println("TimingBus: Publishing " + commandName + ", listeners: " + timingListeners.size());
        
        if (Commands.TIMING_UPDATE.equals(commandName)) {
            // DON'T reuse the shared command for timing - create a new one for thread safety
            Command cmd = new Command(commandName, source, data);
            
            // Fast path for timing updates
            for (IBusListener listener : timingListeners) {
                try {
                    // Simple direct call to onAction
                    listener.onAction(cmd);
                } catch (Exception e) {
                    // Log exceptions but continue with other listeners
                    System.err.println("Error in timing listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Increment diagnostic counter
            eventCount++;
            return;
        }
        
        // For non-timing events, use parent implementation
        super.publish(commandName, source, data);
    }

    // Add a method to check registration
    public boolean isRegistered(IBusListener listener) {
        return timingListeners.contains(listener);
    }

    // Add method to update shared command without creating new objects
    private void updateSharedCommand(String commandName, Object source, Object data) {
        sharedCommand.setCommand(commandName);
        sharedCommand.setSender(source);
        sharedCommand.setData(data);
    }

    // Add a specialized method for highest performance timing events
    public void publishTimingUpdate(TimingUpdate update) {
        // Even more optimized path for timing updates
        Command command = new Command(Commands.TIMING_UPDATE, this, update);
        for (IBusListener listener : timingListeners) {
            try {
                ((Player)listener).onTick(update);  // Direct call to avoid command overhead
            } catch (ClassCastException e) {
                // Fall back to standard method if not a Player
                listener.onAction(command);
            } catch (Exception e) {
                // Minimal error handling
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
                // System.out.println("TimingBus: Registered listener: " + (listener.getClass()
                // != null ? listener.getClass().getSimpleName() : "null"));
            }
        }
    }

    @Override
    public void unregister(IBusListener listener) {
        if (listener != null && timingListeners != null) {
            timingListeners.remove(listener);
            // System.out.println("TimingBus: Unregistered listener: " +
            // (listener.getClass() != null ? listener.getClass().getSimpleName() :
            // "null"));
        }
    }
}

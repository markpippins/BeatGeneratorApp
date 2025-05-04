package com.angrysurfer.core.api;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimingBus extends AbstractBus {
    private static final Logger logger = LoggerFactory.getLogger(TimingBus.class);
    
    // Use a holder pattern for thread-safe lazy initialization
    private static class InstanceHolder {
        private static final TimingBus INSTANCE = new TimingBus();
        
        static {
            System.out.println("TimingBus InstanceHolder initialized - Instance ID: " + 
                             System.identityHashCode(INSTANCE));
        }
    }
    
    // Track initialization state
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private final ConcurrentLinkedQueue<IBusListener> timingListeners = new ConcurrentLinkedQueue<>();
    private final ExecutorService timingExecutor;
    private final Command sharedCommand = new Command(null, null, null);
    private final String instanceId;

    private int eventCount = 0;
    private boolean diagnostic = false;

    private TimingBus() {

        // The super() call must be the first statement
        super();
        
        // Guard against reflection-based instantiation (after super call)
        if (INITIALIZED.getAndSet(true)) {
            throw new IllegalStateException("TimingBus already initialized");
        }
        
        // Generate unique ID for this instance
        instanceId = String.valueOf(System.identityHashCode(this));
        
        // Initialize executor for high-priority timing events
        timingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TimingBus-Thread");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });

        // Add initialization logging
        logger.info("TimingBus instance initialized - ID: {}", instanceId);
        System.out.println("TimingBus instance initialized - ID: " + instanceId);

        // Perform validation checks
        if (timingListeners == null) {
            String error = "CRITICAL ERROR: timingListeners is null in constructor";
            logger.error(error);
            System.err.println(error);
            throw new IllegalStateException(error);
        }

        // Register with our own listeners collection
        register(this);

        // Start diagnostic thread if needed
        if (diagnostic) {
            startDiagnosticThread();
        }
    }

    public static TimingBus getInstance() {
        return InstanceHolder.INSTANCE;
    }
    
    public String getInstanceId() {
        return instanceId;
    }

    private void startDiagnosticThread() {
        new Thread(() -> {
            long lastReport = System.currentTimeMillis();
            int lastEventCount = 0;

            while (true) {
                try {
                    Thread.sleep(5000);
                    long now = System.currentTimeMillis();
                    long eventsDelta = eventCount - lastEventCount;
                    logger.debug("TimingBus health: {} events in {} seconds",
                            eventsDelta, ((now - lastReport) / 1000));
                    lastReport = now;
                    lastEventCount = eventCount;
                } catch (Exception e) {
                    // Ignore
                }
            }
        }, "TimingBus-Monitor-" + instanceId).start();
    }

    @Override
    public void publish(String commandName, Object source, Object data) {
        if (Commands.TIMING_UPDATE.equals(commandName)) {
            Command cmd = new Command(commandName, source, data);

            for (IBusListener listener : timingListeners) {
                if (listener != source) // Avoid sending to self
                    try {
                        listener.onAction(cmd);
                    } catch (Exception e) {
                        System.err.println("Error in timing listener: " + e.getMessage());
                        e.printStackTrace();
                    }
            }

            eventCount++;
        }
    }

    public boolean isRegistered(IBusListener listener) {
        return timingListeners.contains(listener);
    }

    private void updateSharedCommand(String commandName, Object source, Object data) {
        sharedCommand.setCommand(commandName);
        sharedCommand.setSender(source);
        sharedCommand.setData(data);
    }

    public void publishTimingUpdate(TimingUpdate update) {
        Command command = new Command(Commands.TIMING_UPDATE, this, update);
        for (IBusListener listener : timingListeners) {
            try {
                ((Player) listener).onTick(update);
            } catch (ClassCastException e) {
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
            }
        }
    }

    @Override
    public void unregister(IBusListener listener) {
        if (listener != null && timingListeners != null) {
            timingListeners.remove(listener);
        }
    }

    public void addTimingListener(IBusListener listener) {
        if (listener == null) return;

        if (timingListeners == null) {
            System.err.println("TimingBus: timingListeners is null!");
            logger.error("TimingBus: timingListeners is null!");
            return;
        }

        timingListeners.add(listener);
        logger.debug("Added timing listener - now has {} listeners", timingListeners.size());
    }

    public static void testSingleton() {
        TimingBus instance1 = getInstance();
        TimingBus instance2 = getInstance();
        boolean same = instance1 == instance2;

        System.out.println("TimingBus singleton test: instances are " +
                (same ? "THE SAME" : "DIFFERENT"));
    }
}

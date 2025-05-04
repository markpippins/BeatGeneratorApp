package com.angrysurfer.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CommandBus {
    INSTANCE; // This is the singleton instance

    private static final Logger logger = LoggerFactory.getLogger(CommandBus.class);
    private final AbstractBusImpl busImpl;
    private final String instanceId;

    // Constructor runs exactly once when enum is first accessed
    CommandBus() {
        // Generate unique ID for this instance
        instanceId = String.valueOf(System.identityHashCode(this));
        
        // Log initialization
        System.out.println("CommandBus ENUM singleton initialized - ID: " + instanceId + 
                          " by ClassLoader: " + this.getClass().getClassLoader());
        
        // Create implementation
        busImpl = new AbstractBusImpl(true, Runtime.getRuntime().availableProcessors());
    }

    // Singleton accessor - Java guarantees this is thread-safe and returns same instance
    public static CommandBus getInstance() {
        return INSTANCE;
    }
    
    // Delegate methods to implementation
    public void register(IBusListener listener) {
        busImpl.register(listener);
    }
    
    public void unregister(IBusListener listener) {
        busImpl.unregister(listener);
    }
    
    public void publish(String command, Object sender, Object data) {
        busImpl.publish(command, sender, data);
    }
    
    // Instance ID accessor
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * Register a one-time listener that will automatically unregister after processing a single event
     * 
     * @param listener The listener to register for a single event
     * @return The created one-time listener wrapper (can be used to cancel if needed)
     */
    public IBusListener registerOneTime(IBusListener listener) {
        if (listener == null) return null;
        
        // Create a wrapper that will unregister itself after handling one event
        IBusListener oneTimeListener = new IBusListener() {
            @Override
            public void onAction(Command action) {
                try {
                    // Process the event
                    listener.onAction(action);
                } finally {
                    // Unregister regardless of whether processing succeeded
                    unregister(this);
                }
            }
        };
        
        // Register the wrapper
        register(oneTimeListener);
        
        // Return the wrapper in case the caller wants to cancel it
        return oneTimeListener;
    }
    
    // Test method
    public static void testSingleton() {
        CommandBus instance1 = getInstance();
        CommandBus instance2 = getInstance();
        boolean same = instance1 == instance2;
        
        System.out.println("CommandBus singleton test: instances are " + 
                         (same ? "THE SAME" : "DIFFERENT") + 
                         " - ID: " + instance1.getInstanceId());
    }
    
    // Inner class to implement bus functionality
    private static class AbstractBusImpl {
        private final java.util.List<IBusListener> listeners = 
            new java.util.concurrent.CopyOnWriteArrayList<>();
        private final java.util.concurrent.ExecutorService executor;
        
        public AbstractBusImpl(boolean async, int threadCount) {
            if (async) {
                executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
            } else {
                executor = null;
            }
        }
        
        public void register(IBusListener listener) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
                System.out.println("Registered listener - now have " + listeners.size());
            }
        }
        
        public void unregister(IBusListener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }
        
        public void publish(String command, Object sender, Object data) {
            Command cmd = new Command(command, sender, data);
            
            if (executor != null) {
                for (IBusListener listener : listeners) {
                    final IBusListener finalListener = listener;
                    executor.execute(() -> finalListener.onAction(cmd));
                }
            } else {
                for (IBusListener listener : listeners) {
                    listener.onAction(cmd);
                }
            }
        }
    }
}

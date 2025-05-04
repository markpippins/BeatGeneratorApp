package com.angrysurfer.core.api;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppRegistry {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Store references to the bus singletons
    private static CommandBus commandBus;
    private static TimingBus timingBus;

    public static void initialize() {
        if (initialized.compareAndSet(false, true)) {
            System.out.println("Initializing AppRegistry");
            
            // For enum singletons, get the instance using the enum constant or getInstance()
            commandBus = CommandBus.getInstance();
            timingBus = TimingBus.getInstance();
            
            System.out.println("AppRegistry initialized successfully");
        } else {
            System.out.println("AppRegistry already initialized");
        }
    }

    public static CommandBus getCommandBus() {
        if (!initialized.get()) {
            throw new IllegalStateException("AppRegistry not initialized");
        }
        return commandBus;
    }

    public static TimingBus getTimingBus() {
        if (!initialized.get()) {
            throw new IllegalStateException("AppRegistry not initialized");
        }
        return timingBus;
    }
}
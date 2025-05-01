package com.angrysurfer.core.api;

public class CommandBus extends AbstractBus {
    private static CommandBus instance;

    private CommandBus() {
        super();
    }

    public static CommandBus getInstance() {
        if (instance == null) {
            instance = new CommandBus();
        }
        return instance;
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
}

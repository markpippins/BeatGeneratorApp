package com.angrysurfer.beats.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.angrysurfer.beats.LogManager;

public class CommandBus implements CommandListener {
    private static CommandBus instance;
    private final List<CommandListener> listeners = new CopyOnWriteArrayList<>();
    private final LogManager logManager = LogManager.getInstance();

    private CommandBus() {
        // Register self to handle logging commands
        register(this);
    }

    public static CommandBus getInstance() {
        if (instance == null) {
            instance = new CommandBus();
        }
        return instance;
    }

    public void register(CommandListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(CommandListener listener) {
        listeners.remove(listener);
    }

    public void publish(Command action) {
        if (action == null) {
            logManager.error("CommandBus", "Attempted to publish null action");
            return;
        }

        String sender = action.getSender() != null ? action.getSender().getClass().getSimpleName() : "unknown";
        String dataType = action.getData() != null ? action.getData().getClass().getSimpleName() : "null";
        
        logManager.debug("CommandBus", 
            String.format("Publishing command: %s from: %s data: %s", 
                action.getCommand(), sender, dataType));

        listeners.forEach(listener -> {
            try {
                listener.onAction(action);
            } catch (Exception e) {
                logManager.error("CommandBus", 
                    String.format("Error in listener %s handling command %s: %s", 
                        listener.getClass().getSimpleName(), 
                        action.getCommand(), 
                        e.getMessage()));
            }
        });
    }

    @Override
    public void onAction(Command action) {
        // Handle logging commands
        if (action.getData() instanceof LogMessage msg) {
            switch (action.getCommand()) {
                case Commands.LOG_DEBUG -> logManager.debug(msg.source(), msg.message());
                case Commands.LOG_INFO -> logManager.info(msg.source(), msg.message());
                case Commands.LOG_WARN -> logManager.warn(msg.source(), msg.message());
                case Commands.LOG_ERROR -> {
                    if (msg.throwable() != null) {
                        logManager.error(msg.source(), msg.message(), msg.throwable());
                    } else {
                        logManager.error(msg.source(), msg.message());
                    }
                }
            }
        }
    }

    // Helper record for log messages
    public record LogMessage(String source, String message, Throwable throwable) {
        public LogMessage(String source, String message) {
            this(source, message, null);
        }
    }

    // Helper methods to publish log messages
    public void debug(String source, String message) {
        publish(new Command(Commands.LOG_DEBUG, new LogMessage(source, message), this));
    }

    public void info(String source, String message) {
        publish(new Command(Commands.LOG_INFO, new LogMessage(source, message), this));
    }

    public void warn(String source, String message) {
        publish(new Command(Commands.LOG_WARN, new LogMessage(source, message), this));
    }

    public void error(String source, String message) {
        publish(new Command(Commands.LOG_ERROR, new LogMessage(source, message), this));
    }

    public void error(String source, String message, Throwable e) {
        publish(new Command(Commands.LOG_ERROR, new LogMessage(source, message, e), this));
    }
}

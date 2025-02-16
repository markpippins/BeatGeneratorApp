package com.angrysurfer.beats.api;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class CommandBus {
    private static CommandBus instance;
    private final List<CommandListener> listeners = new CopyOnWriteArrayList<>();

    private CommandBus() {}

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
        listeners.forEach(listener -> listener.onAction(action));
    }
}

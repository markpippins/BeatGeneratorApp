package com.angrysurfer.beatsui.api;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ActionBus {
    private static ActionBus instance;
    private final List<ActionListener> listeners = new CopyOnWriteArrayList<>();

    private ActionBus() {}

    public static ActionBus getInstance() {
        if (instance == null) {
            instance = new ActionBus();
        }
        return instance;
    }

    public void register(ActionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(ActionListener listener) {
        listeners.remove(listener);
    }

    public void publish(Action action) {
        listeners.forEach(listener -> listener.onAction(action));
    }
}

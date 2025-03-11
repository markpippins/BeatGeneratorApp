package com.angrysurfer.core.api;

@FunctionalInterface
public interface BusListener {
    void onAction(Command action);
}

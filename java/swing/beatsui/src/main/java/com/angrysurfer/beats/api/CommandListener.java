package com.angrysurfer.beats.api;

@FunctionalInterface
public interface CommandListener {
    void onAction(Command action);
}

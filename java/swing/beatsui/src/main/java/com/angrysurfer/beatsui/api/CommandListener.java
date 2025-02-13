package com.angrysurfer.beatsui.api;

@FunctionalInterface
public interface CommandListener {
    void onAction(Command action);
}

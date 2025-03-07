package com.angrysurfer.core.api;

@FunctionalInterface
public interface CommandListener {
    void onAction(Command action);
}

package com.angrysurfer.core.api;

@FunctionalInterface
public interface IBusListener {
    void onAction(Command action);
}

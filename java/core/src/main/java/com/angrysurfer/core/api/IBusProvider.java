package com.angrysurfer.core.api;

public interface IBusProvider {
    CommandBus getCommandBus();
    TimingBus getTimingBus();
}
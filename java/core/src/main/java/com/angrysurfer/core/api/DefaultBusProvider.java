package com.angrysurfer.core.api;

public class DefaultBusProvider implements IBusProvider {
    private static final CommandBus commandBus = CommandBus.getInstance();  // = new CommandBus();
    private static final TimingBus timingBus = TimingBus.getInstance();     // new TimingBus();
    
    @Override
    public CommandBus getCommandBus() {
        return commandBus;
    }
    
    @Override
    public TimingBus getTimingBus() {
        return timingBus;
    }
}
package com.angrysurfer.core.api;

public class CommandBus extends AbstractBus {
    private static CommandBus instance;

    private CommandBus() {
        super();
    }

    public static CommandBus getInstance() {
        if (instance == null) {
            instance = new CommandBus();
        }
        return instance;
    }

}

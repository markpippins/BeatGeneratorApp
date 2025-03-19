package com.angrysurfer.core.api;

public class TimingBus extends AbstractBus {
    private static TimingBus instance;

    private TimingBus() {
        super();
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

}

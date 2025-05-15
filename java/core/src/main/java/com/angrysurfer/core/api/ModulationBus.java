package com.angrysurfer.core.api;

public class ModulationBus extends AbstractBus {
    private static ModulationBus instance;

    private ModulationBus() {
        super();
    }

    public static ModulationBus getInstance() {
        if (instance == null) {
            instance = new ModulationBus();
        }
        return instance;
    }

}
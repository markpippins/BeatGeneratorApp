package com.angrysurfer.core.event;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;

/**
 * Event for changing a player's instrument
 */
public class PlayerInstrumentChangeEvent extends PlayerEvent {
    private final InstrumentWrapper instrument;

    public PlayerInstrumentChangeEvent(Object creator, Player player, InstrumentWrapper instrument) {
        super(creator, player);
        this.instrument = instrument;
    }

    public InstrumentWrapper getInstrument() {
        return instrument;
    }
}
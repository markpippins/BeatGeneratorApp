package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.InstrumentWrapper;

/**
 * Event for changing a player's instrument
 */
public class PlayerInstrumentChangeEvent extends PlayerEvent {
    private final InstrumentWrapper instrument;
    
    public PlayerInstrumentChangeEvent(Player player, InstrumentWrapper instrument) {
        super(player);
        this.instrument = instrument;
    }
    
    public InstrumentWrapper getInstrument() {
        return instrument;
    }
}
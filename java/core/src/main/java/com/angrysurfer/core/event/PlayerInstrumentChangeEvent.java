package com.angrysurfer.core.event;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;

/**
 * Event for changing a player's instrument
 */
public record PlayerInstrumentChangeEvent(Object creator, Player player, InstrumentWrapper instrument) {

}

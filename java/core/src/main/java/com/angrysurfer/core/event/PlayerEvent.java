package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.InstrumentWrapper;

/**
 * Base class for all player-related events
 */
public abstract class PlayerEvent {
    private final Player player;
    
    public PlayerEvent(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Long getPlayerId() {
        return player != null ? player.getId() : null;
    }
}
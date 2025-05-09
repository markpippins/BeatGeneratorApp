package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for player update requests
 */
public class PlayerUpdateEvent extends PlayerEvent {
    public PlayerUpdateEvent(Player player) {
        super(player);
    }
}
package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for player selection (for UI purposes only, not setting an "active" player)
 */
public class PlayerSelectionEvent extends PlayerEvent {
    public PlayerSelectionEvent(Player player) {
        super(player);
    }
}
package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for requesting a player's instrument preset to be refreshed
 */
public class PlayerRefreshEvent extends PlayerEvent {
    public PlayerRefreshEvent(Object creator, Player player) {
        super(creator, player);
    }
}
package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import lombok.Getter;

/**
 * Event for player selection (for UI purposes only, not setting an "active" player)
 */
@Getter
public class PlayerSelectionEvent extends PlayerEvent {

    private Long playerId;

    public PlayerSelectionEvent(Object creator, Player player) {
        super(creator, player);
    }
}
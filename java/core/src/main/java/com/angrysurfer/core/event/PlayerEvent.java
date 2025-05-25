package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all player-related events
 */

@Getter
@Setter
public abstract class PlayerEvent {

    private final Player player;
    private final Object creator;

    public PlayerEvent(Object creator, Player player) {
        this.creator = creator;
        this.player = player;
    }
}
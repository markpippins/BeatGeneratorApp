package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for player update requests
 */
public record PlayerUpdateEvent(Object creator, Player player) {
        
}
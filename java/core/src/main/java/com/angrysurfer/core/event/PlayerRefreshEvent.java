package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for requesting a player's instrument preset to be refreshed
 */
public record PlayerRefreshEvent(Object creator, Player player) {
}
package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for changing a player's preset
 */
public record PlayerPresetChangeEvent(Object creator, Player player, String soundbank, Integer bankIndex,
                                      Integer presetNumber) {
}
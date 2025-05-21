package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;

/**
 * Event for changing a player's preset
 */
public class PlayerPresetChangeEvent extends PlayerEvent {
    private final Integer presetNumber;
    private final Integer bankIndex;


    public PlayerPresetChangeEvent(Object creator, Player player, Integer bankIndex, Integer presetNumber) {
        super(creator, player);
        this.bankIndex = bankIndex;
        this.presetNumber = presetNumber;
    }

    public Integer getPresetNumber() {
        return presetNumber;
    }

    public Integer getBankIndex() {
        return bankIndex;
    }
}
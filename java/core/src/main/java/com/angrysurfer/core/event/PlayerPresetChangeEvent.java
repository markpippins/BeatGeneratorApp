package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import lombok.Getter;
import lombok.Setter;

/**
 * Event for changing a player's preset
 */
@Getter
@Setter
public class PlayerPresetChangeEvent extends PlayerEvent {
    private final Integer presetNumber;
    private final Integer bankIndex;
    private final String soundbank;


    public PlayerPresetChangeEvent(Object creator, Player player, String soundbank, Integer bankIndex, Integer presetNumber) {
        super(creator, player);
        this.soundbank = soundbank;
        this.bankIndex = bankIndex;
        this.presetNumber = presetNumber;
    }
}
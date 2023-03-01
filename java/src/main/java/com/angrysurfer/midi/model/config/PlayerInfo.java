package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PlayerInfo implements Serializable {
    // Strikes
    int note;
    int minVelocity = 110;
    int maxVelocity = 127;
    private int preset;
    private String instrument;
    private int channel;
    private List<Rule> rules = new ArrayList<>();
    private List<Integer> allowedControlMessages = new ArrayList<>();
    private Long id;
    private boolean muted;
    public PlayerInfo() {

    }
    static void copyValues(Player player, PlayerInfo info){
        info.setAllowedControlMessages(player.getAllowedControlMessages());
        info.setPreset(player.getPreset());
        info.setChannel(player.getChannel());
        info.setInstrument(player.getInstrumentName());
        info.setRules(player.getRules());
        info.setNote(player.getNote());
        info.setId(player.getId());
        info.setMuted(player.isMuted());
        info.setMinVelocity(player.getMinVelocity());
        info.setMaxVelocity(player.getMaxVelocity());
    }

    public static PlayerInfo fromPlayer(Player player) {
        PlayerInfo def = new PlayerInfo();
        copyValues(player, def);
        return def;
    }
}

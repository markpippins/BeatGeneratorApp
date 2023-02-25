package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.condition.Condition;
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
    private List<Condition> conditions = new ArrayList<>();
    private List<Integer> allowedControlMessages = new ArrayList<>();
    private Long id;
    private boolean muted;
    public PlayerInfo() {

    }

    public static PlayerInfo fromPlayer(Player player) {
        PlayerInfo def = new PlayerInfo();
        def.setAllowedControlMessages(player.getAllowedControlMessages());
        def.setPreset(player.getPreset());
        def.setChannel(player.getChannel());
        def.setInstrument(player.getInstrumentName());
        def.setConditions(player.getConditions());
        def.setNote(player.getNote());
        def.setId(player.getId());
        def.setMuted(player.isMuted());
        return def;
    }
}

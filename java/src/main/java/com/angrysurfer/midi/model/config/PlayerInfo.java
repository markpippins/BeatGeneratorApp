package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Eval;
import com.angrysurfer.midi.model.Player;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, Eval> conditions = new HashMap<>();
    private List<Integer> allowedControlMessages = new ArrayList<>();
    private String playerId;

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
        def.setPlayerId(player.getPlayerId());
        return def;
    }
}

package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class PlayerInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    // Strikes
    int note;
    int minVelocity = 110;
    int maxVelocity = 127;
    private int preset;
    private String instrument;
    private int channel;
    @Transient private List<Rule> rules = new ArrayList<>();
    @Transient
    private List<Integer> allowedControlMessages = new ArrayList<>();
    private boolean muted;
    public PlayerInfo() {

    }

//    static void copyValues(PlayerInfo player, Player info){
//        info.setAllowedControlMessages(player.getAllowedControlMessages());
//        info.setPreset(player.getPreset());
//        info.setChannel(player.getChannel());
//        info.setInstrument(player.getInstrumentName());
//        info.setRules(player.getRules());
//        info.setNote(player.getNote());
//        info.setId(player.getId());
//        info.setMuted(player.isMuted());
//        info.setMinVelocity(player.getMinVelocity());
//        info.setMaxVelocity(player.getMaxVelocity());
//    }

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

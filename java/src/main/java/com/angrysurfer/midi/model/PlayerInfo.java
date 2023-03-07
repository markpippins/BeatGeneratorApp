package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
public class PlayerInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private Long tickerId;

    int note;
    int minVelocity = 110;
    int maxVelocity = 127;
    private int preset;
    private String instrument;
    private int channel;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "player_rules")
    private Set<Rule> rules = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private Set<Integer> allowedControlMessages = new HashSet<>();
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

    public static void copyValues(PlayerInfo info, Player player, Map<String, MidiInstrument> instruments) {
        player.setAllowedControlMessages(info.getAllowedControlMessages());
        player.setPreset(info.getPreset());
        player.setChannel(info.getChannel());
        player.setInstrument(instruments.getOrDefault(info.getInstrument(), null));
        player.setRules(info.getRules());
        player.setNote(info.getNote());
        player.setId(info.getId());
        player.setMuted(info.isMuted());
        player.setMinVelocity(info.getMinVelocity());
        player.setMaxVelocity(info.getMaxVelocity());
    }

    public static void copyValues(Player player, PlayerInfo info) {
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

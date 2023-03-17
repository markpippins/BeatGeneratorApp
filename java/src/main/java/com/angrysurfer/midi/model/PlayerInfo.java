package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Entity
public class PlayerInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    

    @OneToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "player_rules", joinColumns = { @JoinColumn(name = "rule_id") }, inverseJoinColumns = {
			@JoinColumn(name = "player_id") })
    private List<Rule> rules = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private Set<Integer> allowedControlMessages = new HashSet<>();
    
    @JsonIgnore
    @ManyToOne
	@JoinColumn(name = "instrument_id")
    private MidiInstrument instrument;

    private Long tickerId;

    private int note;

    private int minVelocity = 110;
    
    private int maxVelocity = 127;
    
    private int preset;
        
    private int channel;
    
    private boolean muted;

    public PlayerInfo() {

    }

    public Long getInstrumentId() {
        return Objects.nonNull(getInstrument()) ? getInstrument().getId() : null;
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

    public static void copyValues(PlayerInfo info, Player player) {
        player.setAllowedControlMessages(info.getAllowedControlMessages());
        player.setPreset(info.getPreset());
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
        info.setInstrument(player.getInstrument());
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

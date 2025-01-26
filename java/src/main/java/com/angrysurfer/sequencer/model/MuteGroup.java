package com.angrysurfer.sequencer.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.sequencer.model.player.AbstractPlayer;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private Set<AbstractPlayer> players = new HashSet<>();
}

package com.angrysurfer.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.core.model.player.AbstractPlayer;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private Set<AbstractPlayer> players = new HashSet<>();
}

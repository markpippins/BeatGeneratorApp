package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private Set<Player> players = new HashSet<>();
}

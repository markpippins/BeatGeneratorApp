package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private Set<Player> players = new HashSet<>();
}

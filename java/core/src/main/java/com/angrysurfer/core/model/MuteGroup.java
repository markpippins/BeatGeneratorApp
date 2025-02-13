package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.core.api.IPlayer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private Set<IPlayer> players = new HashSet<>();
}

package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MuteGroup { // should extend event
    private String name;
    private List<Player> players = new ArrayList<>();
}

package com.angrysurfer.midi.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerUpdateDTO {
    private Long playerId;
    private int updateType;
    private int updateValue;
}

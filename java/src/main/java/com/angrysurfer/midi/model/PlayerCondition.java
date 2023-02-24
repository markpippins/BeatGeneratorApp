package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerCondition {
    int id;

    String instrument;

    String channel;

    String preset;

    int note;

    String operator;

    String comparison;

    double value;

    int minVelocity;

    int maxVelocity;
}

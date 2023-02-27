package com.angrysurfer.midi.model;

import lombok.Getter;

public interface Operator {
    static int TICK = 0;

    static int BEAT = 1;
    static int BAR = 2;
    static int PART = 3;
    static int POSITION = 4;
}

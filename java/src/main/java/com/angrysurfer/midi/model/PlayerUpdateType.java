package com.angrysurfer.midi.model;

public interface PlayerUpdateType {
    static int INSTRUMENT = 0;
    static int NOTE = 1;
    static int PROBABILITY = 2;
    static int MIN_VELOCITY = 3;
    static int MAX_VELOCITY = 4;
    static int MUTE = 5;
}

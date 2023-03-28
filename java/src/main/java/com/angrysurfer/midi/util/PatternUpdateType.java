package com.angrysurfer.midi.util;

public interface PatternUpdateType {
    static int CHANNEL = 0;

    static int ACTIVE = 1;
    static int BASE_NOTE = 2;
    static int LAST_STEP = 3;
    static int DIRECTION = 4;
    static int PROBABILITY = 5;
    static int RANDOM = 6;
    static int TRANSPOSE = 7;
    static int SCALE = 8;

}
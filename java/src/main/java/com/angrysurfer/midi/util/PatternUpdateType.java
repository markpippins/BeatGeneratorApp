package com.angrysurfer.midi.util;

public interface PatternUpdateType {
    static int CHANNEL = 0;
    static int ACTIVE = 1;
    static int ROOT_NOTE = 2;
    static int LAST_STEP = 3;
    static int DIRECTION = 4;
    static int QUANTIZE = 5;
    static int RANDOM = 6;
    static int TRANSPOSE = 7;
    static int LENGTH = 8;
    static int GATE = 9;
    static int REPEATS = 10;
    static int SWING = 11;
    static int DEVICE = 12;
    static int PRESET = 13;
    static int SCALE = 14;
    static int INSTRUMENT = 15;
    static int BEAT_DIVIDER = 16;
    static int RANDOM_DEGREE = 17;
    static int SPEED = 18;
    static int FIRST_STEP = 19;
    static int MUTE = 20;
    static int LOOP = 21;
}
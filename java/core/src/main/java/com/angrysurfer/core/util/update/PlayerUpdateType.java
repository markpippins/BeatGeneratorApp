package com.angrysurfer.core.util.update;

public interface PlayerUpdateType {
    static int INSTRUMENT = 0;
    static int NOTE = 1;
    static int PROBABILITY = 2;
    static int MIN_VELOCITY = 3;
    static int MAX_VELOCITY = 4;
    static int MUTE = 5;
    static int PART  = 6;
    static int LEVEL  = 7;
    static int SWING = 8;
    static int PRESET  = 9;
    static int RATCHET_COUNT  = 10;
    static int RATCHET_INTERVAL  = 11;
    static int CHANNEL  = 12;
    static int SKIPS = 13;
    static int BEAT_FRACTION = 14;
    static int SUBDIVISIONS = 15;
    static int RANDOM_DEGREE = 16;
    static int FADE_IN = 17;
    static int FADE_OUT = 18;
    static int SOLO = 19;
}

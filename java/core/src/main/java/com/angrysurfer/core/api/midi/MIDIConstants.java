package com.angrysurfer.core.api.midi;

public class MIDIConstants {

    public static final int DEFAULT_MONO_SEQUENCERS = 8;

    // MIDI CC values
    public static final int CC_VOLUME = 7;
    public static final int CC_PAN = 10;
    public static final int CC_REVERB = 91;
    public static final int CC_CHORUS = 93;
    public static final int CC_DELAY = 94; // Use for decay
    // Default values for parameters
    public static final int DEFAULT_VELOCITY = 100; // Default note velocity
    public static final int DEFAULT_DECAY = 60; // Default note decay
    public static final int DEFAULT_PROBABILITY = 100; // Default step probability (%)
    public static final int DEFAULT_TICKS_PER_BEAT = 24; // Default timing fallback
    public static final int DEFAULT_MASTER_TEMPO = 96; // Default master tempo
    public static final int DEFAULT_PAN = 64; // Default pan position (center)
    public static final int DEFAULT_CHORUS = 0; // Default chorus effect amount
    public static final int DEFAULT_REVERB = 0; // Default reverb effect amount
    // MIDI and music constants
    public static final int MIDI_DRUM_CHANNEL = 9; // Standard MIDI drum channel
    public static final int MIDI_DRUM_NOTE_OFFSET = 36; // First drum pad note number
    public static final int MAX_MIDI_VELOCITY = 127; // Maximum MIDI velocity
    // Swing parameters
    public static final int NO_SWING = 50; // Percentage value for no swing
    public static final int MAX_SWING = 99; // Maximum swing percentage
    public static final int MIN_SWING = 25; // Minimum swing percentage
    // Pattern generation parameters
    public static final int MAX_DENSITY = 10; // Maximum density for pattern generation
    public static final int[] SEQUENCER_CHANNELS = {3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15};
    public static final long MIN_NOTE_INTERVAL_MS = 1; // 1ms minimum between notes

    public static final int DEFAULT_NOTE = 60; // Middle C
    public static final int DEFAULT_GATE = 75;
}

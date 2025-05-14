package com.angrysurfer.beats.panel.modulation.oscillator;

// Waveform types
public enum WaveformType {
    SINE("Sine"),
    TRIANGLE("Triangle"),
    SAWTOOTH("Sawtooth"),
    SQUARE("Square"),
    PULSE("Pulse"),
    RANDOM("Random");

    private final String displayName;

    WaveformType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

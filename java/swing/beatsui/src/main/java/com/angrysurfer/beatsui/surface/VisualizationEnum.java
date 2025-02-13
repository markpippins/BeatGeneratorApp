package com.angrysurfer.beatsui.surface;

public enum VisualizationEnum {
    // Removed TEXT mode, start with EXPLOSION
    EXPLOSION("Explosion"),
    SPACE("Space"),
    GAME("Game of Life"),
    RAIN("Matrix Rain"),
    WAVE("Wave"),
    BOUNCE("Bounce"),
    SNAKE("Snake"),
    SPIRAL("Spiral"),
    FIREWORKS("Fireworks"),
    PULSE("Pulse"),
    RAINBOW("Rainbow"),
    CLOCK("Clock"),
    CONFETTI("Confetti"),
    MATRIX("Matrix"),
    HEART("Heart Beat"),
    DNA("DNA Helix"),
    PING_PONG("Ping Pong"),
    EQUALIZER("Equalizer"),
    TETRIS("Tetris"),
    COMBAT("Combat"),
    STARFIELD("Starfield"),
    RIPPLE("Ripple"),
    MAZE("Maze Generator"),
    LIFE_SOUP("Life Soup"),
    PLASMA("Plasma"),
    MANDELBROT("Mandelbrot"),
    BINARY("Binary Rain"),
    KALEIDOSCOPE("Kaleidoscope"),
    CELLULAR("Cellular"),
    BROWNIAN("Brownian Motion"),
    CRYSTAL("Crystal Growth"),
    LANGTON("Langton's Ant"),
    SPECTRUM_ANALYZER("Spectrum Analyzer"),
    WAVEFORM("Waveform"),
    OSCILLOSCOPE("Oscilloscope"),
    VU_METERS("VU Meters"),
    FREQUENCY_BANDS("Frequency Bands"),
    MIDI_GRID("MIDI Grid"),
    STEP_SEQUENCER("Step Sequencer"),
    LOOP_PULSE("Loop Pulse"),
    DRUM_PATTERN("Drum Pattern"),
    TIME_DIVISION("Time Division"),
    POLYPHONIC("Polyphonic Lines"),
    PIANO_ROLL("Piano Roll"),
    RUBIKS_COMP("Rubik's Competition"),
    EUCLID("Euclidean Rhythm"),
    MODULAR("Modular CV"),
    POLYRHYTHM("Polyrhythm"),
    ARPEGGIATOR("Arpeggiator"),
    GATE_SEQ("Gate Sequencer"),
    CHORD_PROG("Chord Progression"),
    PROBABILITY("Probability Grid"),
    HARMONICS("Harmonic Series"),
    LFO_MATRIX("LFO Matrix"),
    PHASE_SHIFT("Phase Shifter"),
    XY_PAD("XY Pad"),
    TRIG_BURST("Trigger Burst"),
    TRON("Tron Light Cycles"),
    RACING("Racing"),
    INVADERS("Space Invaders"),
    MISSILE("Missile Command"),
    PACMAN("Pac-Man"),
    BREAKOUT("Breakout"),
    ASTEROID("Asteroids"),
    PONG("Pong Classic"),
    DIGDUG("Dig Dug"),
    CLIMBER("Platform Climber"), // Generic name for similar gameplay style
    FROGGER("Frogger"),
    POLE_POSITION("Pole Position"),
    RAINBOW_MATRIX("Rainbow Japanese Matrix");

    private final String label;

    VisualizationEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static VisualizationEnum fromLabel(String label) {
        return java.util.Arrays.stream(values())
                .filter(v -> v.getLabel().equals(label))
                .findFirst()
                .orElse(null);
    }
}
package com.angrysurfer.core.sequencer;

// Timing parameters
public enum TimingDivision {
    NORMAL("Normal", 4 * 6), // Standard 4 steps per beat
    DOUBLE("Double Time", 8 * 6), // Twice as fast (8 steps per beat)
    HALF("Half Time", 2 * 6), // Half as fast (2 steps per beat) 
    TRIPLET("Triplets", 3 * 6), // Triplet timing (3 steps per beat)
    EIGHTH_TRIPLET("1/8 Triplets", 6 * 6), // Eighth note triplets (6 steps per beat)
    SIXTEENTH("1/16 Notes", 16 * 6), // Sixteenth notes (16 steps per beat)
    SIXTEENTH_TRIPLET("1/16 Triplets", 12 * 6); // Sixteenth note triplets (12 steps per beat)

    // NORMAL("Normal", 4), // Standard 4 steps per beat
    // DOUBLE("Double Time", 8), // Twice as fast (8 steps per beat)
    // HALF("Half Time", 2), // Half as fast (2 steps per beat) 
    // TRIPLET("Triplets", 3), // Triplet timing (3 steps per beat)
    // EIGHTH_TRIPLET("1/8 Triplets", 6), // Eighth note triplets (6 steps per beat)
    // SIXTEENTH("1/16 Notes", 16), // Sixteenth notes (16 steps per beat)
    // SIXTEENTH_TRIPLET("1/16 Triplets", 12); // Sixteenth note triplets (12 steps per beat)

    private final String displayName;
    private final int ticksPerBeat;

    TimingDivision(String displayName, int ticksPerBeat) {
        this.displayName = displayName;
        this.ticksPerBeat = ticksPerBeat;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTicksPerBeat() {
        return ticksPerBeat;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
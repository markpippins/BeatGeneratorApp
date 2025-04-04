package com.angrysurfer.core.sequencer;

// Timing parameters
public enum TimingDivision {
    NORMAL("Normal", 4), // Standard 4 steps per beat
    DOUBLE("Double Time", 8), // Twice as fast (8 steps per beat)
    HALF("Half Time", 2), // Half as fast (2 steps per beat) 
    TRIPLET("Triplets", 3), // Triplet timing (3 steps per beat)
    EIGHTH_TRIPLET("1/8 Triplets", 6), // Eighth note triplets (6 steps per beat)
    SIXTEENTH("1/16 Notes", 16), // Sixteenth notes (16 steps per beat)
    SIXTEENTH_TRIPLET("1/16 Triplets", 12); // Sixteenth note triplets (12 steps per beat)

    private final String displayName;
    private final int stepsPerBeat;

    TimingDivision(String displayName, int stepsPerBeat) {
        this.displayName = displayName;
        this.stepsPerBeat = stepsPerBeat;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStepsPerBeat() {
        return stepsPerBeat;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
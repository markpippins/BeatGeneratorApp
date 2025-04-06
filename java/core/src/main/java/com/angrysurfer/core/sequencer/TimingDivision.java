package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.Comparator;

// Timing parameters
public enum TimingDivision {
    NORMAL("Normal", 24), // Standard 24 steps per beat
    DOUBLE("Double Time", 48), // Twice as fast (48 steps per beat)
    HALF("Half Time", 12), // Half as fast (12 steps per beat) 
    QUARTER("Quarter Time", 6), // Quarter time (6 steps per beat)
    TRIPLET("Triplet", 18), // Triplet timing (18 steps per beat)
    QUARTER_TRIPLET("1/4 Triplets", 18), // Eighth note triplets (36 steps per beat)
    EIGHTH_TRIPLET("1/8 Triplets", 36), // Eighth note triplets (36 steps per beat)
    SIXTEENTH("1/16 Notes", 96), // Sixteenth notes (96 steps per beat)
    SIXTEENTH_TRIPLET("1/16 Triplets", 72), // Sixteenth note triplets (72 steps per beat)
    BEBOP("Bebop", 24), // Jazz timing division
    FIVE_FOUR("5/4 Time", 30), // Five beats per measure
    SEVEN_EIGHT("7/8 Time", 28), // Seven beats per measure
    NINE_EIGHT("9/8 Time", 36), // Nine beats per measure
    TWELVE_EIGHT("12/8 Time", 48), // Twelve beats per measure
    SIX_FOUR("6/4 Time", 36); // Six beats per measure

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

    // Add this static method to get values sorted alphabetically
    public static TimingDivision[] getValuesAlphabetically() {
        TimingDivision[] values = values();
        Arrays.sort(values, Comparator.comparing(TimingDivision::getDisplayName));
        return values;
    }
}
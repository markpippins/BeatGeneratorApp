package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.Comparator;

// Timing parameters calibrated for 96 PPQN system
public enum TimingDivision {
    NORMAL("Normal", 24), // Standard step rate (24 steps per beat)
    DOUBLE("Double Time", 12), // Twice as fast (smaller number = more frequent steps)
    HALF("Half Time", 48), // Half as fast (larger number = less frequent steps)
    QUARTER("Quarter Time", 96), // Quarter speed (even less frequent)
    EIGHTH("Eighth Time", 192), // Eighth notes (fast)
    SIXTEEN("Sixteenth Time", 384), // Eighth notes (fast)
    TRIPLET("Triplet", 32), // 3 notes in the space of 2 (1/8 triplets)
    QUARTER_TRIPLET("1/4 Triplets", 64), // Quarter note triplets
    EIGHTH_TRIPLET("1/8 Triplets", 16), // Eighth note triplets
    SIXTEENTH("1/16 Notes", 6), // Sixteenth notes (very fast)
    SIXTEENTH_TRIPLET("1/16 Triplets", 4), // Sixteenth note triplets
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
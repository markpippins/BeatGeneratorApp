package com.angrysurfer.core.sequencer;

public interface ISequenceGenerator {
    /**
     * Generates a sequence of boolean values.
     *
     * @param length the length of the sequence to generate
     * @return an array of boolean values representing the generated sequence
     */
    public boolean[] generate(int length);

    public String getName();
}

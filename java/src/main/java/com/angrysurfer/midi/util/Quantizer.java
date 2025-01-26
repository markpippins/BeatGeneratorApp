package com.angrysurfer.midi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Quantizer {
    private Boolean[] scale;

    static Logger logger = LoggerFactory.getLogger(Quantizer.class.getCanonicalName());

    public Quantizer(Boolean[] scale) {
        this.scale = scale;
    }

    public int quantizeNote(int note) {
        int val = note;

        while (val > 11)
            val -= 12;

        boolean noteInScale = scale[val];
        if (noteInScale)
            return note;

        int distanceUp = 0;
        while ((val + distanceUp < 11) && (!scale[val + distanceUp]))
            distanceUp++;

        int distanceDown = 0;
        while ((val - distanceDown > 0) && (!scale[val - distanceDown]))
            distanceDown++;

        int result = distanceUp > distanceDown ? note + distanceUp : note - distanceDown;
        logger.info("Quantize " + getNoteForValue(note, Scale.SCALE_NOTES) + " to "
                + getNoteForValue(result, Scale.SCALE_NOTES));
        return result;
    }

    public String getNoteForValue(int value, String[] scale) {
        int note = value;
        while (note > 11)
            note -= 12;
        while (note < 0)
            note += 12;

        int octave = 1;
        for (int i = 0; i < value; i += 12)
            octave++;

        return scale[note] + Integer.toString(octave);
    }
}

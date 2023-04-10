package com.angrysurfer.midi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Quantizer {
    private String key;
    private Boolean[] scale;

    static Logger logger = LoggerFactory.getLogger(Quantizer.class.getCanonicalName());

    public static Boolean[] C_MAJOR_SCALE = {
            true,
            false,
            true,
            false,
            true,
            true,
            false,
            true,
            false,
            true,
            false,
            true,
    };

    public static Boolean[] C_MINOR_SCALE = {
            true,
            false,
            true,
            true,
            false,
            true,
            false,
            true,
            true,
            true,
            true,
            false,
    };

    public static String[] SCALE_NOTES = {
            "C",
            "C♯/D♭",
            "D",
            "D♯/E♭",
            "E",
            "F",
            "F♯/G♭",
            "G",
            "G♯/A♭",
            "A",
            "A♯/B♭",
            "B",
    };

    public Quantizer(Boolean[] scale) {
        setScale(scale);
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
        logger.info("Quantize " + getNoteForValue(note, SCALE_NOTES) + " to " + getNoteForValue(result, SCALE_NOTES));
        return result;
    }

    public String getNoteForValue(int value, String[] scale) {
        int note = value;
        while (note > 11)
            note = note - 12;
        return scale[note];
    }
}

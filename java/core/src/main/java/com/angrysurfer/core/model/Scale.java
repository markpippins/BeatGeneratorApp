package com.angrysurfer.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scale {

    static final Logger logger = LoggerFactory.getLogger(Scale.class);

    public static String[] SCALE_NOTES = {
            "C", "C♯/D♭", "D", "D♯/E♭", "E",
            "F", "F♯/G♭", "G", "G♯/A♭", "A", "A♯/B♭", "B"
    };

    // Define scale patterns as offsets
    public static Map<String, int[]> SCALE_PATTERNS = new HashMap<>();

    static {
        // Major and Minor Scales
        SCALE_PATTERNS.put("Major", new int[] { 0, 2, 4, 5, 7, 9, 11 });
        SCALE_PATTERNS.put("Natural Minor", new int[] { 0, 2, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Harmonic Minor", new int[] { 0, 2, 3, 5, 7, 8, 11 });
        SCALE_PATTERNS.put("Melodic Minor", new int[] { 0, 2, 3, 5, 7, 9, 11 });

        // Pentatonic Scales
        SCALE_PATTERNS.put("Major Pentatonic", new int[] { 0, 2, 4, 7, 9 });
        SCALE_PATTERNS.put("Minor Pentatonic", new int[] { 0, 3, 5, 7, 10 });

        // Double Harmonic Scales
        SCALE_PATTERNS.put("Double Harmonic Minor", new int[] { 0, 1, 4, 5, 7, 8, 11 });
        SCALE_PATTERNS.put("Double Harmonic Major", new int[] { 0, 1, 4, 5, 7, 9, 11 });

        // Blues Scales
        SCALE_PATTERNS.put("Blues", new int[] { 0, 3, 5, 6, 7, 10 });

        // Whole Tone Scale
        SCALE_PATTERNS.put("Whole Tone", new int[] { 0, 2, 4, 6, 8, 10 });

        // Diminished Scales
        SCALE_PATTERNS.put("Diminished (Whole-Half)", new int[] { 0, 2, 3, 5, 6, 8, 9, 11 });
        SCALE_PATTERNS.put("Diminished (Half-Whole)", new int[] { 0, 1, 3, 4, 6, 7, 9, 10 });

        // Augmented Scale
        SCALE_PATTERNS.put("Augmented", new int[] { 0, 3, 4, 7, 8, 11 });

        // Modes (Church Modes)
        SCALE_PATTERNS.put("Ionian (Major)", new int[] { 0, 2, 4, 5, 7, 9, 11 });
        SCALE_PATTERNS.put("Dorian", new int[] { 0, 2, 3, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Phrygian", new int[] { 0, 1, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Lydian", new int[] { 0, 2, 4, 6, 7, 9, 11 });
        SCALE_PATTERNS.put("Mixolydian", new int[] { 0, 2, 4, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Aeolian (Natural Minor)", new int[] { 0, 2, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Locrian", new int[] { 0, 1, 3, 5, 6, 8, 10 });
        
        // Exotic Scales
        SCALE_PATTERNS.put("Hungarian Minor", new int[] { 0, 2, 3, 6, 7, 8, 11 });
        SCALE_PATTERNS.put("Spanish Gypsy", new int[] { 0, 1, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Persian", new int[] { 0, 1, 4, 5, 6, 8, 11 });
        SCALE_PATTERNS.put("Hirajoshi", new int[] { 0, 2, 3, 7, 8 });
        SCALE_PATTERNS.put("In Scale", new int[] { 0, 1, 5, 7, 8 });
        SCALE_PATTERNS.put("Arabian", new int[] { 0, 2, 4, 5, 6, 8, 10 });

        // Byzantine Scales
        // SCALE_PATTERNS.put("Byzantine", new int[] { 0, 1, 4, 5, 7, 8, 11 }); // Duplicate of Double Harmonic Minor

        // Gypsy Scales
        SCALE_PATTERNS.put("Gypsy Major", new int[] { 0, 1, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Gypsy Minor", new int[] { 0, 2, 3, 6, 7, 8, 10 });

        // Arabic Scales and Modes
        SCALE_PATTERNS.put("Arabic", new int[] { 0, 2, 4, 5, 6, 8, 10 });
        SCALE_PATTERNS.put("Hijaz", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Often used in Arabic music
        SCALE_PATTERNS.put("Hijaz Kar", new int[] { 0, 1, 4, 5, 7, 8, 11 }); // Variant of Hijaz
        SCALE_PATTERNS.put("Phrygian Dominant", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Often called "Arabic Scale"
        SCALE_PATTERNS.put("Nahawand", new int[] { 0, 2, 3, 5, 7, 8, 11 }); // Similar to harmonic minor
        SCALE_PATTERNS.put("Nahawand Murassaa", new int[] { 0, 2, 3, 5, 7, 9, 11 }); // Similar to melodic minor
        SCALE_PATTERNS.put("Nikriz", new int[] { 0, 1, 4, 5, 6, 9, 10 });
        SCALE_PATTERNS.put("Saba", new int[] { 0, 1, 4, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Sikah", new int[] { 0, 3, 5, 7, 8, 11 });
        SCALE_PATTERNS.put("Sikah Baladi", new int[] { 0, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Rast", new int[] { 0, 2, 4, 5, 7, 9, 11 });
        SCALE_PATTERNS.put("Bayati", new int[] { 0, 1, 4, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Huzam", new int[] { 0, 2, 3, 7, 8, 10 });
        SCALE_PATTERNS.put("Kurd", new int[] { 0, 2, 3, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Saba Zamzam", new int[] { 0, 1, 4, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Suznak", new int[] { 0, 1, 4, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Nawa Athar", new int[] { 0, 1, 4, 6, 8, 10, 11 });
        SCALE_PATTERNS.put("Nikriz", new int[] { 0, 1, 4, 5, 6, 9, 10 });
        SCALE_PATTERNS.put("Athar Kurd", new int[] { 0, 2, 3, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Hijaz Kar Kurd", new int[] { 0, 1, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Suznak", new int[] { 0, 1, 4, 5, 7, 9, 10 });

        // Exotic and Related Scales
        // SCALE_PATTERNS.put("Hungarian Gypsy", new int[] { 0, 2, 3, 6, 7, 8, 11 }); // Duplicate of Hungarian Minor
        SCALE_PATTERNS.put("Kurdish", new int[] { 0, 2, 3, 5, 6, 8, 10 });
        SCALE_PATTERNS.put("Raga Todi", new int[] { 0, 1, 3, 6, 7, 8, 11 }); // Scale from Indian classical music
        SCALE_PATTERNS.put("Oriental", new int[] { 0, 1, 4, 5, 6, 9, 10 });
        SCALE_PATTERNS.put("Jewish", new int[] { 0, 1, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Ukrainian Dorian", new int[] { 0, 2, 3, 6, 7, 9, 10 });

        // Missing Modes (Church Modes and Variations)
        SCALE_PATTERNS.put("Super Locrian", new int[] { 0, 1, 3, 4, 6, 8, 10 }); // Altered scale
        SCALE_PATTERNS.put("Lydian Augmented", new int[] { 0, 2, 4, 6, 8, 9, 11 });
        // SCALE_PATTERNS.put("Lydian Dominant", new int[] { 0, 2, 4, 6, 7, 9, 10 }); // Duplicate
        SCALE_PATTERNS.put("Mixolydian b6", new int[] { 0, 2, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Locrian Natural 6", new int[] { 0, 1, 3, 5, 6, 9, 10 });
        // SCALE_PATTERNS.put("Phrygian Major", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Duplicate of Phrygian Dominant

        // Jazz and Blues Scales
        SCALE_PATTERNS.put("Half-Whole Diminished", new int[] { 0, 1, 3, 4, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Whole-Half Diminished", new int[] { 0, 2, 3, 5, 6, 8, 9, 11 });
        SCALE_PATTERNS.put("Chromatic", new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
        SCALE_PATTERNS.put("Major Blues", new int[] { 0, 2, 3, 4, 7, 9 });
        SCALE_PATTERNS.put("Minor Blues", new int[] { 0, 3, 5, 6, 7, 10 });
        SCALE_PATTERNS.put("Tritone", new int[] { 0, 1, 4, 6, 7, 10 });
        // SCALE_PATTERNS.put("Altered", new int[] { 0, 1, 3, 4, 6, 8, 10 }); // Duplicate of Super Locrian
        // SCALE_PATTERNS.put("Mixolydian b9 b13", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Duplicate of Phrygian Dominant
        SCALE_PATTERNS.put("Spanish Phrygian", new int[] { 0, 1, 4, 5, 7, 8, 11 });
        SCALE_PATTERNS.put("Dorian b2", new int[] { 0, 1, 3, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Lydian #2", new int[] { 0, 3, 4, 6, 7, 9, 11 });
        SCALE_PATTERNS.put("Super Locrian bb7", new int[] { 0, 1, 3, 4, 6, 8, 9 });
        // SCALE_PATTERNS.put("Whole Tone", new int[] { 0, 2, 4, 6, 8, 10 }); // Duplicate
        // SCALE_PATTERNS.put("Half-Whole Diminished", new int[] { 0, 1, 3, 4, 6, 7, 9, 10 }); // Duplicate
        // SCALE_PATTERNS.put("Whole-Half Diminished", new int[] { 0, 2, 3, 5, 6, 8, 9, 11 }); // Duplicate
        // SCALE_PATTERNS.put("Augmented", new int[] { 0, 3, 4, 7, 8, 11 }); // Duplicate
        SCALE_PATTERNS.put("Augmented Lydian", new int[] { 0, 2, 4, 6, 8, 9, 11 });
        SCALE_PATTERNS.put("Augmented Ionian", new int[] { 0, 2, 4, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Augmented Dorian", new int[] { 0, 2, 3, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Augmented Phrygian", new int[] { 0, 1, 3, 5, 8, 9, 11 });
        // SCALE_PATTERNS.put("Augmented Lydian", new int[] { 0, 2, 4, 6, 8, 9, 11 }); // Duplicate
        SCALE_PATTERNS.put("Augmented Mixolydian", new int[] { 0, 2, 4, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Augmented Aeolian", new int[] { 0, 2, 3, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Augmented Locrian", new int[] { 0, 1, 3, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Augmented Harmonic Minor", new int[] { 0, 2, 3, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Augmented Melodic Minor", new int[] { 0, 2, 3, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Augmented Dorian b2", new int[] { 0, 1, 3, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Augmented Mixolydian b6", new int[] { 0, 2, 4, 5, 8, 9, 10 });
        SCALE_PATTERNS.put("Augmented Locrian b7", new int[] { 0, 1, 3, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Bebop", new int[] { 0, 2, 4, 5, 7, 9, 10, 11 });
        SCALE_PATTERNS.put("Bebop Major", new int[] { 0, 2, 4, 5, 7, 8, 9, 11 });
        SCALE_PATTERNS.put("Bebop Minor", new int[] { 0, 2, 3, 5, 7, 8, 9, 10 });
        SCALE_PATTERNS.put("Bebop Dominant", new int[] { 0, 2, 4, 5, 7, 9, 10, 11 });
        SCALE_PATTERNS.put("Bebop Dorian", new int[] { 0, 2, 3, 5, 7, 9, 10, 11 });
        SCALE_PATTERNS.put("Bebop Phrygian", new int[] { 0, 1, 3, 5, 7, 8, 10, 11 });
        SCALE_PATTERNS.put("Bebop Lydian", new int[] { 0, 2, 4, 6, 7, 9, 10, 11 });
        // SCALE_PATTERNS.put("Bebop Mixolydian", new int[] { 0, 2, 4, 5, 7, 9, 10, 11 }); // Duplicate of Bebop Dominant
        SCALE_PATTERNS.put("Bebop Locrian", new int[] { 0, 1, 3, 5, 6, 8, 10, 11 });
        SCALE_PATTERNS.put("Bebop Harmonic Minor", new int[] { 0, 2, 3, 5, 7, 8, 9, 11 });
        SCALE_PATTERNS.put("Bebop Melodic Minor", new int[] { 0, 2, 3, 5, 7, 9, 10, 11 });
        SCALE_PATTERNS.put("Bebop Dorian b2", new int[] { 0, 1, 3, 5, 7, 9, 10, 11 });
        SCALE_PATTERNS.put("Bebop Mixolydian b6", new int[] { 0, 2, 4, 5, 7, 8, 10, 11 });
        SCALE_PATTERNS.put("Bebop Locrian b7", new int[] { 0, 1, 3, 5, 6, 8, 10, 11 });
        
        // Hexatonic Scales
        SCALE_PATTERNS.put("Augmented Hexatonic", new int[] { 0, 3, 4, 7, 8, 11 });
        SCALE_PATTERNS.put("Prometheus", new int[] { 0, 2, 4, 6, 9, 10 });
        // SCALE_PATTERNS.put("Whole Tone", new int[] { 0, 2, 4, 6, 8, 10 }); // Duplicate
        // SCALE_PATTERNS.put("Blues", new int[] { 0, 3, 5, 6, 7, 10 }); // Duplicate
        SCALE_PATTERNS.put("Tritone", new int[] { 0, 1, 4, 6, 7, 10 });
        SCALE_PATTERNS.put("Raga Megha", new int[] { 0, 1, 3, 6, 7, 10 });


        // Heptatonic (Exotic Scales)
        SCALE_PATTERNS.put("Balinese", new int[] { 0, 1, 3, 7, 8, 9, 10 });
        SCALE_PATTERNS.put("Javanese", new int[] { 0, 1, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Japanese", new int[] { 0, 1, 5, 7, 8, 9, 10 });
        SCALE_PATTERNS.put("Romanian Minor", new int[] { 0, 2, 3, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Neapolitan Major", new int[] { 0, 1, 3, 5, 7, 9, 11 });
        SCALE_PATTERNS.put("Neapolitan Minor", new int[] { 0, 1, 3, 5, 7, 8, 11 });
        SCALE_PATTERNS.put("Enigmatic", new int[] { 0, 1, 4, 6, 8, 10, 11 });
        SCALE_PATTERNS.put("Eight-Tone Spanish", new int[] { 0, 1, 3, 4, 5, 6, 8, 10 });
        SCALE_PATTERNS.put("Leading Whole Tone", new int[] { 0, 2, 4, 6, 8, 10, 11 });
        SCALE_PATTERNS.put("Lydian Minor", new int[] { 0, 2, 4, 6, 7, 8, 10 });
        // SCALE_PATTERNS.put("Lydian Augmented", new int[] { 0, 2, 4, 6, 8, 9, 11 }); // Duplicate
        SCALE_PATTERNS.put("Acoustic", new int[] { 0, 2, 4, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Locrian Major", new int[] { 0, 2, 4, 5, 6, 8, 10 });
        SCALE_PATTERNS.put("Locrian Natural 6", new int[] { 0, 2, 3, 5, 6, 9, 10 });
        SCALE_PATTERNS.put("Ionian Augmented", new int[] { 0, 2, 4, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Dorian #4", new int[] { 0, 2, 3, 6, 7, 9, 10 });
        // SCALE_PATTERNS.put("Lydian Dominant", new int[] { 0, 2, 4, 6, 7, 9, 10 }); // Duplicate
        SCALE_PATTERNS.put("Mixolydian b13", new int[] { 0, 2, 4, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Aeolian b5", new int[] { 0, 2, 3, 5, 6, 8, 10 });
        SCALE_PATTERNS.put("Altered Dominant", new int[] { 0, 1, 3, 4, 6, 8, 10 });
        // SCALE_PATTERNS.put("Phrygian #6", new int[] { 0, 1, 3, 5, 7, 9, 10 }); // Duplicate of Dorian b2
        SCALE_PATTERNS.put("Lydian #2", new int[] { 0, 3, 4, 6, 7, 9, 11 });
        SCALE_PATTERNS.put("Super Locrian", new int[] { 0, 1, 3, 4, 6, 8, 10 });
        // SCALE_PATTERNS.put("Neopolitan", new int[] { 0, 1, 3, 5, 7, 8, 11 }); // Duplicate of Neapolitan Minor
        // SCALE_PATTERNS.put("Lydian #5", new int[] { 0, 2, 4, 6, 8, 9, 11 }); // Duplicate of Lydian Augmented
        // SCALE_PATTERNS.put("Lydian b7", new int[] { 0, 2, 4, 6, 7, 9, 10 }); // Duplicate
        // SCALE_PATTERNS.put("Locrian #2", new int[] { 0, 2, 3, 5, 6, 8, 10 }); // Duplicate of Kurdish
        SCALE_PATTERNS.put("Locrian #6", new int[] { 0, 1, 3, 5, 6, 9, 10 });
        SCALE_PATTERNS.put("Ionian #5", new int[] { 0, 2, 4, 5, 8, 9, 11 });
        SCALE_PATTERNS.put("Dorian b2", new int[] { 0, 1, 3, 5, 7, 9, 10 });
        // SCALE_PATTERNS.put("Phrygian Major", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Duplicate of Phrygian Dominant
        SCALE_PATTERNS.put("Lydian b3", new int[] { 0, 2, 3, 6, 7, 9, 11 });
        // SCALE_PATTERNS.put("Lydian b7", new int[] { 0, 2, 4, 6, 7, 9, 10 }); // Duplicate
        SCALE_PATTERNS.put("Mixolydian b9", new int[] { 0, 2, 4, 5, 7, 8, 10 });
        // SCALE_PATTERNS.put("Aeolian b4", new int[] { 0, 2, 3, 5, 6, 8, 10 }); // Duplicate of Kurdish
        // SCALE_PATTERNS.put("Phrygian #6", new int[] { 0, 1, 3, 5, 7, 9, 10 }); // Duplicate of Dorian b2

        // Pentatonic Variants
        SCALE_PATTERNS.put("Egyptian", new int[] { 0, 2, 5, 7, 10 });
        SCALE_PATTERNS.put("Iwato", new int[] { 0, 1, 5, 6, 10 });
        SCALE_PATTERNS.put("Man Gong", new int[] { 0, 2, 3, 7, 9 });

        // Microtonal and Other Exotic Scales
        SCALE_PATTERNS.put("Quarter-Tone Scale", new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
        SCALE_PATTERNS.put("Arabian Maqam Rast", new int[] { 0, 2, 4, 5, 7, 9, 11 }); // Similar to Major scale but with
                                                                                      // quarter-tones
        SCALE_PATTERNS.put("Arabian Maqam Bayati", new int[] { 0, 2, 3, 5, 7, 8, 10 });
        // SCALE_PATTERNS.put("Arabian Maqam Hijaz", new int[] { 0, 1, 4, 5, 7, 8, 10 }); // Duplicate of Hijaz
        SCALE_PATTERNS.put("Slendro", new int[] { 0, 2, 4, 7, 9 }); // Gamelan music scale
        // SCALE_PATTERNS.put("Pelog", new int[] { 0, 1, 3, 7, 10 }); // Another gamelan music scale
        SCALE_PATTERNS.put("Istrian", new int[] { 0, 1, 3, 7, 9 }); // Istrian folk music scale
        SCALE_PATTERNS.put("Balinese Pelog", new int[] { 0, 1, 3, 7, 8 }); // Balinese gamelan music scale
        // SCALE_PATTERNS.put("Balinese Slendro", new int[] { 0, 2, 5, 7, 10 }); // Duplicate of Javanese Slendro
        // SCALE_PATTERNS.put("Javanese Pelog", new int[] { 0, 1, 3, 7, 8 }); // Duplicate of Balinese Pelog
        SCALE_PATTERNS.put("Javanese Slendro", new int[] { 0, 2, 5, 7, 10 }); // Javanese gamelan music scale

        // African Scales
        SCALE_PATTERNS.put("Equidistant Pentatonic", new int[] { 0, 3, 6, 8, 11 });
        SCALE_PATTERNS.put("West African Hexatonic", new int[] { 0, 2, 3, 5, 7, 9 });
        SCALE_PATTERNS.put("Kumasi Hexatonic", new int[] { 0, 2, 4, 5, 7, 9 });
        SCALE_PATTERNS.put("Mande Heptatonic", new int[] { 0, 2, 4, 5, 7, 9, 11 });
        SCALE_PATTERNS.put("Ewe Scale", new int[] { 0, 2, 3, 5, 7, 8, 10 });
        SCALE_PATTERNS.put("Sudanese Heptatonic", new int[] { 0, 2, 3, 5, 7, 9, 10 });
        SCALE_PATTERNS.put("Ghanaian Octatonic", new int[] { 0, 1, 3, 4, 6, 7, 9, 10 });
        SCALE_PATTERNS.put("Equiheptatonic", new int[] { 0, 2, 4, 6, 8, 10, 12 }); // Approximation for microtonal
                                                                                   // systems
        SCALE_PATTERNS.put("Mbira Scale", new int[] { 0, 2, 4, 7, 9, 11, 14 });
        SCALE_PATTERNS.put("Ake Bono", new int[] { 0, 1, 5, 7, 8 });
        SCALE_PATTERNS.put("Ake Dori", new int[] { 0, 2, 5, 7, 9 });
    }

    // Generate a scale based on the root note and scale name
    public static Boolean[] getScale(String key, String scaleName) {
        int rootIndex = findRootIndex(key);
        int[] pattern = SCALE_PATTERNS.get(scaleName);
        if (rootIndex == -1 || pattern == null) {
            throw new IllegalArgumentException("Invalid key or scale name");
        }

        // Create the scale as a Boolean array
        Boolean[] scale = new Boolean[SCALE_NOTES.length];
        for (int i = 0; i < SCALE_NOTES.length; i++) {
            scale[i] = false;
        }

        // Mark the notes in the scale as true
        for (int step : pattern) {
            int noteIndex = (rootIndex + step) % SCALE_NOTES.length;
            scale[noteIndex] = true;
        }

        return scale;
    }

    // Find the index of the root note in SCALE_NOTES
    private static int findRootIndex(String key) {
        for (int i = 0; i < SCALE_NOTES.length; i++) {
            if (SCALE_NOTES[i].contains(key)) {
                return i;
            }
        }
        return -1; // Key not found
    }

    // For demonstration purposes
    public static void printScale(Boolean[] scale) {
        List<String> notes = new ArrayList<>();
        for (int i = 0; i < SCALE_NOTES.length; i++) {
            if (scale[i]) {
                notes.add(SCALE_NOTES[i]);
            }
        }
        // System.out.println(notes);
    }

    public static void main(String[] args) {
        // Example usage
        String key = "C";
        String scaleName = "Major";

        Boolean[] scale = getScale(key, scaleName);
        // System.out.println("Scale for key " + key + " in " + scaleName + ":");
        printScale(scale);

        // Try another scale
        key = "A";
        scaleName = "Natural Minor";

        scale = getScale(key, scaleName);
        // System.out.println("\nScale for key " + key + " in " + scaleName + ":");
        printScale(scale);
    }

    public static String getNoteNameWithOctave(int midiNote) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = midiNote / 12 - 1; // MIDI octaves start at -1
        int noteIndex = midiNote % 12;
        return noteNames[noteIndex] + octave;
    }

    public static int getRootOffset(String rootNote) {
        switch (rootNote) {
            case "C": return 0;
            case "C#": case "Db": return 1;
            case "D": return 2;
            case "D#": case "Eb": return 3;
            case "E": return 4;
            case "F": return 5;
            case "F#": case "Gb": return 6;
            case "G": return 7;
            case "G#": case "Ab": return 8;
            case "A": return 9;
            case "A#": case "Bb": return 10;
            case "B": return 11;
            default: return 0;
        }
    }
}

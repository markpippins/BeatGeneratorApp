package com.angrysurfer.core.util;

import com.angrysurfer.core.sequencer.Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for converting between note names and MIDI note numbers
 */
public class NoteNameConverter {
    private static final Logger logger = LoggerFactory.getLogger(NoteNameConverter.class);
    
    // Default middle C octave
    private static final int DEFAULT_OCTAVE = 4;
    // Default MIDI note (middle C = 60)
    private static final int DEFAULT_MIDI_NOTE = 60;
    
    /**
     * Convert a note name to MIDI note number
     * @param noteName The note name (e.g., "C", "F#", "C4")
     * @return The MIDI note number, or 60 (middle C) if not found
     */
    public static int noteNameToMidi(String noteName) {
        if (noteName == null || noteName.trim().isEmpty()) {
            return DEFAULT_MIDI_NOTE; // Default to middle C
        }
        
        String normalizedName = noteName.trim();
        
        // First try if it's a valid integer already
        try {
            return Integer.parseInt(normalizedName);
        } catch (NumberFormatException e) {
            // Not a direct integer, continue with name parsing
        }
        
        // Check if it includes an octave number (e.g., "C4")
        if (normalizedName.length() > 1 && Character.isDigit(normalizedName.charAt(normalizedName.length() - 1))) {
            try {
                // Extract note name and octave
                String note = normalizedName.substring(0, normalizedName.length() - 1);
                int octave = Character.getNumericValue(normalizedName.charAt(normalizedName.length() - 1));
                
                // Use Scale's utility method to get MIDI note
                return Scale.getMidiNote(note, octave);
            } catch (Exception ex) {
                logger.warn("Failed to parse note with octave: {}", normalizedName, ex);
            }
        }
        
        // If just a note name without octave is provided (e.g., "C", "F#")
        // use the default octave (middle C = C4)
        try {
            return Scale.getMidiNote(normalizedName, DEFAULT_OCTAVE);
        } catch (Exception ex) {
            logger.warn("Failed to convert note name to MIDI: {}", normalizedName, ex);
            return DEFAULT_MIDI_NOTE;
        }
    }
    
    /**
     * Convert a MIDI note number to a note name with octave
     * @param midiNote The MIDI note number
     * @return The note name with octave (e.g., "C4", "F#3")
     */
    public static String midiToNoteName(int midiNote) {
        try {
            return Scale.getNoteNameWithOctave(midiNote);
        } catch (Exception e) {
            logger.warn("Failed to convert MIDI note to name: {}", midiNote, e);
            return String.valueOf(midiNote);
        }
    }
    
    /**
     * Extract just the note name without the octave
     * @param noteNameWithOctave Note name with octave (e.g., "C4")
     * @return Just the note name (e.g., "C")
     */
    public static String extractNoteName(String noteNameWithOctave) {
        if (noteNameWithOctave == null || noteNameWithOctave.isEmpty()) {
            return "";
        }
        
        // If the last character is a digit, remove it
        if (Character.isDigit(noteNameWithOctave.charAt(noteNameWithOctave.length() - 1))) {
            return noteNameWithOctave.substring(0, noteNameWithOctave.length() - 1);
        }
        return noteNameWithOctave;
    }
    
    /**
     * Extract the octave from a note name with octave
     * @param noteNameWithOctave Note name with octave (e.g., "C4")
     * @return The octave number or DEFAULT_OCTAVE if not found
     */
    public static int extractOctave(String noteNameWithOctave) {
        if (noteNameWithOctave == null || noteNameWithOctave.isEmpty()) {
            return DEFAULT_OCTAVE;
        }
        
        // If the last character is a digit, extract it as the octave
        if (Character.isDigit(noteNameWithOctave.charAt(noteNameWithOctave.length() - 1))) {
            return Character.getNumericValue(noteNameWithOctave.charAt(noteNameWithOctave.length() - 1));
        }
        return DEFAULT_OCTAVE;
    }
    
    /**
     * Get the octave for a MIDI note number
     * @param midiNote The MIDI note number
     * @return The octave number
     */
    public static int getOctave(int midiNote) {
        return Scale.getOctave(midiNote);
    }
    
    /**
     * Transpose a note name by a number of semitones
     * @param noteName The note name (with or without octave)
     * @param semitones Number of semitones to transpose (positive or negative)
     * @return The transposed note name 
     */
    public static String transposeNoteName(String noteName, int semitones) {
        // Convert to MIDI, transpose, convert back
        int midiNote = noteNameToMidi(noteName);
        int transposedMidi = midiNote + semitones;
        
        // Clamp to valid MIDI range (0-127)
        transposedMidi = Math.max(0, Math.min(127, transposedMidi));
        
        return midiToNoteName(transposedMidi);
    }
}
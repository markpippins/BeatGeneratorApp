package com.angrysurfer.core.sequencer;

/**
 * Class to represent a note event
 */
public class NoteEvent {

    private final int note;
    private final int velocity;
    private final int durationMs;

    public NoteEvent(int note, int velocity, int durationMs) {
        this.note = note;
        this.velocity = velocity;
        this.durationMs = durationMs;
    }

    public int getNote() {
        return note;
    }

    public int getVelocity() {
        return velocity;
    }

    public int getDurationMs() {
        // If gate represents a percentage of beat duration, convert to ms
        // Assuming 100 gate = 250ms at 120bpm (500ms per beat)
        return (int)(durationMs * 2.5); // Simple approximation
    }
}
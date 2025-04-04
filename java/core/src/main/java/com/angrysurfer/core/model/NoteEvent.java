package com.angrysurfer.core.model;

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
        return durationMs;
    }
}
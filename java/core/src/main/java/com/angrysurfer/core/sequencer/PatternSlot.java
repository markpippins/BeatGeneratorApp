package com.angrysurfer.core.sequencer;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Represents a pattern slot in a song arrangement
 */
@Getter
@Setter
public class PatternSlot implements Serializable {
    // Getters and setters
    private Long id;
    private Long patternId;
    private int position;  // Position in the song (measured in bars)
    private int length;    // Length of the pattern (in bars)
    private String type;   // "DRUM" or "MELODIC"
    private Integer sequencerId;  // Used for melodic patterns to identify which sequencer

    public PatternSlot() {
        // Default constructor
    }

    public PatternSlot(Long patternId, int position, int length, String type) {
        this.patternId = patternId;
        this.position = position;
        this.length = length;
        this.type = type;
    }

    public PatternSlot(Long patternId, int position, int length, String type, Integer sequencerId) {
        this(patternId, position, length, type);
        this.sequencerId = sequencerId;
    }
}
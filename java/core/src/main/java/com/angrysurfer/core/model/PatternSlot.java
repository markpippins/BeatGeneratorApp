package com.angrysurfer.core.model;

import java.io.Serializable;

/**
 * Represents a pattern slot in a song arrangement
 */
public class PatternSlot implements Serializable {
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
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPatternId() {
        return patternId;
    }
    
    public void setPatternId(Long patternId) {
        this.patternId = patternId;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Integer getSequencerId() {
        return sequencerId;
    }
    
    public void setSequencerId(Integer sequencerId) {
        this.sequencerId = sequencerId;
    }
}
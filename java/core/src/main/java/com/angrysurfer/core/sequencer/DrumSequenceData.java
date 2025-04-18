package com.angrysurfer.core.sequencer;

import java.io.Serializable;

import com.angrysurfer.core.model.Direction;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for persisting drum sequence data to Redis
 */
@Getter
@Setter
public class DrumSequenceData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Sequence identifier
    private Long id;
    
    // Optional name/description
    private String name;
    
    // Global pattern length - NEW FIELD
    private int patternLength;
    
    // Pattern parameters per drum
    private int[] patternLengths;         // Length of pattern for each drum
    private Direction[] directions;        // Direction for each drum
    private TimingDivision[] timingDivisions; // Timing division for each drum
    private boolean[] loopingFlags;        // Whether each drum's pattern loops
    
    // Velocity data
    private int[] velocities;              // Current velocity for each drum
    private int[] originalVelocities;      // Original/saved velocities
    
    // The actual step patterns
    private boolean[][] patterns;          // Step patterns for each drum
    
    public DrumSequenceData() {
        // Default constructor
    }
}
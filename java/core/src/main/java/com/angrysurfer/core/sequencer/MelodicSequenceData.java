package com.angrysurfer.core.sequencer;

import java.io.Serializable;
import java.util.List;

import com.angrysurfer.core.model.Direction;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for serializing melodic sequence data
 */
@Getter
@Setter
public class MelodicSequenceData implements Serializable {
    private Long id;
    private Integer sequencerId;
    
    // Sequence parameters
    private int patternLength;
    private Direction direction;
    private TimingDivision timingDivision;
    private boolean looping;
    private int octaveShift;
    
    // Quantization parameters
    private boolean quantizeEnabled;
    private String rootNote;
    private String scale;
    
    // Pattern data
    private List<Boolean> activeSteps;
    private List<Integer> noteValues;
    private List<Integer> velocityValues;
    private List<Integer> gateValues;
    private List<Integer> probabilityValues;
    private List<Integer> nudgeValues;
    
    // Tilt panel data - store the harmonic tilt values for each bar
    private List<Integer> harmonicTiltValues;
    
    // Constructor
    public MelodicSequenceData() {
        // Default constructor
    }
}
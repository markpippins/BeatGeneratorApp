package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatternStatus {

    private Long pattern;
    private Integer position;
    private Long activeStep;
    private Integer length;
    private Integer firstStep;
    private Integer lastStep;
    private Integer direction;
    private Boolean muted;

    public static PatternStatus from(Pattern pattern) {
        PatternStatus result = new PatternStatus();
        result.setPattern(pattern.getId());
        result.setActiveStep(pattern.getStepCycler().get());
        result.setLength(pattern.getLength());
        result.setFirstStep(pattern.getFirstStep());
        result.setLastStep(pattern.getLastStep());
        result.setDirection(pattern.getDirection());
        result.setMuted(pattern.getMuted());
        result.setPosition(pattern.getPosition());

        return result;
    }
}

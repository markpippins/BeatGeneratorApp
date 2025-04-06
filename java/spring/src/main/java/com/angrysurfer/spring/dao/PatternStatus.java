package com.angrysurfer.spring.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatternStatus {

    static Logger logger = LoggerFactory.getLogger(PatternStatus.class.getCanonicalName());

    private Long pattern;
    private Integer position;
    private Integer activeStep;
    private Integer length;
    private Integer firstStep;
    private Integer lastStep;
    private Integer direction;
    private Boolean muted;

    public static PatternStatus from(Pattern pattern) {
        PatternStatus result = new PatternStatus();
        result.setPattern(pattern.getId());
        result.setActiveStep(pattern.getStepCycler().getPosition().get());
        result.setLength(pattern.getLength());
        result.setFirstStep(pattern.getFirstStep());
        result.setLastStep(pattern.getLastStep());
        result.setDirection(pattern.getDirection());
        result.setMuted(pattern.getMuted());
        result.setPosition(pattern.getPosition());
        logger.info(String.format("pattern %s, step %s", pattern.getPosition(), pattern.getStepCycler().get()));
        return result;
    }
}

package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Song {
    private static final Logger logger = LoggerFactory.getLogger(Song.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;

    @Transient
    private Float beatDuration;

    @Transient
    private Integer ticksPerBeat;

    @Transient
    private Set<Pattern> patterns = new HashSet<>();

    public Pattern getPattern(Long patternId) {
        logger.debug("getPattern() - looking for patternId: {}", patternId);
        Pattern[] result = { null };

        Optional<Pattern> opt = getPatterns().stream().filter(p -> p.getId().equals(patternId)).findAny();
        if (opt.isPresent()) {
            logger.debug("Found pattern {}", patternId);
            result[0] = opt.get();
        }

        if (result[0] == null)
            logger.debug("Pattern {} not found", patternId);

        return result[0];
    }

    public Step getStep(Long stepId) {
        logger.debug("getStep() - looking for stepId: {}", stepId);
        Step[] result = { null };
        getPatterns().forEach(pattern -> {
            Optional<Step> step = pattern.getSteps().stream().filter(s -> s.getId().equals(stepId)).findAny();
            if (step.isPresent()) {
                logger.debug("Found step {} in pattern {}", stepId, pattern.getName());
                result[0] = step.get();
            }
        });

        if (result[0] == null) {
            logger.debug("Step {} not found", stepId);
        }
        return result[0];
    }

}

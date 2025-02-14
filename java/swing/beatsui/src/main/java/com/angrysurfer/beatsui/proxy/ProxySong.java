package com.angrysurfer.beatsui.proxy;

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
public class ProxySong {
    private static final Logger logger = LoggerFactory.getLogger(ProxySong.class);

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
    private Set<ProxyPattern> patterns = new HashSet<>();


    public ProxyStep getStep(Long stepId) {
        logger.debug("getStep() - looking for stepId: {}", stepId);
        ProxyStep[] result = {null};
        getPatterns().forEach(pattern -> {
            Optional<ProxyStep> step = pattern.getSteps().stream().filter(s -> s.getId().equals(stepId)).findAny();
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

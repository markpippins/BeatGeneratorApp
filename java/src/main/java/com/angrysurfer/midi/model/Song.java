package com.angrysurfer.midi.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Song {
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


    public Step getStep(Long stepId) {
        
        Step[] result = {null};
        getPatterns().forEach(pattern -> {
            Optional<Step> step = pattern.getSteps().stream().filter(s -> s.getId().equals(stepId)).findAny();
            if (step.isPresent())
                result[0] = step.get();
        });

        return result[0];
    }

}

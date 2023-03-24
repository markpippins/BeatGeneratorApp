package com.angrysurfer.midi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Step {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private int position;

    private Integer page;

    private Long songId;

    boolean active;
    
    int pitch;

    int velocity;

    int probability;

    int gate;

    public void copyValues(Step step) {
        setId(step.getId());
        setPosition(step.getPosition());
        setPage(step.getPage());
        setSongId(step.getSongId());
        setActive(step.isActive());
        setPitch(step.getPitch()); 
        setVelocity(step.getVelocity());
        setProbability(step.getProbability());
        setGate(step.getGate()); 
    }
}

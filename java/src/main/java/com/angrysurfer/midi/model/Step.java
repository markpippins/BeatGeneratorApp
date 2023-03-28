package com.angrysurfer.midi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    private Integer position;

    private Integer page = 0;

    private Integer channel = 0;

    private Boolean active = false;
    
    private Integer pitch = 60;

    private Integer velocity = 100;

    private Integer probability = 100;

    private Integer gate = 50;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "song_id")
    private Song song;

    public Step() {

    }

    public void copyValues(Step step) {
        setId(step.getId());
        setPosition(step.getPosition());
        setPage(step.getPage());
        setSong(step.getSong());
        setActive(step.getActive());
        setPitch(step.getPitch()); 
        setVelocity(step.getVelocity());
        setProbability(step.getProbability());
        setGate(step.getGate()); 
    }
}

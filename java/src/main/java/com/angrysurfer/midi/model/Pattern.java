package com.angrysurfer.midi.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Pattern {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private Integer position = 0;

    private Integer channel = 0;

    private Integer scale = 0;

    private Boolean active = false;
    
    private Integer probability = 100;

    private Integer direction = 1;

    private Integer length = 16;

    private Integer lastStep = 16;

    @Column(name = "rand")
    private Integer random = 0;

    private Integer baseNote = 60;

    private Integer transpose = 0;
    
    private Boolean oneShot = false;

    private Integer loopCount = 99;

    private Integer swing = 50;

    private Integer gate = 50;

    private Integer preset = 0;

    @Column(name = "delay_bars")
    private Integer delay = 0;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "song_id")
    private Song song;

    @Transient
    Set<Step> steps = new HashSet<>();

    @JsonIgnore
    @Transient
    @JoinColumn(name = "instrument_id")
    private MidiInstrument instrument;

    public Pattern() {

    }

    // public void copyValues(Pattern step) {
    //     setId(step.getId());
    //     setPosition(step.getPosition());
    //     setPage(step.getPage());
    //     setSong(step.getSong());
    //     setActive(step.getActive());
    //     setPitch(step.getPitch()); 
    //     setVelocity(step.getVelocity());
    //     setProbability(step.getProbability());
    //     setGate(step.getGate()); 
    // }
}

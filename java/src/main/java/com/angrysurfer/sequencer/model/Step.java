package com.angrysurfer.sequencer.model;

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

    private Integer position = 0;

    private Boolean active = false;
    
    private Integer pitch = 0;

    private Integer velocity = 100;

    private Integer probability = 100;

    private Integer gate = 50;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "pattern_id")
    private Pattern pattern;

    public Step() {

    }

}

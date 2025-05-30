package com.angrysurfer.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "step")
public class Step {

    private static final Logger logger = LoggerFactory.getLogger(Step.class);

    @Id
    private Long id;

    private Integer position;

    private Boolean active = false;
    
    private Integer pitch = 0;

    private Integer velocity = 100;

    private Integer probability = 100;

    private Integer gate = 50;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_id")
    private Pattern pattern;

    public Step() {
        logger.debug("Creating new Step");
    }

    public void setActive(Boolean active) {
        logger.debug("setActive() - value: {}", active);
        this.active = active;
    }

    public void setPitch(Integer pitch) {
        logger.debug("setPitch() - value: {}", pitch);
        this.pitch = pitch;
    }

}

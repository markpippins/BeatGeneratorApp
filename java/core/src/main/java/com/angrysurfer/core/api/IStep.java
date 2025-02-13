package com.angrysurfer.core.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IStep {

    Long getId();

    Integer getPosition();

    Boolean getActive();

    Integer getPitch();

    Integer getVelocity();

    Integer getProbability();

    Integer getGate();

    IPattern getPattern();

    void setId(Long id);

    void setPosition(Integer position);

    void setVelocity(Integer velocity);

    void setProbability(Integer probability);

    void setGate(Integer gate);

    @JsonIgnore
    void setPattern(IPattern pattern);

    void setActive(Boolean active);

    void setPitch(Integer pitch);

}
package com.angrysurfer.core.api;

import java.util.Set;
import java.util.Stack;

import com.angrysurfer.core.util.Cycler;
import com.angrysurfer.core.util.Quantizer;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IPattern {

    Long getId();

    String getName();

    Integer getPosition();

    Integer getChannel();

    Integer getScale();

    Boolean getActive();

    Boolean getQuantize();

    Integer getDirection();

    Integer getLength();

    Integer getFirstStep();

    Integer getLastStep();

    Integer getSpeed();

    Integer getRandom();

    Integer getRootNote();

    Integer getTranspose();

    Integer getRepeats();

    Integer getSwing();

    Integer getGate();

    Integer getPreset();

    Boolean getMuted();

    Boolean getLoop();

    Integer getBeatDivider();

    boolean isUnsaved();

    Quantizer getQuantizer();

    Stack<Integer> getPlayingNote();

    Integer getDelay();

    ISong getSong();

    Set<IStep> getSteps();

    IInstrument getInstrument();

    Integer getStepCyclerPosition();

    Cycler getStepCycler();

    void setId(Long id);

    void setName(String name);

    void setPosition(Integer position);

    void setChannel(Integer channel);

    void setScale(Integer scale);

    void setActive(Boolean active);

    void setLength(Integer length);

    void setFirstStep(Integer firstStep);

    void setLastStep(Integer lastStep);

    void setRandom(Integer random);

    void setRootNote(Integer rootNote);

    void setTranspose(Integer transpose);

    void setRepeats(Integer repeats);

    void setSwing(Integer swing);

    void setGate(Integer gate);

    void setPreset(Integer preset);

    void setMuted(Boolean muted);

    void setLoop(Boolean loop);

    void setBeatDivider(Integer beatDivider);

    @JsonIgnore
    void setUnsaved(boolean unsaved);

    @JsonIgnore
    void setQuantizer(Quantizer quantizer);

    @JsonIgnore
    void setPlayingNote(Stack<Integer> playingNote);

    void setDelay(Integer delay);

    @JsonIgnore
    void setSong(ISong song);

    void setSteps(Set<IStep> steps);

    void setInstrument(IInstrument instrument);

    @JsonIgnore
    void setStepCyclerPosition(Integer stepCyclerPosition);

    @JsonIgnore
    void setStepCycler(Cycler stepCycler);

    void setQuantize(Boolean quantize);

    void setDirection(Integer direction);

    void setSpeed(Integer speed);

}
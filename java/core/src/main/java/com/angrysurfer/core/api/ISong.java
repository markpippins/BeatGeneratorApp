package com.angrysurfer.core.api;

import java.util.Set;

public interface ISong {

    Long getId();

    String getName();

    Float getBeatDuration();

    Integer getTicksPerBeat();

    Set<IPattern> getPatterns();

    void setId(Long id);

    void setName(String name);

    void setBeatDuration(Float beatDuration);

    void setTicksPerBeat(Integer ticksPerBeat);

    void setPatterns(Set<IPattern> patterns);

    IStep getStep(Long stepId);

}
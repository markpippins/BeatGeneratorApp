package com.angrysurfer.core.api;

import java.util.List;
import java.util.Set;

public interface IPad {

    Long getId();

    Integer getNote();

    List<Integer> getControlCodes();

    String getName();

    Set<IInstrument> getInstruments();

    void setId(Long id);

    void setNote(Integer note);

    void setControlCodes(List<Integer> controlCodes);

    void setName(String name);

    void setInstruments(Set<IInstrument> instruments);

}
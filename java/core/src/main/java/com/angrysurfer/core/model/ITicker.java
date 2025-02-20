package com.angrysurfer.core.model;

import java.util.List;
import java.util.concurrent.Callable;

public interface ITicker {

    Long getId();

    List<Callable<Boolean>> getCallables();

    double getBeat();

    Long getBeatCount();

    void setBeatsPerBar(int beatsPerBar);

    Long getTick();

    Long getTickCount();

    void setTicksPerBeat(int ticksPerBeat);

    Long getBar();

    Long getBarCount();

    void setBars(int bars);

    Long getPart();

    Long getPartCount();

    void setParts(int parts);

    void setPartLength(long partLength);

    void reset();

    double getBeatDivision();

    void afterEnd();

    void beforeStart();

    void onStart();

    void onStop();

    void beforeTick();

    void afterTick();

    void onBeatChange();

    void onBarChange();

    void onPartChange();

    Float getBeatDuration();

    double getInterval();

    Boolean hasSolos();

    boolean isRunning();

    Integer getBars();

    Integer getBeatsPerBar();

    Integer getBeatDivider();

    Long getPartLength();

    Integer getMaxTracks();

    Long getSongLength();

    Long getSwing();

    Integer getTicksPerBeat();

    Float getTempoInBPM();

    Integer getLoopCount();

    Integer getParts();

    Double getNoteOffset();

    void setBars(Integer bars);

    void setBeatsPerBar(Integer beatsPerBar);

    void setBeatDivider(Integer beatDivider);

    void setMaxTracks(Integer maxTracks);

    void setSongLength(Long songLength);

    void setSwing(Long swing);

    void setTempoInBPM(Float tempoInBPM);

    void setLoopCount(Integer loopCount);

    void setParts(Integer parts);

    void setNoteOffset(Double noteOffset);

}

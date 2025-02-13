package com.angrysurfer.core.api;

import java.util.Set;

import com.angrysurfer.core.model.MuteGroup;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ITicker {

    ClockSource getClockSource();

    Set<IPlayer> getRemoveList();

    Cycler getBeatCycler();

    Cycler getBeatCounter();

    Cycler getBarCycler();

    Cycler getBarCounter();

    Cycler getPartCycler();

    Cycler getPartCounter();

    Cycler getTickCycler();

    Cycler getTickCounter();

    boolean isDone();

    Long getId();

    Set<IPlayer> getPlayers();

    Set<Long> getActivePlayerIds();

    double getGranularBeat();

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

    boolean isPaused();

    Set<MuteGroup> getMuteGroups();

    @JsonIgnore
    void setClockSource(ClockSource clockSource);

    @JsonIgnore
    void setRemoveList(Set<IPlayer> removeList);

    @JsonIgnore
    void setBeatCycler(Cycler beatCycler);

    @JsonIgnore
    void setBeatCounter(Cycler beatCounter);

    @JsonIgnore
    void setBarCycler(Cycler barCycler);

    @JsonIgnore
    void setBarCounter(Cycler barCounter);

    @JsonIgnore
    void setPartCycler(Cycler partCycler);

    @JsonIgnore
    void setPartCounter(Cycler partCounter);

    @JsonIgnore
    void setTickCycler(Cycler tickCycler);

    @JsonIgnore
    void setTickCounter(Cycler tickCounter);

    void setDone(boolean done);

    void setId(Long id);

    void setPlayers(Set<IPlayer> players);

    void setActivePlayerIds(Set<Long> activePlayerIds);

    void setGranularBeat(double granularBeat);

    void setBeatDivider(Integer beatDivider);

    void setMaxTracks(Integer maxTracks);

    void setSongLength(Long songLength);

    void setSwing(Long swing);

    void setTempoInBPM(Float tempoInBPM);

    void setLoopCount(Integer loopCount);

    void setNoteOffset(Double noteOffset);

    void setPaused(boolean paused);

    @JsonIgnore
    void setMuteGroups(Set<MuteGroup> muteGroups);

    IPlayer getPlayer(Long playerId);

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

}
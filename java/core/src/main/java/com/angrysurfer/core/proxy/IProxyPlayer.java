package com.angrysurfer.core.proxy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;


import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IProxyPlayer extends Callable<Boolean> {

    Set<ProxyPad> getPads();

    String getName();

    int getChannel();

    Long getSwing();

    Long getLevel();

    Long getNote();

    Long getMinVelocity();

    Long getMaxVelocity();

    Long getPreset();

    Boolean getStickyPreset();

    Long getProbability();

    Long getRandomDegree();

    Long getRatchetCount();

    Long getRatchetInterval();

    Integer getInternalBars();

    Integer getInternalBeats();

    Boolean getUseInternalBeats();

    Boolean getUseInternalBars();

    Long getPanPosition();

    Boolean getPreserveOnPurge();

    double getSparse();

    Cycler getSkipCycler();

    Cycler getSubCycler();

    Cycler getBeatCycler();

    Cycler getBarCycler();

    boolean isSolo();

    boolean isMuted();

    Long getPosition();

    Long getLastTick();

    Long getLastPlayedTick();

    Long getLastPlayedBar();

    Long getSkips();

    double getLastPlayedBeat();

    Long getSubDivisions();

    Long getBeatFraction();

    Long getFadeOut();

    Long getFadeIn();

    Boolean getAccent();

    boolean isUnsaved();

    Boolean getArmForNextTick();

    Set<ProxyRule> getRules();

    List<Integer> getAllowedControlMessages();

    ProxyInstrument getInstrument();

    ProxyTicker getTicker();

    Long getId();

    void setPads(Set<ProxyPad> pads);

    void setName(String name);

    void setChannel(int channel);

    void setSwing(Long swing);

    void setLevel(Long level);

    void setNote(Long note);

    void setMinVelocity(Long minVelocity);

    void setMaxVelocity(Long maxVelocity);

    void setPreset(Long preset);

    void setStickyPreset(Boolean stickyPreset);

    void setProbability(Long probability);

    void setRandomDegree(Long randomDegree);

    void setRatchetCount(Long ratchetCount);

    void setRatchetInterval(Long ratchetInterval);

    void setInternalBars(Integer internalBars);

    void setInternalBeats(Integer internalBeats);

    void setUseInternalBeats(Boolean useInternalBeats);

    void setUseInternalBars(Boolean useInternalBars);

    void setPanPosition(Long panPosition);

    void setPreserveOnPurge(Boolean preserveOnPurge);

    void setSparse(double sparse);

    @JsonIgnore
    void setSkipCycler(Cycler skipCycler);

    @JsonIgnore
    void setSubCycler(Cycler subCycler);

    @JsonIgnore
    void setBeatCycler(Cycler beatCycler);

    @JsonIgnore
    void setBarCycler(Cycler barCycler);

    void setSolo(boolean solo);

    void setMuted(boolean muted);

    void setPosition(Long position);

    void setLastTick(Long lastTick);

    void setLastPlayedTick(Long lastPlayedTick);

    void setLastPlayedBar(Long lastPlayedBar);

    void setSkips(Long skips);

    void setLastPlayedBeat(double lastPlayedBeat);

    void setSubDivisions(Long subDivisions);

    void setBeatFraction(Long beatFraction);

    void setFadeOut(Long fadeOut);

    void setFadeIn(Long fadeIn);

    void setAccent(Boolean accent);

    @JsonIgnore
    void setUnsaved(boolean unsaved);

    @JsonIgnore
    void setArmForNextTick(Boolean armForNextTick);

    void setRules(Set<ProxyRule> rules);

    void setAllowedControlMessages(List<Integer> allowedControlMessages);

    @JsonIgnore
    void setTicker(ProxyTicker ticker);

    void setId(Long id);

    String getPlayerClass();

    Long getSubPosition();

    void setInstrument(ProxyInstrument instrument);

    void onTick(long tick, long bar);

    Long getInstrumentId();

    ProxyRule getRule(Long ruleId);

    void drumNoteOn(long note);

    void noteOn(long note, long velocity);

    void noteOff(long note, long velocity);

    Boolean call();

    boolean isProbable();

    boolean shouldPlay();

}
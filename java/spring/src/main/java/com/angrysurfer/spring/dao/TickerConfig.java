package com.angrysurfer.spring.dao;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.core.api.IPlayer;
import com.angrysurfer.core.api.ITicker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerConfig implements Serializable {
    Set<IPlayer> players = new HashSet<>();
    private Float tempoInBPM;
    private Long songLength;
    private Integer ticksPerBeat;
    private Integer beatsPerBar;
    private Long swing;
    private Integer beatDivider;
    private Integer maxTracks;
    private Long partLength;
    
    public TickerConfig() {
    }

    public TickerConfig(ITicker ticker, Set<IPlayer> players) {
        setTicksPerBeat(ticker.getTicksPerBeat());
        setBeatsPerBar(ticker.getBeatsPerBar());
        setBeatDivider(ticker.getBeatDivider());
        setTempoInBPM(ticker.getTempoInBPM());
        setMaxTracks(ticker.getMaxTracks());
        setPartLength(ticker.getPartLength());
        setSongLength(ticker.getSongLength());
        setSwing(ticker.getSwing());
        ticker.setPlayers(players);
    }

    public void setup(ITicker ticker) {
        ticker.setTicksPerBeat(getTicksPerBeat());
        ticker.setBeatsPerBar(getBeatsPerBar());
        ticker.setBeatDivider(getBeatDivider());
        ticker.setTempoInBPM(getTempoInBPM());
        ticker.setMaxTracks(getMaxTracks());
        ticker.setPartLength(getPartLength());
        ticker.setSongLength(getSongLength());
        ticker.setSwing(getSwing());
    }

}

package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Strike;
import com.angrysurfer.midi.model.Ticker;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BeatGeneratorConfig implements Serializable {
    List<StrikeInfo> players = new ArrayList<>();
    private int tempoInBPM;
    private int songLength;
    private int ticksPerBeat;
    private int beatsPerBar;
    private int swing;
    private double beatDivider;
    private int maxTracks;
    private int partLength;

    public BeatGeneratorConfig() {
    }

    public BeatGeneratorConfig(Ticker ticker, List<Strike> players) {
        setTicksPerBeat(ticker.getTicksPerBeat());
        setBeatsPerBar(ticker.getBeatsPerBar());
        setBeatDivider(ticker.getBeatDivider());
        setTempoInBPM(ticker.getTempoInBPM());
        setMaxTracks(ticker.getMaxTracks());
        setPartLength(ticker.getPartLength());
        setSongLength(ticker.getSongLength());
        setSwing(ticker.getSwing());
        setPlayers(players.stream().map(StrikeInfo::fromStrike).toList());
    }

    public void setup(Ticker ticker) {
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

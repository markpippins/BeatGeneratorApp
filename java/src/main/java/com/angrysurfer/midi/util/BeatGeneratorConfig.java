package com.angrysurfer.midi.util;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Strike;
import com.angrysurfer.midi.model.Ticker;

@Getter
@Setter
public class BeatGeneratorConfig implements Serializable {
    Set<Player> players = new HashSet<>();
    private float tempoInBPM;
    private int songLength;
    private int ticksPerBeat;
    private int beatsPerBar;
    private int swing;
    private double beatDivider;
    private int maxTracks;
    private int partLength;

    public BeatGeneratorConfig() {
    }

    public BeatGeneratorConfig(Ticker ticker, Set<Player> players) {
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

    public void setup(Ticker ticker) {
        ticker.setTicksPerBeat(getTicksPerBeat());
        ticker.setBeatsPerBar(getBeatsPerBar());
        ticker.setBeatDivider(getBeatDivider());
        ticker.setTempoInBPM((float) getTempoInBPM());
        ticker.setMaxTracks(getMaxTracks());
        ticker.setPartLength(getPartLength());
        ticker.setSongLength(getSongLength());
        ticker.setSwing(getSwing());
    }
}

package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.model.Strike;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BeatGeneratorConfig implements Serializable {
    List<StrikeInfo> players = new ArrayList<>();
    private int delay;
    private int songLength;
    private int barLengthInTicks;
    private int swing;
    private double beatDivider;
    private int maxTracks;
    private int partLength;

    public BeatGeneratorConfig() {
    }

    public BeatGeneratorConfig(Ticker ticker, List<Strike> players) {
        setBarLengthInTicks(ticker.getBarLengthInTicks());
        setBeatDivider(ticker.getBeatDivider());
        setDelay(ticker.getDelay());
        setMaxTracks(ticker.getMaxTracks());
        setPartLength(ticker.getPartLength());
        setSongLength(ticker.getSongLength());
        setSwing(ticker.getSwing());
        setPlayers(players.stream().map(StrikeInfo::fromDrumPad).toList());
    }

    public void setup(Ticker ticker) {
        ticker.setBarLengthInTicks(getBarLengthInTicks());
        ticker.setBeatDivider(getBeatDivider());
        ticker.setDelay(getDelay());
        ticker.setMaxTracks(getMaxTracks());
        ticker.setPartLength(getPartLength());
        ticker.setSongLength(getSongLength());
        ticker.setSwing(getSwing());
    }
}

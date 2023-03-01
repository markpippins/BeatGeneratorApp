package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.MuteGroupList;
import com.angrysurfer.midi.model.Ticker;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class TickerInfo {
    static Logger logger = LoggerFactory.getLogger(TickerInfo.class.getCanonicalName());
    public boolean done;
    private int bar;
    private long tick;
    private int ticksPerBeat;
    private int beatsPerBar;
    private int beat;
    private double beatDivider;
    private int tempoInBPM;
    private int partLength;
    private int maxTracks;
    private int songLength;
    private int swing;
    private boolean playing;
    private boolean stopped;
    private MuteGroupList muteGroups;

    public static TickerInfo fromTicker(Ticker ticker) {
        TickerInfo info = new TickerInfo();

        info.setBar(ticker.getBar());
        info.setBeat((int) ticker.getBeat());
        info.setDone(ticker.isDone());
        info.setTempoInBPM(ticker.getTempoInBPM());
        info.setBeatDivider(ticker.getBeatDivider());
        info.setMaxTracks(ticker.getMaxTracks());
        info.setPlaying(ticker.isPlaying());
        info.setSwing(ticker.getSwing());
        info.setStopped(ticker.isStopped());
        info.setSongLength(ticker.getSongLength());
        info.setPartLength(ticker.getPartLength());
        info.setTicksPerBeat(ticker.getTicksPerBeat());
        info.setPartLength(ticker.getPartLength());
        info.setBeatsPerBar(ticker.getBeatsPerBar());
        info.setTick(ticker.getTick());

        return info;
    }

}

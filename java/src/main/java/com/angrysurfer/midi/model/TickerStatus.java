package com.angrysurfer.midi.model;

import lombok.Data;

@Data
public class TickerStatus {
    private Long id; 
    private Long tick;
    private Double beat;
    private Long bar;
    private Long part;

    private Long tickCount;
    private Long beatCount;
    private Long barCount;
    private Long partCount;

    private Integer bars;
    private Integer beatsPerBar;
    private Integer beatDivider;
    private Long partLength;
    private Integer maxTracks;
    private Long songLength;
    private Long swing;
    private Integer ticksPerBeat;
    private Float tempoInBPM;
    private Integer loopCount;
    private Integer parts;
    private Double noteOffset;
    private Boolean playing;

    public TickerStatus(Ticker ticker, boolean isPlaying) {
        setId(ticker.getId());
        setTick(ticker.getTick());
        setBeat(ticker.getBeat());
        setBar(ticker.getBar());
        setPart(ticker.getPart());
        setBars(ticker.getBars());
        setParts(ticker.getParts());
        setMaxTracks(ticker.getMaxTracks());
        setTempoInBPM(ticker.getTempoInBPM());
        setBeatDivider(ticker.getBeatDivider());
        setBeatsPerBar(ticker.getBeatsPerBar());
        setLoopCount(ticker.getLoopCount());
        setNoteOffset(ticker.getNoteOffset());
        setPartLength(ticker.getPartLength());
        setSongLength(ticker.getSongLength());
        setTicksPerBeat(ticker.getTicksPerBeat());
        setSwing(ticker.getSwing());
        setPlaying(isPlaying);
        setTickCount(ticker.getTickCount());
        setBeatCount(ticker.getBeatCount());
        setBarCount(ticker.getBarCount());
        setPartCount(ticker.getPartCount());
    } 
}

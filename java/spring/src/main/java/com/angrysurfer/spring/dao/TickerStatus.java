package com.angrysurfer.spring.dao;

import java.io.Serializable;
import java.util.ArrayList;

import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Ticker;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// @RedisHash("TickerStatus")
public class TickerStatus implements Serializable {

    @Id
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
    private Boolean hasSolos;

    private Integer playerCount;

    ArrayList<PatternStatus> patternStatuses = new ArrayList<>();

    public static TickerStatus from(Ticker ticker, Song song, boolean isPlaying) {

        TickerStatus result = new TickerStatus();

        result.setId(ticker.getId());
        result.setTick(ticker.getTick());
        result.setBeat(ticker.getBeat());
        result.setBar(ticker.getBar());
        result.setPart(ticker.getPart());
        result.setBars(ticker.getBars());
        result.setParts(ticker.getParts());
        result.setMaxTracks(ticker.getMaxTracks());
        result.setTempoInBPM(ticker.getTempoInBPM());
        result.setBeatDivider(ticker.getBeatDivider());
        result.setBeatsPerBar(ticker.getBeatsPerBar());
        result.setLoopCount(ticker.getLoopCount());
        result.setNoteOffset(ticker.getNoteOffset());
        result.setPartLength(ticker.getPartLength());
        result.setSongLength(ticker.getSongLength());
        result.setTicksPerBeat(ticker.getTicksPerBeat());
        result.setSwing(ticker.getSwing());
        result.setPlaying(isPlaying);
        result.setTickCount(ticker.getTickCount());
        result.setBeatCount(ticker.getBeatCount());
        result.setBarCount(ticker.getBarCount());
        result.setPartCount(ticker.getPartCount());
        result.setPlayerCount(ticker.getPlayers().size());
        result.setHasSolos(ticker.hasSolos());
        song.getPatterns().forEach(p -> result.patternStatuses.add(PatternStatus.from(p)));

        return result;
    }
}

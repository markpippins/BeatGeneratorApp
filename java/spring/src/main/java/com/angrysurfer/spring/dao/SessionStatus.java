package com.angrysurfer.spring.dao;

import java.io.Serializable;
import java.util.ArrayList;

import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Session;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// @RedisHash("SessionStatus")
public class SessionStatus implements Serializable {

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
    private Integer partLength;
    private Integer maxTracks;
    private Long songLength;
    private Long swing;
    private Integer ticksPerBeat;
    private Float tempoInBPM;
    private Integer loopCount;
    private Integer parts;
    private Integer noteOffset;
    private Boolean playing;
    private Boolean hasSolos;

    private Integer playerCount;

    ArrayList<PatternStatus> patternStatuses = new ArrayList<>();

    public static SessionStatus from(Session session, Song song, boolean isPlaying) {

        SessionStatus result = new SessionStatus();

        result.setId(session.getId());
        result.setTick(session.getTick());
        result.setBeat(session.getBeat());
        result.setBar(session.getBar());
        result.setPart(session.getPart());
        result.setBars(session.getBars());
        result.setParts(session.getParts());
        result.setMaxTracks(session.getMaxTracks());
        result.setTempoInBPM(session.getTempoInBPM());
        result.setBeatDivider(session.getBeatDivider());
        result.setBeatsPerBar(session.getBeatsPerBar());
        result.setLoopCount(session.getLoopCount());
        result.setNoteOffset(session.getNoteOffset());
        result.setPartLength(session.getPartLength());
        result.setSongLength(session.getSongLength());
        result.setTicksPerBeat(session.getTicksPerBeat());
        result.setSwing(session.getSwing());
        result.setPlaying(isPlaying);
        result.setTickCount(session.getTickCount());
        result.setBeatCount(session.getBeatCount());
        result.setBarCount(session.getBarCount());
        result.setPartCount(session.getPartCount());
        result.setPlayerCount(session.getPlayers().size());
        result.setHasSolos(session.hasSolos());
        song.getPatterns().forEach(p -> result.patternStatuses.add(PatternStatus.from(p)));

        return result;
    }
}

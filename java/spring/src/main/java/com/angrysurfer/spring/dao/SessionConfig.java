package com.angrysurfer.spring.dao;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionConfig implements Serializable {
    Set<Player> players = new HashSet<>();
    private Float tempoInBPM;
    private Long songLength;
    private Integer ticksPerBeat;
    private Integer beatsPerBar;
    private Long swing;
    private Integer beatDivider;
    private Integer maxTracks;
    private Integer partLength;

    public SessionConfig() {
    }

    public SessionConfig(Session session, Set<Player> players) {
        setTicksPerBeat(session.getTicksPerBeat());
        setBeatsPerBar(session.getBeatsPerBar());
        setBeatDivider(session.getBeatDivider());
        setTempoInBPM(session.getTempoInBPM());
        setMaxTracks(session.getMaxTracks());
        setPartLength(session.getPartLength());
        setSongLength(session.getSongLength());
        setSwing(session.getSwing());
        session.setPlayers(players);
    }

    public void setup(Session session) {
        session.setTicksPerBeat(getTicksPerBeat());
        session.setBeatsPerBar(getBeatsPerBar());
        session.setBeatDivider(getBeatDivider());
        session.setTempoInBPM(getTempoInBPM());
        session.setMaxTracks(getMaxTracks());
        session.setPartLength(getPartLength());
        session.setSongLength(getSongLength());
        session.setSwing(getSwing());
    }

}

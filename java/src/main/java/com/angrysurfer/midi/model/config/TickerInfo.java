package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.MuteGroupList;
import com.angrysurfer.midi.model.Strike;
import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.service.IMidiInstrument;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
public class TickerInfo {
    static Logger logger = LoggerFactory.getLogger(TickerInfo.class.getCanonicalName());
    public boolean done;
//    @OneToMany(fetch = FetchType.EAGER)
//    @JoinTable(name = "ticker_player", joinColumns = {@JoinColumn(name = "ticker_id")}, inverseJoinColumns = {
//            @JoinColumn(name = "player_id")})
    @Transient
    Set<PlayerInfo> players = new HashSet<>();
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

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
    @Transient
    private MuteGroupList muteGroups;

    public TickerInfo() {
        setBeat(0);
        setBar(0);
        setTick(0);
        setDone(false);
        setPlaying(false);
        setStopped(false);
        setId(null);
        setSwing(50);
        setMaxTracks(24);
        setPartLength(64);
        setSongLength(Integer.MAX_VALUE);
        setTempoInBPM(120);
        setBeatDivider(4.0);
        setBeatsPerBar(4);
        setTicksPerBeat(24);
//        setMuteGroups();
    }

    public static void copyValues(TickerInfo info, Ticker ticker, Map<String, IMidiInstrument> instruments) {
        ticker.setId(info.getId());
        ticker.setBeat((int) info.getBeat());
        ticker.setDone(info.isDone());
        ticker.setTempoInBPM(info.getTempoInBPM());
        ticker.setBeatDivider(info.getBeatDivider());
        ticker.setMaxTracks(info.getMaxTracks());
        ticker.setPlaying(info.isPlaying());
        ticker.setSwing(info.getSwing());
        ticker.setStopped(info.isStopped());
        ticker.setSongLength(info.getSongLength());
        ticker.setPartLength(info.getPartLength());
        ticker.setTicksPerBeat(info.getTicksPerBeat());
        ticker.setPartLength(info.getPartLength());
        ticker.setBeatsPerBar(info.getBeatsPerBar());
        info.getPlayers().forEach(playerInfo -> {
            Strike strike = new Strike();
            PlayerInfo.copyValues(playerInfo, strike, instruments);
        });
    }

    public static void copyValues(Ticker ticker, TickerInfo info, boolean copyPlayers) {
        info.setId(ticker.getId());
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
        info.setPlayers(copyPlayers ? ticker.getPlayers().stream().map(PlayerInfo::fromPlayer).collect(Collectors.toSet()) : Collections.emptySet());
    }

    public static TickerInfo fromTicker(Ticker ticker, boolean copyPlayers) {
        TickerInfo info = new TickerInfo();
        copyValues(ticker, info, copyPlayers);
        return info;
    }
}

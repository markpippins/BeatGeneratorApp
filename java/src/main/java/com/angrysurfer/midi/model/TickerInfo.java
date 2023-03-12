package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    private List<PlayerInfo> players = new ArrayList<>();
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
    private float tempoInBPM;
    private int partLength;
    private int maxTracks;
    private int songLength;
    private int swing;
    private boolean playing;
    private boolean stopped;
    @Transient
    private MuteGroupList muteGroups;

    public TickerInfo() {
        setBeat(1);
        setBar(1);
        setTick(1);
        setDone(false);
        setPlaying(false);
        setStopped(false);
        setId(null);
        setSwing(Constants.DEFAULT_SWING);
        setMaxTracks(Constants.DEFAULT_MAX_TRACKS);
        setPartLength(Constants.DEFAULT_PART_LENGTH);
        setSongLength(Constants.DEFAULT_SONG_LENGTH);
        setTempoInBPM(Constants.DEFAULT_BPM);
        setBeatDivider(Constants.DEFAULT_BEAT_DIVIDER);
        setBeatsPerBar(Constants.DEFAULT_BEATS_PER_BAR);
        setTicksPerBeat(Constants.DEFAULT_PPQ);
//        setMuteGroups();
    }

    public static void copyToTicker(TickerInfo info, Ticker ticker) {
        ticker.setId(info.getId());
        ticker.setBar(info.getBar());
        ticker.setBeat(info.getBeat());
        ticker.setDone(info.isDone());
        ticker.setBeatDivider(info.getBeatDivider());
        ticker.setMaxTracks(info.getMaxTracks());
        ticker.setSwing(info.getSwing());
        ticker.setSongLength(info.getSongLength());
        ticker.setPartLength(info.getPartLength());
        ticker.setTicksPerBeat(info.getTicksPerBeat());
        ticker.setPartLength(info.getPartLength());
        ticker.setBeatsPerBar(info.getBeatsPerBar());
        info.getPlayers().forEach(playerInfo -> {
            Strike strike = new Strike();
            PlayerInfo.copyValues(playerInfo, strike);
        });
    }

    public static void copyFromTicker(Ticker ticker, TickerInfo info, List<PlayerInfo> players) {
        info.setId(ticker.getId());
        info.setTick(ticker.getTick());
        info.setBeat((int) ticker.getBeat());
        info.setBar(ticker.getBar());
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
        info.setPlayers(players);
        //copyPlayers ? ticker.getPlayers().stream().map(PlayerInfo::fromPlayer).collect(Collectors.toSet()) : Collections.emptySet());
    }

    public static TickerInfo fromTicker(Ticker ticker, List<PlayerInfo> players) {
        TickerInfo info = new TickerInfo();
        copyFromTicker(ticker, info, players);
        return info;
    }
}

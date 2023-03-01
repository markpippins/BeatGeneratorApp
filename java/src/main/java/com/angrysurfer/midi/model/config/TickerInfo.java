package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.MuteGroupList;
import com.angrysurfer.midi.model.Ticker;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
public class TickerInfo {
    static Logger logger = LoggerFactory.getLogger(TickerInfo.class.getCanonicalName());
    public boolean done;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ticker_player", joinColumns = {@JoinColumn(name = "ticker_id")})
    Set<PlayerInfo> players = new HashSet<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public static void copyValues(Ticker ticker, TickerInfo info) {
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
//        info.setPlayers(ticker.getPlayers().stream().map(PlayerInfo::fromPlayer).collect(Collectors.toSet()));
    }

    public static TickerInfo fromTicker(Ticker ticker) {
        TickerInfo info = new TickerInfo();
        copyValues(ticker, info);
        return info;
    }

}

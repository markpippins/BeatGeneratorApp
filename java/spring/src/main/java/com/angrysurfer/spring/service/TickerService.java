package com.angrysurfer.spring.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.TickerManager;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.update.TickerUpdateType;
import com.angrysurfer.spring.dao.TickerStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class TickerService {

    static Logger logger = LoggerFactory.getLogger(TickerService.class.getCanonicalName());

    private TickerManager tickerManager = TickerManager.getInstance();
    private RedisService redisService = RedisService.getInstance();
    private SongService songService;
    private InstrumentService instrumentService;

    public TickerService(SongService songService, InstrumentService instrumentService) {
        this.songService = songService;
        this.instrumentService = instrumentService;
    }

    public void play() {
        Ticker activeTicker = tickerManager.getActiveTicker();
        if (!activeTicker.getClockSource().getCycleListeners().contains(getSongService().getTickListener())) {
            activeTicker.getClockSource().getCycleListeners().add(getSongService().getTickListener());
        }

        getSongService().getSong().setBeatDuration(activeTicker.getBeatDuration());
        getSongService().getSong().setTicksPerBeat(activeTicker.getTicksPerBeat());

        tickerManager.getActiveTicker().play();
    }

    public TickerStatus getTickerStatus() {
        Ticker activeTicker = tickerManager.getActiveTicker();
        return TickerStatus.from(activeTicker, getSongService().getSong(), activeTicker.isRunning());
    }

    public List<Ticker> getAllTickers() {
        return redisService.getAllTickerIds().stream()
                .map(id -> redisService.findTickerById(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Ticker getTicker() {
        return tickerManager.getActiveTicker();
    }

    public Ticker updateTicker(Long tickerId, int updateType, long updateValue) {
        Ticker ticker = redisService.findTickerById(tickerId);
        if (ticker != null) {
            switch (updateType) {
                case TickerUpdateType.PPQ -> ticker.setTicksPerBeat((int) updateValue);
                case TickerUpdateType.BEATS_PER_BAR -> ticker.setBeatsPerBar((int) updateValue);
                case TickerUpdateType.BPM -> {
                    ticker.setTempoInBPM(Float.valueOf(updateValue));
                    if (ticker.getClockSource() != null) {
                        ticker.getClockSource().setTempoInBPM(updateValue);
                    }
                }
                case TickerUpdateType.PARTS -> {
                    ticker.setParts((int) updateValue);
                    ticker.getPartCycler().reset();
                }
                case TickerUpdateType.BASE_NOTE_OFFSET ->
                    ticker.setNoteOffset(ticker.getNoteOffset() + updateValue);
                case TickerUpdateType.BARS -> ticker.setBars((int) updateValue);
                case TickerUpdateType.PART_LENGTH -> ticker.setPartLength(updateValue);
                case TickerUpdateType.MAX_TRACKS -> ticker.setMaxTracks((int) updateValue);
            }
            redisService.saveTicker(ticker);
            return ticker;
        }
        return null;
    }

    public synchronized Ticker next(long currentTickerId) {
        if (tickerManager.canMoveForward()) {
            tickerManager.moveForward();
        }
        return tickerManager.getActiveTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        if (tickerManager.canMoveBack()) {
            tickerManager.moveBack();
        }
        return tickerManager.getActiveTicker();
    }

    public void clearPlayers() {
        Ticker ticker = getTicker();
        if (ticker != null) {
            PlayerManager.getInstance().removeAllPlayers(ticker);
        }
    }

    public Ticker loadTicker(long tickerId) {
        Ticker ticker = redisService.findTickerById(tickerId);
        if (ticker != null) {
            tickerManager.tickerSelected(ticker);
        }
        return ticker;
    }

    public ClockSource getClockSource() {
        Ticker ticker = getTicker();
        return ticker != null ? ticker.getClockSource() : null;
    }

    public void pause() {
        tickerManager.getActiveTicker().setPaused(true);
    }

    public void stop() {
        tickerManager.getActiveTicker().stop();
    }

}
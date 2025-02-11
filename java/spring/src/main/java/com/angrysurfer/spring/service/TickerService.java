package com.angrysurfer.spring.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.engine.TickerEngine;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.spring.dao.TickerStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class TickerService {

    static Logger logger = LoggerFactory.getLogger(TickerService.class.getCanonicalName());

    private TickerEngine engine;

    private DBService dbUtils;

    private SongService songService;

    InstrumentService instrumentService;

    public TickerService(DBService dbUtils, SongService songService, InstrumentService instrumentService) {
        this.dbUtils = dbUtils;
        this.songService = songService;
        this.instrumentService = instrumentService;

        engine = new TickerEngine();
    }

    public void play() {

        if (!engine.getClockSource().getCycleListeners().contains(getSongService().getTickListener()))
            engine.getClockSource().getCycleListeners().add(getSongService().getTickListener());

        getSongService().getSong().setBeatDuration(engine.getTicker().getBeatDuration());
        getSongService().getSong().setTicksPerBeat(engine.getTicker().getTicksPerBeat());

        engine.play();
    }

    public TickerStatus getTickerStatus() {
        return TickerStatus.from(engine.getTicker(), getSongService().getSong(), engine.getTicker().isRunning());
    }

    public List<Ticker> getAllTickers() {
        return dbUtils.findAllTickers();
    }

    public Ticker getTicker() {
        return engine.getTicker();
    }

    public Ticker updateTicker(Long tickerId, int updateType, long updateValue) {
        return dbUtils.saveTicker(engine.updateTicker(dbUtils.findTickerById(tickerId), updateType, updateValue));
    }

    public synchronized Ticker next(long currentTickerId) {
        Ticker ticker = engine.next(currentTickerId, dbUtils.getMaximumTickerId(), dbUtils.getTickerForward(),
                dbUtils.getTickerStrikeFinder(), dbUtils.getPlayerRuleFindSet(), dbUtils.getTickerSaver());

            return ticker;
    }

    public synchronized Ticker previous(long currentTickerId) {
        return engine.previous(currentTickerId, dbUtils.getMinimumTickerId(), dbUtils.getTickerBack(),
                dbUtils.getTickerStrikeFinder(), dbUtils.getPlayerRuleFindSet(), dbUtils.getTickerSaver());
    }

    public void clearPlayers() {
        // engine.clearPlayers();
        getTicker().getPlayers().clear();
    }

    public Ticker loadTicker(long tickerId) {
        return engine.loadTicker(tickerId, dbUtils.getTickerFindOne());
    }

    public ClockSource getClockSource() {
        return getEngine().getClockSource();
    }

    public void pause() {
        engine.pause();
    }

    public void stop() {
        engine.stop();
    }

}
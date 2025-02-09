package com.angrysurfer.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.update.TickerUpdateType;
import com.angrysurfer.spring.dao.TickerStatus;
import com.angrysurfer.spring.repo.Rules;
import com.angrysurfer.spring.repo.Strikes;
import com.angrysurfer.spring.repo.Tickers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class TickerService {

    static Logger logger = LoggerFactory.getLogger(TickerService.class.getCanonicalName());

    private Ticker ticker;
    private Strikes strikeRepo;
    private Rules ruleRepo;
    private Tickers tickerRepo;
    private SongService songService;
    private ClockSource clockSource;
    private ArrayList<ClockSource> clocks = new ArrayList<>();

    private Long lastTickId;

    public TickerService(Tickers tickerRepo, Strikes strikeRepo,
            Rules ruleRepo, SongService songService) {

        this.tickerRepo = tickerRepo;
        this.ruleRepo = ruleRepo;
        this.strikeRepo = strikeRepo;
        this.songService = songService;

        clockSource = new ClockSource(getTicker());
        clocks.add(clockSource);
    }

    public void play() {

        stopRunningClocks();
        // clocks.add(getClockSource());
        getClockSource().getCycleListeners().add(getSongService().getTickListener());
        getSongService().getSong().setBeatDuration(getTicker().getBeatDuration());
        getSongService().getSong().setTicksPerBeat(getTicker().getTicksPerBeat());

        this.ticker.getPlayers().forEach(p -> {
            Instrument instrument = p.getInstrument();
            MidiDevice device = MIDIService.getMidiDevice(instrument.getDeviceName());

            if (Objects.nonNull(device) && !device.isOpen())
                try {
                    device.open();
                    instrument.setDevice(device);
                } catch (MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }
            else
                logger.warn(instrument.getDeviceName() + " not initialized");
        });

        if (Objects.nonNull(getClockSource()) && !getClockSource().isRunning())
            new Thread(getClockSource()).start();
        else {
            new Thread(newClockSource()).start();
        }

        getTicker().getPlayers().forEach(p -> {
            try {
                if (p.getPreset() > -1)
                    p.getInstrument().programChange(p.getChannel(), p.getPreset(), 0);
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                logger.error(e.getMessage(), e);
            }
        });

    }

    private ClockSource newClockSource() {
        ClockSource source = new ClockSource(getTicker());
        clocks.add(source);
        return source;
    }

    private void stopRunningClocks() {
        if (Objects.nonNull(getClockSource()) && getClockSource().isRunning())
            getClockSource().stop();
        clocks.forEach(sr -> sr.stop());
        clocks.clear();
    }

    public Ticker stop() {
        stopRunningClocks();
        getTicker().setPaused(false);
        getTicker().getBeatCycler().reset();
        getTicker().getBarCycler().reset();
        getTicker().setDone(false);
        getTicker().reset();
        return getTicker();
    }

    public void pause() {
        stopRunningClocks();
        getClockSource().pause();
        getClockSource().isRunning();
    }

    public Ticker getTickerInfo() {
        return getTicker();
    }

    public TickerStatus getTickerStatus() {
        return TickerStatus.from(getTicker(), getSongService().getSong(), getClockSource().isRunning());
    }

    public List<Ticker> getAllTickerInfo() {
        return getTickerRepo().findAll();
    }

    public Ticker getTicker() {
        if (Objects.isNull(ticker)) {
            stopRunningClocks();
            ticker = getTickerRepo().save(new Ticker());
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
            newClockSource();
        }

        return ticker;
    }

    public Ticker updateTicker(Long tickerId, int updateType, long updateValue) {

        Ticker ticker = getTicker().getId().equals(tickerId) ? getTicker()
                : getTickerRepo().findById(tickerId).orElseThrow();

        switch (updateType) {
            case TickerUpdateType.PPQ:
                ticker.setTicksPerBeat((int) updateValue);
                break;

            case TickerUpdateType.BEATS_PER_BAR:
                ticker.setBeatsPerBar((int) updateValue);
                break;

            case TickerUpdateType.BPM:
                ticker.setTempoInBPM(Float.valueOf(updateValue));
                if (Objects.nonNull(getClockSource()) &&
                        ticker.getId().equals(getTicker().getId()))
                    getClockSource().setTempoInBPM(updateValue);

                // getSongService().getSong().setTicksPerBeat(getTicker().getTicksPerBeat());
                break;

            case TickerUpdateType.PARTS:
                ticker.setParts((int) updateValue);
                getTicker().getPartCycler().reset();
                break;

            case TickerUpdateType.BASE_NOTE_OFFSET:
                ticker.setNoteOffset((double) ticker.getNoteOffset() + updateValue);
                break;

            case TickerUpdateType.BARS:
                ticker.setBars((int) updateValue);
                break;

            case TickerUpdateType.PART_LENGTH:
                ticker.setPartLength(updateValue);
                break;

            case TickerUpdateType.MAX_TRACKS:
                ticker.setMaxTracks((int) updateValue);
                break;
        }

        return getTickerRepo().save(ticker);
    }

    public synchronized Ticker next(long currentTickerId) {
        stopRunningClocks();
        if (currentTickerId == 0 || getTicker().getPlayers().size() > 0) {
            stopRunningClocks();
            getTicker().getTickCycler().getListeners().clear();
            getTicker().getBeatCycler().getListeners().clear();
            getTicker().getBarCycler().getListeners().clear();
            Long maxTickerId = getTickerRepo().getMaximumTickerId();
            setTicker(Objects.nonNull(maxTickerId) && currentTickerId < maxTickerId
                    ? getTickerRepo().getNextTicker(currentTickerId)
                    : null);
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
            getTicker().getPlayers().addAll(getStrikeRepo().findByTickerId(getTicker().getId()));
            getTicker().getPlayers().forEach(p -> p.setRules(ruleRepo.findByPlayerId(p.getId())));
            ClockSource source = new ClockSource(getTicker());
            clocks.add(source);
            setClockSource(source);
        }

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        stopRunningClocks();
        if (currentTickerId > (getTickerRepo().getMinimumTickerId())) {
            // getTicker().getTickCycler().getListeners().clear();
            getTicker().getTickCounter().getListeners().clear();
            getTicker().getBeatCycler().getListeners().clear();
            getTicker().getBarCycler().getListeners().clear();
            stopRunningClocks();
            setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
            getTicker().getPlayers().addAll(getStrikeRepo().findByTickerId(getTicker().getId()));
            getTicker().getPlayers().forEach(p -> p.setRules(ruleRepo.findByPlayerId(p.getId())));
            newClockSource();
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
        }

        return getTicker();
    }

    // public void clearPlayers() {
    // getTicker().getPlayers().clear();
    // }

    public Ticker loadTicker(long tickerId) {
        stopRunningClocks();
        getTickerRepo().findById(tickerId).ifPresent(this::setTicker);
        return getTicker();
    }

    public Ticker newTicker() {
        stopRunningClocks();
        getTicker().getBeatCycler().getListeners().clear();
        getTicker().getBarCycler().getListeners().clear();
        getTicker().getTickCycler().getListeners().clear();
        getTicker().getTickCounter().getListeners().clear();
        getTicker().getBeatCounter().getListeners().clear();
        getTicker().getBarCounter().getListeners().clear();
        setTicker(null);
        return getTicker();
    }
}
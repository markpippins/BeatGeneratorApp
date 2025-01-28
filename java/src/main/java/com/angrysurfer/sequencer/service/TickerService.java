package com.angrysurfer.sequencer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.sequencer.dao.TickerStatus;
import com.angrysurfer.sequencer.model.Ticker;
import com.angrysurfer.sequencer.model.midi.MidiInstrument;
import com.angrysurfer.sequencer.repo.RuleRepo;
import com.angrysurfer.sequencer.repo.StrikeRepo;
import com.angrysurfer.sequencer.repo.TickerRepo;
import com.angrysurfer.sequencer.repo.TickerStatusDAO;
import com.angrysurfer.sequencer.util.ClockSource;
import com.angrysurfer.sequencer.util.update.TickerUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class TickerService {

    static Logger logger = LoggerFactory.getLogger(TickerService.class.getCanonicalName());

    private Ticker ticker;
    private StrikeRepo strikeRepo;
    private RuleRepo ruleRepo;
    private TickerRepo tickerRepo;
    private SongService songService;
    private ClockSource clockSource;
    private ArrayList<ClockSource> clocks = new ArrayList<>();

    private TickerStatusDAO tickerStatusDAO;

    private Long lastTickId;

    public TickerService(TickerRepo tickerRepo, StrikeRepo strikeRepo,
            RuleRepo ruleRepo, SongService songService, TickerStatusDAO tickerStatusDAO) {

        this.tickerRepo = tickerRepo;
        this.ruleRepo = ruleRepo;
        this.strikeRepo = strikeRepo;
        this.songService = songService;
        this.tickerStatusDAO = tickerStatusDAO;
    }

    public void play() {

        stopRunningSequencers();
        clockSource = new ClockSource(getTicker());
        clocks.add(getClockSource());
        getClockSource().getCycleListeners().add(getSongService().getTickListener());
        getSongService().getSong().setBeatDuration(getTicker().getBeatDuration());
        getSongService().getSong().setTicksPerBeat(getTicker().getTicksPerBeat());

        this.ticker.getPlayers().forEach(p -> {
            MidiInstrument instrument = p.getInstrument();
            MidiDevice device = MIDIService.getMidiDevice(instrument.getDeviceName());

            if (!device.isOpen())
                try {
                    device.open();
                } catch (MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }

            instrument.setDevice(device);
        });

        if (Objects.nonNull(getClockSource()) && !getClockSource().isPlaying())
            new Thread(getClockSource()).start();
        else {
            setClockSource(new ClockSource(getTicker()));
            new Thread(getClockSource()).start();
        }

        getTicker().getPlayers().forEach(p -> {
            try {
                if (p.getPreset() > 0)
                    p.getInstrument().programChange(p.getPreset(), 0);
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                logger.error(e.getMessage(), e);
            }
        });

    }

    private void stopRunningSequencers() {
        clocks.forEach(sr -> sr.stop());
        clocks.clear();
    }

    public Ticker stop() {
        stopRunningSequencers();
        return getTicker();
    }

    public void pause() {
        getClockSource().pause();
        getClockSource().isPlaying();
    }

    public Ticker getTickerInfo() {
        return getTicker();
    }

    public TickerStatus getTickerStatus() {
        return TickerStatus.from(getTicker(), getSongService().getSong(), getClockSource().isPlaying());
    }

    public List<Ticker> getAllTickerInfo() {
        return getTickerRepo().findAll();
    }

    public Ticker getTicker() {
        if (Objects.isNull(ticker)) {
            stopRunningSequencers();
            ticker = getTickerRepo().save(new Ticker());
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
            clockSource = new ClockSource(ticker);
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
                // if (Objects.nonNull(getClockSource()) &&
                // ticker.getId().equals(getTicker().getId()))
                // getClockSource().getSequencer().setTempoInBPM(updateValue);
                getSongService().getSong().setTicksPerBeat(getTicker().getTicksPerBeat());
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
        if (currentTickerId == 0 || getTicker().getPlayers().size() > 0) {
            stopRunningSequencers();
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
            clockSource = new ClockSource(getTicker());
        }

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        if (currentTickerId > (getTickerRepo().getMinimumTickerId())) {
            // getTicker().getTickCycler().getListeners().clear();
            getTicker().getTickCounter().getListeners().clear();
            getTicker().getBeatCycler().getListeners().clear();
            getTicker().getBarCycler().getListeners().clear();
            stopRunningSequencers();
            setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
            getTicker().getPlayers().addAll(getStrikeRepo().findByTickerId(getTicker().getId()));
            getTicker().getPlayers().forEach(p -> p.setRules(ruleRepo.findByPlayerId(p.getId())));
            clockSource = new ClockSource(getTicker());
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
        }

        return getTicker();
    }

    // public void clearPlayers() {
    // getTicker().getPlayers().clear();
    // }

    public Ticker loadTicker(long tickerId) {
        getTickerRepo().findById(tickerId).ifPresent(this::setTicker);
        return getTicker();
    }

    public Ticker newTicker() {
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
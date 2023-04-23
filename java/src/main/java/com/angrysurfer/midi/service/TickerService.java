package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.*;
import com.angrysurfer.midi.util.SequenceRunner;
import com.angrysurfer.midi.util.TickerUpdateType;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

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
    private SequenceRunner sequenceRunner;
    private ArrayList<SequenceRunner> sequenceRunners = new ArrayList<>();

    private TickerStatusDAO tickerStatusDAO;
    // private TickListener listener;
    // private TickerStatusPublisher tickerStatusPublisher = new
    // TickerStatusPublisher(this);
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
        sequenceRunner = new SequenceRunner(getTicker());
        sequenceRunners.add(getSequenceRunner());
        getSequenceRunner().getCycleListeners().add(getSongService().getTickListener());
        getSongService().getSong().setBeatDuration(getTicker().getBeatDuration());
        getSongService().getSong().setTicksPerBeat(getTicker().getTicksPerBeat());

        this.ticker.getPlayers().forEach(p -> p.getInstrument()
                .setDevice(MIDIService.findMidiOutDevice(p.getInstrument().getDeviceName())));

        if (Objects.nonNull(getSequenceRunner()) && !getSequenceRunner().isPlaying())
            new Thread(getSequenceRunner()).start();
        else {
            setSequenceRunner(new SequenceRunner(getTicker()));
            new Thread(getSequenceRunner()).start();
        }

        getTicker().getPlayers().forEach(p -> {
            try {
                p.getInstrument().programChange(p.getPreset(), 0);
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                logger.error(e.getMessage(), e);
            }
        });

    }

    private void stopRunningSequencers() {
        sequenceRunners.forEach(sr -> sr.stop());
        sequenceRunners.clear();
    }

    public Ticker stop() {
        stopRunningSequencers();
        return getTicker();
    }

    public void pause() {
        getSequenceRunner().pause();
        getSequenceRunner().isPlaying();
    }

    public Ticker getTickerInfo() {
        return getTicker();
    }

    public TickerStatus getTickerStatus() {
        return TickerStatus.from(getTicker(), getSongService().getSong(), getSequenceRunner().isPlaying());
    }

    public List<Ticker> getAllTickerInfo() {
        return getTickerRepo().findAll();
    }

    public Ticker getTicker() {
        if (Objects.isNull(ticker)) {
            stopRunningSequencers();
            ticker = getTickerRepo().save(new Ticker());
            getTicker().getTickCycler().getListeners().add(getSongService().getTickListener());
            sequenceRunner = new SequenceRunner(ticker);
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
                if (Objects.nonNull(getSequenceRunner()) && ticker.getId().equals(getTicker().getId()))
                    getSequenceRunner().getSequencer().setTempoInBPM(updateValue);
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
            sequenceRunner = new SequenceRunner(getTicker());
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
            sequenceRunner = new SequenceRunner(getTicker());
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

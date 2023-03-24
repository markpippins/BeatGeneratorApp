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

@Getter
@Setter
@Service
public class TickerService {

    static Logger logger = LoggerFactory.getLogger(TickerService.class.getCanonicalName());

    private Ticker ticker;
    private StrikeRepository strikeRepository;
    private RuleRepository ruleRepository;
    private TickerRepo tickerRepo;
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    private SequenceRunner sequenceRunner;
    private ArrayList<SequenceRunner> sequenceRunners = new ArrayList<>(); 

    public TickerService(TickerRepo tickerRepo, StrikeRepository strikeRepository,
        RuleRepository ruleRepository, StepRepository stepRepository, SongRepository songRepository) {

        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
    }
    
    public void play() {
        
        stopRunningSequencers();
        sequenceRunner = new SequenceRunner(getTicker());
        sequenceRunners.add(getSequenceRunner());

        this.ticker.getPlayers().forEach(p -> p.getInstrument()
            .setDevice(MIDIService.findMidiOutDevice(p.getInstrument().getDeviceName())));

        if (Objects.nonNull(getSequenceRunner()) && !getSequenceRunner().isPlaying())
            new Thread(getSequenceRunner()).start();
        else {
            setSequenceRunner(new SequenceRunner(getTicker()));
            new Thread(getSequenceRunner()).start();
        }
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

    public Ticker getTickerStatus() {
        return getTicker();
    }

    public List<Ticker> getAllTickerInfo() {
        return getTickerRepo().findAll();
    }

    public Ticker getTicker() {
        if (Objects.isNull(ticker)) {
            stopRunningSequencers();
            ticker = getTickerRepo().save(new Ticker());
            sequenceRunner = new SequenceRunner(ticker);
        }
        
        return ticker;
    }

    public Ticker updateTicker(Long tickerId, int updateType, int updateValue) {

        Ticker ticker = getTicker().getId().equals(tickerId) ? getTicker() : getTickerRepo().findById(tickerId).orElseThrow();

        switch (updateType) {
            case TickerUpdateType.PPQ : ticker.setTicksPerBeat(updateValue);
                break;

            case TickerUpdateType.BEATS_PER_BAR : ticker.setBeatsPerBar(updateValue);
                break;

            case TickerUpdateType.BPM: 
                ticker.setTempoInBPM(Float.valueOf(updateValue));
                if (Objects.nonNull(getSequenceRunner()) && ticker.getId().equals(getTicker().getId()))
                    getSequenceRunner().getSequencer().setTempoInBPM(updateValue);
                break;

            case TickerUpdateType.PARTS: ticker.setParts(updateValue);
                break;

            case TickerUpdateType.BARS: ticker.setBars(updateValue);
                break;

            case TickerUpdateType.PART_LENGTH: ticker.setPartLength(updateValue);
                break;

            case TickerUpdateType.MAX_TRACKS: ticker.setMaxTracks(updateValue);
                break;
        }

        return getTickerRepo().save(ticker);
    }

    public synchronized Ticker next(long currentTickerId) {
        if (currentTickerId == 0 || getTicker().getPlayers().size() > 0) {       
            stopRunningSequencers();
            Long maxTickerId = getTickerRepo().getMaximumTickerId();
            setTicker(Objects.nonNull(maxTickerId) && currentTickerId < maxTickerId ?
                getTickerRepo().getNextTicker(currentTickerId) :
                null);
            getTicker().getPlayers().addAll(getStrikeRepository().findByTickerId(getTicker().getId()));
            getTicker().getPlayers().forEach(p -> p.setRules(ruleRepository.findByPlayerId(p.getId())));
            sequenceRunner = new SequenceRunner(getTicker());
        }

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        if (currentTickerId >  (getTickerRepo().getMinimumTickerId())) {
            stopRunningSequencers();
            setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
            getTicker().getPlayers().addAll(getStrikeRepository().findByTickerId(getTicker().getId()));
            getTicker().getPlayers().forEach(p -> p.setRules(ruleRepository.findByPlayerId(p.getId())));
            sequenceRunner = new SequenceRunner(getTicker());
        }

        return getTicker();
    }

    public void clearPlayers() {
        getTicker().getPlayers().clear();
    }

    public Ticker loadTicker(long tickerId) {
        getTickerRepo().findById(tickerId).ifPresent(this::setTicker);
        return getTicker();
    }

    public Ticker newTicker() {
        setTicker(null);
        return getTicker();
    } 
}

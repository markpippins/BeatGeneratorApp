package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.*;
import com.angrysurfer.midi.util.BeatGeneratorConfig;
import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.MidiInstrumentList;
import com.angrysurfer.midi.util.Operator;
import com.angrysurfer.midi.util.SequenceRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;

import static com.angrysurfer.midi.util.PlayerUpdateType.*;
import static com.angrysurfer.midi.util.TickerUpdateType.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Getter
@Setter
@Service
public class PlayerService {

    static Logger logger = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());

    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());
    private final MidiInstrumentRepository midiInstrumentRepo;
    private final ControlCodeRepository controlCodeRepository;
    private final PadRepository padRepository;
    // private Ticker ticker;
    private Song song;
    private MIDIService midiService;
    private StrikeRepository strikeRepository;
    private RuleRepository ruleRepository;
    private TickerRepo tickerRepo;
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    private SequenceRunner sequenceRunner;
    private ArrayList<SequenceRunner> sequenceRunners = new ArrayList<>(); 
    private TickerService tickerService;

    public PlayerService(MIDIService midiService, StrikeRepository strikeRepository,
                                RuleRepository ruleRepository, TickerRepo tickerRepo, 
                                MidiInstrumentRepository midiInstrumentRepository,
                                ControlCodeRepository controlCodeRepository,
                                PadRepository padRepository,
                                StepRepository stepRepository,
                                SongRepository songRepository,
                                TickerService tickerService) {

        this.midiService = midiService;
        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
        this.padRepository = padRepository;
        this.midiInstrumentRepo = midiInstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        this.tickerService = tickerService;
    }


    public void saveConfig() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
            // String instruments = "resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists()) file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getTicker().getPlayers().stream().map(Player::getInstrument).distinct().toList());
            Files.write(file.toPath(), Collections.singletonList(PlayerService.mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(list)), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }    
    }

    // public void play() {
        
    //     stopRunningSequencers();
    //     sequenceRunner = new SequenceRunner(getTicker());
    //     sequenceRunners.add(getSequenceRunner());

    //     getTicker().getPlayers().forEach(p -> p.getInstrument()
    //         .setDevice(MIDIService.findMidiOutDevice(p.getInstrument().getDeviceName())));

    //     if (Objects.nonNull(getSequenceRunner()) && !getSequenceRunner().isPlaying())
    //         new Thread(getSequenceRunner()).start();
    //     else {
    //         setSequenceRunner(new SequenceRunner(getTicker()));
    //         new Thread(getSequenceRunner()).start();
    //     }
    // }

    // private void stopRunningSequencers() {
    //     sequenceRunners.forEach(sr -> sr.stop());
    //     sequenceRunners.clear();
    // }

    // public Ticker stop() {
    //     stopRunningSequencers();
    //     return getTicker();
    // }

    // public void pause() {
    //     getSequenceRunner().pause();
    //     getSequenceRunner().isPlaying();
    // }

    // public Ticker getTickerInfo() {
    //     return getTicker();
    // }

    // public Ticker getTickerStatus() {
    //     return getTicker();
    // }

    // public List<Ticker> getAllTickerInfo() {
    //     return getTickerRepo().findAll();
    // }

    public Ticker getTicker() {
        // if (Objects.isNull(ticker)) {
        //     stopRunningSequencers();
        //     ticker = getTickerRepo().save(new Ticker());
        //     sequenceRunner = new SequenceRunner(ticker);
        // }
        
        // return ticker;
        return getTickerService().getTicker();
    }

    public Strike addPlayer(String instrumentName) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        return addPlayer(midiInstrument);
    }

    public Strike addPlayer(Long instrumentId) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
        return addPlayer(midiInstrument);
    }
 
    public Strike addPlayer(MidiInstrument midiInstrument) {
        
        tickerRepo.flush();

        try {
            midiInstrument.setDevice(MIDIService.findMidiOutDevice(midiInstrument.getDeviceName()));
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        String name = midiInstrument.getName().concat(Integer.toString(getTicker().getPlayers().size()));
        Strike strike = new Strike(name, getTicker(), midiInstrument,
                Strike.KICK + getTicker().getPlayers().size(), Strike.closedHatParams);
        strike.setTicker(getTicker());
        strike = getStrikeRepository().save(strike);
        getTicker().getPlayers().add(strike);
        strike.getSubCycler().setLength(16);
        return strike;
    }

    public Set<Rule> getRules(Long playerId) {
        return this.getRuleRepository().findByPlayerId(playerId);
    }

    public Rule addRule(Long playerId) {
        Rule rule = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0);
        Strike player = getTicker().getPlayer(playerId);
        List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();
        
        if (matches.size() < 2) {
            rule.setPlayer(player);
            getRuleRepository().save(rule);
            player.getRules().add(rule);
            getStrikeRepository().save(player);
    
            return rule;
        }

        return matches.get(0);
    }

    public void removeRule(Long playerId, Long ruleId) {
        Strike player = getTicker().getPlayer(playerId);
        Rule rule = player.getRule(ruleId);

        player.getRules().remove(rule);
        strikeRepository.save(player);
        rule.setPlayer(null);
        getRuleRepository().save(rule);
        // TODO: remove rule from database
        // getRuleRepository().deleteById(ruleId);
    }


    public Ticker updateTicker(Long tickerId, int updateType, int updateValue) {

        Ticker ticker = getTicker().getId().equals(tickerId) ? getTicker() : getTickerRepo().findById(tickerId).orElseThrow();

        switch (updateType) {
            case PPQ : ticker.setTicksPerBeat(updateValue);
                break;

            case BPM: 
                ticker.setTempoInBPM(Float.valueOf(updateValue));
                if (Objects.nonNull(getSequenceRunner()) && ticker.getId().equals(getTicker().getId()))
                    getSequenceRunner().getSequencer().setTempoInBPM(updateValue);
                break;

            case BEATS_PER_BAR: ticker.setBeatsPerBar(updateValue);
                break;

            case PART_LENGTH: ticker.setPartLength(updateValue);
                break;

            case MAX_TRACKS: ticker.setMaxTracks(updateValue);
            break;
        }

        return getTickerRepo().save(ticker);
    }

    public Player updatePlayer(Long playerId, int updateType, int updateValue) {
        Strike strike = getTicker().getPlayer(playerId); 
        // strikeRepository.findById(playerId).orElseThrow();
        switch (updateType) {
            case NOTE -> {
                strike.setNote(updateValue);
                break;
            }
            
            case INSTRUMENT -> {
                MidiInstrument instrument = getMidiInstrumentRepo().findById((long) updateValue).orElseThrow(null);
                instrument.setDevice(MIDIService.findMidiOutDevice(instrument.getDeviceName()));
                strike.setInstrument(instrument);
                break;
            }

            case PRESET -> {                
                strike.setPreset(updateValue);
                try {
                    strike.getInstrument().programChange(updateValue, 0);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }
                break;
            }

            case PROBABILITY -> {                
                strike.setProbability(updateValue);
                break;
            }

            case MIN_VELOCITY -> {                
                strike.setMinVelocity(updateValue);
                break;
            }

            case MAX_VELOCITY -> {                
                strike.setMaxVelocity(updateValue);
                break;
            }

            case MUTE -> {                
                strike.setMuted(updateValue > 0 ? true : false);
                break;
            }
        }

        getStrikeRepository().save(strike);
        getTickerRepo().save(getTicker());

        return strike;
    }

    public void updateRule(Long playerId,
                           Long ruleId,
                           int operatorId,
                           int comparisonId,
                           double newValue,
                           int part) {

        Strike strike = getTicker().getPlayer(playerId);


        Rule rule = strike.getRule(ruleId);
        rule.setOperatorId(operatorId);
        rule.setComparisonId(comparisonId);
        rule.setValue(newValue);
        rule.setPart(part);

        getRuleRepository().save(rule);
        
    }

    public Set<Strike> removePlayer(Long playerId) {
        Player strike = getTicker().getPlayer(playerId);
        getTicker().getPlayers().remove(strike);
        getStrikeRepository().deleteById(strike.getId());
        return getTicker().getPlayers();
    }


    public Strike mutePlayer(Long playerId) {
        Strike strike = getTicker().getPlayer(playerId);
        strike.setMuted(!strike.isMuted());
        return strike;
    }

    // public synchronized Ticker next(long currentTickerId) {
    //     if (currentTickerId == 0 || getTicker().getPlayers().size() > 0) {       
    //         stopRunningSequencers();
    //         Long maxTickerId = getTickerRepo().getMaximumTickerId();
    //         getTickerService().setTicker(Objects.nonNull(maxTickerId) && currentTickerId < maxTickerId ?
    //             getTickerRepo().getNextTicker(currentTickerId) :
    //             null);
    //         getTicker().getPlayers().addAll(getStrikeRepository().findByTickerId(getTicker().getId()));
    //         getTicker().getPlayers().forEach(p -> p.setRules(ruleRepository.findByPlayerId(p.getId())));
    //         sequenceRunner = new SequenceRunner(getTicker());
    //     }

    //     return getTicker();
    // }

    // public synchronized Ticker previous(long currentTickerId) {
    //     if (currentTickerId >  (getTickerRepo().getMinimumTickerId())) {
    //         stopRunningSequencers();
    //         getTickerService().setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
    //         getTicker().getPlayers().addAll(getStrikeRepository().findByTickerId(getTicker().getId()));
    //         getTicker().getPlayers().forEach(p -> p.setRules(ruleRepository.findByPlayerId(p.getId())));
    //         sequenceRunner = new SequenceRunner(getTicker());
    //     }

    //     return getTicker();
    // }

    public void setSteps(List<Step> steps) {
    //    this.getBeatGenerator().setSteps(steps);
    }

    public void save() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists()) file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getTicker().getPlayers().stream().map(p -> p.getInstrument()).distinct().toList());
            Files.write(file.toPath(), Collections.singletonList(PlayerService.mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(list)), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveBeat() {
        try {
            Set<Strike> strikes = new HashSet<>();
            getTicker().getPlayers().forEach(s -> strikes.add((Strike) s));
            String beatFile = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/beats/" + toString() + ".json";
            File file = new File(beatFile);
            if (file.exists()) file.delete();
            Files.write(file.toPath(), Collections.singletonList(PlayerService.mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new BeatGeneratorConfig(getTicker(), strikes))), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearPlayers() {
        getTicker().getPlayers().clear();
    }

    public Ticker loadTicker(long tickerId) {
        // getTickerRepo().findById(tickerId).ifPresent(this::setTicker);
        // return getTicker();
        return tickerService.loadTicker(tickerId);
    }

    public Set<Strike> getPlayers() {
        return getTicker().getPlayers();
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
 
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(MIDIService.findMidiOutDevice(midiInstrument.getDeviceName()));

        log.info(String.join(", ", instrumentName, Integer.toString(channel), Integer.toString(note)));
 
        if (!midiInstrument.getDevice().isOpen())
            try {
                midiInstrument.getDevice().open();
                MidiSystem.getTransmitter().setReceiver(midiInstrument.getDevice().getReceiver());
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        new Thread(new Runnable() {

            public void run() {
                try {
                    ShortMessage noteOn = new ShortMessage();
                    noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 127);
                    midiInstrument.getDevice().getReceiver().send(noteOn, 0L);
                    Thread.sleep(2500);
                    ShortMessage noteOff = new ShortMessage();
                    noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 127);
                    midiInstrument.getDevice().getReceiver().send(noteOff, 0L);
                } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


    // public Ticker newTicker() {
    //     setTicker(null);
    //     return getTicker();
    // } 
}

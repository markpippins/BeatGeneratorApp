package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;

import static com.angrysurfer.midi.model.PlayerUpdateType.*;
import static com.angrysurfer.midi.model.TickerUpdateType.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Getter
@Setter
@Service
public class PlayerService {
    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());
    private final MidiInstrumentRepository midiInstrumentRepo;
    private final ControlCodeRepository controlCodeRepository;
    private final PadRepository padRepository;
    private Ticker ticker;
    private Song song;
    private MIDIService midiService;
    private StrikeRepository strikeRepository;
    private RuleRepository ruleRepository;
    private TickerRepo tickerRepo;
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    static Sequencer sequencer;        


    public PlayerService(MIDIService midiService, StrikeRepository strikeRepository,
                                RuleRepository ruleRepository, TickerRepo tickerRepo, 
                                MidiInstrumentRepository midiInstrumentRepository,
                                ControlCodeRepository controlCodeRepository,
                                PadRepository padRepository,
                                StepRepository stepRepository,
                                SongRepository songRepository) {

        this.midiService = midiService;
        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
        this.padRepository = padRepository;
        this.midiInstrumentRepo = midiInstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
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

    public void play() {
        if (!getTicker().isPaused() && !getTicker().isPlaying())
            new Thread(new Runnable() {
                public void run() {
                    getTicker().run();
                }
            }).start();
        else if (getTicker().isPaused())
            getTicker().pause();
        else getTicker().run();
        getTicker().isPlaying();
    }

    public Ticker stop() {
        if (getTicker().isPlaying())
            getTicker().stop();
        return getTicker();
    }

    public void pause() {
        getTicker().pause();
        getTicker().isPlaying();
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
        if (Objects.isNull(ticker))
            setTicker(getTickerRepo().save(new Ticker()));
        return ticker;
    }

    public Strike addPlayer(String instrumentName) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        // midiInstrument.setDevice(MIDIService.getDevice(midiInstrument.getDeviceName()));
        return addPlayer(midiInstrument);
    }

    public Strike addPlayer(Long instrumentId) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
        midiInstrument.setDevice(MIDIService.findMidiOutDevice(midiInstrument.getDeviceName()));
        return addPlayer(midiInstrument);
    }

    public Strike addPlayer(MidiInstrument midiInstrument) {
        Set<Strike> strikes = getStrikeRepository().findByTickerId(getTicker().getId());

        String name = midiInstrument.getName().concat(Integer.toString(strikes.size()));
        Strike strike = new Strike(name, getTicker(), midiInstrument,
                Strike.KICK + getTicker().getPlayers().size(), Strike.closedHatParams);
        strike.setTicker(getTicker());
        strike = getStrikeRepository().save(strike);
        strikes.add(strike);
        getTicker().setPlayers(strikes);
        return strike;
    }

    public Set<Rule> getRules(Long playerId) {
        return this.getRuleRepository().findByPlayerId(playerId);
    }

    public Rule addRule(Long playerId) {
        Rule[] rule = { new Rule(Operator.BEAT, Comparison.EQUALS, 1.0) };

        getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny()
            .ifPresent(p -> {
                rule[0].setPlayer(p);
                getRuleRepository().save(rule[0]);
                p.getRules().add(rule[0]);
                getStrikeRepository().save(p);
            });
        
        return rule[0];
    }

    public void removeRule(Long playerId, Long ruleId) {
        Strike player = getTicker().getPlayer(playerId);
        player.getRules().remove(player.getRule(ruleId));
        strikeRepository.save(player);
        // TODO: remove rule from database
        // getRuleRepository().deleteById(ruleId);
    }

    public void updateRule(Long playerId,
                           Long ruleId,
                           int operatorId,
                           int comparisonId,
                           double newValue) {

        Optional<Strike> playerOpt = getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        if (playerOpt.isPresent()) {
            Strike strike = playerOpt.get();
            Optional<Rule> rule = strike.getRules().stream().filter(c -> c.getId() == ruleId).findAny();
            if (rule.isPresent()) {
                rule.get().setOperatorId(operatorId);
                rule.get().setComparisonId(comparisonId);
                rule.get().setValue(newValue);
                getRuleRepository().save(rule.get());
            }
        }
    }

    public Set<Strike> removePlayer(Long playerId) {
        Optional<Strike> strike = getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        strike.ifPresent(p -> getTicker().getPlayers().remove(p));
        strike.ifPresent(p -> getStrikeRepository().deleteById(p.getId()));
        return getStrikeRepository().findByTickerId(getTicker().getId());
    }


    public Strike mutePlayer(Long playerId) {
        Strike strike = getTicker().getPlayer(playerId);
        strike.setMuted(!strike.isMuted());
        return strike;
    }

    public synchronized Ticker next(long currentTickerId) {
        if (currentTickerId == 0 || getTicker().getPlayers().size() > 0) {       
            Long maxTickerId = getTickerRepo().getMaximumTickerId();
            int tickerCount = getTickerRepo().findAll().size();

            setTicker( tickerCount > 0 && Objects.nonNull(maxTickerId) && currentTickerId < maxTickerId ?
                getTickerRepo().getNextTicker(currentTickerId) :
                getTickerRepo().save(new Ticker()));

            Set<Strike> strikes = getStrikeRepository().findByTickerId(getTicker().getId());
            getTicker().setSequencer(sequencer);
            getTicker().getPlayers().addAll(strikes);
        }

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        if (currentTickerId >  (getTickerRepo().getMinimumTickerId())) {
            setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
            getTicker().setSequencer(sequencer);
            getTicker().getPlayers().addAll(getStrikeRepository().findByTickerId(getTicker().getId()));
        }

        return getTicker();
    }

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

    public Ticker updateTicker(Long tickerId, int updateType, int updateValue) {

        Ticker tickerToUpdate = getTickerRepo().findById(tickerId).orElseThrow();

        switch (updateType) {
            case PPQ : tickerToUpdate.setTicksPerBeat(updateValue);
                break;

            case BPM: tickerToUpdate.setTempoInBPM(Float.valueOf(updateValue));
                break;

            case BEATS_PER_BAR: tickerToUpdate.setBeatsPerBar(updateValue);
                break;

            case PART_LENGTH: tickerToUpdate.setPartLength(updateValue);
                break;
        }

        tickerToUpdate = getTickerRepo().save(tickerToUpdate);

        if (getTicker().getId().equals(tickerToUpdate.getId())) {
            tickerToUpdate.setSequencer(sequencer);
            setTicker(tickerToUpdate);
        }
            

        return tickerToUpdate;
    }

    public void updatePlayer(Long playerId, int updateType, int updateValue) {
        Optional<Strike> playerOpt = strikeRepository.findById(playerId);
        switch (updateType) {
            case NOTE -> {
                playerOpt.ifPresent(strike -> {
                    strike.setNote(updateValue);
                    getStrikeRepository().save(strike);
                    getTicker().getPlayers().stream().filter(p -> Objects.nonNull(p.getId()) && p.getId().equals(playerId)).findFirst().ifPresent(p -> p.setNote(updateValue));
                });

            }
            case INSTRUMENT -> {
                Optional<MidiInstrument> instrument = getMidiInstrumentRepo().findById((long) updateValue);
                instrument.ifPresent(inst -> {
                    inst.setDevice(MIDIService.findMidiOutDevice(inst.getDeviceName()));
                    playerOpt.ifPresent(strike -> {
                        strike.setInstrument(inst);
                        getStrikeRepository().save(strike);
                        getTicker().getPlayers().stream().filter(p -> Objects.nonNull(p.getId()) && p.getId().equals(playerId)).findFirst().ifPresent(p -> p.setInstrument(inst));
                    });
                });

            }
        }
    }

    public Ticker loadTicker(long tickerId) {
        getTickerRepo().findById(tickerId).ifPresent(this::setTicker);
        getTicker().setSequencer(sequencer);            
        return getTicker();
    }


    public Ticker newTicker() {
        setTicker(getTickerRepo().save(new Ticker()));
        getTicker().setSequencer(sequencer);
        return getTicker();
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
}

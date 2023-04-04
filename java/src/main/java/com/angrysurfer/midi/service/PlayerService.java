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
import static com.angrysurfer.midi.util.RuleUpdateType.*;
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
            if (file.exists())
                file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getPlayers().stream().map(Player::getInstrument).distinct().toList());
            Files.write(file.toPath(),
                    Collections.singletonList(
                            PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Player addPlayer(String instrumentName) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        return addPlayer(midiInstrument);
    }

    public Player addPlayer(Long instrumentId) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
        return addPlayer(midiInstrument);
    }

    public Player addPlayer(MidiInstrument midiInstrument) {

        tickerRepo.flush();

        int note = 60;

        try {
            midiInstrument.setDevice(MIDIService.findMidiOutDevice(midiInstrument.getDeviceName()));
            if (Objects.nonNull(midiInstrument.getLowestNote())) {
                note = rand.nextInt(midiInstrument.getLowestNote(), midiInstrument.getHighestNote());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        String name = midiInstrument.getName().concat(Integer.toString(getPlayers().size()));
        Player player = new Strike(name, getTickerService().getTicker(), midiInstrument, note,
                midiInstrument.getControlCodes().stream().map(cc -> cc.getCode()).toList());
        player.setTicker(getTickerService().getTicker());
        player.setProbability(rand.nextLong(100));
        if (rand.nextBoolean())
            player.setRandomDegree(rand.nextLong(3));

        if (player instanceof Strike)
            player = getStrikeRepository().save((Strike) player);

        getPlayers().add(player);

        int part = rand.nextInt(getTickerService().getTicker().getParts());
        // if (rand.nextBoolean())
        player.getRules().add(getRuleRepository().save(new Rule(player, Operator.TICK, Comparison.EQUALS, 1.0, part)));
        // else
        //     player.getRules().add(getRuleRepository().save(new Rule(player, Operator.TICK, Comparison.EQUALS, rand.nextDouble(1, 
        //     getTickerService().getTicker().getTicksPerBeat()), part)));

        // if (rand.nextBoolean())
        //     player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BEAT, Comparison.EQUALS, rand.nextDouble(1, 
        //     getTickerService().getTicker().getBeatsPerBar()), part)));
        // else
        //     player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BEAT, Comparison.MODULO, rand.nextDouble(1, 
        //     getTickerService().getTicker().getBeatsPerBar()), part)));
        
        // if (rand.nextBoolean())
        //     player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BAR, Comparison.EQUALS, rand.nextDouble(1, 
        //     getTickerService().getTicker().getBars()), part)));
        // else
        //     player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BAR, Comparison.MODULO, rand.nextDouble(1, 
        //     getTickerService().getTicker().getBars()), part)));

            // player.setMuted(true);
        return player;
    }

    public Set<Rule> getRules(Long playerId) {
        return this.getRuleRepository().findByPlayerId(playerId);
    }

    static Random rand = new Random();

    public Rule addRule(Long playerId) {
        Rule rule = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0);

        Player player = getTickerService().getTicker().getPlayer(playerId);
        List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();

        if (matches.size() < 2) {
            rule.setPlayer(player);
            getRuleRepository().save(rule);
            player.getRules().add(rule);
            if (player instanceof Strike)
                getStrikeRepository().save((Strike) player);

            return rule;
        }

        return matches.get(0);
    }

    public void removeRule(Long playerId, Long ruleId) {
        Player player = getTickerService().getTicker().getPlayer(playerId);
        Rule rule = player.getRule(ruleId);

        player.getRules().remove(rule);
        if (player instanceof Strike)
            getStrikeRepository().save((Strike) player);
        rule.setPlayer(null);
        getRuleRepository().save(rule);
    }

    public Player updatePlayer(Long playerId, int updateType, long updateValue) {
        Player player = getTickerService().getTicker().getPlayer(playerId);
        // strikeRepository.findById(playerId).orElseThrow();
        switch (updateType) {
            case NOTE -> {
                player.setNote(updateValue);
                break;
            }

            case INSTRUMENT -> {
                MidiInstrument instrument = getMidiInstrumentRepo().findById((long) updateValue).orElseThrow(null);
                instrument.setDevice(MIDIService.findMidiOutDevice(instrument.getDeviceName()));
                player.setInstrument(instrument);
                break;
            }

            case PRESET -> {
                try {
                    player.setPreset(updateValue);
                    player.getInstrument().programChange(updateValue, 0);
                }   
                  catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }
                break;
            }

            case PROBABILITY -> {
                player.setProbability(updateValue);
                break;
            }

            case MIN_VELOCITY -> {
                player.setMinVelocity(updateValue);
                break;
            }

            case MAX_VELOCITY -> {
                player.setMaxVelocity(updateValue);
                break;
            }

            case RATCHET_COUNT -> {
                player.setRatchetCount(updateValue);
                break;
            }

            case RATCHET_INTERVAL -> {
                player.setRatchetInterval(updateValue);
                break;
            }

            case MUTE -> {
                player.setMuted(updateValue > 0 ? true : false);
                break;
            }

            case SKIPS -> {
                player.setSkips(updateValue);
                player.getSkipCycler().setLength(updateValue);
                player.getSkipCycler().reset();
                break;
            }

            case RANDOM_DEGREE -> {
                player.setRandomDegree(updateValue);
                break;
            }

            case BEAT_FRACTION -> {
                player.setBeatFraction(updateValue);
                break;
            }

            case SUBDIVISIONS -> {
                player.setSubDivisions(updateValue);
                player.getSubCycler().setLength(updateValue);
                player.getSubCycler().reset();
                break;
            }

            case SWING -> {
                player.setSwing(updateValue);
                break;
            }

            case LEVEL -> {
                player.setLevel(updateValue);
                break;
            }

            case CHANNEL -> {
                // strike.setChannel(updateValue > 0 ? true : false);
                break;
            }

        }

        if (player instanceof Strike)
            getStrikeRepository().save((Strike) player);

        getTickerRepo().save(getTickerService().getTicker());

        return player;
    }

    public Rule updateRule(Long ruleId, int updateType, long updateValue) {

        Optional<Rule> opt = getPlayers().stream().flatMap(p -> p.getRules().stream()).filter(r -> r.getId() == updateValue).findAny();
        Rule rule = null;
        if (opt.isPresent()) {
            rule = opt.get();
            switch (updateType) {
                case OPERATOR -> {
                    rule.setOperatorId((int) updateValue);
                    break;
                }
                case COMPARISON -> {
                    rule.setComparisonId((int) updateValue);
                    break;
                }
                case VALUE -> {
                    rule.setValue((double) updateValue);
                    break;
                }
            }

            getRuleRepository().save(rule);
        }

        return rule;
    }

    // public void updateRule(Long playerId,
    //         Long ruleId,
    //         int operatorId,
    //         int comparisonId,
    //         double newValue,
    //         int part) {

    //     Player player = getTickerService().getTicker().getPlayer(playerId);

    //     Rule rule = player.getRule(ruleId);
    //     rule.setOperatorId(operatorId);
    //     rule.setComparisonId(comparisonId);
    //     rule.setValue(newValue);
    //     rule.setPart(part);

    //     getRuleRepository().save(rule);

    // }

    public Set<Player> removePlayer(Long playerId) {
        Player player = getTickerService().getTicker().getPlayer(playerId);
        player.getRules().forEach(r -> ruleRepository.delete(r));
        getPlayers().remove(player);
        if (player instanceof Strike)
            getStrikeRepository().deleteById(player.getId());
        return getPlayers();
    }

    public Player mutePlayer(Long playerId) {
        Player player = getTickerService().getTicker().getPlayer(playerId);
        player.setMuted(!player.isMuted());
        return player;
    }

    public void setSteps(List<Pattern> steps) {
        // this.getBeatGenerator().setSteps(steps);
    }

    public void save() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists())
                file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getPlayers().stream().map(p -> p.getInstrument()).distinct().toList());
            Files.write(file.toPath(),
                    Collections.singletonList(
                            PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveBeat() {
        try {
            Set<Player> strikes = new HashSet<>();
            getPlayers().forEach(s -> strikes.add((Strike) s));
            String beatFile = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/beats/" + toString()
                    + ".json";
            File file = new File(beatFile);
            if (file.exists())
                file.delete();
            Files.write(file.toPath(), Collections.singletonList(PlayerService.mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new BeatGeneratorConfig(getTickerService().getTicker(), strikes))),
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void clearPlayers() {
        getPlayers().stream().filter(p -> p.getRules().size() == 0)
                .forEach(p -> {
                    getPlayers().remove(p);
                    p.setTicker(null);
                    if (p instanceof Strike)
                        strikeRepository.delete((Strike) p);
                });
    }

    public Set<Player> getPlayers() {
        return getTickerService().getTicker().getPlayers();
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

package com.angrysurfer.spring.service;

import static com.angrysurfer.core.util.update.PlayerUpdateType.BEAT_FRACTION;
import static com.angrysurfer.core.util.update.PlayerUpdateType.CHANNEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_IN;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_OUT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.INSTRUMENT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.LEVEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MAX_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MIN_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MUTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.NOTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PRESET;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PROBABILITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RANDOM_DEGREE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_COUNT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_INTERVAL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SKIPS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SOLO;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SUBDIVISIONS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SWING;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.engine.MIDIEngine;
import com.angrysurfer.core.engine.PlayerEngine;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.player.AbstractPlayer;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.db.Save;
import com.angrysurfer.spring.repo.ControlCodes;
import com.angrysurfer.spring.repo.Instruments;
import com.angrysurfer.spring.repo.Pads;
import com.angrysurfer.spring.repo.Rules;
import com.angrysurfer.spring.repo.Songs;
import com.angrysurfer.spring.repo.Steps;
import com.angrysurfer.spring.repo.Strikes;
import com.angrysurfer.spring.repo.Tickers;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class PlayerService {

    static Logger logger = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());

    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());
    private Song song;

    private Instruments midiInstrumentRepo;
    private ClockSource clockSource;

    private ArrayList<ClockSource> clocks = new ArrayList<>();
    private TickerService tickerService;
    private DBUtils dbUtils;

    private PlayerEngine playerEngine;

    public PlayerService(Instruments midiInstrumentRepository,
            TickerService tickerService,
            DBUtils dbUtils) {

        this.midiInstrumentRepo = midiInstrumentRepository;
        this.tickerService = tickerService;
        this.playerEngine = new PlayerEngine();
        this.dbUtils = dbUtils;
    }

    // public void saveConfig() {
    // try {
    // String instruments =
    // "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
    // // String instruments = "resources/config/midi-bak.json";
    // File file = new File(instruments);
    // if (file.exists())
    // file.delete();
    // DeviceConfig list = new DeviceConfig();
    // list.getInstruments().addAll(getPlayers().stream().map(AbstractPlayer::getInstrument).distinct().toList());
    // Files.write(file.toPath(),
    // Collections.singletonList(
    // PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
    // StandardOpenOption.CREATE_NEW);
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }

    public AbstractPlayer addPlayer(String instrumentName) {
        logger.info("addPlayer() - instrumentName: {}", instrumentName);
        Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        return addPlayer(midiInstrument, getNoteForMidiInstrument(midiInstrument));
    }

    private long getNoteForMidiInstrument(Instrument midiInstrument) {
        Long note = Objects.nonNull(midiInstrument.getLowestNote()) ? midiInstrument.getLowestNote() : 60L;
        List<AbstractPlayer> players = getTickerService().getTicker().getPlayers().stream()
                .filter(p -> p.getInstrumentId().equals(midiInstrument.getId())).toList();
        return note + players.size();
    }

    public AbstractPlayer addPlayer(String instrumentName, Long note) {
        logger.info("addPlayer() - instrumentName: {}, note: {}", instrumentName, note);
        Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        return addPlayer(midiInstrument, note);
    }

    public AbstractPlayer addPlayer(Long instrumentId) {
        Instrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
        return addPlayer(midiInstrument);
    }

    public AbstractPlayer addPlayer(Instrument midiInstrument) {
        return addPlayer(midiInstrument, getNoteForMidiInstrument(midiInstrument));
    }

    public AbstractPlayer addPlayer(Instrument midiInstrument, long note) {
        logger.info("addPlayer() - instrument: {}, note: {}", midiInstrument.getName(), note);
        // tickerRepo.flush();

        AbstractPlayer player = this.playerEngine.addPlayer(getTickerService().getTicker(), midiInstrument,
                dbUtils.getPlayerSaver(),
                dbUtils.getTickerSaver());

        return player;
    }

    public Set<Rule> getRules(Long playerId) {
        logger.info("getRules() - playerId: {}", playerId);
        AbstractPlayer player = dbUtils.findStrikeById(playerId);
        return player.getRules();
    }

    static Random rand = new Random();

    public Rule addRule(Long playerId, int operator, int comparison, double value, int part) {
        logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
                playerId, operator, comparison, value, part);

        AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);

        Rule rule = this.playerEngine.addRule(getTickerService().getTicker(), player, operator, comparison, value,
                part);

        dbUtils.saveRule(rule);
        player.getRules().add(rule);
        dbUtils.savePlayer((Strike) player);

        return rule;
    }

    public Rule addRule(Long playerId) {
        return playerEngine.addRule(getTickerService().getTicker(), getTickerService().getTicker().getPlayer(playerId), dbUtils.getRuleSaver(),
                dbUtils.getPlayerSaver());
    }

    public void removeRule(Long playerId, Long ruleId) {
        logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
        AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
        Rule rule = player.getRule(ruleId);

        player.getRules().remove(rule);
        dbUtils.savePlayer(player);
        rule.setPlayer(null);
        dbUtils.deleteRule(rule);
    }

    public AbstractPlayer updatePlayer(Long playerId, int updateType, long updateValue) {
        logger.info("updatePlayer() - playerId: {}, updateType: {}, updateValue: {}",
                playerId, updateType, updateValue);
        AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);

        this.playerEngine.updatePlayer(getTickerService().getTicker(), playerId, updateType, updateValue);

        dbUtils.savePlayer(player);
        dbUtils.saveTicker(getTickerService().getTicker());

        return player;
    }

    public Optional<Rule> getRule(Long ruleId) {
        List<Rule> rules = getPlayers().stream().flatMap(p -> p.getRules().stream()).toList();
        return rules.stream().filter(r -> r.getId().equals(ruleId)).findAny();
    }

    public Rule updateRule(Long ruleId, int updateType, long updateValue) {
        Rule rule = this.playerEngine.updateRule(getTickerService().getTicker(), ruleId, updateType, updateValue);
        return dbUtils.saveRule(rule);
    }

    // public void updateRule(Long playerId,
    // Long ruleId,
    // int operator,
    // int comparison,
    // double newValue,
    // int part) {

    // Player player = getTickerService().getTicker().getPlayer(playerId);

    // Rule rule = player.getRule(ruleId);
    // rule.setOperatorId(operator);
    // rule.setComparison(comparison);
    // rule.setValue(newValue);
    // rule.setPart(part);

    // getRuleRepository().save(rule);

    // }

    public Set<AbstractPlayer> removePlayer(Long playerId) {
        logger.info("removePlayer() - playerId: {}", playerId);
        AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
        player.getRules().forEach(r -> dbUtils.deleteRule(r));
        getPlayers().remove(player);
        dbUtils.deletePlayer(player);
        return getPlayers();
    }

    public AbstractPlayer mutePlayer(Long playerId) {
        return this.playerEngine.mutePlayer(getTickerService().getTicker(), playerId);
    }

    public void setSteps(List<Pattern> steps) {
        logger.info("setSteps() - steps size: {}", steps.size());
        // this.getBeatGenerator().setSteps(steps);
    }

    // TODO: Save
    public void save() {
        // try {
        // String instruments =
        // "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi-bak.json";
        // File file = new File(instruments);
        // if (file.exists())
        // file.delete();
        // DeviceConfig list = new DeviceConfig();
        // list.getInstruments().addAll(getPlayers().stream().map(p ->
        // p.getInstrument()).distinct().toList());
        // Files.write(file.toPath(),
        // Collections.singletonList(
        // PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
        // StandardOpenOption.CREATE_NEW);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
    }

    // TODO: SaveBeat
    public void saveBeat() {
        // try {
        // Set<AbstractPlayer> strikes = new HashSet<>();
        // getPlayers().forEach(s -> strikes.add((Strike) s));
        // String beatFile =
        // "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/beats/" +
        // toString()
        // + ".json";
        // File file = new File(beatFile);
        // if (file.exists())
        // file.delete();
        // Files.write(file.toPath(),
        // Collections.singletonList(PlayerService.mapper.writerWithDefaultPrettyPrinter()
        // .writeValueAsString(new TickerConfig(getTickerService().getTicker(),
        // strikes))),
        // StandardOpenOption.CREATE_NEW);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
    }

    public void clearPlayers() {
        getPlayers().stream().filter(p -> p.getRules().size() == 0)
                .forEach(p -> {
                    getPlayers().remove(p);
                    p.setTicker(null);
                    dbUtils.deletePlayer(p);
                });
    }

    public void clearPlayersWithNoRules() {
        getPlayers().stream().filter(p -> p.getRules().size() == 0)
                .forEach(p -> {
                    if (p.getRules().size() > 0)
                        return;

                    getPlayers().remove(p);
                    p.setTicker(null);
                    dbUtils.deletePlayer(p);
                });
    }

    public Set<AbstractPlayer> getPlayers() {
        return getTickerService().getTicker().getPlayers();
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
        logger.info("playDrumNote() - instrumentName: {}, channel: {}, note: {}",
                instrumentName, channel, note);
        Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(MIDIEngine.getMidiDevice(midiInstrument.getDeviceName()));

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
                    noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 126);
                    midiInstrument.getDevice().getReceiver().send(noteOn, 0L);
                    // Thread.sleep(5000);
                    // ShortMessage noteOff = new ShortMessage();
                    // noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 126);
                    // midiInstrument.getDevice().getReceiver().send(noteOff, 1000L);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}

package com.angrysurfer.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.api.Database;
import com.angrysurfer.core.engine.PlayerEngine;
import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.player.AbstractPlayer;
import com.angrysurfer.core.util.ClockSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class PlayerService {

    static Logger logger = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());

    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(PlayerService.class.getCanonicalName());
    private Song song;

    private ClockSource clockSource;

    private ArrayList<ClockSource> clocks = new ArrayList<>();
    private Database dbUtils;
    private TickerService tickerService;
    private InstrumentService instrumentService;

    private PlayerEngine playerEngine;

    public PlayerService(TickerService tickerService, InstrumentService instrumentService, Database dbUtils) {

        this.tickerService = tickerService;
        this.instrumentService = instrumentService;
        this.playerEngine = new PlayerEngine();
        this.dbUtils = dbUtils;
    }

    public IPlayer addPlayer(String instrumentName) {
        logger.info("addPlayer() - instrumentName: {}", instrumentName);
        Instrument instrument = getInstrumentService().findByName(instrumentName);
        return addPlayer(instrument, getNoteForMidiInstrument(instrument));
    }

    private long getNoteForMidiInstrument(Instrument instrument) {
        logger.info("getNoteForMidiInstrument() - instrument: {}", instrument);
        return playerEngine.getNoteForMidiInstrument(getTicker(), instrument);
    }

    public IPlayer addPlayer(String instrumentName, Long note) {
        logger.info("addPlayer() - instrumentName: {}, note: {}", instrumentName, note);
        return addPlayer(getInstrumentService().findByName(instrumentName), note);
    }

    public IPlayer addPlayer(Long instrumentId) {
        logger.info("addPlayer() - id: {}", instrumentId);
        return addPlayer(getInstrumentService().getInstrumentById(instrumentId));
    }

    public IPlayer addPlayer(Instrument instrument) {
        return addPlayer(instrument, getNoteForMidiInstrument(instrument));
    }

    public IPlayer addPlayer(Instrument instrument, long note) {
        logger.info("addPlayer() - instrument: {}, note: {}", instrument.getName(), note);
        // return playerEngine.addPlayer(getTicker(), instrument, note,
        // dbUtils.getTickerSaver(), dbUtils.getStrikeSaver(), dbUtils.getRuleSaver());
        return playerEngine.addPlayer(getTicker(), instrument, note);
    }

    private Ticker getTicker() {
        return getTickerService().getTicker();
    }

    public Set<Rule> getRules(Long playerId) {
        logger.info("getRules() - playerId: {}", playerId);
        return dbUtils.findStrikeById(playerId).getRules();
    }

    static Random rand = new Random();

    public Rule addRule(Long playerId, int operator, int comparison, double value, int part) {
        logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
                playerId, operator, comparison, value, part);

        // return playerEngine.addRule(getTicker(), getTicker().getPlayer(playerId),
        //         operator, comparison, value, part,
        //         dbUtils.getRuleSaver(), dbUtils.getStrikeSaver());
        return playerEngine.addRule(getTicker(), getTicker().getPlayer(playerId),
                operator, comparison, value, part);
    }

    public Rule addRule(Long playerId) {
//        return playerEngine.addRule(getTicker(), getTicker().getPlayer(playerId),
//            dbUtils.getRuleSaver(),
//            dbUtils.getStrikeSaver());
        return playerEngine.addRule(getTicker(), getTicker().getPlayer(playerId));
    }

    public void removeRule(Long playerId, Long ruleId) {
        logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
//        playerEngine.removeRule(getTicker(), playerId, ruleId, dbUtils.getRuleDeleter(),
//                dbUtils.getStrikeSaver());
        playerEngine.removeRule(getTicker(), playerId, ruleId);
    }

    public IPlayer updatePlayer(Long playerId, int updateType, long updateValue) {
        logger.info("updatePlayer() - playerId: {}, updateType: {}, updateValue: {}",
                playerId, updateType, updateValue);
//        return playerEngine.updatePlayer(getTicker(), playerId, updateType, updateValue,
//                dbUtils.getStrikeSaver());
        return playerEngine.updatePlayer(getTicker(), playerId, updateType, updateValue);

    }

    public Rule getRule(Long ruleId) {
        return dbUtils.findRuleById(ruleId);
    }

    public Rule updateRule(Long ruleId, int updateType, long updateValue) {
//        return playerEngine.updateRule(getTicker(), ruleId, updateType, updateValue,
//                dbUtils.getRuleSaver());
        return playerEngine.updateRule(getTicker(), ruleId, updateType, updateValue);
    }

    public Set<AbstractPlayer> removePlayer(Long playerId) {
        logger.info("removePlayer() - playerId: {}", playerId);
//        return playerEngine.removePlayer(getTicker(), playerId, dbUtils.getStrikeDeleter(),
//                dbUtils.getRuleDeleter(), dbUtils.getTickerSaver());
        return playerEngine.removePlayer(getTicker(), playerId);
    }

    public IPlayer mutePlayer(Long playerId) {
        return playerEngine.mutePlayer(getTicker(), playerId);
    }

    public void clearPlayers() {
//        playerEngine.clearPlayers(getTicker(), dbUtils.getStrikeDeleter(), dbUtils.getTickerSaver());
        playerEngine.clearPlayers(getTicker());
    }

    public void clearPlayersWithNoRules() {
//        playerEngine.clearPlayersWithNoRules(getTicker(), dbUtils.getStrikeDeleter(),
//                dbUtils.getTickerSaver());
        playerEngine.clearPlayersWithNoRules(getTicker());
    }

    public Set<AbstractPlayer> getPlayers() {
        return getTicker().getPlayers();
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
        logger.info("playDrumNote() - instrumentName: {}, channel: {}, note: {}",
                instrumentName, channel, note);
        playerEngine.playDrumNote(getInstrumentService().findByName(instrumentName), channel, note);
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
        // Set<IPlayer> strikes = new HashSet<>();
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
        // .writeValueAsString(new TickerConfig(getTicker(),
        // strikes))),
        // StandardOpenOption.CREATE_NEW);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
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
    // list.getInstruments().addAll(getPlayers().stream().map(IPlayer::getInstrument).distinct().toList());
    // Files.write(file.toPath(),
    // Collections.singletonList(
    // PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
    // StandardOpenOption.CREATE_NEW);
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }

}

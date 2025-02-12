package com.angrysurfer.core.engine;

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
import static com.angrysurfer.core.util.update.RuleUpdateType.COMPARISON;
import static com.angrysurfer.core.util.update.RuleUpdateType.OPERATOR;
import static com.angrysurfer.core.util.update.RuleUpdateType.VALUE;

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

import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.player.IPlayer;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Operator;
import com.angrysurfer.core.util.update.RuleUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerEngine {

    static Logger logger = LoggerFactory.getLogger(PlayerEngine.class.getCanonicalName());

    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    // public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(PlayerEngine.class.getCanonicalName());
    private ClockSource clockSource;
    private ArrayList<ClockSource> clocks = new ArrayList<>();

    public PlayerEngine() {
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

    // public IPlayer addPlayer(String instrumentName) {
    // logger.info("addPlayer() - instrumentName: {}", instrumentName);
    // = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
    // return addPlayer(instrument, getNoteForMidiInstrument(instrument));
    // }

    public long getNoteForMidiInstrument(Ticker ticker, Instrument instrument) {
        Long note = Objects.nonNull(instrument.getLowestNote()) ? instrument.getLowestNote() : 60L;
        return note + ticker.getPlayers().size();
    }

    public IPlayer addPlayer(Ticker ticker, Instrument instrument, long note, Save<Ticker> tickerSaver,
            Save<IPlayer> playerSaver,
            Save<Rule> ruleSaver) {

        logger.info("addPlayer() - instrument: {}", instrument.getName());
        // tickerRepo.flush();

        try {
            instrument.setDevice(MIDIEngine.getMidiDevice(instrument.getDeviceName()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        String name = instrument.getName().concat(Integer.toString(ticker.getPlayers().size()));
        IPlayer player = new Strike(name, ticker, instrument,
                note > 0 ? note : getNoteForMidiInstrument(ticker, instrument),
                instrument.getControlCodes().stream().map(cc -> cc.getCode()).toList());
        player.setTicker(ticker);

        ticker.getPlayers().add(player);

        addRule(ticker, player, ruleSaver, playerSaver);

        return player;
    }

    static Random rand = new Random();

    public Rule addRule(Ticker ticker, IPlayer player, int operator, int comparison, double value, int part,
            Save<Rule> ruleSaver,
            Save<IPlayer> playerSaver) {

        logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
                player.getId(), operator, comparison, value, part);

        Rule rule = new Rule(operator, comparison, value, part, true);
        List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();

        if (matches.size() == 0) {
            rule.setPlayer(player);
            ruleSaver.save(rule);
            player.getRules().add(rule);
            playerSaver.save(player);
            return rule;
        }

        return matches.get(0);
    }

    public Rule addRule(Ticker ticker, IPlayer player, Save<Rule> ruleSaver,
            Save<IPlayer> playerSaver) {

        Rule rule = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0, true);

        List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();

        if (matches.size() < 2) {
            rule.setPlayer(player);
            ruleSaver.save(rule);
            player.getRules().add(rule);
            playerSaver.save(player);
            return rule;
        }

        return matches.get(0);
    }

    public void removeRule(Ticker ticker, Long playerId, Long ruleId, Delete<Rule> ruleDeleter,
            Save<IPlayer> playerSaver) {

        logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
        IPlayer player = ticker.getPlayer(playerId);
        Rule rule = player.getRule(ruleId);
        player.getRules().remove(rule);
        rule.setPlayer(null);
        ruleDeleter.delete(rule);
        playerSaver.save(player);
    }

    public Set<IPlayer> removePlayer(Ticker ticker, Long playerId, Delete<IPlayer> playerDeleter,
            Delete<Rule> ruleDeleter, Save<Ticker> tickerSaver) {

        logger.info("removePlayer() - playerId: {}", playerId);
        IPlayer player = ticker.getPlayer(playerId);

        player.getRules().forEach(r -> ruleDeleter.delete(r));

        ticker.getPlayers().remove(player);
        playerDeleter.delete(player);
        tickerSaver.save(ticker);
        return ticker.getPlayers();
    }

    public IPlayer updatePlayer(Ticker ticker, Long playerId, int updateType, long updateValue,
            Save<IPlayer> playerSaver) {

        logger.info("updatePlayer() - playerId: {}, updateType: {}, updateValue: {}",
                playerId, updateType, updateValue);

        IPlayer player = ticker.getPlayer(playerId);

        switch (updateType) {
            case CHANNEL -> {
                player.noteOff(0, 0);
                player.setChannel((int) updateValue);
                break;
            }

            case NOTE -> {
                player.setNote(updateValue);
                break;
            }

            case INSTRUMENT -> {
                // Instrument instrument = getMidiInstrumentRepo().findById((long)
                // updateValue).orElseThrow(null);
                // player.getInstrument().setDevice(MIDIEngine.getMidiDevice(instrument.getDeviceName()));
                // player.setInstrument(instrument);
                break;
            }

            case PRESET -> {
                try {
                    player.noteOff(0, 0);
                    player.setPreset(updateValue);
                    // Instrument instrument = getMidiInstrumentRepo().findById((long)
                    // player.getInstrumentId())
                    // .orElseThrow(null);
                    player.getInstrument().setDevice(MIDIEngine.getMidiDevice(player.getInstrument().getDeviceName()));
                    player.getInstrument().programChange(player.getChannel(), updateValue, 0);
                    // player.setInstrument(instrument);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
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

            case SOLO -> {
                player.setSolo(updateValue > 0 ? true : false);
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

            case FADE_IN -> {
                player.setFadeIn(updateValue);
                break;
            }

            case FADE_OUT -> {
                player.setFadeOut(updateValue);
                break;
            }
        }

        return playerSaver.save(player);
    }

    public Optional<Rule> getRule(Ticker ticker, Long ruleId) {
        List<Rule> rules = ticker.getPlayers().stream().flatMap(p -> p.getRules().stream()).toList();
        return rules.stream().filter(r -> r.getId().equals(ruleId)).findAny();
    }

    public Rule updateRule(Ticker ticker, Long ruleId, int updateType, long updateValue, Save<Rule> ruleSaver) {

        Optional<Rule> opt = getRule(ticker, ruleId);
        Rule rule = null;
        if (opt.isPresent()) {
            rule = opt.get();
            switch (updateType) {
                case OPERATOR -> {
                    rule.setOperator((int) updateValue);
                    break;
                }

                case COMPARISON -> {
                    rule.setComparison((int) updateValue);
                    break;
                }

                case VALUE -> {
                    rule.setValue((double) updateValue);
                    break;
                }

                case RuleUpdateType.PART -> {
                    rule.setPart((int) updateValue);
                    break;
                }
            }
        }

        return ruleSaver.save(rule);
    }

    public IPlayer mutePlayer(Ticker ticker, Long playerId) {
        IPlayer player = ticker.getPlayer(playerId);
        player.setMuted(!player.isMuted());
        return player;
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
        // .writeValueAsString(new TickerConfig(ticker,
        // strikes))),
        // StandardOpenOption.CREATE_NEW);
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
    }

    public void clearPlayers(Ticker ticker, Delete<IPlayer> playerDeleter, Save<Ticker> tickerSaver) {
        Set<IPlayer> players = ticker.getPlayers();
        players.stream().filter(p -> p.getRules().size() == 0)
                .forEach(p -> {
                    ticker.getPlayers().remove(p);
                    p.setTicker(null);
                    playerDeleter.delete((Strike) p);
                });

        tickerSaver.save(ticker);
    }

    public void clearPlayersWithNoRules(Ticker ticker, Delete<IPlayer> playerDeleter, Save<Ticker> tickerSaver) {
        ticker.getPlayers().stream().filter(p -> p.getRules().size() == 0)
                .forEach(p -> {
                    if (p.getRules().size() > 0)
                        return;
                    ticker.getPlayers().remove(p);
                    p.setTicker(null);
                    playerDeleter.delete((Strike) p);
                });

        tickerSaver.save(ticker);
    }

    public void playDrumNote(Instrument instrument, int channel, int note) {
        logger.info("playDrumNote() - instrumentName: {}, channel: {}, note: {}",
                instrument.getName(), channel, note);

        instrument.setDevice(MIDIEngine.getMidiDevice(instrument.getDeviceName()));

        if (!instrument.getDevice().isOpen())
            try {
                instrument.getDevice().open();
                MidiSystem.getTransmitter().setReceiver(instrument.getDevice().getReceiver());
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        new Thread(new Runnable() {

            public void run() {
                try {
                    ShortMessage noteOn = new ShortMessage();
                    noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 126);
                    instrument.getDevice().getReceiver().send(noteOn, 0L);
                    // Thread.sleep(5000);
                    // ShortMessage noteOff = new ShortMessage();
                    // noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 126);
                    // instrument.getDevice().getReceiver().send(noteOff, 1000L);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}

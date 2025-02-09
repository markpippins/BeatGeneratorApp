// package com.angrysurfer.core.engine;

// import static com.angrysurfer.core.util.update.PlayerUpdateType.BEAT_FRACTION;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.CHANNEL;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_IN;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_OUT;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.INSTRUMENT;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.LEVEL;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.MAX_VELOCITY;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.MIN_VELOCITY;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.MUTE;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.NOTE;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.PRESET;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.PROBABILITY;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.RANDOM_DEGREE;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_COUNT;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_INTERVAL;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.SKIPS;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.SOLO;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.SUBDIVISIONS;
// import static com.angrysurfer.core.util.update.PlayerUpdateType.SWING;
// import static com.angrysurfer.core.util.update.RuleUpdateType.COMPARISON;
// import static com.angrysurfer.core.util.update.RuleUpdateType.OPERATOR;
// import static com.angrysurfer.core.util.update.RuleUpdateType.VALUE;

// import java.io.File;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.StandardOpenOption;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Objects;
// import java.util.Optional;
// import java.util.Random;
// import java.util.Set;

// import javax.sound.midi.InvalidMidiDataException;
// import javax.sound.midi.MidiSystem;
// import javax.sound.midi.MidiUnavailableException;
// import javax.sound.midi.ShortMessage;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.stereotype.Service;

// import com.angrysurfer.core.config.DeviceConfig;
// import com.angrysurfer.core.model.Pattern;
// import com.angrysurfer.core.model.Rule;
// import com.angrysurfer.core.model.Song;
// import com.angrysurfer.core.model.midi.Instrument;
// import com.angrysurfer.core.model.player.AbstractPlayer;
// import com.angrysurfer.core.model.player.Strike;
// import com.angrysurfer.core.repo.ControlCodes;
// import com.angrysurfer.core.repo.Instruments;
// import com.angrysurfer.core.repo.Pads;
// import com.angrysurfer.core.repo.Rules;
// import com.angrysurfer.core.repo.Songs;
// import com.angrysurfer.core.repo.Steps;
// import com.angrysurfer.core.repo.Strikes;
// import com.angrysurfer.core.repo.Tickers;
// import com.angrysurfer.core.util.ClockSource;
// import com.angrysurfer.core.util.Comparison;
// import com.angrysurfer.core.util.Operator;
// import com.angrysurfer.core.util.update.RuleUpdateType;
// import com.angrysurfer.spring.dao.BeatConfig;
// import com.angrysurfer.spring.service.MIDIService;
// import com.angrysurfer.spring.service.TickerService;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// @Service
// public class PlayerEngine {

//     static Logger logger = LoggerFactory.getLogger(PlayerEngine.class.getCanonicalName());

//     static final String RAZZ = "Razzmatazz";
//     static final String MICROFREAK = "MicroFreak";
//     public static ObjectMapper mapper = new ObjectMapper();
//     static Logger log = LoggerFactory.getLogger(PlayerEngine.class.getCanonicalName());
//     private final Instruments midiInstrumentRepo;
//     // private final ControlCodes controlCodeRepository;
//     // private final Pads padRepository;
//     private Song song;
//     private MIDIService midiService;
//     private Strikes strikeRepository;
//     private Rules ruleRepository;
//     private Tickers tickerRepo;
//     private Steps stepDataRepository;
//     private Songs songRepository;
//     private ClockSource clockSource;
//     private ArrayList<ClockSource> clocks = new ArrayList<>();
//     private TickerService tickerService;

//     public PlayerEngine(MIDIService midiService, Strikes strikeRepository,
//             Rules ruleRepository, Tickers tickerRepo,
//             Instruments midiInstrumentRepository,
//             ControlCodes controlCodeRepository,
//             Pads padRepository,
//             Steps stepRepository,
//             Songs songRepository,
//             TickerService tickerService) {

//         this.midiService = midiService;
//         this.tickerRepo = tickerRepo;
//         this.ruleRepository = ruleRepository;
//         this.strikeRepository = strikeRepository;
//         this.padRepository = padRepository;
//         this.midiInstrumentRepo = midiInstrumentRepository;
//         this.controlCodeRepository = controlCodeRepository;
//         this.stepDataRepository = stepRepository;
//         this.songRepository = songRepository;
//         this.tickerService = tickerService;
//     }

//     // public void saveConfig() {
//     // try {
//     // String instruments =
//     // "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
//     // // String instruments = "resources/config/midi-bak.json";
//     // File file = new File(instruments);
//     // if (file.exists())
//     // file.delete();
//     // DeviceConfig list = new DeviceConfig();
//     // list.getInstruments().addAll(getPlayers().stream().map(AbstractPlayer::getInstrument).distinct().toList());
//     // Files.write(file.toPath(),
//     // Collections.singletonList(
//     // PlayerService.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
//     // StandardOpenOption.CREATE_NEW);
//     // } catch (IOException e) {
//     // throw new RuntimeException(e);
//     // }
//     // }

//     public AbstractPlayer addPlayer(String instrumentName) {
//         logger.info("addPlayer() - instrumentName: {}", instrumentName);
//         Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
//         return addPlayer(midiInstrument, getNoteForMidiInstrument(midiInstrument));
//     }

//     private long getNoteForMidiInstrument(Instrument midiInstrument) {
//         Long note = Objects.nonNull(midiInstrument.getLowestNote()) ? midiInstrument.getLowestNote() : 60L;
//         List<AbstractPlayer> players = getTickerService().getTicker().getPlayers().stream()
//                 .filter(p -> p.getInstrumentId().equals(midiInstrument.getId())).toList();
//         return note + players.size();
//     }

//     public AbstractPlayer addPlayer(String instrumentName, Long note) {
//         logger.info("addPlayer() - instrumentName: {}, note: {}", instrumentName, note);
//         Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
//         return addPlayer(midiInstrument, note);
//     }

//     public AbstractPlayer addPlayer(Long instrumentId) {
//         Instrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
//         return addPlayer(midiInstrument);
//     }

//     public AbstractPlayer addPlayer(Instrument midiInstrument) {
//         return addPlayer(midiInstrument, getNoteForMidiInstrument(midiInstrument));
//     }

//     public AbstractPlayer addPlayer(Instrument midiInstrument, long note) {
//         logger.info("addPlayer() - instrument: {}, note: {}", midiInstrument.getName(), note);
//         tickerRepo.flush();

//         try {
//             midiInstrument.setDevice(MIDIService.getMidiDevice(midiInstrument.getDeviceName()));
//         } catch (Exception e) {
//             logger.error(e.getMessage(), e);
//         }

//         String name = midiInstrument.getName().concat(Integer.toString(getPlayers().size()));
//         AbstractPlayer player = new Strike(name, getTickerService().getTicker(), midiInstrument, note,
//                 midiInstrument.getControlCodes().stream().map(cc -> cc.getCode()).toList());
//         player.setTicker(getTickerService().getTicker());

//         if (player instanceof Strike)
//             player = getStrikeRepository().save((Strike) player);

//         getPlayers().add(player);

//         return player;
//     }
//     // player.setProbability(rand.nextLong(100));
//     // if (rand.nextBoolean())
//     // player.setRandomDegree(rand.nextLong(3));
//     // int part = rand.nextInt(getTickerService().getTicker().getParts());
//     // if (rand.nextBoolean())
//     // player.getRules().add(getRuleRepository().save(new Rule(player,
//     // Operator.TICK, Comparison.EQUALS, 1.0, part)));
//     // else
//     // player.getRules().add(getRuleRepository().save(new Rule(player,
//     // Operator.TICK, Comparison.EQUALS, rand.nextDouble(1,
//     // getTickerService().getTicker().getTicksPerBeat()), part)));

//     // if (rand.nextBoolean())
//     // player.getRules().add(getRuleRepository().save(new Rule(player,
//     // Operator.BEAT, Comparison.EQUALS, rand.nextDouble(1,
//     // getTickerService().getTicker().getBeatsPerBar()), part)));
//     // else
//     // player.getRules().add(getRuleRepository().save(new Rule(player,
//     // Operator.BEAT, Comparison.MODULO, rand.nextDouble(1,
//     // getTickerService().getTicker().getBeatsPerBar()), part)));

//     // if (rand.nextBoolean())
//     // player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BAR,
//     // Comparison.EQUALS, rand.nextDouble(1,
//     // getTickerService().getTicker().getBars()), part)));
//     // else
//     // player.getRules().add(getRuleRepository().save(new Rule(player, Operator.BAR,
//     // Comparison.MODULO, rand.nextDouble(1,
//     // getTickerService().getTicker().getBars()), part)));
//     // player.setMuted(true);

//     public Set<Rule> getRules(Long playerId) {
//         logger.info("getRules() - playerId: {}", playerId);
//         return this.getRuleRepository().findByPlayerId(playerId);
//     }

//     static Random rand = new Random();

//     public Rule addRule(Long playerId, int operator, int comparison, double value, int part) {
//         logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
//                 playerId, operator, comparison, value, part);
//         Rule rule = new Rule(operator, comparison, value, part);

//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();

//         if (matches.size() == 0) {
//             rule.setPlayer(player);
//             getRuleRepository().save(rule);
//             player.getRules().add(rule);
//             if (player instanceof Strike)
//                 getStrikeRepository().save((Strike) player);

//             return rule;
//         }

//         return matches.get(0);
//     }

//     public Rule addRule(Long playerId) {
//         Rule rule = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0);

//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         List<Rule> matches = player.getRules().stream().filter(r -> r.isEqualTo(rule)).toList();

//         if (matches.size() < 2) {
//             rule.setPlayer(player);
//             getRuleRepository().save(rule);
//             player.getRules().add(rule);
//             if (player instanceof Strike)
//                 getStrikeRepository().save((Strike) player);

//             return rule;
//         }

//         return matches.get(0);
//     }

//     public void removeRule(Long playerId, Long ruleId) {
//         logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         Rule rule = player.getRule(ruleId);

//         player.getRules().remove(rule);
//         if (player instanceof Strike)
//             getStrikeRepository().save((Strike) player);
//         rule.setPlayer(null);
//         getRuleRepository().save(rule);
//     }

//     public AbstractPlayer updatePlayer(Long playerId, int updateType, long updateValue) {
//         logger.info("updatePlayer() - playerId: {}, updateType: {}, updateValue: {}",
//                 playerId, updateType, updateValue);
//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         // strikeRepository.findById(playerId).orElseThrow();
//         switch (updateType) {
//             case CHANNEL -> {
//                 player.noteOff(0, 0);
//                 player.setChannel((int) updateValue);
//                 break;
//             }

//             case NOTE -> {
//                 player.setNote(updateValue);
//                 break;
//             }

//             case INSTRUMENT -> {
//                 Instrument instrument = getMidiInstrumentRepo().findById((long) updateValue).orElseThrow(null);
//                 instrument.setDevice(MIDIService.getMidiDevice(instrument.getDeviceName()));
//                 player.setInstrument(instrument);
//                 break;
//             }

//             case PRESET -> {
//                 try {
//                     player.noteOff(0, 0);
//                     player.setPreset(updateValue);
//                     Instrument instrument = getMidiInstrumentRepo().findById((long) player.getInstrumentId())
//                             .orElseThrow(null);
//                     instrument.setDevice(MIDIService.getMidiDevice(instrument.getDeviceName()));
//                     instrument.programChange(player.getChannel(), updateValue, 0);
//                     player.setInstrument(instrument);
//                 } catch (InvalidMidiDataException | MidiUnavailableException e) {
//                     logger.error(e.getMessage(), e);
//                 }
//                 break;
//             }

//             case PROBABILITY -> {
//                 player.setProbability(updateValue);
//                 break;
//             }

//             case MIN_VELOCITY -> {
//                 player.setMinVelocity(updateValue);
//                 break;
//             }

//             case MAX_VELOCITY -> {
//                 player.setMaxVelocity(updateValue);
//                 break;
//             }

//             case RATCHET_COUNT -> {
//                 player.setRatchetCount(updateValue);
//                 break;
//             }

//             case RATCHET_INTERVAL -> {
//                 player.setRatchetInterval(updateValue);
//                 break;
//             }

//             case MUTE -> {
//                 player.setMuted(updateValue > 0 ? true : false);
//                 break;
//             }

//             case SOLO -> {
//                 player.setSolo(updateValue > 0 ? true : false);
//                 break;
//             }

//             case SKIPS -> {
//                 player.setSkips(updateValue);
//                 player.getSkipCycler().setLength(updateValue);
//                 player.getSkipCycler().reset();
//                 break;
//             }

//             case RANDOM_DEGREE -> {
//                 player.setRandomDegree(updateValue);
//                 break;
//             }

//             case BEAT_FRACTION -> {
//                 player.setBeatFraction(updateValue);
//                 break;
//             }

//             case SUBDIVISIONS -> {
//                 player.setSubDivisions(updateValue);
//                 player.getSubCycler().setLength(updateValue);
//                 player.getSubCycler().reset();
//                 break;
//             }

//             case SWING -> {
//                 player.setSwing(updateValue);
//                 break;
//             }

//             case LEVEL -> {
//                 player.setLevel(updateValue);
//                 break;
//             }

//             case FADE_IN -> {
//                 player.setFadeIn(updateValue);
//                 break;
//             }

//             case FADE_OUT -> {
//                 player.setFadeOut(updateValue);
//                 break;
//             }
//         }

//         if (player instanceof Strike)
//             getStrikeRepository().save((Strike) player);

//         getTickerRepo().save(getTickerService().getTicker());

//         return player;
//     }

//     public Optional<Rule> getRule(Long ruleId) {
//         List<Rule> rules = getPlayers().stream().flatMap(p -> p.getRules().stream()).toList();
//         return rules.stream().filter(r -> r.getId().equals(ruleId)).findAny();
//     }

//     public Rule updateRule(Long ruleId, int updateType, long updateValue) {

//         Optional<Rule> opt = getRule(ruleId);
//         Rule rule = null;
//         if (opt.isPresent()) {
//             rule = opt.get();
//             switch (updateType) {
//                 case OPERATOR -> {
//                     rule.setOperator((int) updateValue);
//                     break;
//                 }

//                 case COMPARISON -> {
//                     rule.setComparison((int) updateValue);
//                     break;
//                 }

//                 case VALUE -> {
//                     rule.setValue((double) updateValue);
//                     break;
//                 }

//                 case RuleUpdateType.PART -> {
//                     rule.setPart((int) updateValue);
//                     break;
//                 }
//             }

//             getRuleRepository().save(rule);
//         }

//         return rule;
//     }

//     // public void updateRule(Long playerId,
//     // Long ruleId,
//     // int operator,
//     // int comparison,
//     // double newValue,
//     // int part) {

//     // Player player = getTickerService().getTicker().getPlayer(playerId);

//     // Rule rule = player.getRule(ruleId);
//     // rule.setOperatorId(operator);
//     // rule.setComparison(comparison);
//     // rule.setValue(newValue);
//     // rule.setPart(part);

//     // getRuleRepository().save(rule);

//     // }

//     public Set<AbstractPlayer> removePlayer(Long playerId) {
//         logger.info("removePlayer() - playerId: {}", playerId);
//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         player.getRules().forEach(r -> ruleRepository.delete(r));
//         getPlayers().remove(player);
//         if (player instanceof Strike)
//             getStrikeRepository().deleteById(player.getId());
//         return getPlayers();
//     }

//     public AbstractPlayer mutePlayer(Long playerId) {
//         AbstractPlayer player = getTickerService().getTicker().getPlayer(playerId);
//         player.setMuted(!player.isMuted());
//         return player;
//     }

//     public void setSteps(List<Pattern> steps) {
//         logger.info("setSteps() - steps size: {}", steps.size());
//         // this.getBeatGenerator().setSteps(steps);
//     }

//     public void save() {
//         try {
//             String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi-bak.json";
//             File file = new File(instruments);
//             if (file.exists())
//                 file.delete();
//             DeviceConfig list = new DeviceConfig();
//             list.getInstruments().addAll(getPlayers().stream().map(p -> p.getInstrument()).distinct().toList());
//             Files.write(file.toPath(),
//                     Collections.singletonList(
//                             PlayerEngine.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list)),
//                     StandardOpenOption.CREATE_NEW);
//         } catch (IOException e) {
//             throw new RuntimeException(e);
//         }
//     }

//     public void saveBeat() {
//         try {
//             Set<AbstractPlayer> strikes = new HashSet<>();
//             getPlayers().forEach(s -> strikes.add((Strike) s));
//             String beatFile = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/beats/" + toString()
//                     + ".json";
//             File file = new File(beatFile);
//             if (file.exists())
//                 file.delete();
//             Files.write(file.toPath(), Collections.singletonList(PlayerEngine.mapper.writerWithDefaultPrettyPrinter()
//                     .writeValueAsString(new BeatConfig(getTickerService().getTicker(), strikes))),
//                     StandardOpenOption.CREATE_NEW);
//         } catch (IOException e) {
//             throw new RuntimeException(e);
//         }
//     }

//     public void clearPlayers() {
//         getPlayers().stream().filter(p -> p.getRules().size() == 0)
//                 .forEach(p -> {
//                     getPlayers().remove(p);
//                     p.setTicker(null);
//                     if (p instanceof Strike)
//                         strikeRepository.delete((Strike) p);
//                 });
//     }

//     public void clearPlayersWithNoRules() {
//         getPlayers().stream().filter(p -> p.getRules().size() == 0)
//                 .forEach(p -> {
//                     if (p.getRules().size() > 0)
//                         return;

//                     getPlayers().remove(p);
//                     p.setTicker(null);
//                     if (p instanceof Strike)
//                         strikeRepository.delete((Strike) p);
//                 });
//     }

//     public Set<AbstractPlayer> getPlayers() {
//         return getTickerService().getTicker().getPlayers();
//     }

//     public void playDrumNote(String instrumentName, int channel, int note) {
//         logger.info("playDrumNote() - instrumentName: {}, channel: {}, note: {}",
//                 instrumentName, channel, note);
//         Instrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
//         midiInstrument.setDevice(MIDIService.getMidiDevice(midiInstrument.getDeviceName()));

//         log.info(String.join(", ", instrumentName, Integer.toString(channel), Integer.toString(note)));

//         if (!midiInstrument.getDevice().isOpen())
//             try {
//                 midiInstrument.getDevice().open();
//                 MidiSystem.getTransmitter().setReceiver(midiInstrument.getDevice().getReceiver());
//             } catch (MidiUnavailableException e) {
//                 throw new RuntimeException(e);
//             }
//         new Thread(new Runnable() {

//             public void run() {
//                 try {
//                     ShortMessage noteOn = new ShortMessage();
//                     noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 126);
//                     midiInstrument.getDevice().getReceiver().send(noteOn, 0L);
//                     // Thread.sleep(5000);
//                     // ShortMessage noteOff = new ShortMessage();
//                     // noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 126);
//                     // midiInstrument.getDevice().getReceiver().send(noteOff, 1000L);
//                 } catch (InvalidMidiDataException | MidiUnavailableException e) {
//                     throw new RuntimeException(e);
//                 }
//             }
//         }).start();
//     }

// }

package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.repo.RuleRepository;
import com.angrysurfer.midi.repo.PlayerInfoRepository;
import com.angrysurfer.midi.repo.TickerInfoRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.angrysurfer.midi.controller.PlayerUpdateType.NOTE;

@Service
public class BeatGeneratorService implements IBeatGeneratorService {

    static final AtomicLong conditionsCounter = new AtomicLong(-0);
    static final Random rand = new Random();
    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    static Logger log = LoggerFactory.getLogger(BeatGeneratorService.class.getCanonicalName());
    static Logger logger = LoggerFactory.getLogger(BeatGenerator.class.getCanonicalName());
    static Integer[] notes = new Integer[]{27, 22, 27, 23};// {-1, 33 - 24, 26 - 24, 21 - 24};
    static Set<Integer> microFreakParams = Set.of(5, 9, 10, 12, 13, 23, 24, 28, 83, 91, 92, 93, 94, 102, 103);
    static Set<Integer> fireballParams = Set.of(40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60);
    static Set<Integer> razParams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);
    static Set<Integer> closedHatParams = Set.of(24, 25, 26, 27, 28, 29, 30, 31);
    static Set<Integer> kickParams = Set.of(1, 2, 3, 4, 12, 13, 14, 15);
    static Set<Integer> snarePrams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);
    static String deviceName = "mrcc";
    static int KICK = 36;
    static int SNARE = 37;
    static int CLOSED_HAT = 38;
    static int OPEN_HAT = 39;
//    static MidiDevice device = getDevice();
    private final BeatGenerator beatGenerator = new BeatGenerator(Integer.MAX_VALUE);
    private IMIDIService midiService;
    private PlayerInfoRepository playerInfoRepository;
    private RuleRepository ruleRepository;

    private TickerInfo tickerInfo;

    private TickerInfoRepo tickerInfoRepo;
    public BeatGeneratorService(IMIDIService midiService, PlayerInfoRepository playerInfoRepository,
                                RuleRepository ruleRepository, TickerInfoRepo tickerInfoRepo) {
        this.midiService = midiService;
        this.ruleRepository = ruleRepository;
        this.playerInfoRepository = playerInfoRepository;
        this.tickerInfoRepo = tickerInfoRepo;
        this.tickerInfo = TickerInfo.fromTicker(beatGenerator);
    }

    @Override
    public boolean play() {
        if (!beatGenerator.isPaused() && !beatGenerator.isPlaying())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    beatGenerator.play();
                }
            }).start();
        else if (beatGenerator.isPaused())
            beatGenerator.pause();
        else beatGenerator.play();

        return beatGenerator.isPlaying();
    }

    @Override
    public boolean stop() {
        if (beatGenerator.isPlaying())
            beatGenerator.stop();

        return beatGenerator.isPlaying();
    }

    @Override
    public boolean pause() {
        beatGenerator.pause();
        return !beatGenerator.isPlaying();
    }

    @Override
    public boolean previous() {
        return false;
    }

    @Override
    public TickerInfo getTickerInfo() {
        return TickerInfo.fromTicker(beatGenerator);
    }

    @Override
    public Map<String, IMidiInstrument> getInstruments() {
        return beatGenerator.getInstrumentMap();
    }

    @Override
    public IMidiInstrument getInstrument(int channel) {
        try {
            return getInstruments().values().stream().filter(i -> i.getChannel() == channel).findAny().orElseThrow();
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void sendMessage(int messageType, int channel, int data1, int data2) {
        IMidiInstrument instrument = getInstrument(channel);
        if (Objects.nonNull(instrument)) {
            List<MidiDevice> devices = this.midiService.findMidiDevice(instrument.getDevice().getDeviceInfo().getName());
            if (!devices.isEmpty()) {
                MidiDevice device = devices.get(0);
                if (!device.isOpen())
                    try {
                        device.open();
                        MidiSystem.getTransmitter().setReceiver(device.getReceiver());
                    } catch (MidiUnavailableException e) {
                        throw new RuntimeException(e);
                    }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ShortMessage message = new ShortMessage();
                            message.setMessage(messageType, channel, data1, data2);
                            device.getReceiver().send(message, 0L);
                            logger.info(String.join(", ",
                                    MidiMessage.lookupCommand(message.getCommand()),
                                    "Channel: ".concat(Integer.valueOf(message.getChannel()).toString()),
                                    "Data 1: ".concat(Integer.valueOf(message.getData1()).toString()),
                                    "Data 2: ".concat(Integer.valueOf(message.getData2()).toString())));
                        } catch (InvalidMidiDataException | MidiUnavailableException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    public void clearPlayers() {
        this.beatGenerator.clearEventSources();
    }

    @Override
    public PlayerInfo addPlayer(String instrument) {
        Rule rule = new Rule();
        rule.setComparisonId(Comparison.EQUALS);
        rule.setOperatorId(Operator.BEAT);
        rule.setValue(1.0);
        ruleRepository.save(rule);

        Strike strike = new Strike(instrument.concat(Integer.toString(getPlayers().size())),
                beatGenerator, beatGenerator.getInstrument(instrument), KICK + getPlayers().size(), closedHatParams);
        strike.getRules().add(rule);
        PlayerInfo info = playerInfoRepository.save(PlayerInfo.fromPlayer(strike));
        strike.setId(info.getId());
        this.beatGenerator.getPlayers().add(strike);
        TickerInfo.copyValues(this.beatGenerator, this.tickerInfo);
        this.tickerInfo.getPlayers().add(info);
        this.tickerInfo = tickerInfoRepo.save(tickerInfo);
        return info;
    }

    @Override
    public void updateRule(Long playerId,
                           int conditionId,
                           int operatorId,
                           int comparisonId,
                           double newValue) {

        Optional<Player> playerOpt = beatGenerator.getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            Optional<Rule> condition = player.getRules().stream().filter(c -> c.getId() == conditionId).findAny();
            if (condition.isPresent()) {
                condition.get().setOperatorId(operatorId);
                condition.get().setComparisonId(comparisonId);
                condition.get().setValue(newValue);
            }
        }
    }

    @Override
    public List<PlayerInfo> removePlayer(Long playerId) {
        Optional<Player> player = beatGenerator.getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        player.ifPresent(p -> beatGenerator.getPlayers().remove(p));
        return getPlayers();
    }

    @Override
    public PlayerInfo mutePlayer(Long playerId) {
        beatGenerator.getPlayers().stream().filter(p -> p.getId() == playerId)
                .findAny().ifPresent(value -> value.setMuted(!value.isMuted()));
        return null;
    }

    @Override
    public List<Rule> getRules(Long playerId) {
        return Collections.emptyList();
    }

    @Override
    public void next() {
        this.beatGenerator.next();
    }

    @Override
    public void save() {
        beatGenerator.save();
    }

    @Override
    public void saveBeat() {
//        beatGenerator.saveBeat(getPlayers());
    }

    @Override
    public void updatePlayer(Long playerId, int updateType, int updateValue) {
        switch (updateType) {
            case NOTE: {
                Optional<Player> playerOpt = beatGenerator.getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
                playerOpt.ifPresent(player -> player.setNote(updateValue));
                break;
            }
        }
    }

    @Override
    public Rule addRule(Long playerId) {
        Rule rule = new Rule();

        Optional<Player> playerOpt = beatGenerator.getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        if (playerOpt.isPresent()) {
            rule.setOperatorId(Operator.BEAT);
            rule.setComparisonId(Comparison.EQUALS);
            rule.setValue(1.0);

            rule = ruleRepository.save(rule);
            playerOpt.get().addCondition(rule);
        }

        return rule;
    }

    @Override
    public void removeRule(Long playerId, Long conditionId) {
        Optional<Player> playerOpt = beatGenerator.getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
        if (playerOpt.isPresent()) {
            Optional<Rule> conditionOpt = playerOpt.get().getRules().stream().filter(c -> c.getId() == conditionId).findAny();
            conditionOpt.ifPresent(rule -> playerOpt.get().getRules().remove(rule));
        }
    }

    @Override
    public List<PlayerInfo> getPlayers() {
        return beatGenerator.getPlayers()
                .stream().map(PlayerInfo::fromPlayer).toList();
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
        IMidiInstrument instrument = beatGenerator.getInstrument(instrumentName);
        log.info(String.join(", ", instrumentName, Integer.toString(channel), Integer.toString(note)));
        if (Objects.nonNull(instrument)) {
            List<MidiDevice> devices = this.midiService.findMidiDevice(instrument.getDevice().getDeviceInfo().getName());
            if (!devices.isEmpty()) {
                MidiDevice device = devices.get(0);
                if (!device.isOpen())
                    try {
                        device.open();
                        MidiSystem.getTransmitter().setReceiver(device.getReceiver());
                    } catch (MidiUnavailableException e) {
                        throw new RuntimeException(e);
                    }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ShortMessage noteOn = new ShortMessage();
                            noteOn.setMessage(ShortMessage.NOTE_ON, channel, note, 127);
                            device.getReceiver().send(noteOn, 0L);
                            Thread.sleep(2500);
                            ShortMessage noteOff = new ShortMessage();
                            noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 127);
                            device.getReceiver().send(noteOff, 0L);

//                             instrument.noteOn(channel, note);
//                            Thread.sleep(2500);
//                             instrument.noteOff(channel, note);
                        } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }
    }
}

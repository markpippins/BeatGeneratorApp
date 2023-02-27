package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.repo.RuleRepository;
import com.angrysurfer.midi.repo.StrikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.*;

import static com.angrysurfer.midi.controller.PlayerUpdateType.NOTE;
import static com.angrysurfer.midi.service.MidiInstrument.logger;

@Service
public class BeatGeneratorService implements IBeatGeneratorService {

    static Logger log = LoggerFactory.getLogger(BeatGeneratorService.class.getCanonicalName());

    private final BeatGenerator beatGenerator = new BeatGenerator(Integer.MAX_VALUE);
    private IMIDIService midiService;

    private StrikeRepository strikeRepository;

    private RuleRepository ruleRepository;

    public BeatGeneratorService(IMIDIService midiService, StrikeRepository strikeRepository,
                                RuleRepository ruleRepository) {
        this.midiService = midiService;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
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
        return this.beatGenerator.addPlayer(instrument);
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
    public PlayerInfo removePlayer(Long playerId) {
        Optional<Player> player = beatGenerator.getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
        player.ifPresent(p -> beatGenerator.getRemoveList().add(p));
        return null;
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

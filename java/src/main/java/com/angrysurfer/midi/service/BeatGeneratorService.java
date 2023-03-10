package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.MidiMessage;
import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.MidiInstrumentRepository;
import com.angrysurfer.midi.repo.PlayerInfoRepository;
import com.angrysurfer.midi.repo.RuleRepository;
import com.angrysurfer.midi.repo.TickerInfoRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Getter
public class BeatGeneratorService {
    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger(BeatGeneratorService.class.getCanonicalName());
    static Integer[] notes = new Integer[]{27, 22, 27, 23};// {-1, 33 - 24, 26 - 24, 21 - 24};
    static Set<Integer> microFreakParams = Set.of(5, 9, 10, 12, 13, 23, 24, 28, 83, 91, 92, 93, 94, 102, 103);
    static Set<Integer> fireballParams = Set.of(40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60);
    static Set<Integer> razParams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);
    static Set<Integer> closedHatParams = Set.of(24, 25, 26, 27, 28, 29, 30, 31);
    static Set<Integer> kickParams = Set.of(1, 2, 3, 4, 12, 13, 14, 15);
    static Set<Integer> snarePrams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);
    static int KICK = 36;
    static int SNARE = 37;
    static int CLOSED_HAT = 38;
    static int OPEN_HAT = 39;
    private final MidiInstrumentRepository midiInstrumentRepo;
    private final ControlCodeRepository controlCodeRepository;
    private final PadRepository padRepository;
    private Ticker ticker;
    private MIDIService midiService;
    private PlayerInfoRepository playerInfoRepository;
    private RuleRepository ruleRepository;
    private TickerInfoRepo tickerInfoRepo;
    // private Map<String, MidiInstrument> instrumentMap = new HashMap<>();

    public BeatGeneratorService(MIDIService midiService, PlayerInfoRepository playerInfoRepository,
                                RuleRepository ruleRepository, TickerInfoRepo tickerInfoRepo,
                                MidiInstrumentRepository midiInstrumentRepository,
                                ControlCodeRepository controlCodeRepository,
                                PadRepository padRepository) {
        this.midiService = midiService;
        this.ruleRepository = ruleRepository;
        this.playerInfoRepository = playerInfoRepository;
        this.padRepository = padRepository;
        this.tickerInfoRepo = tickerInfoRepo;
        this.midiInstrumentRepo = midiInstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;

        this.ticker = new Ticker();
        loadConfig();
    }

    public static MidiDevice getDevice(String deviceName) {
        try {
            MidiDevice result = MidiSystem.getMidiDevice(Stream.of(MidiSystem.getMidiDeviceInfo()).
                    filter(info -> info.getName().toLowerCase().contains(deviceName.toLowerCase())).toList().get(0));
            return result;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadConfig() {

        // getInstrumentMap().clear();
        if (midiInstrumentRepo.findAll().isEmpty())
            try {
                String filepath = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
//                String filepath = "resources/config/midi.json";
                MidiInstrumentList config = mapper.readValue(new File(filepath), MidiInstrumentList.class);

                config.getInstruments().forEach(instrument -> {
                    instrument = midiInstrumentRepo.save(instrument);
                    MidiInstrument finalInstrumentDef = instrument;
                    instrument.getAssignments().keySet().forEach(code -> {
                        ControlCode controlCode = new ControlCode();
                        controlCode.setCode(code);
                        controlCode.setName(finalInstrumentDef.getAssignments().get(code));
                        if (finalInstrumentDef.getBoundaries().containsKey(code)) {
                            controlCode.setLowerBound(finalInstrumentDef.getBoundaries().get(code)[0]);
                            controlCode.setUpperBound(finalInstrumentDef.getBoundaries().get(code)[1]);
                        }
                        controlCode = controlCodeRepository.save(controlCode);
                        finalInstrumentDef.getControlCodes().add(controlCode);
                    });
                    instrument = midiInstrumentRepo.save(finalInstrumentDef);
                    addPadInfo(instrument);
                    // getInstrumentMap().put(instrument.getName(), instrument);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        
        // else midiInstrumentRepo.findAll().forEach(instru -> getInstrumentMap().put(instru.getName(), instru));

        // getInstrumentMap().values().forEach(i -> {
        //     MidiDevice device = getDevice(i.getDeviceName()); 
        //     if (midiService.select(device)) 
        //         i.setDevice(device);
        // });
    }

    public void saveConfig() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
            // String instruments = "resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists()) file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getTicker().getPlayers().stream().map(p -> p.getInstrument()).distinct().toList());
            Files.write(file.toPath(), Collections.singletonList(BeatGeneratorService.mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(list)), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }    
    }


    private void addPadInfo(MidiInstrument instrumentInfo) {
        int padCount = instrumentInfo.getHighestNote() - instrumentInfo.getLowestNote();
        if (padCount == 7) {
            List<Pad> pads = new ArrayList<>(IntStream.range(0, 8).mapToObj(i -> new Pad()).toList());
            instrumentInfo.getControlCodes().forEach(cc -> {
                if (kickParams.contains(cc.getCode()))
                    pads.get(0).getControlCodes().add(cc.getCode());
                    
                if (snarePrams.contains(cc.getCode()))
                    pads.get(1).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 23 && cc.getCode() < 29)
                    pads.get(2).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 28 && cc.getCode() < 32)
                    pads.get(3).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 31 && cc.getCode() < 40)
                    pads.get(4).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 39 && cc.getCode() < 45)
                    pads.get(3).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 44 && cc.getCode() < 56)
                    pads.get(5).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 55 && cc.getCode() < 64)
                    pads.get(6).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 63 && cc.getCode() < 72)
                    pads.get(7).getControlCodes().add(cc.getCode());
            });

            pads.get(0).setName("Kick");
            pads.get(1).setName("Snare");
            pads.get(2).setName("Hi-Hat Closed");
            pads.get(3).setName("Hi-Hat Open");
            pads.get(4).setName("Ride");
            pads.get(5).setName("Low Tom");
            pads.get(6).setName("Mid Tom");
            pads.get(7).setName("Hi Tom");

            pads.forEach(pad -> instrumentInfo.getPads().add(padRepository.save(pad)));
            midiInstrumentRepo.save(instrumentInfo);
        }
    }

    public boolean play() {
        if (!getTicker().isPaused() && !getTicker().isPlaying())
            new Thread(new Runnable() {

                public void run() {
                    getTicker().run();
                }
            }).start();
        else if (getTicker().isPaused())
            getTicker().pause();
        else getTicker().run();

        return getTicker().isPlaying();
    }


    public TickerInfo stop() {
        if (getTicker().isPlaying())
            getTicker().stop();
        return TickerInfo.fromTicker(getTicker(), getPlayers());
    }


    public boolean pause() {
        getTicker().pause();
        return !getTicker().isPlaying();
    }

    public TickerInfo getTickerInfo() {
        return TickerInfo.fromTicker(ticker, getPlayers());
    }

    public TickerInfo getTickerStatus() {
        return TickerInfo.fromTicker(getTicker(), Collections.emptyList());
    }

    public List<TickerInfo> getAllTickerInfo() {
        return tickerInfoRepo.findAll();
    }

    public List<MidiInstrument> getAllInstruments() {
            List<MidiInstrument> results = midiInstrumentRepo.findAll();
        results.forEach(i -> i.setDevice(getDevice(i.getDeviceName())));
        return results;
    }

    public MidiInstrument getInstrument(int channel) {
        Optional<MidiInstrument> instrument = midiInstrumentRepo.findByChannel(channel);
        if (instrument.isPresent())
            instrument.get().setDevice(getDevice(instrument.get().getDeviceName()));
        return instrument.orElseThrow();
    }


    public void sendMessage(int messageType, int channel, int data1, int data2) {
        MidiInstrument instrument = getInstrument(channel);
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

                    public void run() {
                        try {
                            ShortMessage message = new ShortMessage();
                            message.setMessage(messageType, channel, data1, data2);
                            device.getReceiver().send(message, 0L);
                            log.info(String.join(", ",
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

    public PlayerInfo addPlayer(String instrumentName) {

        MidiInstrument midiInstrument = midiInstrumentRepo.findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(getDevice(midiInstrument.getDeviceName()));
        Strike strike = new Strike(instrumentName.concat(Integer.toString(getPlayers().size())),
                    ticker, midiInstrument, KICK + getPlayers().size(), closedHatParams);

        PlayerInfo playerInfo = PlayerInfo.fromPlayer(strike);
        playerInfo.setTickerId(this.getTicker().getId());
        playerInfoRepository.save(playerInfo);

        strike.setId(playerInfo.getId());
        this.getTicker().getPlayers().add(strike);
     
        return playerInfo;
    }


    public void updateRule(Long playerId,
                           int conditionId,
                           int operatorId,
                           int comparisonId,
                           double newValue) {

        Optional<Player> playerOpt = getTicker().getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
        if (playerOpt.isPresent()) {
            Player player = playerOpt.get();
            Optional<Rule> rule = player.getRules().stream().filter(c -> c.getId() == conditionId).findAny();
            if (rule.isPresent()) {
                rule.get().setOperatorId(operatorId);
                rule.get().setComparisonId(comparisonId);
                rule.get().setValue(newValue);
                ruleRepository.save(rule.get());
            }
        }
    }


    public List<PlayerInfo> removePlayer(Long playerId) {
        Optional<Player> player = getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        player.ifPresent(p -> getTicker().getPlayers().remove(p));
        player.ifPresent(p -> playerInfoRepository.deleteById(p.getId()));
        return getPlayers();
    }


    public PlayerInfo mutePlayer(Long playerId) {
        Player player = getTicker().getPlayers().stream().filter(p -> p.getId() == playerId)
                .findAny().orElseThrow();
        player.setMuted(!player.isMuted());
        return PlayerInfo.fromPlayer(player);
    }


    public Set<Rule> getRules(Long playerId) {
        Set<Rule> result = new HashSet<>();
        Optional<PlayerInfo> playerOpt = getPlayers().stream().filter(p -> p.getId().equals(playerId)).findAny();
        if (playerOpt.isPresent())
            result = playerOpt.get().getRules();
        return result;
    }


    public TickerInfo next(long currentTickerId) {
        if (currentTickerId > 0)
            saveCurrentRecord(currentTickerId);

        Optional<TickerInfo> currentTickerInfo = tickerInfoRepo.findById(currentTickerId);

        if ((getTicker().getPlayers().size() > 0) || currentTickerId == 0) {
            this.getTicker().reset();
            TickerInfo tickerInfo = tickerInfoRepo.getNextTicker(currentTickerId);
            if (Objects.isNull(tickerInfo))
                tickerInfo = tickerInfoRepo.save(new TickerInfo());
            else {
                tickerInfo.getPlayers().clear();
                TickerInfo.copyToTicker(tickerInfo, this.ticker);
                List<PlayerInfo> players = playerInfoRepository.findByTickerId(tickerInfo.getId());
                tickerInfo.getPlayers().addAll(players);

                for (PlayerInfo info : tickerInfo.getPlayers()) {
                    MidiInstrument instrument =  getInstrument(info.getChannel());
                    Strike strike = new Strike(instrument.getName().concat(Integer.toString(getPlayers().size())),
                            getTicker(), instrument, KICK + getPlayers().size(), closedHatParams);
                        PlayerInfo.copyValues(info, strike);
                    getTicker().getPlayers().add(strike);
                }
            }
            this.getTicker().setId(tickerInfo.getId());
            return tickerInfo;
        }
        return currentTickerInfo.orElseThrow();
    }

    public TickerInfo previous(long currentTickerId) {
        if (currentTickerId > 0)
            saveCurrentRecord(currentTickerId);

        if (currentTickerId >  1) {
            TickerInfo tickerInfo = tickerInfoRepo.getPreviousTicker(currentTickerId);
            if (Objects.nonNull(tickerInfo)) {
                this.getTicker().reset();
                this.getTicker().setId(tickerInfo.getId());
                List<PlayerInfo> players = playerInfoRepository.findByTickerId(tickerInfo.getId());
                tickerInfo.getPlayers().addAll(players);
                TickerInfo.copyToTicker(tickerInfo, this.ticker);
            }


            for (PlayerInfo info : tickerInfo.getPlayers()) {
                MidiInstrument instrument =  getInstrument(info.getChannel());
                Strike strike = new Strike(instrument.getName().concat(Integer.toString(getPlayers().size())),
                        getTicker(), instrument, KICK + getPlayers().size(), closedHatParams);
                PlayerInfo.copyValues(info, strike);
                getTicker().getPlayers().add(strike);
            }

            return tickerInfo;
        }

        return tickerInfoRepo.findById(1L).orElseThrow();
    }

    private void saveCurrentRecord(long currentTickerId) {
        Optional<TickerInfo> tickerInfoOpt = this.tickerInfoRepo.findById(currentTickerId);
        if (tickerInfoOpt.isPresent()) {
            TickerInfo info = tickerInfoOpt.get();
            TickerInfo.copyFromTicker(getTicker(), info, getPlayers());
            this.tickerInfoRepo.save(info);
        }
    }


    public void setSteps(List<StepData> steps) {
    //    this.getBeatGenerator().setSteps(steps);
    }


    public void save() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists()) file.delete();
            MidiInstrumentList list = new MidiInstrumentList();
            list.getInstruments().addAll(getTicker().getPlayers().stream().map(p -> p.getInstrument()).distinct().toList());
            Files.write(file.toPath(), Collections.singletonList(BeatGeneratorService.mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(list)), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveBeat() {
        try {
            List<Strike> strikes = new ArrayList<>();
            getTicker().getPlayers().stream().filter(p -> p instanceof Strike).forEach(s -> strikes.add((Strike) s));
            String beatFile = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/beats/" + toString() + ".json";
            File file = new File(beatFile);
            if (file.exists()) file.delete();
            Files.write(file.toPath(), Collections.singletonList(BeatGeneratorService.mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new BeatGeneratorConfig(getTicker(), strikes))), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadBeat(String fileNamem) {
        // try {
        //     String filepath = "resources/beats/" + toString() + ".json";
        //     File file = new File(filepath);
        //     if (!file.exists()) {
        //         file.createNewFile();
        //     }

        //     getPlayers().clear();
        //     BeatGeneratorConfig config = BeatGeneratorService.mapper.readValue(new File(filepath), BeatGeneratorConfig.class);
        //     config.setup(this);

        //     config.getPlayers().forEach(drumPadDef -> {
        //         Strike pad = new Strike();
        //         MidiInstrument instrument = new MidiInstrument(null, BeatGeneratorService.getDevice(), drumPadDef.getChannel());
        //         instrument.setDevice(device);
        //         pad.setInstrument(instrument);
        //         pad.setNote(drumPadDef.getNote());
        //         pad.setPreset(drumPadDef.getPreset());
        //         pad.setRules(drumPadDef.getRules());
        //         pad.setAllowedControlMessages(drumPadDef.getAllowedControlMessages());
        //         pad.setMaxVelocity(drumPadDef.getMaxVelocity());
        //         pad.setMinVelocity(drumPadDef.getMaxVelocity());
        //         pad.setTicker(this);
        //         getPlayers().add(pad);
        //     });
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }
    }
    

    public void updatePlayer(Long playerId, int updateType, int updateValue) {
        Optional<PlayerInfo> playerInfoOpt = playerInfoRepository.findById(playerId);
        switch (updateType) {
            case NOTE: {
                playerInfoOpt.ifPresent(playerInfo -> {
                    playerInfo.setNote(updateValue);
                    playerInfoRepository.save(playerInfo);
                    Optional<Player> player = getTicker().getPlayers().stream().filter(p->p.getId().equals(playerId)).findFirst();
                    player.ifPresent(p -> p.setNote(updateValue));
                });
                break;
            }
            case INSTRUMENT: {
                Optional<MidiInstrument> instrument = midiInstrumentRepo.findById((long) updateValue);
                instrument.ifPresent(inst -> {
                    inst.setDevice(getDevice(inst.getDeviceName()));
                    playerInfoOpt.ifPresent(playerInfo -> {
                        playerInfo.setInstrument(inst);
                        playerInfoRepository.save(playerInfo);
                        Optional<Player> player = getTicker().getPlayers().stream().filter(p->p.getId().equals(playerId)).findFirst();
                        player.ifPresent(p -> p.setInstrument(inst));
                    });
                });

                break;
            }
        }
    }


    public Rule addRule(Long playerId) {
        Rule rule = new Rule();

        Optional<Player> playerOpt = getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        if (playerOpt.isPresent()) {
            rule.setOperatorId(Operator.BEAT);
            rule.setComparisonId(Comparison.EQUALS);
            rule.setValue(1.0);

            rule = ruleRepository.save(rule);
            this.ruleRepository.save(rule);
            playerOpt.get().addRule(rule);

            PlayerInfo info = getPlayers().stream().filter(p -> p.getId().equals(playerOpt.get().getId())).findFirst().orElseThrow();
            info.getRules().add(rule);
            this.playerInfoRepository.save(info);
        }

        return rule;
    }


    public void removeRule(Long playerId, Long conditionId) {
        Optional<Player> playerOpt = getTicker().getPlayers().stream().filter(p -> p.getId() == playerId).findAny();
        if (playerOpt.isPresent()) {
            Optional<Rule> conditionOpt = playerOpt.get().getRules().stream().filter(c -> c.getId() == conditionId).findAny();
            conditionOpt.ifPresent(rule -> playerOpt.get().getRules().remove(rule));
        }
    }


    public TickerInfo loadTicker(long tickerId) {
        TickerInfo result = null;

        Optional<TickerInfo> infoOpt = tickerInfoRepo.findById(tickerId);
        if (infoOpt.isPresent()) {
            result = infoOpt.get();
            TickerInfo.copyToTicker(result, ticker);
            // getInstrumentMap().values().forEach(i -> i.setDevice(getDevice(i.getDeviceName())));
        }

        return result;
    }


    public TickerInfo newTicker() {
        this.ticker = new Ticker();
        TickerInfo result = new TickerInfo();
        tickerInfoRepo.save(result);
        this.getTicker().setId(result.getId());
        return result;
    }


    public List<PlayerInfo> getPlayers() {
        return playerInfoRepository.findByTickerId(getTicker().getId());
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
 
        MidiInstrument midiInstrument = midiInstrumentRepo.findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(getDevice(midiInstrument.getDeviceName()));

        log.info(String.join(", ", instrumentName, Integer.toString(channel), Integer.toString(note)));
 
        // List<MidiDevice> devices = this.midiService.findMidiDevice(instrument.getDevice().getDeviceInfo().getName());
        // if (!devices.isEmpty()) {
            // MidiDevice device = devices.get(0);
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
        // }
    }

    
    public void clearPlayers() {
        getTicker().getPlayers().clear();
    }

    public TickerInfo updateTicker(Long tickerId, int updateType, int updateValue) {

        Optional<TickerInfo> tickerInfoOpt = tickerInfoRepo.findById(tickerId);
        
        TickerInfo info = tickerInfoOpt.orElseThrow();
        switch (updateType) {
            case PPQ : {
                    info.setTicksPerBeat(updateValue);
                    getTicker().setTicksPerBeat(updateValue);
                    info = tickerInfoRepo.save(info);
                };

                break;

            case BPM: {
                    info.setTempoInBPM(updateValue);
                    getTicker().setTempoInBPM(Float.valueOf(updateValue));                
                    info = tickerInfoRepo.save(info);
                };

                break;

            case BEATS_PER_BAR: {
                info.setTempoInBPM(updateValue);
                getTicker().setBeatsPerBar(updateValue);                
                info = tickerInfoRepo.save(info);
            };

            case PART_LENGTH: {
                info.setTempoInBPM(updateValue);
                getTicker().setPartLength(updateValue);                
                info = tickerInfoRepo.save(info);
            };

            break;

        }

        return info;
    }
        
}

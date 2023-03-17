package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.ControlCodeRepository;
import com.angrysurfer.midi.repo.MidiInstrumentRepository;
import com.angrysurfer.midi.repo.PadRepository;
import com.angrysurfer.midi.repo.PlayerInfoRepository;
import com.angrysurfer.midi.repo.RuleRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.repo.TickerRepo;
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
    private PlayerInfoRepository playerInfoRepository;
    private RuleRepository ruleRepository;
    private TickerRepo tickerRepo;
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    static Sequencer sequencer;        


    public PlayerService(MIDIService midiService, PlayerInfoRepository playerInfoRepository,
                                RuleRepository ruleRepository, TickerRepo tickerRepo, 
                                MidiInstrumentRepository midiInstrumentRepository,
                                ControlCodeRepository controlCodeRepository,
                                PadRepository padRepository,
                                StepRepository stepRepository,
                                SongRepository songRepository) {

        this.midiService = midiService;
        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.playerInfoRepository = playerInfoRepository;
        this.padRepository = padRepository;
        this.midiInstrumentRepo = midiInstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        
        try {
            sequencer = MidiSystem.getSequencer();
            setTicker(new Ticker());
            getTicker().setSequencer(sequencer);
            } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }    
    }


    public void saveConfig() {
        try {
            String instruments = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
            // String instruments = "resources/config/midi-bak.json";
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

    public Ticker stop() {
        if (getTicker().isPlaying())
            getTicker().stop();
        return getTicker();
    }

    public boolean pause() {
        getTicker().pause();
        return !getTicker().isPlaying();
    }

    public Ticker getTickerInfo() {
        return getTicker();
    }
    
    public Ticker getTickerStatus() {
        return getTicker();
    }

    public List<Ticker> getAllTickerInfo() {
        return tickerRepo.findAll();
    }

    public List<PlayerInfo> getPlayerInfos(Long tickerId) {
        return playerInfoRepository.findByTickerId(tickerId);
    }


    public PlayerInfo addPlayer(String instrumentName) {

        MidiInstrument midiInstrument = getMidiInstrumentRepo().findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(MIDIService.getDevice(midiInstrument.getDeviceName()));
        List<PlayerInfo> infos = getPlayerInfos(getTicker().getId());
        Strike strike = new Strike(instrumentName.concat(Integer.toString(infos.size())),
                    ticker, midiInstrument, Strike.KICK + infos.size(), Strike.closedHatParams);

        PlayerInfo playerInfo = PlayerInfo.fromPlayer(strike);
        playerInfo.setTickerId(this.getTicker().getId());
        getPlayerInfoRepository().save(playerInfo);

        strike.setId(playerInfo.getId());
        this.getTicker().getPlayers().add(strike);
     
        return playerInfo;
    }

    public PlayerInfo addPlayer(Long instrumentId) {
        MidiInstrument midiInstrument = getMidiInstrumentRepo().findById(instrumentId).orElseThrow();
        midiInstrument.setDevice(MIDIService.getDevice(midiInstrument.getDeviceName()));
        List<PlayerInfo> infos = getPlayerInfos(getTicker().getId());
        Strike strike = new Strike(midiInstrument.getName().concat(Integer.toString(infos.size())),
                    ticker, midiInstrument, Strike.KICK + infos.size(), Strike.closedHatParams);

        PlayerInfo playerInfo = PlayerInfo.fromPlayer(strike);
        playerInfo.setTickerId(this.getTicker().getId());
        getPlayerInfoRepository().save(playerInfo);

        strike.setId(playerInfo.getId());
        this.getTicker().getPlayers().add(strike);
     
        return playerInfo;
    }


    public List<Rule> getRules(Long playerId) {
        return this.ruleRepository.findByPlayerId(playerId);
    }

    public Rule addRule(Long playerId) {
        Rule[] rules = { new Rule() };
        rules[0].setOperatorId(Operator.BEAT);
        rules[0].setComparisonId(Comparison.EQUALS);
        rules[0].setValue(1.0);
        rules[0] = ruleRepository.save(rules[0]);

        PlayerInfo info = this.playerInfoRepository.findById(playerId).orElseThrow(); 
        info.getRules().add(rules[0]);
        this.playerInfoRepository.save(info);
        getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny().ifPresent(p -> p.setRules(info.getRules()));
        return rules[0];
    }

    public void removeRule(Long playerId, Long ruleId) {

       getRuleRepository().deleteById(ruleId);
       getTicker().getPlayers().stream().filter(p -> p.getId() == playerId).findAny()
        .ifPresent(p -> p.getRules().stream().filter(c -> c.getId() == ruleId).findAny()
        .ifPresent(rule -> p.getRules().remove(rule)));
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
                getRuleRepository().save(rule.get());
            }
        }
    }

    public List<PlayerInfo> removePlayer(Long playerId) {
        Optional<Player> player = getTicker().getPlayers().stream().filter(p -> Objects.equals(p.getId(), playerId)).findAny();
        player.ifPresent(p -> getTicker().getPlayers().remove(p));
        player.ifPresent(p -> getPlayerInfoRepository().deleteById(p.getId()));
        return getPlayerInfoRepository().findByTickerId(getTicker().getId());
    }


    public PlayerInfo mutePlayer(Long playerId) {
        Player player = getTicker().getPlayers().stream().filter(p -> p.getId() == playerId)
                .findAny().orElseThrow();
        player.setMuted(!player.isMuted());
        return PlayerInfo.fromPlayer(player);
    }

    public synchronized Ticker next(long currentTickerId) {

        if (currentTickerId == 0 || this.playerInfoRepository.findByTickerId(currentTickerId).size() > 0) {
            
            tickerRepo.flush();
            Long maxTickerId = tickerRepo.getMaximumTickerId();
            int tickerCount = getTickerRepo().findAll().size(); 
            if (tickerCount > 0 && Objects.nonNull(maxTickerId) && currentTickerId < maxTickerId) {
                Long id = getTickerRepo().getNextTickerId(currentTickerId);
                Ticker newTicker = getTickerRepo().findById(id).orElseThrow();
                setTicker(newTicker);
                getTicker().setSequencer(sequencer);
            }
            else {
                Ticker newTicker = new Ticker();
                getTickerRepo().save(newTicker);
                setTicker(newTicker);
                getTicker().setSequencer(sequencer);
            }

            List<PlayerInfo> infos = playerInfoRepository.findByTickerId(ticker.getId());
            for (PlayerInfo info : infos) {
                MidiInstrument instrument =  getMidiService().getInstrument(info.getChannel());
                Strike strike = new Strike(instrument.getName().concat(Integer.toString(infos.size())),
                        getTicker(), instrument, Strike.KICK + infos.size(), Strike.closedHatParams);
                    PlayerInfo.copyValues(info, strike);
                getTicker().getPlayers().add(strike);
            }

        }

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId) {
        if (currentTickerId >  (getTickerRepo().getMinimumTickerId())) {
            setTicker(getTickerRepo().getPreviousTicker(currentTickerId));
            getTicker().setSequencer(sequencer);

            List<PlayerInfo> infos = playerInfoRepository.findByTickerId(ticker.getId());
            for (PlayerInfo info : infos) {
                MidiInstrument instrument =  getMidiService().getInstrument(info.getChannel());
                Strike strike = new Strike(instrument.getName().concat(Integer.toString(infos.size())),
                        getTicker(), instrument, Strike.KICK + infos.size(), Strike.closedHatParams);
                PlayerInfo.copyValues(info, strike);
                getTicker().getPlayers().add(strike);
            }
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
            List<Strike> strikes = new ArrayList<>();
            getTicker().getPlayers().stream().filter(p -> p instanceof Strike).forEach(s -> strikes.add((Strike) s));
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

        Ticker ticker = getTickerRepo().findById(tickerId).orElseThrow();

        switch (updateType) {
            case PPQ : ticker.setTicksPerBeat(updateValue);
                break;

            case BPM: ticker.setTempoInBPM(Float.valueOf(updateValue));
                break;

            case BEATS_PER_BAR: ticker.setBeatsPerBar(updateValue);
                break;

            case PART_LENGTH:
                ticker.setPartLength(updateValue);
                break;

        }

        ticker = getTickerRepo().save(ticker);
        if (getTicker().getId().equals(tickerId)) {
            setTicker(ticker);
            getTicker().setSequencer(sequencer);
        }
            

        return ticker;
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
                    inst.setDevice(MIDIService.getDevice(inst.getDeviceName()));
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

    public Ticker loadTicker(long tickerId) {
        tickerRepo.findById(tickerId).ifPresent(t -> setTicker(t));
        getTicker().setSequencer(sequencer);            
        return getTicker();
    }


    public Ticker newTicker() {
        setTicker(new Ticker());
        getTicker().setSequencer(sequencer);
        return tickerRepo.save(ticker);
    }

    public List<PlayerInfo> getPlayers() {
        return Objects.nonNull(getTicker()) ? playerInfoRepository.findByTickerId(getTicker().getId()) : Collections.emptyList();
    }

    public void playDrumNote(String instrumentName, int channel, int note) {
 
        MidiInstrument midiInstrument = midiInstrumentRepo.findByName(instrumentName).orElseThrow();
        midiInstrument.setDevice(MIDIService.getDevice(midiInstrument.getDeviceName()));

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

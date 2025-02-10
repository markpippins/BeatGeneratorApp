package com.angrysurfer.spring.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Pad;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.ControlCode;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.player.AbstractPlayer;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.util.db.Delete;
import com.angrysurfer.core.util.db.FindList;
import com.angrysurfer.core.util.db.FindOne;
import com.angrysurfer.core.util.db.FindSet;
import com.angrysurfer.core.util.db.Next;
import com.angrysurfer.core.util.db.Prior;
import com.angrysurfer.core.util.db.Save;
import com.angrysurfer.spring.repo.ControlCodes;
import com.angrysurfer.spring.repo.Instruments;
import com.angrysurfer.spring.repo.Pads;
import com.angrysurfer.spring.repo.Patterns;
import com.angrysurfer.spring.repo.Rules;
import com.angrysurfer.spring.repo.Songs;
import com.angrysurfer.spring.repo.Steps;
import com.angrysurfer.spring.repo.Strikes;
import com.angrysurfer.spring.repo.Tickers;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class DBUtils {

    private Instruments InstrumentRepo;
    private ControlCodes controlCodeRepository;
    private Pads padRepository;
    private Patterns patternRepository;
    private Strikes strikeRepository;
    private Rules ruleRepository;
    private Tickers tickerRepo;
    private Steps stepDataRepository;
    private Songs songRepository;

    public DBUtils(Strikes strikeRepository,
            Rules ruleRepository, Tickers tickerRepo,
            Instruments InstrumentRepository,
            ControlCodes controlCodeRepository,
            Pads padRepository,
            Patterns patternRepository,
            Steps stepRepository,
            Songs songRepository) {

        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
        this.padRepository = padRepository;
        this.InstrumentRepo = InstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        this.patternRepository = patternRepository;
    };

    private FindSet<Rule> playerRuleFindSet = (Long playerId) -> {
        return getRuleRepository().findByPlayerId(playerId);
    };

    private Save<Rule> ruleSaver = (Rule r) -> {
        return getRuleRepository().save(r);
    };

    private Delete<Rule> ruleDeleter = (Rule r) -> getStrikeRepository().deleteById(r.getId());

    private Save<AbstractPlayer> playerSaver = (AbstractPlayer p) -> {
        if (p instanceof Strike)
            return getStrikeRepository().save((Strike) p);

        p.setUnsaved(true);
        return p;
    };

    private Delete<AbstractPlayer> playerDeleter = (AbstractPlayer p) -> {
        if (p instanceof Strike)
            getStrikeRepository().deleteById(p.getId());
    };

    private FindSet<Strike> tickerStrikeFinder = (Long id) -> {
        return strikeRepository.findByTickerId(id);
    };

    private FindSet<Step> patternStepFinder = (Long id) -> {
        return stepDataRepository.findByPatternId(id);
    };

    private FindSet<Pattern> songPatternFinder = (Long id) -> {
        return patternRepository.findBySongId(id);
    };

    private FindOne<Ticker> tickerFindOne = (Long id) -> {
        return tickerRepo.findById(id);
    };

    private FindList<Ticker> tickerFinder = (Long id) -> {
        return tickerRepo.findAll();
    };

    private Save<Ticker> tickerSaver = (Ticker t) -> {
        return getTickerRepo().save(t);
    };

    private Next<Ticker> tickerForward = (Long id) -> {
        return getTickerRepo().getNextTicker(id);
    };

    private Prior<Ticker> tickerBack = (Long id) -> {
        return getTickerRepo().getPreviousTicker(id);
    };

    private com.angrysurfer.core.util.db.Max<Ticker> tickerMax = () -> {
        return getTickerRepo().getMaximumTickerId();
    };

    private com.angrysurfer.core.util.db.Max<Ticker> tickerMin = () -> {
        return getTickerRepo().getMinimumTickerId();
    };

    private Save<Song> songSaver = (Song s) -> {
        return getSongRepository().save(s);
    };

    private Delete<Song> songDeleter = (Song s) -> getSongRepository().deleteById(s.getId());

    private Save<Step> stepSaver = (Step s) -> {
        return getStepDataRepository().save(s);
    };

    private Delete<Step> stepDeleter = (Step s) -> getStepDataRepository().deleteById(s.getId());

    private Save<Pad> padSaver = (Pad p) -> {
        return getPadRepository().save(p);
    };

    private Delete<Pad> padDeleter = (Pad p) -> getPadRepository().deleteById(p.getId());

    private Save<Pattern> patternSaver = (Pattern p) -> {
        return getPatternRepository().save(p);
    };

    private Delete<Pattern> patternDeleter = (Pattern p) -> getPatternRepository().deleteById(p.getId());

    private Save<Instrument> instrumentSaver = (Instrument i) -> {
        return getInstrumentRepo().save(i);
    };

    private Delete<Instrument> instrumentDeleter = (Instrument i) -> getInstrumentRepo().deleteById(i.getId());

    private Save<ControlCode> controlCodeSaver = (ControlCode c) -> {
        return getControlCodeRepository().save(c);
    };

    private Delete<ControlCode> controlCodeDeleter = (ControlCode c) -> getControlCodeRepository()
            .deleteById(c.getId());

    public Rule saveRule(Rule rule) {
        return ruleSaver.save(rule);
    }

    public void deleteRule(Rule rule) {
        ruleDeleter.delete(rule);
    }

    public Set<Strike> strikesForTicker(Long tickerId) {
        return strikeRepository.findByTickerId(tickerId);
    }

    public AbstractPlayer savePlayer(AbstractPlayer player) {
        return playerSaver.save(player);
    }

    public void deletePlayer(AbstractPlayer player) {
        playerDeleter.delete(player);
    }

    public Ticker saveTicker(Ticker ticker) {
        return tickerSaver.save(ticker);
    }

    public void deleteTicker(Long id) {
        tickerRepo.deleteById(id);
    }

    public void deleteAllTickers() {
        tickerRepo.deleteAll();
    }

    public Rule findRuleById(Long id) {
        return ruleRepository.findById(id).orElse(null);
    }

    public Ticker findTickerById(Long id) {
        return tickerRepo.findById(id).orElse(null);
    }

    public Strike findStrikeById(Long id) {
        return strikeRepository.findById(id).orElse(null);
    }

    public Song saveSong(Song song) {
        return songSaver.save(song);
    }

    public void deleteSong(Song song) {
        songDeleter.delete(song);
    }

    public Step saveStep(Step step) {
        return stepSaver.save(step);
    }

    public void deleteStep(Step step) {
        stepDeleter.delete(step);
    }

    public Pad savePad(Pad pad) {
        return padSaver.save(pad);
    }

    public void deletePad(Pad pad) {
        padDeleter.delete(pad);
    }

    public Pattern savePattern(Pattern p) {
        return patternSaver.save(p);
    }

    public void deletePattern(Pattern p) {
        patternDeleter.delete(p);
    }

    public Instrument saveInstrument(Instrument instrument) {
        return instrumentSaver.save(instrument);
    }

    public void deleteInstrument(Instrument instrument) {
        instrumentDeleter.delete(instrument);
    }

    public ControlCode saveControlCode(ControlCode controlCode) {
        return controlCodeSaver.save(controlCode);
    }

    public void deleteControlCode(ControlCode controlCode) {
        controlCodeDeleter.delete(controlCode);
    }

    public Pad findPadById(Long id) {
        return padRepository.findById(id).orElse(null);
    }

    public Instrument findInstrumentById(Long id) {
        return InstrumentRepo.findById(id).orElse(null);
    }

    public ControlCode findControlCodeById(Long id) {
        return controlCodeRepository.findById(id).orElse(null);
    }

    public Song findSongById(Long id) {
        return songRepository.findById(id).orElse(null);
    }

    public Step findStepById(Long id) {
        return stepDataRepository.findById(id).orElse(null);
    }

    public void clearDatabase() {
        strikeRepository.deleteAll();
        ruleRepository.deleteAll();
        tickerRepo.deleteAll();
        stepDataRepository.deleteAll();
        songRepository.deleteAll();
    }

    public long getMinimumSongId() {
        return songRepository.getMinimumSongId();
    }

    public Song getPreviousSong(long currentSongId) {
        return songRepository.getPreviousSong(currentSongId);
    }

    public Long getMaximumSongId() {
        return songRepository.getMaximumSongId();
    }

    public Song getNextSong(long currentSongId) {
        return songRepository.getNextSong(currentSongId);
    }

    public Set<Pattern> findPatternBySongId(Long id) {
        return patternRepository.findBySongId(id);
    }

    public Set<Step> findStepsByPatternId(Long id) {
        return stepDataRepository.findByPatternId(id);
    }

    public List<Ticker> findAllTickers() {
        return tickerRepo.findAll();
    }

    public long getMinimumTickerId() {
        return getTickerRepo().getMinimumTickerId();
    }

    public long getMaximumTickerId() {
        return getTickerRepo().getMaximumTickerId();
    }
}

package com.angrysurfer.spring.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.angrysurfer.core.api.Database;
import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.FindAll;
import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Max;
import com.angrysurfer.core.api.db.Min;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.model.Caption;
import com.angrysurfer.core.model.ControlCode;
import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Pad;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.spring.repo.Captions;
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
public class HibernateService implements Database {

    private Instruments instrumentRepository;
    private Pads padRepository;
    private Patterns patternRepository;
    private Strikes strikeRepository;
    private Rules ruleRepository;
    private Tickers tickerRepo;
    private Steps stepDataRepository;
    private Songs songRepository;
    private ControlCodes controlCodeRepository;
    private Captions captionRepository;

    public HibernateService(Strikes strikeRepository,
            Rules ruleRepository, Tickers tickerRepo,
            Instruments InstrumentRepository,
            Captions captionRepository,
            ControlCodes controlCodeRepository,
            Pads padRepository,
            Patterns patternRepository,
            Steps stepRepository,
            Songs songRepository) {

        this.tickerRepo = tickerRepo;
        this.ruleRepository = ruleRepository;
        this.strikeRepository = strikeRepository;
        this.padRepository = padRepository;
        this.instrumentRepository = InstrumentRepository;
        this.controlCodeRepository = controlCodeRepository;
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        this.patternRepository = patternRepository;
        this.captionRepository = captionRepository;
    };

    // Caption lambdas
    private FindAll<Caption> captionFindAll = () -> getCaptionRepository().findAll();
    private FindOne<Caption> captionFindOne = (Long id) -> getCaptionRepository().findById(id);
    private Save<Caption> captionSaver = (Caption caption) -> getCaptionRepository().save(caption);
    private Delete<Caption> captionDeleter = (Caption caption) -> getCaptionRepository().deleteById(caption.getId());

    // ControlCode lambdas
    private FindAll<ControlCode> controlCodeFindAll = () -> getControlCodeRepository().findAll();
    private FindOne<ControlCode> controlCodeFindOne = (Long id) -> getControlCodeRepository().findById(id);
    private Save<ControlCode> controlCodeSaver = (ControlCode c) -> getControlCodeRepository().save(c);
    private Delete<ControlCode> controlCodeDeleter = (ControlCode c) -> getControlCodeRepository()
            .deleteById(c.getId());

    // Instrument lambdas
    private FindAll<Instrument> instrumentFindAll = () -> getInstrumentRepository().findAll();
    private FindOne<Instrument> instrumentFindOne = (Long id) -> getInstrumentRepository().findById(id);
    private Save<Instrument> instrumentSaver = (Instrument i) -> getInstrumentRepository().save(i);
    private Delete<Instrument> instrumentDeleter = (Instrument i) -> getInstrumentRepository().deleteById(i.getId());

    // Pad lambdas
    private FindAll<Pad> padFindAll = () -> getPadRepository().findAll();
    private FindOne<Pad> padFindOne = (Long id) -> getPadRepository().findById(id);
    private Save<Pad> padSaver = (Pad p) -> getPadRepository().save(p);
    private Delete<Pad> padDeleter = (Pad p) -> getPadRepository().deleteById(p.getId());

    // Pattern lambdas
    private FindAll<Pattern> patternFindAll = () -> getPatternRepository().findAll();
    private FindOne<Pattern> patternFindOne = (Long id) -> getPatternRepository().findById(id);
    private FindSet<Pattern> songPatternFinder = (Long id) -> getPatternRepository().findBySongId(id);
    private Save<Pattern> patternSaver = (Pattern p) -> getPatternRepository().save(p);
    private Delete<Pattern> patternDeleter = (Pattern p) -> getPatternRepository().deleteById(p.getId());

    // Rule lambdas
    private FindAll<Rule> ruleFindAll = () -> getRuleRepository().findAll();
    private FindOne<Rule> ruleFindOne = (Long id) -> getRuleRepository().findById(id);
    private FindSet<Rule> playerRuleFindSet = (Long playerId) -> getRuleRepository().findByPlayerId(playerId);
    private Save<Rule> ruleSaver = (Rule r) -> getRuleRepository().save(r);
    private Delete<Rule> ruleDeleter = (Rule r) -> getRuleRepository().deleteById(r.getId());

    // Song lambdas
    private FindAll<Song> songFindAll = () -> getSongRepository().findAll();
    private FindOne<Song> songFindOne = (Long id) -> getSongRepository().findById(id);
    private Save<Song> songSaver = (Song s) -> getSongRepository().save(s);
    private Delete<Song> songDeleter = (Song s) -> getSongRepository().deleteById(s.getId());
    private Next<Song> songForward = (Long id) -> getSongRepository().count() > 0 ? getSongRepository().getNextSong(id)
            : null;
    private Prior<Song> songBack = (
            Long id) -> getSongRepository().count() > 0 ? getSongRepository().getPreviousSong(id) : null;

    private Max<Song> songMax = () -> getSongRepository().count() > 0 ? getSongRepository().getMaximumSongId() : -1;
    private Min<Song> songMin = () -> getSongRepository().count() > 0 ? getSongRepository().getMinimumSongId() : -1;

    // Step lambdas
    private FindAll<Step> stepFindAll = () -> getStepDataRepository().findAll();
    private FindOne<Step> stepFindOne = (Long id) -> getStepDataRepository().findById(id);
    private FindSet<Step> patternStepFinder = (Long id) -> getStepDataRepository().findByPatternId(id);
    private Save<Step> stepSaver = (Step s) -> getStepDataRepository().save(s);
    private Delete<Step> stepDeleter = (Step s) -> getStepDataRepository().deleteById(s.getId());

    // Strike lambdas
    private FindAll<Strike> strikeFindAll = () -> getStrikeRepository().findAll();
    private FindOne<Strike> strikeFindOne = (Long id) -> getStrikeRepository().findById(id);
    private FindSet<Strike> tickerStrikeFinder = (Long id) -> getStrikeRepository().findByTickerId(id);
    private Save<IPlayer> strikeSaver = (IPlayer s) -> getStrikeRepository().save((Strike) s);
    private Delete<IPlayer> strikeDeleter = (IPlayer s) -> getStrikeRepository().deleteById(s.getId());

    // Ticker lambdas
    private FindAll<Ticker> tickerFindAll = () -> getTickerRepo().findAll();
    private FindOne<Ticker> tickerFindOne = (Long id) -> getTickerRepo().findById(id);
    private Save<Ticker> tickerSaver = (Ticker t) -> getTickerRepo().save(t);
    private Delete<Ticker> tickerDeleter = (Ticker t) -> getTickerRepo().deleteById(t.getId());
    private Next<Ticker> tickerForward = (Long id) -> getTickerRepo().getNextTicker(id);
    private Prior<Ticker> tickerBack = (Long id) -> getTickerRepo().getPreviousTicker(id);
    private Max<Ticker> tickerMax = () -> getTickerRepo().count() > 0 ? getTickerRepo().getMaximumTickerId() : -1;
    private Min<Ticker> tickerMin = () -> getTickerRepo().count() > 0 ? getTickerRepo().getMinimumTickerId() : -1;

    public void clearDatabase() {
        strikeRepository.deleteAll();
        ruleRepository.deleteAll();
        tickerRepo.deleteAll();
        stepDataRepository.deleteAll();
        songRepository.deleteAll();
    }

    // Caption related public methods

    public Caption findCaptionById(Long id) {
        return captionFindOne.find(id).orElse(null);
    }

    public List<Caption> findAllCaptions() {
        return captionFindAll.findAll();
    }

    public Caption saveCaption(Caption caption) {
        return captionSaver.save(caption);
    }

    public void deleteCaption(Caption caption) {
        captionDeleter.delete(caption);
    }

    // ControlCode related public methods

    public ControlCode findControlCodeById(Long id) {
        return controlCodeFindOne.find(id).orElse(null);
    }

    public List<ControlCode> findAllControlCodes() {
        return controlCodeFindAll.findAll();
    }

    public ControlCode saveControlCode(ControlCode controlCode) {
        return controlCodeSaver.save(controlCode);
    }

    public void deleteControlCode(ControlCode controlCode) {
        controlCodeDeleter.delete(controlCode);
    }

    // Instrument related public methods

    public Instrument findInstrumentById(Long id) {
        return instrumentFindOne.find(id).orElse(null);
    }

    public List<Instrument> findAllInstruments() {
        return instrumentFindAll.findAll();
    }

    public Instrument saveInstrument(Instrument instrument) {
        return instrumentSaver.save(instrument);
    }

    public void deleteInstrument(Instrument instrument) {
        instrumentDeleter.delete(instrument);
    }

    // Pad related public methods

    public Pad findPadById(Long id) {
        return padFindOne.find(id).orElse(null);
    }

    public List<Pad> findAllPads() {
        return padFindAll.findAll();
    }

    public Pad savePad(Pad pad) {
        return padSaver.save(pad);
    }

    public void deletePad(Pad pad) {
        padDeleter.delete(pad);
    }

    // Pattern related public methods

    public Pattern findPatternById(Long id) {
        return patternFindOne.find(id).orElse(null);
    }

    public Set<Pattern> findPatternBySongId(Long id) {
        return songPatternFinder.find(id);
    }

    public List<Pattern> findAllPatterns() {
        return patternFindAll.findAll();
    }

    public Pattern savePattern(Pattern pattern) {
        return patternSaver.save(pattern);
    }

    public void deletePattern(Pattern pattern) {
        patternDeleter.delete(pattern);
    }

    // Rule related public methods

    public Rule findRuleById(Long id) {
        return ruleFindOne.find(id).orElse(null);
    }

    public Set<Rule> findRulesByPlayerId(Long playerId) {
        return playerRuleFindSet.find(playerId);
    }

    public Rule saveRule(Rule rule) {
        return ruleSaver.save(rule);
    }

    public void deleteRule(Rule rule) {
        ruleDeleter.delete(rule);
    }

    // Song related public methods

    public Song findSongById(Long id) {
        return songFindOne.find(id).orElse(null);
    }

    public List<Song> findAllSongs() {
        return songFindAll.findAll();
    }

    public Song saveSong(Song song) {
        return songSaver.save(song);
    }

    public void deleteSong(Song song) {
        songDeleter.delete(song);
    }

    public Song getNextSong(long currentSongId) {
        return songForward.next(currentSongId);
    }

    public Song getPreviousSong(long currentSongId) {
        return songBack.prior(currentSongId);
    }

    public long getMinimumSongId() {
        return songMin.getMinimumId();
    }

    public Long getMaximumSongId() {
        return songMax.getMaxId();
    }

    // Step related public methods

    public Step findStepById(Long id) {
        return stepFindOne.find(id).orElse(null);
    }

    public Set<Step> findStepsByPatternId(Long id) {
        return patternStepFinder.find(id);
    }

    public Step saveStep(Step step) {
        return stepSaver.save(step);
    }

    public void deleteStep(Step step) {
        stepDeleter.delete(step);
    }

    // Strike related public methods

    public Strike findStrikeById(Long id) {
        return strikeFindOne.find(id).orElse(null);
    }

    public Set<Strike> strikesForTicker(Long tickerId) {
        return tickerStrikeFinder.find(tickerId);
    }

    public IPlayer savePlayer(IPlayer player) {
        if (player instanceof Strike) {
            return strikeSaver.save((Strike) player);
        }

        return player;
    }

    public void deletePlayer(IPlayer player) {
        if (player instanceof Strike) {
            strikeDeleter.delete((Strike) player);
        }
    }

    // Ticker related public methods

    public Ticker findTickerById(Long id) {
        return tickerFindOne.find(id).orElse(null);
    }

    public List<Ticker> findAllTickers() {
        return tickerFindAll.findAll();
    }

    public Ticker saveTicker(Ticker ticker) {
        return tickerSaver.save(ticker);
    }

    public void deleteTicker(Long id) {
        tickerDeleter.delete(tickerRepo.findById(id).orElse(null));
    }

    public long getMinimumTickerId() {
        return tickerMin.getMinimumId();
    }

    public long getMaximumTickerId() {
        return tickerMax.getMaxId();
    }

    public void deleteAllTickers() {
        tickerRepo.deleteAll();
    }

}

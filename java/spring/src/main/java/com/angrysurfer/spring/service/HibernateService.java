package com.angrysurfer.spring.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.angrysurfer.core.api.Database;
import com.angrysurfer.core.api.ICaption;
import com.angrysurfer.core.api.IControlCode;
import com.angrysurfer.core.api.IInstrument;
import com.angrysurfer.core.api.IPad;
import com.angrysurfer.core.api.IPattern;
import com.angrysurfer.core.api.IPlayer;
import com.angrysurfer.core.api.IRule;
import com.angrysurfer.core.api.ISong;
import com.angrysurfer.core.api.IStep;
import com.angrysurfer.core.api.ITicker;
import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.FindAll;
import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Max;
import com.angrysurfer.core.api.db.Min;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
// import com.angrysurfer.core.IDBService;
import com.angrysurfer.core.model.Pad;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.model.ui.Caption;
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
    private FindAll<ICaption> captionFindAll = () -> getCaptionRepository().findAll();
    private FindOne<ICaption> captionFindOne = (Long id) -> getCaptionRepository().findById(id);
    private Save<ICaption> captionSaver = (ICaption caption) -> getCaptionRepository().save(caption);
    private Delete<ICaption> captionDeleter = (ICaption caption) -> getCaptionRepository().deleteById(caption.getId());

    // ControlCode lambdas
    private FindAll<IControlCode> controlCodeFindAll = () -> getControlCodeRepository().findAll();
    private FindOne<IControlCode> controlCodeFindOne = (Long id) -> getControlCodeRepository().findById(id);
    private Save<IControlCode> controlCodeSaver = (IControlCode c) -> getControlCodeRepository().save(c);
    private Delete<IControlCode> controlCodeDeleter = (IControlCode c) -> getControlCodeRepository()
            .deleteById(c.getId());

    // Instrument lambdas
    private FindAll<IInstrument> instrumentFindAll = () -> getInstrumentRepository().findAll();
    private FindOne<IInstrument> instrumentFindOne = (Long id) -> getInstrumentRepository().findById(id);
    private Save<IInstrument> instrumentSaver = (IInstrument i) -> getInstrumentRepository().save(i);
    private Delete<IInstrument> instrumentDeleter = (IInstrument i) -> getInstrumentRepository().deleteById(i.getId());

    // Pad lambdas
    private FindAll<IPad> padFindAll = () -> getPadRepository().findAll();
    private FindOne<IPad> padFindOne = (Long id) -> getPadRepository().findById(id);
    private Save<IPad> padSaver = (IPad p) -> getPadRepository().save(p);
    private Delete<IPad> padDeleter = (IPad p) -> getPadRepository().deleteById(p.getId());

    // Pattern lambdas
    private FindAll<IPattern> patternFindAll = () -> getPatternRepository().findAll();
    private FindOne<IPattern> patternFindOne = (Long id) -> getPatternRepository().findById(id);
    private FindSet<IPattern> songPatternFinder = (Long id) -> getPatternRepository().findBySongId(id);
    private Save<IPattern> patternSaver = (IPattern p) -> getPatternRepository().save(p);
    private Delete<IPattern> patternDeleter = (IPattern p) -> getPatternRepository().deleteById(p.getId());

    // Rule lambdas
    private FindAll<IRule> ruleFindAll = () -> getRuleRepository().findAll();
    private FindOne<IRule> ruleFindOne = (Long id) -> getRuleRepository().findById(id);
    private FindSet<IRule> playerRuleFindSet = (Long playerId) -> getRuleRepository().findByPlayerId(playerId);
    private Save<IRule> ruleSaver = (IRule r) -> getRuleRepository().save(r);
    private Delete<IRule> ruleDeleter = (IRule r) -> getRuleRepository().deleteById(r.getId());

    // Song lambdas
    private FindAll<ISong> songFindAll = () -> getSongRepository().findAll();
    private FindOne<ISong> songFindOne = (Long id) -> getSongRepository().findById(id);
    private Save<ISong> songSaver = (ISong s) -> getSongRepository().save(s);
    private Delete<ISong> songDeleter = (ISong s) -> getSongRepository().deleteById(s.getId());
    private Next<ISong> songForward = (Long id) -> getSongRepository().count() > 0 ? getSongRepository().getNextSong(id)
            : null;
    private Prior<ISong> songBack = (
            Long id) -> getSongRepository().count() > 0 ? getSongRepository().getPreviousSong(id) : null;

    private Max<ISong> songMax = () -> getSongRepository().count() > 0 ? getSongRepository().getMaximumSongId() : -1;
    private Min<ISong> songMin = () -> getSongRepository().count() > 0 ? getSongRepository().getMinimumSongId() : -1;

    // Step lambdas
    private FindAll<IStep> stepFindAll = () -> getStepDataRepository().findAll();
    private FindOne<IStep> stepFindOne = (Long id) -> getStepDataRepository().findById(id);
    private FindSet<IStep> patternStepFinder = (Long id) -> getStepDataRepository().findByPatternId(id);
    private Save<IStep> stepSaver = (IStep s) -> getStepDataRepository().save(s);
    private Delete<IStep> stepDeleter = (IStep s) -> getStepDataRepository().deleteById(s.getId());

    // Strike lambdas
    private FindAll<IPlayer> strikeFindAll = () -> getStrikeRepository().findAll();
    private FindOne<IPlayer> strikeFindOne = (Long id) -> getStrikeRepository().findById(id);
    private FindSet<IPlayer> tickerStrikeFinder = (Long id) -> getStrikeRepository().findByTickerId(id);
    private Save<IPlayer> strikeSaver = (IPlayer s) -> getStrikeRepository().save((Strike) s);
    private Delete<IPlayer> strikeDeleter = (IPlayer s) -> getStrikeRepository().deleteById(s.getId());

    // Ticker lambdas
    private FindAll<ITicker> tickerFindAll = () -> getTickerRepo().findAll();
    private FindOne<ITicker> tickerFindOne = (Long id) -> getTickerRepo().findById(id);
    private Save<ITicker> tickerSaver = (ITicker t) -> getTickerRepo().save(t);
    private Delete<ITicker> tickerDeleter = (ITicker t) -> getTickerRepo().deleteById(t.getId());
    private Next<ITicker> tickerForward = (Long id) -> getTickerRepo().getNextTicker(id);
    private Prior<ITicker> tickerBack = (Long id) -> getTickerRepo().getPreviousTicker(id);
    private Max<ITicker> tickerMax = () -> getTickerRepo().count() > 0 ? getTickerRepo().getMaximumTickerId() : -1;
    private Min<ITicker> tickerMin = () -> getTickerRepo().count() > 0 ? getTickerRepo().getMinimumTickerId() : -1;

    public void clearDatabase() {
        strikeRepository.deleteAll();
        ruleRepository.deleteAll();
        tickerRepo.deleteAll();
        stepDataRepository.deleteAll();
        songRepository.deleteAll();
    }

    // Caption related public methods

    public ICaption findCaptionById(Long id) {
        return captionFindOne.find(id).orElse(null);
    }

    public List<ICaption> findAllCaptions() {
        return captionFindAll.findAll();
    }

    public ICaption saveCaption(Caption caption) {
        return captionSaver.save(caption);
    }

    public void deleteCaption(Caption caption) {
        captionDeleter.delete(caption);
    }

    // ControlCode related public methods

    public IControlCode findControlCodeById(Long id) {
        return controlCodeFindOne.find(id).orElse(null);
    }

    public List<IControlCode> findAllControlCodes() {
        return controlCodeFindAll.findAll();
    }

    public IControlCode saveControlCode(IControlCode controlCode) {
        return controlCodeSaver.save(controlCode);
    }

    public void deleteControlCode(IControlCode controlCode) {
        controlCodeDeleter.delete(controlCode);
    }

    // Instrument related public methods

    public IInstrument findInstrumentById(Long id) {
        return instrumentFindOne.find(id).orElse(null);
    }

    public List<IInstrument> findAllInstruments() {
        return instrumentFindAll.findAll();
    }

    public IInstrument saveInstrument(Instrument instrument) {
        return instrumentSaver.save(instrument);
    }

    public void deleteInstrument(Instrument instrument) {
        instrumentDeleter.delete(instrument);
    }

    // Pad related public methods

    public IPad findPadById(Long id) {
        return padFindOne.find(id).orElse(null);
    }

    public List<IPad> findAllPads() {
        return padFindAll.findAll();
    }

    public IPad savePad(Pad pad) {
        return padSaver.save(pad);
    }

    public void deletePad(Pad pad) {
        padDeleter.delete(pad);
    }

    // Pattern related public methods

    public IPattern findPatternById(Long id) {
        return patternFindOne.find(id).orElse(null);
    }

    public Set<IPattern> findPatternBySongId(Long id) {
        return songPatternFinder.find(id);
    }

    public List<IPattern> findAllPatterns() {
        return patternFindAll.findAll();
    }

    public IPattern savePattern(IPattern pattern) {
        return patternSaver.save(pattern);
    }

    public void deletePattern(IPattern pattern) {
        patternDeleter.delete(pattern);
    }

    // Rule related public methods

    public IRule findRuleById(Long id) {
        return ruleFindOne.find(id).orElse(null);
    }

    public Set<IRule> findRulesByPlayerId(Long playerId) {
        return playerRuleFindSet.find(playerId);
    }

    public IRule saveRule(IRule rule) {
        return ruleSaver.save(rule);
    }

    public void deleteRule(IRule rule) {
        ruleDeleter.delete(rule);
    }

    // Song related public methods

    public ISong findSongById(Long id) {
        return songFindOne.find(id).orElse(null);
    }

    public List<ISong> findAllSongs() {
        return songFindAll.findAll();
    }

    public ISong saveSong(ISong song) {
        return songSaver.save(song);
    }

    public void deleteSong(ISong song) {
        songDeleter.delete(song);
    }

    public ISong getNextSong(long currentSongId) {
        return songForward.next(currentSongId);
    }

    public ISong getPreviousSong(long currentSongId) {
        return songBack.prior(currentSongId);
    }

    public long getMinimumSongId() {
        return songMin.getMinimumId();
    }

    public Long getMaximumSongId() {
        return songMax.getMaxId();
    }

    // Step related public methods

    public IStep findStepById(Long id) {
        return stepFindOne.find(id).orElse(null);
    }

    public Set<IStep> findStepsByPatternId(Long id) {
        return patternStepFinder.find(id);
    }

    public IStep saveStep(IStep step) {
        return stepSaver.save(step);
    }

    public void deleteStep(IStep step) {
        stepDeleter.delete(step);
    }

    // Strike related public methods

    public IPlayer findStrikeById(Long id) {
        return strikeFindOne.find(id).orElse(null);
    }

    public Set<IPlayer> strikesForTicker(Long tickerId) {
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

    public ITicker findTickerById(Long id) {
        return tickerFindOne.find(id).orElse(null);
    }

    public List<ITicker> findAllTickers() {
        return tickerFindAll.findAll();
    }

    public ITicker saveTicker(ITicker ticker) {
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

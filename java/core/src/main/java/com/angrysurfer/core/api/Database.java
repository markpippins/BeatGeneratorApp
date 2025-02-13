package com.angrysurfer.core.api;

import java.util.List;
import java.util.Set;

import com.angrysurfer.core.api.db.Delete;
import com.angrysurfer.core.api.db.FindAll;
import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Max;
import com.angrysurfer.core.api.db.Min;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.model.Pad;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.ui.Caption;

public interface Database {

    FindAll<ICaption> getCaptionFindAll();

    FindOne<ICaption> getCaptionFindOne();

    Save<ICaption> getCaptionSaver();

    Delete<ICaption> getCaptionDeleter();

    FindAll<IControlCode> getControlCodeFindAll();

    FindOne<IControlCode> getControlCodeFindOne();

    Save<IControlCode> getControlCodeSaver();

    Delete<IControlCode> getControlCodeDeleter();

    FindAll<IInstrument> getInstrumentFindAll();

    FindOne<IInstrument> getInstrumentFindOne();

    Save<IInstrument> getInstrumentSaver();

    Delete<IInstrument> getInstrumentDeleter();

    FindAll<IPad> getPadFindAll();

    FindOne<IPad> getPadFindOne();

    Save<IPad> getPadSaver();

    Delete<IPad> getPadDeleter();

    FindAll<IPattern> getPatternFindAll();

    FindOne<IPattern> getPatternFindOne();

    FindSet<IPattern> getSongPatternFinder();

    Save<IPattern> getPatternSaver();

    Delete<IPattern> getPatternDeleter();

    FindAll<IRule> getRuleFindAll();

    FindOne<IRule> getRuleFindOne();

    FindSet<IRule> getPlayerRuleFindSet();

    Save<IRule> getRuleSaver();

    Delete<IRule> getRuleDeleter();

    FindAll<ISong> getSongFindAll();

    FindOne<ISong> getSongFindOne();

    Save<ISong> getSongSaver();

    Delete<ISong> getSongDeleter();

    Next<ISong> getSongForward();

    Prior<ISong> getSongBack();

    Max<ISong> getSongMax();

    Min<ISong> getSongMin();

    FindAll<IStep> getStepFindAll();

    FindOne<IStep> getStepFindOne();

    FindSet<IStep> getPatternStepFinder();

    Save<IStep> getStepSaver();

    Delete<IStep> getStepDeleter();

    FindAll<IPlayer> getStrikeFindAll();

    FindOne<IPlayer> getStrikeFindOne();

    FindSet<IPlayer> getTickerStrikeFinder();

    Save<IPlayer> getStrikeSaver();

    Delete<IPlayer> getStrikeDeleter();

    FindAll<ITicker> getTickerFindAll();

    FindOne<ITicker> getTickerFindOne();

    Save<ITicker> getTickerSaver();

    Delete<ITicker> getTickerDeleter();

    Next<ITicker> getTickerForward();

    Prior<ITicker> getTickerBack();

    Max<ITicker> getTickerMax();

    Min<ITicker> getTickerMin();

    void setCaptionFindAll(FindAll<ICaption> captionFindAll);

    void setCaptionFindOne(FindOne<ICaption> captionFindOne);

    void setCaptionSaver(Save<ICaption> captionSaver);

    void setCaptionDeleter(Delete<ICaption> captionDeleter);

    void setControlCodeFindAll(FindAll<IControlCode> controlCodeFindAll);

    void setControlCodeFindOne(FindOne<IControlCode> controlCodeFindOne);

    void setControlCodeSaver(Save<IControlCode> controlCodeSaver);

    void setControlCodeDeleter(Delete<IControlCode> controlCodeDeleter);

    void setInstrumentFindAll(FindAll<IInstrument> instrumentFindAll);

    void setInstrumentFindOne(FindOne<IInstrument> instrumentFindOne);

    void setInstrumentSaver(Save<IInstrument> instrumentSaver);

    void setInstrumentDeleter(Delete<IInstrument> instrumentDeleter);

    void setPadFindAll(FindAll<IPad> padFindAll);

    void setPadFindOne(FindOne<IPad> padFindOne);

    void setPadSaver(Save<IPad> padSaver);

    void setPadDeleter(Delete<IPad> padDeleter);

    void setPatternFindAll(FindAll<IPattern> patternFindAll);

    void setPatternFindOne(FindOne<IPattern> patternFindOne);

    void setSongPatternFinder(FindSet<IPattern> songPatternFinder);

    void setPatternSaver(Save<IPattern> patternSaver);

    void setPatternDeleter(Delete<IPattern> patternDeleter);

    void setRuleFindAll(FindAll<IRule> ruleFindAll);

    void setRuleFindOne(FindOne<IRule> ruleFindOne);

    void setPlayerRuleFindSet(FindSet<IRule> playerRuleFindSet);

    void setRuleSaver(Save<IRule> ruleSaver);

    void setRuleDeleter(Delete<IRule> ruleDeleter);

    void setSongFindAll(FindAll<ISong> songFindAll);

    void setSongFindOne(FindOne<ISong> songFindOne);

    void setSongSaver(Save<ISong> songSaver);

    void setSongDeleter(Delete<ISong> songDeleter);

    void setSongForward(Next<ISong> songForward);

    void setSongBack(Prior<ISong> songBack);

    void setSongMax(Max<ISong> songMax);

    void setSongMin(Min<ISong> songMin);

    void setStepFindAll(FindAll<IStep> stepFindAll);

    void setStepFindOne(FindOne<IStep> stepFindOne);

    void setPatternStepFinder(FindSet<IStep> patternStepFinder);

    void setStepSaver(Save<IStep> stepSaver);

    void setStepDeleter(Delete<IStep> stepDeleter);

    void setStrikeFindAll(FindAll<IPlayer> strikeFindAll);

    void setStrikeFindOne(FindOne<IPlayer> strikeFindOne);

    void setTickerStrikeFinder(FindSet<IPlayer> tickerStrikeFinder);

    void setStrikeSaver(Save<IPlayer> strikeSaver);

    void setStrikeDeleter(Delete<IPlayer> strikeDeleter);

    void setTickerFindAll(FindAll<ITicker> tickerFindAll);

    void setTickerFindOne(FindOne<ITicker> tickerFindOne);

    void setTickerSaver(Save<ITicker> tickerSaver);

    void setTickerDeleter(Delete<ITicker> tickerDeleter);

    void setTickerForward(Next<ITicker> tickerForward);

    void setTickerBack(Prior<ITicker> tickerBack);

    void setTickerMax(Max<ITicker> tickerMax);

    void setTickerMin(Min<ITicker> tickerMin);

    void clearDatabase();

    // Caption related public methods
    ICaption findCaptionById(Long id);

    List<ICaption> findAllCaptions();

    ICaption saveCaption(Caption caption);

    void deleteCaption(Caption caption);

    // ControlCode related public methods
    IControlCode findControlCodeById(Long id);

    List<IControlCode> findAllControlCodes();

    IControlCode saveControlCode(IControlCode controlCode);

    void deleteControlCode(IControlCode controlCode);

    // Instrument related public methods
    IInstrument findInstrumentById(Long id);

    List<IInstrument> findAllInstruments();

    IInstrument saveInstrument(Instrument instrument);

    void deleteInstrument(Instrument instrument);

    // Pad related public methods
    IPad findPadById(Long id);

    List<IPad> findAllPads();

    IPad savePad(Pad pad);

    void deletePad(Pad pad);

    // Pattern related public methods
    IPattern findPatternById(Long id);

    Set<IPattern> findPatternBySongId(Long id);

    List<IPattern> findAllPatterns();

    IPattern savePattern(IPattern pattern);

    void deletePattern(IPattern pattern);

    // Rule related public methods
    IRule findRuleById(Long id);

    Set<IRule> findRulesByPlayerId(Long playerId);

    IRule saveRule(IRule rule);

    void deleteRule(IRule rule);

    // Song related public methods
    ISong findSongById(Long id);

    List<ISong> findAllSongs();

    ISong saveSong(ISong song);

    void deleteSong(ISong song);

    ISong getNextSong(long currentSongId);

    ISong getPreviousSong(long currentSongId);

    long getMinimumSongId();

    Long getMaximumSongId();

    // Step related public methods
    IStep findStepById(Long id);

    Set<IStep> findStepsByPatternId(Long id);

    IStep saveStep(IStep step);

    void deleteStep(IStep step);

    // Strike related public methods
    IPlayer findStrikeById(Long id);

    Set<IPlayer> strikesForTicker(Long tickerId);

    IPlayer savePlayer(IPlayer player);

    void deletePlayer(IPlayer player);

    // Ticker related public methods
    ITicker findTickerById(Long id);

    List<ITicker> findAllTickers();

    ITicker saveTicker(ITicker ticker);

    void deleteTicker(Long id);

    long getMinimumTickerId();

    long getMaximumTickerId();

    void deleteAllTickers();

}
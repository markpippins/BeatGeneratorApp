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
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.ControlCode;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.model.player.IPlayer;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.model.ui.Caption;

public interface Database {

    FindAll<Caption> getCaptionFindAll();

    FindOne<Caption> getCaptionFindOne();

    Save<Caption> getCaptionSaver();

    Delete<Caption> getCaptionDeleter();

    FindAll<ControlCode> getControlCodeFindAll();

    FindOne<ControlCode> getControlCodeFindOne();

    Save<ControlCode> getControlCodeSaver();

    Delete<ControlCode> getControlCodeDeleter();

    FindAll<Instrument> getInstrumentFindAll();

    FindOne<Instrument> getInstrumentFindOne();

    Save<Instrument> getInstrumentSaver();

    Delete<Instrument> getInstrumentDeleter();

    FindAll<Pad> getPadFindAll();

    FindOne<Pad> getPadFindOne();

    Save<Pad> getPadSaver();

    Delete<Pad> getPadDeleter();

    FindAll<Pattern> getPatternFindAll();

    FindOne<Pattern> getPatternFindOne();

    FindSet<Pattern> getSongPatternFinder();

    Save<Pattern> getPatternSaver();

    Delete<Pattern> getPatternDeleter();

    FindAll<Rule> getRuleFindAll();

    FindOne<Rule> getRuleFindOne();

    FindSet<Rule> getPlayerRuleFindSet();

    Save<Rule> getRuleSaver();

    Delete<Rule> getRuleDeleter();

    FindAll<Song> getSongFindAll();

    FindOne<Song> getSongFindOne();

    Save<Song> getSongSaver();

    Delete<Song> getSongDeleter();

    Next<Song> getSongForward();

    Prior<Song> getSongBack();

    Max<Song> getSongMax();

    Min<Song> getSongMin();

    FindAll<Step> getStepFindAll();

    FindOne<Step> getStepFindOne();

    FindSet<Step> getPatternStepFinder();

    Save<Step> getStepSaver();

    Delete<Step> getStepDeleter();

    FindAll<Strike> getStrikeFindAll();

    FindOne<Strike> getStrikeFindOne();

    FindSet<Strike> getTickerStrikeFinder();

    Save<IPlayer> getStrikeSaver();

    Delete<IPlayer> getStrikeDeleter();

    FindAll<Ticker> getTickerFindAll();

    FindOne<Ticker> getTickerFindOne();

    Save<Ticker> getTickerSaver();

    Delete<Ticker> getTickerDeleter();

    Next<Ticker> getTickerForward();

    Prior<Ticker> getTickerBack();

    Max<Ticker> getTickerMax();

    Min<Ticker> getTickerMin();

    void setCaptionFindAll(FindAll<Caption> captionFindAll);

    void setCaptionFindOne(FindOne<Caption> captionFindOne);

    void setCaptionSaver(Save<Caption> captionSaver);

    void setCaptionDeleter(Delete<Caption> captionDeleter);

    void setControlCodeFindAll(FindAll<ControlCode> controlCodeFindAll);

    void setControlCodeFindOne(FindOne<ControlCode> controlCodeFindOne);

    void setControlCodeSaver(Save<ControlCode> controlCodeSaver);

    void setControlCodeDeleter(Delete<ControlCode> controlCodeDeleter);

    void setInstrumentFindAll(FindAll<Instrument> instrumentFindAll);

    void setInstrumentFindOne(FindOne<Instrument> instrumentFindOne);

    void setInstrumentSaver(Save<Instrument> instrumentSaver);

    void setInstrumentDeleter(Delete<Instrument> instrumentDeleter);

    void setPadFindAll(FindAll<Pad> padFindAll);

    void setPadFindOne(FindOne<Pad> padFindOne);

    void setPadSaver(Save<Pad> padSaver);

    void setPadDeleter(Delete<Pad> padDeleter);

    void setPatternFindAll(FindAll<Pattern> patternFindAll);

    void setPatternFindOne(FindOne<Pattern> patternFindOne);

    void setSongPatternFinder(FindSet<Pattern> songPatternFinder);

    void setPatternSaver(Save<Pattern> patternSaver);

    void setPatternDeleter(Delete<Pattern> patternDeleter);

    void setRuleFindAll(FindAll<Rule> ruleFindAll);

    void setRuleFindOne(FindOne<Rule> ruleFindOne);

    void setPlayerRuleFindSet(FindSet<Rule> playerRuleFindSet);

    void setRuleSaver(Save<Rule> ruleSaver);

    void setRuleDeleter(Delete<Rule> ruleDeleter);

    void setSongFindAll(FindAll<Song> songFindAll);

    void setSongFindOne(FindOne<Song> songFindOne);

    void setSongSaver(Save<Song> songSaver);

    void setSongDeleter(Delete<Song> songDeleter);

    void setSongForward(Next<Song> songForward);

    void setSongBack(Prior<Song> songBack);

    void setSongMax(Max<Song> songMax);

    void setSongMin(Min<Song> songMin);

    void setStepFindAll(FindAll<Step> stepFindAll);

    void setStepFindOne(FindOne<Step> stepFindOne);

    void setPatternStepFinder(FindSet<Step> patternStepFinder);

    void setStepSaver(Save<Step> stepSaver);

    void setStepDeleter(Delete<Step> stepDeleter);

    void setStrikeFindAll(FindAll<Strike> strikeFindAll);

    void setStrikeFindOne(FindOne<Strike> strikeFindOne);

    void setTickerStrikeFinder(FindSet<Strike> tickerStrikeFinder);

    void setStrikeSaver(Save<IPlayer> strikeSaver);

    void setStrikeDeleter(Delete<IPlayer> strikeDeleter);

    void setTickerFindAll(FindAll<Ticker> tickerFindAll);

    void setTickerFindOne(FindOne<Ticker> tickerFindOne);

    void setTickerSaver(Save<Ticker> tickerSaver);

    void setTickerDeleter(Delete<Ticker> tickerDeleter);

    void setTickerForward(Next<Ticker> tickerForward);

    void setTickerBack(Prior<Ticker> tickerBack);

    void setTickerMax(Max<Ticker> tickerMax);

    void setTickerMin(Min<Ticker> tickerMin);

    void clearDatabase();

    // Caption related public methods
    Caption findCaptionById(Long id);

    List<Caption> findAllCaptions();

    Caption saveCaption(Caption caption);

    void deleteCaption(Caption caption);

    // ControlCode related public methods
    ControlCode findControlCodeById(Long id);

    List<ControlCode> findAllControlCodes();

    ControlCode saveControlCode(ControlCode controlCode);

    void deleteControlCode(ControlCode controlCode);

    // Instrument related public methods
    Instrument findInstrumentById(Long id);

    List<Instrument> findAllInstruments();

    Instrument saveInstrument(Instrument instrument);

    void deleteInstrument(Instrument instrument);

    // Pad related public methods
    Pad findPadById(Long id);

    List<Pad> findAllPads();

    Pad savePad(Pad pad);

    void deletePad(Pad pad);

    // Pattern related public methods
    Pattern findPatternById(Long id);

    Set<Pattern> findPatternBySongId(Long id);

    List<Pattern> findAllPatterns();

    Pattern savePattern(Pattern pattern);

    void deletePattern(Pattern pattern);

    // Rule related public methods
    Rule findRuleById(Long id);

    Set<Rule> findRulesByPlayerId(Long playerId);

    Rule saveRule(Rule rule);

    void deleteRule(Rule rule);

    // Song related public methods
    Song findSongById(Long id);

    List<Song> findAllSongs();

    Song saveSong(Song song);

    void deleteSong(Song song);

    Song getNextSong(long currentSongId);

    Song getPreviousSong(long currentSongId);

    long getMinimumSongId();

    Long getMaximumSongId();

    // Step related public methods
    Step findStepById(Long id);

    Set<Step> findStepsByPatternId(Long id);

    Step saveStep(Step step);

    void deleteStep(Step step);

    // Strike related public methods
    Strike findStrikeById(Long id);

    Set<Strike> strikesForTicker(Long tickerId);

    IPlayer savePlayer(IPlayer player);

    void deletePlayer(IPlayer player);

    // Ticker related public methods
    Ticker findTickerById(Long id);

    List<Ticker> findAllTickers();

    Ticker saveTicker(Ticker ticker);

    void deleteTicker(Long id);

    long getMinimumTickerId();

    long getMaximumTickerId();

    void deleteAllTickers();

}
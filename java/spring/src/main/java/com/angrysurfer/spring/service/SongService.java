package com.angrysurfer.spring.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.api.Database;
import com.angrysurfer.core.engine.SongEngine;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.util.CyclerListener;
import com.angrysurfer.core.util.TickCyclerListener;
import com.angrysurfer.spring.dao.SongStatus;

import lombok.Getter;
import lombok.Setter;

interface NoteProvider {
    public int getNoteForStep(Step step, Pattern pattern, long tick);

}

@Getter
@Setter
@Service
public class SongService implements NoteProvider {

    static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());

    private Database dbUtils;
    private InstrumentService instrumentService;

    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener tickListener;
    // private CyclerListener beatListener = new BeatCyclerListener();
    // private CyclerListener barListener = new BarCyclerListener();

    private SongEngine songEngine;

    public SongService(Database dbUtils, InstrumentService instrumentService) {
        this.dbUtils = dbUtils;
        this.instrumentService = instrumentService;
        this.songEngine = new SongEngine(instrumentService.getInstrumentEngine());
        this.tickListener = new TickCyclerListener(songEngine);

        Long songId = dbUtils.getMinimumSongId();
        if (Objects.nonNull(songId) && songId > 0) {
            Optional<Song> opt = dbUtils.getSongFindOne().find(songId);
            if (opt.isPresent())
                loadSong(opt.get());
        }

        if (Objects.isNull(songEngine.getSong()))
            songEngine.setSong(newSong());
    }

    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        return this.songEngine.getNoteForStep(step, pattern, tick);
    }

    public SongStatus getSongStatus() {
        return SongStatus.from(getSong());
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        logger.info("updatePattern() - patternId: {}, updateType: {}, updateValue: {}",
                patternId, updateType, updateValue);
        return dbUtils.savePattern(this.songEngine.updatePattern(patternId, updateType, updateValue));
    }

    public Step updateStep(Long stepId, int updateType, int updateValue) {
        logger.info("updateStep() - stepId: {}, updateType: {}, updateValue: {}",
                stepId, updateType, updateValue);
        return dbUtils.saveStep(this.songEngine.updateStep(stepId, updateType, updateValue));
    }

    public Song loadSong(Song song) {

        logger.info("loadSong() - songId: {}", song.getId());
        setSong(song);
        getSong().setPatterns(dbUtils.findPatternBySongId(song.getId()));
        getSong().getPatterns().forEach(pattern -> {
            pattern.setSong(getSong());
            pattern.setSteps(dbUtils.findStepsByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });

        return song;
    }

    public Song newSong() {
        logger.info("newSong()");
        return this.songEngine.newSong(dbUtils.getInstrumentSaver(), dbUtils.getSongSaver(), dbUtils.getPatternSaver(),
                dbUtils.getStepSaver());

    }

    public synchronized Song next(long currentSongId) {
        logger.info("next() - currentSongId: {}", currentSongId);
        if (currentSongId == 0 || getSong().getPatterns().size() > 0) {
            Long maxSongId = dbUtils.getMaximumSongId();
            Song song = Objects.nonNull(maxSongId) && currentSongId < maxSongId
                    ? dbUtils.getNextSong(currentSongId)
                    : null;
            if (Objects.nonNull(song))
                loadSong(song);
            else
                songEngine.setSong(newSong());
        }

        return songEngine.getSong();
    }

    public synchronized Song previous(long currentSongId) {
        logger.info("previous() - currentSongId: {}", currentSongId);
        // songRepo.flush();
        if (currentSongId > (dbUtils.getMinimumSongId()))
            loadSong(dbUtils.getPreviousSong(currentSongId));

        return songEngine.getSong();
    }

    public synchronized void setSong(Song song) {
        songEngine.setSong(song);
    }

    public synchronized Song getSong() {
        return songEngine.getSong();
    }

    public Pattern addPattern() {
        // getSongRepo().flush();
        return this.songEngine.addPattern(dbUtils.getPatternSaver());
    }

    public Set<Pattern> removePattern(Long patternId) {
        Pattern pattern = getSong().getPatterns().stream().filter(s -> s.getId().equals(patternId)).findAny()
                .orElseThrow();
        getSong().getPatterns().remove(pattern);
        dbUtils.deletePattern(pattern);
        return getSong().getPatterns();
    }

    public Song getSongInfo() {
        if (Objects.isNull(songEngine.getSong()))
            next(0);
        return getSong();
    }

    // public Pattern addStep(int page) {
    // getSongRepo().flush();

    // Pattern step = new Pattern();
    // step.setPosition(getSong().getPatterns().size());
    // step.setPage(page);
    // step.setSong(getSong());
    // step = getStepRepo().save(step);
    // getSong().getPatterns().add(step);
    // return step;
    // }

    // public Set<Pattern> removeStep(Long stepId) {
    // Pattern step = getSong().getPatterns().stream().filter(s ->
    // s.getId().equals(stepId)).findAny().orElseThrow();
    // getSong().getPatterns().remove(step);
    // getStepRepo().delete(step);
    // return getSong().getPatterns();
    // }

}
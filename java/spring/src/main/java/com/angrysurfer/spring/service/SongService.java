package com.angrysurfer.spring.service;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.sequencer.SongEngine;
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

    private InstrumentService instrumentService;
    private RedisService redisService = RedisService.getInstance();

    private TickCyclerListener tickListener;

    private SongEngine songEngine = SessionManager.getInstance().getSongEngine();

    public SongService(InstrumentService instrumentService) {

        this.instrumentService = instrumentService;
        this.tickListener = new TickCyclerListener(songEngine);

        // Initialize with first song if available
        Long songId = redisService.getMinimumSongId();
        if (Objects.nonNull(songId) && songId > 0) {
            Song song = redisService.findSongById(songId);
            if (song != null) {
                loadSong(song);
            }
        }

        if (Objects.isNull(songEngine.getActiveSong()))
            songEngine.songSelected(newSong());
    }

    @Override
    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        return songEngine.getNoteForStep(step, pattern, tick);
    }

    public SongStatus getSongStatus() {
        return SongStatus.from(getSong());
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        logger.info("updatePattern() - patternId: {}, updateType: {}, updateValue: {}",
                patternId, updateType, updateValue);
        Pattern pattern = getSong().getPattern(patternId);
        if (pattern != null) {
            pattern = songEngine.updatePattern(pattern, updateType, updateValue);
            return redisService.savePattern(pattern);
        }
        return null;
    }

    public Step updateStep(Long stepId, int updateType, int updateValue) {
        logger.info("updateStep() - stepId: {}, updateType: {}, updateValue: {}",
                stepId, updateType, updateValue);
        Step step = findStep(stepId);
        if (step != null) {
            step = songEngine.updateStep(step, updateType, updateValue);
            return redisService.saveStep(step);
        }
        return null;
    }

    public Song loadSong(Song song) {

        logger.info("loadSong() - songId: {}", song.getId());
        song.setPatterns(redisService.findPatternsBySongId(song.getId()));
        song.getPatterns().forEach(pattern -> {
            pattern.setSong(song);
            pattern.setSteps(redisService.findStepsByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });
        songEngine.songSelected(song);
        return song;
    }

    public Song newSong() {
        logger.info("newSong()");
        Song song = new Song();// songEngine.createNewSong(instrumentService.getInstrumentList());
        return redisService.saveSong(song);
    }

    public synchronized Song next(long currentSongId) {
        logger.info("next() - currentSongId: {}", currentSongId);
        if (currentSongId == 0 || getSong().getPatterns().size() > 0) {
            Long maxSongId = redisService.getMaximumSongId();
            Long nextId = redisService.getNextSongId(currentSongId);
            if (Objects.nonNull(maxSongId) && currentSongId < maxSongId && nextId != null) {
                Song nextSong = redisService.findSongById(nextId);
                if (nextSong != null) {
                    loadSong(nextSong);
                }
            } else {
                songEngine.songSelected(newSong());
            }
        }
        return getSong();
    }

    public synchronized Song previous(long currentSongId) {
        logger.info("previous() - currentSongId: {}", currentSongId);
        Long minId = redisService.getMinimumSongId();
        if (currentSongId > minId) {
            Long prevId = redisService.getPreviousSongId(currentSongId);
            if (prevId != null) {
                loadSong(redisService.findSongById(prevId));
            }
        }
        return getSong();
    }

    public synchronized void setSong(Song song) {
        songEngine.songSelected(song);
    }

    public synchronized Song getSong() {
        return songEngine.getActiveSong();
    }

    public Pattern addPattern() {
        Pattern pattern = new Pattern();
        pattern = songEngine.addPattern(pattern);
        return redisService.savePattern(pattern);
    }

    public Set<Pattern> removePattern(Long patternId) {
        Pattern pattern = getSong().getPattern(patternId);
        if (pattern != null) {
            Set<Pattern> patterns = songEngine.removePattern(pattern);
            redisService.deletePattern(pattern);
            return patterns;
        }
        return getSong().getPatterns();
    }

    private Step findStep(Long stepId) {
        return getSong().getPatterns().stream()
                .flatMap(p -> p.getSteps().stream())
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    public Song getSongInfo() {
        if (Objects.isNull(getSong()))
            next(0);
        return getSong();
    }

}
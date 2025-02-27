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
import com.angrysurfer.core.service.SongManager;
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
    private SongManager songManager = SongManager.getInstance();
    private TickCyclerListener tickListener;

    public SongService(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
        this.tickListener = new TickCyclerListener();

        // Initialize with first song if available
        Long songId = redisService.getMinimumSongId();
        if (Objects.nonNull(songId) && songId > 0) {
            Song song = redisService.findSongById(songId);
            if (song != null) {
                loadSong(song);
            }
        }

        if (Objects.isNull(songManager.getActiveSong())) {
            songManager.songSelected(newSong());
        }
    }

    @Override
    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        return songManager.getNoteForStep(step, pattern, tick);
    }

    public SongStatus getSongStatus() {
        return SongStatus.from(getSong());
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        logger.info("updatePattern() - patternId: {}, updateType: {}, updateValue: {}",
                patternId, updateType, updateValue);
        Pattern pattern = getSong().getPattern(patternId);
        if (pattern != null) {
            pattern = songManager.updatePattern(pattern, updateType, updateValue);
            return redisService.savePattern(pattern);
        }
        return null;
    }

    public Step updateStep(Long stepId, int updateType, int updateValue) {
        logger.info("updateStep() - stepId: {}, updateType: {}, updateValue: {}",
                stepId, updateType, updateValue);
        Step step = findStep(stepId);
        if (step != null) {
            step = songManager.updateStep(step, updateType, updateValue);
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
        songManager.songSelected(song);
        tickListener.setSong(song);
        return song;
    }

    public Song newSong() {
        logger.info("newSong()");
        Song song = new Song();// songManager.createNewSong(instrumentService.getInstrumentList());
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
                songManager.songSelected(newSong());
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
        songManager.songSelected(song);
    }

    public synchronized Song getSong() {
        return songManager.getActiveSong();
    }

    public Pattern addPattern() {
        Pattern pattern = new Pattern();
        pattern = songManager.addPattern(pattern);
        return redisService.savePattern(pattern);
    }

    public Set<Pattern> removePattern(Long patternId) {
        Pattern pattern = getSong().getPattern(patternId);
        if (pattern != null) {
            Set<Pattern> patterns = songManager.removePattern(pattern);
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
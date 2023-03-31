package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.util.CyclerListener;
import com.angrysurfer.midi.util.PatternUpdateType;
import com.angrysurfer.midi.util.StepUpdateType;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.sound.midi.ShortMessage;

@Getter
@Setter
@Service
public class SongService {

    private final class BarCyclerListenerImplementation implements CyclerListener {
        @Override
        public void advanced(long position) {
            logger.info("bars complete");
        }

        @Override
        public void cycleComplete() {
            logger.info("bars complete");
        }

        @Override
        public void starting() {
            this.advanced(1);
        }
    }

    private final class BeatCyclerListenerImplementation implements CyclerListener {

        Stack<Thread> noteOffs = new Stack<>();

        @Override
        public void advanced(long position) {
            handleBeat((int) position);
        }

        @Override
        public void cycleComplete() {
        }

        @Override
        public void starting() {
            song.getPatterns().forEach(p -> {

            });

            handleBeat(0);
        }

        private void handleBeat(Integer position) {

            while (noteOffs.size() > 0)
                noteOffs.pop().start();
            
            song.getPatterns().forEach(pattern -> {
                pattern.getSteps().stream()
                        .filter(s -> s.getActive() && s.getPosition().equals(position)).toList().forEach(step -> {

                    int note = pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose());
                    noteOffs.push(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_OFF, note, step.getVelocity());
                        }
                    }));
                    
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_ON, note, step.getVelocity());
                        }
                    }).start();
            
                });
            });
        }

    }

    static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());

    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    private PatternRepository patternRepository;
    private MIDIService midiService;

    private Song song;
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener beatListener = new BeatCyclerListenerImplementation();
    private CyclerListener barListener = new BarCyclerListenerImplementation();

    public SongService(PatternRepository patternRepository, StepRepository stepRepository,
            SongRepository songRepository, MIDIService midiService) {
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        this.patternRepository = patternRepository;
        this.midiService = midiService;
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        Pattern pattern = getSong().getPatterns().stream().filter(p -> p.getId().equals(patternId)).findFirst().orElseThrow();

        switch (updateType) {
            case PatternUpdateType.ACTIVE:
                pattern.setActive(!pattern.getActive());
                break;

            case PatternUpdateType.LAST_STEP:
                pattern.setLastStep(updateValue);
                break;

            case PatternUpdateType.DIRECTION:
                pattern.setDirection(updateValue);
                break;

            case PatternUpdateType.INSTRUMENT:
                pattern.setInstrument(null);
                midiService.getInstrumentByChannel(updateValue).forEach(i -> pattern.setInstrument(i));
                pattern.setChannel(updateValue);
                // if (pattern.getInstrument().getChannel() != pattern.getChannel())
                //     pattern.getInstrument().setChannel(updateValue);
                break;

            case PatternUpdateType.CHANNEL:
                // pattern.setInstrument(null);
                // midiService.getInstrumentByChannel(updateValue).forEach(i -> pattern.setInstrument(i));
                pattern.setChannel(updateValue);
                // if (pattern.getInstrument().getChannel() != pattern.getChannel())
                //     pattern.getInstrument().setChannel(updateValue);
                break;

            case PatternUpdateType.PROBABILITY:
                pattern.setProbability(updateValue);
                break;

            case PatternUpdateType.RANDOM:
                pattern.setRandom(updateValue);
                break;

            case PatternUpdateType.ROOT_NOTE:
                pattern.setRootNote(updateValue);
                break;

            case PatternUpdateType.SCALE:
                pattern.setScale(updateValue);
                break;

            case PatternUpdateType.LENGTH:
                pattern.setLength(updateValue);
                break;

            case PatternUpdateType.SWING:
                pattern.setSwing(updateValue);
                break;

            case PatternUpdateType.PRESET:
                pattern.setPreset(updateValue);
                break;

            case PatternUpdateType.REPEATS:
                pattern.setRepeats(updateValue);
                break;

            case PatternUpdateType.GATE:
                pattern.setGate(updateValue);
                break;

            case PatternUpdateType.TRANSPOSE:
                pattern.setTranspose(updateValue);
                break;
        }

        return getPatternRepository().save(pattern);
    }

    public Step updateStep(Long stepId, int updateType, int updateValue) {

        Step step = getSong().getStep(stepId);

        switch (updateType) {
            case StepUpdateType.ACTIVE:
                step.setActive(!step.getActive());
                break;

            case StepUpdateType.GATE:
                step.setGate(updateValue);
                break;

            case StepUpdateType.PITCH:
                step.setPitch(updateValue);
                break;

            case StepUpdateType.PROBABILITY:
                step.setProbability(updateValue);
                break;

            case StepUpdateType.VELOCITY:
                step.setVelocity(updateValue);
                break;
        }

        // Map<Integer, Pattern> page = songStepsMap.containsKey(step.getPage()) ?
        // songStepsMap.get(step.getPage()) : new ConcurrentHashMap<>();
        // page.put(step.getIndex(), step);
        // songStepsMap.put(step.getPage(), page);

        return getStepDataRepository().save(step);
    }

    public Song loadSong(Song song) {
        song.setPatterns(getPatternRepository().findBySongId(getSong().getId()));
        song.getPatterns().forEach(pattern -> {
            pattern.setSong(getSong());
            pattern.setSteps(getStepDataRepository().findByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });

        if (song.getPatterns().size() == 0) {

            getPatternRepository().findBySongId(song.getId()).forEach(p -> getPatternRepository().delete(p));

            IntStream.range(0, 8).forEach(i -> {
                Pattern pattern = new Pattern();
                pattern.setSong(getSong());
                pattern.setPosition(i);
                getPatternRepository().save(pattern);
    
                song.getPatterns().add(pattern);
    
                IntStream.range(0, 16).forEach(j -> {
                    Step step = new Step();
                    step.setPattern(pattern);
                    step.setPosition(j);
                    getStepDataRepository().save(step);
    
                    pattern.getSteps().add(step);
                });
    
            });
        }

        return song;
    }

    public Song newSong() {
        songRepository.flush();
        Song song = songRepository.save(new Song());

        IntStream.range(0, 8).forEach(i -> {
            Pattern pattern = new Pattern();
            pattern.setSong(getSong());
            pattern.setPosition(i);
            getPatternRepository().save(pattern);

            song.getPatterns().add(pattern);

            IntStream.range(0, 16).forEach(j -> {
                Step step = new Step();
                step.setPattern(pattern);
                step.setPosition(j);
                getStepDataRepository().save(step);

                pattern.getSteps().add(step);
            });

        });

        setSong(song);

        return song;
    }

    public synchronized Song next(long currentSongId) {
        songRepository.flush();
        if (currentSongId == 0 || getSong().getPatterns().size() > 0) {
            Long maxSongId = getSongRepository().getMaximumSongId();
            setSong(Objects.nonNull(maxSongId) && currentSongId < maxSongId
                    ? getSongRepository().getNextSong(currentSongId)
                    : null);
            if (Objects.nonNull(getSong()))
                loadSong(getSong());
            else
                setSong(newSong());
        }

        return getSong();
    }

    public synchronized Song previous(long currentSongId) {
        songRepository.flush();
        if (currentSongId > (getSongRepository().getMinimumSongId())) {
            setSong(getSongRepository().getPreviousSong(currentSongId));
            loadSong(getSong());
        }

        return getSong();
    }

    // public Song getSong() {
    // if (Objects.isNull(song))
    // setSong(newSong());

    // return this.song;
    // }

    public Pattern addPattern(int page) {
        getSongRepository().flush();

        Pattern pattern = new Pattern();
        pattern.setPosition(getSong().getPatterns().size());
        // pattern.setPage(page);
        pattern.setSong(getSong());
        pattern = getPatternRepository().save(pattern);
        getSong().getPatterns().add(pattern);
        return pattern;
    }

    public Set<Pattern> removePattern(Long patternId) {
        Pattern pattern = getSong().getPatterns().stream().filter(s -> s.getId().equals(patternId)).findAny()
                .orElseThrow();
        getSong().getPatterns().remove(pattern);
        getPatternRepository().delete(pattern);
        return getSong().getPatterns();
    }

    public Song getSongInfo() {
        if (Objects.isNull(this.song))
            next(0);
        return getSong();
    }

    // public Pattern addStep(int page) {
    // getSongRepository().flush();

    // Pattern step = new Pattern();
    // step.setPosition(getSong().getPatterns().size());
    // step.setPage(page);
    // step.setSong(getSong());
    // step = getStepDataRepository().save(step);
    // getSong().getPatterns().add(step);
    // return step;
    // }

    // public Set<Pattern> removeStep(Long stepId) {
    // Pattern step = getSong().getPatterns().stream().filter(s ->
    // s.getId().equals(stepId)).findAny().orElseThrow();
    // getSong().getPatterns().remove(step);
    // getStepDataRepository().delete(step);
    // return getSong().getPatterns();
    // }

}
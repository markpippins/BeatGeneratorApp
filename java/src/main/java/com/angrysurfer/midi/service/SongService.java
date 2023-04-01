package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.util.Cycler;
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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
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
            this.advanced(0);
        }
    }

    private final class BeatCyclerListenerImplementation implements CyclerListener {

        @Override
        public void advanced(long position) {
            handleBeat((int) position - 1);
        }

        @Override
        public void cycleComplete() {
        }

        @Override
        public void starting() {
            song.getPatterns().forEach(p -> {
                p.getStepCycler().setLength(p.getLastStep());
            });
        }

        private void handleBeat(Integer position) {

            logger.info(String.format("handling beat %s", position));
            
            song.getPatterns().forEach(pattern -> {
                double playTime = song.getBeatDuration() / pattern.getBeatDivider();

                // pattern.getSteps().stream()
                //         .filter(s -> ((long ) s.getPosition() == patter)).toList().forEach(step -> {

                    logger.info(String.format("Pattern %s, Step %s", pattern.getPosition(), pattern.getStepCycler().get()));
                    IntStream.range(0, pattern.getBeatDivider()).forEach(bd -> {

                        Step step = pattern.getSteps().stream().filter(s -> s.getPosition() == pattern.getStepCycler().get() - 1).findAny().orElseThrow();
                        double delay = playTime * step.getPosition();
    
                        if (step.getActive()) {
                            int note = pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (step.getPosition() > 1)
                                        try {
                                            Thread.sleep((long) delay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        
                                    midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_ON, note, step.getVelocity());
                                    
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep((long) (pattern.getGate() * 0.1 * delay));
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                
                                            midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_OFF, note, step.getVelocity());
                                        }
                                    }).start();
                                }
                            }).start();
    
                        }
    
                        pattern.getStepCycler().advance();

                    });



                // });
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

    public Song getSong() {
        if (Objects.isNull(song))
            return next(0);

        return this.song;
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

            case PatternUpdateType.BEAT_DIVIDER:
                pattern.setBeatDivider(updateValue);
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

            case PatternUpdateType.PRESET:
                pattern.setPreset(updateValue);
                try {
                    pattern.getInstrument().programChange(updateValue, 0);
                } catch (InvalidMidiDataException | MidiUnavailableException e) {
                    logger.error(e.getMessage(), e);
                }
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

        // if (song.getPatterns().size() == 0) {

        //     getPatternRepository().findBySongId(song.getId()).forEach(p -> getPatternRepository().delete(p));

        //     IntStream.range(0, 6).forEach(page -> {
        //         Pattern pattern = new Pattern();
        //         pattern.setSong(getSong());
        //         pattern.setPosition(page);
        //         pattern.setInstrument(this.midiService.getInstrumentByChannel(page).get(0));
        //         pattern.setChannel(page);
        //         getPatternRepository().save(pattern);
    
        //         song.getPatterns().add(pattern);
    
        //         IntStream.range(0, 16).forEach(j -> {
        //             Step step = new Step();
        //             step.setPattern(pattern);
        //             step.setPosition(j);
        //             getStepDataRepository().save(step);
    
        //             pattern.getSteps().add(step);
        //         });
    
        //     });
        // }

        return song;
    }

    public Song newSong() {
        songRepository.flush();
        Song song = songRepository.save(new Song());

        IntStream.range(0, 8).forEach(i -> {
            Pattern pattern = new Pattern();
            pattern.setSong(getSong());
            pattern.setPosition(i);
            pattern.setInstrument(this.midiService.getInstrumentByChannel(i).get(0));
            pattern.setChannel(i);
            pattern.setName(pattern.getInstrument().getName());
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
            if (Objects.nonNull(this.song)) 
                loadSong(this.song);
            else
                setSong(newSong());
        }

        return this.song;
    }

    public synchronized Song previous(long currentSongId) {
        songRepository.flush();
        if (currentSongId > (getSongRepository().getMinimumSongId())) {
            setSong(getSongRepository().getPreviousSong(currentSongId));
            loadSong(this.song);
        }

        return this.song;
    }

    // public Song getSong() {
    // if (Objects.isNull(song))
    // setSong(newSong());

    // return this.song;
    // }

    public Pattern addPattern() {
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
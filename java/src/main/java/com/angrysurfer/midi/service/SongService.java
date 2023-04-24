package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepo;
import com.angrysurfer.midi.repo.SongRepo;
import com.angrysurfer.midi.repo.StepRepo;
import com.angrysurfer.midi.util.Constants;
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

    private final class TickCyclerListener implements CyclerListener {

        static final Random rand = new Random();

        private Integer ticks = 0;

        @Override
        public void advanced(long tick) {
            if (tick == 0)
                onBeatStart();
            handleTick(tick);
        }

        private void handleTick(long tick) {

            song.getPatterns().forEach(pattern -> {

                // logger.info(String.format("Pattern  %s", pattern.getPosition()));

                while (pattern.getStepCycler().get() < pattern.getFirstStep())
                    pattern.getStepCycler().advance();

                Step step = pattern.getSteps().stream()
                        .filter(s -> s.getPosition() == (long) pattern.getStepCycler().get())
                        .findAny().orElseThrow();

                // logger.info(String.format("Pattern %s, Step %s", pattern.getPosition(), step.getPosition()));

                // if (!pattern.getMuted() && (tick == 0 || (tick % (song.getTicksPerBeat() / pattern.getSpeed()) == 0))) {
                //     while (!pattern.getPlayingNote().empty())
                //         midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_OFF,
                //                 pattern.getPlayingNote().pop(), 0);

                //     if (step.getActive() && step.getProbability() == 100
                //             || (step.getProbability() > rand.nextInt(100))
                //                     && pattern.getLength() > step.getPosition()) {

                //         final int note = pattern.getQuantize() ? pattern.getQuantizer().quantizeNote(
                //                 pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose()))
                //                 : pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose());

                //         // TO DO: Offer a whole-gate option as implented by pushing note on
                //         // Pattern.playingNote OR the thread implementation below
                //         // pattern.getPlayingNote().push(note);
                //         new Thread(new Runnable() {
                //             @Override
                //             public void run() {
                //                 try {
                //                     midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_ON,
                //                             note, step.getVelocity());
                //                     Thread.sleep((long) (1.0 / step.getGate() * song.getBeatDuration()));
                //                     midiService.sendMessageToChannel(pattern.getChannel(), ShortMessage.NOTE_OFF,
                //                             note, step.getVelocity());
                //                 } catch (InterruptedException e) {
                //                     logger.error(e.getMessage(), e);
                //                 }
                //             }
                //         }).start();
                //     }
                // }

                pattern.getStepCycler().advance();
            });

            ticks++;
        }

        private void onBeatStart() {
            ticks = 0;
        }

        @Override
        public void cycleComplete() {
            // logger.info("ticks complete");
        }

        @Override
        public void starting() {
            logger.info("starting");
            getSong().getPatterns().forEach(p -> p.getStepCycler().setLength(p.getSteps().size()));
            getSong().getPatterns().forEach(p -> p.getStepCycler().reset());
            this.advanced(0);
        }
    }

    static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());

    private StepRepo stepRepo;
    private SongRepo songRepo;
    private PatternRepo patternRepo;
    private MIDIService midiService;

    private Song song;
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener tickListener = new TickCyclerListener();
    // private CyclerListener beatListener = new BeatCyclerListener();
    // private CyclerListener barListener = new BarCyclerListener();

    public SongService(PatternRepo patternRepo, StepRepo stepRepo,
            SongRepo songRepo, MIDIService midiService) {
        this.stepRepo = stepRepo;
        this.songRepo = songRepo;
        this.patternRepo = patternRepo;
        this.midiService = midiService;
    }

    public Song getSong() {
        if (Objects.isNull(song))
            return next(0);
        return this.song;
    }

    public SongStatus getSongStatus() {
        return SongStatus.from(getSong());
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        Pattern pattern = getSong().getPatterns().stream().filter(p -> p.getId().equals(patternId)).findFirst()
                .orElseThrow();

        switch (updateType) {
            case PatternUpdateType.ACTIVE:
                pattern.setActive(!pattern.getActive());
                break;

            case PatternUpdateType.FIRST_STEP:
                pattern.setFirstStep(updateValue);
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

            case PatternUpdateType.SPEED:
                pattern.setSpeed(updateValue);
                break;

            case PatternUpdateType.INSTRUMENT:
                pattern.setInstrument(null);
                midiService.getInstrumentByChannel(updateValue).forEach(i -> pattern.setInstrument(i));
                pattern.setChannel(updateValue);
                // if (pattern.getInstrument().getChannel() != pattern.getChannel())
                // pattern.getInstrument().setChannel(updateValue);
                break;

            case PatternUpdateType.CHANNEL:
                // pattern.setInstrument(null);
                // midiService.getInstrumentByChannel(updateValue).forEach(i ->
                // pattern.setInstrument(i));
                pattern.setChannel(updateValue);
                // if (pattern.getInstrument().getChannel() != pattern.getChannel())
                // pattern.getInstrument().setChannel(updateValue);
                break;

            case PatternUpdateType.QUANTIZE:
                pattern.setQuantize(!pattern.getQuantize());
                break;

            case PatternUpdateType.MUTE:
                pattern.setMuted(!pattern.getMuted());
                break;

            case PatternUpdateType.LOOP:
                pattern.setLoop(!pattern.getLoop());
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

        return getPatternRepo().save(pattern);
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

        return getStepRepo().save(step);
    }

    public Song loadSong(Song song) {
        song.setPatterns(getPatternRepo().findBySongId(getSong().getId()));
        song.getPatterns().forEach(pattern -> {
            pattern.setSong(getSong());
            pattern.setSteps(getStepRepo().findByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });

        // if (song.getPatterns().size() == 0) {

        // getPatternRepo().findBySongId(song.getId()).forEach(p ->
        // getPatternRepo().delete(p));

        // IntStream.range(0, 6).forEach(page -> {
        // Pattern pattern = new Pattern();
        // pattern.setSong(getSong());
        // pattern.setPosition(page);
        // pattern.setInstrument(this.midiService.getInstrumentByChannel(page).get(0));
        // pattern.setChannel(page);
        // getPatternRepo().save(pattern);

        // song.getPatterns().add(pattern);

        // IntStream.range(0, 16).forEach(j -> {
        // Step step = new Step();
        // step.setPattern(pattern);
        // step.setPosition(j);
        // getStepRepo().save(step);

        // pattern.getSteps().add(step);
        // });

        // });
        // }

        return song;
    }

    public Song newSong() {
        songRepo.flush();
        Song song = songRepo.save(new Song());

        // picking up MIDI channels 2-9
        IntStream.range(1, Constants.DEFAULT_XOX_TRACKS).forEach(i -> {
            Pattern pattern = new Pattern();
            pattern.setSong(getSong());
            pattern.setPosition(song.getPatterns().size() + 1);
            pattern.setInstrument(this.midiService.getInstrumentByChannel(i).get(0));
            pattern.setChannel(i);
            pattern.setName(pattern.getInstrument().getName());
            getPatternRepo().save(pattern);
            song.getPatterns().add(pattern);

            IntStream.range(0, Constants.DEFAULT_XOX_PATTERN_LENGTH).forEach(j -> {
                Step step = new Step();
                step.setPattern(pattern);
                step.setPosition(pattern.getSteps().size() + 1);
                getStepRepo().save(step);

                pattern.getSteps().add(step);
            });

        });

        setSong(song);

        return song;
    }

    public synchronized Song next(long currentSongId) {
        songRepo.flush();
        if (currentSongId == 0 || getSong().getPatterns().size() > 0) {
            Long maxSongId = getSongRepo().getMaximumSongId();
            setSong(Objects.nonNull(maxSongId) && currentSongId < maxSongId
                    ? getSongRepo().getNextSong(currentSongId)
                    : null);
            if (Objects.nonNull(this.song))
                loadSong(this.song);
            else
                setSong(newSong());
        }

        return this.song;
    }

    public synchronized Song previous(long currentSongId) {
        songRepo.flush();
        if (currentSongId > (getSongRepo().getMinimumSongId())) {
            setSong(getSongRepo().getPreviousSong(currentSongId));
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
        getSongRepo().flush();

        Pattern pattern = new Pattern();
        pattern.setPosition(getSong().getPatterns().size());
        // pattern.setPage(page);
        pattern.setSong(getSong());
        pattern = getPatternRepo().save(pattern);
        getSong().getPatterns().add(pattern);
        return pattern;
    }

    public Set<Pattern> removePattern(Long patternId) {
        Pattern pattern = getSong().getPatterns().stream().filter(s -> s.getId().equals(patternId)).findAny()
                .orElseThrow();
        getSong().getPatterns().remove(pattern);
        getPatternRepo().delete(pattern);
        return getSong().getPatterns();
    }

    public Song getSongInfo() {
        if (Objects.isNull(this.song))
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
package com.angrysurfer.spring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.CyclerListener;
import com.angrysurfer.core.util.update.PatternUpdateType;
import com.angrysurfer.core.util.update.StepUpdateType;
import com.angrysurfer.spring.dao.SongStatus;
import com.angrysurfer.spring.repo.Patterns;
import com.angrysurfer.spring.repo.Songs;
import com.angrysurfer.spring.repo.Steps;

import lombok.Getter;
import lombok.Setter;

interface NoteProvider {
    public int getNoteForStep(Step step, Pattern pattern, long tick);

}

@Getter
@Setter
@Service
public class SongService implements NoteProvider {
    private final class TickCyclerListener implements CyclerListener {

        private Integer ticks = 0;

        @Override
        public void advanced(long tick) {
            if (tick == 0)
                onBeatStart();
            handleTick(tick);
        }

        private void handleTick(long tick) {
            // logger.info(String.format("Tick %s", tick));
            getSong().getPatterns().forEach(pattern -> {

                // logger.info(String.format("Pattern %s", pattern.getPosition()));

                while (pattern.getStepCycler().get() < pattern.getFirstStep())
                    pattern.getStepCycler().advance();

                Step step = pattern.getSteps().stream()
                        .filter(s -> s.getPosition() == (long) pattern.getStepCycler().get())
                        .findAny().orElseThrow();

                final int note = getNoteForStep(step, pattern, tick);
                if (note > -1) {
                    logger.info(String.format("Note: %s", note));

                    // // TO DO: Offer a whole-gate option as implented by pushing note on
                    // // Pattern.playingNote OR the thread implementation below
                    pattern.getPlayingNote().push(note);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pattern.getInstrument().sendToDevice(
                                        new ShortMessage(ShortMessage.NOTE_ON, pattern.getChannel(), note, note));
                                Thread.sleep((long) (1.0 / step.getGate() * getSong().getBeatDuration()));
                                pattern.getInstrument().sendToDevice(
                                        new ShortMessage(ShortMessage.NOTE_OFF, pattern.getChannel(), 0, 0));
                            } catch (InterruptedException | MidiUnavailableException | InvalidMidiDataException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }).start();
                }

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
            getSong().getPatterns().forEach(p -> p.getStepCycler().setLength((long) p.getSteps().size()));
            getSong().getPatterns().forEach(p -> p.getStepCycler().reset());
            this.advanced(0);
        }
    }

    static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());

    private Steps stepRepo;
    private Songs songRepo;
    private Patterns patternRepo;
    private MIDIService midiService;
    InstrumentService instrumentService;

    private Song song;
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener tickListener = new TickCyclerListener();
    // private CyclerListener beatListener = new BeatCyclerListener();
    // private CyclerListener barListener = new BarCyclerListener();

    public SongService(Patterns patternRepo, Steps stepRepo,
            Songs songRepo, MIDIService midiService, InstrumentService instrumentService) {
        this.stepRepo = stepRepo;
        this.songRepo = songRepo;
        this.patternRepo = patternRepo;
        this.midiService = midiService;
        this.instrumentService = instrumentService;
        this.song = newSong();
    }

    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        logger.info(String.format("getNoteForStep() Pattern %s, Step %s", pattern.getPosition(),
        step.getPosition()));

        Random rand = new Random();
        int note = -1;

        if (!pattern.getMuted() && (tick == 0 || (tick % (getSong().getTicksPerBeat() /
                pattern.getSpeed()) == 0))) {

            if (step.getActive() && step.getProbability() == 100
                    || (step.getProbability() > rand.nextInt(100))
                            && pattern.getLength() > step.getPosition()) {

                note = pattern.getQuantize() ? pattern.getQuantizer().quantizeNote(
                        pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose()))
                        : pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose());

            }
        }

        return note;
    }

    public SongStatus getSongStatus() {
        return SongStatus.from(getSong());
    }

    public Pattern updatePattern(Long patternId, int updateType, int updateValue) {
        logger.info("updatePattern() - patternId: {}, updateType: {}, updateValue: {}", 
            patternId, updateType, updateValue);
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
                // pattern.setInstrument(null);
                // midiService.getInstrumentByChannel(updateValue).forEach(i ->
                // pattern.setInstrument(i));
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
                    pattern.getInstrument().programChange(pattern.getChannel(), updateValue, 0);
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
        logger.info("updateStep() - stepId: {}, updateType: {}, updateValue: {}", 
            stepId, updateType, updateValue);
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
        logger.info("loadSong() - songId: {}", song.getId());
        getSong().setPatterns(getPatternRepo().findBySongId(getSong().getId()));
        getSong().getPatterns().forEach(pattern -> {
            pattern.setSong(getSong());
            pattern.setSteps(getStepRepo().findByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });

        return song;
    }

    public Song newSong() {
        logger.info("newSong()");
        this.song = new Song();

        List<String> devices = new ArrayList<>();

        MIDIService.getMidiOutDevices().forEach(device -> devices.add(device.getDeviceInfo().getName()));

        if (devices.size() > 0)
            try {
                songRepo.save(song);

                final List<Instrument> instruments = this.instrumentService.getAllInstruments();

                IntStream.range(0, Constants.DEFAULT_XOX_TRACKS).forEach(i -> {
                    Pattern pattern = new Pattern();
                    pattern.setSong(song);
                    pattern.setPosition(song.getPatterns().size() + 1);

                    var vv = instruments.stream().filter(ins -> ins.receivesOn(i) && ins.getAvailable()).toList();

                    if (vv.size() > 0) {
                        pattern.setInstrument((Instrument) vv.getFirst());
                        pattern.setName(((Instrument) vv.getFirst()).getName());
                    } else {

                        if (devices.size() > 0) {
                            Instrument instrument = new Instrument();
                            Integer[] channels = { i };
                            instrument.setChannels(channels);
                            instrument.setDeviceName(devices.getFirst());
                            instrument
                                    .setName(devices.getFirst() + " " + Integer.toString(i));
                            // instrument.setDevice(devices.getFirst());
                            instrument = instrumentService.save(instrument);
                            pattern.setInstrument(instrument);
                        }
                    }

                    pattern.setChannel(i);
                    song.getPatterns().add(getPatternRepo().save(pattern));
                    IntStream.range(0, 16).forEach(j -> {
                        Step step = new Step();
                        step.setPattern(pattern);
                        step.setPosition(pattern.getSteps().size() + 1);
                        pattern.getSteps().add(getStepRepo().save(step));
                    });
                });
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        return song;
    }

    public synchronized Song next(long currentSongId) {
        logger.info("next() - currentSongId: {}", currentSongId);
        if (currentSongId == 0 || getSong().getPatterns().size() > 0) {
            Long maxSongId = getSongRepo().getMaximumSongId();
            setSong(Objects.nonNull(maxSongId) && currentSongId < maxSongId
                    ? getSongRepo().getNextSong(currentSongId)
                    : null);
            if (Objects.nonNull(this.song))
                loadSong(this.song);
            else
                newSong();
        }

        return this.song;
    }

    public synchronized Song previous(long currentSongId) {
        logger.info("previous() - currentSongId: {}", currentSongId);
        songRepo.flush();
        if (currentSongId > (getSongRepo().getMinimumSongId())) {
            setSong(getSongRepo().getPreviousSong(currentSongId));
            loadSong(this.song);
        }

        return this.song;
    }

    public synchronized Song getSong() {
        if (Objects.isNull(song))
            newSong();

        return this.song;
    }

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
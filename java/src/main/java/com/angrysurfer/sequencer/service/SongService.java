package com.angrysurfer.sequencer.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.sequencer.dao.SongStatus;
import com.angrysurfer.sequencer.model.Pattern;
import com.angrysurfer.sequencer.model.Song;
import com.angrysurfer.sequencer.model.Step;
import com.angrysurfer.sequencer.model.midi.Instrument;
import com.angrysurfer.sequencer.repo.PatternRepo;
import com.angrysurfer.sequencer.repo.SongRepo;
import com.angrysurfer.sequencer.repo.StepRepo;
import com.angrysurfer.sequencer.util.Constants;
import com.angrysurfer.sequencer.util.listener.CyclerListener;
import com.angrysurfer.sequencer.util.update.PatternUpdateType;
import com.angrysurfer.sequencer.util.update.StepUpdateType;

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

                // logger.info(String.format("Pattern %s, Step %s", pattern.getPosition(),
                // step.getPosition()));

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
                                midiService.sendMessage(pattern.getInstrument(), pattern.getChannel(),
                                        ShortMessage.NOTE_ON, note, note);
                                Thread.sleep((long) (1.0 / step.getGate() * getSong().getBeatDuration()));
                                midiService.sendMessage(pattern.getInstrument(), pattern.getChannel(),
                                        ShortMessage.NOTE_OFF, note, note);
                            } catch (InterruptedException e) {
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

    private StepRepo stepRepo;
    private SongRepo songRepo;
    private PatternRepo patternRepo;
    private MIDIService midiService;
    InstrumentService instrumentService;

    private Song song;
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener tickListener = new TickCyclerListener();
    // private CyclerListener beatListener = new BeatCyclerListener();
    // private CyclerListener barListener = new BarCyclerListener();

    public SongService(PatternRepo patternRepo, StepRepo stepRepo,
            SongRepo songRepo, MIDIService midiService, InstrumentService instrumentService) {
        this.stepRepo = stepRepo;
        this.songRepo = songRepo;
        this.patternRepo = patternRepo;
        this.midiService = midiService;
        this.instrumentService = instrumentService;
        this.song = newSong();
    }

    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        // logger.info(String.format("Pattern %s, Step %s", pattern.getPosition(),
        // step.getPosition()));

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
        getSong().setPatterns(getPatternRepo().findBySongId(getSong().getId()));
        getSong().getPatterns().forEach(pattern -> {
            pattern.setSong(getSong());
            pattern.setSteps(getStepRepo().findByPatternId(pattern.getId()));
            pattern.getSteps().forEach(s -> s.setPattern(pattern));
        });

        return song;
    }

    public Song newSong() {
        this.song = new Song();

        try {
            songRepo.save(song);

            IntStream.range(1, Constants.DEFAULT_XOX_TRACKS).forEach(i -> {
                Pattern pattern = new Pattern();
                pattern.setSong(song);
                pattern.setPosition(song.getPatterns().size() + 1);
                List<Instrument> instruments = this.instrumentService.getInstrumentByChannel(i);
                if (instruments.size() > 0) {
                    pattern.setInstrument((Instrument) instruments.get(0));
                    pattern.setName(((Instrument) instruments.get(0)).getName());
                } else {
                    MidiDevice[] devices = { null, null };

                    MIDIService.getMidiOutDevices().forEach(device -> {
                        logger.info("Device: " + device.getDeviceInfo().getName());
                        if (device.getDeviceInfo().getName().contains("Microsoft GS")) {
                            devices[0] = device;
                        }
                        if (device.getDeviceInfo().getName().contains("Gervill")) {
                            devices[1] = device;
                        }
                    });

                    if (Objects.nonNull(devices[0])) {
                        Instrument instrument = new Instrument();
                        instrument.setChannel(i);
                        instrument.setDeviceName(devices[0].getDeviceInfo().getName());
                        instrument.setName(devices[0].getDeviceInfo().getName());
                        instrument.setDevice(devices[0]);
                        // instrument = instrumentService.save(instrument);
                        pattern.setInstrument(instrument);
                    } else

                    if (Objects.nonNull(devices[1])) {
                        Instrument instrument = new Instrument();
                        instrument.setChannel(i);
                        instrument.setDeviceName(devices[1].getDeviceInfo().getName());
                        instrument.setName(devices[1].getDeviceInfo().getName());
                        instrument.setDevice(devices[1]);
                        // instrument = instrumentService.save(instrument);
                        pattern.setInstrument(instrument);
                    }
                }

                pattern.setChannel(i);
                song.getPatterns().add(getPatternRepo().save(pattern));
                IntStream.range(0, Constants.DEFAULT_XOX_PATTERN_LENGTH).forEach(j -> {
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
package com.angrysurfer.core.sequencer;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.util.update.PatternUpdateType;
import com.angrysurfer.core.util.update.StepUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SongEngine {
    private static final Logger logger = LoggerFactory.getLogger(SongEngine.class);

    private Song activeSong;
    private CommandBus commandBus = CommandBus.getInstance();
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    public SongEngine() {
        setupCommandBusListener();
    }

    // public static SongEngine getInstance() {
    // if (instance == null) {
    // instance = new SongEngine();
    // }
    // return instance;
    // }

    public Pattern updatePattern(Pattern pattern, int updateType, int updateValue) {
        logger.info("updatePattern() - patternId: {}, updateType: {}, updateValue: {}",
                pattern.getId(), updateType, updateValue);

        switch (updateType) {
            case PatternUpdateType.ACTIVE -> pattern.setActive(!pattern.getActive());
            case PatternUpdateType.FIRST_STEP -> pattern.setFirstStep(updateValue);
            case PatternUpdateType.LAST_STEP -> pattern.setLastStep(updateValue);
            case PatternUpdateType.BEAT_DIVIDER -> pattern.setBeatDivider(updateValue);
            case PatternUpdateType.DIRECTION -> pattern.setDirection(updateValue);
            case PatternUpdateType.SPEED -> pattern.setSpeed(updateValue);
            case PatternUpdateType.CHANNEL -> pattern.setChannel(updateValue);
            case PatternUpdateType.QUANTIZE -> pattern.setQuantize(!pattern.getQuantize());
            case PatternUpdateType.MUTE -> pattern.setMuted(!pattern.getMuted());
            case PatternUpdateType.LOOP -> pattern.setLoop(!pattern.getLoop());
            case PatternUpdateType.RANDOM -> pattern.setRandom(updateValue);
            case PatternUpdateType.PRESET -> handlePresetChange(pattern, updateValue);
            case PatternUpdateType.ROOT_NOTE -> pattern.setRootNote(updateValue);
            case PatternUpdateType.SCALE -> pattern.setScale(updateValue);
            case PatternUpdateType.LENGTH -> pattern.setLength(updateValue);
            case PatternUpdateType.SWING -> pattern.setSwing(updateValue);
            case PatternUpdateType.REPEATS -> pattern.setRepeats(updateValue);
            case PatternUpdateType.GATE -> pattern.setGate(updateValue);
            case PatternUpdateType.TRANSPOSE -> pattern.setTranspose(updateValue);
        }

        commandBus.publish(Commands.PATTERN_UPDATED, this, pattern);
        return pattern;
    }

    private void handlePresetChange(Pattern pattern, int updateValue) {
        pattern.setPreset(updateValue);
        if (pattern.getInstrument() != null) {
            pattern.getInstrument().programChange(updateValue, 0);
        }
    }

    public Step updateStep(Step step, int updateType, int updateValue) {
        logger.info("updateStep() - stepId: {}, updateType: {}, updateValue: {}",
                step.getId(), updateType, updateValue);

        switch (updateType) {
            case StepUpdateType.ACTIVE -> step.setActive(!step.getActive());
            case StepUpdateType.GATE -> step.setGate(updateValue);
            case StepUpdateType.PITCH -> step.setPitch(updateValue);
            case StepUpdateType.PROBABILITY -> step.setProbability(updateValue);
            case StepUpdateType.VELOCITY -> step.setVelocity(updateValue);
        }

        commandBus.publish(Commands.STEP_UPDATED, this, step);
        return step;
    }

    public Pattern addPattern(Pattern pattern) {
        pattern.setPosition(activeSong.getPatterns().size());
        pattern.setSong(activeSong);
        activeSong.getPatterns().add(pattern);
        commandBus.publish(Commands.PATTERN_ADDED, this, pattern);
        return pattern;
    }

    public Set<Pattern> removePattern(Pattern pattern) {
        activeSong.getPatterns().remove(pattern);
        commandBus.publish(Commands.PATTERN_REMOVED, this, pattern);
        return activeSong.getPatterns();
    }

    public int getNoteForStep(Step step, Pattern pattern, long tick) {
        if (!pattern.getMuted() && (tick == 0 || (tick % (activeSong.getTicksPerBeat() / pattern.getSpeed()) == 0))) {
            Random rand = new Random();
            if (step.getActive() && step.getProbability() == 100 ||
                    (step.getProbability() > rand.nextInt(100)) && pattern.getLength() > step.getPosition()) {

                return pattern.getQuantize()
                        ? pattern.getQuantizer().quantizeNote(pattern.getRootNote() + step.getPitch() +
                                (12 * pattern.getTranspose()))
                        : pattern.getRootNote() + step.getPitch() + (12 * pattern.getTranspose());
            }
        }
        return -1;
    }

    private void setupCommandBusListener() {
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.SONG_SELECTED -> {
                        if (action.getData() instanceof Song) {
                            songSelected((Song) action.getData());
                        }
                    }
                    case Commands.SONG_UPDATED -> {
                        if (action.getData() instanceof Song) {
                            songUpdated((Song) action.getData());
                        }
                    }
                }
            }
        });
    }

    public void songSelected(Song song) {
        if (!Objects.equals(activeSong, song)) {
            this.activeSong = song;
            commandBus.publish(Commands.SONG_CHANGED, this, song);
        }
    }

    public void songUpdated(Song song) {
        if (Objects.equals(activeSong.getId(), song.getId())) {
            this.activeSong = song;
            commandBus.publish(Commands.SONG_CHANGED, this, song);
        }
    }

    public boolean canMoveForward() {
        return true; // Implement actual logic based on your requirements
    }

    public boolean canMoveBack() {
        return activeSong != null && activeSong.getId() > 1;
    }

    public void moveForward() {
        // Implement navigation logic
    }

    public void moveBack() {
        // Implement navigation logic
    }
}
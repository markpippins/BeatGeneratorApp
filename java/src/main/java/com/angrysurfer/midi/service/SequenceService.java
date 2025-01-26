package com.angrysurfer.midi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.midi.Sequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.midi.model.Pattern;
import com.angrysurfer.midi.model.Song;
import com.angrysurfer.midi.model.Step;
import com.angrysurfer.midi.util.listener.CyclerListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class SequenceService {

    static Logger logger = LoggerFactory.getLogger(SequenceService.class.getCanonicalName());

    private SongService songService;
    private TickerService tickerService;

    private Song song;
    private Map<Integer, Map<Integer, Pattern>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener sequenceGenerator = new SequenceGenerator();

    public SequenceService(SongService songService, TickerService tickerService) {
        this.songService = songService;
        this.tickerService = tickerService;
    }

    public void populateSequence(Sequence sequence) {
        this.song = songService.getSong();
        // this.tickerService.getTicker()
    }

    private final class SequenceGenerator implements CyclerListener {

        private Integer ticks = 0;

        @Override
        public void advanced(long tick) {
            if (tick == 0)
                onBeatStart();
            handleTick(tick);
        }

        private void handleTick(long tick) {

            song.getPatterns().forEach(pattern -> {

                // logger.info(String.format("Pattern %s", pattern.getPosition()));

                while (pattern.getStepCycler().get() < pattern.getFirstStep())
                    pattern.getStepCycler().advance();

                Step step = pattern.getSteps().stream()
                        .filter(s -> s.getPosition() == (long) pattern.getStepCycler().get())
                        .findAny().orElseThrow();

                int note = songService.getNoteForStep(step, pattern, tick);

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

}
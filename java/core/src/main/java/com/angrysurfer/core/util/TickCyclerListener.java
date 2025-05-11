package com.angrysurfer.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.sequencer.SongEngine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickCyclerListener implements CyclerListener {

    static Logger logger = LoggerFactory.getLogger(TickCyclerListener.class.getCanonicalName());
    /**
     *
     */
    private Integer ticks = 0;

    private SongEngine songEngine;

    /**
     * @param songEngine
     */
    public TickCyclerListener(SongEngine songEngine) {
        this.songEngine = songEngine;
    }

    @Override
    public void advanced(long tick) {
        if (tick == 0)
            onBeatStart();
        handleTick(tick);
    }

    private void handleTick(long tick) {
        logger.info(String.format("Tick %s", tick));
        songEngine.getActiveSong().getPatterns().forEach(pattern -> {

            logger.info(String.format("Pattern %s", pattern.getPosition()));

            while (pattern.getStepCycler().get() < pattern.getFirstStep())
                pattern.getStepCycler().advance();

            Step step = pattern.getSteps().stream()
                    .filter(s -> s.getPosition() == (long) pattern.getStepCycler().get())
                    .findAny().orElseThrow();

            final int note = songEngine.getNoteForStep(step, pattern, tick);
            if (note > -1) {
                logger.info(String.format("Note: %s", note));

                // // TO DO: Offer a whole-gate option as implented by pushing note on
                // // Pattern.playingNote OR the thread implementation below
                pattern.getPlayingNote().push(note);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //try {
                            //pattern.getInstrument().sendToDevice(
                            //        new ShortMessage(ShortMessage.NOTE_ON, pattern.getChannel(), note, note));
                            //Thread.sleep((long) (1.0 / step.getGate()
                            //        * songEngine.getActiveSong().getBeatDuration()));
                            //pattern.getInstrument().sendToDevice(
                            //        new ShortMessage(ShortMessage.NOTE_OFF, pattern.getChannel(), 0, 0));
                        //} catch (InterruptedException | MidiUnavailableException | InvalidMidiDataException e) {
                        //    logger.error(e.getMessage(), e);
                        //}
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
        logger.info("Tick Cycle complete");
    }

    @Override
    public void starting() {
        logger.info("starting");
        songEngine.getActiveSong().getPatterns().forEach(p -> p.getStepCycler().setLength(p.getSteps().size()));
        songEngine.getActiveSong().getPatterns().forEach(p -> p.getStepCycler().reset());
        this.advanced(0);
    }
}
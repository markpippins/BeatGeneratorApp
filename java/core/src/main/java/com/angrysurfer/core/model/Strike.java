package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Strike extends Player {

    public static int KICK = 36;
    public static int SNARE = 37;
    public static int CLOSED_HAT = 38;
    public static int OPEN_HAT = 39;

    public static List<Integer> razParams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    public static List<Integer> closedHatParams = List.of(24, 25, 26, 27, 28, 29, 30, 31);
    public static List<Integer> kickParams = List.of(1, 2, 3, 4, 12, 13, 14, 15);
    public static List<Integer> snarePrams = List.of(16, 17, 18, 19, 20, 21, 22, 23);

    /**
     * Default constructor
     */
    public Strike() {
        setRules(new HashSet<>()); // Initialize rules set
        setDrumPlayer(true);
    }

    /**
     * Main constructor for Strike with basic parameters
     */
    public Strike(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowedControlMessages) {
        initialize(name, session, instrument, allowedControlMessages);
        setRootNote(note);
        setDrumPlayer(true);
        setDrumPlayer(true);
    }

    /**
     * Extended constructor with velocity parameters
     */
    public Strike(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        initialize(name, session, instrument, allowableControlMessages);
        setRootNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
        setDrumPlayer(true);
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // Get additional timing values from the session
//        Session session = getSession();
//        if (session == null) {
//            System.err.println("Strike.onTick: No session available");
//            return;
//        }

        // Check if we should play based on the current timing
        boolean shouldPlayResult = shouldPlay(timingUpdate);

        if (shouldPlayResult) {
            try {
                int noteToPlay = getRootNote();
                // System.out.println("Strike.onTick playing note: " + noteToPlay);
                drumNoteOn(noteToPlay);
            } catch (Exception e) {
                System.err.println("Error in Strike.onTick: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleSwing() {
        try {
            double offset = getSession().getBeatDuration() * rand.nextLong(getSwing()) * .01;
            try {
                Thread.sleep((long) offset);
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted in handleSwing: {}", e.getMessage());
            }
            logger.debug("Creating swing Ratchet with offset: {}", offset);
            new Ratchet(this, getSession().getTick() + (long) offset, 1, 0);
        } catch (Exception e) {
            logger.error("Error in handleSwing: {}", e.getMessage(), e);
        }
    }

    private void handleRachets() {
        try {
            double numberOfTicksToWait = getRatchetInterval() * (getSession().getTicksPerBeat() / getSubDivisions());
            logger.debug("Creating {} ratchets with interval: {}", getRatchetCount(), numberOfTicksToWait);

            LongStream.range(1, getRatchetCount() + 1).forEach(i -> {
                try {
                    new Ratchet(this, i * numberOfTicksToWait, getRatchetInterval(), 0);
                } catch (Exception e) {
                    logger.error("Error creating Ratchet {}: {}", i, e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error("Error in handleRachets: {}", e.getMessage(), e);
        }
    }

    public Object[] toRow() {
        logger.debug("Converting Strike to row - ID: {}, Name: {}", getId(), getName());
        return new Object[]{
            getName(),
            getChannel(),
            getSwing(),
            getLevel(),
            getRootNote(),
            getMinVelocity(),
            getMaxVelocity(),
            getPreset(),
            getStickyPreset(),
            getProbability(),
            getRandomDegree(),
            getRatchetCount(),
            getRatchetInterval(),
            getUseInternalBeats(),
            getUseInternalBars(),
            getPanPosition(),
            getPreserveOnPurge(),
            getSparse()
        };
    }

}

package com.angrysurfer.core.model;

import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SessionManager;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.stream.LongStream;

@Getter
@Setter
public class Strike extends Player {

    /**
     * Default constructor
     */
    public Strike() {
        setRules(new HashSet<>()); // Initialize rules set
        setDrumPlayer(true);
        setFollowSessionOffset(true);
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
        setFollowSessionOffset(true);
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
        setFollowSessionOffset(true);
    }

    @Override
    public Integer getDefaultChannel() {
        return SequencerConstants.MIDI_DRUM_CHANNEL;
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {

        // Check if we should play based on the current timing
        if (!getFollowRules() || shouldPlay(timingUpdate)) {
            try {
                int noteToPlay = getRootNote();
                // System.out.println("Strike.onTick playing note: " + noteToPlay);(
                if (getFollowSessionOffset())
                    noteToPlay += SessionManager.getInstance().getActiveSession().getNoteOffset();
                noteOn(noteToPlay, getLevel());
            } catch (Exception e) {
                System.err.println("Error in Strike.onTick: " + e.getMessage());
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

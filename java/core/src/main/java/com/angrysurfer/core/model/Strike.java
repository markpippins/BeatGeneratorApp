package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.midi.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Strike extends Player {

    private static final Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

    static final Random rand = new Random();

    public static long KICK = 36;
    public static long SNARE = 37;
    public static long CLOSED_HAT = 38;
    public static long OPEN_HAT = 39;

    public static List<Integer> razParams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    public static List<Integer> closedHatParams = List.of(24, 25, 26, 27, 28, 29, 30, 31);
    public static List<Integer> kickParams = List.of(1, 2, 3, 4, 12, 13, 14, 15);
    public static List<Integer> snarePrams = List.of(16, 17, 18, 19, 20, 21, 22, 23);

    public Strike() {
        setNote(KICK);
        setRules(new HashSet<>()); // Initialize rules set
    }

    public Strike(String name, Session session, Instrument instrument, long note,
            List<Integer> allowedControlMessages) {
        super(name, session, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Session session, Instrument instrument, long note,
            List<Integer> allowableControlMessages, long minVelocity, long maxVelocity) {
        super(name, session, instrument, allowableControlMessages);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    @Override
    public void onTick(long tick, long bar) {
        // logger.info(String.format("Tick: %s", tick));
        if (tick == 1 && getSubDivisions() > 1 && getBeatFraction() > 1) {
            try {
                double numberOfTicksToWait = getBeatFraction() * (getSession().getTicksPerBeat() / getSubDivisions());
                logger.debug("Creating Ratchet with wait ticks: {}", numberOfTicksToWait);
                new Ratchet(this, numberOfTicksToWait, getRatchetInterval(), 0);
                handleRachets();
            } catch (Exception e) {
                logger.error("Error creating Ratchet: {}", e.getMessage(), e);
            }
        }
        else if (getSkipCycler().getLength() == 0 || getSkipCycler().atEnd()) {
            if (getSwing() > 0)
                handleSwing();
            if (getProbability().equals(100L) || rand.nextInt(100) > getProbability())
                drumNoteOn((long) (getNote() + getSession().getNoteOffset()));
        }

        getSkipCycler().advance();
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
        return new Object[] {
                getName(),
                getChannel(),
                getSwing(),
                getLevel(),
                getNote(),
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

    public static Strike fromRow(Object[] row) {
        Strike strike = new Strike();
        strike.setName((String) row[0]);
        // ... existing fromRow code ...
        return strike;
    }
}
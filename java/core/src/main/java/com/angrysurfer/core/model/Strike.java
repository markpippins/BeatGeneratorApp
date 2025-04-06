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

    public Strike() {
        setRootNote(KICK);
        setRules(new HashSet<>()); // Initialize rules set
    }

    public Strike(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowedControlMessages) {
        super(name, session, instrument, allowedControlMessages);
        setRootNote(note);
    }

    public Strike(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        super(name, session, instrument, allowableControlMessages);
        setRootNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // Get additional timing values from the session
        Session session = getSession();
        if (session == null) {
            System.err.println("Strike.onTick: No session available");
            return;
        }
      
        // Check if we should play based on the current timing
        boolean shouldPlayResult = shouldPlay(timingUpdate);
        
        if (shouldPlayResult) {
            try {
                int noteToPlay = getRootNote();
                // System.out.println("Strike.onTick playing note: " + noteToPlay);
                drumNoteOn(noteToPlay);
            } catch(Exception e) {
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
        return new Object[] {
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

    public static Strike fromRow(Object[] row) {
        Strike strike = new Strike();
        strike.setName((String) row[0]);
        // ... existing fromRow code ...
        return strike;
    }

    // Add this debug method
    private void debugTimingValues(Session session) {
        try {
            // System.out.println("RAW CYCLER VALUES FOR: " + getName());
            // System.out.println("  tickCycler: " + session.getTick());
            // System.out.println("  beatCycler: " + session.getBeat());
            // System.out.println("  barCycler: " + session.getBar());
            // System.out.println("  partCycler: " + session.getPart());
        } catch (Exception e) {
            System.err.println("Error debugging cycler values: " + e.getMessage());
        }
    }

    // TEMPORARY TEST METHOD
    // @Override
    // public boolean shouldPlay() {
    //     Session session = getSession();
    //     if (session == null) return false;
        
    //     // Get raw timing values
    //     long tick = session.getTick();
    //     double beat = session.getBeat();
    //     long ticksPerBeat = session.getTicksPerBeat();
        
    //     // Calculate position within the beat (1 to ticksPerBeat)
    //     long tickInBeat = ((tick - 1) % ticksPerBeat) + 1;
        
    //     // System.out.println("SIMPLIFIED TEST - Player " + getName() + ": tickInBeat=" + tickInBeat);
        
    //     // Play on the first tick of each beat
    //     boolean shouldPlayNow = (tickInBeat == 1);
    //     if (shouldPlayNow) {
    //         // System.out.println("SIMPLIFIED TEST - Will play note for player: " + getName());
    //     }
        
    //     return shouldPlayNow;
    // }
}
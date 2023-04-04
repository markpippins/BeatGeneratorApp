package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;

import java.util.*;
import java.util.stream.LongStream;

@Getter
@Setter
@Entity
public class Strike extends Player {

    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

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
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, long note, List<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, long note,
            List<Integer> allowableControlMessages, long minVelocity, long maxVelocity) {
        super(name, ticker, instrument, allowableControlMessages);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    
    @Override
    public void onTick(long tick, long bar) {
  
            if (getBeatFraction() != 1 && getSubCycler().getLength() > 1) {
                try {
                        // beatFraction = beatFraction * getBeatFraction() * getTicker().getTick();
                        // double beatDivider = 1.0 / getTicker().getTicksPerBeat() / getSubCycler().getLength();
                        // double beatFraction = getTicker().getTick() == 1 ? 0 : beatDivider * getBeatFraction() * getTicker().getTick();

                    // double beatFraction = getTicker().getTick() == 1 ? 0 : getTicker().getTicksPerBeat() / getSubCycler().getLength();
                    double beatFraction = getTicker().getTick() == 1 ? 0 : getTicker().getTicksPerBeat() / getSubDivisions();

                    double delay = (getBeatFraction() * beatFraction) - getTicker().getTicksPerBeat();
                    Thread.sleep((long) delay);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        
            if (getSkipCycler().getLength() == 0 || getSkipCycler().atEnd()) {
                // else handleSwing();
                handleRachets();
                long velocity = (long) ((getLevel() * 0.01) * rand.nextLong(getMinVelocity(), getMaxVelocity()));
                drumNoteOn(getNote(), velocity);
            }
    
            // getSkipCycler().advance();
    }
    
    private void handleSwing() {
        double offset = rand.nextDouble(getTicker().getTicksPerBeat() * getSwing() * 0.1);
        new Ratchet(this, getTicker().getTick() + (long) offset, 1, 0);
    }

    private void handleRachets() {
        LongStream.range(1, getRatchetCount() + 1).forEach(i -> {
            double base = getTicker().getTicksPerBeat() / getTicker().getBeatsPerBar(); 
            double offset = getTicker().getTick() + (base * i * getRatchetInterval());
            new Ratchet(this,(long) offset, getRatchetInterval(), 0);
        });
    }
}
// Rule rule = new Rule(Operator.TICK, Comparison.EQUALS, offset, 0);

// Strike ratchet = new Strike(String.format("%s ratchet - %s", getName(), i + 1), getTicker(), 
//         getInstrument(), getNote(), getAllowedControlMessages()) {
//             public void onTick(long tick, long bar) {
//                 drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
//                 getTicker().getPlayers().remove(this);
//             }
            
//         };

// ratchet.setId((long) getTicker().getMaxTracks() + i);
// ratchet.setTicker(getTicker());
// ratchet.getRules().add(rule);

// getTicker().getPlayers().add(ratchet);

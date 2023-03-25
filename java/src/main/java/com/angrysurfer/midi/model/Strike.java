package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;

import jakarta.persistence.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

@Getter
@Setter
@Entity
public class Strike extends Player {
    static final Random rand = new Random();

    public static int KICK = 36;
    public static int SNARE = 37;
    public static int CLOSED_HAT = 38;
    public static int OPEN_HAT = 39;

    public static List<Integer> razParams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    public static List<Integer> closedHatParams = List.of(24, 25, 26, 27, 28, 29, 30, 31);
    public static List<Integer> kickParams = List.of(1, 2, 3, 4, 12, 13, 14, 15);
    public static List<Integer> snarePrams = List.of(16, 17, 18, 19, 20, 21, 22, 23);

    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

    private int ratchetCount = 0;
    
    private double ratchetInterval = 0;

    public Strike() {
    
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note, List<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note,
            List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        super(name, ticker, instrument, allowableControlMessages);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    
    @Override
    public void onTick(long tick, long bar) {
        drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
        handleRachets();
    }
    
    private void handleRachets() {
        IntStream.range(1, getRatchetCount() + 1).forEach(i -> {


            double base = getTicker().getTicksPerBeat() / getTicker().getBeatsPerBar(); 
            double offset = getTicker().getTick() + (base * i * getRatchetInterval());
            Rule rule = new Rule(Operator.TICK, Comparison.EQUALS, offset, 0);

            Strike ratchet = new Strike(String.format("%s ratchet - %s", getName(), i + 1), getTicker(), 
                    getInstrument(), getNote(), getAllowedControlMessages()) {
                        public void onTick(long tick, long bar) {
                            drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
                            getTicker().getPlayers().remove(this);
                        }
                        
                    };
            
            ratchet.setId((long) getTicker().getMaxTracks() + i);
            ratchet.setTicker(getTicker());
            ratchet.getRules().add(rule);
            
            getTicker().getPlayers().add(ratchet);
        });
    }
}

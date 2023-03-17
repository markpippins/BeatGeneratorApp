package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Set;

@Getter
@Setter
@Entity
public class Strike extends Player {
    static final Random rand = new Random();

    public static int KICK = 36;
    public static int SNARE = 37;
    public static int CLOSED_HAT = 38;
    public static int OPEN_HAT = 39;

    public static Set<Integer> razParams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);
    public static Set<Integer> closedHatParams = Set.of(24, 25, 26, 27, 28, 29, 30, 31);
    public static Set<Integer> kickParams = Set.of(1, 2, 3, 4, 12, 13, 14, 15);
    public static Set<Integer> snarePrams = Set.of(16, 17, 18, 19, 20, 21, 22, 23);

    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

//    List<Ratchet> ratchets = new ArrayList<>();

    public Strike() {
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note, Set<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note,
                  Set<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        super(name, ticker, instrument, allowableControlMessages);
        setName(name);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    @Override
    public void onTick(long tick, int bar) {
        drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
        setLastPlayedBar(bar);
        setLastPlayedBeat(getTicker().getBeat());
    }
}

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
    }
}

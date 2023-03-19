package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.List;
import java.util.Objects;
import java.util.Random;
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

    // @Id
    // @GeneratedValue(strategy = GenerationType.SEQUENCE)
    // @Column(name = "id", nullable = false)
    // private Long id;

//    List<Ratchet> ratchets = new ArrayList<>();

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    public void setTicker(Ticker ticker)  {
        this.ticker = ticker;

    }

    public Strike() {
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note, List<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, MidiInstrument instrument, int note,
            List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
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

    @Override
    public Integer getChannel() {
        if (Objects.nonNull(getInstrument()) || getInstrument().isInitialized()) 
            return getInstrument().getChannel(); 
            
        return 9;
    }


    public void setInstrument(MidiInstrument instrument) {
        super.setInstrument(instrument);
    }
}

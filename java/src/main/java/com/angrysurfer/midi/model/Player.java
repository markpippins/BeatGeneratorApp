package com.angrysurfer.midi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
//@Entity
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Player implements Callable<Boolean>, Serializable {
    static final Random rand = new Random();


    @Transient
    private List<Pad> pads = new ArrayList<>();

    private int note;
    private int minVelocity = 110;
    private int maxVelocity = 127;
    private boolean even = true;
    private boolean muted = false;
    private int position = 0;
    private Long lastTick = 0L;
    private int preset;
    private int lastPlayedTick;
    private int lastPlayedBar;
    private Double lastPlayedBeat;
    private String name;
    private String instrumentName;

    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private List<Integer> allowedControlMessages = new ArrayList<>();
    
    @JsonIgnore
    @ManyToOne
	@JoinColumn(name = "instrument_id")
    private MidiInstrument instrument;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @ManyToOne()
    @JoinTable( name = "player_ticker", joinColumns = { @JoinColumn( name = "player_id" ) }, inverseJoinColumns = {
       @JoinColumn( name = "ticker_id")})
	@JoinColumn(name = "ticker_id")
    private Ticker ticker;

    public Player() {

    }

    public Player(String name, Ticker ticker, MidiInstrument instrument) {
        setName(name);
        setInstrument(instrument);
        setInstrumentName(instrument.getName());
    }

    public Player(String name, Ticker ticker, MidiInstrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setInstrumentName(instrument.getName());
        setAllowedControlMessages(allowedControlMessages);
    }
    

    public abstract void onTick(long tick, int bar);

    @Override
    public Boolean call() {
        setEven(getTicker().getTick() % 2 == 0);
        if (getLastTick() == getTicker().getTick())
            return Boolean.FALSE;
        long tick = getTicker().getTick();
        int bar = getTicker().getBar();
        setLastTick(tick);

        if (shouldPlay() &&
                !isMuted() &&
                getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers()
                        .stream().filter(e -> e.getLastPlayedTick() == tick)
                        .toList().size() > 0))
            onTick(tick, bar);

        // setBeat(getBeat() + getTicker().getBeatDivision());
        // if (getBeat() >= getTicker().getBeatsPerBar() +  Constants.DEFAULT_BEAT_OFFSET)
        //     setBeat(Constants.DEFAULT_BEAT_OFFSET);
        return Boolean.TRUE;
    }

    public void drumNoteOn(int note, int velocity) {
        noteOn(note, velocity);
        noteOff(note, velocity);
    }

    public void noteOn(int note, int velocity) {
       try {
            getInstrument().noteOn(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void noteOff(int note, int velocity) {
        try {
            getInstrument().noteOff(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    abstract public Set<Rule> getRules();

    abstract public void setRules(Set<Rule> rules);

   
    public void setInstrument(MidiInstrument instrument) {
        this.instrument = instrument;
        setInstrumentName(instrument.getName());
    }

    @JsonIgnore
    public Integer getChannel() {
        return getInstrument().getChannel();
    }

    @JsonIgnore
    public boolean shouldPlay() {
        AtomicBoolean play = new AtomicBoolean(getRules().size() > 0);
        getRules().forEach(rule -> {
            switch (rule.getOperatorId()) {
                case Operator.TICK -> {
                    if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getTick(), rule.getValue()))
                        play.set(false);
                }
                case Operator.BEAT -> {
                    if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getBeat(), rule.getValue()))
                        play.set(false);
                }
                case Operator.BAR -> {
                    if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getBar(), rule.getValue()))
                        play.set(false);
                }
                case Operator.POSITION -> {
                    if (!Comparison.evaluate(rule.getComparisonId(), getPosition(), rule.getValue()))
                        play.set(false);
                }
            }
        });
        return play.get();
    }
}

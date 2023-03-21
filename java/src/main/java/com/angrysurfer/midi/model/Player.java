package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Player implements Callable<Boolean>, Serializable {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());

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
    private int probability = 100;
    private int lastPlayedTick;
    private int lastPlayedBar;
    private Double lastPlayedBeat;
    private String name;
    private int swing = 50;
    private int level = 100;

    @Transient   
    private Set<Rule> rules = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private List<Integer> allowedControlMessages = new ArrayList<>();
    
    // TODO: replace part with an array of parts

    @ElementCollection
    @CollectionTable(name = "playerParts")
    private List<Integer> playerParts = new ArrayList<>();
    
    @JsonIgnore
    @ManyToOne
	@JoinColumn(name = "instrument_id")
    private MidiInstrument instrument;
    
    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    public Player() {

    }

    public Player(String name, Ticker ticker, MidiInstrument instrument) {
        setName(name);
        setInstrument(instrument);
    }

    public Player(String name, Ticker ticker, MidiInstrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    public void setInstrument(MidiInstrument instrument) {
        this.instrument = instrument;
    }

    public abstract void onTick(long tick, int bar);

    public Long getInstrumentId() {
        return (Objects.nonNull(getInstrument()) ? getInstrument().getId() : null);
    }
    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public int getChannel() {
        return Objects.nonNull(instrument) ? instrument.getChannel() : 0;
    }

    @Override
    public Boolean call() {
        if (getLastTick() == getTicker().getTick())
            return Boolean.FALSE;
        
        long tick = getTicker().getTick();
        int bar = getTicker().getBar();
        setLastTick(tick);
        setEven(tick % 2 == 0);

        if (getTicker().getActivePlayerIds().contains(this.getId()))
            getTicker().getActivePlayerIds().remove(this.getId());
        if (shouldPlay() && !isMuted()) {
            // getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers().stream().filter(e -> e.getLastPlayedTick() == tick)
                // .toList().size() > 0)) {
                    getTicker().getActivePlayerIds().add(this.getId());
                    onTick(tick, bar);
                    setLastPlayedBar(bar);
                    setLastPlayedBeat(getTicker().getBeat());
                }
        // logger.info(String.format("%s not playing tick %s, beat %s, bar %s", getName(), tick, getTicker().getBeat(), getTicker().getBar()));
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

    @JsonIgnore
    public boolean shouldPlay() {
        AtomicBoolean play = new AtomicBoolean(getRules().size() > 0);
        getRules().stream().filter(r -> r.getPart() == 0 || r.getPart() == getTicker().getPart()).forEach(rule -> {
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

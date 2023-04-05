package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Cycler;
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

    private String name;
    private Long swing = 0L;
    private Long level = 100L;
    private Long note = 60L;
    private Long minVelocity = 100L;
    private Long maxVelocity = 110L;
    private Long preset = 1L;
    private Long probability = 100L;
    private Long randomDegree = 0L;
    private Long ratchetCount = 0L;
    private Long ratchetInterval = 0L;
    private Long internalBars;
    private Long internalBeats;
    private Boolean useInternalBeats = false;
    private Boolean useInternalBars = false;
    private Long panPosition = 63L;
    private Boolean preserveOnPurge = false;
    private Set<Long> tickRange = new HashSet<>();
    @JsonIgnore
    @Transient
    private Cycler skipCycler = new Cycler(0);

    @JsonIgnore
    @Transient
    private Cycler subCycler = new Cycler(16);

    @JsonIgnore
    @Transient
    private Cycler beatCycler = new Cycler(16);

    @JsonIgnore
    @Transient
    private Cycler barCycler = new Cycler(16);

    @Transient
    private boolean muted = false;

    @Transient
    private Long position;

    @Transient
    private Long lastTick = 0L;

    @Transient
    private Long lastPlayedTick = 0L;

    @Transient
    private Long lastPlayedBar;

    @Transient
    private Long skips = 0L;

    @Transient
    private double lastPlayedBeat;

    private Long subDivisions = 4L;

    private Long beatFraction = 1L;

    private Long fadeOut = 0L;

    private Long fadeIn = 0L;

    private Boolean accent = false;
    
    @JsonIgnore
    @Transient
    private Boolean armForNextTick = false;

    @Transient   
    private Set<Rule> rules = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private List<Integer> allowedControlMessages = new ArrayList<>();
    
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
        setTicker(ticker);
    }

    public Player(String name, Ticker ticker, MidiInstrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    // public Long getNote() {
    //     Long result = randomDegree == 0L ? note : rand.nextBoolean() ? note + rand.nextLong(randomDegree) : note - rand.nextLong(randomDegree);

    //     if (result > getInstrument().getHighestNote())
    //         result = getInstrument().getLowestNote() + rand.nextLong(randomDegree);

    //     if (result < getInstrument().getLowestNote())
    //         result = getInstrument().getHighestNote() - rand.nextLong(randomDegree);

    //     return (long) (result + getTicker().getNoteOffset());
    // }

    public String getPlayerClass() {
        return getClass().getSimpleName();
    }

    public Long getSubPosition() {
        return getSubCycler().get();
    }

    public void setInstrument(MidiInstrument instrument) {
        this.instrument = instrument;
    }

    public abstract void onTick(long tick, long bar);

    public Long getInstrumentId() {
        return (Objects.nonNull(getInstrument()) ? getInstrument().getId() : null);
    }
    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public int getChannel() {
        return Objects.nonNull(instrument) ? instrument.getChannel() : 0;
    }

    public void drumNoteOn(long note, long velocity) {
        noteOn(note, velocity);
        noteOff(note, velocity);
    }

    public void noteOn(long note, long velocity) {
       try {
            getInstrument().noteOn(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void noteOff(long note, long velocity) {
        try {
            getInstrument().noteOff(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean call() {
            if (getLastTick() == getTicker().getTick())
                return Boolean.FALSE;
            
                //  && !muteGroupPartnerSoundedOnThisTick()
            // && strikeHasNoMuteGroupConflict()
            if (shouldPlay()) {
                            getTicker().getActivePlayerIds().add(getId());
                            setLastPlayedBar(getTicker().getBar());
                            setLastPlayedBeat(getTicker().getBeat());
                            setLastPlayedTick(getTicker().getTick());
                            onTick(getTicker().getTick(), getTicker().getBar());
                        }

            setLastTick(getTicker().getTick());
                // logger.info(String.format("%s not playing tick %s, beat %s, bar %s", getName(), tick, getTicker().getBeat(), getTicker().getBar()));

            return Boolean.TRUE;
    }

    @Transient
    @JsonIgnore
    private boolean isProbable() {
        long test = rand.nextLong(101) ;
        long probable = getProbability();
        boolean result = test < probable;
        return result;
    }

    private boolean strikeHasNoMuteGroupConflict() {
        return getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers()
        .stream().filter(e -> e.getLastPlayedTick() == getTicker().getTick())
            .toList().size() == 0);
    }

    public boolean shouldPlay() {
        logger.info(String.format("ShouldPlay() Tick: %s", getTicker().getTick()));

        List<Rule> applicable = getRules().stream().
            filter(r -> r.getPart() == 0 || ((long) r.getPart()) == getTicker().getPart()).toList();

        AtomicBoolean play = new AtomicBoolean(applicable.size() > 0);
        
        applicable.forEach(rule -> {
                switch (rule.getOperator()) {
                    case Operator.TICK -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getTick(), rule.getValue()))
                            play.set(false);
                    }
                    
                    case Operator.BEAT -> { 
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getBeat(), rule.getValue())) 
                                play.set(false);
                    }

                    case Operator.BEAT_DURATION -> { 
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getBeat(), rule.getValue())) 
                                play.set(false);
                    }

                    case Operator.BAR -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getBar(), rule.getValue()))
                            play.set(false);
                    }
                    
                    case Operator.PART -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getPart(), rule.getValue()))
                            play.set(false);
                    }
                    
                    case Operator.TICK_COUNT -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getTickCounter().get(), 
                            rule.getValue()))
                                play.set(false);
                    }

                    case Operator.BEAT_COUNT -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getBeatCounter().get(), 
                            rule.getValue()))
                                play.set(false);
                    }

                    case Operator.BAR_COUNT -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getBarCounter().get(), 
                            rule.getValue()))
                                play.set(false);
                    }

                    case Operator.PART_COUNT -> {
                        if (!Comparison.evaluate(rule.getComparison(), getTicker().getPartCounter().get(), 
                            rule.getValue()))
                                play.set(false);
                    }
                }
            });

            getSkipCycler().advance();

            getSubCycler().advance();

            return play.get();
    }

}

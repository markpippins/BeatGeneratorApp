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
    private int swing = 50;
    private int level = 100;
    private int note = 60;
    private int minVelocity = 110;
    private int maxVelocity = 127;
    private int preset = 1;
    private int probability = 100;

    @JsonIgnore
    @Transient
    private Cycler subCycler = new Cycler();

    @Transient
    private boolean muted = false;

    @Transient
    private int position = 0;

    @Transient
    private Long lastTick = 0L;

    @Transient
    private Long lastPlayedTick;

    @Transient
    private Long lastPlayedBar;

    @Transient
    private double lastPlayedBeat;

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
    }

    public Player(String name, Ticker ticker, MidiInstrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setAllowedControlMessages(allowedControlMessages);
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

    @Override
    public Boolean call() {
        if (getLastTick() == getTicker().getTick())
            return Boolean.FALSE;
        
            
        if (shouldPlay() && !isMuted() &&
            getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers()
                .stream().filter(e -> e.getLastPlayedTick() == getTicker().getTick())
                    .toList().size() > 0)) {
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
                        double beat = getTicker().getBeat();
                        double beatFraction = getTicker().getTick() == 1 ? 0 : 1.0 / getTicker().getTicksPerBeat() * getSubCycler().get();
                        if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getBeat() + beatFraction, rule.getValue())) 
                            // if (getTicker().getBeatCounter().get() / getTicker().getBeatsPerBar() % getSubCycler().getLength() == 0)
                                play.set(false);
                        getSubCycler().advance();
                    }

                    case Operator.BAR -> {
                        if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getBar(), rule.getValue()))
                            play.set(false);
                    }
                    
                    case Operator.PART -> {
                        if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getPart(), rule.getValue()))
                            play.set(false);
                    }
                    
                    case Operator.ACTUAL_BAR -> {
                        if (!Comparison.evaluate(rule.getComparisonId(), getTicker().getBeatCounter().get() / getTicker().getBeatsPerBar(), 
                            rule.getValue()))
                                play.set(false);
                    }
                }
            });

        return play.get();
    }

}

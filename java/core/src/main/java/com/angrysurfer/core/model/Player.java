package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.service.PlayerExecutor;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
import com.angrysurfer.core.util.Operator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Player implements Callable<Boolean>, Serializable, CommandListener {

    static final Random rand = new Random();

    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());

    private Set<Pad> pads = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private Long instrumentId;

    @JsonIgnore
    @Transient
    public boolean isSelected = false;

    private String name = "Player";
    private int channel = 0;
    private Long swing = 0L;
    private Long level = 100L;
    private Long note = 60L;
    private Long minVelocity = 100L;
    private Long maxVelocity = 110L;
    private Long preset = 1L;
    private Boolean stickyPreset = false;
    private Long probability = 100L;
    private Long randomDegree = 0L;
    private Long ratchetCount = 0L;
    private Long ratchetInterval = 1L;
    private Integer internalBars = Constants.DEFAULT_BAR_COUNT;
    private Integer internalBeats = Constants.DEFAULT_BEATS_PER_BAR;
    private Boolean useInternalBeats = false;
    private Boolean useInternalBars = false;
    private Long panPosition = 63L;
    private Boolean preserveOnPurge = false;
    private double sparse = 0.0;

    @JsonIgnore
    private Cycler skipCycler = new Cycler(0);

    @JsonIgnore
    private Cycler subCycler = new Cycler(16);

    @JsonIgnore
    private Cycler beatCycler = new Cycler(16);

    @JsonIgnore
    private Cycler barCycler = new Cycler(16);

    private boolean solo = false;

    private boolean muted = false;

    private Long position;

    private Long lastTick = 0L;

    private Long lastPlayedTick = 0L;

    private Long lastPlayedBar;

    private Long skips = 0L;

    private double lastPlayedBeat;

    private Long subDivisions = 4L;

    private Long beatFraction = 1L;

    private Long fadeOut = 0L;

    private Long fadeIn = 0L;

    private Boolean accent = false;

    @JsonIgnore
    private boolean unsaved = false;

    @JsonIgnore
    private Boolean armForNextTick = false;

    private Set<Rule> rules = new HashSet<>();

    private List<Integer> allowedControlMessages = new ArrayList<>();

    @JsonIgnore
    private Instrument instrument;

    @JsonIgnore
    private Session session;

    // Add TimingBus
    private final TimingBus timingBus = TimingBus.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();

    public Player() {
        commandBus.register(this);
        timingBus.register(this); // Register for timing events
    }

    public Player(String name, Session session, Instrument instrument) {
        this(); // Call default constructor to ensure registration
        setName(name);
        setInstrument(instrument);
        setSession(session);
    }

    public Player(String name, Session session, Instrument instrument,
            List<Integer> allowedControlMessages) {
        this(name, session, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
        this.instrumentId = Objects.nonNull(instrument) ? instrument.getId() : null;
    }

    public String getPlayerClassName() {
        return getClass().getSimpleName().toLowerCase();
    }

    public Long getSubPosition() {
        return getSubCycler().get();
    }

    public abstract void onTick(long tick, long bar);

    // public Long getInstrumentId() {
    // return (Objects.nonNull(getInstrument()) ? getInstrument().getId() : null);
    // }

    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public void drumNoteOn(long note) {
        logger.debug("drumNoteOn() - note: {}", note);
        long velocity = (long) ((getLevel() * 0.01)
                * rand.nextLong(getMinVelocity() > 0 ? getMinVelocity() : 100,
                        getMaxVelocity() > getMinVelocity() ? getMaxVelocity() : 126));
        
        // Send note on message
        noteOn(note, velocity);
        
        // Schedule note off instead of blocking with Thread.sleep
        final long finalVelocity = velocity;
        
        // Use ScheduledExecutorService for note-off scheduling
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
            () -> {
                try {
                    noteOff(note, finalVelocity);
                } catch (Exception e) {
                    logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
                }
            },
            100, // Shorter note duration (100ms instead of 2500ms)
            java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }

    public void noteOn(long note, long velocity) {
        logger.debug("noteOn() - note: {}, velocity: {}", note, velocity);
        try {
            getInstrument().noteOn(getChannel(), note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Error in noteOn: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void noteOff(long note, long velocity) {
        logger.debug("noteOff() - note: {}, velocity: {}", note, velocity);
        try {
            getInstrument().noteOff(getChannel(), note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Error in noteOff: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean call() {
        // Deprecated - keep for interface compatibility
        return true;
    }

    @JsonIgnore
    public boolean isProbable() {
        long test = rand.nextLong(101);
        long probable = getProbability();
        boolean result = test < probable;
        return result;
    }

    private boolean hasNoMuteGroupConflict() {
        return true;
    }

    private Set<Rule> filterByPart(Set<Rule> rules, boolean includeNoPart) {
        return rules.stream()
                .filter(r -> r.getPart() == 0
                        || (includeNoPart && ((long) r.getPart()) == getSession().getPart()))
                .collect(Collectors.toSet());
    }

    public boolean shouldPlay() {
        logger.debug("shouldPlay() - evaluating rules for player: {}", getName());
        Set<Rule> applicable = filterByPart(getRules(), true);

        AtomicBoolean play = new AtomicBoolean(true);
        AtomicBoolean hasTick = new AtomicBoolean(false);
        AtomicBoolean hasBeat = new AtomicBoolean(false);
        AtomicBoolean hasBar = new AtomicBoolean(false);

        long tick = getSession().getTick();
        long bar = getSession().getBar();
        double beat = getSession().getBeat();
        long fractionLength = getSession().getTicksPerBeat() / getSubDivisions();
        AtomicLong beatFraction = new AtomicLong(0L);
        if (getBeatFraction() > 1)
            LongStream.range(1L, getBeatFraction()).forEach(f -> beatFraction.addAndGet(fractionLength));

        applicable.forEach(rule -> {
            switch (rule.getOperator()) {
                case Comparison.TICK -> {
                    if (Operator.evaluate(rule.getComparison(), tick, rule.getValue()))
                        hasTick.set(true);
                    break;
                }

                case Comparison.BEAT -> {
                    if (Operator.evaluate(rule.getComparison(), beat, rule.getValue()))
                        hasBeat.set(true);
                    break;
                }

                case Comparison.BAR -> {
                    if (Operator.evaluate(rule.getComparison(), bar, rule.getValue()))
                        hasBar.set(true);
                    break;
                }

                case Comparison.BEAT_DURATION -> {
                    if (!Operator.evaluate(rule.getComparison(), beat, rule.getValue()))
                        play.set(false);
                    break;
                }

                case Comparison.TICK_COUNT -> {
                    if (!Operator.evaluate(rule.getComparison(), ((Session) getSession()).getTickCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Comparison.BEAT_COUNT -> {
                    if (!Operator.evaluate(rule.getComparison(), ((Session) getSession()).getBeatCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Comparison.BAR_COUNT -> {
                    if (!Operator.evaluate(rule.getComparison(), ((Session) getSession()).getBarCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Comparison.PART_COUNT -> {
                    if (!Operator.evaluate(rule.getComparison(), ((Session) getSession()).getPartCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }
            }
        });

        getSkipCycler().advance();
        getSubCycler().advance();
        boolean result = (hasTick.get() && hasBeat.get() && hasBar.get() && play.get())
                || (tick == 1 && (hasTick.get() || hasBeat.get()));
        logger.debug("shouldPlay() result: {} for player: {}", result, getName());
        return result;
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.BASIC_TIMING_TICK -> {
                // Only process if our session is running
                if (getSession() != null) {
                    // Check if we should play this tick
                    if (((!getSession().hasSolos() && !isMuted()) || 
                        (getSession().hasSolos() && isSolo())) && shouldPlay()) {
                        
                        // Capture variables for use in lambda
                        final long tick = getSession().getTick();
                        final long bar = getSession().getBar();
                        final double beat = getSession().getBeat();
                        
                        // Submit to thread pool instead of executing directly
                        PlayerExecutor.getInstance().submit(() -> {
                            getSession().getActivePlayerIds().add(getId());
                            setLastPlayedBar(bar);
                            setLastPlayedBeat(beat);
                            setLastPlayedTick(tick);
                            onTick(tick, lastPlayedBar);
                            setLastTick(tick);
                        });
                    }
                }
            }
        }
    }

}

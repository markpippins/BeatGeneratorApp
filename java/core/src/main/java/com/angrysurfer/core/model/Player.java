package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
    private Boolean enabled = false;

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
                java.util.concurrent.TimeUnit.MILLISECONDS);
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

        if (applicable.isEmpty()) {
            logger.debug("No applicable rules for player {}", getName());
            return false;
        }

        // Timing values
        // Long sessionTick = getSession().getTickCounter().get();
        // Long sessionBeat = getSession().getBeatCounter().get();
        // Long sessionBar = getSession().getBarCounter().get();
        // Long sessionPart = ((Session) getSession()).getPartCounter().get();

        Long sessionTick = getSession().getTickCycler().get();
        Long sessionBeat = getSession().getBeatCycler().get();
        Long sessionBar = getSession().getBarCycler().get();
        Long sessionPart = getSession().getPartCycler().get();

        return shouldPlay(applicable, sessionTick, sessionBeat, sessionBar, sessionPart);
    }

    public boolean shouldPlay(Set<Rule> applicable, Long sessionTick, Long sessionBeat, Long sessionBar,
            Long sessionPart) {

        long tick = getSession().getTick();
        long bar = getSession().getBar();
        double beat = getSession().getBeat();

        // Group rules by operator type
        Map<Integer, List<Rule>> rulesByType = applicable.stream()
                .collect(Collectors.groupingBy(Rule::getOperator));

        // Check position rules (TICK, BEAT, BAR) - ALL matching types must have at
        // least one match
        boolean hasTickRules = rulesByType.containsKey(Comparison.TICK) && !rulesByType.get(Comparison.TICK).isEmpty();
        boolean hasBeatRules = rulesByType.containsKey(Comparison.BEAT) && !rulesByType.get(Comparison.BEAT).isEmpty();
        boolean hasBarRules = rulesByType.containsKey(Comparison.BAR) && !rulesByType.get(Comparison.BAR).isEmpty();
        boolean hasPartRules = rulesByType.containsKey(Comparison.PART) && !rulesByType.get(Comparison.PART).isEmpty();

        // Keep track of which rule types matched
        boolean tickRulesMatched = false;
        boolean beatRulesMatched = false;
        boolean barRulesMatched = false;
        boolean partRulesMatched = false;

        // Check TICK rules
        if (hasTickRules) {
            for (Rule rule : rulesByType.get(Comparison.TICK)) {
                if (Operator.evaluate(rule.getComparison(), tick, rule.getValue())) {
                    logger.debug("TICK rule matched: {} {} {}",
                            rule.getOperatorText(), rule.getComparisonText(), rule.getValue());
                    tickRulesMatched = true;
                    break; // One matching rule is enough for this type
                }
            }

            if (!tickRulesMatched) {
                logger.debug("TICK rules present but none matched");
                return false; // If we have tick rules but none matched, don't play
            }
        } else {
            // NEW CONDITION: If no tick rules are specified, only play when tick = 1
            if (tick != 1) {
                logger.debug("No TICK rules present and current tick is not 1 (current tick: {})", tick);
                return false;
            } else {
                logger.debug("No TICK rules present but current tick is 1, continuing evaluation");
                // Mark as matched since we're on tick 1
                tickRulesMatched = true;
            }
        }

        // Rest of the method remains the same...

        // Check BEAT rules
        if (hasBeatRules) {
            for (Rule rule : rulesByType.get(Comparison.BEAT)) {
                if (Operator.evaluate(rule.getComparison(), beat, rule.getValue())) {
                    logger.debug("BEAT rule matched: {} {} {}",
                            rule.getOperatorText(), rule.getComparisonText(), rule.getValue());
                    beatRulesMatched = true;
                    break; // One matching rule is enough for this type
                }
            }

            if (!beatRulesMatched) {
                logger.debug("BEAT rules present but none matched");
                return false; // If we have beat rules but none matched, don't play
            }
        }

        // Check BAR rules
        if (hasBarRules) {
            for (Rule rule : rulesByType.get(Comparison.BAR)) {
                if (Operator.evaluate(rule.getComparison(), bar, rule.getValue())) {
                    logger.debug("BAR rule matched: {} {} {}",
                            rule.getOperatorText(), rule.getComparisonText(), rule.getValue());
                    barRulesMatched = true;
                    break; // One matching rule is enough for this type
                }
            }

            if (!barRulesMatched) {
                logger.debug("BAR rules present but none matched");
                return false; // If we have bar rules but none matched, don't play
            }
        }

        // Check PART rules
        if (hasPartRules) {
            for (Rule rule : rulesByType.get(Comparison.PART)) {
                // Special case for Part=0 (All parts) - always matches
                if (rule.getValue() == 0 ||
                        Operator.evaluate(rule.getComparison(), sessionPart, rule.getValue())) {
                    logger.debug("PART rule matched: {} {} {}",
                            rule.getOperatorText(), rule.getComparisonText(), rule.getValue());
                    partRulesMatched = true;
                    break; // One matching rule is enough for this type
                }
            }

            if (!partRulesMatched) {
                logger.debug("PART rules present but none matched");
                return false; // If we have part rules but none matched, don't play
            }
        }

        // If we have no position rules at all, don't play
        if (!hasTickRules && !hasBeatRules && !hasBarRules && !hasPartRules) {
            logger.debug("No position rules defined");
            return false;
        }

        // Check "constraint rules" (can only prevent playing)
        // Check BEAT_DURATION rules
        for (Rule rule : rulesByType.getOrDefault(Comparison.BEAT_DURATION, List.of())) {
            if (!Operator.evaluate(rule.getComparison(), beat, rule.getValue())) {
                logger.debug("BEAT_DURATION constraint not met: {} {} {}",
                        rule.getOperatorText(), rule.getComparisonText(), rule.getValue());
                return false;
            }
        }

        // Additional constraint checks (unchanged)
        for (Rule rule : rulesByType.getOrDefault(Comparison.TICK_COUNT, List.of())) {
            if (!Operator.evaluate(rule.getComparison(), sessionTick, rule.getValue())) {
                logger.debug("TICK_COUNT constraint not met");
                return false;
            }
        }

        for (Rule rule : rulesByType.getOrDefault(Comparison.BEAT_COUNT, List.of())) {
            if (!Operator.evaluate(rule.getComparison(), sessionBeat, rule.getValue())) {
                logger.debug("BEAT_COUNT constraint not met");
                return false;
            }
        }

        for (Rule rule : rulesByType.getOrDefault(Comparison.BAR_COUNT, List.of())) {
            if (!Operator.evaluate(rule.getComparison(), sessionBar, rule.getValue())) {
                logger.debug("BAR_COUNT constraint not met");
                return false;
            }
        }

        for (Rule rule : rulesByType.getOrDefault(Comparison.PART_COUNT, List.of())) {
            if (!Operator.evaluate(rule.getComparison(), sessionPart, rule.getValue())) {
                logger.debug("PART_COUNT constraint not met");
                return false;
            }
        }

        // Consider sparse value for randomization
        if (getSparse() > 0 && rand.nextDouble() < getSparse()) {
            logger.debug("Note skipped due to sparse value: {}", getSparse());
            return false;
        }

        // Advance cyclers for next time
        getSkipCycler().advance();
        getSubCycler().advance();

        // All position rules matched, and no constraint rules prevented playing
        logger.debug("All rules matched - player will play");
        return true;
    }

    @Override
    public void onAction(Command action) {
        if (getSession() != null && getEnabled())
            switch (action.getCommand()) {
                case Commands.BASIC_TIMING_TICK -> {
                    // Only process if our session is running
                    // Check if we should play this tick
                    if (((!getSession().hasSolos() && !isMuted()) ||
                            (isSolo())) && shouldPlay()) {

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
                case Commands.TRANSPORT_STOP -> {
                    // Disable self when transport stops
                    setEnabled(false);

                    // Optional: Log this action if needed
                    if (logger != null) {
                        logger.debug("Player " + getName() + " (ID: " + getId() + ") disabled due to transport stop");
                    }
                }

                case Commands.TRANSPORT_PLAY -> {
                    // Optionally re-enable self on transport start/play
                    setEnabled(true);

                    // Optional: Log this action if needed
                    if (logger != null) {
                        logger.debug("Player " + getName() + " (ID: " + getId() + ") enabled due to transport start");
                    }
                }
            }
    }

    /**
     * Clean up resources when this player is no longer needed
     */
    public void dispose() {
        // Unregister from command bus to prevent memory leaks
        commandBus.unregister(this);

        // Any other cleanup code...
    }

}

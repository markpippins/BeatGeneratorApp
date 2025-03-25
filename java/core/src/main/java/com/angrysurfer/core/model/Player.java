package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.management.OperationsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.midi.Instrument;
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
public abstract class Player implements Callable<Boolean>, Serializable, IBusListener {

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
        // Register with command and timing buses
        commandBus.register(this);
        timingBus.register(this);
        System.out.println("Player constructor: Registered with buses");
    }

    public Player(String name, Session session, Instrument instrument) {
        this(); // Call default constructor to ensure registration
        setName(name);
        setInstrument(instrument);
        setSession(session);
    }

    public Player(String name, Session session, Instrument instrument, List<Integer> allowedControlMessages) {
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

    // public Long getSubPosition() {
    // return getSub();
    // }

    public abstract void onTick(long tick, long bar);
    // {
    // System.out.println("CRITICAL DEBUG - Player: " + getName()
    // + " - BYPASSING ALL RULE CHECKS AND PLAYING UNCONDITIONALLY");
    // // ... rest of the method ...
    // }

    // public Long getInstrumentId() {
    // return (Objects.nonNull(getInstrument()) ? getInstrument().getId() : null);
    // }

    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public void drumNoteOn(long note) {
        logger.debug("drumNoteOn() - note: {}", note);
        // long velocity = (long) ((getLevel() * 0.01)
        // * rand.nextLong(getMinVelocity() > 0 ? getMinVelocity() : 100,
        // getMaxVelocity() > getMinVelocity() ? getMaxVelocity() : 126));

        long velocity = getMaxVelocity();
        // Send note on message

        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                noteOn(note, velocity);
            } catch (Exception e) {
                logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
            }
        }, 0, // Shorter note duration (100ms instead of 2500ms)
                java.util.concurrent.TimeUnit.MILLISECONDS);

        // Schedule note off instead of blocking with Thread.sleep
        final long finalVelocity = velocity;

        // Use ScheduledExecutorService for note-off scheduling
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                noteOff(note, finalVelocity);
            } catch (Exception e) {
                logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
            }
        }, 200, // Shorter note duration (100ms instead of 2500ms)
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void noteOn(long note, long velocity) {
        logger.debug("noteOn() - note: {}, velocity: {}", note, velocity);

        long fixedVelocity = velocity < 126 ? velocity : 126;

        try {
            // Set playing state to true
            setPlaying(true);

            // Schedule UI refresh after a short delay
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this), 50, // 50ms delay
                    java.util.concurrent.TimeUnit.MILLISECONDS);

            getInstrument().noteOn(getChannel(), note, fixedVelocity);
        } catch (Exception e) {
            logger.error("Error in noteOn: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void noteOff(long note, long velocity) {
        logger.debug("noteOff() - note: {}, velocity: {}", note, velocity);
        try {
            getInstrument().noteOff(getChannel(), note, velocity);

            // Set playing state to false
            setPlaying(false);

            // Schedule UI refresh after a short delay
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this), 50, // 50ms delay
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
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
        if (rules == null || session == null)
            return new HashSet<>();

        long currentPart = session.getPart();

        return rules.stream().filter(r -> {
            int rulePart = r.getPart();
            return rulePart == 0 || (includeNoPart && rulePart == currentPart);
        }).collect(Collectors.toSet());
    }

    public boolean shouldPlay() {
        // Skip processing if there are no rules or no session
        if (rules == null || rules.isEmpty() || session == null) {
            return false;
        }

        // Get current timing values from session
        long tick = session.getTick();
        long bar = session.getBar();
        double beat = session.getBeat();
        long part = session.getPart();

        // Filter rules to only those applicable to the current part
        Set<Rule> applicable = filterByPart(rules, true);
        if (applicable.isEmpty()) {
            return false;
        }

        // logger.debug("Player {} evaluating rules at tick={}, beat={}, bar={},
        // part={}",
        // getName(), tick, beat, bar, part);

        // Categorize rules by operator type
        Map<Integer, List<Rule>> rulesByType = applicable.stream().collect(Collectors.groupingBy(Rule::getOperator));

        // Check if we have rules for each timing type
        boolean hasTickRules = rulesByType.containsKey(Comparison.TICK) && !rulesByType.get(Comparison.TICK).isEmpty();
        boolean hasBeatRules = rulesByType.containsKey(Comparison.BEAT) && !rulesByType.get(Comparison.BEAT).isEmpty();
        boolean hasBarRules = rulesByType.containsKey(Comparison.BAR) && !rulesByType.get(Comparison.BAR).isEmpty();
        boolean hasPartRules = rulesByType.containsKey(Comparison.PART) && !rulesByType.get(Comparison.PART).isEmpty();

        // If we only have constraint rules (bar/part) without triggering rules
        // (beat/tick), don't play
        if ((hasBarRules || hasPartRules) && !hasBeatRules && !hasTickRules) {
            // logger.debug("Player {} has only constraint rules, no trigger rules - won't
            // play", getName());
            return false;
        }

        // CRITICAL FIX: If we have beat rules but no tick rules, only play on first
        // tick of beat
        if (!hasTickRules && hasBeatRules && (tick % session.getTicksPerBeat()) != 1) {
            // logger.debug("Player {} has beat rules but this isn't first tick of beat -
            // won't play", getName());
            return false;
        }

        // Evaluate triggering rules (tick and beat)
        boolean tickMatched = !hasTickRules; // Default to true if no tick rules
        boolean beatMatched = !hasBeatRules; // Default to true if no beat rules

        // Check tick rules
        if (hasTickRules) {
            for (Rule rule : rulesByType.get(Comparison.TICK)) {
                if (Operator.evaluate(rule.getComparison(), tick, rule.getValue())) {
                    tickMatched = true;
                    logger.debug("Player {} matched tick rule: {}", getName(), rule);
                    break;
                }
            }
        }

        // Check beat rules
        if (hasBeatRules) {
            for (Rule rule : rulesByType.get(Comparison.BEAT)) {
                if (Operator.evaluate(rule.getComparison(), beat, rule.getValue())) {
                    beatMatched = true;
                    logger.debug("Player {} matched beat rule: {}", getName(), rule);
                    break;
                }
            }
        }

        // If either required trigger rule type didn't match, don't play
        if (!tickMatched || !beatMatched) {
            logger.debug("Player {} trigger rules didn't match - won't play", getName());
            return false;
        }

        // Now check constraint rules (bar and part)
        boolean barMatched = !hasBarRules; // Default to true if no bar rules
        boolean partMatched = !hasPartRules; // Default to true if no part rules

        // Check bar rules
        if (hasBarRules) {
            for (Rule rule : rulesByType.get(Comparison.BAR)) {
                if (Operator.evaluate(rule.getComparison(), bar, rule.getValue())) {
                    barMatched = true;
                    logger.debug("Player {} matched bar rule: {}", getName(), rule);
                    break;
                }
            }
        }

        // Check part rules
        if (hasPartRules) {
            for (Rule rule : rulesByType.get(Comparison.PART)) {
                if (Operator.evaluate(rule.getComparison(), part, rule.getValue())) {
                    partMatched = true;
                    logger.debug("Player {} matched part rule: {}", getName(), rule);
                    break;
                }
            }
        }

        // Both constraints must match if present
        if (!barMatched || !partMatched) {
            logger.debug("Player {} constraint rules didn't match - won't play", getName());
            return false;
        }

        // Update player state for next evaluation
        getSkipCycler().advance();
        getSubCycler().advance();

        logger.debug("Player {} - ALL RULES PASSED - will play", getName());
        return true;
    }

    // private Set<Rule> filterByPart(Set<Rule> rules, boolean includeNoPart) {
    // return rules.stream()
    // .filter(r -> r.getPart() == 0 || (includeNoPart && ((long) r.getPart()) ==
    // getSession().getPart()))
    // .collect(Collectors.toSet());
    // }

    // public boolean shouldPlay() {

    // Set<Rule> applicable = filterByPart(getRules(), true);

    // AtomicBoolean play = new AtomicBoolean(true);
    // AtomicBoolean hasTick = new AtomicBoolean(false);
    // AtomicBoolean hasBeat = new AtomicBoolean(false);
    // AtomicBoolean hasBar = new AtomicBoolean(false);

    // long tick = getSession().getTick();
    // double beat = getSession().getBeat();
    // long bar = getSession().getBar();

    // // long fractionLength = getTicker().getTicksPerBeat() / getSubDivisions();
    // // AtomicLong beatFraction = new AtomicLong(0L);
    // // if (getBeatFraction() > 1)
    // // LongStream.range(1L, getBeatFraction()).forEach(f ->
    // // beatFraction.addAndGet(fractionLength));

    // applicable.forEach(rule -> {
    // switch (rule.getComparison()) {
    // case Comparison.TICK -> {
    // if (Operator.evaluate(rule.getComparison(), tick, rule.getValue()))
    // hasTick.set(true);
    // // logger.info(String.format("HasTick: %s", hasTick.get()));
    // break;
    // }

    // case Comparison.BEAT -> {
    // if (Operator.evaluate(rule.getComparison(), beat, rule.getValue()))
    // hasBeat.set(true);
    // // logger.info(String.format("HasBeat: %s", hasBeat.get()));
    // break;
    // }

    // case Comparison.BAR -> {
    // if (Operator.evaluate(rule.getComparison(), bar, rule.getValue()))
    // hasBar.set(true);
    // // logger.info(String.format("HasBar: %s", hasBar.get()));
    // // play.set(false);
    // break;
    // }

    // // case Operator.PART -> {
    // // if (!Comparison.evaluate(rule.getComparison(), getTicker().getPart(),
    // // rule.getValue()))
    // // play.set(false);
    // // }

    // case Comparison.BEAT_DURATION -> {
    // if (!Operator.evaluate(rule.getComparison(), beat, rule.getValue()))
    // play.set(false);
    // break;
    // }

    // case Comparison.TICK_COUNT -> {
    // if (!Operator.evaluate(rule.getComparison(), getSession().getTickCount(),
    // rule.getValue()))
    // play.set(false);
    // }

    // case Comparison.BEAT_COUNT -> {
    // if (!Operator.evaluate(rule.getComparison(), getSession().getBeatCount(),
    // rule.getValue()))
    // play.set(false);
    // }

    // case Comparison.BAR_COUNT -> {
    // if (!Operator.evaluate(rule.getComparison(), getSession().getBarCount(),
    // rule.getValue()))
    // play.set(false);
    // }

    // case Comparison.PART_COUNT -> {
    // if (!Operator.evaluate(rule.getComparison(), getSession().getPartCount(),
    // rule.getValue()))
    // play.set(false);
    // }
    // }
    // });

    // getSkipCycler().advance();
    // getSubCycler().advance();
    // boolean result = (hasTick.get() && hasBeat.get() && hasBar.get() &&
    // play.get())
    // || (tick == 1 && (hasTick.get() || hasBeat.get()));
    // // logger.info(String.format("Returning: %s", result));

    // return result;
    // }

    /**
     * Determines whether this player would play at the specified position based on
     * its rules. This method is used by visualizations to predict when players will
     * trigger.
     * 
     * @param rules The set of rules to evaluate
     * @param tick  The tick position
     * @param beat  The beat position
     * @param bar   The bar position
     * @param part  The part position
     * @return true if player would play at this position, false otherwise
     */
    public boolean shouldPlayAt(Set<Rule> rules, int tick, int beat, int bar, int part) {
        // Skip processing if there are no rules
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        // Filter rules to only those applicable to the given part
        Set<Rule> applicable = rules.stream().filter(r -> r.getPart() == 0 || r.getPart() == part)
                .collect(Collectors.toSet());

        if (applicable.isEmpty()) {
            return false;
        }

        // Categorize rules by operator type
        Map<Integer, List<Rule>> rulesByType = applicable.stream().collect(Collectors.groupingBy(Rule::getOperator));

        // Check if we have rules for each timing type
        boolean hasTickRules = rulesByType.containsKey(Comparison.TICK) && !rulesByType.get(Comparison.TICK).isEmpty();
        boolean hasBeatRules = rulesByType.containsKey(Comparison.BEAT) && !rulesByType.get(Comparison.BEAT).isEmpty();
        boolean hasBarRules = rulesByType.containsKey(Comparison.BAR) && !rulesByType.get(Comparison.BAR).isEmpty();
        boolean hasPartRules = rulesByType.containsKey(Comparison.PART) && !rulesByType.get(Comparison.PART).isEmpty();

        // If we have no trigger rules, don't play
        if (!hasBeatRules && !hasTickRules) {
            return false;
        }

        // Evaluate triggering rules (tick and beat)
        boolean tickMatched = !hasTickRules; // Default to true if no tick rules
        boolean beatMatched = !hasBeatRules; // Default to true if no beat rules

        // Check tick rules
        if (hasTickRules) {
            for (Rule rule : rulesByType.get(Comparison.TICK)) {
                if (Operator.evaluate(rule.getComparison(), tick, rule.getValue())) {
                    tickMatched = true;
                    break;
                }
            }
        }

        // Check beat rules
        if (hasBeatRules) {
            for (Rule rule : rulesByType.get(Comparison.BEAT)) {
                if (Operator.evaluate(rule.getComparison(), beat, rule.getValue())) {
                    beatMatched = true;
                    break;
                }
            }
        }

        // If either required trigger rule type didn't match, don't play
        if (!tickMatched || !beatMatched) {
            return false;
        }

        // Now check constraint rules (bar and part)
        boolean barMatched = !hasBarRules; // Default to true if no bar rules
        boolean partMatched = !hasPartRules; // Default to true if no part rules

        // Check bar rules
        if (hasBarRules) {
            for (Rule rule : rulesByType.get(Comparison.BAR)) {
                if (Operator.evaluate(rule.getComparison(), bar, rule.getValue())) {
                    barMatched = true;
                    break;
                }
            }
        }

        // Check part rules
        if (hasPartRules) {
            for (Rule rule : rulesByType.get(Comparison.PART)) {
                if (Operator.evaluate(rule.getComparison(), part, rule.getValue())) {
                    partMatched = true;
                    break;
                }
            }
        }

        // Both constraints must match if present
        return barMatched && partMatched;
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        String cmd = action.getCommand();

        System.out.println("Player " + getName() + " received command: " + cmd);

        if (getSession() != null && getEnabled()) {
            switch (cmd) {
            case Commands.TIME_TICK -> {
                if (!isRunning()) {
                    System.out.println("Player " + getName() + " - Skipping tick (not running)");
                    return;
                }

                System.out.println("Player " + getName() + " processing tick");

                // FIXED: Get current timing values directly from session
                // instead of from the event data
                Session session = getSession();
                long tick = session.getTick();
                double beat = session.getBeat();
                long bar = session.getBar();
                long part = session.getPart();

                // Debug the actual values from the cyclers
                System.out.println("Player " + getName() + " - TIME VALUES FROM SESSION CYCLERS:");
                System.out.println("  - Tick: " + tick + " (cycler position: " + session.getTick() + ")");
                System.out.println("  - Beat: " + beat + " (cycler position: " + session.getBeat() + ")");
                System.out.println("  - Bar: " + bar + " (cycler position: " + session.getBar() + ")");
                System.out.println("  - Part: " + part + " (cycler position: " + session.getPart() + ")");

                // Call the specific player implementation with values from session
                if (!isMuted())
                    onTick(tick, bar);
            }
            case Commands.TRANSPORT_STOP -> {
                // Disable self when transport stops
                setEnabled(false);
            }
            case Commands.TRANSPORT_PLAY -> {
                // Re-enable self on transport start/play
                setEnabled(true);
            }
            }
        }
    }

    /**
     * Determines if the player should be processing timing events
     */
    public boolean isRunning() {
        return enabled && session != null && session.isRunning();
    }

    /**
     * Clean up resources when this player is no longer needed
     */
    public void dispose() {
        // Unregister from command bus to prevent memory leaks
        commandBus.unregister(this);

        // Any other cleanup code...
    }

    // Add this property to the Player class
    @JsonIgnore
    @Transient
    private boolean isPlaying = false;

    // Add getter/setter
    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

}

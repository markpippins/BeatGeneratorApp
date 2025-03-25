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

    public Long getSubPosition() {
        return getSubCycler().get();
    }

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
        return rules.stream()
                .filter(r -> r.getPart() == 0 || (includeNoPart && ((long) r.getPart()) == getSession().getPart()))
                .collect(Collectors.toSet());
    }

    public boolean shouldPlay() {
        // Get all the values from the session
        long tick = getSession().getTick();
        double beat = getSession().getBeat();
        long bar = getSession().getBar();
        long part = getSession().getPart();
        long tickCount = getSession().getTickCount();
        long beatCount = getSession().getBeatCount();
        long barCount = getSession().getBarCount();
        long partCount = getSession().getPartCount();

        logger.debug("Player {} checking shouldPlay - tick: {}, beat: {}, bar: {}, part: {}", getName(), tick, beat,
                bar, part);

        // Get applicable rules
        Set<Rule> applicable = getRules();
        if (applicable == null || applicable.isEmpty()) {
            logger.debug("Player {} has no rules", getName());
            return false;
        }

        logger.debug("Player {} has {} rules to evaluate", getName(), applicable.size());

        // Use the overloaded method that considers all session values
        return shouldPlay(applicable, tick, beat, bar, part, tickCount, beatCount, barCount, partCount);
    }

    public boolean shouldPlay(Set<Rule> applicable, Long sessionTick, Double sessionBeat, Long sessionBar,
            Long sessionPart, Long sessionTickCount, Long sessionBeatCount, Long sessionBarCount,
            Long sessionPartCount) {

        if (applicable == null || applicable.isEmpty()) {
            logger.debug("Player {} has no rules", getName());
            return false;
        }

        System.out.println("Player " + getName() + " - Evaluating rules - tick: " + sessionTick + ", beat: "
                + sessionBeat + ", bar: " + sessionBar + ", part: " + sessionPart);

        // Key fix: Convert long to int for correct comparison
        int currentPartInt = sessionPart.intValue();

        // First, separate rules by part
        Map<Integer, List<Rule>> rulesByPart = applicable.stream().collect(Collectors.groupingBy(r -> r.getPart()));

        System.out.println("Player " + getName() + " - Rules by part: " + rulesByPart.keySet());

        // Get rules for current part and part 0 (all parts)
        List<Rule> currentPartRules = rulesByPart.getOrDefault(currentPartInt, new ArrayList<>());
        List<Rule> allPartRules = rulesByPart.getOrDefault(0, new ArrayList<>()); // Use 0, not 0L

        System.out.println("Player " + getName() + " - Current part rules: " + currentPartRules.size()
                + ", Part 0 rules: " + allPartRules.size());

        // Combine rules from current part and part 0
        List<Rule> effectiveRules = new ArrayList<>();
        effectiveRules.addAll(currentPartRules);
        effectiveRules.addAll(allPartRules);

        if (effectiveRules.isEmpty()) {
            System.out.println("Player " + getName() + " - No effective rules found for part " + sessionPart);
            return false;
        }

        // Group effective rules by type
        Map<Integer, List<Rule>> rulesByType = effectiveRules.stream()
                .collect(Collectors.groupingBy(r -> r.getOperator()));

        // Get rules for each timing type
        List<Rule> tickRules = rulesByType.getOrDefault(Comparison.TICK, new ArrayList<>());
        List<Rule> beatRules = rulesByType.getOrDefault(Comparison.BEAT, new ArrayList<>());
        List<Rule> barRules = rulesByType.getOrDefault(Comparison.BAR, new ArrayList<>());

        // Calculate timing values
        int ticksPerBeat = getSession().getTicksPerBeat();
        int beatsPerBar = getSession().getBeatsPerBar();

        // Calculate absolute position within the bar
        long tickInBar = ((sessionTick - 1) % (ticksPerBeat * beatsPerBar)) + 1;

        // Calculate which beat we're in (1-based)
        long currentBeat = ((tickInBar - 1) / ticksPerBeat) + 1;

        // Calculate position within current beat (1-based)
        long tickInBeat = ((tickInBar - 1) % ticksPerBeat) + 1;

        logger.debug("Player {} timing calcs - tickInBar: {}, currentBeat: {}, tickInBeat: {}", getName(), tickInBar,
                currentBeat, tickInBeat);

        // First check beat rules since they're more restrictive
        boolean beatMatched = false;
        if (!beatRules.isEmpty()) {
            beatMatched = beatRules.stream()
                    .anyMatch(rule -> Operator.evaluate(rule.getComparison(), currentBeat, rule.getValue()));

            if (!beatMatched) {
                logger.debug("Player {} - beat rules exist but none matched for beat {}", getName(), currentBeat);
                return false;
            }
        }

        // Then check tick rules
        boolean tickMatched = false;
        if (!tickRules.isEmpty()) {
            tickMatched = tickRules.stream()
                    .anyMatch(rule -> Operator.evaluate(rule.getComparison(), tickInBeat, rule.getValue()));

            if (!tickMatched) {
                logger.debug("Player {} - tick rules exist but none matched for tick {} in beat", getName(),
                        tickInBeat);
                return false;
            }
        } else if (beatMatched) {
            // If we have beat rules but no tick rules, only play on first tick of matching
            // beats
            if (tickInBeat != 1) {
                logger.debug("Player {} - has beat rules but no tick rules, and not on first tick of beat", getName());
                return false;
            }
            tickMatched = true;
        }

        // Must have at least one triggering rule match
        if (!tickMatched && !beatMatched) {
            logger.debug("Player {} - no tick or beat triggers matched", getName());
            return false;
        }

        // Now check constraining rules (bar, count rules, etc.)

        // Check bar rules (constraints)
        if (!barRules.isEmpty() && !barRules.stream()
                .anyMatch(rule -> Operator.evaluate(rule.getComparison(), sessionBar, rule.getValue()))) {
            logger.debug("Player {} - bar constraints not met for bar {}", getName(), sessionBar);
            return false;
        }

        // Check count-based constraints
        // TICK_COUNT
        List<Rule> tickCountRules = rulesByType.getOrDefault(Comparison.TICK_COUNT, new ArrayList<>());
        if (!tickCountRules.isEmpty() && !tickCountRules.stream()
                .anyMatch(rule -> Operator.evaluate(rule.getComparison(), sessionTickCount, rule.getValue()))) {
            logger.debug("Player {} - tick count constraints not met", getName());
            return false;
        }

        // BEAT_COUNT
        List<Rule> beatCountRules = rulesByType.getOrDefault(Comparison.BEAT_COUNT, new ArrayList<>());
        if (!beatCountRules.isEmpty() && !beatCountRules.stream()
                .anyMatch(rule -> Operator.evaluate(rule.getComparison(), sessionBeatCount, rule.getValue()))) {
            logger.debug("Player {} - beat count constraints not met", getName());
            return false;
        }

        // BAR_COUNT
        List<Rule> barCountRules = rulesByType.getOrDefault(Comparison.BAR_COUNT, new ArrayList<>());
        if (!barCountRules.isEmpty() && !barCountRules.stream()
                .anyMatch(rule -> Operator.evaluate(rule.getComparison(), sessionBarCount, rule.getValue()))) {
            logger.debug("Player {} - bar count constraints not met", getName());
            return false;
        }

        // PART_COUNT
        List<Rule> partCountRules = rulesByType.getOrDefault(Comparison.PART_COUNT, new ArrayList<>());
        if (!partCountRules.isEmpty() && !partCountRules.stream()
                .anyMatch(rule -> Operator.evaluate(rule.getComparison(), sessionPartCount, rule.getValue()))) {
            logger.debug("Player {} - part count constraints not met", getName());
            return false;
        }

        // Consider sparse value for randomization
        if (getSparse() > 0 && rand.nextDouble() < getSparse()) {
            logger.debug("Player {} - note skipped due to sparse value: {}", getName(), getSparse());
            return false;
        }

        // All rules passed
        logger.debug("Player {} - all rules passed, will play", getName());
        return true;
    }

    /**
     * Simplified rule evaluation that properly handles the first-tick-only case for
     * players that have beat/bar rules but no tick rules
     */
    public boolean shouldPlayAt(Set<Rule> applicable, long tick, double beat, long bar, long part) {
        if (applicable == null || applicable.isEmpty()) {
            return false;
        }

        // Group rules by operator type for easier processing
        Map<Integer, List<Rule>> rulesByType = applicable.stream().collect(Collectors.groupingBy(Rule::getOperator));

        // Check if we have rules for each timing type
        boolean hasTickRules = rulesByType.containsKey(Comparison.TICK) && !rulesByType.get(Comparison.TICK).isEmpty();
        boolean hasBeatRules = rulesByType.containsKey(Comparison.BEAT) && !rulesByType.get(Comparison.BEAT).isEmpty();
        boolean hasBarRules = rulesByType.containsKey(Comparison.BAR) && !rulesByType.get(Comparison.BAR).isEmpty();

        // CRITICAL FIX: If no tick rules but we have beat or bar rules,
        // then ONLY play on the first tick of the beat (tick == 1)
        if (!hasTickRules && (hasBeatRules || hasBarRules) && tick != 1) {
            return false;
        }

        // Process tick rules (if any)
        if (hasTickRules) {
            boolean tickMatched = false;
            for (Rule rule : rulesByType.get(Comparison.TICK)) {
                if (Operator.evaluate(rule.getComparison(), tick, rule.getValue())) {
                    tickMatched = true;
                    break;
                }
            }

            if (!tickMatched)
                return false; // No tick rule matched
        }

        // Process beat rules (if any)
        if (hasBeatRules) {
            boolean beatMatched = false;
            for (Rule rule : rulesByType.get(Comparison.BEAT)) {
                if (Operator.evaluate(rule.getComparison(), beat, rule.getValue())) {
                    beatMatched = true;
                    break;
                }
            }

            if (!beatMatched)
                return false; // No beat rule matched
        }

        // Process bar rules (if any)
        if (hasBarRules) {
            boolean barMatched = false;
            for (Rule rule : rulesByType.get(Comparison.BAR)) {
                if (Operator.evaluate(rule.getComparison(), bar, rule.getValue())) {
                    barMatched = true;
                    break;
                }
            }

            if (!barMatched)
                return false; // No bar rule matched
        }

        // If we got this far, all rule types matched (or weren't present)
        return true;
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
                System.out.println("  - Tick: " + tick + " (cycler position: " + session.getTickCycler().get() + ")");
                System.out.println("  - Beat: " + beat + " (cycler position: " + session.getBeatCycler().get() + ")");
                System.out.println("  - Bar: " + bar + " (cycler position: " + session.getBarCycler().get() + ")");
                System.out.println("  - Part: " + part + " (cycler position: " + session.getPartCycler().get() + ")");

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

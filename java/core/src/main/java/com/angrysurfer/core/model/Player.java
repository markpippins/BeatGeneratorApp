package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import com.angrysurfer.core.model.feature.Pad;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
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
        // System.out.println("Player constructor: Registered with buses");
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

    public void setMinVelocity(Long minVelocity) {
        this.minVelocity = minVelocity;
        if (minVelocity > maxVelocity) {
            setMaxVelocity(minVelocity);
        }
    }

    public void setMaxVelocity(Long maxVelocity) {
        this.maxVelocity = maxVelocity;
        if (minVelocity > maxVelocity) {
            setMinVelocity(maxVelocity);
        }
    }

    // public Long getSubPosition() {
    // return getSub();
    // }s

    public abstract void onTick(long tickCount, long beatCount, long barCount, long part);

    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public void drumNoteOn(long note) {
        logger.debug("drumNoteOn() - note: {}", note);


        int velWeight = getMaxVelocity().intValue() - getMinVelocity().intValue();

        long velocity = getMinVelocity() + rand.nextInt(velWeight + 1);
        // Send note on message
        int randWeight = randomDegree.intValue() > 0 ? rand.nextInt(randomDegree.intValue()) : 0;

        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                noteOn(note + randWeight + getSession().getNoteOffset(), velocity);
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
    
    public boolean shouldPlay(long currentTick, long currentBeat, long currentBar, long currentPart) {
        // Early out if no applicable rules or player not enabled
        if (rules == null || rules.isEmpty() || !getEnabled()) {
            return false;
        }


        boolean debug = true;
        if (debug) {
            logger.info("Player {}: Evaluating rules at tick={}, beat={}, bar={}, part={}",
                    getName(), currentTick, currentBeat, currentBar, currentPart);
        }

        // Refresh rule cache if needed
        if (!hasCachedRules) {
            cacheRulesByType();
            hasCachedRules = true;
            if (debug) {
                logger.info("Player {}: Cached {} tick, {} beat, {} bar rules",
                        getName(), tickRuleCache.size(), beatRuleCache.size(), barRuleCache.size());
            }
        }

        // Instead of comparing the raw tick count, compute the position within the beat.
        // This ensures a tick rule of "1" fires only on the first tick of each beat.
        long ticksPerBeat = session.getTicksPerBeat();
        long tickInBeat = ((currentTick - 1) % ticksPerBeat) + 1;
        if (debug) {
            logger.info("Player {}: currentTickInBeat={}", getName(), tickInBeat);
        }

        // Evaluate tick rules: default to true if none exists.
        boolean tickTriggered = tickRuleCache.isEmpty();
        if (!tickTriggered) {
            for (Rule rule : tickRuleCache) {
                // Compare using tickInBeat
                boolean match = Operator.evaluate(rule.getComparison(), (double) tickInBeat, rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Tick rule: comp={}, tickInBeat={}, ruleVal={}, result={}",
                            rule.getComparison(), tickInBeat, rule.getValue(), match);
                }
                if (match) {
                    tickTriggered = true;
                    break;
                }
            }
        }

        // Evaluate beat rules: default to true if none exists.
        boolean beatTriggered = beatRuleCache.isEmpty();
        if (!beatTriggered) {
            for (Rule rule : beatRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), currentBeat, rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Beat rule: comp={}, currentBeat={}, ruleVal={}, result={}",
                            rule.getComparison(), currentBeat, rule.getValue(), match);
                }
                if (match) {
                    beatTriggered = true;
                    break;
                }
            }
        }

        if (!tickTriggered || !beatTriggered) {
            if (debug) {
                logger.info("Player {}: Trigger condition not met. tickTriggered={}, beatTriggered={}",
                        getName(), tickTriggered, beatTriggered);
            }
            return false;
        }

        // Enforce bar rules (if any)
        if (!barRuleCache.isEmpty()) {
            boolean barMatched = false;
            for (Rule rule : barRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), (double) currentBar, rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Bar rule: comp={}, currentBar={}, ruleVal={}, result={}",
                            rule.getComparison(), currentBar, rule.getValue(), match);
                }
                if (match) {
                    barMatched = true;
                    break;
                }
            }
            if (!barMatched) {
                if (debug) {
                    logger.info("Player {}: Bar rule did not match.", getName());
                }
                return false;
            }
        }

        // Use global count values from the session rather than current (cyclical) ones.
        double globalTickCount = (double) getSession().getTickCount();
        double globalBeatCount = getSession().getBeatCount();
        double globalBarCount = getSession().getBarCount();
        double globalPartCount = getSession().getPartCount();

        // Evaluate tick count rules: default to true if none exists.
        boolean tickCountMatched = tickCountRuleCache.isEmpty();
        if (!tickCountMatched) {
            for (Rule rule : tickCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), globalTickCount, rule.getValue().doubleValue());
                logger.info("Tick Count rule: comp={}, globalTickCount={}, ruleVal={}, result={}", rule.getComparison(), globalTickCount, rule.getValue(), match);
                if (match) {
                    tickCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate beat count rules: default to true if none exists.
        boolean beatCountMatched = beatCountRuleCache.isEmpty();
        if (!beatCountMatched) {
            for (Rule rule : beatCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), globalBeatCount, rule.getValue().doubleValue());
                logger.info("Beat Count rule: comp={}, globalBeatCount={}, ruleVal={}, result={}", rule.getComparison(), globalBeatCount, rule.getValue(), match);
                if (match) {
                    beatCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate bar count rules: default to true if none exists.
        boolean barCountMatched = barCountRuleCache.isEmpty();
        if (!barCountMatched) {
            for (Rule rule : barCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), globalBarCount, rule.getValue().doubleValue());
                logger.info("Bar Count rule: comp={}, globalBarCount={}, ruleVal={}, result={}", rule.getComparison(), globalBarCount, rule.getValue(), match);
                if (match) {
                    barCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate part count rules: default to true if none exists.
        boolean partCountMatched = partCountRuleCache.isEmpty();
        if (!partCountMatched) {
            for (Rule rule : partCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(), globalPartCount, rule.getValue().doubleValue());
                logger.info("Part Count rule: comp={}, globalPartCount={}, ruleVal={}, result={}", rule.getComparison(), globalPartCount, rule.getValue(), match);
                if (match) {
                    partCountMatched = true;
                    break;
                }
            }
        }

        // All count constraints must match if present
        if (!tickCountMatched || !beatCountMatched || !barCountMatched || !partCountMatched) {
            logger.info("Player {}: Count constraints not met: tickCountMatched={}, beatCountMatched={}, barCountMatched={}, partCountMatched={}", getName(), tickCountMatched, beatCountMatched, barCountMatched, partCountMatched);
            return false;
        }

        // Lastly, check probability (or other conditions)
        if (!isProbable()) {
            if (debug) {
                logger.info("Player {}: Failed isProbable check.", getName());
            }
            return false;
        }

        if (debug) {
            logger.info("Player {}: All checks passed, should play.", getName());
        }

        return true;
    }

    // Add this method to Player class
    private void cacheRulesByType() {
        // Clear existing caches
        tickRuleCache.clear();
        beatRuleCache.clear();
        barRuleCache.clear();
        partRuleCache.clear();
        
        // Skip if no rules
        if (rules == null || rules.isEmpty()) {
            return;
        }
        
        // Process each rule once and cache it
        for (Rule rule : rules) {
            // Group by operator type
            switch (rule.getOperator()) {
                case Comparison.TICK:
                    tickRuleCache.add(rule);
                    break;
                case Comparison.BEAT:
                    beatRuleCache.add(rule);
                    break;
                case Comparison.BAR:
                    barRuleCache.add(rule);
                    break;
                case Comparison.PART:
                    partRuleCache.computeIfAbsent(Long.valueOf(rule.getPart()), k -> new HashSet<>())
                                 .add(rule);
                    break;
                case Comparison.TICK_COUNT:
                    tickCountRuleCache.add(rule);
                    break;
                case Comparison.BEAT_COUNT:
                    beatCountRuleCache.add(rule);
                    break;
                case Comparison.BAR_COUNT:
                    barCountRuleCache.add(rule);
                    break;
                case Comparison.PART_COUNT:
                    partCountRuleCache.add(rule);
                    break;
            }
        }
    }

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

        // // System.out.println("Player " + getName() + " received command: " + cmd);

        if (getSession() != null && getEnabled()) {
            switch (cmd) {
            case Commands.TIME_TICK -> {
                if (!isRunning()) {
                    // System.out.println("Player " + getName() + " - Skipping tick (not running)");
                    return;
                }

                Session session = getSession();
                long tickCount = session.getTickCount();
                long beatCount = session.getBeatCount();
                long barCount = session.getBarCount();
                long part = session.getPart();

                // System.out.println("Player " + getName() + " processing tick, current tick: " + tick);

                // Only trigger if we haven't already triggered for this tick
                if (tickCount == lastTriggeredTick) {
                    // System.out.println("Player " + getName() + " - Already triggered for tick " + tick);
                    return;
                }
                lastTriggeredTick = tickCount;

                // Optionally, check shouldPlay() here (which evaluates tick/beat rules)
                if (shouldPlay(tickCount, beatCount, barCount, part)) {
                    if (getEnabled() && !isMuted())
                        onTick(tickCount, session.getBeatCount(), session.getBarCount(), part);
                }
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

    // Add these properties to the Player class
    @JsonIgnore
    @Transient
    private boolean hasCachedRules = false;

    @JsonIgnore
    @Transient
    private final Set<Rule> tickRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> beatRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> barRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Map<Long, Set<Rule>> partRuleCache = new HashMap<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> tickCountRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> beatCountRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> barCountRuleCache = new HashSet<>();

    @JsonIgnore
    @Transient
    private final Set<Rule> partCountRuleCache = new HashSet<>();

    // Add this method to Player class
    public void invalidateRuleCache() {
        hasCachedRules = false;
        logger.info("Player {}: Rule cache invalidated", getName());
    }

    // Make sure this gets called when rules are set or modified
    public void setRules(Set<Rule> rules) {
        this.rules = rules;
        invalidateRuleCache(); // Invalidate cache when rules change
    }

    // Add a call to invalidateRuleCache in addRule/removeRule methods
    public void addRule(Rule rule) {
        if (rules == null) {
            rules = new HashSet<>();
        }
        rules.add(rule);
        invalidateRuleCache(); // Invalidate cache when rule added
    }

    public void removeRule(Rule rule) {
        if (rules != null) {
            rules.remove(rule);
            invalidateRuleCache(); // Invalidate cache when rule removed
        }
    }

    // Add this property to the Player class
    @JsonIgnore
    @Transient
    private long lastTriggeredTick = -1;
}

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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.feature.Pad;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Player implements Callable<Boolean>, Serializable, IBusListener {

    static final Random rand = new Random();

    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());

    private Set<Pad> pads = new HashSet<>();

    private Long id;

    private Long instrumentId;

    @JsonIgnore
    public boolean isSelected = false;

    private String name = "Player";

    private Integer channel = 0;

    private Integer swing = 0;

    private Integer level = 100;

    private Integer rootNote = 60;

    private Integer minVelocity = 100;

    private Integer maxVelocity = 110;

    private Integer preset = 1;

    private Boolean stickyPreset = false;

    private Integer probability = 100;

    private Integer randomDegree = 0;

    private Integer ratchetCount = 0;

    private Integer ratchetInterval = 1;

    private Integer internalBars = Constants.DEFAULT_BAR_COUNT;

    private Integer internalBeats = Constants.DEFAULT_BEATS_PER_BAR;

    private Boolean useInternalBeats = false;

    private Boolean useInternalBars = false;

    private Integer panPosition = 63;

    private Boolean preserveOnPurge = false;

    private double sparse = 0.0;

    private boolean solo = false;

    private boolean muted = false;

    private Integer position;

    private Long lastTick = 0L;

    private Long lastPlayedTick = 0L;

    private Long lastPlayedBar;

    private Integer skips = 0;

    private double lastPlayedBeat;

    private Integer subDivisions = 4;

    private Integer beatFraction = 1;

    private Integer fadeOut = 0;

    private Integer fadeIn = 0;

    private Boolean accent = false;

    private String scale = "Chromatic";

    private Integer duration = 100;

    private Boolean x0xPlayer = false;

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

    @JsonIgnore
    private boolean unsaved = false;

    @JsonIgnore
    private Boolean armForNextTick = false;

    private Set<Rule> rules = new HashSet<>();

    private List<Integer> allowedControlMessages = new ArrayList<>();

    @JsonIgnore
    private InstrumentWrapper instrument;

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

    public Player(String name, Session session, InstrumentWrapper instrument) {
        this(); // Call default constructor to ensure registration
        setName(name);
        setInstrument(instrument);
        setSession(session);
    }

    public Player(String name, Session session, InstrumentWrapper instrument, List<Integer> allowedControlMessages) {
        this(name, session, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    public void setInstrument(InstrumentWrapper instrument) {
        this.instrument = instrument;
        this.instrumentId = Objects.nonNull(instrument) ? instrument.getId() : null;
    }

    public String getPlayerClassName() {
        return getClass().getSimpleName().toLowerCase();
    }

    // public Long getSubPosition() {
    // return getSub();
    // }

    public abstract void onTick(TimingUpdate timingUpdate);

    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    public void drumNoteOn(int note) {
        logger.debug("drumNoteOn() - note: {}", note);

        int velWeight = getMaxVelocity() - getMinVelocity();

        int velocity = getMinVelocity() + rand.nextInt(velWeight + 1);
        // Send note on message
        int randWeight = randomDegree > 0 ? rand.nextInt(randomDegree) : 0;

        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                noteOn(note + randWeight + getSession().getNoteOffset(), velocity);
            } catch (Exception e) {
                logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
            }
        }, 0, // Shorter note duration (100ms instead of 2500ms)
                java.util.concurrent.TimeUnit.MILLISECONDS);

        // Schedule note off instead of blocking with Thread.sleep
        final int finalVelocity = velocity;

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

    public void noteOn(int note, int velocity) {
        logger.debug("noteOn() - note: {}, velocity: {}", note, velocity);

        int fixedVelocity = velocity < 126 ? velocity : 126;

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

    public void noteOn(int note, int velocity, int decay) {
        logger.debug("noteOn() - note: {}, velocity: {}", note, velocity);

        int fixedVelocity = velocity < 126 ? velocity : 126;

        try {
            // Set playing state to true
            setPlaying(true);

            // Schedule UI refresh after a short delay
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this), decay, // 50ms
                                                                                                            // delay
                    java.util.concurrent.TimeUnit.MILLISECONDS);

            // getInstrument().noteOn(getChannel(), note, fixedVelocity);
            getInstrument().playMidiNote(getChannel(), note, fixedVelocity, decay);

        } catch (Exception e) {
            logger.error("Error in noteOn: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void noteOff(int note, int velocity) {
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
        int test = rand.nextInt(101);
        int probable = getProbability();

        boolean result = test < probable;
        return result;
    }

    private boolean hasNoMuteGroupConflict() {
        return true;
    }

    private Set<Rule> filterByPart(Set<Rule> rules, boolean includeNoPart) {
        if (rules == null || session == null) {
            return new HashSet<>();
        }

        int currentPart = session.getPart();

        return rules.stream().filter(r -> {
            int rulePart = r.getPart();
            return rulePart == 0 || (includeNoPart && rulePart == currentPart);
        }).collect(Collectors.toSet());
    }

    /**
     * Determines whether this player should play at the given position
     *
     * @param tickPosition        Current position within beat (1-based)
     * @param timingUpdate.beat() Current position within bar (1-based)
     * @param timingUpdate.bar()  Current position within pattern (1-based)
     * @param partPosition        Current position within arrangement (1-based)
     * @param tickCount           Global tick counter (continuously increasing)
     * @param beatCount           Global beat counter (continuously increasing)
     * @param barCount            Global bar counter (continuously increasing)
     * @param partCount           Global part counter (continuously increasing)
     */
    public boolean shouldPlay(TimingUpdate timingUpdate) {
        // Early out if no applicable rules or player not enabled
        if (rules == null || rules.isEmpty() || !getEnabled()) {
            return false;
        }

        boolean debug = false; // Set to true for verbose logging
        if (debug) {
            logger.info("Player {}: Evaluating rules at position tick={}, beat={}, bar={}, part={}",
                    getName(), timingUpdate.tick(), timingUpdate.beat(), timingUpdate.bar(), timingUpdate.part());
            logger.info("Player {}: Global counters: tick={}, beat={}, bar={}, part={}",
                    getName(), timingUpdate.tickCount(), timingUpdate.beatCount(), timingUpdate.barCount(),
                    timingUpdate.partCount());
        }

        // Refresh rule cache if needed
        if (!hasCachedRules) {
            cacheRulesByType();
            hasCachedRules = true;
            if (debug) {
                logger.info("Player {}: Cached {} tick, {} beat, {} bar rules", getName(),
                        tickRuleCache.size(), beatRuleCache.size(), barRuleCache.size());
            }
        }

        // Evaluate tick rules: default to true if none exists
        // Now using tickPosition directly (ticks are 1-based in our system)
        boolean tickTriggered = tickRuleCache.isEmpty();
        if (!tickTriggered) {
            for (Rule rule : tickRuleCache) {
                // Use positional tick value directly
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.tick(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Tick rule: comp={}, tickPosition={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.tick(), rule.getValue(), match);
                }
                if (match) {
                    tickTriggered = true;
                    break;
                }
            }
        }

        // Evaluate beat rules: default to true if none exists
        // Now using timingUpdate.beat() directly (beats are 1-based in our system)
        boolean beatTriggered = beatRuleCache.isEmpty();
        if (!beatTriggered) {
            for (Rule rule : beatRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.beat(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Beat rule: comp={}, timingUpdate.beat()={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.beat(), rule.getValue(), match);
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
        // Now using timingUpdate.bar() directly (bars are 1-based in our system)
        if (!barRuleCache.isEmpty()) {
            boolean barMatched = false;
            for (Rule rule : barRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        (double) timingUpdate.bar(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Bar rule: comp={}, timingUpdate.bar()={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.bar(), rule.getValue(), match);
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

        // Now evaluate count rules using the global counters
        // Evaluate tick count rules: default to true if none exists
        boolean tickCountMatched = tickCountRuleCache.isEmpty();
        if (!tickCountMatched) {
            for (Rule rule : tickCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.tickCount(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Tick Count rule: comp={}, tickCount={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.tickCount(), rule.getValue(), match);
                }
                if (match) {
                    tickCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate beat count rules: default to true if none exists
        boolean beatCountMatched = beatCountRuleCache.isEmpty();
        if (!beatCountMatched) {
            for (Rule rule : beatCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.beatCount(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Beat Count rule: comp={}, beatCount={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.beatCount(), rule.getValue(), match);
                }
                if (match) {
                    beatCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate bar count rules: default to true if none exists
        boolean barCountMatched = barCountRuleCache.isEmpty();
        if (!barCountMatched) {
            for (Rule rule : barCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.barCount(), rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Bar Count rule: comp={}, barCount={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.barCount(), rule.getValue(), match);
                }
                if (match) {
                    barCountMatched = true;
                    break;
                }
            }
        }

        // Evaluate part count rules: default to true if none exists
        boolean partCountMatched = partCountRuleCache.isEmpty();
        if (!partCountMatched) {
            for (Rule rule : partCountRuleCache) {
                boolean match = Operator.evaluate(rule.getComparison(),
                        timingUpdate.partCount(),
                        rule.getValue().doubleValue());
                if (debug) {
                    logger.info("Part Count rule: comp={}, partCount={}, ruleVal={}, result={}",
                            rule.getComparison(), timingUpdate.partCount(), rule.getValue(), match);
                }
                if (match) {
                    partCountMatched = true;
                    break;
                }
            }
        }

        // All count constraints must match if present
        if (!tickCountMatched || !beatCountMatched || !barCountMatched || !partCountMatched) {
            if (debug) {
                logger.info(
                        "Player {}: Count constraints not met: tickCount={}, beatCount={}, barCount={}, partCount={}",
                        getName(), tickCountMatched, beatCountMatched, barCountMatched, partCountMatched);
            }
            return false;
        }

        // Lastly, check probability
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
                    partRuleCache.computeIfAbsent(Long.valueOf(rule.getPart()), k -> new HashSet<>()).add(rule);
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
     * Determines whether this player would play at the specified position based
     * on its rules. This method is used by visualizations to predict when
     * players will trigger.
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
        if (action == null || action.getCommand() == null) {
            return;
        }

        String cmd = action.getCommand();

        // // System.out.println("Player " + getName() + " received command: " + cmd);
        if (getSession() != null && getEnabled()) {
            switch (cmd) {
                case Commands.TIMING_UPDATE -> {

                    if (getRules().isEmpty() || !(action.getData() instanceof TimingUpdate)) {
                        return;
                    }

                    TimingUpdate timingUpdate = (TimingUpdate) action.getData();
                    if (timingUpdate.tickCount() == lastTriggeredTick) {
                        return;
                    }

                    if (shouldPlay(timingUpdate)) {

                        if (getEnabled() && !isMuted()) {
                            onTick(timingUpdate);
                            setLastPlayedTick(timingUpdate.tick());
                        }
                    }

                    lastTriggeredTick = timingUpdate.tickCount();
                }

                case Commands.TRANSPORT_STOP -> {
                    // Disable self when transport stops
                    setEnabled(false);
                }
                case Commands.TRANSPORT_START -> {
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
     * Clean up resources when this player is no inter needed
     */
    public void dispose() {
        // Unregister from command bus to prevent memory leaks
        commandBus.unregister(this);
    }

    // Add this property to the Player class
    @JsonIgnore
    private boolean isPlaying = false;

    // Add getter/setter
    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    @JsonIgnore
    private boolean hasCachedRules = false;

    @JsonIgnore
    private final Set<Rule> tickRuleCache = new HashSet<>();

    @JsonIgnore
    private final Set<Rule> beatRuleCache = new HashSet<>();

    @JsonIgnore
    private final Set<Rule> barRuleCache = new HashSet<>();

    @JsonIgnore
    private final Map<Long, Set<Rule>> partRuleCache = new HashMap<>();

    @JsonIgnore
    private final Set<Rule> tickCountRuleCache = new HashSet<>();

    @JsonIgnore
    private final Set<Rule> beatCountRuleCache = new HashSet<>();

    @JsonIgnore
    private final Set<Rule> barCountRuleCache = new HashSet<>();

    @JsonIgnore
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
    private long lastTriggeredTick = -1;
}

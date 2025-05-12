package com.angrysurfer.core.model;

import java.io.Serializable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.angrysurfer.core.service.InternalSynthManager;
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

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.*;

@Getter
@Setter
public abstract class Player implements Callable<Boolean>, Serializable, IBusListener {

    static final Random rand = new Random();

    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());

    private Set<Pad> pads = new HashSet<>();

    private Long id;

    private Long instrumentId;

    private Boolean isDefault = false;
    
    @JsonIgnore
    public transient boolean isSelected = false;

    private boolean melodicPlayer = false;

    private boolean drumPlayer = false;

    private String name = "Player";

    private Integer defaultChannel = 0;

    private Integer swing = 0;

    private Integer level = 100;

    private Integer rootNote = 60;

    private Integer minVelocity = 100;

    private Integer maxVelocity = 110;

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

    @JsonIgnore
    private transient Object owner;

    @JsonIgnore
    @Transient
    private final TimingBus timingBus = TimingBus.getInstance();

    @JsonIgnore
    @Transient
    private final CommandBus commandBus = CommandBus.getInstance();

    // Add these fields to Player class
    @JsonIgnore
    private static final ExecutorService NOTE_EXECUTOR = Executors.newFixedThreadPool(4);
    @JsonIgnore
    private static final ScheduledExecutorService NOTE_OFF_SCHEDULER = Executors.newScheduledThreadPool(4);

    // Add UI update throttling
    @JsonIgnore
    private long lastUiUpdateTime = 0;
    private static final long MIN_UI_UPDATE_INTERVAL = 100; // Only update UI every 100ms max

    // Add a new optimized method
    private boolean usingInternalSynth = true;
    private InternalSynthManager internalSynthManager = null;

    // Update these fields
    @JsonIgnore
    private transient final ShortMessage reuseableMessage = new ShortMessage();

    @JsonIgnore
    private transient final Object messageLock = new Object();

    private long lastNoteTime = 0;
    private static final long NOTE_THROTTLE_THRESHOLD = 1; // 1ms minimum between notes

    // Add initialization method
    @JsonIgnore
    private void initializeTransientFields() {
        // No longer needed as reuseableMessage and messageLock are final
    }

    // Call this in noteOn, triggerNoteWithThrottle, etc.
    private void ensureInitialized() {
        // No longer needed as reuseableMessage and messageLock are final
    }

    /**
     * Simple initialization for minimal player setup
     */
    protected void initialize(String name, Session session, InstrumentWrapper instrument,
            List<Integer> allowedControlMessages) {
        // Register with command and timing buses
        setName(name);
        setSession(session);
        setInstrument(instrument);
        setAllowedControlMessages(allowedControlMessages);

        commandBus.register(this);
        timingBus.register(this);

        // Initialize rules collection
        rules = new HashSet<>();
    }

    public void setInstrument(InstrumentWrapper instrument) {
        if (instrument == null) {
            return;
        }
        this.instrument = instrument;
        this.instrument.setChannel(getDefaultChannel());
        this.instrumentId = instrument.getId();
        if (this.getInstrument().getDeviceName().contains("Gervill"))
            getInstrument().setInternal(true);
    }

    public String getPlayerClassName() {
        return getClass().getSimpleName().toLowerCase();
    }

    @JsonIgnore
    @Transient
    public MidiDevice getDevice() {
        return Objects.nonNull(getInstrument()) ? getInstrument().getDevice() : null; 
    }

    @JsonIgnore
    @Transient
    public Integer getChannel() {
        if (getInstrument() != null)
            return getInstrument().getChannel();

        return getDefaultChannel();
    }

    public void setDefaultChannel(Integer channel) {
        defaultChannel = channel;
        if (getInstrument() != null)
            getInstrument().setChannel(channel);
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

        NOTE_OFF_SCHEDULER.schedule(() -> {
            try {
                triggerNoteWithThrottle(note + randWeight + getSession().getNoteOffset(), velocity);
            } catch (Exception e) {
                logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
            }
        }, 0, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Schedule note off instead of blocking with Thread.sleep
        final int finalVelocity = velocity;

        // Use ScheduledExecutorService for note-off scheduling
        NOTE_OFF_SCHEDULER.schedule(() -> {
            try {
                noteOff(note, finalVelocity);
            } catch (Exception e) {
                logger.error("Error in scheduled noteOff: {}", e.getMessage(), e);
            }
        }, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Trigger a note with throttling to prevent MIDI buffer overflows
     * 
     * @param note     MIDI note number to play
     * @param velocity Note velocity (0-127)
     */
    public void triggerNoteWithThrottle(int note, int velocity) {
        // First check if we have an instrument to play through
        if (instrument == null) {
            return;
        }

        // Update player state
        setPlaying(true);

        // Throttle rapid note triggering
        long now = System.nanoTime() / 1_000_000; // Current time in ms
        long timeSinceLastNote = now - lastNoteTime;

        if (timeSinceLastNote < NOTE_THROTTLE_THRESHOLD) {
            // Use the executor to slightly delay the note if we're sending too many
            NOTE_EXECUTOR.submit(() -> {
                try {
                    // Small sleep to prevent overwhelming the MIDI system
                    Thread.sleep(NOTE_THROTTLE_THRESHOLD - timeSinceLastNote + 1);
                    sendNoteOnMessage(note, velocity);
                } catch (Exception e) {
                    logger.error("Error in throttled note trigger: {}", e.getMessage(), e);
                }
            });
        } else {
            // Send note immediately if we're not throttling
            sendNoteOnMessage(note, velocity);
        }

        // Always update the last note time
        lastNoteTime = now;

        // Update UI only if needed (throttled)
        updateUIIfNeeded();
    }

    /**
     * Helper method to send the actual MIDI note-on message
     */
    private void sendNoteOnMessage(int note, int velocity) {
        try {
            // Use synchronized block to prevent concurrent modification of the reusable
            // message
            synchronized (messageLock) {
                reuseableMessage.setMessage(ShortMessage.NOTE_ON, note, velocity);
                // Replace sendMessage with sendToDevice
                instrument.sendMessage(reuseableMessage);
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Error sending note-on message: {}", e.getMessage(), e);
        }
    }

    /**
     * Facade method to play a note with standard duration
     * 
     * @param note     MIDI note number
     * @param velocity Note velocity (0-127)
     */
    public void noteOn(int note, int velocity) {
        // Update player state
        setPlaying(true);
        updateUIIfNeeded();

        try {
            // Delegate to instrument wrapper
            if (instrument != null) {
                synchronized (messageLock) {
                    reuseableMessage.setMessage(ShortMessage.NOTE_ON, note, velocity);
                    instrument.sendMessage(reuseableMessage); // Changed from sendMessage to sendToDevice
                }
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Error in noteOn: {}", e.getMessage(), e);
        }
    }

    /**
     * Facade method to play a note with specified decay time
     * 
     * @param note     MIDI note number
     * @param velocity Note velocity (0-127)
     * @param decay    Note duration in ms
     */
    public void noteOn(int note, int velocity, int decay) {
        // Update player state
        setPlaying(true);
        updateUIIfNeeded();

        try {
            // Delegate to instrument wrapper
            if (instrument != null) {
                instrument.playMidiNote(note, velocity, decay);
            }
        } catch (Exception e) {
            logger.error("Error in noteOn with decay: {}", e.getMessage(), e);
        }
    }

    /**
     * Facade method to play a note with specified gate time
     * 
     * @param note     MIDI note number
     * @param velocity Note velocity (0-127)
     * @param gate     Note gate time in ms
     */
    // public void noteOn(int note, int velocity, int gate) {
    // // Update player state
    // setPlaying(true);
    // updateUIIfNeeded();
    //
    // // Skip note if player is muted
    // if (level <= 0) {
    // return;
    // }
    //
    // try {
    // // Delegate to instrument wrapper
    // if (instrument != null) {
    // synchronized (messageLock) {
    // reuseableMessage.setMessage(ShortMessage.NOTE_ON, note, velocity);
    // instrument.sendToDevice(reuseableMessage);
    // }
    // }
    // } catch (Exception e) {
    // logger.error("Error in noteOn: {}", e.getMessage(), e);
    // }
    // }

    /**
     * Facade method to stop a note
     * 
     * @param note     MIDI note number
     * @param velocity Release velocity (usually 0)
     */
    public void noteOff(int note, int velocity) {
        try {
            // Delegate to instrument wrapper
            if (instrument != null) {
                instrument.noteOff(note, velocity);
            }

            // Update player state
            setPlaying(false);

            // Schedule UI update
            NOTE_OFF_SCHEDULER.schedule(
                    () -> CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this),
                    50, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Error in noteOff: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends All Notes Off message on player's channel
     * Uses MIDI Control Change #123 to immediately stop all playing notes
     */
    public void allNotesOff() {
        logger.debug("Sending All Notes Off for player {} on channel {}", getName(), getChannel());

        try {
            // First approach: Use control change 123 (All Notes Off)
            if (instrument != null) {
                instrument.controlChange(123, 0);
                logger.debug("Sent All Notes Off message to instrument {}",
                        instrument.getName());
            }

            // Second approach: Send explicit note off messages for safety
            // Some hardware/software synths don't properly respond to CC 123
            try {
                // Send note off for all possible MIDI notes (0-127)
                if (instrument != null) {
                    for (int note = 0; note < 128; note++) {
                        synchronized (messageLock) {
                            reuseableMessage.setMessage(ShortMessage.NOTE_OFF, getChannel(), note, 0);
                            instrument.sendMessage(reuseableMessage);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error sending explicit note-off messages: {}", e.getMessage());
            }

            // Update player state
            setPlaying(false);

            // Notify UI
            updateUIIfNeeded();

        } catch (Exception e) {
            logger.error("Error in allNotesOff: {}", e.getMessage(), e);
        }
    }

    /**
     * Update the UI if sufficient time has passed since last update
     */
    private void updateUIIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastUiUpdateTime > MIN_UI_UPDATE_INTERVAL) {
            lastUiUpdateTime = now;
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this);
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
     * @param timingUpdate Current TIMING_UPDATE
     */
    public boolean shouldPlay(TimingUpdate timingUpdate) {
        // Quick checks first
        if (!getEnabled() || isMuted()) {
            return false;
        }

        // CRITICAL CHANGE: Let sequencers play without rule checks
        if (isMelodicPlayer() || isDrumPlayer()) {
            return true;
        }

        // Rest of the method remains the same for players with rules
        // ...

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

    /**
     * Set the pan position for this player
     * 
     * @param pan The pan position (0-127, 64 is center)
     */
    public void setPan(int pan) {
        this.panPosition = pan;
    }

    /**
     * Set the chorus effect amount
     * 
     * @param amount Chorus amount (0-100)
     */
    public void setChorus(int amount) {
        // this.chorus = chorus;
    }

    /**
     * Set the reverb effect amount
     * 
     * @param amount Reverb amount (0-100)
     */
    public void setReverb(int amount) {
        // this.reverb = amount;
    }

    // Add cleanup method to shutdown pools on application exit
    public static void shutdownExecutors() {
        NOTE_EXECUTOR.shutdown();
        NOTE_OFF_SCHEDULER.shutdown();
    }

    // Add this method to handle proper deserialization
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Reinitialize transient fields
        try {
            initializeTransientFields();
        } catch (Exception e) {
            // Log but continue - we can create a new one each time if needed
            System.err.println("Error reinitializing MIDI message: " + e.getMessage());
        }
    }

    public Integer getPreset() {
        return Objects.nonNull(instrument) ? instrument.getPreset() : 0;
    }
}

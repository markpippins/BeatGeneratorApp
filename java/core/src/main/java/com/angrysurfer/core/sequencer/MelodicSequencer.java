package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    // Add as static fields in both sequencer classes
    private static final ScheduledExecutorService SHARED_NOTE_SCHEDULER = Executors.newScheduledThreadPool(2);

    // Add these constants at the top of the class with other constants
    private static final int NO_SWING = 50; // Percentage value for no swing
    public static final int MAX_SWING = 99; // Maximum swing percentage
    public static final int MIN_SWING = 25; // Minimum swing percentage

    // Add these instance variables with other properties
    private int swingPercentage = 50; // Default swing percentage (50 = no swing)
    private boolean swingEnabled = false; // Swing enabled flag

    // Sequencing parameters
    private int currentStep = 0; // Current step in the pattern
    private int patternLength = 16; // Default pattern length
    private Direction direction = Direction.FORWARD; // Default direction
    private boolean looping = true; // Default to looping
    private long tickCounter = 0; // Tick counter
    private long ticksPerStep = 24; // Base ticks per step (96 PPQN / 4 = 24)
    private boolean isPlaying = false; // Flag indicating playback state
    private int bounceDirection = 1; // Direction for bounce mode
    private int channel = 3; // Default MIDI channel

    private TimingDivision timingDivision = TimingDivision.NORMAL;

    // Scale & quantization
    private String selectedRootNote = "C";
    private String scale = Scale.SCALE_CHROMATIC;
    private Boolean[] scaleNotes;
    private boolean quantizeEnabled = true;
    private Quantizer quantizer;
    private int octaveShift = 0; // Compatibility for scale

    // Pattern data storage
    private List<Boolean> activeSteps = new ArrayList<>(); // Step on/off state
    private List<Integer> noteValues = new ArrayList<>(); // Note for each step
    private List<Integer> velocityValues = new ArrayList<>(); // Velocity for each step
    private List<Integer> gateValues = new ArrayList<>(); // Gate time for each step
    private List<Integer> probabilityValues = new ArrayList<>();
    private List<Integer> nudgeValues = new ArrayList<>();

    // Note object for storing melodic properties (NEW)
    private Player player;

    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    // Add master tempo field to synchronize with session
    private int masterTempo;

    // Add these fields
    private Integer id;
    private Long melodicSequenceId = 0L;

    // Add this field to the class
    private boolean latchEnabled = false;

    // Add or modify this field and methods
    private Consumer<NoteEvent> noteEventPublisher;

    // Add this field to the class
    private int currentTilt = 0;

    // Add field for next pattern ID
    private Long nextPatternId = null;

    // Add these fields and methods to MelodicSequencer class
    private List<Integer> harmonicTiltValues = new ArrayList<>(16);

    // Fix this in MelodicSequencer - add reusable message as in DrumSequencer
    private final javax.sound.midi.ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    // Add this to both sequencers to track last note time
    private long lastNoteTriggeredTime = 0;
    private static final long MIN_NOTE_INTERVAL_MS = 1; // 1ms minimum between notes

    private void triggerNoteWithThrottle(int note, int velocity) {
        long now = System.currentTimeMillis();
        if (now - lastNoteTriggeredTime >= MIN_NOTE_INTERVAL_MS) {
            player.noteOn(note, velocity);
            lastNoteTriggeredTime = now;
        } else {
            // Queue or skip if too many notes trigger at once
        }
    }

    /**
     * Initialize the harmonic tilt values with defaults (0)
     */
    public void initializeHarmonicTiltValues() {
        harmonicTiltValues = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            harmonicTiltValues.add(0);
        }
        logger.debug("Initialized harmonic tilt values with defaults");
    }

    /**
     * Get all harmonic tilt values
     * 
     * @return List of tilt values for each bar
     */
    public List<Integer> getHarmonicTiltValues() {
        if (harmonicTiltValues == null || harmonicTiltValues.isEmpty()) {
            logger.warn("Tilt values not initialized, creating defaults");
            initializeHarmonicTiltValues();
        }
        return harmonicTiltValues;
    }

    /**
     * Set all harmonic tilt values
     * 
     * @param values List of tilt values for each bar
     */
    public void setHarmonicTiltValues(List<Integer> values) {
        // Create a defensive copy to avoid external modification
        harmonicTiltValues = new ArrayList<>(values);

        // Make sure we have exactly 16 values
        while (harmonicTiltValues.size() < 16) {
            harmonicTiltValues.add(0);
        }

        // Truncate if more than 16
        if (harmonicTiltValues.size() > 16) {
            harmonicTiltValues = harmonicTiltValues.subList(0, 16);
        }

        // If we're tracking the current bar, update the current tilt value
        int currentBar = getCurrentBar();
        if (currentBar >= 0 && currentBar < harmonicTiltValues.size()) {
            setCurrentTilt(harmonicTiltValues.get(currentBar));
        }
    }

    /**
     * Get the tilt value for a specific bar
     * 
     * @param barIndex The bar index (0-15)
     * @return The tilt value (-7 to 7)
     */
    public int getTiltValueForBar(int barIndex) {
        if (barIndex >= 0 && barIndex < harmonicTiltValues.size()) {
            return harmonicTiltValues.get(barIndex);
        }
        return 0; // Default
    }

    /**
     * Set the tilt value for a specific bar
     * 
     * @param barIndex The bar index (0-15)
     * @param value    The tilt value (-7 to 7)
     */
    public void setTiltValueForBar(int barIndex, int value) {
        if (barIndex >= 0 && barIndex < harmonicTiltValues.size()) {
            harmonicTiltValues.set(barIndex, value);

            // If this is the current bar, update the current tilt
            if (barIndex == getCurrentBar()) {
                setCurrentTilt(value);
            }
        }
    }

    /**
     * Get the current bar index from the sequencer state
     */
    private int getCurrentBar() {
        // Implementation depends on how you track the current bar
        // This might come from the TimingBus or other timing mechanism
        return (int) ((getCurrentStep() / 16) % 16);
    }

    public void setNoteEventPublisher(Consumer<NoteEvent> publisher) {
        this.noteEventPublisher = publisher;
    }

    /**
     * Set the next pattern to automatically switch to when the current pattern
     * completes
     * 
     * @param patternId The ID of the next pattern, or null to disable automatic
     *                  switching
     */
    public void setNextPatternId(Long patternId) {
        this.nextPatternId = patternId;
        logger.info("Set next melodic pattern ID: {}", patternId);
    }

    /**
     * Get the next pattern ID that will be loaded when the current pattern
     * completes
     * 
     * @return The next pattern ID or null if no pattern is queued
     */
    public Long getNextPatternId() {
        return nextPatternId;
    }

    /**
     * Creates a new melodic sequencer
     */
    public MelodicSequencer() {
        // Initialize pattern data with default values
        initializePatternData();

        // Initialize note properties (NEW)
        initializeNote();

        // Initialize harmonic tilt values
        initializeHarmonicTiltValues();

        // Register with CommandBus
        CommandBus.getInstance().register(this);

        TimingBus.getInstance().register(this);

        // Initialize the quantizer with default settings
        updateQuantizer();

        logger.info("MelodicSequencer initialized and registered with CommandBus");
    }

    public MelodicSequencer(Integer channel) {
        this();
        this.channel = channel; // Set the channel for this instance

        // Initialize tilt values
        initializeHarmonicTiltValues();
    }

    /**
     * Creates a new melodic sequencer with ID and loads first sequence if available
     */
    public MelodicSequencer(Integer id, Integer channel) {
        this.id = id;
        this.channel = channel;

        // Initialize pattern data with default values
        initializePatternData();
        
        // Initialize harmonic tilt values
        initializeHarmonicTiltValues();
        
        // Register with CommandBus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);
        
        // Initialize the quantizer with default settings
        updateQuantizer();
        
        // Wait for session to be available before initializing player
        CommandBus.getInstance().publish(
            Commands.REQUEST_ONE_TIME_NOTIFICATION,
            this,
            new RequestForOneTimeNotification(Commands.SYSTEM_READY, () -> {
                // Initialize n------ote properties after system is ready
                initializeNote();
                
                // Load first sequence if available (or create new one)
                if (id != null) {
                    loadFirstSequence();
                }
                
                logger.info("MelodicSequencer {} fully initialized after system ready", id);
            })
        );

        logger.info("MelodicSequencer {} registered with CommandBus, waiting for system ready", id);
    }

    /**
     * Initialize the Note object with proper InstrumentWrapper and Session integration
     */
    private void initializeNote() {
        // Don't directly create with RedisService.getInstance().newNote()
        
        // Get the current session from SessionManager
        Session session = SessionManager.getInstance().getActiveSession();
        if (session == null) {
            logger.error("Cannot initialize player - no active session");
            return;
        }
        
        // Check if a player already exists for this sequencer in the session
        Player existingPlayer = findExistingPlayerForSequencer(session);
        
        if (existingPlayer != null) {
            // Use existing player
            logger.info("Using existing player {} for sequencer {}", existingPlayer.getId(), id);
            player = existingPlayer;
            
            // Update channel if needed
            if (player.getChannel() != channel) {
                player.setChannel(channel);
                // Save the player through PlayerManager
                PlayerManager.getInstance().savePlayerProperties(player);
            }
        } else {
            // Create new player through PlayerManager
            logger.info("Creating new player for sequencer {}", id);
            
            // Get an instrument from InstrumentManager
            InstrumentWrapper instrument = getDefaultInstrument();
            
            // Create a Note player with proper name
            String playerName = "Melody " + (id != null ? id : "");
            player = new Note(playerName, session, instrument, 60, null);
            
            // Configure base properties
            player.setMinVelocity(60);
            player.setMaxVelocity(127);
            player.setLevel(100);
            player.setChannel(channel);
            
            // Associate player with this sequencer
            player.setOwner(this);
            
            // Generate an ID for the player if needed
            if (player.getId() == null) {
                player.setId(RedisService.getInstance().getNextPlayerId());
            }
            
            // Add to session
            session.getPlayers().add(player);
            
            // Save through PlayerManager
            PlayerManager.getInstance().savePlayerProperties(player);
            
            logger.info("Created new player {} for melodic sequencer {}", player.getId(), id);
        }
        
        // Always initialize the instrument once player is set up
        if (player.getInstrument() == null || player.isUsingInternalSynth()) {
            initializeInternalInstrument(player);
        }
    }

    /**
     * Initialize an internal synthesizer instrument for a player
     * Used when a player doesn't have an instrument or is using internal synth
     *
     * @param player The player to initialize with internal instrument
     */
    private void initializeInternalInstrument(Player player) {
        if (player == null) {
            logger.warn("Cannot initialize internal instrument for null player");
            return;
        }

        try {
            // Try to get an internal instrument from the manager
            InstrumentManager manager = InstrumentManager.getInstance();
            InstrumentWrapper internalInstrument = null;
            
            // First try to find an existing internal instrument for this channel
            for (InstrumentWrapper instrument : manager.getCachedInstruments()) {
                if (Boolean.TRUE.equals(instrument.getInternal()) && 
                    instrument.getChannel() != null && 
                    instrument.getChannel() == player.getChannel()) {
                    internalInstrument = instrument;
                    logger.info("Found existing internal instrument for channel {}", player.getChannel());
                    break;
                }
            }
            
            // If no instrument found, create a new one
            if (internalInstrument == null) {
                internalInstrument = new InstrumentWrapper(
                    "Internal Synth", 
                    null, 
                    player.getChannel()
                );
                internalInstrument.setInternal(true);
                internalInstrument.setDeviceName("Gervill");
                internalInstrument.setSoundbankName("Default");
                internalInstrument.setBankIndex(0);
                internalInstrument.setCurrentPreset(player.getPreset() != null ? player.getPreset() : player.getChannel());  // Default to piano
                internalInstrument.setId(9985L + player.getChannel());
                
                // Register with instrument manager
                manager.updateInstrument(internalInstrument);
                logger.info("Created new internal instrument for channel {}", player.getChannel());
            }
            
            // Assign instrument to player
            player.setInstrument(internalInstrument);
            player.setUsingInternalSynth(true);
            player.setPreset(0); // Piano
            
            // Save the player
            PlayerManager.getInstance().savePlayerProperties(player);
            
            logger.info("Player {} initialized with internal instrument", player.getId());
            
            // Send program change to actually set the instrument sound
            initializeInstrument();
            
        } catch (Exception e) {
            logger.error("Failed to initialize internal instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Find a player in the session that belongs to this sequencer
     */
    private Player findExistingPlayerForSequencer(Session session) {
        if (session == null || id == null) {
            return null;
        }
        
        // Look for a player that's associated with this sequencer
        for (Player p : session.getPlayers()) {
            if (p instanceof Note && 
                p.getOwner() != null && 
                p.getOwner() instanceof MelodicSequencer && 
                ((MelodicSequencer) p.getOwner()).getId() != null &&
                ((MelodicSequencer) p.getOwner()).getId().equals(id)) {
                
                return p;
            }
        }
        
        return null;
    }

    /**
     * Get a default instrument for melodic sequencing
     */
    private InstrumentWrapper getDefaultInstrument() {
        // Create with null device (indicates internal synth)
        InstrumentWrapper internalInstrument = new InstrumentWrapper(
            "Internal Synth", 
            null,  // Explicitly pass null for device
            channel
        );
        
        // Configure as internal instrument
        internalInstrument.setInternal(true);
        internalInstrument.setDeviceName("Gervill");
        internalInstrument.setSoundbankName("Default");
        internalInstrument.setBankIndex(0);
        internalInstrument.setCurrentPreset(0);  // Default to piano
        internalInstrument.setId(9985L + channel);
        
        // Register with manager
        InstrumentManager.getInstance().updateInstrument(internalInstrument);
        
        return internalInstrument;
    }

    /**
     * Initialize pattern data arrays with default values
     */
    private void initializePatternData() {
        activeSteps = new ArrayList<>(patternLength);
        noteValues = new ArrayList<>(patternLength);
        velocityValues = new ArrayList<>(patternLength);
        gateValues = new ArrayList<>(patternLength);
        probabilityValues = new ArrayList<>(patternLength); // Add this
        nudgeValues = new ArrayList<>(patternLength); // Add this

        // Fill with default values
        for (int i = 0; i < patternLength; i++) {
            activeSteps.add(false); // All steps off by default
            noteValues.add(60); // Middle C
            velocityValues.add(100); // Medium-high velocity
            gateValues.add(50); // 50% gate time
            probabilityValues.add(100); // 100% probability by default
            nudgeValues.add(0); // No timing nudge by default
        }
    }

    /**
     * Process a timing tick and play notes through the Note's InstrumentWrapper
     *
     * @param tick Current tick count
     */
    public void processTick(Long tick) {
        if (!isPlaying || tick == null) {
            return;
        }

        tickCounter = tick;

        // IMPORTANT: Use timing division to calculate tick interval
        // rather than using a fixed value of 24
        int ticksForDivision = timingDivision.getTicksPerBeat();

        // Make sure we have a valid minimum value
        if (ticksForDivision <= 0) {
            ticksForDivision = 24; // Emergency fallback
        }

        // Check if it's time for the next step using the timing division
        if (tick % ticksForDivision == 0) {
            // Get previous step for highlighting updates
            int prevStep = currentStep;

            // Calculate the next step based on the current direction
            calculateNextStep();

            // Notify listeners of step update
            if (stepUpdateListener != null) {
                stepUpdateListener.accept(new StepUpdateEvent(prevStep, currentStep));
            }

            // Trigger the note for the current step
            triggerNote(currentStep);
        }
    }

    /**
     * Calculate the previous step based on current direction
     */
    @SuppressWarnings("unused")
    private int getPreviousStep() {
        switch (direction) {
            case FORWARD:
                return (currentStep + patternLength - 1) % patternLength;
            case BACKWARD:
                return (currentStep + 1) % patternLength;
            case BOUNCE:
                // For bounce, it depends on the current bounce direction
                if (bounceDirection > 0) {
                    return currentStep > 0 ? currentStep - 1 : 0;
                } else {
                    return currentStep < patternLength - 1 ? currentStep + 1 : patternLength - 1;
                }
            case RANDOM:
            default:
                return currentStep; // For random, just use current position
        }
    }

    /**
     * Play a note directly using the Note's InstrumentWrapper
     *
     * @param midiNote The MIDI note number to play
     * @param velocity The velocity (volume) of the note
     * @param duration The duration in milliseconds
     */
    @SuppressWarnings("unused")
    private void playNote(int midiNote, int velocity, int duration) {
        if (player == null || player.getInstrument() == null) {
            logger.warn("Cannot play note directly - note or instrument is null");
            return;
        }

        try {
            final InstrumentWrapper instrument = player.getInstrument();
            final int channel = player.getChannel();

            // Log what we're about to play
            logger.info("Playing note directly: note={}, vel={}, duration={}, channel={}, instrument={}",
                    midiNote, velocity, duration, channel, instrument.getName());

            // Set the channel's current program if needed
            if (player.getPreset() != null) {
                // Send bank select messages if a bank is specified
                if (instrument.getBankIndex() != null && instrument.getBankIndex() > 0) {
                    instrument.controlChange(channel, 0, 0); // Bank MSB
                    instrument.controlChange(channel, 32, instrument.getBankIndex()); // Bank LSB
                }

                // Send program change
                instrument.programChange(channel, player.getPreset().intValue(), 0);
            }

            // Play the note - ERROR IS HERE
            instrument.noteOn(channel, midiNote, player.getLevel()); // Correct - use instrument directly

            // Schedule note off after the specified duration
            final int noteToStop = midiNote; // Capture for use in lambda

            new Thread(() -> {
                try {
                    Thread.sleep(duration);
                    instrument.noteOff(channel, noteToStop, 0);
                } catch (Exception e) {
                    logger.error("Error in note thread: {}", e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error playing note directly: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate the next step based on the current direction
     */
    private void calculateNextStep() {
        int oldStep = currentStep;
        boolean patternCompleted = false;

        switch (direction) {
            case FORWARD -> {
                currentStep++;

                // Check if we've reached the end of the pattern
                if (currentStep >= patternLength) {
                    currentStep = 0;
                    patternCompleted = true;

                    // Handle pattern switching if enabled
                    if (nextPatternId != null) {
                        Long currentId = melodicSequenceId;
                        // Load the next pattern
                        if (RedisService.getInstance().findMelodicSequenceById(nextPatternId, id) != null) {
                            // Store current playback state
                            boolean wasPlaying = isPlaying;

                            // Load new pattern
                            RedisService.getInstance().applyMelodicSequenceToSequencer(
                                    RedisService.getInstance().findMelodicSequenceById(nextPatternId, id),
                                    this);

                            // Restore playback state
                            isPlaying = wasPlaying;

                            // Notify about pattern switch
                            CommandBus.getInstance().publish(
                                    Commands.MELODIC_PATTERN_SWITCHED,
                                    this,
                                    new PatternSwitchEvent(currentId, nextPatternId));

                            // Clear the next pattern ID (one-shot behavior)
                            nextPatternId = null;
                        }
                    }

                    // Generate a new pattern if latch is enabled
                    if (latchEnabled) {
                        // Get the current octave range setting (default to 2 if unset)
                        int octaveRange = 2;

                        // Use a consistent density (adjust as needed)
                        int density = 50;

                        // Generate a new pattern
                        generatePattern(octaveRange, density);

                        // Notify UI of pattern change
                        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);

                        logger.info("Latch mode: Generated new pattern at cycle end");
                    }

                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }

            // Handle other direction cases similarly
            case BACKWARD -> {
                currentStep--;

                if (currentStep < 0) {
                    currentStep = patternLength - 1;
                    patternCompleted = true;

                    // Handle pattern switching if enabled
                    if (nextPatternId != null) {
                        Long currentId = melodicSequenceId;
                        // Load the next pattern
                        if (RedisService.getInstance().findMelodicSequenceById(nextPatternId, id) != null) {
                            // Store current playback state
                            boolean wasPlaying = isPlaying;

                            // Load new pattern
                            RedisService.getInstance().applyMelodicSequenceToSequencer(
                                    RedisService.getInstance().findMelodicSequenceById(nextPatternId, id),
                                    this);

                            // Restore playback state
                            isPlaying = wasPlaying;

                            // Notify about pattern switch
                            CommandBus.getInstance().publish(
                                    Commands.MELODIC_PATTERN_SWITCHED,
                                    this,
                                    new PatternSwitchEvent(currentId, nextPatternId));

                            // Clear the next pattern ID (one-shot behavior)
                            nextPatternId = null;
                        }
                    }

                    // Generate a new pattern if latch is enabled
                    if (latchEnabled) {
                        int octaveRange = 2;
                        int density = 50;
                        generatePattern(octaveRange, density);
                        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
                        logger.info("Latch mode: Generated new pattern at cycle end");
                    }

                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }

            case BOUNCE -> {
                // Update step counter according to bounce direction
                currentStep += bounceDirection;

                // Check if we need to change bounce direction
                if (currentStep <= 0 || currentStep >= patternLength - 1) {
                    bounceDirection *= -1;

                    // At pattern end, generate new pattern if latch enabled
                    if (currentStep <= 0 || currentStep >= patternLength - 1) {
                        if (latchEnabled) {
                            int octaveRange = 2;
                            int density = 50;
                            generatePattern(octaveRange, density);
                            CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
                            logger.info("Latch mode: Generated new pattern at cycle end");
                        }
                    }

                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }

            case RANDOM -> {
                // Random doesn't have a clear cycle end, so we'll consider
                // hitting step 0 as the "cycle end" for latch purposes
                int priorStep = currentStep;

                // Generate random step
                currentStep = (int) (Math.random() * patternLength);

                // If we randomly hit step 0 and we weren't at step 0 before
                if (currentStep == 0 && priorStep != 0) {
                    if (latchEnabled) {
                        int octaveRange = 2;
                        int density = 50;
                        generatePattern(octaveRange, density);
                        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
                        logger.info("Latch mode: Generated new pattern at random cycle point");
                    }
                }
            }
        }

        // Notify step listeners about the step change
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(oldStep, currentStep));
        }
    }

    /**
     * Triggers a note for the specified step, applying probability and nudge.
     * 
     * @param stepIndex The step index
     */
    private void triggerNote(int stepIndex) {
        // Only trigger if step is active
        if (stepIndex >= 0 && stepIndex < activeSteps.size() && activeSteps.get(stepIndex)) {
            // Get step parameters
            int[] note = { getNoteValue(stepIndex) };
            int velocity = getVelocityValue(stepIndex);
            int gate = getGateValue(stepIndex);
            int probability = getProbabilityValue(stepIndex);
            int nudge = getNudgeValue(stepIndex);

            // Apply swing to even-numbered steps (odd indices in 0-indexed array)
            if (swingEnabled && stepIndex % 2 == 1) {
                // Calculate swing amount based on percentage
                int swingAmount = calculateSwingAmount();
                nudge += swingAmount;
            }

            // Apply scale quantization if enabled
            if (quantizeEnabled) {
                note[0] = quantizeNote(note[0]);
            }

            // Apply octave shift and tilt
            note[0] = applyOctaveShift(note[0]);
            note[0] = applyTilt(note[0]);

            // Check probability - only play if random number is less than probability
            if (Math.random() * 100 < probability) {
                // Create the note event
                final NoteEvent noteEvent = new NoteEvent(note[0], velocity, gate);

                // Apply nudge if specified
                if (nudge > 0) {
                    final int finalNote = note[0];
                    final int finalVel = velocity;

                    SHARED_NOTE_SCHEDULER.schedule(() -> {
                        player.noteOn(finalNote, finalVel, gate);
                        // Other logic...
                    }, nudge, TimeUnit.MILLISECONDS);
                } else {
                    player.noteOn(note[0], velocity, gate);
                    // Other logic...
                }

                // Add this at the end:
                if (noteEventPublisher != null && velocity > 0) {
                    NoteEvent event = new NoteEvent(note[0], velocity, gate);
                    noteEventPublisher.accept(event);
                }
            } else {
                // Note skipped due to probability
                logger.debug("Step {} skipped due to probability: {}/100", stepIndex, probability);
            }
        }
    }

    /**
     * Apply tilt to a MIDI note value
     *
     * @param noteValue MIDI note value to shift
     * @return Tilted note value
     */
    private int applyTilt(int noteValue) {
        int shiftedValue = noteValue + getCurrentTilt();
        // Keep notes in MIDI range (0-127)
        return Math.max(0, Math.min(127, shiftedValue));
    }

    /**
     * Get the root note of the melodic sequencer
     *
     * @return MIDI note value (0-127)
     */
    public Integer getRootNote() {
        return player != null ? player.getRootNote() : 60;
    }

    /**
     * Set the root note of the melodic sequencer
     *
     * @param rootNote MIDI note value (0-127)
     */
    public void setRootNote(Integer rootNote) {
        if (player != null && rootNote != null) {
            player.setRootNote(rootNote);
        }
    }

    /**
     * Get the name of the melody
     *
     * @return The melody name
     */
    public String getName() {
        return player != null ? player.getName() : "Melody";
    }

    /**
     * Set the name of the melody
     *
     * @param name The new melody name
     */
    public void setName(String name) {
        if (player != null && name != null) {
            player.setName(name);
        }
    }

    /**
     * Get the velocity level
     *
     * @return Velocity level (0-127)
     */
    public Long getLevel() {
        return player != null ? player.getLevel() : 100L;
    }

    /**
     * Set the velocity level
     *
     * @param level Velocity level (0-127)
     */
    public void setLevel(int level) {
        if (player != null && level >= 0 && level <= 127) {
            player.setLevel(level);
        }
    }

    /**
     * Decrement the octave shift by 1
     */
    public void decrementOctaveShift() {
        octaveShift--;
        logger.info("Octave shift decreased to {}", octaveShift);
    }

    /**
     * Increment the octave shift by 1
     */
    public void incrementOctaveShift() {
        octaveShift++;
        logger.info("Octave shift increased to {}", octaveShift);
    }

    /**
     * Clear the entire pattern
     */
    public void clearPattern() {
        // Set all steps to inactive
        for (int i = 0; i < patternLength; i++) {
            activeSteps.set(i, false);
        }

        // Notify listeners about the pattern update
        notifyPatternUpdated();
    }

    /**
     * Generate a random pattern with the given parameters
     *
     * @param octaveRange The number of octaves to span (1-4)
     * @param density     Percentage of steps to activate (1-100)
     */
    public void generatePattern(int octaveRange, int density) {
        // Clear existing pattern
        clearPattern();

        // Calculate base note from root note
        int baseNote = 60; // Middle C by default

        // Calculate number of steps to activate
        int stepsToActivate = patternLength * density / 100;
        stepsToActivate = Math.max(1, Math.min(stepsToActivate, patternLength));

        // Always activate the first step
        if (activeSteps.size() > 0) {
            activeSteps.set(0, true);
        }
        if (noteValues.size() > 0) {
            noteValues.set(0, baseNote);
        }
        if (velocityValues.size() > 0) {
            velocityValues.set(0, 100);
        }
        if (gateValues.size() > 0) {
            gateValues.set(0, 75);
        }

        stepsToActivate--;

        // Activate random remaining steps
        for (int i = 0; i < stepsToActivate; i++) {
            // Find an inactive step
            int step;
            do {
                step = (int) (Math.random() * patternLength);
            } while (step < activeSteps.size() && activeSteps.get(step));

            // Activate it if valid index
            if (step < activeSteps.size()) {
                activeSteps.set(step, true);
            }

            // Generate a random note within the octave range if valid index
            if (step < noteValues.size()) {
                int noteOffset = (int) (Math.random() * (12 * octaveRange));
                noteValues.set(step, baseNote + noteOffset);
            }

            // Random velocity (60-127) if valid index
            if (step < velocityValues.size()) {
                velocityValues.set(step, 60 + (int) (Math.random() * 68));
            }

            // Random gate time (25-100) if valid index
            if (step < gateValues.size()) {
                gateValues.set(step, 25 + (int) (Math.random() * 76));
            }
        }

        logger.info("Generated pattern: octaves={}, density={}", octaveRange, density);
    }

    /**
     * Quantize a note to the current scale
     *
     * @param note The MIDI note number to quantize
     * @return The quantized note number
     */
    public int quantizeNote(int note) {
        if (quantizer == null) {
            updateQuantizer(); // Ensure we have a quantizer
        }

        try {
            return quantizer.quantizeNote(note);
        } catch (Exception e) {
            logger.error("Error quantizing note {}: {}", note, e.getMessage());
            return note; // Return original note if quantization fails
        }
    }

    /**
     * Update the quantizer with current scale settings
     */
    public void updateQuantizer() {
        // Create Boolean array representing which notes are in the scale
        scaleNotes = createScaleArray(selectedRootNote, scale);

        // Create new quantizer with the scale
        quantizer = new Quantizer(scaleNotes);

        logger.info("Quantizer updated with root note {} and scale {}", selectedRootNote, scale);
    }

    /**
     * Create a Boolean array representing which notes are in the scale
     */
    private Boolean[] createScaleArray(String rootNote, String scaleName) {
        Boolean[] result = new Boolean[12];
        for (int i = 0; i < 12; i++) {
            result[i] = false;
        }

        // Get root note index using Scale class
        int rootIndex = Scale.getRootNoteIndex(rootNote);

        // Get scale pattern from Scale class - FIX: Use a proper int[] array as default
        int[] pattern = Scale.SCALE_PATTERNS.getOrDefault(scaleName,
                new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });

        // Apply pattern to root note
        for (int offset : pattern) {
            int noteIndex = (rootIndex + offset) % 12;
            result[noteIndex] = true;
        }

        return result;
    }

    /**
     * Set the root note for scale quantization
     *
     * @param rootNote Root note name (C, C#, D, etc.)
     */
    public void setRootNote(String rootNote) {
        this.selectedRootNote = rootNote;
        updateQuantizer();

        // IMPORTANT: Re-quantize all notes in the pattern when root note changes
        if (quantizeEnabled && quantizer != null) {
            for (int i = 0; i < noteValues.size(); i++) {
                noteValues.set(i, quantizer.quantizeNote(noteValues.get(i)));
            }

            // Notify listeners that pattern has been updated
            CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
        }

        logger.info("Root note set to {} and notes re-quantized", rootNote);
    }

    /**
     * Set the scale for note quantization
     *
     * @param scale Scale name
     */
    public void setScale(String scale) {
        this.scale = scale;
        updateQuantizer();

        // IMPORTANT: Re-quantize all notes in the pattern when scale changes
        if (quantizeEnabled && quantizer != null) {
            for (int i = 0; i < noteValues.size(); i++) {
                noteValues.set(i, quantizer.quantizeNote(noteValues.get(i)));
            }

            // Notify listeners that pattern has been updated
            CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
        }

        logger.info("Scale set to {} and notes re-quantized", scale);
    }

    /**
     * Apply octave shift to a MIDI note value
     *
     * @param noteValue MIDI note value to shift
     * @return Shifted note value
     */
    public int applyOctaveShift(int noteValue) {
        int shiftedValue = noteValue + (octaveShift * 12);
        // Keep notes in MIDI range (0-127)
        return Math.max(0, Math.min(127, shiftedValue));
    }

    /**
     * Enable or disable quantization
     *
     * @param enabled True to enable, false to disable
     */
    public void setQuantizeEnabled(boolean enabled) {
        this.quantizeEnabled = enabled;
        logger.info("Quantization {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Check if quantization is enabled
     *
     * @return True if quantization is enabled
     */
    public boolean isQuantizeEnabled() {
        return quantizeEnabled;
    }

    /**
     * Sets all data for a specific step.
     * 
     * @param stepIndex   The step index
     * @param active      Whether the step is active
     * @param note        The note value
     * @param velocity    The velocity value
     * @param gate        The gate value
     * @param probability The probability value (0-100)
     * @param nudge       The nudge value in milliseconds
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability,
            int nudge) {
        // Set active state
        while (activeSteps.size() <= stepIndex) {
            activeSteps.add(false);
        }
        activeSteps.set(stepIndex, active);

        // Set note value
        while (noteValues.size() <= stepIndex) {
            noteValues.add(60); // Default to middle C
        }
        noteValues.set(stepIndex, note);

        // Set velocity value
        while (velocityValues.size() <= stepIndex) {
            velocityValues.add(100); // Default velocity
        }
        velocityValues.set(stepIndex, velocity);

        // Set gate value
        while (gateValues.size() <= stepIndex) {
            gateValues.add(50); // Default gate
        }
        gateValues.set(stepIndex, gate);

        // Set probability value
        setProbabilityValue(stepIndex, probability);

        // Set nudge value
        setNudgeValue(stepIndex, nudge);
    }

    // Keep the old method for backward compatibility
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        // Call the new method with default probability and nudge values
        setStepData(stepIndex, active, note, velocity, gate, 100, 0);
    }

    /**
     * Reset the sequencer to start position
     */
    public void reset() {
        // Initialize step counter based on direction
        currentStep = direction == Direction.FORWARD ? 0 : patternLength - 1;
        tickCounter = 0;

        // Notify UI that we've reset
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(-1, currentStep));
        }

        logger.debug("Melodic sequencer reset to step {}", currentStep);
    }

    /**
     * Start playback of the sequencer
     */
    public void start() {
        if (isPlaying) {
            return;
        }
        
        // Make sure player has instrument before starting
        ensurePlayerHasInstrument();
        
        // Then initialize MIDI instrument
        initializeInstrument();
        
        isPlaying = true;
        
        // Reset position if needed
        if (currentStep >= patternLength) {
            reset();
        }
        
        logger.info("Melodic sequencer {} started playback", id);
        
        // Notify listeners
        CommandBus.getInstance().publish(Commands.SEQUENCER_STATE_CHANGED, this, 
            Map.of("sequencerId", id, "state", "started"));
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (isPlaying) {
            isPlaying = false;

            // Notify via command bus that we've stopped
            // CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this);
            logger.info("Melodic sequencer playback stopped");
        }
    }

    /**
     * Set the timing division for step playback
     *
     * @param division The timing division to use
     */
    public void setTimingDivision(TimingDivision division) {
        if (division != null) {
            this.timingDivision = division;

            logger.info("Timing division set to {}", division);
        }
    }

    /**
     * Get the current timing division
     *
     * @return The current timing division
     */
    public TimingDivision getTimingDivision() {
        return timingDivision;
    }

    /**
     * Update master tempo from session
     */
    public void updateMasterTempo(int sessionTicksPerBeat) {
        this.masterTempo = sessionTicksPerBeat;
        logger.info("Updated master tempo to {}", masterTempo);
    }

    /**
     * Implementation of IBusListener interface
     */
    @Override
    public void onAction(Command action) {
        if (action == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    if (update.tick() != null) {
                        processTick(update.tick());
                    }
                }
            }

            case Commands.TRANSPORT_START -> {
                logger.info("Received TRANSPORT_START command");
                // Sync with master tempo when starting
                masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                logger.info("Master tempo set to {} ticks per beat", masterTempo);
                start();
            }

            case Commands.TRANSPORT_STOP -> {
                logger.info("Received TRANSPORT_STOP command");
                stop();
            }

            // Handle other commands as needed
        }
    }

    /**
     * Get the current step counter value
     *
     * @return Current step position
     */
    public int getCurrentStep() {
        return currentStep;
    }

    // Getter methods for sequence data
    public List<Boolean> getActiveSteps() {
        return new ArrayList<>(activeSteps);
    }

    public List<Integer> getNoteValues() {
        return new ArrayList<>(noteValues);
    }

    public List<Integer> getVelocityValues() {
        return new ArrayList<>(velocityValues);
    }

    public List<Integer> getGateValues() {
        return new ArrayList<>(gateValues);
    }

    /**
     * Check if a specific step is active in the sequence
     *
     * @param stepIndex The index of the step to check
     * @return true if the step is active, false otherwise
     */
    public boolean isStepActive(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < activeSteps.size()) {
            return activeSteps.get(stepIndex);
        }
        return false;
    }

    /**
     * Gets the note value for a specific step.
     * 
     * @param stepIndex The step index
     * @return MIDI note value (0-127)
     */
    public int getNoteValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < noteValues.size()) {
            return noteValues.get(stepIndex);
        }
        return 60; // Default to middle C if step is out of bounds
    }

    /**
     * Gets the velocity value for a specific step.
     * 
     * @param stepIndex The step index
     * @return Velocity value (0-127)
     */
    public int getVelocityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < velocityValues.size()) {
            return velocityValues.get(stepIndex);
        }
        return 100; // Default velocity if step is out of bounds
    }

    /**
     * Gets the gate value for a specific step.
     * 
     * @param stepIndex The step index
     * @return Gate value (0-100)
     */
    public int getGateValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < gateValues.size()) {
            return gateValues.get(stepIndex);
        }
        return 50; // Default gate time if step is out of bounds
    }

    /**
     * Calculate note duration in milliseconds based on gate length percentage and
     * current tempo
     * 
     * @param gateLength Gate length (0-100 percent)
     * @return Duration in milliseconds
     */
    @SuppressWarnings("unused")
    private int calculateNoteDuration(int gateLength) {
        // Safety check for gate length
        int safeGateLength = Math.max(1, Math.min(100, gateLength));

        // Calculate beat duration in milliseconds (60000ms / BPM)
        // Using fixed tempo calculation that works with 24 ticks per step
        double beatsPerMinute = 120.0; // Default BPM

        if (masterTempo > 0) {
            // Calculate BPM from masterTempo (typically 96 PPQN)
            beatsPerMinute = 60.0 * (masterTempo / 24.0);
        }

        int beatDurationMs = (int) (60000 / beatsPerMinute);

        // Apply gate percentage to get note duration
        return (beatDurationMs * safeGateLength) / 100;
    }

    /**
     * Gets the probability value for a specific step.
     * 
     * @param stepIndex The step index
     * @return Probability value (0-100)
     */
    public int getProbabilityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < probabilityValues.size()) {
            return probabilityValues.get(stepIndex);
        }
        return 100; // Default to 100% if step is out of bounds
    }

    /**
     * Sets the probability value for a specific step.
     * 
     * @param stepIndex   The step index
     * @param probability Probability value (0-100)
     */
    public void setProbabilityValue(int stepIndex, int probability) {
        // Ensure probability is in valid range
        probability = Math.max(0, Math.min(100, probability));

        // Ensure step index is valid
        if (stepIndex >= 0) {
            // Expand lists if needed
            while (probabilityValues.size() <= stepIndex) {
                probabilityValues.add(100);
            }
            probabilityValues.set(stepIndex, probability);
        }
    }

    /**
     * Gets the nudge value for a specific step.
     * 
     * @param stepIndex The step index
     * @return Nudge value in milliseconds
     */
    public int getNudgeValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < nudgeValues.size()) {
            return nudgeValues.get(stepIndex);
        }
        return 0; // Default to no nudge if step is out of bounds
    }

    /**
     * Sets the nudge value for a specific step.
     * 
     * @param stepIndex The step index
     * @param nudge     Nudge value in milliseconds
     */
    public void setNudgeValue(int stepIndex, int nudge) {
        // Ensure nudge is positive
        nudge = Math.max(0, nudge);

        // Ensure step index is valid
        if (stepIndex >= 0) {
            // Expand lists if needed
            while (nudgeValues.size() <= stepIndex) {
                nudgeValues.add(0);
            }
            nudgeValues.set(stepIndex, nudge);
        }
    }

    /**
     * Gets all probability values.
     * 
     * @return List of probability values
     */
    public List<Integer> getProbabilityValues() {
        return Collections.unmodifiableList(probabilityValues);
    }

    /**
     * Gets all nudge values.
     * 
     * @return List of nudge values
     */
    public List<Integer> getNudgeValues() {
        return Collections.unmodifiableList(nudgeValues);
    }

    /**
     * Push the sequence forward by one step, wrapping the last element to the first
     * position
     */
    public void pushForward() {
        if (patternLength <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate active steps
        if (activeSteps.size() > 0) {
            boolean lastValue = activeSteps.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                activeSteps.set(i, activeSteps.get(i - 1));
            }
            activeSteps.set(0, lastValue);
        }

        // Rotate note values
        if (noteValues.size() > 0) {
            int lastValue = noteValues.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                noteValues.set(i, noteValues.get(i - 1));
            }
            noteValues.set(0, lastValue);
        }

        // Rotate velocity values
        if (velocityValues.size() > 0) {
            int lastValue = velocityValues.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                velocityValues.set(i, velocityValues.get(i - 1));
            }
            velocityValues.set(0, lastValue);
        }

        // Rotate gate values
        if (gateValues.size() > 0) {
            int lastValue = gateValues.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                gateValues.set(i, gateValues.get(i - 1));
            }
            gateValues.set(0, lastValue);
        }

        // Rotate probability values
        if (probabilityValues.size() > 0) {
            int lastValue = probabilityValues.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                probabilityValues.set(i, probabilityValues.get(i - 1));
            }
            probabilityValues.set(0, lastValue);
        }

        // Rotate nudge values
        if (nudgeValues.size() > 0) {
            int lastValue = nudgeValues.get(patternLength - 1);
            for (int i = patternLength - 1; i > 0; i--) {
                nudgeValues.set(i, nudgeValues.get(i - 1));
            }
            nudgeValues.set(0, lastValue);
        }

        logger.info("Sequence pushed forward by one step");

        // Notify listeners that the pattern has been updated
        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
    }

    /**
     * Pull the sequence backward by one step, wrapping the first element to the
     * last position
     */
    public void pullBackward() {
        if (patternLength <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate active steps
        if (activeSteps.size() > 0) {
            boolean firstValue = activeSteps.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                activeSteps.set(i, activeSteps.get(i + 1));
            }
            activeSteps.set(patternLength - 1, firstValue);
        }

        // Rotate note values
        if (noteValues.size() > 0) {
            int firstValue = noteValues.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                noteValues.set(i, noteValues.get(i + 1));
            }
            noteValues.set(patternLength - 1, firstValue);
        }

        // Rotate velocity values
        if (velocityValues.size() > 0) {
            int firstValue = velocityValues.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                velocityValues.set(i, velocityValues.get(i + 1));
            }
            velocityValues.set(patternLength - 1, firstValue);
        }

        // Rotate gate values
        if (gateValues.size() > 0) {
            int firstValue = gateValues.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                gateValues.set(i, gateValues.get(i + 1));
            }
            gateValues.set(patternLength - 1, firstValue);
        }

        // Rotate probability values
        if (probabilityValues.size() > 0) {
            int firstValue = probabilityValues.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                probabilityValues.set(i, probabilityValues.get(i + 1));
            }
            probabilityValues.set(patternLength - 1, firstValue);
        }

        // Rotate nudge values
        if (nudgeValues.size() > 0) {
            int firstValue = nudgeValues.get(0);
            for (int i = 0; i < patternLength - 1; i++) {
                nudgeValues.set(i, nudgeValues.get(i + 1));
            }
            nudgeValues.set(patternLength - 1, firstValue);
        }

        logger.info("Sequence pulled backward by one step");

        // Notify listeners that the pattern has been updated
        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
    }

    /**
     * Rotates the pattern one step to the left (backward)
     */
    public void rotatePatternLeft() {
        if (patternLength <= 1)
            return;

        // Ensure all lists have adequate size
        while (activeSteps.size() < patternLength)
            activeSteps.add(false);
        while (noteValues.size() < patternLength)
            noteValues.add(60);
        while (velocityValues.size() < patternLength)
            velocityValues.add(100);
        while (gateValues.size() < patternLength)
            gateValues.add(50);
        while (probabilityValues.size() < patternLength)
            probabilityValues.add(100);
        while (nudgeValues.size() < patternLength)
            nudgeValues.add(0);

        // Store the first step's values
        boolean firstActive = activeSteps.get(0);
        int firstNote = noteValues.get(0);
        int firstVelocity = velocityValues.get(0);
        int firstGate = gateValues.get(0);
        int firstProbability = probabilityValues.get(0);
        int firstNudge = nudgeValues.get(0);

        // Shift all steps left by one position
        for (int i = 0; i < patternLength - 1; i++) {
            activeSteps.set(i, activeSteps.get(i + 1));
            noteValues.set(i, noteValues.get(i + 1));
            velocityValues.set(i, velocityValues.get(i + 1));
            gateValues.set(i, gateValues.get(i + 1));
            probabilityValues.set(i, probabilityValues.get(i + 1));
            nudgeValues.set(i, nudgeValues.get(i + 1));
        }

        // Place the saved first step values at the last position
        activeSteps.set(patternLength - 1, firstActive);
        noteValues.set(patternLength - 1, firstNote);
        velocityValues.set(patternLength - 1, firstVelocity);
        gateValues.set(patternLength - 1, firstGate);
        probabilityValues.set(patternLength - 1, firstProbability);
        nudgeValues.set(patternLength - 1, firstNudge);

        // Notify listeners about the pattern update
        notifyPatternUpdated();
    }

    /**
     * Rotates the pattern one step to the right (forward)
     */
    public void rotatePatternRight() {
        if (patternLength <= 1)
            return;

        // Ensure all lists have adequate size
        while (activeSteps.size() < patternLength)
            activeSteps.add(false);
        while (noteValues.size() < patternLength)
            noteValues.add(60);
        while (velocityValues.size() < patternLength)
            velocityValues.add(100);
        while (gateValues.size() < patternLength)
            gateValues.add(50);
        while (probabilityValues.size() < patternLength)
            probabilityValues.add(100);
        while (nudgeValues.size() < patternLength)
            nudgeValues.add(0);

        // Store the last step's values
        boolean lastActive = activeSteps.get(patternLength - 1);
        int lastNote = noteValues.get(patternLength - 1);
        int lastVelocity = velocityValues.get(patternLength - 1);
        int lastGate = gateValues.get(patternLength - 1);
        int lastProbability = probabilityValues.get(patternLength - 1);
        int lastNudge = nudgeValues.get(patternLength - 1);

        // Shift all steps right by one position
        for (int i = patternLength - 1; i > 0; i--) {
            activeSteps.set(i, activeSteps.get(i - 1));
            noteValues.set(i, noteValues.get(i - 1));
            velocityValues.set(i, velocityValues.get(i - 1));
            gateValues.set(i, gateValues.get(i - 1));
            probabilityValues.set(i, probabilityValues.get(i - 1));
            nudgeValues.set(i, nudgeValues.get(i - 1));
        }

        // Place the saved last step values at the first position
        activeSteps.set(0, lastActive);
        noteValues.set(0, lastNote);
        velocityValues.set(0, lastVelocity);
        gateValues.set(0, lastGate);
        probabilityValues.set(0, lastProbability);
        nudgeValues.set(0, lastNudge);

        // Notify listeners about the pattern update
        notifyPatternUpdated();
    }

    // For backward compatibility
    public void rotatePattern() {
        rotatePatternRight();
    }

    /**
     * Sets whether latch mode is enabled.
     * When enabled, a new pattern is generated each cycle.
     * 
     * @param enabled True to enable latch mode, false to disable
     */
    public void setLatchEnabled(boolean enabled) {
        this.latchEnabled = enabled;
        logger.info("Latch mode set to: {}", enabled);
    }

    /**
     * Checks if latch mode is enabled
     * 
     * @return True if latch mode is enabled
     */
    public boolean isLatchEnabled() {
        return latchEnabled;
    }

    /**
     * Sets a temporary note offset that applies to all notes in the current bar
     * without changing the stored pattern data
     * 
     * @param tiltValue The amount to shift notes up or down (-4 to +4 semitones)
     */
    public void setCurrentTilt(int tiltValue) {
        this.currentTilt = tiltValue;

        // Log the application of tilt
        logger.debug("Setting tilt for sequencer channel {} to {}", getChannel(), tiltValue);
    }

    /**
     * Gets the current tilt value
     * 
     * @return The current tilt value (-4 to +4)
     */
    public int getCurrentTilt() {
        return currentTilt;
    }

    /**
     * Notify listeners about the pattern update
     */
    private void notifyPatternUpdated() {
        CommandBus.getInstance().publish(Commands.PATTERN_UPDATED, this, this);
    }

    /**
     * Load the first available sequence for this sequencer
     * If no sequences exist, create a new one
     */
    public void loadFirstSequence() {
        try {
            RedisService redis = RedisService.getInstance();

            // Get the first sequence ID for this sequencer
            Long firstId = redis.getMinimumMelodicSequenceId(this.id);

            if (firstId != null) {
                // Load existing sequence
                MelodicSequenceData data = redis.findMelodicSequenceById(firstId, this.id);
                if (data != null) {
                    redis.applyMelodicSequenceToSequencer(data, this);
                    logger.info("Loaded first melodic sequence (ID: {}) for sequencer {}",
                            firstId, this.id);
                    return;
                }
            }

            // No sequence exists, create a new one
            logger.info("No melodic sequences found for sequencer {}, creating default", this.id);
            MelodicSequenceData newData = redis.newMelodicSequence(this.id);
            redis.applyMelodicSequenceToSequencer(newData, this);

            initializeInstrument();

        } catch (Exception e) {
            logger.error("Error loading first melodic sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Sets the global swing percentage
     * 
     * @param percentage Value from 25 (minimum swing) to 99 (maximum swing), 50 =
     *                   no swing
     */
    public void setSwingPercentage(int percentage) {
        // Limit to valid range
        this.swingPercentage = Math.max(MIN_SWING, Math.min(MAX_SWING, percentage));
        logger.info("Swing percentage set to: {}", swingPercentage);
    }

    /**
     * Gets the current swing percentage
     * 
     * @return The swing percentage (25-99)
     */
    public int getSwingPercentage() {
        return swingPercentage;
    }

    /**
     * Enables or disables swing
     * 
     * @param enabled True to enable swing, false to disable
     */
    public void setSwingEnabled(boolean enabled) {
        this.swingEnabled = enabled;
        logger.info("Swing enabled: {}", enabled);
    }

    /**
     * Checks if swing is enabled
     * 
     * @return True if swing is enabled
     */
    public boolean isSwingEnabled() {
        return swingEnabled;
    }

    /**
     * Calculate swing amount in milliseconds based on current tempo and timing
     * division
     * 
     * @return Swing amount in milliseconds
     */
    private int calculateSwingAmount() {
        // Get session BPM
        float bpm = SessionManager.getInstance().getActiveSession().getTempoInBPM();
        if (bpm <= 0) {
            bpm = 120; // Default fallback
        }

        // Calculate step duration in milliseconds
        float stepDurationMs = 60000f / bpm; // Duration of quarter note in ms

        // Adjust for timing division based on actual enum values
        switch (timingDivision) {
            case NORMAL -> stepDurationMs *= 1; // No change for normal timing
            case DOUBLE -> stepDurationMs /= 2; // Double time (faster)
            case HALF -> stepDurationMs *= 2; // Half time (slower)
            case QUARTER -> stepDurationMs *= 4; // Quarter time (very slow)
            case TRIPLET -> stepDurationMs *= 2.0f / 3.0f; // Triplet feel
            case QUARTER_TRIPLET -> stepDurationMs *= 4.0f / 3.0f; // Quarter note triplets
            case EIGHTH_TRIPLET -> stepDurationMs *= 1.0f / 3.0f; // Eighth note triplets
            case SIXTEENTH -> stepDurationMs *= 1.0f / 4.0f; // Sixteenth notes
            case SIXTEENTH_TRIPLET -> stepDurationMs *= 1.0f / 6.0f; // Sixteenth note triplets
            case BEBOP -> stepDurationMs *= 1; // Same as normal for swing calculations
            case FIVE_FOUR -> stepDurationMs *= 5.0f / 4.0f; // 5/4 time
            case SEVEN_EIGHT -> stepDurationMs *= 7.0f / 8.0f; // 7/8 time
            case NINE_EIGHT -> stepDurationMs *= 9.0f / 8.0f; // 9/8 time
            case TWELVE_EIGHT -> stepDurationMs *= 12.0f / 8.0f; // 12/8 time
            case SIX_FOUR -> stepDurationMs *= 6.0f / 4.0f; // 6/4 time
        }

        // Calculate swing percentage (convert from 50-75% to 0-25%)
        float swingFactor = (swingPercentage - 50) / 100f;

        // Return swing amount in milliseconds
        return (int) (stepDurationMs * swingFactor);
    }

    /**
     * Initialize the MIDI device with the current instrument settings
     * Call this before starting playback
     */
    public void initializeInstrument() {
        if (player == null || player.getInstrument() == null) {
            logger.warn("Cannot initialize MIDI instrument: Player or instrument is null");
            return;
        }

        try {
            // Get current instrument settings
            InstrumentWrapper instrument = player.getInstrument();
            int channel = player.getChannel();
            Integer preset = player.getPreset();
            
            // Make sure the channel matches our expected channel
            if (channel != this.channel) {
                player.setChannel(this.channel);
                channel = this.channel;
            }

            // Mark instrument as assigned to player
            instrument.setAssignedToPlayer(true);

            // Update the instrument in the cache
            InstrumentManager.getInstance().updateInstrument(instrument);

            if (preset != null) {
                // Apply bank select if needed
                if (instrument.getBankMSB() != 0 || instrument.getBankLSB() != 0) {
                    // Bank MSB (CC#0)
                    instrument.controlChange(channel, 0, instrument.getBankMSB());
                    // Bank LSB (CC#32)
                    instrument.controlChange(channel, 32, instrument.getBankLSB());
                    
                    logger.info("Sent bank select MSB: {}, LSB: {} on channel {}",
                        instrument.getBankMSB(), instrument.getBankLSB(), channel);
                }

                // Send program change
                instrument.programChange(channel, preset, 0);
                logger.info("Initialized instrument {} with preset {} on channel {}",
                        instrument.getName(), preset, channel);
                        
                // Save player using PlayerManager to ensure persistence
                PlayerManager.getInstance().savePlayerProperties(player);
            }
        } catch (Exception e) {
            logger.error("Error initializing instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize the player if it doesn't already have a valid instrument
     */
    public void ensurePlayerHasInstrument() {
        if (player != null && player.getInstrument() == null) {
            logger.warn("Player {} has no instrument, initializing default", player.getId());
            initializeInternalInstrument(player);
        }
    }
}

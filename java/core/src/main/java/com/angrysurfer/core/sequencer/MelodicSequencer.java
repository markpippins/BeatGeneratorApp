package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

import com.angrysurfer.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.event.StepUpdateEvent;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    public static final int[] SEQUENCER_CHANNELS = { 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15 };
    
    private int channel = 0; // Current step in the pattern

    // Keep these as class fields for real-time playback
    private int currentStep = 0; // Current step in the pattern
    private long tickCounter = 0; // Tick counter
    private boolean isPlaying = false; // Flag indicating playback state
    private int bounceDirection = 1; // Direction for bounce mode

    private Integer currentBar = null;

    // Add the data object that will store all pattern and settings data

    private MelodicSequenceData sequenceData = new MelodicSequenceData();

    public MelodicSequenceData getSequenceData() {
        return sequenceData;
    }

    public void setSequenceData(MelodicSequenceData data) {
        this.sequenceData = data;
        // Update anything that depends on the data
        updateQuantizer();
    }

    // Calculated/computed fields
    private Boolean[] scaleNotes; // Computed from root note and scale
    private Quantizer quantizer; // Computed from scale notes

    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    // Add master tempo field to synchronize with session
    private int masterTempo;

    // Add these fields
    private Integer id;

    // Add this field to the class
    private boolean latchEnabled = false;

    // Add or modify this field and methods
    private Consumer<NoteEvent> noteEventPublisher;

    // Add field for next pattern ID
    private Long nextPatternId = null;

    // Add these fields and methods to MelodicSequencer class
    private List<Integer> harmonicTiltValues = new ArrayList<>(16);

    // Fix this in MelodicSequencer - add reusable message as in DrumSequencer
    private final javax.sound.midi.ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    // Add this to both sequencers to track last note time
    private long lastNoteTriggeredTime = 0;
    private static final long MIN_NOTE_INTERVAL_MS = 1; // 1ms minimum between notes

    public static final int MIN_SWING = 50;

    public static final int MAX_SWING = 99;

    // Note object for storing melodic properties
    private Player player;

    private int currentTilt = 0;

    /**
     * Creates a new melodic sequencer
     */
    public MelodicSequencer(Integer id) {

        setId(id);
        
        initializePlayer();
        // Register with CommandBus
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Initialize the quantizer with default settings
        updateQuantizer();

        logger.info("MelodicSequencer initialized and registered with CommandBus");
    }

    /**
     * Update the quantizer with current scale settings from sequencer data
     */
    public void updateQuantizer() {
        // Create Boolean array representing which notes are in the scale
        scaleNotes = sequenceData.createScaleArray(getSelectedRootNote(), getScale());

        // Create new quantizer with the scale
        quantizer = new Quantizer(scaleNotes);

        logger.info("Quantizer updated with root note {} and scale {}",
                getSelectedRootNote(), getScale());
    }

    // Delegate methods to access data from sequencerData

    public int getSwingPercentage() {
        return sequenceData.getSwingPercentage();
    }

    public void setSwingPercentage(int percentage) {
        sequenceData.setSwingPercentage(percentage);
        logger.info("Swing percentage set to: {}", percentage);
    }

    public boolean isSwingEnabled() {
        return sequenceData.isSwingEnabled();
    }

    public void setSwingEnabled(boolean enabled) {
        sequenceData.setSwingEnabled(enabled);
        logger.info("Swing enabled: {}", enabled);
    }

    public int getPatternLength() {
        return sequenceData.getPatternLength();
    }

    public void setPatternLength(int length) {
        sequenceData.setPatternLength(length);
    }

    public Direction getDirection() {
        return sequenceData.getDirection();
    }

    public void setDirection(Direction direction) {
        sequenceData.setDirection(direction);
    }

    public boolean isLooping() {
        return sequenceData.isLooping();
    }

    public void setLooping(boolean looping) {
        sequenceData.setLooping(looping);
    }

    public int getChannel() {
        return player != null ? player.getChannel() : SEQUENCER_CHANNELS[getId() % SEQUENCER_CHANNELS.length];
    }

    public void setChannel(int channel) {

        // Update the player's channel too
        if (player != null) {
            player.setChannel(channel);

            // If the player has an instrument, update it too
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(channel);

                // Update the instrument in the manager to persist the change
                InstrumentManager.getInstance().updateInstrument(player.getInstrument());
            }
        }

        // Save the player to persist changes
        if (player != null) {
            PlayerManager.getInstance().savePlayerProperties(player);
        }

        logger.info("Set channel {} for melodic sequencer {}", channel, id);
    }

    public TimingDivision getTimingDivision() {
        return sequenceData.getTimingDivision();
    }

    public void setTimingDivision(TimingDivision division) {
        if (division != null) {
            sequenceData.setTimingDivision(division);
            logger.info("Timing division set to {}", division);
        }
    }

    public String getSelectedRootNote() {
        return sequenceData.getRootNote();
    }

    public void setSelectedRootNote(String rootNote) {
        sequenceData.setRootNote(rootNote);
        updateQuantizer();

        // Re-quantize notes if quantization is enabled
        if (isQuantizeEnabled()) {
            requantizeAllNotes();
        }
    }

    /**
     * Re-quantize all notes in the pattern based on current scale settings
     */
    private void requantizeAllNotes() {
        List<Integer> notes = sequenceData.getNoteValues();
        for (int i = 0; i < notes.size(); i++) {
            notes.set(i, quantizeNote(notes.get(i)));
        }
    }

    public String getScale() {
        return sequenceData.getScale();
    }

    public void setScale(String scale) {
        sequenceData.setScale(scale);
        updateQuantizer();

        // Re-quantize notes if quantization is enabled
        if (isQuantizeEnabled()) {
            requantizeAllNotes();
        }
    }

    public boolean isQuantizeEnabled() {
        return sequenceData.isQuantizeEnabled();
    }

    public void setQuantizeEnabled(boolean enabled) {
        sequenceData.setQuantizeEnabled(enabled);
        logger.info("Quantization {}", enabled ? "enabled" : "disabled");
    }

    public int getOctaveShift() {
        return sequenceData.getOctaveShift();
    }

    public void setOctaveShift(int octaveShift) {
        sequenceData.setOctaveShift(octaveShift);
    }

    public void decrementOctaveShift() {
        sequenceData.setOctaveShift(sequenceData.getOctaveShift() - 1);
        logger.info("Octave shift decreased to {}", sequenceData.getOctaveShift());
    }

    public void incrementOctaveShift() {
        sequenceData.setOctaveShift(sequenceData.getOctaveShift() + 1);
        logger.info("Octave shift increased to {}", sequenceData.getOctaveShift());
    }

    // Delegate methods for step data

    public boolean isStepActive(int stepIndex) {
        return sequenceData.isStepActive(stepIndex);
    }

    public int getNoteValue(int stepIndex) {
        return sequenceData.getNoteValue(stepIndex);
    }

    public int getVelocityValue(int stepIndex) {
        return sequenceData.getVelocityValue(stepIndex);
    }

    public int getGateValue(int stepIndex) {
        return sequenceData.getGateValue(stepIndex);
    }

    public int getProbabilityValue(int stepIndex) {
        return sequenceData.getProbabilityValue(stepIndex);
    }

    public int getNudgeValue(int stepIndex) {
        return sequenceData.getNudgeValue(stepIndex);
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability,
            int nudge) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate, probability, nudge);
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate);
    }

    public List<Boolean> getActiveSteps() {
        if (sequenceData == null) {
            logger.warn("sequenceData is null, returning empty active steps list");
            return Collections.emptyList();
        }

        List<Boolean> steps = sequenceData.getActiveSteps();
        // logger.debug("Retrieved {} active steps, {} are on",
        // steps.size(),
        // steps.stream().filter(b -> b).count());
        return steps;
    }

    public List<Integer> getNoteValues() {
        return sequenceData.getNoteValues();
    }

    public List<Integer> getVelocityValues() {
        return sequenceData.getVelocityValues();
    }

    public List<Integer> getGateValues() {
        return sequenceData.getGateValues();
    }

    public List<Integer> getProbabilityValues() {
        return sequenceData.getProbabilityValues();
    }

    public List<Integer> getNudgeValues() {
        return sequenceData.getNudgeValues();
    }

    // Pattern manipulation methods - delegate to data object

    public void clearPattern() {
        sequenceData.clearPattern();
        notifyPatternUpdated();
    }

    public void generatePattern(int octaveRange, int density) {
        sequenceData.generatePattern(octaveRange, density);
        logger.info("Generated pattern: octaves={}, density={}", octaveRange, density);
        notifyPatternUpdated();
    }

    public void rotatePatternRight() {
        sequenceData.rotatePatternRight();
        notifyPatternUpdated();
    }

    public void rotatePatternLeft() {
        sequenceData.rotatePatternLeft();
        notifyPatternUpdated();
    }

    public void rotatePattern() {
        rotatePatternRight();
    }

    /**
     * Start playback of the sequencer
     */
    public void start() {
        if (isPlaying) {
            return;
        }

        if (getHarmonicTiltValues() == null || getHarmonicTiltValues().isEmpty()) {
            logger.warn("No harmonic tilt values set, using default 0");
            setHarmonicTiltValues(Collections.singletonList(0));
        }
        currentTilt = getHarmonicTiltValues().get(0);
        // Reset position if needed
        if (currentStep >= getSequenceData().getPatternLength()) {
            reset();
        }

        PlayerManager.getInstance().applyInstrumentPreset(player);

        isPlaying = true;
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
     * Initialize the player if it doesn't already have a valid instrument
     */
    public void ensurePlayerHasInstrument() {
        if (player != null && player.getInstrument() == null) {
            logger.warn("Player {} has no instrument, initializing default", getChannel());
            // Use PlayerManager with our enhanced API
            PlayerManager.getInstance().initializeInternalInstrument(player, true);
        }
    }

    public void processTick(Long tick) {
        if (!isPlaying || tick == null) {
            return;
        }

        tickCounter = tick;

        // Use timing division from data object
        int ticksForDivision = sequenceData.getTimingDivision().getTicksPerBeat();

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
     * Calculate the next step based on the current direction
     */
    private void calculateNextStep() {
        int oldStep = currentStep;
        boolean patternCompleted = false;

        switch (sequenceData.getDirection()) {
            case FORWARD -> {
                currentStep++;

                // Check if we've reached the end of the pattern
                if (currentStep >= sequenceData.getPatternLength()) {
                    currentStep = 0;
                    patternCompleted = true;

                    // Handle pattern switching if enabled
                    handlePatternCompletion();

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case BACKWARD -> {
                currentStep--;

                if (currentStep < 0) {
                    currentStep = sequenceData.getPatternLength() - 1;
                    patternCompleted = true;

                    // Handle pattern switching
                    handlePatternCompletion();

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case BOUNCE -> {
                // Update step counter according to bounce direction
                currentStep += bounceDirection;

                // Check if we need to change bounce direction
                if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                    bounceDirection *= -1;

                    // At pattern end, handle pattern completion
                    if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                        handlePatternCompletion();
                    }

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case RANDOM -> {
                // Random doesn't have a clear cycle end, so we'll consider
                // hitting step 0 as the "cycle end" for latch purposes
                int priorStep = currentStep;

                // Generate random step
                currentStep = (int) (Math.random() * sequenceData.getPatternLength());

                // If we randomly hit step 0 and we weren't at step 0 before
                if (currentStep == 0 && priorStep != 0) {
                    handlePatternCompletion();
                }
            }
        }

        // Notify step listeners about the step change
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(oldStep, currentStep));
        }
    }

    /**
     * Handle actions that happen when a pattern completes
     */
    private void handlePatternCompletion() {
        // Generate a new pattern if latch is enabled
        if (latchEnabled) {
            int octaveRange = 2;
            int density = 50;
            generatePattern(octaveRange, density);
            logger.info("Latch mode: Generated new pattern at cycle end");
        }

        // Handle pattern switching if enabled
        if (nextPatternId != null) {
            Long currentId = sequenceData.getId();
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
    }

    /**
     * Quantize a note to the current scale
     * 
     * @param noteValue The note value to quantize
     * @return The quantized note
     */
    public int quantizeNote(int noteValue) {
        if (!sequenceData.isQuantizeEnabled() || quantizer == null) {
            return noteValue;
        }

        // Apply octave shift
        int octaveOffset = sequenceData.getOctaveShift() * 12;
        int shiftedNote = noteValue + octaveOffset;

        // Apply quantization
        int quantizedNote = quantizer.quantizeNote(shiftedNote);

        logger.debug("Quantized note {} to {} (with octave shift {})",
                noteValue, quantizedNote, sequenceData.getOctaveShift());

        return quantizedNote;
    }

    /**
     * Reset the sequencer to its initial state
     * Stops playback and resets step counter
     */
    public void reset() {
        // Stop playback
        isPlaying = false;

        // Reset step counter
        currentStep = 0;

        // Reset tick counter
        tickCounter = 0;

        // Reset bounce direction
        bounceDirection = 1;

        // All notes off if player exists
        if (player != null && player.getInstrument() != null) {
            try {
                // player.allNotesOff();
                logger.debug("All notes off sent during reset");
            } catch (Exception e) {
                logger.error("Error sending all notes off: {}", e.getMessage(), e);
            }
        }

        // Notify listeners about step change to highlight reset position
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(-1, currentStep));
        }

        logger.info("Sequencer reset");
    }

    /**
     * Trigger a note for the specified step
     * 
     * @param stepIndex The step to trigger
     */
    public void triggerNote(int stepIndex) {
        // Skip if not playing or player not available
        if (!isPlaying || player == null) {
            return;
        }

        // Check if step is active
        boolean stepActive = sequenceData.isStepActive(stepIndex);
        if (!stepActive) {
            return;
        }

        // Check probability
        int probability = sequenceData.getProbabilityValue(stepIndex);
        if (probability < 100) {
            // Generate random number (0-99)
            int rand = (int) (Math.random() * 100);

            // Skip if random number is >= probability
            if (rand >= probability) {
                logger.debug("Step {} skipped due to probability ({} < {})",
                        stepIndex, rand, probability);
                return;
            }
        }

        // Get step parameters
        int noteValue = sequenceData.getNoteValue(stepIndex);
        int velocity = sequenceData.getVelocityValue(stepIndex);
        int gate = sequenceData.getGateValue(stepIndex);

        if (sequenceData.isQuantizeEnabled()) {
            noteValue = quantizeNote(noteValue);
        }

        // apply harmonic tilt
        noteValue = noteValue + currentTilt;

        try {
            // Check for too-frequent note triggers
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNoteTriggeredTime < MIN_NOTE_INTERVAL_MS) {
                logger.debug("Ignoring note trigger - too soon after last note ({} ms)",
                        currentTime - lastNoteTriggeredTime);
                return;
            }
            lastNoteTriggeredTime = currentTime;

            // Calculate note duration based on gate percentage
            // (assuming a full step is 4 beats)

            int noteOnVelocity = velocity;

            // Create note event
            NoteEvent event = new NoteEvent(
                    // channel,
                    noteValue,
                    noteOnVelocity,
                    gate
            // currentTime
            );

            // Trigger note through player
            if (player != null) {
                player.noteOn(noteValue, noteOnVelocity, gate);
            }

            // Notify listeners
            if (noteEventListener != null) {
                noteEventListener.accept(event);
            }

            logger.debug("Triggered step {} - note:{} vel:{} gate:{} tilt:{}",
                    stepIndex, noteValue, velocity, gate, currentTilt);

        } catch (Exception e) {
            logger.error("Error triggering note: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify listeners that the pattern has been updated
     */
    public void notifyPatternUpdated() {
        // Publish pattern updated event
        CommandBus.getInstance().publish(
                Commands.MELODIC_PATTERN_UPDATED,
                this,
                sequenceData);

        logger.debug("Pattern updated notification sent");
    }

    /**
     * Update the sequencer based on provided sequence data
     * 
     * @param sequenceData The data to use for updating
     */
    public void updateFromData(MelodicSequenceData sequenceData) {
        if (sequenceData == null) {
            logger.warn("Cannot update from null sequence data");
            return;
        }

        // Store current playback state
        boolean wasPlaying = isPlaying;

        // Update the sequencer data
        this.sequenceData = sequenceData;

        // Update computed fields
        updateQuantizer();

        // Reset step counter
        currentStep = 0;

        // Restore playback state
        isPlaying = wasPlaying;

        logger.info("Sequencer updated from sequence data (ID: {})",
                sequenceData.getId());
    }

    /**
     * Initialize the Note object with proper InstrumentWrapper and Session
     * integration
     */
    private void initializePlayer() {
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
            // String playerName = "Melody " + getChannel();
            // Update channel if needed
            // setChannel(getSequenceData().getChannel());
            if (player.getChannel() != getChannel()) {
                player.setChannel(getChannel());
                player.setOwner(this);
                setPlayer(existingPlayer);
                // Save the player through PlayerManager
                PlayerManager.getInstance().savePlayerProperties(player);
            }
        } else {
            // Create new player through PlayerManager
            logger.info("Creating new player for sequencer {}", id);
            setPlayer(RedisService.getInstance().newNote());

            Integer tag = getId() + 1;

            player.setOwner(this);
            player.setName("Melo " + tag.toString() + " [" + getChannel() + "] ");
            player.setChannel(getChannel());

            // Get an instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance()
                    .getOrCreateInternalSynthInstrument(getChannel(), true);
            if (instrument != null) {
                player.setInstrument(instrument);
                PlayerManager.getInstance().savePlayerProperties(player);
                logger.info("Created new player {} for melodic sequencer {}", player.getId(), id);
            }

            if (player.getInstrument() != null) {
                MidiDevice device = DeviceManager.getInstance().getMidiDevice(player.getInstrument().getDeviceName());
                if (device != null)
                    player.getInstrument().setDevice(device);
            }

            // ensurePlayerHasInstrument();
            // Always initialize the instrument once player is set up
            if (player.getInstrument() == null) {
                PlayerManager.getInstance().initializeInternalInstrument(player, true);
            }

            // If we have soundbank/bank/preset settings in the sequence data, apply them
            if (sequenceData.getSoundbankName() != null || sequenceData.getBankIndex() != null ||
                    sequenceData.getPreset() != null) {

                if (player.getInstrument() != null) {
                    // Apply saved settings
                    if (sequenceData.getSoundbankName() != null) {
                        player.getInstrument().setSoundbankName(sequenceData.getSoundbankName());
                    }

                    if (sequenceData.getBankIndex() != null) {
                        player.getInstrument().setBankIndex(sequenceData.getBankIndex());
                    }

                    if (sequenceData.getPreset() != null) {
                        player.getInstrument().setPreset(10);
                    }

                    // Send program change
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                }
            }

            if (player.getInstrument() != null) {
                player.getInstrument().setAssignedToPlayer(true);
            }
            session.getPlayers().add(player);
            SessionManager.getInstance().saveSession(session);
            logger.info("Added new player to session {}: {}", session.getId(), player.getId());

        }
    }

    /**
     * Find a player in the session that belongs to this sequencer
     */
    private Player findExistingPlayerForSequencer(Session session) {
        if (session == null || id == null) {
            return null;
        }

        // Look for a player that's associated with this sequencer AND has the correct
        // channel
        for (Player p : session.getPlayers()) {
            if (p instanceof Note &&
                    p.getOwner() != null &&
                    p.getOwner() instanceof MelodicSequencer &&
                    ((MelodicSequencer) p.getOwner()).getId() != null &&
                    ((MelodicSequencer) p.getOwner()).getId().equals(id) &&
                    p.getChannel() == getChannel()) { // Added channel check

                return p;
            }
        }

        logger.info("No player found for sequencer {} and channel {}", id, getChannel());
        return null;
    }

    /**
     * Set harmonic tilt values from a list
     */
    public void setHarmonicTiltValues(List<Integer> tiltValues) {
        if (tiltValues == null || tiltValues.isEmpty()) {
            logger.warn("Attempted to set null or empty tilt values");
            return;
        }

        // Create an array of the right size
        int[] tiltArray = new int[Math.max(sequenceData.getPatternLength(), tiltValues.size())];

        // Copy values from list to array
        for (int i = 0; i < tiltValues.size(); i++) {
            tiltArray[i] = tiltValues.get(i);
        }

        // Set on sequence data
        sequenceData.setHarmonicTiltValues(tiltArray);

        logger.info("Set {} harmonic tilt values in sequencer", tiltValues.size());
    }

    /**
     * Implementation of IBusListener interface
     */
    @Override
    public void onAction(Command action) {
        if (action == null) {
            return;
        }

        if (Commands.REPAIR_MIDI_CONNECTIONS.equals(action.getCommand())) {
            repairMidiConnections();
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    if (update.tick() != null) {
                        processTick(update.tick());
                    }

                    if (update.bar() != null) {
                        currentBar = update.bar() - 1; // Adjust for 0-based index
                        logger.debug("Current bar updated to {}", currentBar);
                        if (getHarmonicTiltValues() != null && getHarmonicTiltValues().size() > currentBar) {
                            // Get the current tilt value based on the bar
                            currentTilt = getHarmonicTiltValues().get(currentBar);
                            logger.debug("Current tilt value for bar {}: {}", currentBar, currentTilt);
                        }
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

    public List<Integer> getHarmonicTiltValues() {
        // Add more detailed logging

        if (sequenceData == null) {
            logger.error("sequenceData is null in getHarmonicTiltValues()");
            return new ArrayList<>();
        }

        int[] rawValues = sequenceData.getHarmonicTiltValuesRaw();
        if (rawValues == null) {
            logger.error("Raw harmonic tilt values array is null in sequencer");
            return new ArrayList<>();
        }

        // Convert using Arrays.stream for better performance
        List<Integer> result = Arrays.stream(rawValues).boxed().collect(Collectors.toList());
        logger.debug("getHarmonicTiltValues(): returning {} values from raw array of length {}",
                result.size(), rawValues.length);

        return result;
    }

    /**
     * Attempts to repair MIDI connections if they have been lost
     * This can be called when playback is not producing sound
     */
    public void repairMidiConnections() {
        logger.info("Attempting to repair MIDI connections for melodic sequencer {}", id);
        
        try {
            if (player == null) {
                logger.warn("No player for sequencer {}, creating", id);
                initializePlayer();
                return;
            }
            
            if (player.getInstrument() == null) {
                logger.warn("Player has no instrument, initializing...");
                PlayerManager.getInstance().initializeInternalInstrument(player, true);
            }
            
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                logger.error("Failed to initialize instrument");
                return;
            }
            
            String deviceName = instrument.getDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Gervill"; // Default to Gervill
                instrument.setDeviceName(deviceName);
            }
            
            // Try to get a fresh device and receiver
            MidiDevice device = DeviceManager.getMidiDevice(deviceName);
            if (device == null) {
                device = DeviceManager.getInstance().getDefaultOutputDevice();
                if (device != null) {
                    deviceName = device.getDeviceInfo().getName();
                    instrument.setDeviceName(deviceName);
                }
            }
            
            if (device != null) {
                if (!device.isOpen()) {
                    device.open();
                }
                instrument.setDevice(device);
                
                Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(deviceName, device);
                if (receiver != null) {
                    //instrument.setReceiver(receiver);
                    logger.info("Successfully reconnected sequencer {} to device {}", id, deviceName);
                    
                    // Apply instrument settings
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                } else {
                    logger.warn("Failed to get receiver for sequencer {}", id);
                }
            } else {
                logger.warn("Could not get device for sequencer {}", id);
            }
            
            // Save changes
            PlayerManager.getInstance().savePlayerProperties(player);
        } catch (Exception e) {
            logger.error("Error repairing melodic sequencer {}: {}", id, e.getMessage());
        }
    }
}

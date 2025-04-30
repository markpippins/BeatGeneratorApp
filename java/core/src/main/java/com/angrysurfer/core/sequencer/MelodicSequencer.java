package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

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
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    // Keep these as class fields for real-time playback
    private int currentStep = 0; // Current step in the pattern
    private long tickCounter = 0; // Tick counter
    private boolean isPlaying = false; // Flag indicating playback state
    private int bounceDirection = 1; // Direction for bounce mode

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

    public static final int MIN_SWING = 50;

    public static final int MAX_SWING = 99;

    // Note object for storing melodic properties
    private Player player;

    /**
     * Creates a new melodic sequencer
     */
    public MelodicSequencer() {

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
        return sequenceData.getChannel();
    }

    public void setChannel(int channel) {
        sequenceData.setChannel(channel);

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

        // Make sure player has instrument before starting
        // ensurePlayerHasInstrument();
        // applyInstrumentPreset();

        // Then initialize MIDI instrument
        // initializeInstrument();

        isPlaying = true;

        // Reset position if needed
        if (currentStep >= getSequenceData().getPatternLength()) {
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
     * Initialize the player if it doesn't already have a valid instrument
     */
    public void ensurePlayerHasInstrument() {
        if (player != null && player.getInstrument() == null) {
            logger.warn("Player {} has no instrument, initializing default", player.getId());
            initializeInternalInstrument(player);
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
     * Initialize the instrument for this sequencer
     * Called by PlayerManager during setup
     */
    public void initializeInstrument() {
        // If we don't have a player yet, exit
        if (player == null) {
            logger.warn("Cannot initialize instrument - no player assigned to sequencer");
            return;
        }

        try {
            // Get the instrument from the player
            if (player.getInstrument() == null) {
                // No instrument assigned, try to create a default one
                // First try to find an existing instrument for this channel
                int channel = sequenceData.getChannel();
                player.setInstrument(InstrumentManager.getInstance()
                        .getOrCreateInternalSynthInstrument(channel));

                logger.info("Created default instrument for sequencer on channel {}", channel);
            }

            // Ensure proper channel alignment
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(player.getChannel());

                // Apply the instrument preset
                applyInstrumentPreset();

                logger.info("Initialized instrument {} for player {} on channel {}",
                        player.getInstrument().getName(),
                        player.getName(),
                        player.getChannel());
            }
        } catch (Exception e) {
            logger.error("Error initializing instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Apply the current instrument preset
     * Sends the appropriate program change MIDI message to set the instrument sound
     */
    public void applyInstrumentPreset() {
        if (player == null || player.getInstrument() == null) {
            logger.debug("Cannot apply preset - player or instrument is null");
            return;
        }

        // Get channel and preset
        Integer channel = player.getChannel();
        Integer preset = player.getPreset();

        if (channel == null || preset == null) {
            logger.debug("Cannot apply preset - missing channel or preset value");
            return;
        }

        try {
            // Get the instrument from the player
            InstrumentWrapper instrument = player.getInstrument();

            // Get bank index from instrument if available
            Integer bankIndex = instrument.getBankIndex();

            if (bankIndex != null && bankIndex > 0) {
                // Calculate MSB and LSB from bankIndex
                int bankMSB = (bankIndex >> 7) & 0x7F; // Controller 0
                int bankLSB = bankIndex & 0x7F; // Controller 32

                // Send bank select messages
                instrument.controlChange(0, bankMSB); // Bank MSB
                instrument.controlChange(32, bankLSB); // Bank LSB

                logger.debug("Sent bank select MSB={}, LSB={} for bank={}",
                        bankMSB, bankLSB, bankIndex);
            }

            // Send program change with correct parameters (second param should be 0)
            instrument.programChange(preset, 0); // Fixed: added second parameter

            logger.info("Applied program change {} on channel {} for player {}",
                    preset, channel, player.getName());

        } catch (Exception e) {
            logger.error("Failed to apply instrument preset: {}", e.getMessage(), e);
        }
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
        int harmonicTilt = sequenceData.getHarmonicTiltValue(stepIndex);

        // Apply quantization if enabled
        if (sequenceData.isQuantizeEnabled()) {
            noteValue = quantizeNote(noteValue);
        }

        // Apply harmonic tilt
        if (harmonicTilt != 0) {
            noteValue += harmonicTilt;
            noteValue = Math.max(0, Math.min(127, noteValue)); // Clamp to MIDI range
        }

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
            int channel = sequenceData.getChannel();
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
                    stepIndex, noteValue, velocity, gate, harmonicTilt);

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

            // Update channel if needed
            if (player.getChannel() != getSequenceData().getChannel()) {
                player.setChannel(getSequenceData().getChannel());
                // Save the player through PlayerManager
                PlayerManager.getInstance().savePlayerProperties(player);
            }
        } else {
            // Create new player through PlayerManager
            logger.info("Creating new player for sequencer {}", id);
            player = RedisService.getInstance().newNote();

            // Get an instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance()
                    .getOrCreateInternalSynthInstrument(getSequenceData().getChannel());

            if (instrument != null) {

                // Create a Note player with proper name
                String playerName = "Melody " + (id != null ? id : "");
                player = new Note(playerName, session, instrument, 60, null);
                // Configure base properties
                player.setMinVelocity(60);
                player.setMaxVelocity(127);
                player.setLevel(100);
                player.setChannel(getSequenceData().getChannel());

                // Associate player with this sequencer
                player.setOwner(this);

                // Generate an ID for the player if needed
                if (player.getId() == null) {
                    player.setId(RedisService.getInstance().getNextPlayerId());
                }

                // Add to session
                session.getPlayers().add(player);
                player.setInstrument(instrument);

                // Save through PlayerManager
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
                initializeInternalInstrument(player);
            }
        }
    }

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
                // Get a reference to the internal synthesizer device
                MidiDevice internalSynthDevice;
                try {
                    internalSynthDevice = InternalSynthManager.getInstance().getInternalSynthDevice();
                } catch (MidiUnavailableException e) {
                    logger.error("Failed to get internal synth device: {}", e.getMessage());
                    return;
                }

                // Generate a numeric ID for the instrument
                long numericId = 9985L + player.getChannel();

                // Create the instrument with proper constructor parameters
                internalInstrument = new InstrumentWrapper(
                        "Internal Synth " + player.getChannel(), // Name
                        internalSynthDevice, // Device (not a String)
                        player.getChannel() // Channel
                );

                // Set the rest of the properties
                internalInstrument.setInternal(true);
                internalInstrument.setDeviceName("Gervill");
                internalInstrument.setSoundbankName("Default");
                internalInstrument.setBankIndex(0);
                internalInstrument.setId(numericId); // Set ID separately
                internalInstrument.setChannel(player.getChannel());
                internalInstrument
                        .setCurrentPreset(player.getPreset() != null ? player.getPreset() : player.getChannel());

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

        // Look for a player that's associated with this sequencer AND has the correct
        // channel
        for (Player p : session.getPlayers()) {
            if (p instanceof Note &&
                    p.getOwner() != null &&
                    p.getOwner() instanceof MelodicSequencer &&
                    ((MelodicSequencer) p.getOwner()).getId() != null &&
                    ((MelodicSequencer) p.getOwner()).getId().equals(id) &&
                    p.getChannel() == getSequenceData().getChannel()) { // Added channel check

                return p;
            }
        }

        logger.info("No player found for sequencer {} and channel {}", id, getSequenceData().getChannel());
        return null;
    }

    /**
     * Get a default instrument for melodic sequencing
     */
    private InstrumentWrapper getDefaultInstrument() {
        // Create with null device (indicates internal synth)
        InstrumentWrapper internalInstrument = new InstrumentWrapper(
                "Internal Synth",
                null, // Explicitly pass null for device
                getChannel());

        // Configure as internal instrument
        internalInstrument.setInternal(true);
        internalInstrument.setDeviceName("Gervill");
        internalInstrument.setSoundbankName("Default");
        internalInstrument.setBankIndex(0);
        internalInstrument.setCurrentPreset(0); // Default to piano
        internalInstrument.setId(9985L + getSequenceData().getChannel());

        // Register with manager
        InstrumentManager.getInstance().updateInstrument(internalInstrument);

        return internalInstrument;
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
}

package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.feature.Note;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    // Sequencing parameters
    private int stepCounter = 0;          // Current step in the pattern
    private int patternLength = 16;       // Default pattern length
    private Direction direction = Direction.FORWARD; // Default direction
    private boolean looping = true;       // Default to looping
    private long tickCounter = 0;          // Tick counter
    private long ticksPerStep = 24;       // Base ticks per step (96 PPQN / 4 = 24)
    private boolean isPlaying = false;    // Flag indicating playback state
    private int bounceDirection = 1;      // Direction for bounce mode
    private int channel = 3;          // Default MIDI channel

    private TimingDivision timingDivision = TimingDivision.NORMAL;

    // Scale & quantization
    private String selectedRootNote = "C";
    private String selectedScale = "Chromatic";
    private Boolean[] scaleNotes;
    private boolean quantizeEnabled = true;
    private Quantizer quantizer;
    private int octaveShift = 0;             // Compatibility for scale

    // Pattern data storage
    private List<Boolean> activeSteps = new ArrayList<>();      // Step on/off state
    private List<Integer> noteValues = new ArrayList<>();       // Note for each step
    private List<Integer> velocityValues = new ArrayList<>();   // Velocity for each step
    private List<Integer> gateValues = new ArrayList<>();       // Gate time for each step

    // Note object for storing melodic properties (NEW)
    private Note notePlayer;

    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    // Add master tempo field to synchronize with session
    private int masterTempo;

    // Add these fields
    private Integer id;
    private Long melodicSequenceId = 0L;

    /**
     * Creates a new melodic sequencer
     */
    public MelodicSequencer() {
        // Initialize pattern data with default values
        initializePatternData();

        // Initialize note properties (NEW)
        initializeNote();

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
    }

    // Constructor with ID
    public MelodicSequencer(Integer id, Integer channel) {
        this.id = id;
        this.channel = channel;
        // Other initialization...
    }

    /**
     * Initialize the Note object with proper InstrumentWrapper and connected MidiDevice
     */
    private void initializeNote() {
        notePlayer = new Note();
        notePlayer.setName("Melody");
        notePlayer.setRootNote(60); // Middle C
        notePlayer.setMinVelocity(60);
        notePlayer.setMaxVelocity(127);
        notePlayer.setLevel(100);
        notePlayer.setChannel(channel); // Use the sequencer's channel setting

        try {
            // Get the InstrumentManager instance
            InstrumentManager instrumentManager = InstrumentManager.getInstance();

            // Look for an internal synth instrument first
            InstrumentWrapper internalInstrument = null;
            String deviceName = "Gervill"; // Default Java synth name

            // Try to find existing internal instrument
            List<InstrumentWrapper> instruments = instrumentManager.getCachedInstruments();
            for (InstrumentWrapper instrument : instruments) {
                if (Boolean.TRUE.equals(instrument.getInternal())) {
                    internalInstrument = instrument;
                    internalInstrument.setInternal(true);
                    internalInstrument.setDeviceName("Gervill"); 
                    internalInstrument.setSoundbankName("Default");
                    internalInstrument.setBankIndex(0);
                    internalInstrument.setCurrentPreset(0);  // Piano
                    break;
                }
            }

            // If no internal instrument found, create one
            if (internalInstrument == null) {
                // Create a new internal instrument - but don't set the device yet!
                internalInstrument = new InstrumentWrapper("Internal Synth", null, channel);
                internalInstrument.setInternal(true);
                internalInstrument.setDeviceName(deviceName);
                
                // Set default soundbank parameters
                internalInstrument.setSoundbankName("Default");
                internalInstrument.setBankIndex(0);
                internalInstrument.setCurrentPreset(0);  // Piano

                // Give it a unique ID
                // note.setPreset(internalInstrument.getCurrentPreset());
            }

            internalInstrument.setId(9985L + getNotePlayer().getChannel()); // Unique ID for internal synth
            notePlayer.setInstrument(internalInstrument);

            // Now properly connect the device using DeviceManager
            // Check if device is available
            List<String> availableDevices = DeviceManager.getInstance().getAvailableOutputDeviceNames();
            if (!availableDevices.contains(deviceName)) {
                CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Device not available: " + deviceName));
                    
                logger.error("Device not available: {}", deviceName);
            } else {
                // Try to reinitialize the device connection
                MidiDevice device = DeviceManager.getMidiDevice(deviceName);
                if (device != null) {
                    if (!device.isOpen()) {
                        device.open();
                    }
                    
                    boolean connected = device.isOpen();
                    if (connected) {
                        internalInstrument.setDevice(device);
                        internalInstrument.setAvailable(true);
                        
                        // Update in cache/config
                        instrumentManager.updateInstrument(internalInstrument);
                        logger.info("Successfully connected to device: {}", deviceName);
                    } else {
                        logger.error("Failed to open device: {}", deviceName);
                    }
                }
            }

            // Set the instrument on the note
            notePlayer.setInstrument(internalInstrument);

            // Configure the note's channel
            notePlayer.setChannel(channel);

            logger.info("Initialized Note with InstrumentWrapper: {}", internalInstrument.getName());
            
            // Test the connection
            try {
                if (internalInstrument.getAvailable()) {
                    internalInstrument.noteOn(channel, 60, 100);
                    Thread.sleep(100);
                    internalInstrument.noteOff(channel, 60, 0);
                    logger.info("Test note played successfully");
                    
                }
            } catch (Exception e) {
                logger.warn("Test note failed: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Note with InstrumentWrapper: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize pattern data arrays with default values
     */
    private void initializePatternData() {
        activeSteps = new ArrayList<>(patternLength);
        noteValues = new ArrayList<>(patternLength);
        velocityValues = new ArrayList<>(patternLength);
        gateValues = new ArrayList<>(patternLength);

        // Fill with default values
        for (int i = 0; i < patternLength; i++) {
            activeSteps.add(false);          // All steps off by default
            noteValues.add(60);              // Middle C
            velocityValues.add(100);         // Medium-high velocity
            gateValues.add(50);              // 50% gate time
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

        // FIXED APPROACH: Use same fixed ticksPerStep as DrumSequencer
        int fixedTicksPerStep = 24; // Match the value used in DrumSequencer
        
        // Check if it's time for the next step using fixed step size
        if (tick % fixedTicksPerStep == 0) {
            // Get previous step for highlighting updates
            int prevStep = stepCounter;
            
            // Calculate the next step based on the current direction
            calculateNextStep();
            
            // Notify listeners of step update
            if (stepUpdateListener != null) {
                stepUpdateListener.accept(new StepUpdateEvent(prevStep, stepCounter));
            }
            
            // Check if the current step is active
            if (stepCounter >= 0 && stepCounter < activeSteps.size() && activeSteps.get(stepCounter)) {
                // Get parameters for the current step
                int noteValue = noteValues.get(stepCounter);
                int velocity = velocityValues.get(stepCounter);
                int gateLength = gateValues.get(stepCounter);
                
                // Apply octave shift and quantize note if needed
                if (quantizeEnabled && quantizer != null) {
                    noteValue = quantizer.quantizeNote(noteValue);
                }
                int finalNoteValue = applyOctaveShift(noteValue);
                
                // Calculate note duration based on gate value and tempo
                int durationMs = calculateNoteDuration(gateLength);
                
                // Play the note directly using the sequencer's internal method
                playNoteDirectly(finalNoteValue, velocity, durationMs);
                
                // Also notify the UI through the listener if needed
                if (noteEventListener != null) {
                    noteEventListener.accept(new NoteEvent(finalNoteValue, velocity, durationMs));
                }
            }
        }
    }

    /**
     * Calculate the previous step based on current direction
     */
    private int getPreviousStep() {
        switch (direction) {
            case FORWARD:
                return (stepCounter + patternLength - 1) % patternLength;
            case BACKWARD:
                return (stepCounter + 1) % patternLength;
            case BOUNCE:
                // For bounce, it depends on the current bounce direction
                if (bounceDirection > 0) {
                    return stepCounter > 0 ? stepCounter - 1 : 0;
                } else {
                    return stepCounter < patternLength - 1 ? stepCounter + 1 : patternLength - 1;
                }
            case RANDOM:
            default:
                return stepCounter; // For random, just use current position
        }
    }

    /**
     * Play a note directly using the Note's InstrumentWrapper
     *
     * @param midiNote The MIDI note number to play
     * @param velocity The velocity (volume) of the note
     * @param duration The duration in milliseconds
     */
    private void playNoteDirectly(int midiNote, int velocity, int duration) {
        if (notePlayer == null || notePlayer.getInstrument() == null) {
            logger.warn("Cannot play note directly - note or instrument is null");
            return;
        }

        try {
            final InstrumentWrapper instrument = notePlayer.getInstrument();
            final int channel = notePlayer.getChannel();

            // Log what we're about to play
            logger.info("Playing note directly: note={}, vel={}, duration={}, channel={}, instrument={}",
                    midiNote, velocity, duration, channel, instrument.getName());

            // Set the channel's current program if needed
            if (notePlayer.getPreset() != null) {
                // Send bank select messages if a bank is specified
                if (instrument.getBankIndex() != null && instrument.getBankIndex() > 0) {
                    instrument.controlChange(channel, 0, 0);     // Bank MSB
                    instrument.controlChange(channel, 32, instrument.getBankIndex()); // Bank LSB
                }

                // Send program change
                instrument.programChange(channel, notePlayer.getPreset().intValue(), 0);
            }

            // Play the note - ERROR IS HERE
            instrument.noteOn(channel, midiNote, velocity);  // Correct - use instrument directly

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
        switch (direction) {
            case FORWARD -> {
                stepCounter++;
                if (stepCounter >= patternLength) {
                    stepCounter = 0;
                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }
            case BACKWARD -> {
                stepCounter--;
                if (stepCounter < 0) {
                    stepCounter = patternLength - 1;
                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }
            case BOUNCE -> {
                stepCounter += bounceDirection;

                if (stepCounter >= patternLength) {
                    stepCounter = patternLength - 2;
                    bounceDirection = -1;
                } else if (stepCounter < 0) {
                    stepCounter = 1;
                    bounceDirection = 1;
                }

                if (stepCounter == 0 || stepCounter == patternLength - 1) {
                    if (!looping) {
                        isPlaying = false;
                    }
                }
            }
            case RANDOM -> {
                // Remember old step
                int oldStep = stepCounter;

                // Generate random step
                stepCounter = (int) (Math.random() * patternLength);

                // Ensure we don't get the same step twice
                if (stepCounter == oldStep && patternLength > 1) {
                    stepCounter = (stepCounter + 1) % patternLength;
                }
            }
        }
    }

    /**
     * Get the root note of the melodic sequencer
     *
     * @return MIDI note value (0-127)
     */
    public Integer getRootNote() {
        return notePlayer != null ? notePlayer.getRootNote() : 60;
    }

    /**
     * Set the root note of the melodic sequencer
     *
     * @param rootNote MIDI note value (0-127)
     */
    public void setRootNote(Integer rootNote) {
        if (notePlayer != null && rootNote != null) {
            notePlayer.setRootNote(rootNote);
        }
    }

    /**
     * Get the name of the melody
     *
     * @return The melody name
     */
    public String getName() {
        return notePlayer != null ? notePlayer.getName() : "Melody";
    }

    /**
     * Set the name of the melody
     *
     * @param name The new melody name
     */
    public void setName(String name) {
        if (notePlayer != null && name != null) {
            notePlayer.setName(name);
        }
    }

    /**
     * Get the velocity level
     *
     * @return Velocity level (0-127)
     */
    public Long getLevel() {
        return notePlayer != null ? notePlayer.getLevel() : 100L;
    }

    /**
     * Set the velocity level
     *
     * @param level Velocity level (0-127)
     */
    public void setLevel(int level) {
        if (notePlayer != null && level >= 0 && level <= 127) {
            notePlayer.setLevel(level);
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
        for (int i = 0; i < activeSteps.size(); i++) {
            activeSteps.set(i, false);
        }
        logger.info("Pattern cleared");
    }

    /**
     * Generate a random pattern with the given parameters
     *
     * @param octaveRange The number of octaves to span (1-4)
     * @param density Percentage of steps to activate (1-100)
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
        scaleNotes = createScaleArray(selectedRootNote, selectedScale);
        
        // Create new quantizer with the scale
        quantizer = new Quantizer(scaleNotes);
        
        logger.info("Quantizer updated with root note {} and scale {}", selectedRootNote, selectedScale);
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
        int[] pattern = Scale.SCALE_PATTERNS.getOrDefault(scaleName, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});
        
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
        if (rootNote != null && !rootNote.equals(selectedRootNote)) {
            selectedRootNote = rootNote;
            updateQuantizer();
            logger.info("Root note set to {}", rootNote);
        }
    }

    /**
     * Set the scale for note quantization
     *
     * @param scale Scale name
     */
    public void setScale(String scale) {
        if (scale != null && !scale.equals(selectedScale)) {
            selectedScale = scale;
            updateQuantizer();
            logger.info("Scale set to {}", scale);
        }
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
     * Set data for a specific step
     *
     * @param stepIndex The step index
     * @param isActive Whether the step is active
     * @param noteValue The MIDI note value
     * @param velocityValue The velocity value
     * @param gateValue The gate time value
     */
    public void setStepData(int stepIndex, boolean isActive, int noteValue, int velocityValue, int gateValue) {
        if (stepIndex >= 0 && stepIndex < patternLength) {
            if (stepIndex < activeSteps.size()) {
                activeSteps.set(stepIndex, isActive);
            }

            if (stepIndex < noteValues.size()) {
                noteValues.set(stepIndex, noteValue);
            }

            if (stepIndex < velocityValues.size()) {
                velocityValues.set(stepIndex, velocityValue);
            }

            if (stepIndex < gateValues.size()) {
                gateValues.set(stepIndex, gateValue);
            }
        }
    }

    /**
     * Reset the sequencer to start position
     */
    public void reset() {
        // Initialize step counter based on direction
        stepCounter = direction == Direction.FORWARD ? 0 : patternLength - 1;
        tickCounter = 0;

        // Notify UI that we've reset
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(-1, stepCounter));
        }

        logger.debug("Melodic sequencer reset to step {}", stepCounter);
    }

    /**
     * Start playback
     */
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            // Get current master tempo from session
            masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
            reset();

            logger.info("Melodic sequencer playback started with master tempo: {}", masterTempo);
        }
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
                play();
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
    public int getStepCounter() {
        return stepCounter;
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
     * Calculate note duration in milliseconds based on gate length percentage and current tempo
     * 
     * @param gateLength Gate length (0-100 percent)
     * @return Duration in milliseconds
     */
    private int calculateNoteDuration(int gateLength) {
        // Safety check for gate length
        int safeGateLength = Math.max(1, Math.min(100, gateLength));
        
        // Calculate beat duration in milliseconds (60000ms / BPM)
        // Using fixed tempo calculation that works with 24 ticks per step
        double beatsPerMinute = 120.0;  // Default BPM
        
        if (masterTempo > 0) {
            // Calculate BPM from masterTempo (typically 96 PPQN)
            beatsPerMinute = 60.0 * (masterTempo / 24.0);
        }
        
        int beatDurationMs = (int)(60000 / beatsPerMinute);
        
        // Apply gate percentage to get note duration
        return (beatDurationMs * safeGateLength) / 100;
    }
}

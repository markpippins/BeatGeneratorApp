package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.feature.Note;

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
    private Note note;

    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

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

    /**
     * Initialize a default Note object (NEW)
     */
    private void initializeNote() {
        note = new Note();
        note.setName("Melody");
        note.setRootNote(60); // Middle C
        note.setMinVelocity(60);
        note.setMaxVelocity(127);
        note.setLevel(100);

        logger.info("Created default note for melodic sequencer: {}", note.getName());
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
     * Process a timing tick
     *
     * @param tick Current tick count
             */
    public void processTick(Long tick) {
        if (!isPlaying || tick == null) {
            return;
        }
        
        tickCounter = tick.intValue();

        // Check if it's time for the next step
        if (tick % ticksPerStep == 0) {
            // Store old step for UI update
            int oldStep = stepCounter;

            // Calculate next step based on direction and pattern length
            calculateNextStep();

            // Check if the current step is active
            if (stepCounter < activeSteps.size() && activeSteps.get(stepCounter)) {
                // Retrieve the specific note, velocity, and gate values for this step
                int noteValue = stepCounter < noteValues.size()
                        ? noteValues.get(stepCounter) : 60;  // Default to middle C

                // Apply octave shift and quantization if enabled
                if (quantizeEnabled) {
                    noteValue = quantizeNote(noteValue);
                }
                noteValue = applyOctaveShift(noteValue);

                int velocity = stepCounter < velocityValues.size() ? velocityValues.get(stepCounter) : 100;
                int gateTime = stepCounter < gateValues.size() ? gateValues.get(stepCounter) * 5 : 250;

                // Then trigger the note:
                if (noteEventListener != null) {
                    logger.debug("Playing note: step={}, note={}, vel={}, gate={}", 
                               stepCounter, noteValue, velocity, gateTime);
                    
                    noteEventListener.accept(
                        new NoteEvent(noteValue, velocity, gateTime)
                    );
                }

                // Notify UI of step change
                if (stepUpdateListener != null) {
                    stepUpdateListener.accept(new StepUpdateEvent(oldStep, stepCounter));
                }
            }
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
        return note != null ? note.getRootNote() : 60;
    }

    /**
     * Set the root note of the melodic sequencer
     *
     * @param rootNote MIDI note value (0-127)
     */
    public void setRootNote(Integer rootNote) {
        if (note != null && rootNote != null) {
            note.setRootNote(rootNote);
        }
    }

    /**
     * Get the name of the melody
     *
     * @return The melody name
     */
    public String getName() {
        return note != null ? note.getName() : "Melody";
    }

    /**
     * Set the name of the melody
     *
     * @param name The new melody name
     */
    public void setName(String name) {
        if (note != null && name != null) {
            note.setName(name);
        }
    }

    /**
     * Get the velocity level
     *
     * @return Velocity level (0-127)
     */
    public Long getLevel() {
        return note != null ? note.getLevel() : 100L;
    }

    /**
     * Set the velocity level
     *
     * @param level Velocity level (0-127)
     */
    public void setLevel(int level) {
        if (note != null && level >= 0 && level <= 127) {
            note.setLevel(level);
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
     * Set the root note for scale quantization
     *
     * @param rootNote Root note name (C, C#, D, etc.)
     */
    public void setRootNote(String rootNote) {
        this.selectedRootNote = rootNote;
        logger.info("Root note set to {}", rootNote);
    }

    /**
     * Set the scale for note quantization
     *
     * @param scale Scale name
     */
    public void setScale(String scale) {
        this.selectedScale = scale;
        logger.info("Scale set to {}", scale);
    }

    /**
     * Update the quantizer with current scale settings
     */
    public void updateQuantizer() {
        Boolean[] scaleNotes = createScaleNotesArray();
        quantizer = new Quantizer(scaleNotes);
        logger.info("Quantizer updated for {}, {}", selectedRootNote, selectedScale);
    }

    /**
     * Create a Boolean array representing which notes are in the scale
     */
    private Boolean[] createScaleNotesArray() {
        Boolean[] notes = new Boolean[12];
        for (int i = 0; i < 12; i++) {
            notes[i] = Boolean.TRUE; // Default to all notes (chromatic)
        }

        // Get root note index (C=0, C#=1, etc.)
        int rootIndex = getRootNoteIndex(selectedRootNote);

        // Apply scale pattern based on selected scale
        if ("Major".equals(selectedScale)) {
            // Major scale pattern: 0,2,4,5,7,9,11
            for (int i = 0; i < 12; i++) {
                notes[i] = Boolean.FALSE; // Reset all
            }
            notes[rootIndex] = Boolean.TRUE;
            notes[(rootIndex + 2) % 12] = Boolean.TRUE;
            notes[(rootIndex + 4) % 12] = Boolean.TRUE;
            notes[(rootIndex + 5) % 12] = Boolean.TRUE;
            notes[(rootIndex + 7) % 12] = Boolean.TRUE;
            notes[(rootIndex + 9) % 12] = Boolean.TRUE;
            notes[(rootIndex + 11) % 12] = Boolean.TRUE;
        } else if ("Minor".equals(selectedScale)) {
            // Natural minor scale pattern: 0,2,3,5,7,8,10
            for (int i = 0; i < 12; i++) {
                notes[i] = Boolean.FALSE; // Reset all
            }
            notes[rootIndex] = Boolean.TRUE;
            notes[(rootIndex + 2) % 12] = Boolean.TRUE;
            notes[(rootIndex + 3) % 12] = Boolean.TRUE;
            notes[(rootIndex + 5) % 12] = Boolean.TRUE;
            notes[(rootIndex + 7) % 12] = Boolean.TRUE;
            notes[(rootIndex + 8) % 12] = Boolean.TRUE;
            notes[(rootIndex + 10) % 12] = Boolean.TRUE;
        }
        // Chromatic scale (default) has all notes enabled

        return notes;
    }

    /**
     * Convert root note name to index
     */
    private int getRootNoteIndex(String rootNote) {
        return switch (rootNote) {
            case "C" ->
                0;
            case "C#", "Db" ->
                1;
            case "D" ->
                2;
            case "D#", "Eb" ->
                3;
            case "E" ->
                4;
            case "F" ->
                5;
            case "F#", "Gb" ->
                6;
            case "G" ->
                7;
            case "G#", "Ab" ->
                8;
            case "A" ->
                9;
            case "A#", "Bb" ->
                10;
            case "B" ->
                11;
            default ->
                0; // Default to C
        };
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
     * Quantize a note value to the current scale
     *
     * @param noteValue The note value to quantize
     * @return The quantized note value
     */
    public int quantizeNote(int noteValue) {
        if (quantizer != null && quantizeEnabled) {
            return quantizer.quantizeNote(noteValue);
        }
        return noteValue;
    }

    /**
     * Apply octave shift to a note value
     *
     * @param noteValue The note value to shift
     * @return The shifted note value
     */
    public int applyOctaveShift(int noteValue) {
        return Math.min(127, Math.max(0, noteValue + (12 * octaveShift)));
    }

    /**
     * Get the current octave shift
     *
     * @return The octave shift value
     */
    public int getOctaveShift() {
        return octaveShift;
    }

    /**
     * Set the octave shift
     *
     * @param shift The octave shift value
     */
    public void setOctaveShift(int shift) {
        this.octaveShift = shift;
        logger.info("Octave shift set to {}", shift);
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
            reset();

            // Notify via command bus that we've started
            // CommandBus.getInstance().publish(Commands.TRANSPORT_START, this);
            logger.info("Melodic sequencer playback started");
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
            
            // Update the ticksPerStep based on the timing division
            this.ticksPerStep = calculateTicksPerStep(division);
            
            logger.info("Timing division set to {}, ticks per step: {}", division, ticksPerStep);
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
     * Calculate ticks per step based on timing division
     */
    private long calculateTicksPerStep(TimingDivision timing) {
        // Use the ticksPerBeat value from the enum
        return (24 * 4) / timing.getTicksPerBeat();
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
                play();
            }

            case Commands.TRANSPORT_STOP -> {
                stop();
            }

            // Handle other commands as needed
        }
    }
}

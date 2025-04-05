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

import lombok.Getter;
import lombok.Setter;

/**
 * Core sequencer engine that handles timing, pattern sequencing,
 * and note generation for melodic patterns.
 */
@Getter
@Setter
public class MelodicSequencer implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    // Sequencer state
    private int currentStep = 0;             // Current step in the pattern
    private int stepCounter = 0;             // Current step in X0X pattern (0-15)
    private int tickCounter = 0;             // Count ticks within current step
    private int ticksPerStep = 24;            // How many ticks make one X0X step
    private int nextStepTick = 0;            // When to trigger the next step
    private boolean patternCompleted = false; // Flag for when pattern has completed but transport continues
    
    // Pattern parameters
    private int patternLength = 16;          // Number of steps in pattern
    private boolean isLooping = true;        // Whether pattern should loop
    private Direction direction = Direction.FORWARD; // Playback direction
    private boolean bounceForward = true;    // Used for bounce direction
    
    // Timing & MIDI parameters
    private TimingDivision timingDivision = TimingDivision.NORMAL;
    private int latencyCompensation = 20;    // ms to compensate for system latency
    private int activeMidiChannel = 15;      // Use channel 16 (15-based index)
    private int lookAheadMs = 40;            // How far ahead to schedule notes
    private boolean useAheadScheduling = true; // Enable/disable look-ahead
    private boolean isPlaying = false;       // Flag indicating playback state
    
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
    
    // Event handling
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;
    
    /**
     * Creates a new melodic sequencer
     */
    public MelodicSequencer() {
        // Initialize pattern data with default values
        initializePatternData();
        
        // Create a simple default pattern (every 4th step)
        for (int i = 0; i < 16; i += 4) {
            activeSteps.set(i, true);
            noteValues.set(i, 60 + (i % 12)); // C, E, G, B sequence
        }
        
        // Register with CommandBus
        CommandBus.getInstance().register(this);
        
        TimingBus.getInstance().register(this);

        // Initialize the quantizer with default settings
        updateQuantizer();
        
        // Set up a diagnostics timer to check if we're receiving timing updates
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                logger.info("MelodicSequencer diagnostic: isPlaying={}, currentStep={}, tickCounter={}",
                          isPlaying, stepCounter, tickCounter);
            }
        }, 5000, 5000); // Check every 5 seconds
        
        logger.info("MelodicSequencer initialized and registered with CommandBus");
    }
    
    /**
     * Initialize pattern data arrays with default values
     */
    private void initializePatternData() {
        activeSteps.clear();
        noteValues.clear();
        velocityValues.clear();
        gateValues.clear();
        
        // Fill with default values
        for (int i = 0; i < patternLength; i++) {
            activeSteps.add(false);          // All steps off by default
            noteValues.add(60);              // Middle C
            velocityValues.add(100);         // Medium-high velocity
            gateValues.add(50);              // 50% gate time
        }
    }
    
    /**
     * Process timing updates from the timing bus
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    // Process tick for step sequencing
                    tickCounter++;
                    
                    if (tickCounter >= nextStepTick) {
                        processStepAdvance();
                    }
                }
            }
            case Commands.TRANSPORT_START -> {
                resetSequence();
                isPlaying = true;
                logger.info("Sequencer started");
            }
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                // Notify UI that we stopped (with -1 as new step to indicate no active step)
                notifyStepUpdate(stepCounter, -1);
                logger.info("Sequencer stopped");
            }
            // Handle other commands as needed
        }
    }
    
    /**
     * Process step advance - calculate next step and trigger notes if needed
     */
    private void processStepAdvance() {
        // Remember current step for highlighting
        int oldStep = stepCounter;
        
        // Calculate the next step based on direction
        int nextStep = calculateNextStep(stepCounter, patternLength, direction);
        
        // Update step counter
        stepCounter = nextStep;
        
        // Notify UI of step change
        notifyStepUpdate(oldStep, stepCounter);
        
        // Check if a note should be triggered on this step
        if (stepCounter >= 0 && stepCounter < activeSteps.size() && activeSteps.get(stepCounter)) {
            triggerNote(stepCounter);
        }
        
        // Reset tick counter and calculate next step time
        tickCounter = 0;
        nextStepTick = ticksPerStep;
        
        // Log the step advance
        logger.debug("Step advanced to: {}", stepCounter);
    }
    
    /**
     * Trigger note for a specific step
     */
    private void triggerNote(int step) {
        if (step < 0 || step >= noteValues.size()) {
            return;
        }
        
        // Get note value
        int noteValue = noteValues.get(step);
        
        // Apply quantization if enabled
        int quantizedNote = quantizeNote(noteValue);
        
        // Apply octave shift
        int shiftedNote = applyOctaveShift(quantizedNote);
        
        // Get velocity and gate
        int velocity = step < velocityValues.size() ? velocityValues.get(step) : 100;
        int gate = step < gateValues.size() ? gateValues.get(step) : 100;
        
        // Create note event
        NoteEvent event = new NoteEvent(shiftedNote, velocity, gate);
        
        // Notify listener
        if (noteEventListener != null) {
            noteEventListener.accept(event);
        }
    }
    
    /**
     * Notifies listeners about step updates
     */
    private void notifyStepUpdate(int oldStep, int newStep) {
        StepUpdateEvent event = new StepUpdateEvent(oldStep, newStep);
        
        // 1. Notify direct listeners
        if (stepUpdateListener != null) {
            stepUpdateListener.accept(event);
        }
        
        // 2. Also publish on command bus so other components can respond
        CommandBus.getInstance().publish(Commands.SEQUENCER_STEP_UPDATE, this, event);
        
        // Log the update
        logger.debug("Published step update: {} -> {}", oldStep, newStep);
    }
    
    /**
     * Reset the sequence to initial state
     */
    public void resetSequence() {
        stepCounter = 0;
        tickCounter = 0;
        patternCompleted = false;
        nextStepTick = ticksPerStep;
    }
    
    /**
     * Calculate the next step based on the current direction
     */
    private int calculateNextStep(int currentStep, int patternLength, Direction direction) {
        switch (direction) {
            case FORWARD:
                return (currentStep + 1) % patternLength;
                
            case BACKWARD:
                return (currentStep - 1 + patternLength) % patternLength;
                
            case BOUNCE:
                if (bounceForward) {
                    int nextStep = currentStep + 1;
                    if (nextStep >= patternLength - 1) {
                        bounceForward = false;
                        return Math.max(0, patternLength - 1);
                    }
                    return nextStep;
                } else {
                    int nextStep = currentStep - 1;
                    if (nextStep <= 0) {
                        bounceForward = true;
                        return 0;
                    }
                    return nextStep;
                }
                
            case RANDOM:
                return (int) (Math.random() * patternLength);
                
            default:
                return (currentStep + 1) % patternLength;
        }
    }
    
    /**
     * Check if the pattern has ended based on current and next step
     */
    private boolean hasPatternEnded(int currentStep, int nextStep, int patternLength, Direction direction) {
        switch (direction) {
            case FORWARD:
                return currentStep == patternLength - 1 && nextStep == 0;
                
            case BACKWARD:
                return currentStep == 0 && nextStep == patternLength - 1;
                
            case BOUNCE:
                // In bounce mode, we consider the pattern ended when
                // we reach either end and change direction
                if (bounceForward) {
                    return currentStep == patternLength - 2 && nextStep == patternLength - 1;
                } else {
                    return currentStep == 1 && nextStep == 0;
                }
                
            case RANDOM:
                // For random mode, pattern only ends at the last step index
                return currentStep == patternLength - 1;
                
            default:
                return false;
        }
    }
    
    /**
     * Apply quantization to a note if enabled
     */
    public int quantizeNote(int note) {
        if (quantizer != null && quantizeEnabled) {
            return quantizer.quantizeNote(note);
        }
        return note;
    }
    
    /**
     * Apply octave shift to a note
     */
    public int applyOctaveShift(int note) {
        // Add 12 semitones per octave
        int shiftedNote = note + (octaveShift * 12);
        
        // Ensure the note is within valid MIDI range (0-127)
        return Math.max(0, Math.min(127, shiftedNote));
    }
    
    /**
     * Update the quantizer based on selected root note and scale
     */
    public void updateQuantizer() {
        try {
            scaleNotes = com.angrysurfer.core.sequencer.Scale.getScale(selectedRootNote, selectedScale);
            quantizer = new Quantizer(scaleNotes);
            logger.info("Quantizer updated for {} {}", selectedRootNote, selectedScale);
        } catch (Exception e) {
            logger.error("Error creating quantizer: {}", e.getMessage());
            // Default to chromatic scale if there's an error
            Boolean[] chromaticScale = new Boolean[12];
            for (int i = 0; i < 12; i++) {
                chromaticScale[i] = true;
            }
            scaleNotes = chromaticScale;
            quantizer = new Quantizer(scaleNotes);
        }
    }
    
    /**
     * Set step data for a specific step index
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        if (stepIndex < 0 || stepIndex >= patternLength) {
            return;
        }
        
        // Resize collections if needed
        ensureCapacity(stepIndex + 1);
        
        // Update step data
        activeSteps.set(stepIndex, active);
        noteValues.set(stepIndex, note);
        velocityValues.set(stepIndex, velocity);
        gateValues.set(stepIndex, gate);
    }
    
    /**
     * Ensure collections have at least the specified capacity
     */
    private void ensureCapacity(int capacity) {
        while (activeSteps.size() < capacity) {
            activeSteps.add(false);
        }
        
        while (noteValues.size() < capacity) {
            noteValues.add(60);
        }
        
        while (velocityValues.size() < capacity) {
            velocityValues.add(100);
        }
        
        while (gateValues.size() < capacity) {
            gateValues.add(50);
        }
    }
    
    /**
     * Clear the pattern (set all steps inactive)
     */
    public void clearPattern() {
        for (int i = 0; i < activeSteps.size(); i++) {
            activeSteps.set(i, false);
        }
        logger.info("Pattern cleared");
    }
    
    /**
     * Generate random pattern data
     */
    public void generatePattern(int octaveRange, int density) {
        // Clear existing pattern
        clearPattern();
        
        // Calculate note range based on octaves
        int baseNote = 60 - ((octaveRange * 12) / 2); // Center around middle C (60)
        int totalNoteRange = octaveRange * 12;
        
        logger.info("Generating pattern with {} octave range: {} to {}", 
                octaveRange, baseNote, baseNote + totalNoteRange - 1);
        
        // Generate for each step
        for (int i = 0; i < patternLength; i++) {
            // Determine if step should be active based on density (0-100)
            boolean activateStep = Math.random() * 100 < density;
            
            if (activateStep) {
                // Calculate random note within range
                int randomNote = baseNote + (int)(Math.random() * totalNoteRange);
                randomNote = Math.max(24, Math.min(96, randomNote));
                
                // Quantize if enabled
                if (quantizeEnabled && quantizer != null) {
                    randomNote = quantizeNote(randomNote);
                }
                
                // Generate random velocity and gate values
                int velocity = 40 + (int)(Math.random() * 60);  // 40-100
                int gate = 30 + (int)(Math.random() * 50);      // 30-80
                
                // Set the step data
                setStepData(i, true, randomNote, velocity, gate);
            }
        }
    }
    
    /**
     * Set step update listener
     */
    public void setStepUpdateListener(Consumer<StepUpdateEvent> listener) {
        this.stepUpdateListener = listener;
    }
    
    /**
     * Set note event listener
     */
    public void setNoteEventListener(Consumer<NoteEvent> listener) {
        this.noteEventListener = listener;
    }
    
    public void decrementOctaveShift() {
        octaveShift--;
    }

    public void incrementOctaveShift() {
        octaveShift++;
    }

    // Method name compatibility for root note
    public void setRootNote(String selectedRootNote) {
        this.selectedRootNote = selectedRootNote;
    }

    public String getRootNote() {
        return this.selectedRootNote;
    }

    // Method name compatibility for scale
    public void setScale(String scale) {
        this.selectedScale = scale;
    }

    public String getScale() {
        return this.selectedScale;
    }
}
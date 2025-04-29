package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Direction;

import lombok.Getter;
import lombok.Setter;

/**
 * Data container for melodic sequencer patterns and settings
 */
@Getter
@Setter
public class MelodicSequenceData {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceData.class);

    // Swing settings
    private int swingPercentage = 50; // Default swing percentage (50 = no swing)
    private boolean swingEnabled = false; // Swing enabled flag

    // Sequencing parameters
    private int patternLength = 16; // Default pattern length
    private Direction direction = Direction.FORWARD; // Default direction
    private boolean looping = true; // Default to looping
    private int channel = 3; // Default MIDI channel
    private TimingDivision timingDivision = TimingDivision.NORMAL;

    // Scale & quantization
    private String rootNote = "C";
    private String scale = Scale.SCALE_CHROMATIC;
    private Boolean[] scaleNotes;
    private boolean quantizeEnabled = true;
    private int octaveShift = 0; // Compatibility for scale

    // Pattern data storage
    private List<Boolean> activeSteps = new ArrayList<>(); // Step on/off state
    private List<Integer> noteValues = new ArrayList<>(); // Note for each step
    private List<Integer> velocityValues = new ArrayList<>(); // Velocity for each step
    private List<Integer> gateValues = new ArrayList<>(); // Gate time for each step
    private List<Integer> probabilityValues = new ArrayList<>(); // Probability for each step
    private List<Integer> nudgeValues = new ArrayList<>(); // Timing nudge for each step
    private List<Integer> harmonicTiltValues = new ArrayList<>(16); // Harmonic tilt values
    
    // Identifying information
    private Long id = 0L;
    private Integer sequencerId;
    private String name = "Unnamed Pattern";
    
    /**
     * Default constructor
     */
    public MelodicSequenceData() {
        initializePatternData();
    }
    
    /**
     * Initialize pattern data with default values
     */
    public void initializePatternData() {
        activeSteps = new ArrayList<>(patternLength);
        noteValues = new ArrayList<>(patternLength);
        velocityValues = new ArrayList<>(patternLength);
        gateValues = new ArrayList<>(patternLength);
        probabilityValues = new ArrayList<>(patternLength);
        nudgeValues = new ArrayList<>(patternLength);
        harmonicTiltValues = new ArrayList<>(patternLength);

        // Fill with default values
        for (int i = 0; i < patternLength; i++) {
            activeSteps.add(false); // All steps off by default
            noteValues.add(60); // Middle C
            velocityValues.add(100); // Medium-high velocity
            gateValues.add(50); // 50% gate time
            probabilityValues.add(100); // 100% probability by default
            nudgeValues.add(0); // No timing nudge by default
            harmonicTiltValues.add(0); // No harmonic tilt by default
        }
    }
    
    /**
     * Get the note value for a specific step
     * 
     * @param stepIndex The step index
     * @return The note value (0-127)
     */
    public int getNoteValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < noteValues.size()) {
            return noteValues.get(stepIndex);
        }
        return 60; // Default to middle C
    }
    
    /**
     * Get the velocity value for a specific step
     * 
     * @param stepIndex The step index
     * @return The velocity value (0-127)
     */
    public int getVelocityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < velocityValues.size()) {
            return velocityValues.get(stepIndex);
        }
        return 100; // Default velocity
    }
    
    /**
     * Get the gate value for a specific step
     * 
     * @param stepIndex The step index
     * @return The gate value (0-100)
     */
    public int getGateValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < gateValues.size()) {
            return gateValues.get(stepIndex);
        }
        return 50; // Default gate
    }
    
    /**
     * Get the probability value for a specific step
     * 
     * @param stepIndex The step index
     * @return The probability value (0-100)
     */
    public int getProbabilityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < probabilityValues.size()) {
            return probabilityValues.get(stepIndex);
        }
        return 100; // Default probability
    }
    
    /**
     * Set the probability value for a specific step
     * 
     * @param stepIndex The step index
     * @param probability The probability value (0-100)
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
     * Get the nudge value for a specific step
     * 
     * @param stepIndex The step index
     * @return The nudge value in milliseconds
     */
    public int getNudgeValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < nudgeValues.size()) {
            return nudgeValues.get(stepIndex);
        }
        return 0; // Default nudge
    }
    
    /**
     * Set the nudge value for a specific step
     * 
     * @param stepIndex The step index
     * @param nudge The nudge value in milliseconds
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
     * Check if a specific step is active
     * 
     * @param stepIndex The step index
     * @return True if the step is active, false otherwise
     */
    public boolean isStepActive(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < activeSteps.size()) {
            return activeSteps.get(stepIndex);
        }
        return false;
    }
    
    /**
     * Set all data for a specific step
     * 
     * @param stepIndex The step index
     * @param active Whether the step is active
     * @param note The note value
     * @param velocity The velocity value
     * @param gate The gate value
     * @param probability The probability value
     * @param nudge The nudge value
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability, int nudge) {
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
    
    /**
     * Set all data for a specific step including harmonic tilt
     * 
     * @param stepIndex The step index
     * @param active Whether the step is active
     * @param note The note value
     * @param velocity The velocity value
     * @param gate The gate value
     * @param probability The probability value
     * @param nudge The nudge value
     * @param harmonicTilt The harmonic tilt value
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, 
                            int probability, int nudge, int harmonicTilt) {
        // Set all the other values first
        setStepData(stepIndex, active, note, velocity, gate, probability, nudge);
        
        // Set harmonic tilt value
        while (harmonicTiltValues.size() <= stepIndex) {
            harmonicTiltValues.add(0);
        }
        harmonicTiltValues.set(stepIndex, harmonicTilt);
    }
    
    /**
     * Set step data with default probability and nudge
     * (for backward compatibility)
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        setStepData(stepIndex, active, note, velocity, gate, 100, 0);
    }
    
    /**
     * Rotate the pattern one step to the right
     */
    public void rotatePatternRight() {
        if (patternLength <= 1) {
            return;
        }

        // Ensure all lists have adequate size
        while (activeSteps.size() < patternLength) activeSteps.add(false);
        while (noteValues.size() < patternLength) noteValues.add(60);
        while (velocityValues.size() < patternLength) velocityValues.add(100);
        while (gateValues.size() < patternLength) gateValues.add(50);
        while (probabilityValues.size() < patternLength) probabilityValues.add(100);
        while (nudgeValues.size() < patternLength) nudgeValues.add(0);

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
    }
    
    /**
     * Rotate the pattern one step to the left
     */
    public void rotatePatternLeft() {
        if (patternLength <= 1) {
            return;
        }

        // Ensure all lists have adequate size
        while (activeSteps.size() < patternLength) activeSteps.add(false);
        while (noteValues.size() < patternLength) noteValues.add(60);
        while (velocityValues.size() < patternLength) velocityValues.add(100);
        while (gateValues.size() < patternLength) gateValues.add(50);
        while (probabilityValues.size() < patternLength) probabilityValues.add(100);
        while (nudgeValues.size() < patternLength) nudgeValues.add(0);

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
    }
    
    /**
     * Clear the pattern (set all steps to inactive)
     */
    public void clearPattern() {
        for (int i = 0; i < patternLength; i++) {
            if (i < activeSteps.size()) {
                activeSteps.set(i, false);
            } else {
                activeSteps.add(false);
            }
        }
    }
    
    /**
     * Generate a random pattern
     * 
     * @param octaveRange Number of octaves to span (1-4)
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
            
            // Default probability and nudge
            if (step < probabilityValues.size()) {
                probabilityValues.set(step, 100);
            }
            if (step < nudgeValues.size()) {
                nudgeValues.set(step, 0);
            }
        }
    }
    
    /**
     * Get the harmonic tilt value for a specific step
     * 
     * @param stepIndex The step index
     * @return The harmonic tilt value
     */
    public int getHarmonicTiltValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < harmonicTiltValues.size()) {
            return harmonicTiltValues.get(stepIndex);
        }
        return 0; // Default tilt value
    }
    
    /**
     * Set the harmonic tilt value for a specific step
     * 
     * @param stepIndex The step index
     * @param tiltValue The harmonic tilt value
     */
    public void setHarmonicTiltValue(int stepIndex, int tiltValue) {
        // Ensure step index is valid
        if (stepIndex >= 0) {
            // Expand list if needed
            while (harmonicTiltValues.size() <= stepIndex) {
                harmonicTiltValues.add(0);
            }
            harmonicTiltValues.set(stepIndex, tiltValue);
        }
    }
    
    /**
     * Initialize harmonic tilt values with defaults
     */
    public void initializeHarmonicTiltValues() {
        harmonicTiltValues.clear();
        for (int i = 0; i < 16; i++) {
            harmonicTiltValues.add(0);
        }
    }

    /**
     * Create a Boolean array representing which notes are in the scale
     * 
     * @param rootNote The root note as a string (e.g., "C", "F#")
     * @param scaleName The scale name (e.g., "MAJOR", "MINOR")
     * @return Boolean array where true indicates notes in the scale
     */
    public Boolean[] createScaleArray(String rootNote, String scaleName) {
        Boolean[] result = new Boolean[12];
        
        // Default to all notes if invalid input
        if (rootNote == null || scaleName == null) {
            for (int i = 0; i < 12; i++) {
                result[i] = Boolean.TRUE;
            }
            return result;
        }
        
        // Get the root note as integer (0-11)
        int rootIndex = Scale.getRootNoteIndex(rootNote);
        
        // Get the scale pattern
        int[] scalePattern = Scale.getScalePattern(scaleName);
        
        // Initialize all to false
        for (int i = 0; i < 12; i++) {
            result[i] = Boolean.FALSE;
        }
        
        // Mark notes in scale as true
        for (int offset : scalePattern) {
            int noteIndex = (rootIndex + offset) % 12;
            result[noteIndex] = Boolean.TRUE;
        }
        
        return result;
    }

    /**
     * Update the scale notes array based on current root note and scale
     */
    public void updateScaleNotes() {
        scaleNotes = createScaleArray(rootNote, scale);
    }
}
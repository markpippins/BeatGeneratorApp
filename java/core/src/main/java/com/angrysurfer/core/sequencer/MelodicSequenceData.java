package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    // private int channel = 3; // Default MIDI channel
    private TimingDivision timingDivision = TimingDivision.NORMAL;

    // Scale & quantization
    private String rootNote = "C";
    private String scale = Scale.SCALE_CHROMATIC;
    private Boolean[] scaleNotes;
    private boolean quantizeEnabled = true;
    private int octaveShift = 0; // Compatibility for scale

    // Pattern data storage
    private boolean[] activeSteps; // Step on/off state
    private int[] noteValues; // Note for each step
    private int[] velocityValues; // Velocity for each step
    private int[] gateValues; // Gate time for each step
    private int[] probabilityValues; // Probability for each step
    private int[] nudgeValues; // Timing nudge for each step
    private int[] harmonicTiltValues; // Harmonic til

    // Identifying information
    private Long id = 0L;
    private Integer sequencerId;
    private String name = "Unnamed Pattern";

    private String soundbankName = "Default";
    private Integer bankIndex = 0;
    private Integer preset = 0;

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
        // Always create new arrays with current pattern length
        activeSteps = new boolean[patternLength];
        noteValues = new int[patternLength];
        velocityValues = new int[patternLength];
        gateValues = new int[patternLength];
        probabilityValues = new int[patternLength];
        nudgeValues = new int[patternLength];
        harmonicTiltValues = new int[patternLength];

        // Fill with default values
        for (int i = 0; i < patternLength; i++) {
            activeSteps[i] = false; // All steps off by default
            noteValues[i] = 60; // Middle C
            velocityValues[i] = 100; // Medium-high velocity
            gateValues[i] = 50; // 50% gate time
            probabilityValues[i] = 100; // 100% probability by default
            nudgeValues[i] = 0; // No timing nudge by default
            harmonicTiltValues[i] = 0; // No harmonic tilt by default
        }
    }

    public int getNoteValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < noteValues.length) {
            return noteValues[stepIndex];
        }
        return 60; // Default to middle C
    }

    /**
     * Get the velocity value for a specific step
     */
    public int getVelocityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < velocityValues.length) {
            return velocityValues[stepIndex];
        }
        return 100; // Default velocity
    }

    /**
     * Get the gate value for a specific step
     */
    public int getGateValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < gateValues.length) {
            return gateValues[stepIndex];
        }
        return 50; // Default gate
    }

    /**
     * Get the probability value for a specific step
     */
    public int getProbabilityValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < probabilityValues.length) {
            return probabilityValues[stepIndex];
        }
        return 100; // Default probability
    }

    /**
     * Set the probability value for a specific step
     */
    public void setProbabilityValue(int stepIndex, int probability) {
        probability = Math.max(0, Math.min(100, probability));

        if (stepIndex >= 0) {
            ensureArraySize(stepIndex + 1);
            probabilityValues[stepIndex] = probability;
        }
    }

    /**
     * Get the nudge value for a specific step
     */
    public int getNudgeValue(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < nudgeValues.length) {
            return nudgeValues[stepIndex];
        }
        return 0; // Default nudge
    }

    /**
     * Set the nudge value for a specific step
     */
    public void setNudgeValue(int stepIndex, int nudge) {
        nudge = Math.max(0, nudge);

        if (stepIndex >= 0) {
            ensureArraySize(stepIndex + 1);
            nudgeValues[stepIndex] = nudge;
        }
    }

    public int[] getHarmonicTiltValuesRaw() {
        return harmonicTiltValues;
    }

    /**
     * Get harmonic tilt values as a list with safety checks
     */
    public List<Integer> getHarmonicTiltValues() {
        // Add null check to prevent NullPointerException
        if (harmonicTiltValues == null) {
            logger.warn("harmonicTiltValues array is null, initializing with defaults for pattern length {}",
                    patternLength);
            harmonicTiltValues = new int[patternLength];
        }

        List<Integer> result = Arrays.stream(harmonicTiltValues).boxed().collect(Collectors.toList());
        logger.debug("MelodicSequenceData.getHarmonicTiltValues() returning list of size {}", result.size());

        return result;
    }

    // Add a setter for arrays if not already present
    public void setHarmonicTiltValues(int[] values) {
        if (values == null) {
            logger.warn("Attempt to set null harmonicTiltValues, ignoring");
            return;
        }

        harmonicTiltValues = new int[values.length];
        System.arraycopy(values, 0, harmonicTiltValues, 0, values.length);
        logger.debug("Set {} harmonic tilt values", values.length);
    }

    /**
     * Set a single harmonic tilt value with safety checks
     */
    public void setHarmonicTiltValue(int index, int value) {
        // Ensure the array exists
        if (harmonicTiltValues == null) {
            logger.info("Creating new harmonicTiltValues array with length {}", patternLength);
            harmonicTiltValues = new int[patternLength];
        }

        // Resize if needed
        if (index >= harmonicTiltValues.length) {
            int[] newArray = new int[Math.max(patternLength, index + 1)];
            System.arraycopy(harmonicTiltValues, 0, newArray, 0, harmonicTiltValues.length);
            harmonicTiltValues = newArray;
        }

        // Set the value
        harmonicTiltValues[index] = value;
    }

    /**
     * Check if a specific step is active
     */
    public boolean isStepActive(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < activeSteps.length) {
            return activeSteps[stepIndex];
        }
        return false;
    }

    /**
     * Ensure arrays are large enough to accommodate the given index
     */
    private void ensureArraySize(int requiredSize) {
        if (requiredSize > patternLength) {
            // Update pattern length
            int oldLength = patternLength;
            patternLength = requiredSize;

            // Resize activeSteps array
            boolean[] newActiveSteps = new boolean[patternLength];
            System.arraycopy(activeSteps, 0, newActiveSteps, 0, Math.min(activeSteps.length, patternLength));
            activeSteps = newActiveSteps;

            // Resize noteValues array
            int[] newNoteValues = new int[patternLength];
            System.arraycopy(noteValues, 0, newNoteValues, 0, Math.min(noteValues.length, patternLength));
            for (int i = oldLength; i < patternLength; i++) {
                newNoteValues[i] = 60; // Default to middle C
            }
            noteValues = newNoteValues;

            // Resize velocityValues array
            int[] newVelocityValues = new int[patternLength];
            System.arraycopy(velocityValues, 0, newVelocityValues, 0, Math.min(velocityValues.length, patternLength));
            for (int i = oldLength; i < patternLength; i++) {
                newVelocityValues[i] = 100; // Default velocity
            }
            velocityValues = newVelocityValues;

            // Resize gateValues array
            int[] newGateValues = new int[patternLength];
            System.arraycopy(gateValues, 0, newGateValues, 0, Math.min(gateValues.length, patternLength));
            for (int i = oldLength; i < patternLength; i++) {
                newGateValues[i] = 50; // Default gate
            }
            gateValues = newGateValues;

            // Resize probabilityValues array
            int[] newProbabilityValues = new int[patternLength];
            System.arraycopy(probabilityValues, 0, newProbabilityValues, 0,
                    Math.min(probabilityValues.length, patternLength));
            for (int i = oldLength; i < patternLength; i++) {
                newProbabilityValues[i] = 100; // Default probability
            }
            probabilityValues = newProbabilityValues;

            // Resize nudgeValues array
            int[] newNudgeValues = new int[patternLength];
            System.arraycopy(nudgeValues, 0, newNudgeValues, 0, Math.min(nudgeValues.length, patternLength));
            nudgeValues = newNudgeValues;

            // Resize harmonicTiltValues array
            int[] newHarmonicTiltValues = new int[patternLength];
            System.arraycopy(harmonicTiltValues, 0, newHarmonicTiltValues, 0,
                    Math.min(harmonicTiltValues.length, patternLength));
            harmonicTiltValues = newHarmonicTiltValues;
        }
    }

    /**
     * Set all data for a specific step
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability,
            int nudge) {
        if (stepIndex >= 0) {
            ensureArraySize(stepIndex + 1);

            activeSteps[stepIndex] = active;
            noteValues[stepIndex] = note;
            velocityValues[stepIndex] = velocity;
            gateValues[stepIndex] = gate;
            probabilityValues[stepIndex] = Math.max(0, Math.min(100, probability));
            nudgeValues[stepIndex] = Math.max(0, nudge);
        }
    }

    /**
     * Set all data for a specific step including harmonic tilt
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate,
            int probability, int nudge, int harmonicTilt) {
        setStepData(stepIndex, active, note, velocity, gate, probability, nudge);

        if (stepIndex >= 0) {
            ensureArraySize(stepIndex + 1);
            harmonicTiltValues[stepIndex] = harmonicTilt;
        }
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
        if (patternLength <= 1)
            return;

        // Store the last step's values
        boolean lastActive = activeSteps[patternLength - 1];
        int lastNote = noteValues[patternLength - 1];
        int lastVelocity = velocityValues[patternLength - 1];
        int lastGate = gateValues[patternLength - 1];
        int lastProbability = probabilityValues[patternLength - 1];
        int lastNudge = nudgeValues[patternLength - 1];
        // int lastHarmonicTilt = harmonicTiltValues[patternLength - 1];

        // Shift elements right
        for (int i = patternLength - 1; i > 0; i--) {
            activeSteps[i] = activeSteps[i - 1];
            noteValues[i] = noteValues[i - 1];
            velocityValues[i] = velocityValues[i - 1];
            gateValues[i] = gateValues[i - 1];
            probabilityValues[i] = probabilityValues[i - 1];
            nudgeValues[i] = nudgeValues[i - 1];
            // harmonicTiltValues[i] = harmonicTiltValues[i - 1];
        }

        // Place saved last values at the first position
        activeSteps[0] = lastActive;
        noteValues[0] = lastNote;
        velocityValues[0] = lastVelocity;
        gateValues[0] = lastGate;
        probabilityValues[0] = lastProbability;
        nudgeValues[0] = lastNudge;
        // harmonicTiltValues[0] = lastHarmonicTilt;
    }

    /**
     * Rotate the pattern one step to the left
     */
    public void rotatePatternLeft() {
        if (patternLength <= 1)
            return;

        // Store the first step's values
        boolean firstActive = activeSteps[0];
        int firstNote = noteValues[0];
        int firstVelocity = velocityValues[0];
        int firstGate = gateValues[0];
        int firstProbability = probabilityValues[0];
        int firstNudge = nudgeValues[0];
        // int firstHarmonicTilt = harmonicTiltValues[0];

        // Shift elements left
        for (int i = 0; i < patternLength - 1; i++) {
            activeSteps[i] = activeSteps[i + 1];
            noteValues[i] = noteValues[i + 1];
            velocityValues[i] = velocityValues[i + 1];
            gateValues[i] = gateValues[i + 1];
            probabilityValues[i] = probabilityValues[i + 1];
            nudgeValues[i] = nudgeValues[i + 1];
            // harmonicTiltValues[i] = harmonicTiltValues[i + 1];
        }

        // Place saved first values at the last position
        activeSteps[patternLength - 1] = firstActive;
        noteValues[patternLength - 1] = firstNote;
        velocityValues[patternLength - 1] = firstVelocity;
        gateValues[patternLength - 1] = firstGate;
        probabilityValues[patternLength - 1] = firstProbability;
        nudgeValues[patternLength - 1] = firstNudge;
        // harmonicTiltValues[patternLength - 1] = firstHarmonicTilt;
    }

    /**
     * Clear the pattern (set all steps to inactive)
     */
    public void clearPattern() {
        for (int i = 0; i < patternLength; i++) {
            activeSteps[i] = false;
        }
    }

    /**
     * Generate a random pattern
     * 
     * @param octaveRange Number of octaves to span (1-4)
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

    }

    /**
     * Create a Boolean array representing which notes are in the scale
     * 
     * @param rootNote  The root note as a string (e.g., "C", "F#")
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

    /**
     * Get probability values as a List<Integer>
     */
    public List<Integer> getProbabilityValues() {
        // Add null check to prevent NullPointerException
        if (probabilityValues == null) {
            logger.warn("probabilityValues array is null, initializing with defaults");
            probabilityValues = new int[patternLength];
            // Fill with default values (100%)
            for (int i = 0; i < patternLength; i++) {
                probabilityValues[i] = 100;
            }
        }
        return Arrays.stream(probabilityValues).boxed().collect(Collectors.toList());
    }

    /**
     * Get nudge values as a List<Integer>
     */
    public List<Integer> getNudgeValues() {
        // Add null check to prevent NullPointerException
        if (nudgeValues == null) {
            logger.warn("nudgeValues array is null, initializing with defaults");
            nudgeValues = new int[patternLength];
            // All zeros by default
        }
        return Arrays.stream(nudgeValues).boxed().collect(Collectors.toList());
    }

    /**
     * Get active steps as a List<Boolean>
     */
    public List<Boolean> getActiveSteps() {
        // Add null check to prevent NullPointerException
        if (activeSteps == null) {
            logger.warn("activeSteps array is null, initializing with defaults");
            activeSteps = new boolean[patternLength];
            // All false by default
        }

        Boolean[] result = new Boolean[activeSteps.length];
        for (int i = 0; i < activeSteps.length; i++) {
            result[i] = activeSteps[i];
        }
        return Arrays.asList(result);
    }

    /**
     * Get note values as a List<Integer>
     */
    public List<Integer> getNoteValues() {
        // Add null check to prevent NullPointerException
        if (noteValues == null) {
            logger.warn("noteValues array is null, initializing with defaults");
            noteValues = new int[patternLength];
            // Fill with default values (middle C)
            for (int i = 0; i < patternLength; i++) {
                noteValues[i] = 60;
            }
        }
        return Arrays.stream(noteValues).boxed().collect(Collectors.toList());
    }

    /**
     * Get velocity values as a List<Integer>
     */
    public List<Integer> getVelocityValues() {
        // Add null check to prevent NullPointerException
        if (velocityValues == null) {
            logger.warn("velocityValues array is null, initializing with defaults");
            velocityValues = new int[patternLength];
            // Fill with default velocity (100)
            for (int i = 0; i < patternLength; i++) {
                velocityValues[i] = 100;
            }
        }
        return Arrays.stream(velocityValues).boxed().collect(Collectors.toList());
    }

    /**
     * Get gate values as a List<Integer>
     */
    public List<Integer> getGateValues() {
        // Add null check to prevent NullPointerException
        if (gateValues == null) {
            logger.warn("gateValues array is null, initializing with defaults");
            gateValues = new int[patternLength];
            // Fill with default gate time (50%)
            for (int i = 0; i < patternLength; i++) {
                gateValues[i] = 50;
            }
        }
        return Arrays.stream(gateValues).boxed().collect(Collectors.toList());
    }

    /**
     * Handle pattern length changes
     */
    public void setPatternLength(int newLength) {
        if (newLength != patternLength) {
            int oldLength = patternLength;
            patternLength = newLength;
            ensureArraySize(patternLength);
        }
    }
}
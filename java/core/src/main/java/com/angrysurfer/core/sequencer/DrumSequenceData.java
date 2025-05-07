package com.angrysurfer.core.sequencer;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;

import lombok.Getter;
import lombok.Setter;

/**
 * Data storage class for DrumSequencer constants and state.
 * This class is responsible for storing all the pattern data and constants
 * used by the DrumSequencer.
 */
@Getter
@Setter
public class DrumSequenceData {
    // Constants
    public static final int DRUM_PAD_COUNT = 16; // Number of drum pads

    // MIDI and music constants
    public static final int MIDI_DRUM_CHANNEL = 9; // Standard MIDI drum channel
    public static final int MIDI_DRUM_NOTE_OFFSET = 36; // First drum pad note number
    public static final int MAX_MIDI_VELOCITY = 127; // Maximum MIDI velocity

    // MIDI CC values
    public static final int CC_VOLUME = 7;
    public static final int CC_PAN = 10;
    public static final int CC_REVERB = 91;
    public static final int CC_CHORUS = 93;
    public static final int CC_DELAY = 94; // Use for decay

    // Default values for parameters
    public static final int DEFAULT_VELOCITY = 100; // Default note velocity
    public static final int DEFAULT_DECAY = 60; // Default note decay
    public static final int DEFAULT_PROBABILITY = 100; // Default step probability (%)
    public static final int DEFAULT_TICKS_PER_BEAT = 24; // Default timing fallback
    public static final int DEFAULT_MASTER_TEMPO = 96; // Default master tempo
    public static final int DEFAULT_PAN = 64; // Default pan position (center)
    public static final int DEFAULT_CHORUS = 0; // Default chorus effect amount
    public static final int DEFAULT_REVERB = 0; // Default reverb effect amount

    // Swing parameters
    public static final int NO_SWING = 50; // Percentage value for no swing
    public static final int MAX_SWING = 99; // Maximum swing percentage
    public static final int MIN_SWING = 25; // Minimum swing percentage

    // Pattern generation parameters
    public static final int MAX_DENSITY = 10; // Maximum density for pattern generation

    // Button dimensions
    public static final int DRUM_PAD_SIZE = 28; // Standard drum pad button size

    // Pattern length defaults
    private int defaultPatternLength = 32; // Default pattern length
    private int maxPatternLength = 64; // Maximum pattern length

    // Global sequencing state
    private long tickCounter = 0; // Count ticks
    private int beatCounter = 0; // Count beats
    private int ticksPerStep = 24; // Base ticks per step
    private boolean isPlaying = false; // Global play state
    private int absoluteStep = 0; // Global step counter independent of individual drum steps

    // Per-drum sequencing state
    private int[] currentStep; // Current step for each drum
    private boolean[] patternCompleted; // Pattern completion flag for each drum
    private long[] nextStepTick; // Next step trigger tick for each drum

    // Per-drum pattern parameters
    private int[] patternLengths; // Pattern length for each drum
    private Direction[] directions; // Direction for each drum
    private TimingDivision[] timingDivisions; // Timing for each drum
    private boolean[] loopingFlags; // Loop setting for each drum
    private int[] bounceDirections; // 1 for forward, -1 for backward (for bounce mode)
    private int[] velocities; // Velocity for each drum
    private int[] originalVelocities; // Saved original velocities for resetting

    // Pattern data storage
    private boolean[][] patterns; // [drumIndex][stepIndex]

    // Per-step parameter values for each drum
    private int[][] stepVelocities; // Velocity for each step of each drum [drumIndex][stepIndex]
    private int[][] stepDecays; // Decay (gate time) for each step of each drum [drumIndex][stepIndex]
    private int[][] stepProbabilities; // Probability for each step [drumIndex][stepIndex]
    private int[][] stepNudges; // Timing nudge for each step [drumIndex][stepIndex]

    // Effect parameters
    private int[][] stepPans; // Pan position (0-127) for each step [drumIndex][stepIndex]
    private int[][] stepChorus; // Chorus amount (0-100) for each step [drumIndex][stepIndex]
    private int[][] stepReverb; // Reverb amount (0-100) for each step [drumIndex][stepIndex]

    // Track last sent effect values to avoid redundant MIDI messages
    private int[][] lastSentPans;
    private int[][] lastSentChorus;
    private int[][] lastSentReverb;
    private int[][] lastSentDecays; // For delay effect

    // These track the last sent values to avoid redundant messages
    private int[][] lastPanValues;
    private int[][] lastReverbValues;
    private int[][] lastChorusValues;
    private int[][] lastDecayValues;

    // Selection state
    private int selectedPadIndex = 0; // Currently selected drum pad

    // Swing parameters
    private int swingPercentage = 50; // Default swing percentage (50 = no swing)
    private boolean swingEnabled = false; // Swing enabled flag


    // Master tempo
    private int masterTempo;

    // Add field for next pattern ID
    private Long nextPatternId = null;

    // Track pattern completion for switching
    private boolean patternJustCompleted = false;

    // Reusable arrays for effects to avoid constant object creation
    private final int[] effectControllers = new int[4];
    private final int[] effectValues = new int[4];

    // Sequence identifier
    private Long id = -1L; // Unique ID for the sequence

    // Optional name/description
    private String name;

    // Pattern parameters per drum

    /**
     * Initialize drum sequencer data with default values
     */
    public DrumSequenceData() {
        initializeArrays();
    }

    /**
     * Initialize all arrays with default values
     */
    private void initializeArrays() {
        // Initialize per-drum state arrays
        currentStep = new int[DRUM_PAD_COUNT];
        patternCompleted = new boolean[DRUM_PAD_COUNT];
        nextStepTick = new long[DRUM_PAD_COUNT];

        // Initialize per-drum parameter arrays
        patternLengths = new int[DRUM_PAD_COUNT];
        directions = new Direction[DRUM_PAD_COUNT];
        timingDivisions = new TimingDivision[DRUM_PAD_COUNT];
        loopingFlags = new boolean[DRUM_PAD_COUNT];
        bounceDirections = new int[DRUM_PAD_COUNT];
        velocities = new int[DRUM_PAD_COUNT];
        originalVelocities = new int[DRUM_PAD_COUNT];

        // Set default values
        java.util.Arrays.fill(velocities, DEFAULT_VELOCITY);
        java.util.Arrays.fill(originalVelocities, DEFAULT_VELOCITY);
        java.util.Arrays.fill(patternLengths, defaultPatternLength);
        java.util.Arrays.fill(directions, Direction.FORWARD);
        java.util.Arrays.fill(timingDivisions, TimingDivision.NORMAL);
        java.util.Arrays.fill(loopingFlags, true);
        java.util.Arrays.fill(bounceDirections, 1);

        // Set master tempo default
        masterTempo = DEFAULT_MASTER_TEMPO;

        // Initialize pattern and step parameter arrays
        patterns = new boolean[DRUM_PAD_COUNT][maxPatternLength];
        stepVelocities = new int[DRUM_PAD_COUNT][maxPatternLength];
        stepDecays = new int[DRUM_PAD_COUNT][maxPatternLength];
        stepProbabilities = new int[DRUM_PAD_COUNT][maxPatternLength];
        stepNudges = new int[DRUM_PAD_COUNT][maxPatternLength];

        // Initialize effect arrays
        stepPans = new int[DRUM_PAD_COUNT][maxPatternLength];
        stepChorus = new int[DRUM_PAD_COUNT][maxPatternLength];
        stepReverb = new int[DRUM_PAD_COUNT][maxPatternLength];

        // Initialize effect tracking arrays
        lastSentPans = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastSentChorus = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastSentReverb = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastSentDecays = new int[DRUM_PAD_COUNT][maxPatternLength];

        lastPanValues = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastReverbValues = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastChorusValues = new int[DRUM_PAD_COUNT][maxPatternLength];
        lastDecayValues = new int[DRUM_PAD_COUNT][maxPatternLength];

        // Initialize arrays with default values
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < maxPatternLength; j++) {
                // Set initial values to -1 to ensure first message gets sent
                lastPanValues[i][j] = -1;
                lastReverbValues[i][j] = -1;
                lastChorusValues[i][j] = -1;
                lastDecayValues[i][j] = -1;

                // Set default values for step parameters
                stepVelocities[i][j] = DEFAULT_VELOCITY;
                stepDecays[i][j] = DEFAULT_DECAY;
                stepProbabilities[i][j] = DEFAULT_PROBABILITY;
                stepNudges[i][j] = 0;

                // Set defaults for effect parameters
                stepPans[i][j] = DEFAULT_PAN;
                stepChorus[i][j] = DEFAULT_CHORUS;
                stepReverb[i][j] = DEFAULT_REVERB;

                // Initialize last sent values to invalid defaults
                lastSentPans[i][j] = -1;
                lastSentChorus[i][j] = -1;
                lastSentReverb[i][j] = -1;
                lastSentDecays[i][j] = -1;
            }
        }

    }

    /**
     * Get whether a step is active for a specific drum
     * 
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return true if the step is active
     */
    public boolean isStepActive(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT &&
                stepIndex >= 0 && stepIndex < maxPatternLength) {
            return patterns[drumIndex][stepIndex];
        }
        return false;
    }

    /**
     * Toggle a step on/off for a specific drum
     * 
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     * @return The new state of the step
     */
    public boolean toggleStep(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT &&
                stepIndex >= 0 && stepIndex < maxPatternLength) {
            patterns[drumIndex][stepIndex] = !patterns[drumIndex][stepIndex];
            return patterns[drumIndex][stepIndex];
        }
        return false;
    }

    /**
     * Reset all pattern data to initial state
     * 
     * @param preservePositions Whether to preserve current positions
     */
    public void reset(boolean preservePositions) {
        if (!preservePositions) {
            // Reset step positions and state
            java.util.Arrays.fill(currentStep, 0);
            java.util.Arrays.fill(patternCompleted, false);
            java.util.Arrays.fill(nextStepTick, 0);
            java.util.Arrays.fill(bounceDirections, 1);

            // Reset global counters
            tickCounter = 0;
            beatCounter = 0;
            absoluteStep = 0;
        } else {
            // Just reset state flags but keep positions
            java.util.Arrays.fill(patternCompleted, false);
            java.util.Arrays.fill(currentStep, absoluteStep);
        }
    }

    /**
     * Calculate the next step for a drum based on its direction
     * 
     * @param drumIndex The drum to calculate for
     * @return The previous step index (for UI updates)
     */
    public int calculateNextStep(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            return 0;
        }

        // Store previous step for returning
        int previousStep = currentStep[drumIndex];

        Direction direction = directions[drumIndex];
        int length = patternLengths[drumIndex];

        switch (direction) {
            case FORWARD:
                currentStep[drumIndex] = (currentStep[drumIndex] + 1) % length;

                // Check for pattern completion
                if (currentStep[drumIndex] == 0) {
                    patternCompleted[drumIndex] = true;
                }
                break;

            case BACKWARD:
                currentStep[drumIndex] = (currentStep[drumIndex] - 1 + length) % length;

                // Check for pattern completion
                if (currentStep[drumIndex] == length - 1) {
                    patternCompleted[drumIndex] = true;
                }
                break;

            case BOUNCE:
                // Get bounce direction (1 or -1)
                int bounce = bounceDirections[drumIndex];

                // Move step
                currentStep[drumIndex] += bounce;

                // Check bounds and reverse if needed
                if (currentStep[drumIndex] >= length) {
                    currentStep[drumIndex] = length - 2;
                    bounceDirections[drumIndex] = -1;
                    patternCompleted[drumIndex] = true;
                } else if (currentStep[drumIndex] < 0) {
                    currentStep[drumIndex] = 1;
                    bounceDirections[drumIndex] = 1;
                    patternCompleted[drumIndex] = true;
                }
                break;

            case RANDOM:
                int oldStep = currentStep[drumIndex];
                // Generate a random step position
                currentStep[drumIndex] = (int) (Math.random() * length);

                // Ensure we don't get the same step twice in a row
                if (currentStep[drumIndex] == oldStep && length > 1) {
                    currentStep[drumIndex] = (currentStep[drumIndex] + 1) % length;
                }

                // Random is considered complete after each step
                patternCompleted[drumIndex] = true;
                break;
        }

        return previousStep;
    }

    /**
     * Clear all patterns (set all steps to inactive)
     */
    public void clearPatterns() {
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < maxPatternLength; j++) {
                patterns[i][j] = false;
            }
        }
    }

    /**
     * Check if all drum patterns are completed
     * 
     * @return true if all patterns are completed
     */
    public boolean areAllPatternsCompleted() {
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            if (!patternCompleted[i] && loopingFlags[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generate a random pattern for a drum with a specific density
     * 
     * @param drumIndex The drum index
     * @param density   The pattern density (1-10)
     */
    public void generatePattern(int drumIndex, int density) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            return;
        }

        int length = patternLengths[drumIndex];

        // Clear existing pattern
        for (int step = 0; step < length; step++) {
            patterns[drumIndex][step] = false;
        }

        // Generate new pattern based on density (1-10)
        int hitsToAdd = Math.max(1, Math.min(MAX_DENSITY, density)) * length / MAX_DENSITY;

        // Always add a hit on the first beat
        patterns[drumIndex][0] = true;
        hitsToAdd--;

        // Randomly distribute remaining hits
        while (hitsToAdd > 0) {
            int step = (int) (Math.random() * length);
            if (!patterns[drumIndex][step]) {
                patterns[drumIndex][step] = true;
                hitsToAdd--;
            }
        }
    }

    /**
     * Push a pattern forward by one step (rotation)
     * 
     * @param drumIndex The drum index
     */
    public void pushPatternForward(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            return;
        }

        int length = patternLengths[drumIndex];

        if (length <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate main pattern (trigger states)
        boolean lastTrigger = patterns[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            patterns[drumIndex][i] = patterns[drumIndex][i - 1];
        }
        patterns[drumIndex][0] = lastTrigger;

        // Rotate step velocities
        int lastVelocity = stepVelocities[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepVelocities[drumIndex][i] = stepVelocities[drumIndex][i - 1];
        }
        stepVelocities[drumIndex][0] = lastVelocity;

        // Rotate step decays
        int lastDecay = stepDecays[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepDecays[drumIndex][i] = stepDecays[drumIndex][i - 1];
        }
        stepDecays[drumIndex][0] = lastDecay;

        // Rotate step probabilities
        int lastProbability = stepProbabilities[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepProbabilities[drumIndex][i] = stepProbabilities[drumIndex][i - 1];
        }
        stepProbabilities[drumIndex][0] = lastProbability;

        // Rotate step nudges
        int lastNudge = stepNudges[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepNudges[drumIndex][i] = stepNudges[drumIndex][i - 1];
        }
        stepNudges[drumIndex][0] = lastNudge;

        // Rotate step pans
        int lastPan = stepPans[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepPans[drumIndex][i] = stepPans[drumIndex][i - 1];
        }
        stepPans[drumIndex][0] = lastPan;

        // Rotate step chorus
        int lastChorus = stepChorus[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepChorus[drumIndex][i] = stepChorus[drumIndex][i - 1];
        }
        stepChorus[drumIndex][0] = lastChorus;

        // Rotate step reverb
        int lastReverb = stepReverb[drumIndex][length - 1];
        for (int i = length - 1; i > 0; i--) {
            stepReverb[drumIndex][i] = stepReverb[drumIndex][i - 1];
        }
        stepReverb[drumIndex][0] = lastReverb;
    }

    /**
     * Pull a pattern backward by one step (rotation)
     * 
     * @param drumIndex The drum index
     */
    public void pullPatternBackward(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) {
            return;
        }

        int length = patternLengths[drumIndex];

        if (length <= 1) {
            return; // No need to rotate a single-step pattern
        }

        // Rotate main pattern (trigger states)
        boolean firstTrigger = patterns[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            patterns[drumIndex][i] = patterns[drumIndex][i + 1];
        }
        patterns[drumIndex][length - 1] = firstTrigger;

        // Rotate step velocities
        int firstVelocity = stepVelocities[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepVelocities[drumIndex][i] = stepVelocities[drumIndex][i + 1];
        }
        stepVelocities[drumIndex][length - 1] = firstVelocity;

        // Rotate step decays
        int firstDecay = stepDecays[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepDecays[drumIndex][i] = stepDecays[drumIndex][i + 1];
        }
        stepDecays[drumIndex][length - 1] = firstDecay;

        // Rotate step probabilities
        int firstProbability = stepProbabilities[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepProbabilities[drumIndex][i] = stepProbabilities[drumIndex][i + 1];
        }
        stepProbabilities[drumIndex][length - 1] = firstProbability;

        // Rotate step nudges
        int firstNudge = stepNudges[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepNudges[drumIndex][i] = stepNudges[drumIndex][i + 1];
        }
        stepNudges[drumIndex][length - 1] = firstNudge;

        // Rotate step pans
        int firstPan = stepPans[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepPans[drumIndex][i] = stepPans[drumIndex][i + 1];
        }
        stepPans[drumIndex][length - 1] = firstPan;

        // Rotate step chorus
        int firstChorus = stepChorus[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepChorus[drumIndex][i] = stepChorus[drumIndex][i + 1];
        }
        stepChorus[drumIndex][length - 1] = firstChorus;

        // Rotate step reverb
        int firstReverb = stepReverb[drumIndex][0];
        for (int i = 0; i < length - 1; i++) {
            stepReverb[drumIndex][i] = stepReverb[drumIndex][i + 1];
        }
        stepReverb[drumIndex][length - 1] = firstReverb;
    }
}
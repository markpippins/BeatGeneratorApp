package com.angrysurfer.core.sequencer;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data storage class for DrumSequencer constants and state.
 * This class is responsible for storing all the pattern data and constants
 * used by the DrumSequencer.
 */
@Getter
@Setter
public class DrumSequenceData {

    // Reusable arrays for effects to avoid constant object creation
    private final int[] effectControllers = new int[4];
    private final int[] effectValues = new int[4];
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
    // Sequence identifier
    private Long id = -1L; // Unique ID for the sequence

    // Optional name/description
    private String name;

    // Instrument information
    private Long[] instrumentIds; // Store instrument IDs for each drum
    private String[] soundbankNames; // Store soundbank names for each drum
    private Integer[] presets; // Store preset numbers for each drum
    private Integer[] bankIndices; // Store bank indices for each drum
    private String[] deviceNames; // Store device names for each drum
    private String[] instrumentNames; // Store instrument names for each drum

    // Root notes for each drum pad
    private int[] rootNotes; // Store root note for each drum pad

    // Mute state for each step of each drum
    private int[][] stepMuteValues; // Mute state for each step of each drum [drumIndex][barIndex]

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
        currentStep = new int[SequencerConstants.DRUM_PAD_COUNT];
        patternCompleted = new boolean[SequencerConstants.DRUM_PAD_COUNT];
        nextStepTick = new long[SequencerConstants.DRUM_PAD_COUNT];

        // Initialize per-drum parameter arrays
        patternLengths = new int[SequencerConstants.DRUM_PAD_COUNT];
        directions = new Direction[SequencerConstants.DRUM_PAD_COUNT];
        timingDivisions = new TimingDivision[SequencerConstants.DRUM_PAD_COUNT];
        loopingFlags = new boolean[SequencerConstants.DRUM_PAD_COUNT];
        bounceDirections = new int[SequencerConstants.DRUM_PAD_COUNT];
        velocities = new int[SequencerConstants.DRUM_PAD_COUNT];
        originalVelocities = new int[SequencerConstants.DRUM_PAD_COUNT];

        // Set default values
        java.util.Arrays.fill(velocities, SequencerConstants.DEFAULT_VELOCITY);
        java.util.Arrays.fill(originalVelocities, SequencerConstants.DEFAULT_VELOCITY);
        java.util.Arrays.fill(patternLengths, defaultPatternLength);
        java.util.Arrays.fill(directions, Direction.FORWARD);
        java.util.Arrays.fill(timingDivisions, TimingDivision.NORMAL);
        java.util.Arrays.fill(loopingFlags, true);
        java.util.Arrays.fill(bounceDirections, 1);

        // Set master tempo default
        masterTempo = SequencerConstants.DEFAULT_MASTER_TEMPO;

        // Initialize pattern and step parameter arrays
        patterns = new boolean[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepVelocities = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepDecays = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepProbabilities = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepNudges = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];

        // Initialize effect arrays
        stepPans = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepChorus = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        stepReverb = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];

        // Initialize effect tracking arrays
        lastSentPans = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastSentChorus = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastSentReverb = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastSentDecays = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];

        lastPanValues = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastReverbValues = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastChorusValues = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];
        lastDecayValues = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];

        // Initialize mute arrays
        stepMuteValues = new int[SequencerConstants.DRUM_PAD_COUNT][maxPatternLength];

        // Initialize arrays with default values
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            for (int j = 0; j < maxPatternLength; j++) {
                // Set initial values to -1 to ensure first message gets sent
                lastPanValues[i][j] = -1;
                lastReverbValues[i][j] = -1;
                lastChorusValues[i][j] = -1;
                lastDecayValues[i][j] = -1;

                // Set default values for step parameters
                stepVelocities[i][j] = SequencerConstants.DEFAULT_VELOCITY;
                stepDecays[i][j] = SequencerConstants.DEFAULT_DECAY;
                stepProbabilities[i][j] = SequencerConstants.DEFAULT_PROBABILITY;
                stepNudges[i][j] = 0;

                // Set defaults for effect parameters
                stepPans[i][j] = SequencerConstants.DEFAULT_PAN;
                stepChorus[i][j] = SequencerConstants.DEFAULT_CHORUS;
                stepReverb[i][j] = SequencerConstants.DEFAULT_REVERB;

                // Initialize last sent values to invalid defaults
                lastSentPans[i][j] = -1;
                lastSentChorus[i][j] = -1;
                lastSentReverb[i][j] = -1;
                lastSentDecays[i][j] = -1;

                // Initialize mute values to not muted
                stepMuteValues[i][j] = 0; // 0 = not muted, 1 = muted
            }
        }

        // Initialize instrument data arrays
        instrumentIds = new Long[SequencerConstants.DRUM_PAD_COUNT];
        soundbankNames = new String[SequencerConstants.DRUM_PAD_COUNT];
        presets = new Integer[SequencerConstants.DRUM_PAD_COUNT];
        bankIndices = new Integer[SequencerConstants.DRUM_PAD_COUNT];
        deviceNames = new String[SequencerConstants.DRUM_PAD_COUNT];
        instrumentNames = new String[SequencerConstants.DRUM_PAD_COUNT];

        // Initialize with defaults
        java.util.Arrays.fill(presets, 0); // Default drum kit preset
        java.util.Arrays.fill(bankIndices, 0); // Default bank

        // Initialize root notes
        rootNotes = new int[SequencerConstants.DRUM_PAD_COUNT];

        // Initialize with default values (standard GM drum kit starting at note 36)
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            rootNotes[i] = SequencerConstants.MIDI_DRUM_NOTE_OFFSET + i; // Default to standard GM drum mapping
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
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT &&
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
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT &&
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
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
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
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
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
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
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
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            return;
        }

        int length = patternLengths[drumIndex];

        // Clear existing pattern
        for (int step = 0; step < length; step++) {
            patterns[drumIndex][step] = false;
        }

        // Generate new pattern based on density (1-10)
        int hitsToAdd = Math.max(1, Math.min(SequencerConstants.MAX_DENSITY, density)) * length / SequencerConstants.MAX_DENSITY;

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
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
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
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
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

    /**
     * Get mute value for a drum at a specific bar
     *
     * @param drumIndex The drum index
     * @param barIndex  The bar index
     * @return 1 if muted, 0 if not muted
     */
    public int getMuteValue(int drumIndex, int barIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT &&
                barIndex >= 0 && barIndex < maxPatternLength) {
            return stepMuteValues[drumIndex][barIndex];
        }
        return 0;
    }

    /**
     * Set mute value for a drum at a specific bar
     *
     * @param drumIndex The drum index
     * @param barIndex  The bar index
     * @param value     1 to mute, 0 to unmute
     */
    public void setMuteValue(int drumIndex, int barIndex, int value) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT &&
                barIndex >= 0 && barIndex < maxPatternLength) {
            stepMuteValues[drumIndex][barIndex] = value;
        }
    }

    /**
     * Get all mute values for a drum
     *
     * @param drumIndex The drum index
     * @return List of mute values for the drum
     */
    public List<Integer> getMuteValues(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            return Arrays.stream(stepMuteValues[drumIndex])
                    .boxed()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Set all mute values for a drum
     *
     * @param drumIndex  The drum index
     * @param muteValues List of mute values (1=muted, 0=unmuted)
     */
    public void setMuteValues(int drumIndex, List<Integer> muteValues) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && muteValues != null) {
            for (int i = 0; i < Math.min(muteValues.size(), maxPatternLength); i++) {
                stepMuteValues[drumIndex][i] = muteValues.get(i);
            }
        }
    }

    /**
     * Get the default timing division (when no drum is specified)
     * @return The default timing division
     */
    public TimingDivision getTimingDivision() {
        // Return a default timing division when no specific drum is requested
        return TimingDivision.NORMAL;
    }

    /**
     * Get the timing division for a specific drum
     * @param drumIndex The index of the drum
     * @return The timing division for that drum
     */
    public TimingDivision getTimingDivision(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < timingDivisions.length) {
            return timingDivisions[drumIndex];
        }
        return TimingDivision.NORMAL; // Default fallback
    }
}
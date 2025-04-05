package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.sequencer.TimingUpdate;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
public class DrumEffectsSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(DrumSequencerPanel.class.getName());

    // UI Components
    private final List<DrumButton> drumButtons = new ArrayList<>();
    private final List<DrumSequencerGridButton> selectorButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> decayDials = new ArrayList<>();
    private final List<Dial> cutoffDials = new ArrayList<>();
    private final List<Dial> resonanceDials = new ArrayList<>();

    private int selectedPadIndex = -1; // Default to no selection

    // Sequence parameters
    private JSpinner lastStepSpinner;
    private JCheckBox loopCheckbox;
    private int patternLength = 16;
    private boolean isLooping = true;

    private Direction currentDirection = Direction.FORWARD;
    private boolean bounceForward = true; // Used for bounce direction to track current direction
    private JComboBox<String> directionCombo;

    private int octaveShift = 0; // Octave shift for note selection 
    private JLabel octaveLabel;

    private TimingDivision timingDivision = TimingDivision.NORMAL;
    private JComboBox<TimingDivision> timingCombo;

    private Consumer<NoteEvent> noteEventConsumer;

    // Callback support for timing changes
    private Consumer<TimingDivision> timingChangeListener;

    private JComboBox<String> rangeCombo;

    // Number of drum pads and pattern length
    private static final int DRUM_PAD_COUNT = 16; // 16 tracks
    private static final int PATTERN_LENGTH = 16; // Length of each pattern

    // Patterns array to hold on/off states for each drum pad
    private boolean[][] patterns = new boolean[DRUM_PAD_COUNT][PATTERN_LENGTH];

    // Array to hold the strikes (drum pads)
    private Strike[] strikes = new Strike[DRUM_PAD_COUNT];

    // Variable to track the active pattern
    private int activePatternIndex = 0; // Default to the first pattern

    int beat = 0;

    /**
     * Create a new SequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumEffectsSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        this.noteEventConsumer = noteEventConsumer;

        // Register with TimingBus
        TimingBus.getInstance().register(this);

        // Initialize strikes with root notes starting from 36
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            strikes[i] = new Strike(); // Initialize each strike
            strikes[i].setRootNote(36 + i); // Set root note for each
            strikes[i].setX0xPlayer(true);
        }

        // Initialize other parameters
        initialize();
    }

    /**
     * Initialize the panel
     */
    private void initialize() {

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add sequence parameters panel at the top
        JPanel sequenceParamsPanel = createSequenceParametersPanel();
        add(sequenceParamsPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Create panel for sequence parameters (last step, loop, etc.)
     */
    private JPanel createSequenceParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(50, 25));
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            System.out.println("Last step set to: " + lastStep);

            // Update pattern length
            patternLength = lastStep;
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Dir:"));

        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            switch (selectedIndex) {
                case 0:
                    currentDirection = Direction.FORWARD;
                    break;
                case 1:
                    currentDirection = Direction.BACKWARD;
                    break;
                case 2:
                    currentDirection = Direction.BOUNCE;
                    bounceForward = true; // Reset bounce direction when selected
                    break;
                case 3:
                    currentDirection = Direction.RANDOM;
                    break;
            }
            System.out.println("Direction set to: " + currentDirection);
        });
        directionPanel.add(directionCombo);

        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));

        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            TimingDivision selected = (TimingDivision) timingCombo.getSelectedItem();
            timingDivision = selected;
            System.out.println("Timing set to: " + selected + " (" + selected.getTicksPerBeat() + " steps per beat)");

            // Notify listeners that timing has changed
            if (timingChangeListener != null) {
                timingChangeListener.accept(selected);
            }
        });
        timingPanel.add(timingCombo);

        // Octave shift controls
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Oct:"));

        // Down button
        JButton octaveDownBtn = new JButton("-");
        octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveDownBtn.setFocusable(false);
        octaveDownBtn.addActionListener(e -> {
            if (octaveShift > -3) {  // Limit to -3 octaves
                octaveShift--;
                updateOctaveLabel();
            }
        });

        // Up button
        JButton octaveUpBtn = new JButton("+");
        octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveUpBtn.setFocusable(false);
        octaveUpBtn.addActionListener(e -> {
            if (octaveShift < 3) {  // Limit to +3 octaves
                octaveShift++;
                updateOctaveLabel();
            }
        });

        // Label showing current octave
        octaveLabel = new JLabel("0");
        octaveLabel.setPreferredSize(new Dimension(20, 20));
        octaveLabel.setHorizontalAlignment(JLabel.CENTER);

        octavePanel.add(octaveDownBtn);
        octavePanel.add(octaveLabel);
        octavePanel.add(octaveUpBtn);

        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            boolean looping = loopCheckbox.isSelected();
            System.out.println("Loop set to: " + looping);

            // Update looping state
            isLooping = looping;
        });

        JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rangeCombo = new JComboBox<>(new String[]{"1", "2", "3", "4"});
        generatePanel.add(rangeCombo);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        clearBtn.setFocusable(false);
        clearBtn.addActionListener(e -> {
            clearPattern();
        });

        JButton generateBtn = new JButton("Generate");
        generateBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        generateBtn.setFocusable(false);
        generateBtn.addActionListener(e -> {
            generatePattern();
        });

        generatePanel.add(clearBtn);
        generatePanel.add(generateBtn);

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);
        panel.add(octavePanel);     // Add the octave panel
        panel.add(loopCheckbox);
        panel.add(generatePanel);

        return panel;
    }

    private void clearPattern() {
        logger.info("Clearing pattern - resetting all controls");

        // Reset velocity and decay dials to moderate values
        for (int i = 0; i < velocityDials.size(); i++) {
            velocityDials.get(i).setValue(70); // 70% velocity is a good default
        }

        for (int i = 0; i < decayDials.size(); i++) {
            decayDials.get(i).setValue(50); // 50% decay time is a good default
        }

        // Unselect all buttons
        for (int i = 0; i < selectorButtons.size(); i++) {
            selectorButtons.get(i).setSelected(false);
            selectorButtons.get(i).setHighlighted(false);
        }

        // Force repaint to ensure UI updates
        validate();
        repaint();
    }

    private void generatePattern() {
        if (selectedPadIndex < 0 || selectedPadIndex >= DRUM_PAD_COUNT) {
            // No drum button is selected, show a message
            JOptionPane.showMessageDialog(this, "Please select a drum pad to generate a pattern.", "No Drum Pad Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Populate the pattern for the selected drum button
        for (int i = 0; i < PATTERN_LENGTH; i++) {
            // Example logic to randomly activate steps in the pattern
            patterns[selectedPadIndex][i] = Math.random() < 0.5; // 50% chance to activate the step
        }

        // Refresh the trigger buttons to reflect the new pattern
        refreshTriggerButtonsForPad(selectedPadIndex);
    }

    /**
     * Updates the octave label to show current octave shift
     */
    private void updateOctaveLabel() {
        String prefix = octaveShift > 0 ? "+" : "";
        octaveLabel.setText(prefix + octaveShift);
        System.out.println("Octave shift: " + octaveShift);
    }

    /**
     * Create a combo box with all avail Apply octave shift to a note after
     * quantization
     *
     * @param note The note to apply octave shift to
     * @return The shifted note
     */
    private int applyOctaveShift(int note) {
        // Add 12 semitones per octave
        int shiftedNote = note + (octaveShift * 12);

        // Ensure the note is within valid MIDI range (0-127)
        return Math.max(0, Math.min(127, shiftedNote));
    }

    /**
     * Create a column for the sequencer
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 3 knobs
        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (i > 0) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                // Add label to the column
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = new Dial();

            dial.setGradientStartColor(getKnobColor(i).brighter());
            dial.setGradientEndColor(getKnobColor(i).darker());
            dial.setKnobColor(getKnobColor(i));

            // Store the dial in the appropriate collection based on its type
            switch (i) {
                case 0:
                    velocityDials.add(dial); // Store the velocity dial
                    break;

                case 1:
                    decayDials.add(dial); // Store the decay dial
                    break;

                case 2:
                    cutoffDials.add(dial); // Store the decay dial
                    break;

                case 3:
                    resonanceDials.add(dial); // Store the decay dial
                    break;
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }
        // Add small spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add the trigger button - make it a toggle button
        DrumSequencerGridButton triggerButton = new DrumSequencerGridButton("");
        
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));

        // Make it toggleable
        // triggerButton.setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton.addActionListener(e -> {
            // Get the current toggle state
            boolean isOn = triggerButton.isSelected();

            // Update the pattern array for the selected drum pad
            if (selectedPadIndex >= 0 && selectedPadIndex < DRUM_PAD_COUNT) {
                // Update the pattern for this step and the selected drum
                patterns[selectedPadIndex][index] = isOn;
                // logger.info("Step {} for drum {} is now {}", index, selectedPadIndex, (isOn ? "ON" : "OFF"));
            } else {
                // No drum pad is selected
                logger.info("No drum pad selected - select a pad first to edit pattern");
                // We could show a message here if desired
            }
        });

        selectorButtons.add(triggerButton);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        DrumButton drumButton = new DrumButton();
        drumButtons.add(drumButton);
        drumButton.setName("DrumButton-" + index);
        drumButton.setToolTipText("Pad " + (index + 1));
        drumButton.setText(Integer.toString(index + 1));
        drumButton.setToggle(true);
        drumButton.setExclusive(true);
        // Add action to manually trigger the note when pad button is clicked
        drumButton.addActionListener(e -> selectDrumPad(index));

        // {
        // if (index < noteDials.size()) {
        //     // Get note from dial
        //     NoteSelectionDial noteDial = noteDials.get(index);
        //     int noteValue = noteDial.getValue();
        //     // Apply quantization if enabled
        //     int quantizedNote = quantizeNote(noteValue);
        //     // Apply octave shift
        //     int shiftedNote = applyOctaveShift(quantizedNote);
        //     // Get velocity
        //     int velocity = 127; // Full velocity for manual triggers
        //     if (index < velocityDials.size()) {
        //         velocity = (int) Math.round(velocityDials.get(index).getValue() * 1.27);
        //         velocity = Math.max(1, Math.min(127, velocity));
        //     }
        //     // Get decay time
        //     int decayTime = 250; // Longer decay time for manual triggers
        //     if (index < decayDials.size()) {
        //         decayTime = (int) Math.round(50 + decayDials.get(index).getValue() * 4.5);
        //     }
        //     // Trigger the note through the callback
        //     if (noteEventConsumer != null) {
        //         noteEventConsumer.accept(new NoteEvent(shiftedNote, velocity, decayTime));
        //     }
        // }
        // });
        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(drumButton);
        column.add(buttonPanel2);

        return column;
    }

    /**
     * Update the step highlighting in the sequencer
     */
    public void updateStep(int oldStep, int newStep) {
        // Validate indices to prevent out of bounds errors
        if (oldStep >= 0 && oldStep < selectorButtons.size()) {
            DrumSequencerGridButton oldButton = selectorButtons.get(oldStep);
            oldButton.setHighlighted(false);
            oldButton.repaint(); // Force immediate visual update
        }

        if (newStep >= 0 && newStep < selectorButtons.size()) {
            DrumSequencerGridButton newButton = selectorButtons.get(newStep);
            newButton.setHighlighted(true);
            newButton.repaint(); // Force immediate visual update
        }
        
        // Log the step change if needed
        // logger.format("Step highlight updated: {} -> {}", oldStep, newStep);
    }

    /**
     * Reset the sequencer state
     */
    public void reset() {
        beat = 0;
        
        // Clear all highlighting
        for (int i = 0; i < selectorButtons.size(); i++) {
            DrumSequencerGridButton button = selectorButtons.get(i);
            if (button != null) {
                button.setHighlighted(false);
                button.repaint();
            }
        }
    }

    /**
     * Get the maximum pattern length
     */
    public int getPatternLength() {
        return patternLength;
    }

    /**
     * Check if the sequencer is in loop mode
     */
    public boolean isLooping() {
        return isLooping;
    }

    /**
     * Get the current direction
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Check if bounce is forward
     */
    public boolean isBounceForward() {
        return bounceForward;
    }

    /**
     * Set bounce direction
     */
    public void setBounceForward(boolean forward) {
        this.bounceForward = forward;
    }

    /**
     * Get the knob label for a specific index
     */
    private String getKnobLabel(int i) {
        switch (i) {
            case 0:
                return "Velocity";
            case 1:
                return "Decay";
            case 2:
                return "Cutoff";
            case 3:
                return "Resonance";
            case 4:
                return "Drive";
            case 5:
                return "Tone";
            default:
                return "";
        }

    }

    /**
     * Get the knob label for a specific index
     */
    private Color getKnobColor(int i) {
        switch (i) {
            case 0:
                return ColorUtils.warmMustard;
            case 1:
                return ColorUtils.coolBlue;
            case 2:
                return ColorUtils.deepNavy;
            case 3:
                return ColorUtils.deepOrange;
            case 4:
                return ColorUtils.deepTeal;
            case 5:
                return ColorUtils.slateGray;
            default:
                return ColorUtils.deepNavy;
        }

    }

    public void setTimingChangeListener(Consumer<TimingDivision> listener) {
        this.timingChangeListener = listener;
    }

    public TimingDivision getTimingDivision() {
        return timingDivision;
    }

    // Method to select a drum pad and update the UI
    private void selectDrumPad(int padIndex) {
        selectedPadIndex = padIndex; // Update selected pad index
        activePatternIndex = padIndex; // Set active pattern index to the selected pad
        refreshTriggerButtonsForPad(padIndex); // Refresh the trigger buttons to show the current pattern
    }

    // Method to toggle the step for the active drum pad
    private void toggleStepForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0 && selectedPadIndex < DRUM_PAD_COUNT) {
            patterns[selectedPadIndex][stepIndex] = !patterns[selectedPadIndex][stepIndex]; // Toggle the state
            refreshTriggerButtonsForPad(selectedPadIndex); // Refresh the trigger buttons to reflect the change
        }
    }

    // Method to refresh the trigger buttons for the selected pad
    private void refreshTriggerButtonsForPad(int padIndex) {
        for (int i = 0; i < selectorButtons.size(); i++) {
            DrumSequencerGridButton button = selectorButtons.get(i);
            button.setSelected(patterns[padIndex][i]); // Set button state based on the stored pattern
            button.setHighlighted(false); // Reset highlight
        }
    }

    /**
     * Called on each beat to update the sequencer state
     */
    private void onBeat() {
        try {
            // Calculate the previous step to unhighlight
            int previousStep = (beat > 0) ? 
                    (int)((beat - 1) % patternLength) : 
                    (int)(patternLength - 1);
            
            // Calculate current step index (0-based, mod pattern length)
            int currentStep = getCurrentStep();
            
            // Update visual highlighting
            updateStep(previousStep, currentStep);
            
            // Process all drum pads to trigger notes
            for (int padIndex = 0; padIndex < DRUM_PAD_COUNT; padIndex++) {
                // Only trigger if this pad has this step activated
                if (currentStep < patternLength && patterns[padIndex][currentStep]) {
                    // Get the drum sound for this pad
                    Strike strike = strikes[padIndex];
                    if (strike != null) {
                        // Send note event to trigger the sound
                        noteEventConsumer.accept(new NoteEvent(
                            strike.getRootNote(),
                            127, // Full velocity
                            250  // Duration in ms
                        ));
                    }
                }
            }
            
            // Increment beat counter
            beat++;
            
            // Handle looping behavior
            if (isLooping && beat >= patternLength) {
                // logger.debug("Loop point reached at beat {}, resetting to 0", beat);
                beat = 0;
            }
        } catch (Exception e) {
            // logger.error("Error in onBeat(): {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate the current step safely accounting for looping
     */
    private int getCurrentStep() {
        // Make sure we wrap properly around the pattern length (e.g., 16 steps)
        return (int) (beat % patternLength);
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (action.getData() instanceof TimingUpdate) {
                    TimingUpdate update = (TimingUpdate) action.getData();
                    
                    // Only update on tick (or whatever timing resolution you need)
                    // This ensures the sequencer advances at the right time
                    if (update.tick() == 1) { // First tick of each beat
                        onBeat();
                    }
                }
            }
            
            case Commands.TRANSPORT_START -> {
                // Reset beat counter when transport starts
                beat = 0;
                logger.info("Transport started - resetting beat counter");
            }
            
            case Commands.TRANSPORT_STOP -> {
                // Reset state when stopped
                reset();
                logger.info("Transport stopped - resetting sequencer state");
            }
        }
    }

    // Method to find the index of a DrumButton in the list
    private int getDrumButtonIndex(DrumButton button) {
        for (int i = 0; i < drumButtons.size(); i++) {
            if (drumButtons.get(i) == button) {
                return i; // Return the index if found
            }
        }
        return -1; // Return -1 if not found
    }
}

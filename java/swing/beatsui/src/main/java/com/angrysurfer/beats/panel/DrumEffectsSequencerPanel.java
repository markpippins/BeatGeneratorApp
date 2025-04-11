package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.DrumStepUpdateEvent;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.DrumSequencerManager;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
public class DrumEffectsSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(DrumSequencerPanel.class.getName());

    // UI Components
    private final List<DrumButton> drumButtons = new ArrayList<>();
    private final List<TriggerButton> selectorButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> decayDials = new ArrayList<>();
    private final List<Dial> probabilityDials = new ArrayList<>();
    private final List<Dial> nudgeDials = new ArrayList<>();

    private int selectedPadIndex = -1; // Default to no selection

    // Sequence parameters
    private Consumer<NoteEvent> noteEventConsumer;
    private Consumer<TimingDivision> timingChangeListener;

    // Add reference to the shared sequencer
    private DrumSequencer sequencer;

    /**
     * Create a new SequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumEffectsSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        this.noteEventConsumer = noteEventConsumer;

        // Get the shared sequencer instance from DrumSequencerManager
        sequencer = DrumSequencerManager.getInstance().getSequencer(0);

        // If no sequencer exists yet, create one
        if (sequencer == null) {
            logger.info("Creating new drum sequencer through manager");
            sequencer = DrumSequencerManager.getInstance().newSequencer(noteEventConsumer);
        }

        // Register with CommandBus for updates
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        // Initialize other parameters
        initialize();

        // Select the first drum by default if available
        if (!drumButtons.isEmpty()) {
            selectDrumPad(0);
        }
    }

    /**
     * Initialize the panel
     */
    private void initialize() {

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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

        // Initialize drum pads with numbered labels
        initializeDrumPads();
    }

    /**
     * Create a column for the sequencer
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            // label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            // Add label to the column
            column.add(labelPanel);

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = new Dial();

            dial.setGradientStartColor(getKnobColor(i).brighter());
            dial.setGradientEndColor(getKnobColor(i).darker());
            dial.setKnobColor(getKnobColor(i));

            // Configure each dial based on its type
            switch (i) {
                case 0: // Velocity
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(100); // Default value

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (selectedPadIndex >= 0) {
                            sequencer.setStepVelocity(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    velocityDials.add(dial);
                    break;

                case 1: // Decay
                    dial.setMinimum(1);
                    dial.setMaximum(200);
                    dial.setValue(60); // Default value

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (selectedPadIndex >= 0) {
                            sequencer.setStepDecay(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    decayDials.add(dial);
                    break;

                case 2: // Probability
                    dial.setMinimum(0);
                    dial.setMaximum(100);
                    dial.setValue(100); // Default to 100% (always plays)
                    
                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (selectedPadIndex >= 0) {
                            // Update the sequencer
                            sequencer.setStepProbability(selectedPadIndex, index, dial.getValue());
                            
                            // Add debug output if needed (uncomment to enable)
                            // System.out.println("Set probability for drum " + selectedPadIndex + 
                            //                   ", step " + index + " to " + dial.getValue() + "%");
                        }
                    });
                    probabilityDials.add(dial);
                    break;

                case 3: // Nudge
                    dial.setMinimum(0);     // Changed from -50 to 0
                    dial.setMaximum(100);   // Changed from 50 to 100
                    dial.setValue(0);       // Default value (no delay)

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (selectedPadIndex >= 0) {
                            sequencer.setStepNudge(selectedPadIndex, index, dial.getValue());
                            
                            // Optional debug output
                            // System.out.println("Set nudge for drum " + selectedPadIndex + 
                            //                   ", step " + index + " to " + dial.getValue() + "ms");
                        }
                    });
                    nudgeDials.add(dial);
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
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));

        // Initially disabled until a drum is selected
        triggerButton.setEnabled(selectedPadIndex >= 0);

        // Add action listener that toggles the step in the pattern
        triggerButton.addActionListener(e -> {
            toggleStepForActivePad(index);
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

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(drumButton);
        column.add(buttonPanel2);

        return column;
    }

    /**
     * Update the step highlighting in the sequencer
     */
    public void updateStep(int oldStep, int newStep) {
        // First reset all buttons to ensure clean state
        for (TriggerButton button : selectorButtons) {
            button.setHighlighted(false);
        }

        // Then highlight only the current step
        if (newStep >= 0 && newStep < selectorButtons.size()) {
            TriggerButton newButton = selectorButtons.get(newStep);
            newButton.setHighlighted(true);
            newButton.repaint();

            // Debug output
            // System.out.println("Highlighting drum effect step: " + newStep);
        }
    }

    /**
     * Reset the sequencer state
     */
    public void reset() {
        // Clear all highlighting
        for (int i = 0; i < selectorButtons.size(); i++) {
            TriggerButton button = selectorButtons.get(i);
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
        return sequencer.getPatternLength(selectedPadIndex);
    }

    /**
     * Check if the sequencer is in loop mode
     */
    public boolean isLooping() {
        return sequencer.isLooping(selectedPadIndex);
    }

    /**
     * Get the current direction
     */
    public Direction getCurrentDirection() {
        return sequencer.getDirection(selectedPadIndex);
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
                return "Probability";
            case 3:
                return "Nudge";
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
        return sequencer.getTimingDivision(selectedPadIndex);
    }

    // Method to select a drum pad and update the UI
    private void selectDrumPad(int padIndex) {
        // Only process if actually changing selection
        if (padIndex != selectedPadIndex) {
            // Clear previous selection
            if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
                drumButtons.get(selectedPadIndex).setSelected(false);
                drumButtons.get(selectedPadIndex).repaint();
            }

            // Set new selection
            selectedPadIndex = padIndex;

            // Update drum button visual state
            if (padIndex >= 0 && padIndex < drumButtons.size()) {
                drumButtons.get(padIndex).setSelected(true);
                drumButtons.get(padIndex).repaint();

                // Enable trigger buttons
                setTriggerButtonsEnabled(true);

                // Notify other components of the selection change
                CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED,
                        this, new DrumPadSelectionEvent(-1, padIndex));
            } else {
                // No valid selection - disable trigger buttons
                setTriggerButtonsEnabled(false);
            }

            // Refresh trigger buttons to show the pattern for the selected pad
            refreshTriggerButtonsForPad(padIndex);

            // Update controls to match the selected pad's settings
            updateControlsFromSequencer();
        }
    }

    // Update refresh method to get data from sequencer
    private void refreshTriggerButtonsForPad(int padIndex) {
        // Handle no selection case
        if (padIndex < 0) {
            for (TriggerButton button : selectorButtons) {
                button.setSelected(false);
                button.setHighlighted(false);
                button.setEnabled(false);
                button.repaint();
            }
            return;
        }

        // A pad is selected, so enable all buttons
        setTriggerButtonsEnabled(true);

        // Update each button's state
        for (int i = 0; i < selectorButtons.size(); i++) {
            TriggerButton button = selectorButtons.get(i);

            // Set selected state based on pattern
            boolean isActive = sequencer.isStepActive(padIndex, i);
            button.setSelected(isActive);

            // Highlight current step if playing
            if (sequencer.isPlaying()) {
                int[] steps = sequencer.getCurrentStep();
                if (padIndex < steps.length) {
                    button.setHighlighted(i == steps[padIndex]);
                }
            } else {
                button.setHighlighted(false);
            }

            // Force repaint
            button.repaint();

            // Update dial values for this step
            if (i < velocityDials.size()) {
                velocityDials.get(i).setValue(sequencer.getStepVelocity(padIndex, i));
            }

            if (i < decayDials.size()) {
                decayDials.get(i).setValue(sequencer.getStepDecay(padIndex, i));
            }

            if (i < probabilityDials.size()) {
                probabilityDials.get(i).setValue(sequencer.getStepProbability(padIndex, i));
            }

            if (i < nudgeDials.size()) {
                nudgeDials.get(i).setValue(sequencer.getStepNudge(padIndex, i));
            }
        }
    }

    // Update control modification to use sequencer
    private void updateControlsFromSequencer() {
        if (selectedPadIndex < 0) {
            return;
        }

        // Update all dials to match the sequencer's current values for the selected drum
        for (int i = 0; i < selectorButtons.size(); i++) {
            // Update velocity dials
            if (i < velocityDials.size()) {
                Dial dial = velocityDials.get(i);
                dial.setValue(sequencer.getStepVelocity(selectedPadIndex, i));
            }

            // Update decay dials
            if (i < decayDials.size()) {
                Dial dial = decayDials.get(i);
                dial.setValue(sequencer.getStepDecay(selectedPadIndex, i));
            }

            // Update probability dials
            if (i < probabilityDials.size()) {
                Dial dial = probabilityDials.get(i);
                dial.setValue(sequencer.getStepProbability(selectedPadIndex, i));
            }

            // Update nudge dials
            if (i < nudgeDials.size()) {
                Dial dial = nudgeDials.get(i);
                dial.setValue(sequencer.getStepNudge(selectedPadIndex, i));
            }
        }
    }

    // Add helper method to set enabled state of all trigger buttons
    private void setTriggerButtonsEnabled(boolean enabled) {
        for (TriggerButton button : selectorButtons) {
            button.setEnabled(enabled);
            // When disabling, also clear toggle and highlight state
            if (!enabled) {
                button.setSelected(false);
                button.setHighlighted(false);
            }
            button.repaint();
        }
    }

    // Update onAction method to better handle step updates
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                // Only update if we have a drum selected and are playing
                if (selectedPadIndex >= 0 && sequencer.isPlaying() && action.getData() instanceof TimingUpdate) {
                    // Get the current sequencer state
                    int[] steps = sequencer.getCurrentStep();

                    // Safety check for array bounds
                    if (selectedPadIndex < steps.length) {
                        int currentStep = steps[selectedPadIndex];
                        int previousStep = calculatePreviousStep(currentStep);

                        // Log for debugging
                        // System.out.println("DrumEffectsSequencer updating step: old=" + previousStep + ", new=" + currentStep);
                        // Update visual highlighting for the step change
                        updateStep(previousStep, currentStep);
                    }
                }
            }

            case Commands.TRANSPORT_START -> {
                // When transport starts, refresh the grid to show current pattern
                if (selectedPadIndex >= 0) {
                    refreshTriggerButtonsForPad(selectedPadIndex);
                }
            }

            case Commands.TRANSPORT_STOP -> {
                // Clear all highlighting when stopped
                reset();

                // Refresh grid to show pattern without highlighting
                if (selectedPadIndex >= 0) {
                    refreshTriggerButtonsForPad(selectedPadIndex);
                }
            }

            case Commands.STEP_UPDATED -> {
                // Handle step updates coming from sequencer
                if (action.getData() instanceof DrumStepUpdateEvent event
                        && event.getDrumIndex() == selectedPadIndex) {
                    updateStep(event.getOldStep(), event.getNewStep());
                }
            }

            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_CREATED, Commands.DRUM_SEQUENCE_UPDATED -> {
                // When sequence changes, refresh the grid for the current selection
                if (selectedPadIndex >= 0) {
                    refreshTriggerButtonsForPad(selectedPadIndex);
                    updateControlsFromSequencer();
                }
            }
        }
    }

    // Helper to calculate previous step
    private int calculatePreviousStep(int currentStep) {
        if (currentStep <= 0) {
            return sequencer.getPatternLength(selectedPadIndex) - 1;
        }
        return currentStep - 1;
    }

    // Add the missing toggleStepForActivePad method
    private void toggleStepForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0) {
            // Toggle the step in the sequencer
            boolean wasActive = sequencer.isStepActive(selectedPadIndex, stepIndex);
            sequencer.toggleStep(selectedPadIndex, stepIndex);

            // Verify the toggle took effect
            boolean isNowActive = sequencer.isStepActive(selectedPadIndex, stepIndex);

            if (wasActive == isNowActive) {
                System.err.println("WARNING: Toggle step failed for pad " + selectedPadIndex + ", step " + stepIndex);
            }

            // Update the visual state of the button
            TriggerButton button = selectorButtons.get(stepIndex);
            button.setSelected(isNowActive);
            button.repaint();

            // If the step is now active, update the dials to reflect the step parameters
            if (isNowActive) {
                // Only update specific step's dials
                if (stepIndex < velocityDials.size()) {
                    velocityDials.get(stepIndex).setValue(sequencer.getStepVelocity(selectedPadIndex, stepIndex));
                }

                if (stepIndex < decayDials.size()) {
                    decayDials.get(stepIndex).setValue(sequencer.getStepDecay(selectedPadIndex, stepIndex));
                }

                if (stepIndex < probabilityDials.size()) {
                    probabilityDials.get(stepIndex).setValue(sequencer.getStepProbability(selectedPadIndex, stepIndex));
                }

                if (stepIndex < nudgeDials.size()) {
                    nudgeDials.get(stepIndex).setValue(sequencer.getStepNudge(selectedPadIndex, stepIndex));
                }
            }

            // Debug output
            // System.out.println("Toggled step " + stepIndex + " for drum " + selectedPadIndex + ": " + isNowActive);
            // Notify other components of the pattern change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, null, this);
        }
    }

    // Replace the initializeDrumPads method with this:
    private void initializeDrumPads() {
        // Apply numbered labels to each pad with beat indicators
        for (int i = 0; i < drumButtons.size(); i++) {
            DrumButton button = drumButtons.get(i);
            
            // Set the pad number (1-based)
            button.setPadNumber(i + 1);  // Change to i+1 for 1-based numbering
            
            // Set main beat flag for pads 1, 5, 9, 13 (zero-indexed as 0, 4, 8, 12)
            button.setIsMainBeat(i == 0 || i == 4 || i == 8 || i == 12);
        }
    }
}

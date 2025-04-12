package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
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

    private DrumSequenceNavigationPanel navigationPanel;

    private JSpinner lastStepSpinner;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;

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

        // Initialize UI components
        initialize();
        
        // NOTE: Don't select the first drum here, it will be done in initializeDrumPads()
        // which will execute after all buttons are created
    }

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Use a consistent BorderLayout
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create top panel to hold navigation, parameters and info
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Create sequence navigation panel (in center)
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);

        // Create sequence parameters panel (left side)
        JPanel sequenceParamsPanel = createSequenceParametersPanel();

        // SWAPPED: Navigation panel now goes on left
        topPanel.add(navigationPanel, BorderLayout.WEST);

        // SWAPPED: Parameters panel now goes in center
        topPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Drum info panel stays on right
        topPanel.add(createSwingControls(), BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

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
            // dial.setKnobColor(getKnobColor(i));
            
            // Configure each dial based on its type
            switch (i) {
                case 0: // Velocity
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(100); // Default value
                    dial.setKnobColor(ColorUtils.getDialColor("velocity"));

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
                    dial.setKnobColor(ColorUtils.getDialColor("decay"));

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
                    dial.setKnobColor(ColorUtils.getDialColor("probability"));

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
                    dial.setMinimum(0);     
                    dial.setMaximum(250);   
                    dial.setValue(0);     
                    dial.setKnobColor(ColorUtils.getDialColor("nudge"));

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (selectedPadIndex >= 0) {
                            sequencer.setStepNudge(selectedPadIndex, index, dial.getValue());
                            
                            // Optional debug output
                            // System.out.println("Set nudge for drum " + selectedPadIndex + 
                            //                   ", step " + index + " to " + dial.getValue() + "ms");
                            setToolTipText("" + dial.getValue() + "ms");
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
            
            // IMPORTANT: Update sequence parameter controls to match selected drum
            updateSequenceParameterControls();
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

            case Commands.DRUM_PAD_SELECTED -> {
                // Only respond to events from other panels to avoid feedback loops
                if (action.getData() instanceof DrumPadSelectionEvent event && action.getSender() != this) {
                    int newSelection = event.getNewSelection();
                    // Make sure the drum index is valid
                    if (newSelection >= 0 && newSelection < drumButtons.size()) {
                        // Update UI on EDT
                        SwingUtilities.invokeLater(() -> selectDrumPad(newSelection));
                    }
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
        // Names for each drum part - these will be used in tooltips
        String[] drumNames = {
            "Kick", "Snare", "Closed HH", "Open HH", 
            "Tom 1", "Tom 2", "Tom 3", "Crash",
            "Ride", "Rim", "Clap", "Cow", 
            "Clave", "Shaker", "Perc 1", "Perc 2"
        };
        
        // Apply numbered labels and tooltips to each pad with beat indicators
        for (int i = 0; i < drumButtons.size(); i++) {
            DrumButton button = drumButtons.get(i);
            
            // Set the pad number (1-based)
            button.setPadNumber(i + 1);
            
            // Set main beat flag for pads 1, 5, 9, 13 (zero-indexed as 0, 4, 8, 12)
            button.setIsMainBeat(i == 0 || i == 4 || i == 8 || i == 12);
            
            // Set detailed tooltip
            String drumName = (i < drumNames.length) ? drumNames[i] : "Drum " + (i + 1);
            button.setToolTipText(drumName + " - Click to edit effects for this drum");
        }
        
        // Ensure the first pad is automatically selected after initialization
        SwingUtilities.invokeLater(() -> {
            if (!drumButtons.isEmpty()) {
                selectDrumPad(0);
                // Log that first pad was selected
                logger.info("First drum pad automatically selected");
            }
        });
    }

    private JPanel createSequenceParametersPanel() {
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));

        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last Step:"));
        
        lastStepSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 16, 1));
        lastStepSpinner.setPreferredSize(new Dimension(50, 25));
        lastStepSpinner.addChangeListener(e -> {
            int steps = (Integer)lastStepSpinner.getValue();
            if (selectedPadIndex >= 0) {
                // logger.info("Setting last step to {} for drum {}", steps, selectedPadIndex);
                sequencer.setPatternLength(selectedPadIndex, steps);
                updateStepButtonsForDrum(selectedPadIndex);
            }
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo - Make label skinnier
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel directionLabel = new JLabel("↔️");
        directionLabel.setPreferredSize(new Dimension(20, 25)); // Make smaller
        directionPanel.add(directionLabel);

        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = Direction.FORWARD; // Default

            switch (selectedIndex) {
                case 0 -> direction = Direction.FORWARD;
                case 1 -> direction = Direction.BACKWARD;
                case 2 -> direction = Direction.BOUNCE;
                case 3 -> direction = Direction.RANDOM;
            }

            // logger.info("Setting direction to {} for drum {}", direction, selectedPadIndex);
            sequencer.setDirection(selectedPadIndex, direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo - Make label skinnier
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel timingLabel = new JLabel("🕒");
        timingLabel.setPreferredSize(new Dimension(20, 25)); // Make smaller
        timingPanel.add(timingLabel);

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                // logger.info("Setting timing to {} for drum {}", division, selectedPadIndex);
                sequencer.setTimingDivision(selectedPadIndex, division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox - Make skinnier
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setPreferredSize(new Dimension(40, 25));
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            boolean loop = loopToggleButton.isSelected();
            // logger.info("Setting loop to {} for drum {}", loop, selectedPadIndex);
            sequencer.setLooping(selectedPadIndex, loop);
        });

        // Pattern density control
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.add(new JLabel("Density:"));
        
        densitySpinner = new JSpinner(new SpinnerNumberModel(50, 25, 100, 25));
        densitySpinner.setPreferredSize(new Dimension(60, 25));
        rangePanel.add(densitySpinner);

        // Random pattern generator button
        generatePatternButton = new JButton("🎲");
        generatePatternButton.setPreferredSize(new Dimension(40, 25));
        generatePatternButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        generatePatternButton.addActionListener(e -> {
            if (selectedPadIndex >= 0) {
                int density = (Integer)densitySpinner.getValue();
                sequencer.generatePattern(density);
                refreshGridUI();
            }
        });

        // Clear pattern button
        clearPatternButton = new JButton("🗑️");
        clearPatternButton.setPreferredSize(new Dimension(40, 25));
        clearPatternButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        clearPatternButton.addActionListener(e -> {
            if (selectedPadIndex >= 0) {
                sequencer.clearPattern();
                refreshGridUI();
            }
        });

        // Create rotation panel
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rotationPanel.add(new JLabel("Rotate:"));

        // Push forward button 
        JButton pushForwardButton = new JButton("⟶");
        pushForwardButton.setToolTipText("Push pattern forward (right)");
        pushForwardButton.setPreferredSize(new Dimension(40, 25));
        pushForwardButton.setMargin(new Insets(2, 2, 2, 2));
        pushForwardButton.addActionListener(e -> {
            sequencer.pushForward();
            refreshGridUI();
        });

        // Pull backward button
        JButton pullBackwardButton = new JButton("⟵");
        pullBackwardButton.setToolTipText("Pull pattern backward (left)");
        pullBackwardButton.setPreferredSize(new Dimension(40, 25));
        pullBackwardButton.setMargin(new Insets(2, 2, 2, 2));
        pullBackwardButton.addActionListener(e -> {
            sequencer.pullBackward();
            refreshGridUI();
        });

        // Add buttons to rotation panel
        rotationPanel.add(pullBackwardButton);
        rotationPanel.add(pushForwardButton);

        // Final assembly in createSequenceParametersPanel
        controlsPanel.add(lastStepPanel);
        controlsPanel.add(directionPanel);
        controlsPanel.add(timingPanel);
        controlsPanel.add(loopToggleButton);
        controlsPanel.add(rangePanel);
        controlsPanel.add(rotationPanel);        // Add the rotation panel here
        controlsPanel.add(generatePatternButton);
        controlsPanel.add(clearPatternButton);

        return controlsPanel;
    }

    // Copy this for swing controls
    private JPanel createSwingControls() {
        JPanel swingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        swingPanel.setBorder(BorderFactory.createTitledBorder("Swing"));
        
        // Swing on/off toggle
        JToggleButton swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        swingToggle.setPreferredSize(new Dimension(50, 25)); // Slightly wider for text "On"
        swingToggle.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        swingToggle.addActionListener(e -> {
            sequencer.setSwingEnabled(swingToggle.isSelected());
        });
        swingPanel.add(swingToggle);

        // Swing amount slider
        JSlider swingSlider = new JSlider(JSlider.HORIZONTAL, 50, 75, sequencer.getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        swingSlider.setPreferredSize(new Dimension(150, 40));

        JLabel valueLabel = new JLabel(sequencer.getSwingPercentage() + "%");

        swingSlider.addChangeListener(e -> {
            int value = swingSlider.getValue();
            sequencer.setSwingPercentage(value);
            valueLabel.setText(value + "%");
        });

        swingPanel.add(swingSlider);
        swingPanel.add(valueLabel);

        return swingPanel;
    }

    /**
     * Update step buttons for the specified drum
     */
    private void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0) {
            // No drum selected, disable all buttons
            setTriggerButtonsEnabled(false);
            return;
        }
        
        // Enable all buttons
        setTriggerButtonsEnabled(true);
        
        // Update each button based on sequencer state
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        for (int i = 0; i < selectorButtons.size(); i++) {
            TriggerButton button = selectorButtons.get(i);
            boolean isInPattern = i < patternLength;
            boolean isActive = isInPattern && sequencer.isStepActive(drumIndex, i);
            
            // Update button state
            button.setEnabled(true);
            button.setSelected(isActive);
            
            // Visual update
            if (!isInPattern) {
                button.setBackground(Color.DARK_GRAY);
            } else {
                button.setBackground(isActive ? ColorUtils.deepOrange : Color.GRAY);
            }
            
            // Force repaint
            button.repaint();
        }
        
        // Update dials to match the current drum
        updateControlsFromSequencer();
    }

    /**
     * Refresh the entire UI to match the sequencer state
     */
    private void refreshGridUI() {
        // Refresh the appropriate UI elements for the selected drum
        if (selectedPadIndex >= 0) {
            // Update trigger buttons
            refreshTriggerButtonsForPad(selectedPadIndex);
            
            // Update parameter controls
            updateControlsFromSequencer();
            
            // Update sequence parameters in UI
            updateSequenceParameterControls();
        } else {
            // No drum selected, disable all controls
            setTriggerButtonsEnabled(false);
        }
    }

    /**
     * Update sequence parameter controls based on the selected drum
     */
    private void updateSequenceParameterControls() {
        if (selectedPadIndex < 0) {
            return;
        }
        
        // Update last step spinner
        lastStepSpinner.setValue(sequencer.getPatternLength(selectedPadIndex));
        
        // Update direction combo
        Direction dir = sequencer.getDirection(selectedPadIndex);
        switch (dir) {
            case FORWARD -> directionCombo.setSelectedIndex(0);
            case BACKWARD -> directionCombo.setSelectedIndex(1);
            case BOUNCE -> directionCombo.setSelectedIndex(2);
            case RANDOM -> directionCombo.setSelectedIndex(3);
        }
        
        // Update timing combo
        timingCombo.setSelectedItem(sequencer.getTimingDivision(selectedPadIndex));
        
        // Update loop toggle
        loopToggleButton.setSelected(sequencer.isLooping(selectedPadIndex));
    }
}

package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.panel.MainPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.DrumSequencerManager;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
@Getter
@Setter
public class DrumParamsSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(DrumParamsSequencerPanel.class.getName());

    // UI Components
    private final List<TriggerButton> selectorButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> decayDials = new ArrayList<>();
    private final List<Dial> probabilityDials = new ArrayList<>();
    private final List<Dial> nudgeDials = new ArrayList<>();

    private int selectedPadIndex = -1; // Default to no selection

    // Sequence parameters
    private Consumer<NoteEvent> noteEventConsumer;

    // Add reference to the shared sequencer
    private DrumSequencer sequencer;

    // Replace DrumParamsSequencerParametersPanel with SequencerParametersPanel
    private DrumSequenceNavigationPanel navigationPanel;
    private DrumSequencerParametersPanel sequenceParamsPanel; // Changed from DrumParamsSequencerParametersPanel
    private DrumSequencerMaxLengthPanel maxLengthPanel; // New field
    private DrumSequenceGeneratorPanel generatorPanel; // New field
    private DrumSequencerSwingPanel swingPanel;

    // Add as a class field
    private boolean updatingControls = false;

    // Add this field at the class level
    private JPanel drumButtonsPanel;

    // Add this field:
    private DrumButtonsPanel drumPadPanel;

    // Add this field to the class
    private boolean isHandlingSelection = false;

    /**
     * Create a new SequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumParamsSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        this.noteEventConsumer = noteEventConsumer;

        // Get the shared sequencer instance from DrumSequencerManager
        sequencer = DrumSequencerManager.getInstance().getSequencer(0);

        // If no sequencer exists yet, create one
        if (sequencer == null) {
            logger.info("Creating new drum sequencer through manager");
            sequencer = DrumSequencerManager.getInstance().newSequencer(noteEventConsumer);

            // Double check we got a sequencer
            if (sequencer == null) {
                throw new IllegalStateException("Failed to create sequencer");
            }
        }

        // Create required panels BEFORE initialize() is called
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_SELECTED,
                Commands.DRUM_INSTRUMENTS_UPDATED,
                Commands.HIGHLIGHT_STEP
        });

        logger.info("DrumParamsSequencerPanel registered for specific events");

        TimingBus.getInstance().register(this);

        // Initialize UI components
        initialize();

        // Add key listener for Escape key to return to DrumSequencer panel
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // Navigate to DrumSequencer tab
                    MainPanel mainPanel = findMainPanel();
                    if (mainPanel != null) {
                        // The index 0 is for "Drum" tab (DrumSequencerPanel)
                        mainPanel.setSelectedTab(0);
                    }
                }
            }
        });

        // Make the panel focusable to receive key events
        setFocusable(true);

        // When the panel gains focus or becomes visible, request focus
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
            }
        });

        // Request focus when the panel becomes visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });

        // NOTE: Don't select the first drum here, it will be done in
        initializeDrumPads();
        // which will execute after all buttons are created
    }

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Clear existing collections to avoid duplicates
        selectorButtons.clear();
        velocityDials.clear();
        decayDials.clear();
        probabilityDials.clear();
        nudgeDials.clear();

        // REDUCED: from 5,5 to 2,2
        setLayout(new BorderLayout(2, 2));
        UIHelper.setPanelBorder(this);

        // Create west panel to hold navigation
        JPanel westPanel = new JPanel(new BorderLayout(2, 2));

        // Create east panel for sound parameters
        JPanel eastPanel = new JPanel(new BorderLayout(2, 2));
        eastPanel.add(new SoundParametersPanel(), BorderLayout.NORTH);

        // Create top panel to hold west, center and east panels
        JPanel topPanel = new JPanel(new BorderLayout(2, 2));

        // Navigation panel goes NORTH-WEST
        UIHelper.addSafely(westPanel, navigationPanel, BorderLayout.NORTH);

        // Add panels to the top panel
        UIHelper.addSafely(topPanel, westPanel, BorderLayout.EAST);
        UIHelper.addSafely(topPanel, eastPanel, BorderLayout.WEST);

        // Add top panel to main layout
        UIHelper.addSafely(this, topPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 2, 0));
        // REDUCED: from 10,10,10,10 to 5,5,5,5
        sequencePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            UIHelper.addSafely(sequencePanel, columnPanel);
        }

        // Create a panel to hold both the sequence panel and drum buttons
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Add sequence panel directly to CENTER
        UIHelper.addSafely(centerPanel, sequencePanel, BorderLayout.CENTER);

        // Create drum pad panel with callback
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

        // Create a panel for the drum section (drum buttons only)
        JPanel drumSection = new JPanel(new BorderLayout(2, 2));
        drumSection.add(new MuteSequencerPanel(sequencer), BorderLayout.NORTH);
        drumSection.add(drumPadPanel, BorderLayout.CENTER);

        // Add the drum section to the SOUTH of the centerPanel
        UIHelper.addSafely(centerPanel, drumSection, BorderLayout.SOUTH);

        // Add the center panel to the main layout
        UIHelper.addSafely(this, centerPanel, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        // REDUCED: from 5,5 to 2,2
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 2));

        // Add sequence parameters panel
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Add MaxLengthPanel to the WEST position
        maxLengthPanel = new DrumSequencerMaxLengthPanel(sequencer);
        bottomPanel.add(maxLengthPanel, BorderLayout.WEST);

        // Create a container for the right-side panels
        // REDUCED: from 5,0 to 2,0
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        swingPanel = new DrumSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Create a column for the sequencer (modified to remove drum buttons)
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        // REDUCED: from 5,2,5,2 to 2,1,2,1
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            column.add(labelPanel);

            // Create dial with appropriate settings based on type
            Dial dial = new Dial();
            dial.setName(getKnobLabel(i) + "-" + index);

            // Configure dial based on its type
            switch (i) {
                case 0: // Velocity
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(100); // Default value
                    dial.setKnobColor(UIHelper.getDialColor("velocity")); // Set knob color
                    // dial.setColors(new Color(0, 180, 255), new Color(0, 100, 200), Color.WHITE);
                    // Add to collection and event listener
                    velocityDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            sequencer.setStepVelocity(selectedPadIndex, index, value);

                            // Publish an event for parameter change
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new Object[]{
                                            selectedPadIndex,
                                            index,
                                            value,
                                            sequencer.getStepDecay(selectedPadIndex, index),
                                            sequencer.getStepProbability(selectedPadIndex, index),
                                            sequencer.getStepNudge(selectedPadIndex, index)
                                    }
                            );
                        }
                    });
                    break;

                case 1: // Decay
                    dial.setMinimum(0);
                    dial.setMaximum(1000);
                    dial.setValue(250); // Default value
                    dial.setKnobColor(UIHelper.getDialColor("decay")); // Set knob color
                    // Add to collection and event listener
                    decayDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            sequencer.setStepDecay(selectedPadIndex, index, value);

                            // Publish an event for parameter change
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new Object[]{
                                            selectedPadIndex,
                                            index,
                                            sequencer.getStepVelocity(selectedPadIndex, index),
                                            value,
                                            sequencer.getStepProbability(selectedPadIndex, index),
                                            sequencer.getStepNudge(selectedPadIndex, index)
                                    }
                            );
                        }
                    });
                    break;

                case 2: // Probability
                    dial.setMinimum(0);
                    dial.setMaximum(100);
                    dial.setValue(100); // Default value
                    dial.setKnobColor(UIHelper.getDialColor("probability")); // Set knob color
                    // Add to collection and event listener
                    probabilityDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            sequencer.setStepProbability(selectedPadIndex, index, value);

                            // Publish an event for parameter change
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new Object[]{
                                            selectedPadIndex,
                                            index,
                                            sequencer.getStepVelocity(selectedPadIndex, index),
                                            sequencer.getStepDecay(selectedPadIndex, index),
                                            value,
                                            sequencer.getStepNudge(selectedPadIndex, index)
                                    }
                            );
                        }
                    });
                    break;

                case 3: // Nudge
                    dial.setMinimum(-50);
                    dial.setMaximum(50);
                    dial.setValue(0); // Default value
                    dial.setKnobColor(UIHelper.getDialColor("nudge")); // Set knob color

                    // Add to collection and event listener
                    nudgeDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            sequencer.setStepNudge(selectedPadIndex, index, value);

                            // Publish an event for parameter change
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new Object[]{
                                            selectedPadIndex,
                                            index,
                                            sequencer.getStepVelocity(selectedPadIndex, index),
                                            sequencer.getStepDecay(selectedPadIndex, index),
                                            sequencer.getStepProbability(selectedPadIndex, index),
                                            value
                                    }
                            );
                        }
                    });
                    break;
            }

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        // REDUCED: from 0,5 to 0,2
        column.add(Box.createRigidArea(new Dimension(0, 2)));

        // Add only the trigger button - not the drum button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setEnabled(selectedPadIndex >= 0);
        triggerButton.addActionListener(e -> toggleStepForActivePad(index));

        selectorButtons.add(triggerButton);

        // Center the button horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(triggerButton);
        column.add(buttonPanel);

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
        for (TriggerButton button : selectorButtons) {
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
     * Get the knob label for a specific index
     */
    private String getKnobLabel(int i) {
        return switch (i) {
            case 0 -> "Velocity";
            case 1 -> "Decay";
            case 2 -> "Probability";
            case 3 -> "Nudge";
            case 4 -> "Drive";
            case 5 -> "Tone";
            default -> "";
        };

    }

    // Update refresh method to get data from sequencer
    public void refreshTriggerButtonsForPad(int padIndex) {
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
                int[] steps = sequencer.getSequenceData().getCurrentStep();
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

        // Update all dials to match the sequencer's current values for the selected
        // drum
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

    /**
     * Update all dials to reflect the values for the currently selected drum
     */
    private void updateDialsForSelectedPad() {
        if (selectedPadIndex < 0) {
            return;
        }

        updatingControls = true;
        try {
            // Update all dials for each step
            for (int step = 0; step < Math.min(16, selectorButtons.size()); step++) {
                // Update velocity dials
                if (step < velocityDials.size()) {
                    Dial dial = velocityDials.get(step);
                    dial.setValue(sequencer.getStepVelocity(selectedPadIndex, step));
                }

                // Update decay dials
                if (step < decayDials.size()) {
                    Dial dial = decayDials.get(step);
                    dial.setValue(sequencer.getStepDecay(selectedPadIndex, step));
                }

                // Update probability dials
                if (step < probabilityDials.size()) {
                    Dial dial = probabilityDials.get(step);
                    dial.setValue(sequencer.getStepProbability(selectedPadIndex, step));
                }

                // Update nudge dials
                if (step < nudgeDials.size()) {
                    Dial dial = nudgeDials.get(step);
                    dial.setValue(sequencer.getStepNudge(selectedPadIndex, step));
                }
            }

            // Also update trigger buttons to reflect active steps
            refreshTriggerButtonsForPad(selectedPadIndex);

        } finally {
            updatingControls = false;
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
            case Commands.CHANGE_THEME -> {
                // Handle theme change by recreating drum pad panel
                SwingUtilities.invokeLater(() -> {
                    // Remember which pad was selected
                    int currentSelection = selectedPadIndex;

                    // Find the centering panel that contains our drumPadPanel
                    Container parent = drumPadPanel.getParent();

                    if (parent != null) {
                        // Remove the old panel
                        parent.remove(drumPadPanel);

                        // Create a new drum pad panel with updated theme colors
                        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

                        // Add it back to the layout
                        parent.add(drumPadPanel);

                        // Update UI
                        parent.revalidate();
                        parent.repaint();

                        // Restore selection state
                        if (currentSelection >= 0) {
                            drumPadPanel.selectDrumPad(currentSelection);
                        }

                        System.out.println("DrumParamsSequencerPanel: Recreated drum pad panel after theme change");
                    }
                });
            }

            case Commands.TIMING_UPDATE -> {
                // Only update if we have a drum selected and are playing
                if (selectedPadIndex >= 0 && sequencer.isPlaying() && action.getData() instanceof TimingUpdate) {
                    // Get the current sequencer state
                    int[] steps = sequencer.getSequenceData().getCurrentStep();

                    // Safety check for array bounds
                    if (selectedPadIndex < steps.length) {
                        int currentStep = steps[selectedPadIndex];
                        int previousStep = calculatePreviousStep(currentStep);

                        // Log for debugging
                        // System.out.println("DrumEffectsSequencer updating step: old=" + previousStep
                        // + ", new=" + currentStep);
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

                    // Update dial positions for the loaded drum sequence
                    updateDialsForSelectedPad();
                }
            }

            case Commands.DRUM_PAD_SELECTED -> {
                // Only respond to events from other panels to avoid loops
                if (action.getData() instanceof DrumPadSelectionEvent event && action.getSender() != this) {
                    int newSelection = event.getNewSelection();

                    // Check if index is valid and different
                    if (newSelection != selectedPadIndex &&
                            newSelection >= 0 &&
                            newSelection < drumPadPanel.getButtonCount()) {

                        // Skip heavy operations - just update necessary state
                        selectedPadIndex = newSelection;

                        // Update UI without triggering further events
                        SwingUtilities.invokeLater(() -> {
                            drumPadPanel.selectDrumPadNoCallback(newSelection);
                            refreshTriggerButtonsForPad(newSelection);
                        });
                    }
                }
            }

//            case Commands.PLAYER_UPDATED, Commands.INSTRUMENT_CHANGED -> {
//                // Update the info label if this affects our selected pad
//                if (selectedPadIndex >= 0) {
//                    SwingUtilities.invokeLater(this::updateInstrumentInfoLabel);
//                }
//            }
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
            // System.out.println("Toggled step " + stepIndex + " for drum " +
            // selectedPadIndex + ": " + isNowActive);
            // Notify other components of the pattern change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, null, this);
        }
    }

    // Replace the handleDrumPadSelected method with this version
    private void handleDrumPadSelected(int padIndex) {
        // Don't process if already selected or we're in the middle of handling a
        // selection
        if (padIndex == selectedPadIndex || isHandlingSelection)
            return;

        try {
            // Set flag to prevent recursive calls
            isHandlingSelection = true;

            selectedPadIndex = padIndex;
            sequencer.setSelectedPadIndex(padIndex);

            // Get the player for this pad index
            if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[padIndex];

                if (player != null && player.getInstrument() != null) {
                    // player.getInstrument().setDevice(DeviceManager.getMidiDevice(player.getInstrument().getDeviceName()));
                    // PlayerManager.getInstance().ensureChannelConsistency();
                    // PlayerManager.getInstance().applyPlayerPreset(player);

                    // player.drumNoteOn(player.getRootNote());

                    CommandBus.getInstance().publish(
                            Commands.STATUS_UPDATE,
                            this,
                            new StatusUpdate(
                                    "Selected pad: " + player.getName()));
                    CommandBus.getInstance().publish(
                            Commands.PLAYER_ACTIVATION_REQUEST,
                            this,
                            player);
                }
            }

            // Update UI in a specific order
            setTriggerButtonsEnabled(true);
            refreshTriggerButtonsForPad(selectedPadIndex);
            updateDialsForSelectedPad();

            // Then do other updates
            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateControls(padIndex);
            }

            // Publish drum pad event LAST and only if we're handling a direct user
            // selection
            CommandBus.getInstance().publish(
                    Commands.DRUM_PAD_SELECTED,
                    this,
                    new DrumPadSelectionEvent(-1, padIndex));
        } finally {
            // Always clear the flag when done
            isHandlingSelection = false;
        }
    }

    // Helper method to find the MainPanel ancestor
    private MainPanel findMainPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof MainPanel) {
                return (MainPanel) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    // Update the initializeDrumPads method to delegate to the panel
    private void initializeDrumPads() {
        // Select the first pad after initialization
        SwingUtilities.invokeLater(() -> drumPadPanel.selectDrumPad(0));
    }

}

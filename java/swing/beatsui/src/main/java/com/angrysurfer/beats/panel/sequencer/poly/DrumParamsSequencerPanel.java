package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.panel.MainPanel;
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
public class DrumParamsSequencerPanel extends JPanel implements IBusListener {

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
        }

        // Register with CommandBus for updates
        CommandBus.getInstance().register(this);
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
        // initializeDrumPads()
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

        // Create west panel to hold navigation
        JPanel westPanel = new JPanel(new BorderLayout(5, 5));

        // Create east panel for sound parameters
        JPanel eastPanel = new JPanel(new BorderLayout(5, 5));

        // Create top panel to hold west and east panels
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Create sequence navigation panel using the custom component
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.NORTH);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(eastPanel, BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns (now without drum buttons)
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Create a panel to hold both the sequence panel and drum buttons
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create drum buttons panel with matching column spacing
        drumButtonsPanel = createDrumButtonsPanel();
        centerPanel.add(drumButtonsPanel, BorderLayout.SOUTH);
        
        // Add the center panel instead of just the scroll pane
        add(centerPanel, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        // Add MaxLengthPanel to the WEST position
        maxLengthPanel = new DrumSequencerMaxLengthPanel(sequencer);
        bottomPanel.add(maxLengthPanel, BorderLayout.WEST);

        // Add sequence parameters panel
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        swingPanel = new DrumSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Initialize drum pads with numbered labels
        initializeDrumPads();
    }

    /**
     * Create a column for the sequencer (modified to remove drum buttons)
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            column.add(labelPanel);

            // Create dial with appropriate settings
            Dial dial = new Dial();
            // Configure dial based on its type...
            // (existing code for dial configuration)

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }
        
        // Add spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add only the trigger button - not the drum button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setEnabled(selectedPadIndex >= 0);
        triggerButton.addActionListener(e -> {
            toggleStepForActivePad(index);
        });

        selectorButtons.add(triggerButton);
        
        // Center the button horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(triggerButton);
        column.add(buttonPanel);

        return column;
    }

    /**
     * Create a panel with drum buttons that align with columns above
     */
    private JPanel createDrumButtonsPanel() {
        // Clear existing buttons before recreating
        drumButtons.clear();
        
        // Use GridLayout to match column spacing exactly
        JPanel drumButtonsPanel = new JPanel(new GridLayout(1, 16, 5, 0));
        drumButtonsPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        
        // Create 16 drum buttons
        for (int i = 0; i < 16; i++) {
            JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            
            DrumButton drumButton = new DrumButton();
            drumButton.setName("DrumButton-" + i);
            drumButton.setToolTipText("Pad " + (i + 1));
            drumButton.setText(Integer.toString(i + 1));
            drumButton.setExclusive(true);
            
            final int index = i;
            drumButton.addActionListener(e -> selectDrumPad(index));
            
            // Add to container for alignment
            buttonContainer.add(drumButton);
            drumButtons.add(drumButton);
            
            // Add the container to the panel
            drumButtonsPanel.add(buttonContainer);
        }
        
        // Initialize drum pads with named labels after creation
        initializeDrumPads();
        
        return drumButtonsPanel;
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
                drumButtons.get(selectedPadIndex).setText("");
                drumButtons.get(selectedPadIndex).repaint();
            }

            // Set new selection
            selectedPadIndex = padIndex;
            
            // Update sequencer's selected pad index
            sequencer.setSelectedPadIndex(padIndex);

            // Update drum button visual state using clearer approach
            if (padIndex >= 0 && padIndex < drumButtons.size()) {
                DrumButton button = drumButtons.get(padIndex);
                button.setSelected(true);
                button.repaint();

                // Enable trigger buttons
                setTriggerButtonsEnabled(true);

                // Notify other components of the selection change
                CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED,
                        this, new DrumPadSelectionEvent(-1, padIndex));
                        
                // Refresh trigger buttons to show the pattern for the selected pad
                refreshTriggerButtonsForPad(padIndex);
        
                // Update controls to match the selected pad's settings
                updateControlsFromSequencer();
        
                // Update dial positions for the selected drum
                updateDialsForSelectedPad();
        
                // Update sequence parameter controls to match selected drum
                if (sequenceParamsPanel != null) {
                    sequenceParamsPanel.updateControls(padIndex);
                }
            } else {
                // No valid selection - disable trigger buttons
                setTriggerButtonsEnabled(false);
            }
        }
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
        
        // Set the class-level flag
        updatingControls = true;
        
        try {
            // Ensure we're on the EDT
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> updateDialsForSelectedPad());
                return;
            }
            
            // Update all dials for each step
            for (int step = 0; step < 16; step++) {
                // Get velocity dial and update its value
                if (step < velocityDials.size()) {
                    Dial velocityDial = velocityDials.get(step);
                    int velocity = sequencer.getStepVelocity(selectedPadIndex, step);
                    velocityDial.setValue(velocity);
                    velocityDial.repaint(); // Add explicit repaint
                }
                
                // Get decay dial and update its value
                if (step < decayDials.size()) {
                    Dial decayDial = decayDials.get(step);
                    int decay = sequencer.getStepDecay(selectedPadIndex, step);
                    decayDial.setValue(decay);
                    decayDial.repaint(); // Add explicit repaint
                }
                
                // Get probability dial and update its value
                if (step < probabilityDials.size()) {
                    Dial probDial = probabilityDials.get(step);
                    int probability = sequencer.getStepProbability(selectedPadIndex, step);
                    probDial.setValue(probability);
                    probDial.repaint(); // Add explicit repaint
                }
                
                // Get nudge dial and update its value
                if (step < nudgeDials.size()) {
                    Dial nudgeDial = nudgeDials.get(step);
                    int nudge = sequencer.getStepNudge(selectedPadIndex, step);
                    nudgeDial.setValue(nudge);
                    nudgeDial.repaint(); // Add explicit repaint
                }
            }
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
                // Handle theme change by recreating drum buttons
                SwingUtilities.invokeLater(() -> {
                    // Remember which pad was selected
                    int currentSelection = selectedPadIndex;
                    
                    // Get the parent container (centerPanel)
                    Container parent = drumButtonsPanel.getParent();
                    
                    // Remove the old panel
                    if (parent != null) {
                        parent.remove(drumButtonsPanel);
                        
                        // Create a new drum buttons panel with updated theme colors
                        drumButtonsPanel = createDrumButtonsPanel();
                        
                        // Add it back to the layout
                        parent.add(drumButtonsPanel, BorderLayout.SOUTH);
                        
                        // Update UI
                        parent.revalidate();
                        parent.repaint();
                        
                        // Restore selection state
                        if (currentSelection >= 0 && currentSelection < drumButtons.size()) {
                            selectDrumPad(currentSelection);
                        }
                        
                        System.out.println("DrumParamsSequencerPanel: Recreated drum buttons after theme change");
                    }
                });
            }
            
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
                // Only respond to events from other panels to avoid feedback loops
                if (action.getData() instanceof DrumPadSelectionEvent event && action.getSender() != this) {
                    int newSelection = event.getNewSelection();
                    // logger.info("Received drum selection event: {}", newSelection);

                    // Check if index is valid
                    if (newSelection >= 0 && newSelection < drumButtons.size()) {
                        // Update the selection on the EDT to avoid UI threading issues
                        SwingUtilities.invokeLater(() -> {
                            selectDrumPad(newSelection);
                        });
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
            // System.out.println("Toggled step " + stepIndex + " for drum " +
            // selectedPadIndex + ": " + isNowActive);
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
            button.setMainBeat(i == 0 || i == 4 || i == 8 || i == 12);

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

    /**
     * Refresh the entire UI to match the sequencer state
     */
    public void refreshGridUI() {
        // Refresh the appropriate UI elements for the selected drum
        if (selectedPadIndex >= 0) {
            // Update trigger buttons
            refreshTriggerButtonsForPad(selectedPadIndex);

            // Update parameter controls
            updateControlsFromSequencer();

            // Update sequence parameters in UI
            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateControls(selectedPadIndex);
            }
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
        
        // Use the custom panel's method to update controls
        sequenceParamsPanel.updateControls(selectedPadIndex);
    }

    private JPanel createTriggerButtonsPanel() {
        // Create a panel with GridLayout to match the columns above
        JPanel triggerButtonsPanel = new JPanel(new GridLayout(1, 16, 5, 0));
        triggerButtonsPanel.setBorder(new EmptyBorder(10, 10, 2, 10));
        
        // Create 16 trigger buttons, one for each column
        for (int i = 0; i < 16; i++) {
            JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            
            // Create the trigger button
            TriggerButton triggerButton = new TriggerButton("");
            triggerButton.setName("TriggerButton-" + i);
            triggerButton.setToolTipText("Step " + (i + 1));

            // Initially disabled until a drum is selected
            triggerButton.setEnabled(selectedPadIndex >= 0);

            // Add action listener that toggles the step in the pattern
            final int index = i;
            triggerButton.addActionListener(e -> {
                toggleStepForActivePad(index);
            });
            
            // Add to the container panel for proper centering
            buttonContainer.add(triggerButton);
            
            // Store in the list for later use
            selectorButtons.add(triggerButton);
            
            // Add the container to the trigger buttons panel
            triggerButtonsPanel.add(buttonContainer);
        }
        
        return triggerButtonsPanel;
    }

    /**
     * Get the currently selected drum pad index
     */
    public int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    /**
     * Get the sequencer instance
     */
    public DrumSequencer getSequencer() {
        return sequencer;
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
}

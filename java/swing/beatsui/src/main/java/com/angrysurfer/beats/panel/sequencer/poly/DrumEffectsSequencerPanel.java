package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.panel.MainPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.DrumStepUpdateEvent;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.angrysurfer.core.service.PlayerManager;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
public class DrumEffectsSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(DrumSequencerPanel.class.getName());

    // UI Components
    private final List<DrumButton> drumButtons = new ArrayList<>();
    private final List<TriggerButton> selectorButtons = new ArrayList<>();
    private final List<Dial> panDials = new ArrayList<>();
    private final List<Dial> delayDials = new ArrayList<>();
    private final List<Dial> chorusDials = new ArrayList<>();
    private final List<Dial> reverbDials = new ArrayList<>();

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

    // Add this field to DrumEffectsSequencerPanel
    private DrumButtonsPanel drumPadPanel;

    private JLabel instrumentInfoLabel;

    // Add this field to the class
    private boolean isHandlingSelection = false;

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
        SwingUtilities.invokeLater(() -> {
            drumPadPanel.selectDrumPad(0);
            handleDrumPadSelected(0);
        });

    }

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Clear collections before rebuilding
        selectorButtons.clear();
        panDials.clear();
        delayDials.clear();
        chorusDials.clear();
        reverbDials.clear();

        // REDUCED: from 5,5 to 2,2
        setLayout(new BorderLayout(2, 2));
        // REDUCED: from 5,5,5,5 to 2,2,2,2
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Create west panel to hold navigation
        // REDUCED: from 5,5 to 2,2
        JPanel westPanel = new JPanel(new BorderLayout(2, 2));

        // Create east panel for sound parameters
        // REDUCED: from 5,5 to 2,2
        JPanel eastPanel = new JPanel(new BorderLayout(2, 2));
        eastPanel.add(createSoundParametersPanel(), BorderLayout.NORTH);

        // Create top panel to hold west and east panels
        // REDUCED: from 5,5 to 2,2
        JPanel topPanel = new JPanel(new BorderLayout(2, 2));

        // Create sequence navigation panel using the custom component
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.NORTH);

        // Create center panel for the info label
        JPanel centerPanel = new JPanel(new GridBagLayout());  // Use GridBagLayout for true centering
    
        // Create and add the instrument info label
        instrumentInfoLabel = new JLabel("No drum selected");
        instrumentInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Add constraints to center vertically and horizontally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(instrumentInfoLabel, gbc);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(centerPanel, BorderLayout.CENTER); // Add center panel with label
        topPanel.add(eastPanel, BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        // REDUCED: from 5,0 to 2,0
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 2, 0));
        // REDUCED: from 10,10,10,10 to 5,5,5,5
        sequencePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Create a panel to hold both the sequence panel and drum buttons
        JPanel centerPanelMain = new JPanel(new BorderLayout());

        // Add sequence panel directly to CENTER
        centerPanelMain.add(sequencePanel, BorderLayout.CENTER);

        // Create drum pad panel with callback
        JPanel drumSection = new JPanel(new BorderLayout());
        drumSection.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); 
        
        
        drumSection.add(new MuteSequencerPanel(sequencer), BorderLayout.SOUTH);
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);
        drumSection.add(drumPadPanel, BorderLayout.CENTER);

        // IMPORTANT: Add drum pad panel DIRECTLY to the border layout, no wrapping
        // panels
        centerPanelMain.add(drumSection, BorderLayout.SOUTH);

        // Add the center panel to the main layout
        add(centerPanelMain, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        // REDUCED: from 5,5 to 2,2
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 2));

        // Add MaxLengthPanel to the WEST position
        maxLengthPanel = new DrumSequencerMaxLengthPanel(sequencer);
        bottomPanel.add(maxLengthPanel, BorderLayout.WEST);

        // Add sequence parameters panel
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        // REDUCED: from 5,0 to 2,0
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        // Use the new DrumSequenceGeneratorPanel
        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        // Add swing panel using the custom component
        swingPanel = new DrumSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        // Add the right panel container to the east position
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom panel to the main panel
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Create a column for the sequencer
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

            // Create dial with appropriate settings
            Dial dial = new Dial();
            dial.setName(getKnobLabel(i) + "-" + index);

            // Configure each dial based on its type
            switch (i) {
                case 0: // Pan
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(64); // Default value (centered)
                    dial.setKnobColor(UIUtils.getDialColor("pan"));

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            sequencer.setStepPan(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    panDials.add(dial);
                    break;

                case 1: // Delay/Decay
                    dial.setMinimum(1);
                    dial.setMaximum(200);
                    dial.setValue(60); // Default value
                    dial.setKnobColor(UIUtils.getDialColor("delay"));

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            sequencer.setStepDecay(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    delayDials.add(dial);
                    break;

                case 2: // Chorus
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(0); // Default to 0 (off)
                    dial.setKnobColor(UIUtils.getDialColor("chorus"));

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            sequencer.setStepChorus(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    chorusDials.add(dial);
                    break;

                case 3: // Reverb
                    dial.setMinimum(0);
                    dial.setMaximum(127);
                    dial.setValue(0); // Default to 0 (off)
                    dial.setKnobColor(UIUtils.getDialColor("reverb"));

                    // Add change listener
                    dial.addChangeListener(e -> {
                        if (!updatingControls && selectedPadIndex >= 0) {
                            sequencer.setStepReverb(selectedPadIndex, index, dial.getValue());
                        }
                    });
                    reverbDials.add(dial);
                    break;
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));

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
                return "Pan";
            case 1:
                return "Delay";
            case 2:
                return "Chorus";
            case 3:
                return "Reverb";
            case 4:
                return "Drive";
            case 5:
                return "Tone";
            default:
                return "Unassigned";
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

                // Add after other UI updates:
                updateSoundParametersPanel(padIndex);
            } else {
                // No valid selection - disable trigger buttons
                setTriggerButtonsEnabled(false);
            }
        }
    }

    // Replace the handleDrumPadSelected method with this version
    private void handleDrumPadSelected(int padIndex) {
        // Don't process if already selected or we're in the middle of handling a selection
        if (padIndex == selectedPadIndex || isHandlingSelection) return;

        try {
            // Set flag to prevent recursive calls
            isHandlingSelection = true;
            
            selectedPadIndex = padIndex;
            sequencer.setSelectedPadIndex(padIndex);
            
            // Get the player for this pad index
            if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[padIndex];
                
                if (player != null) {
                    // Just set active player without triggering events
                    PlayerManager.getInstance().setActivePlayer(player);
                }
            }
            
            // Update UI in a specific order
            updateInstrumentInfoLabel();
            setTriggerButtonsEnabled(true);
            refreshTriggerButtonsForPad(selectedPadIndex);
            updateDialsForSelectedPad();
            
            // Then do other updates
            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateControls(padIndex);
            }
            updateSoundParametersPanel();
            
            // Publish drum pad event LAST and only if we're handling a direct user selection
            CommandBus.getInstance().publish(
                Commands.DRUM_PAD_SELECTED,
                this, 
                new DrumPadSelectionEvent(-1, padIndex)
            );
        } finally {
            // Always clear the flag when done
            isHandlingSelection = false;
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
            if (i < panDials.size()) {
                panDials.get(i).setValue(sequencer.getStepPan(padIndex, i));
            }

            if (i < delayDials.size()) {
                delayDials.get(i).setValue(sequencer.getStepDecay(padIndex, i));
            }

            if (i < chorusDials.size()) {
                chorusDials.get(i).setValue(sequencer.getStepChorus(padIndex, i));
            }

            if (i < reverbDials.size()) {
                reverbDials.get(i).setValue(sequencer.getStepReverb(padIndex, i));
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
            // Update pan dials
            if (i < panDials.size()) {
                Dial dial = panDials.get(i);
                dial.setValue(sequencer.getStepPan(selectedPadIndex, i));
            }

            // Update delay dials
            if (i < delayDials.size()) {
                Dial dial = delayDials.get(i);
                dial.setValue(sequencer.getStepDecay(selectedPadIndex, i));
            }

            // Update chorus dials
            if (i < chorusDials.size()) {
                Dial dial = chorusDials.get(i);
                dial.setValue(sequencer.getStepChorus(selectedPadIndex, i));
            }

            // Update reverb dials
            if (i < reverbDials.size()) {
                Dial dial = reverbDials.get(i);
                dial.setValue(sequencer.getStepReverb(selectedPadIndex, i));
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
                // Update dials...
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
                // Handle theme change by recreating drum pad panel
                SwingUtilities.invokeLater(() -> {
                    // Remember which pad was selected
                    int currentSelection = selectedPadIndex;

                    // Find the parent container that contains our drumPadPanel
                    Container parent = drumPadPanel.getParent();

                    if (parent != null) {
                        // Remove the old panel
                        parent.remove(drumPadPanel);

                        // Create a new drum pad panel with updated theme colors
                        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

                        // Add it back to the layout
                        parent.add(drumPadPanel, BorderLayout.SOUTH);

                        // Update UI
                        parent.revalidate();
                        parent.repaint();

                        // Restore selection state
                        if (currentSelection >= 0) {
                            drumPadPanel.selectDrumPad(currentSelection);
                        }

                        System.out.println("DrumEffectsSequencerPanel: Recreated drum pad panel after theme change");
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
                            // Just visually select without callbacks
                            drumPadPanel.selectDrumPadNoCallback(newSelection);
                            
                            // Update minimal UI elements
                            updateInstrumentInfoLabel();
                            refreshTriggerButtonsForPad(newSelection);
                        });
                    }
                }
            }

            case Commands.PLAYER_UPDATED, Commands.INSTRUMENT_CHANGED -> {
                // Update the info label if this affects our selected pad
                if (selectedPadIndex >= 0) {
                    SwingUtilities.invokeLater(this::updateInstrumentInfoLabel);
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

            // Update UI...

            // If the step is now active, update the dials to reflect the step parameters
            if (isNowActive) {
                // Only update specific step's dials
                if (stepIndex < panDials.size()) {
                    panDials.get(stepIndex).setValue(sequencer.getStepPan(selectedPadIndex, stepIndex));
                }

                if (stepIndex < delayDials.size()) {
                    delayDials.get(stepIndex).setValue(sequencer.getStepDecay(selectedPadIndex, stepIndex));
                }

                if (stepIndex < chorusDials.size()) {
                    chorusDials.get(stepIndex).setValue(sequencer.getStepChorus(selectedPadIndex, stepIndex));
                }

                if (stepIndex < reverbDials.size()) {
                    reverbDials.get(stepIndex).setValue(sequencer.getStepReverb(selectedPadIndex, stepIndex));
                }
            }

            // Notify about pattern change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, null, this);
        }
    }

    // Replace the initializeDrumPads method with this:
    private void initializeDrumPads() {
        // Select the first pad after initialization
        SwingUtilities.invokeLater(() -> {
            drumPadPanel.selectDrumPad(0);
            handleDrumPadSelected(0);
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

    /**
     * Creates the sound parameters panel
     */
    private JPanel createSoundParametersPanel() {
        // Get the currently selected player
        int selectedPadIndex = getSelectedPadIndex();
        Player player = null;

        // Get the selected drum's Player object if available
        if (selectedPadIndex >= 0 && selectedPadIndex < sequencer.getPlayers().length) {
            player = sequencer.getPlayers()[selectedPadIndex];
        }

        // Create the standardized SoundParametersPanel
        SoundParametersPanel panel = new SoundParametersPanel(player);

        // Return the panel
        return panel;
    }

    /**
     * Updates the sound parameters panel with the selected drum's player
     */
    private void updateSoundParametersPanel(int padIndex) {
        if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
            Player player = sequencer.getPlayers()[padIndex];

            // Find and update the SoundParametersPanel
            findAndUpdateSoundPanel(this, player);
        }
    }

    /**
     * Helper method to find and update SoundParametersPanel recursively
     */
    private void findAndUpdateSoundPanel(JPanel panel, Player player) {
        if (panel instanceof SoundParametersPanel) {
            ((SoundParametersPanel) panel).setPlayer(player);
            return;
        }

        // Search children
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                findAndUpdateSoundPanel((JPanel) component, player);
            }
        }
    }

    /**
     * Updates the sound parameters panel when a drum is selected
     */
    private void updateSoundParametersPanel() {
        // Find the SoundParametersPanel and update its player
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                findAndUpdateSoundPanel((JPanel) component);
            }
        }
    }

    /**
     * Helper method to find and update SoundParametersPanel recursively
     */
    private void findAndUpdateSoundPanel(JPanel panel) {
        if (panel instanceof SoundParametersPanel) {
            // Get the selected drum's Player object
            if (selectedPadIndex >= 0 && selectedPadIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[selectedPadIndex];
                ((SoundParametersPanel) panel).setPlayer(player);
            } else {
                // No selection, clear the panel
                ((SoundParametersPanel) panel).setPlayer(null);
            }
            return;
        }

        // Search children
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                findAndUpdateSoundPanel((JPanel) component);
            }
        }
    }

    /**
     * Update the instrument info label with current player and instrument information
     */
    private void updateInstrumentInfoLabel() {
        if (selectedPadIndex < 0 || selectedPadIndex >= sequencer.getPlayers().length) {
            instrumentInfoLabel.setText("No drum selected");
            return;
        }

        Player player = sequencer.getPlayers()[selectedPadIndex];
        String playerName = player != null ? player.getName() : "Unknown";
        String instrumentName = "No Instrument";
        String channelInfo = "";

        if (player != null && player.getInstrument() != null) {
            instrumentName = player.getInstrument().getName();
            int channel = player.getChannel() != null ? player.getChannel() : 9; // Default to 10 for display
            channelInfo = " (Ch " + (channel + 1) + ")";
        }

        instrumentInfoLabel.setText(playerName + " - " + instrumentName + channelInfo);
    }
}

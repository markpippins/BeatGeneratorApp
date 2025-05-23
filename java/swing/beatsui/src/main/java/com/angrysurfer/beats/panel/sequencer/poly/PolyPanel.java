package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.panel.MainPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.AccentButton;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.DrumStepParametersEvent;
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

@Getter
@Setter
public abstract class PolyPanel extends JPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(PolyPanel.class.getName());

    private final List<TriggerButton> selectorButtons = new ArrayList<>();
    private final List<AccentButton> accentButtons = new ArrayList<>();

    private DrumSequenceNavigationPanel navigationPanel;
    private DrumSequencerParametersPanel sequenceParamsPanel; // Changed from DrumParamsSequencerParametersPanel
    private DrumSequencerMaxLengthPanel maxLengthPanel; // New field
    private DrumSequenceGeneratorPanel generatorPanel; // New field
    private DrumSequencerSwingPanel swingPanel;

    private DrumButtonsPanel drumPadPanel;

    private int selectedPadIndex = -1; // Default to no selection
    private boolean updatingControls = false;
    private boolean isHandlingSelection = false;

    private DrumSequencer sequencer;

    private Consumer<NoteEvent> noteEventConsumer;

    private MuteSequencerPanel muteSequencerPanel; // Add this field to PolyPanel

    public PolyPanel() {
        super();
        setup();
    }

    public PolyPanel(LayoutManager layout) {
        super(layout);
        setup();
    }

    private void setup() {
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

        createUI();

        // which will execute after all buttons are created
        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_SELECTED,
                Commands.DRUM_INSTRUMENTS_UPDATED,
                Commands.HIGHLIGHT_STEP,

                // Add these events to respond to DrumSequenceModifier operations
                Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED,
                Commands.PATTERN_UPDATED,
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                Commands.DRUM_STEP_EFFECTS_CHANGED,
                Commands.DRUM_GRID_REFRESH_REQUESTED,

                // Add these to respond to dialog results
                Commands.FILL_PATTERN_SELECTED,
                Commands.EUCLIDEAN_PATTERN_SELECTED,
                Commands.MAX_LENGTH_SELECTED,

                // Add mute events
                Commands.DRUM_TRACK_MUTE_CHANGED,
                Commands.DRUM_TRACK_MUTE_VALUES_CHANGED
        });

        logger.info("DrumParamsSequencerPanel registered for specific events");

        TimingBus.getInstance().register(this);
    }

    abstract JPanel createSequenceColumn(int i);


    abstract void updateControlsFromSequencer();


    private void refreshControls() {
        if (selectedPadIndex > -1) {
            refreshAccentButtonsForPad(selectedPadIndex);
            refreshTriggerButtonsForPad(selectedPadIndex);
            updateControlsFromSequencer();
        }

        updateControlsFromSequencer();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {

            case Commands.CHANGE_THEME -> {
                // Handle theme change by recreating drum pad panel
                SwingUtilities.invokeLater(this::handleThemeChange);

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

                        updateStep(previousStep, currentStep);
                    }
                }
            }

            case Commands.TRANSPORT_START, Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_CREATED,
                 Commands.PATTERN_UPDATED, Commands.DRUM_SEQUENCE_UPDATED -> refreshControls();

            case Commands.TRANSPORT_STOP -> {
                reset();
                refreshControls();
            }

            case Commands.DRUM_STEP_PARAMETERS_CHANGED -> {
                if (action.getData() instanceof DrumStepParametersEvent event
                        && event.getDrumIndex() == selectedPadIndex) {
                    refreshControls();
                }
            }

            case Commands.STEP_UPDATED, Commands.DRUM_STEP_UPDATED -> {
                // Handle step updates coming from sequencer
                if (action.getData() instanceof DrumStepUpdateEvent event
                        && event.getDrumIndex() == selectedPadIndex) {
                    updateStep(event.getOldStep(), event.getNewStep());
                }
            }

            case Commands.DRUM_PAD_SELECTED -> handleDrumPadSelected(action);

        }
    }

    private void handleDrumPadSelected(Command action) {
        // Only respond to events from other panels to avoid loops
        if (action.getData() instanceof DrumPadSelectionEvent event && action.getSender() != this) {
            int newSelection = event.getNewSelection();

            // Check if index is valid and different
            if (newSelection != selectedPadIndex
                    && newSelection >= 0
                    && newSelection < drumPadPanel.getButtonCount()) {

                // Skip heavy operations - just update necessary state
                selectedPadIndex = newSelection;

                // Update UI without triggering further events
                SwingUtilities.invokeLater(() -> {
                    drumPadPanel.selectDrumPadNoCallback(newSelection);
                    refreshControls();
                });
            }
        }
    }

    private void handleThemeChange() {
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
    }

    // Add helper method to set enabled state of all trigger buttons
    void setAccentButtonsEnabled(boolean enabled) {
        for (AccentButton button : accentButtons) {
            button.setEnabled(enabled);
            // When disabling, also clear toggle and highlight state
            if (!enabled) {
                button.setSelected(false);
                button.setHighlighted(false);
            }
            button.repaint();
        }
    }

    void setTriggerButtonsEnabled(boolean enabled) {
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

    protected JPanel createAccentPanel(int index) {
        AccentButton accentButton = new AccentButton(Integer.toString(index + 1));
        accentButton.setName("AccentButton-" + index);
        accentButton.setToolTipText("Step " + (index + 1));
        accentButton.setEnabled(true);
        accentButton.setPreferredSize(new Dimension(20, 20));
        accentButton.setMaximumSize(new Dimension(20, 20));
        accentButton.addActionListener(e -> toggleAccentForActivePad((index)));
        accentButtons.add(accentButton);

        // Center the button horizontally
        JPanel accentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        accentPanel.add(accentButton);
        return accentPanel;
    }

    int calculatePreviousStep(int currentStep) {
        if (currentStep <= 0) {
            return sequencer.getPatternLength(selectedPadIndex) - 1;
        }
        return currentStep - 1;
    }

    void updateStep(int oldStep, int newStep) {
        // First reset all buttons to ensure clean state
        for (TriggerButton button : selectorButtons) {
            button.setHighlighted(false);
        }

        for (AccentButton button : accentButtons) {
            button.setHighlighted(false);
        }

        // Then highlight only the current step
        if (newStep >= 0 && newStep < selectorButtons.size()) {
            TriggerButton newButton = selectorButtons.get(newStep);
            newButton.setHighlighted(true);
            newButton.repaint();

            // Highlight accent button too
            if (newStep < accentButtons.size()) {
                AccentButton accentButton = accentButtons.get(newStep);
                accentButton.setHighlighted(true);
                accentButton.repaint();
            }
        }
    }

    void createUI() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Create required panels BEFORE initialize() is called
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

        // Clear existing collections to avoid duplicates
        accentButtons.clear();
        selectorButtons.clear();

        // clearDials();
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

        // Create and add mute sequencer panel
        muteSequencerPanel = new MuteSequencerPanel(sequencer);
        drumSection.add(muteSequencerPanel, BorderLayout.NORTH);
        drumSection.add(drumPadPanel, BorderLayout.CENTER);

        UIHelper.addSafely(centerPanel, drumSection, BorderLayout.SOUTH);

        UIHelper.addSafely(this, centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(2, 2));
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);
        bottomPanel.add(sequenceParamsPanel, BorderLayout.WEST);

        maxLengthPanel = new DrumSequencerMaxLengthPanel(sequencer);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rightPanel.add(maxLengthPanel);

        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        swingPanel = new DrumSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

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

    }

    // Replace the handleDrumPadSelected method with this version
    private void handleDrumPadSelected(int padIndex) {
        // Don't process if already selected or we're in the middle of handling a
        // selection
        if (padIndex == selectedPadIndex || isHandlingSelection) {
            Player player = sequencer.getPlayers()[padIndex];
            player.noteOn(player.getRootNote(), 100, 100);
            return;
        }

        try {
            // Set flag to prevent recursive calls
            isHandlingSelection = true;

            selectedPadIndex = padIndex;
            sequencer.setSelectedPadIndex(padIndex);

            // Get the player for this pad index
            if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[padIndex];

                if (player != null && player.getInstrument() != null) {
                    CommandBus.getInstance().publish(Commands.STATUS_UPDATE, this, new StatusUpdate("Selected pad: " + player.getName()));
                    CommandBus.getInstance().publish(Commands.PLAYER_ACTIVATION_REQUEST, this, player);
                }
            }

            // Update UI in a specific order
            setAccentButtonsEnabled(true);
            setTriggerButtonsEnabled(true);
            refreshAccentButtonsForPad(selectedPadIndex);
            refreshTriggerButtonsForPad(selectedPadIndex);

            // Update mute panel for the newly selected drum pad
            if (muteSequencerPanel != null) {
                muteSequencerPanel.syncWithSequencer();
            }

            // Then do other updates
            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateControls(padIndex);
            }

            // Publish drum pad event LAST and only if we're handling a direct user
            // selection
            CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED, this, new DrumPadSelectionEvent(-1, padIndex));
        } finally {
            // Always clear the flag when done
            isHandlingSelection = false;
        }
    }

    void refreshAccentButtonsForPad(int padIndex) {
        // Handle no selection case
        if (padIndex < 0) {
            for (AccentButton button : accentButtons) {
                button.setSelected(false);
                button.setHighlighted(false);
                button.setEnabled(false);
                button.repaint();
            }
            return;
        }

        // A pad is selected, so enable all buttons
        setAccentButtonsEnabled(true);

        // Update each button's state
        for (int i = 0; i < accentButtons.size(); i++) {
            AccentButton button = accentButtons.get(i);

            // Set selected state based on pattern
            boolean isActive = sequencer.getSequenceData().isStepAccented(padIndex, i);
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
        }

    }

    void refreshTriggerButtonsForPad(int padIndex) {
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
        }

    }

    JPanel createTriggerPanel(int index) {
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setEnabled(selectedPadIndex >= 0);
        triggerButton.addActionListener(e -> toggleStepForActivePad(index));

        selectorButtons.add(triggerButton);

        // Center the button horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(triggerButton);
        return buttonPanel;
    }

    void toggleAccentForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0) {
            sequencer.toggleAccent(selectedPadIndex, stepIndex);
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, sequencer);
        }
    }

    void toggleStepForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0) {
            // Toggle the step in the sequencer
            boolean wasActive = sequencer.isStepActive(selectedPadIndex, stepIndex);
            sequencer.toggleStep(selectedPadIndex, stepIndex);

            // Verify the toggle took effect
            boolean isNowActive = sequencer.isStepActive(selectedPadIndex, stepIndex);
            boolean hasAccent = sequencer.getSequenceData().isStepAccented(selectedPadIndex, stepIndex);

            if (wasActive == isNowActive) {
                System.err.println("WARNING: Toggle step failed for pad " + selectedPadIndex + ", step " + stepIndex);
            }

            // Update the visual state of the button
            TriggerButton triggerButton = selectorButtons.get(stepIndex);
            triggerButton.setSelected(isNowActive);
            triggerButton.repaint();

            // If the step is now active, update the dials to reflect the step parameters
            if (isNowActive) {
                toggleStepForActivePad(stepIndex);
            }

            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, null, this);
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

        for (AccentButton button : accentButtons) {
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
     * Get the knob label for a specific index
     */
    String getKnobLabel(int i) {
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

    // Helper method to find the MainPanel ancestor
    MainPanel findMainPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof MainPanel) {
                return (MainPanel) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    Dial createDial(String name, int index, int minimum, int maximum, int defaultValue) {
        Dial dial = new Dial();

        dial.setMinimum(minimum);
        dial.setMaximum(maximum);
        dial.setValue(defaultValue);
        dial.setKnobColor(UIHelper.getDialColor(name)); // Set knob color
        dial.setName(getKnobLabel(index) + "-" + index);

        return dial;
    }

}
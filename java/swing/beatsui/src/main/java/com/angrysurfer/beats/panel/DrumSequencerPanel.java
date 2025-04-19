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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.visualization.Visualizer;
import com.angrysurfer.beats.widget.DrumSequencerButton;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.angrysurfer.core.service.InternalSynthManager;

import lombok.Getter;
import lombok.Setter;

/**
 * A sequencer panel with X0X-style step sequencing capabilities. This is the UI
 * component for the DrumSequencer.
 */
@Getter
@Setter
public class DrumSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumEffectsSequencerPanel.class);

    private DrumSequencerGridButton[][] gridButtons;

    // UI Components
    private List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private DrumSequencerInfoPanel drumInfoPanel;
    private DrumSequenceNavigationPanel navigationPanel;
    private Visualizer visualizer;

    // Core sequencer - manages all sequencing logic
    private DrumSequencer sequencer;

    // UI state
    private int selectedPadIndex = 0; // Default to first drum

    // Parameters panel components
    private DrumSequencerParametersPanel sequenceParamsPanel;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;

    // Replace the local DRUM_PAD_COUNT constant with DrumSequencer's version
    private static final int DRUM_PAD_COUNT = DrumSequencer.DRUM_PAD_COUNT;

    // Add these constants referencing DrumSequencer constants
    private static final int DEFAULT_VELOCITY = DrumSequencer.DEFAULT_VELOCITY;
    private static final int DEFAULT_DECAY = DrumSequencer.DEFAULT_DECAY;
    private static final int DEFAULT_PROBABILITY = DrumSequencer.DEFAULT_PROBABILITY;
    private static final int MIN_SWING = DrumSequencer.MIN_SWING;
    private static final int MAX_SWING = DrumSequencer.MAX_SWING;
    private static final int MIDI_DRUM_NOTE_OFFSET = DrumSequencer.MIDI_DRUM_NOTE_OFFSET;

    // UI constants
    private static final int DRUM_BUTTON_SIZE = 28;
    private static final int GRID_BUTTON_SIZE = 24;
    private static final int CONTROL_HEIGHT = 25;
    private static final int SMALL_CONTROL_WIDTH = 40;
    private static final int MEDIUM_CONTROL_WIDTH = 60;
    private static final int LARGE_CONTROL_WIDTH = 90;

    // Debug mode flag
    private boolean debugMode = false;

    // Add this field to the class
    private boolean isPlaying = false;

    // Add this field to the class
    private boolean isSelectingDrumPad = false;

    // Add this field to the class
    private boolean updatingUI = false;

    // Add this field to DrumSequencerPanel:
    private DrumSequencerSwingPanel swingPanel;

    // Add this field to DrumSequencerPanel:
    private DrumSelectorPanel drumSelectorPanel;

    /**
     * Create a new DrumSequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer
        sequencer = DrumSequencerManager.getInstance().newSequencer(
                noteEventConsumer,
                event -> updateStepHighlighting(event.getDrumIndex(), event.getOldStep(), event.getNewStep()));

        // Register with the command bus - MAKE SURE THIS IS HERE
        CommandBus.getInstance().register(this);

        // Debug: Print confirmation of registration
        System.out.println("DrumSequencerPanel registered with CommandBus");

        // Add as listener
        CommandBus.getInstance().register(this);
        logger.info("DrumSequencerPanel registered as listener");

        // Initialize UI components
        initialize();
    }

    /**
     * Initialize the UI components - revised to fix duplication and layout
     * issues
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

        // Create sequence navigation panel
        DrumSequenceNavigationPanel navigationPanel = new DrumSequenceNavigationPanel(sequencer);

        // Create swing control panel
        JPanel swingPanel = createSwingControlPanel();

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.NORTH);

        // Sound parameters go NORTH-EAST
        eastPanel.add(createSoundParametersPanel(), BorderLayout.NORTH);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(eastPanel, BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

        // Create drum selector panel and add to WEST of main layout
        JPanel drumPadsPanel = createDrumPadsPanel();
        add(drumPadsPanel, BorderLayout.WEST);

        // Create the center grid panel with sequence buttons
        JPanel sequencePanel = createSequenceGridPanel();
        // visualizer = new Visualizer(sequencePanel, gridButtons);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        // Create and add the sequence parameters panel using our new class
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer, this);
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Create and add generate panel
        JPanel generatePanel = createGeneratePanel();
        rightPanel.add(generatePanel);

        // Use the new swing panel
        swingPanel = createSwingControlPanel();
        rightPanel.add(swingPanel);

        // Add the right panel container to the east position
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom panel to the main panel
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a dedicated panel for drum pattern generation controls
     */
    private JPanel createGeneratePanel() {
        // Size constants
        final int SMALL_CONTROL_WIDTH = 40;
        final int MEDIUM_CONTROL_WIDTH = 90;
        final int CONTROL_HEIGHT = 25;

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Generate"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Create density combo without a label
        String[] densityOptions = { "25%", "50%", "75%", "100%" };
        JComboBox<String> densityCombo = new JComboBox<>(densityOptions);
        densityCombo.setSelectedIndex(1); // Default to 50%
        densityCombo.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        densityCombo.setToolTipText("Set pattern density");

        // Generate button with dice icon
        JButton generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        generateButton.setMargin(new Insets(2, 2, 2, 2));
        generateButton.addActionListener(e -> {
            // Get selected density from the combo
            int density = (densityCombo.getSelectedIndex() + 1) * 25;
            sequencer.generatePattern(density);
            refreshGridUI();
        });

        panel.add(generateButton);
        panel.add(densityCombo);

        return panel;
    }

    // Replace createSwingControlPanel with this:
    private JPanel createSwingControlPanel() {
        // Create panel with specified dimensions and add to container
        swingPanel = new DrumSequencerSwingPanel(sequencer);
        return swingPanel;
    }

    /**
     * Create the drum pads panel on the left side
     */
    private JPanel createDrumPadsPanel() {
        // Create and return the new drum selector panel
        drumSelectorPanel = new DrumSelectorPanel(sequencer, this);
        return drumSelectorPanel;
    }

    /**
     * Handle selection of a drum pad - completely revised to fix display issues
     */
    public void selectDrumPad(int padIndex) {
        logger.info("DrumSequencerPanel: Selecting drum pad {}", padIndex);
        // Guard against recursive calls
        if (isSelectingDrumPad) {
            return;
        }

        isSelectingDrumPad = true;
        try {
            // Store the selected pad index
            selectedPadIndex = padIndex;

            // Tell sequencer about the selection (without sending another event back)
            sequencer.setSelectedPadIndex(padIndex);

            // Update UI for all drum rows
            for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                updateRowAppearance(i, i == padIndex);
            }
            
            // Update the drum selector panel buttons
            if (drumSelectorPanel != null) {
                drumSelectorPanel.updateButtonSelection(padIndex);
            }

            // Update parameter controls for the selected drum
            updateParameterControls();

            // Update info panel
            if (drumInfoPanel != null) {
                drumInfoPanel.updateInfo(padIndex);
            }

            // IMPORTANT: Notify other panels through the command bus
            CommandBus.getInstance().publish(
                    Commands.DRUM_PAD_SELECTED,
                    this, // Send 'this' as the source to prevent circular updates
                    new DrumPadSelectionEvent(-1, padIndex));
            logger.info("DrumSequencerPanel: Published selection event for pad {}", padIndex);
        } finally {
            isSelectingDrumPad = false;
        }
    }

    /**
     * Update appearance of an entire drum row
     */
    private void updateRowAppearance(int drumIndex, boolean isSelected) {
        int patternLength = sequencer.getPatternLength(drumIndex);

        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Keep all buttons visible
                button.setVisible(true);

                // Style based on whether step is active and in pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Update button appearance using the button's own functionality
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Add subtle highlighting to the selected row
                if (isSelected) {
                    // Highlight the selected row's border
                    button.setBorder(BorderFactory.createLineBorder(
                            UIUtils.dustyAmber, 1));
                } else {
                    // Normal border for other rows
                    button.setBorder(BorderFactory.createLineBorder(
                            Color.DARK_GRAY, 1));
                }
            }
        }
    }

    /**
     * Update step highlighting during playback
     *
     * @param drumIndex The drum to highlight
     * @param oldStep   The previous step to un-highlight
     * @param newStep   The new step to highlight
     */
    private void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Only update highlighting if we're actually playing
        if (!isPlaying) {
            return;
        }

        // Ensure we're on the EDT for UI updates
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateStepHighlighting(drumIndex, oldStep, newStep));
            return;
        }

        // Calculate button indices based on the drum and step
        int oldButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + oldStep;
        int newButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + newStep;

        // Ensure indices are valid
        if (oldButtonIndex >= 0 && oldButtonIndex < triggerButtons.size()) {
            DrumSequencerGridButton oldButton = triggerButtons.get(oldButtonIndex);
            if (oldButton != null) {
                oldButton.setHighlighted(false);
            }
        }

        if (newButtonIndex >= 0 && newButtonIndex < triggerButtons.size()) {
            DrumSequencerGridButton newButton = triggerButtons.get(newButtonIndex);
            if (newButton != null) {
                newButton.setHighlighted(true);
            }
        }
    }

    /**
     * Update all step buttons for a specific drum
     */
    public void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT || triggerButtons.isEmpty()) {
            // Invalid drum index or buttons not initialized yet
            logger.warn("Cannot update step buttons: invalid drum index {} or buttons not initialized", drumIndex);
            return;
        }

        // Get pattern length for this drum
        int patternLength = sequencer.getPatternLength(drumIndex);
        logger.debug("Updating step buttons for drum {} with pattern length {}", drumIndex, patternLength);

        // Update all buttons for this row
        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;

            // Safety check
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Make button visible regardless of pattern length - CRITICAL FIX
                button.setVisible(true);

                // Update button state based on pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = isInPattern && sequencer.isStepActive(drumIndex, step);

                // Update button state
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Style based on whether step is active and in pattern
                if (!isInPattern) {
                    button.setBackground(UIUtils.charcoalGray);
                } else {
                    if (isActive) {
                        button.setBackground(UIUtils.deepOrange);
                    } else {
                        button.setBackground(UIUtils.slateGray);
                    }
                }

                // Always repaint
                button.repaint();
            }
        }
    }

    /**
     * Create step button with proper behavior
     */
    private DrumSequencerGridButton createStepButton(int drumIndex, int step) {
        DrumSequencerGridButton button = new DrumSequencerGridButton();

        // Make button square with constant size
        button.setPreferredSize(new Dimension(GRID_BUTTON_SIZE, GRID_BUTTON_SIZE));

        // Add debug info if needed
        if (debugMode) {
            button.setText(String.format("%d,%d", drumIndex, step));
            button.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 8));
        }

        // Add action listener to toggle step state
        button.addActionListener(e -> {
            sequencer.toggleStep(drumIndex, step);
            button.setSelected(sequencer.isStepActive(drumIndex, step));
        });

        // Add right-click context menu
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }
        });

        return button;
    }

    /**
     * Create the step grid panel with proper cell visibility
     */
    private JPanel createSequenceGridPanel() {
        // Use consistent cell size with even spacing
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, sequencer.getDefaultPatternLength(), 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Initialize both storage structures
        triggerButtons = new ArrayList<>(DRUM_PAD_COUNT * sequencer.getDefaultPatternLength()); // Pre-size the list
        gridButtons = new DrumSequencerGridButton[DRUM_PAD_COUNT][sequencer.getDefaultPatternLength()]; // Initialize the 2D array

        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                DrumSequencerGridButton button = createStepButton(drumIndex, step);

                // IMPORTANT: Set initial state based on sequencer
                boolean isInPattern = step < sequencer.getPatternLength(drumIndex);
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Configure button
                button.setEnabled(isInPattern); // Use enabled state for in-pattern
                button.setSelected(isActive);
                button.setVisible(true); // Always make buttons visible

                // Add to panel and tracking list
                panel.add(button);
                triggerButtons.add(button);

                // Also store in the 2D array for direct access by coordinates
                gridButtons[drumIndex][step] = button;
            }
        }

        return panel;
    }

    /**
     * Display a context menu for a step button
     */
    private void showContextMenu(Component component, int x, int y, int drumIndex, int step) {
        JPopupMenu menu = new JPopupMenu();

        // Add menu items for step operations
        JMenuItem fillItem = new JMenuItem("Fill From Here...");
        fillItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            Object[] params = new Object[] { sequencer, drumIndex, step };
            CommandBus.getInstance().publish(Commands.SHOW_FILL_DIALOG, this, params);
        });
        menu.add(fillItem);

        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> clearRow(drumIndex));
        menu.add(clearRowItem);
        
        // Add Set Max Length option
        JMenuItem setMaxLengthItem = new JMenuItem("Set Max Length...");
        setMaxLengthItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            CommandBus.getInstance().publish(Commands.SHOW_MAX_LENGTH_DIALOG, this, sequencer);
        });
        menu.add(setMaxLengthItem);

        // Add divider
        menu.addSeparator();

        // Add pattern generation items
        JMenuItem patternItem = new JMenuItem("Apply Pattern...");
        patternItem.addActionListener(e -> {
            Object[] options = { "Every 2nd Step", "Every 3rd Step", "Every 4th Step" };
            int choice = javax.swing.JOptionPane.showOptionDialog(
                    this,
                    "Choose pattern type:",
                    "Pattern Generator",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice >= 0) {
                applyPatternEveryN(drumIndex, 2 + choice);
            }
        });
        menu.add(patternItem);

        // Add Euclidean Pattern option
        JMenuItem euclideanItem = new JMenuItem("Euclidean Pattern...");
        euclideanItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            Object[] params = new Object[] { sequencer, drumIndex };
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });
        menu.add(euclideanItem);

        // Show the menu
        menu.show(component, x, y);
    }

    /**
     * Clear all steps in a drum row
     */
    public void clearRow(int drumIndex) {
        boolean success = DrumSequenceModifier.clearDrumTrack(sequencer, drumIndex);
        if (success) {
            updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Apply a pattern that activates every Nth step
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, n);
        if (success) {
            updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Refresh the entire grid UI to match the current sequencer state
     */
    public void refreshGridUI() {
        if (triggerButtons == null || triggerButtons.isEmpty()) {
            logger.warn("Cannot refresh grid UI - triggerButtons list is empty");
            return;
        }

        logger.info("Refreshing entire grid UI for sequence {}", sequencer.getDrumSequenceId());

        // Temporarily disable any listeners if needed
        boolean wasListeningToChanges = true; // Add field if needed

        try {
            // wasListeningToChanges = disableUIEventListeners();

            // Ensure we refresh ALL drums and ALL steps
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) { // Just update the visible
                    // sequencer.getDefaultPatternLength() steps
                    // Correct index calculation: drumRow * stepsPerRow + stepColumn
                    int buttonIndex = drumIndex * sequencer.getDefaultPatternLength() + step;

                    if (buttonIndex < triggerButtons.size()) {
                        DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                        if (button != null) {
                            // Get the current state from the sequencer
                            boolean active = sequencer.isStepActive(drumIndex, step);

                            // Force update button state without triggering events
                            button.setToggled(active);
                            button.setHighlighted(false); // Clear any highlighting
                            button.repaint(); // Force immediate repaint
                        }
                    }
                }

                // Update the drum row's appearance
                updateRowAppearance(drumIndex, drumIndex == selectedPadIndex);
            }
        } finally {
            // Re-enable listeners if needed
            // if (wasListeningToChanges) {
            // enableUIEventListeners();
            // }
        }

        // Make sure the UI shows the correct selected drum
        updateParameterControls();

        // Ensure proper visual refresh
        revalidate();
        repaint();

        logger.info("Grid UI refresh completed");
    }

    /**
     * Clear all step highlighting across all drum rows
     */
    private void clearAllStepHighlighting() {
        for (DrumSequencerGridButton button : triggerButtons) {
            button.clearTemporaryState();
            button.repaint();
        }
    }

    /**
     * Update the parameter controls to reflect the current selected drum
     */
    private void updateParameterControls() {
        // Check if we have a valid selection before updating
        if (selectedPadIndex < 0 || selectedPadIndex >= DRUM_PAD_COUNT) {
            logger.warn("Cannot update parameters - invalid drum index: {}", selectedPadIndex);
            return;
        }

        // Use the new panel's method to update controls
        sequenceParamsPanel.updateControls(selectedPadIndex);
    }

    /**
     * Sync the UI with the current state of the sequencer
     */
    public void syncUIWithSequencer() {
        // For each drum pad
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) { // Just update the visible
                                                                        // sequencer.getDefaultPatternLength() steps
                // Correct index calculation: drumRow * stepsPerRow + stepColumn
                int buttonIndex = drumIndex * sequencer.getDefaultPatternLength() + step;

                if (buttonIndex < triggerButtons.size()) {
                    DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                    button.setToggled(sequencer.isStepActive(drumIndex, step));
                }
            }
        }
    }

    /**
     * Enable debug mode to show grid indices
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;

        // Show indices on buttons in debug mode
        if (triggerButtons != null) {
            for (int i = 0; i < triggerButtons.size(); i++) {
                DrumSequencerGridButton button = triggerButtons.get(i);
                int drumIndex = i / sequencer.getDefaultPatternLength();
                int stepIndex = i % sequencer.getDefaultPatternLength();

                if (debugMode) {
                    button.setText(drumIndex + "," + stepIndex);
                    button.setForeground(Color.YELLOW);
                } else {
                    button.setText("");
                }
            }
        }
    }

    /**
     * Handle command bus messages
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED -> {
                // Update the UI to reflect new sequence data
                SwingUtilities.invokeLater(() -> {
                    // Update the entire grid UI
                    refreshGridUI();

                    // Update the parameter controls for the selected drum
                    updateParameterControls();

                    // Update the drum info panel
                    if (drumInfoPanel != null) {
                        drumInfoPanel.updateInfo(selectedPadIndex);
                    }

                    // Reset all step highlighting
                    clearAllStepHighlighting();
                });
            }

            case Commands.DRUM_PAD_SELECTED -> {
                if (action.getData() instanceof DrumPadSelectionEvent event) {
                    selectDrumPad(event.getNewSelection());
                }
            }

            case Commands.TRANSPORT_START -> {
                // Show step highlighting when playing starts
                isPlaying = true;
            }

            case Commands.TRANSPORT_STOP -> {
                // Hide step highlighting when playing stops
                isPlaying = false;
                clearAllStepHighlighting();
            }

            case Commands.MAX_LENGTH_SELECTED -> {
                if (action.getData() instanceof Integer maxLength) {
                    List<Integer> updatedDrums = DrumSequenceModifier.applyMaxPatternLength(sequencer, maxLength);
                    
                    // Update UI for affected drums
                    for (int drumIndex : updatedDrums) {
                        updateStepButtonsForDrum(drumIndex);
                    }
                    
                    // Update parameter controls if the selected drum was affected
                    if (updatedDrums.contains(selectedPadIndex)) {
                        updateParameterControls();
                    }
                    
                    // Show confirmation message
                    showPatternLengthUpdateMessage(updatedDrums.size());
                }
            }

            case Commands.EUCLIDEAN_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    boolean[] pattern = (boolean[]) result[1];
                    
                    // Use the static method from DrumSequenceModifier
                    boolean success = DrumSequenceModifier.applyEuclideanPattern(sequencer, drumIndex, pattern);
                    
                    // If successful, update the UI
                    if (success) {
                        updateStepButtonsForDrum(drumIndex);
                        updateParameterControls();
                    }
                }
            }

            case Commands.FILL_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    int startStep = (Integer) result[1];
                    String fillType = (String) result[2];

                    // Apply the fill pattern
                    int patternLength = sequencer.getPatternLength(drumIndex);

                    for (int i = startStep; i < patternLength; i++) {
                        boolean shouldActivate = false;

                        switch (fillType) {
                            case "all" -> shouldActivate = true;
                            case "everyOther" -> shouldActivate = ((i - startStep) % 2) == 0;
                            case "every4th" -> shouldActivate = ((i - startStep) % 4) == 0;
                            case "decay" -> {
                                shouldActivate = true;
                                sequencer.setVelocity(drumIndex, 
                                    Math.max(DEFAULT_VELOCITY / 2, DEFAULT_VELOCITY - ((i - startStep) * 8)));
                            }
                        }

                        if (shouldActivate) {
                            sequencer.toggleStep(drumIndex, i);
                        }
                    }

                    // Update UI to reflect changes
                    updateStepButtonsForDrum(drumIndex);
                }
            }
        }
    }

    /**
     * Creates the sound parameters panel with drum kit selection and sound editing
     */
    private JPanel createSoundParametersPanel() {
        // Size constants
        final int SMALL_CONTROL_WIDTH = 30;
        final int LARGE_CONTROL_WIDTH = 90;
        final int CONTROL_HEIGHT = 25;

        // Create the panel with a titled border
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sound Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Create kit/preset combo
        JComboBox<String> kitCombo = new JComboBox<>();
        kitCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH * 2, CONTROL_HEIGHT));
        kitCombo.setToolTipText("Select drum kit");
        populateDrumKitCombo(kitCombo);

        // Add listener for kit changes
        kitCombo.addActionListener(e -> {
            if (updatingUI || kitCombo.getSelectedIndex() < 0)
                return;

            int kitIndex = kitCombo.getSelectedIndex();
            logger.info("Selected drum kit index: {}", kitIndex);

            // Get the currently selected player from the players array
            int selectedDrum = getSelectedPadIndex();
            if (selectedDrum >= 0 && selectedDrum < sequencer.getPlayers().length) {
                // Update the selected drum's kit/preset
                sequencer.getPlayers()[selectedDrum].setPreset(kitIndex);

                // Inform the system about the kit change
                CommandBus.getInstance().publish(
                        Commands.PLAYER_UPDATED,
                        this,
                        sequencer.getPlayers()[selectedDrum]);
            }
        });

        // Create edit button with pencil icon and skinny width
        JButton editButton = new JButton("✎");
        editButton.setToolTipText("Edit drum sound");
        editButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        editButton.setMargin(new Insets(1, 1, 1, 1));
        editButton.setFocusable(false);

        // Add listener for edit button
        editButton.addActionListener(e -> {
            // Get the currently selected player
            int selectedDrum = getSelectedPadIndex();
            if (selectedDrum >= 0 && selectedDrum < sequencer.getPlayers().length) {
                logger.info("Opening drum sound editor for: {}", sequencer.getPlayers()[selectedDrum].getName());

                // Send the selected player to the editor
                CommandBus.getInstance().publish(
                        Commands.PLAYER_SELECTED,
                        this,
                        sequencer.getPlayers()[selectedDrum]);

                CommandBus.getInstance().publish(
                        Commands.PLAYER_EDIT_REQUEST,
                        this,
                        sequencer.getPlayers()[selectedDrum]);
            } else {
                logger.warn("Cannot edit player - No drum selected or player not initialized");
            }
        });

        // Add components to panel
        panel.add(kitCombo);
        panel.add(editButton);

        return panel;
    }

    /**
     * Populate the drum kit combo with available drum kits
     */
    private void populateDrumKitCombo(JComboBox<String> combo) {
        updatingUI = true;
        int selectedDrum = getSelectedPadIndex();
        if (selectedDrum >= 0 && selectedDrum < sequencer.getPlayers().length)
            try {
                combo.removeAllItems();

                Player player = sequencer.getPlayers()[selectedDrum];
                if (player == null) {
                    logger.warn("No player found for selected drum index: {}", selectedDrum);
                    return;
                }
                InstrumentWrapper instrument = player.getInstrument();
                if (instrument == null) {
                    logger.warn("No instrument found for selected drum index: {}", selectedDrum);
                    return;
                }

                Long id = instrument.getId();
                if (id == null) {
                    logger.warn("No instrument ID found for selected drum index: {}", selectedDrum);
                    return;
                }

                // Get the list of drum kit names
                List<String> presets = InternalSynthManager.getInstance().getPresetNames(id);

                // Add each kit with its index
                for (int i = 0; i < presets.size(); i++) {
                    combo.addItem(i + ": " + presets.get(i));
                }

                // Select current kit if available for the selected drum
                if (sequencer.getPlayers()[selectedDrum] != null &&
                        sequencer.getPlayers()[selectedDrum].getPreset() != null) {

                    int currentKit = sequencer.getPlayers()[selectedDrum].getPreset();
                    if (currentKit >= 0 && currentKit < presets.size()) {
                        combo.setSelectedItem(currentKit + ": " + presets.get(currentKit));
                    }
                }
            } finally {
                updatingUI = false;
            }
    }

    /**
     * Shows a confirmation dialog for pattern length updates
     * @param updatedCount The number of drum patterns that were modified
     */
    private void showPatternLengthUpdateMessage(int updatedCount) {
        SwingUtilities.invokeLater(() -> {
            if (updatedCount > 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Updated pattern length for " + updatedCount + " drums.",
                    "Pattern Length Updated",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "No drum patterns were affected.",
                    "Pattern Length Check",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }

    // If there's an updateSwingControls() method, update it too:
    private void updateSwingControls() {
        if (swingPanel != null) {
            swingPanel.updateControls();
        }
    }
}

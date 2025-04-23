package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.visualization.Visualizer;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
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

    private static final Logger logger = LoggerFactory.getLogger(DrumParamsPanel.class);

    // UI Components
    private DrumSequencerInfoPanel drumInfoPanel;
    private DrumSequenceNavigationPanel navigationPanel;
    private Visualizer visualizer;

    // Core sequencer - manages all sequencing logic
    private DrumSequencer sequencer;

    // UI state
    // private int selectedPadIndex = 0; // Default to first drum

    // Parameters panel components
    private SequencerParametersPanel sequenceParamsPanel;
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
    private DrumSwingPanel swingPanel;

    // Add this field to DrumSequencerPanel:
    private DrumSelectorPanel drumSelectorPanel;

    // Add this field instead:
    private DrumSequencerGridPanel gridPanel;

    private JScrollPane scrollPane;

    private JPanel sequencePanel;

    // Add this field to the class:
    private DrumSequenceGeneratorPanel generatorPanel;

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
                event -> gridPanel.updateStepHighlighting(event.getDrumIndex(), event.getOldStep(),
                        event.getNewStep()));

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
        sequencePanel = createSequenceGridPanel();
        // visualizer = new Visualizer(sequencePanel, gridPanel);

        // Wrap in scroll pane
        scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(new MaxLengthPanel(sequencer), BorderLayout.WEST);

        // Create and add the sequence parameters panel using our new class
        sequenceParamsPanel = new SequencerParametersPanel(sequencer);

        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Create and add generate panel
        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        // Use the new swing panel
        swingPanel = createSwingControlPanel();
        rightPanel.add(swingPanel);

        // Add the right panel container to the east position
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom panel to the main panel
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Replace createSwingControlPanel with this:
    private JPanel createSwingControlPanel() {
        // Create panel with specified dimensions and add to container
        swingPanel = new DrumSwingPanel(sequencer);
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
     * Create the sequence grid panel with step buttons
     */
    private JPanel createSequenceGridPanel() {
        gridPanel = new DrumSequencerGridPanel(sequencer, this);
        return gridPanel;
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
            DrumSequencerManager.getInstance().setSelectedPadIndex(padIndex);

            // Tell sequencer about the selection (without sending another event back)
            sequencer.setSelectedPadIndex(padIndex);

            // Update UI for all drum rows
            for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                gridPanel.updateRowAppearance(i, i == padIndex);
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
     * Update the parameter controls to reflect the current selected drum
     */
    private void updateParameterControls() {
        // Check if we have a valid selection before updating
        if (DrumSequencerManager.getInstance().getSelectedPadIndex() < 0 || DrumSequencerManager.getInstance().getSelectedPadIndex() >= DRUM_PAD_COUNT) {
            // logger.warn("Cannot update parameters - invalid drum index: {}", selectedPadIndex);
            return;
        }

        // Use the new panel's method to update controls
        sequenceParamsPanel.updateControls(DrumSequencerManager.getInstance().getSelectedPadIndex());
    }

    /**
     * Enable debug mode to show grid indices
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;
        gridPanel.toggleDebugMode();
    }

    /**
     * Enable or disable debug mode to show grid indices
     * 
     * @param enabled Whether debug mode should be enabled
     */
    public void toggleDebugMode(boolean enabled) {
        this.debugMode = enabled;

        // Delegate to the grid panel
        if (gridPanel != null) {
            gridPanel.toggleDebugMode();
        }
    }

    /**
     * Handle command bus messages
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED -> {
                // Update the UI to reflect new sequence data
                SwingUtilities.invokeLater(() -> {
                    // Update the entire grid UI
                    gridPanel.refreshGridUI();

                    // Update the parameter controls for the selected drum
                    updateParameterControls();

                    // Update the drum info panel
                    if (drumInfoPanel != null) {
                        drumInfoPanel.updateInfo(DrumSequencerManager.getInstance().getSelectedPadIndex());
                    }

                    // Reset all step highlighting
                    gridPanel.clearAllStepHighlighting();
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
                if (gridPanel != null) {
                    gridPanel.setPlayingState(true);
                }
            }

            case Commands.TRANSPORT_STOP -> {
                // Hide step highlighting when playing stops
                isPlaying = false;
                if (gridPanel != null) {
                    gridPanel.setPlayingState(false);
                }
            }

            case Commands.MAX_LENGTH_SELECTED -> {
                if (action.getData() instanceof Integer maxLength) {
                    List<Integer> updatedDrums = DrumSequenceModifier.applyMaxPatternLength(sequencer, maxLength);

                    // Update UI for affected drums
                    for (int drumIndex : updatedDrums) {
                        gridPanel.updateStepButtonsForDrum(drumIndex);
                    }

                    // Update parameter controls if the selected drum was affected
                    if (updatedDrums.contains(DrumSequencerManager.getInstance().getSelectedPadIndex())) {
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
                        gridPanel.updateStepButtonsForDrum(drumIndex);
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
                    gridPanel.updateStepButtonsForDrum(drumIndex);
                }
            }

            case Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Update step buttons for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        updateStepButtonsForDrum(drumIndex);
                    });
                }
            }

            case Commands.DRUM_GRID_REFRESH_REQUESTED -> {
                // Refresh the entire grid UI
                SwingUtilities.invokeLater(() -> {
                    refreshGridUI();
                });
            }

            case Commands.DRUM_PATTERN_CLEAR_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Clear the pattern for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        clearRow(drumIndex);
                    });
                }
            }
        }
    }

    /**
     * Clears all steps for a specific drum track
     * 
     * @param drumIndex The index of the drum to clear
     */
    public void clearRow(int drumIndex) {
        boolean success = DrumSequenceModifier.clearDrumTrack(sequencer, drumIndex);
        if (success) {
            // Update UI after successful clear
            gridPanel.updateStepButtonsForDrum(drumIndex);

            // Update parameter controls if clearing the selected drum
            if (drumIndex == DrumSequencerManager.getInstance().getSelectedPadIndex()) {
                updateParameterControls();
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
            int selectedDrum = DrumSequencerManager.getInstance().getSelectedPadIndex();
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
            int selectedDrum = DrumSequencerManager.getInstance().getSelectedPadIndex();
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
        int selectedDrum = DrumSequencerManager.getInstance().getSelectedPadIndex();
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
     * 
     * @param updatedCount The number of drum patterns that were modified
     */
    private void showPatternLengthUpdateMessage(int updatedCount) {
        SwingUtilities.invokeLater(() -> {
            if (updatedCount > 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Updated pattern length for " + updatedCount + " drums.",
                        "Pattern Length Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "No drum patterns were affected.",
                        "Pattern Length Check",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // If there's an updateSwingControls() method, update it too:
    private void updateSwingControls() {
        if (swingPanel != null) {
            swingPanel.updateControls();
        }
    }

    /**
     * Updates the UI for a specific drum's steps
     * 
     * @param drumIndex The index of the drum to update
     */
    public void updateStepButtonsForDrum(int drumIndex) {
        if (gridPanel != null) {
            gridPanel.updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Refreshes the entire grid UI
     */
    public void refreshGridUI() {
        if (gridPanel != null) {
            gridPanel.refreshGridUI();
        }
    }

    /**
     * Update step highlighting based on current step position - delegates to grid
     * panel
     * 
     * @param oldStep The previous step to un-highlight
     * @param newStep The new step to highlight
     */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Delegate to the grid panel which has direct access to the buttons
        if (gridPanel != null) {
            gridPanel.updateStepHighlighting(drumIndex, oldStep, newStep);
        }
    }

    /**
     * Completely recreate the grid panel to reflect changes in max pattern length
     */
    public void recreateGridPanel() {

        // gridPanel.clearGridButtons();
        // gridPanel.createGridButtons();
        // gridPanel.refreshGridUI();
        // gridPanel.updateStepButtonsForDrum(selectedPadIndex);
        // Refresh layout
        // revalidate();
        // repaint();
    }
}

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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.visualization.Visualizer;
import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.DrumSequencerButton;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
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

    // UI Components
    private final List<DrumSequencerButton> drumButtons = new ArrayList<>();
    private List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private DrumSequencerInfoPanel drumInfoPanel;
    private DrumSequenceNavigationPanel navigationPanel;

    // Core sequencer - manages all sequencing logic
    private DrumSequencer sequencer;

    // UI state
    private int selectedPadIndex = 0; // Default to first drum

    // Parameters panel components
    private JSpinner lastStepSpinner;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;

    // Replace the local DRUM_PAD_COUNT constant with DrumSequencer's version
    private static final int DRUM_PAD_COUNT = DrumSequencer.DRUM_PAD_COUNT;

    // Add these constants referencing DrumSequencer constants
    private static final int MAX_STEPS = DrumSequencer.MAX_STEPS;
    private static final int DEFAULT_PATTERN_LENGTH = DrumSequencer.DEFAULT_PATTERN_LENGTH;
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

        // Create sequence parameters panel
        JPanel sequenceParamsPanel = createSequenceParametersPanel();

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
        // new Visualizer(sequencePanel, gridButtons);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the bottom controls
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        // Add sequence parameters to the center
        sequenceParamsPanel = createSequenceParametersPanel();
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Create and add generate panel
        JPanel generatePanel = createGeneratePanel();
        rightPanel.add(generatePanel);

        // Add swing panel
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

    private JPanel createSequenceParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-MAX_STEPS, default DEFAULT_PATTERN_LENGTH
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(DEFAULT_PATTERN_LENGTH, 1, MAX_STEPS, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        lastStepSpinner.setToolTipText("Set the last step of the pattern (1-" + MAX_STEPS + ")");
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            logger.info("Setting last step to {} for drum {}", lastStep, selectedPadIndex);

            // Use the selected drum index, not a hardcoded value
            sequencer.setPatternLength(selectedPadIndex, lastStep);
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo - Make label skinnier
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel directionLabel = new JLabel("↔️");
        // directionLabel.setPreferredSize(new Dimension(20, 25)); // Make smaller
        // directionPanel.add(directionLabel);

        directionCombo = new JComboBox<>(new String[] { "Forward", "Backward", "Bounce", "Random" });
        directionCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = Direction.FORWARD; // Default

            switch (selectedIndex) {
                case 0 ->
                    direction = Direction.FORWARD;
                case 1 ->
                    direction = Direction.BACKWARD;
                case 2 ->
                    direction = Direction.BOUNCE;
                case 3 ->
                    direction = Direction.RANDOM;
            }

            logger.info("Setting direction to {} for drum {}", direction, selectedPadIndex);

            // Use the selected drum index, not a hardcoded value
            sequencer.setDirection(selectedPadIndex, direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo - Make label skinnier
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // JLabel timingLabel = new JLabel("🕒");
        // timingLabel.setPreferredSize(new Dimension(20, 25)); // Make smaller
        // timingPanel.add(timingLabel);

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing to {} for drum {}", division, selectedPadIndex);

                // Use the selected drum index, not a hardcoded value
                sequencer.setTimingDivision(selectedPadIndex, division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox - Make skinnier
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT)); // Reduce width
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            boolean loop = loopToggleButton.isSelected();
            logger.info("Setting loop to {} for drum {}", loop, selectedPadIndex);

            // Use the selected drum index, not a hardcoded value
            sequencer.setLooping(selectedPadIndex, loop);
        });

        // ADD CLEAR AND GENERATE BUTTONS - Make skinnier
        clearPatternButton = new JButton("🗑️");
        clearPatternButton.setToolTipText("Clear the pattern for this drum");
        clearPatternButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT)); // Reduce width
        clearPatternButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding

        // Create rotation panel for push/pull buttons
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        // rotationPanel.add(new JLabel("Rotate:"));

        // Push forward button
        JButton pushForwardButton = new JButton("⟶");
        pushForwardButton.setToolTipText("Push pattern forward (right)");
        pushForwardButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        pushForwardButton.setMargin(new Insets(2, 2, 2, 2));
        pushForwardButton.addActionListener(e -> {
            sequencer.pushForward();
            refreshGridUI();
        });

        // Pull backward button
        JButton pullBackwardButton = new JButton("⟵");
        pullBackwardButton.setToolTipText("Pull pattern backward (left)");
        pullBackwardButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        pullBackwardButton.setMargin(new Insets(2, 2, 2, 2));
        pullBackwardButton.addActionListener(e -> {
            sequencer.pullBackward();
            refreshGridUI();
        });

        // Add buttons to rotation panel
        rotationPanel.add(pullBackwardButton);
        rotationPanel.add(pushForwardButton);

        // Add all components to panel in a single row
        panel.add(timingPanel);
        panel.add(directionPanel);
        panel.add(loopToggleButton);
        panel.add(lastStepPanel);
        panel.add(rotationPanel);
        panel.add(clearPatternButton);

        return panel;
    }

    private JPanel createSwingControlPanel() {
        JPanel swingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        swingPanel.setBorder(BorderFactory.createTitledBorder("Swing"));

        // Swing toggle - Make skinnier
        JToggleButton swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        swingToggle.setPreferredSize(new Dimension(50, 25)); // Slightly wider for text "On"
        swingToggle.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        swingToggle.addActionListener(e -> {
            sequencer.setSwingEnabled(swingToggle.isSelected());
        });
        swingPanel.add(swingToggle);

        // Swing amount slider
        JSlider swingSlider = new JSlider(JSlider.HORIZONTAL, MIN_SWING, MAX_SWING, sequencer.getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        swingSlider.setPreferredSize(new Dimension(100, 30));

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
     * Create the drum pads panel on the left side
     */
    private JPanel createDrumPadsPanel() {
        // Use GridLayout for perfect vertical alignment with grid cells
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        // Create drum buttons for standard drum kit sounds
        String[] drumNames = {
                "Kick", "Snare", "Closed HH", "Open HH",
                "Tom 1", "Tom 2", "Tom 3", "Crash",
                "Ride", "Rim", "Clap", "Cow",
                "Clave", "Shaker", "Perc 1", "Perc 2"
        };

        // Default MIDI notes for General MIDI drums
        int[] defaultNotes = {
                MIDI_DRUM_NOTE_OFFSET, MIDI_DRUM_NOTE_OFFSET + 2, MIDI_DRUM_NOTE_OFFSET + 6, MIDI_DRUM_NOTE_OFFSET + 10,
                MIDI_DRUM_NOTE_OFFSET + 5, MIDI_DRUM_NOTE_OFFSET + 7, MIDI_DRUM_NOTE_OFFSET + 9,
                MIDI_DRUM_NOTE_OFFSET + 13,
                MIDI_DRUM_NOTE_OFFSET + 15, MIDI_DRUM_NOTE_OFFSET + 1, MIDI_DRUM_NOTE_OFFSET + 3,
                MIDI_DRUM_NOTE_OFFSET + 20,
                MIDI_DRUM_NOTE_OFFSET + 39, MIDI_DRUM_NOTE_OFFSET + 34, MIDI_DRUM_NOTE_OFFSET + 24,
                MIDI_DRUM_NOTE_OFFSET + 25
        };

        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            final int drumIndex = i;

            // Create a Strike object for this drum pad
            Strike strike = new Strike();
            strike.setName(drumNames[i]);
            strike.setRootNote(defaultNotes[i]);
            strike.setLevel(100); // Default velocity

            // Set the strike in the sequencer
            sequencer.setStrike(drumIndex, strike);

            // Create the drum button with proper selection handling
            DrumSequencerButton drumButton = new DrumSequencerButton(drumIndex, sequencer);
            drumButton.setText(drumNames[i]);
            drumButton.setToolTipText("Select " + drumNames[i] + " (Note: " + defaultNotes[i] + ")");

            // THIS IS THE KEY PART - Add action listener for drum selection
            drumButton.addActionListener(e -> selectDrumPad(drumIndex));

            // Add to our tracking list
            drumButtons.add(drumButton);

            // Add to the panel
            panel.add(drumButton);
        }

        return panel;
    }

    /**
     * Handle selection of a drum pad - completely revised to fix display issues
     */
    private void selectDrumPad(int padIndex) {
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

        for (int step = 0; step < DEFAULT_PATTERN_LENGTH; step++) {
            int buttonIndex = (drumIndex * DEFAULT_PATTERN_LENGTH) + step;
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
                            ColorUtils.dustyAmber, 1));
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
        int oldButtonIndex = drumIndex * DEFAULT_PATTERN_LENGTH + oldStep;
        int newButtonIndex = drumIndex * DEFAULT_PATTERN_LENGTH + newStep;

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
    private void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT || triggerButtons.isEmpty()) {
            // Invalid drum index or buttons not initialized yet
            logger.warn("Cannot update step buttons: invalid drum index {} or buttons not initialized", drumIndex);
            return;
        }

        // Get pattern length for this drum
        int patternLength = sequencer.getPatternLength(drumIndex);
        logger.debug("Updating step buttons for drum {} with pattern length {}", drumIndex, patternLength);

        // Update all buttons for this row
        for (int step = 0; step < DEFAULT_PATTERN_LENGTH; step++) {
            int buttonIndex = (drumIndex * DEFAULT_PATTERN_LENGTH) + step;

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
                    button.setBackground(ColorUtils.charcoalGray);
                } else {
                    if (isActive) {
                        button.setBackground(ColorUtils.deepOrange);
                    } else {
                        button.setBackground(ColorUtils.slateGray);
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

    private DrumSequencerGridButton[][] gridButtons;

    /**
     * Create the step grid panel with proper cell visibility
     */
    private JPanel createSequenceGridPanel() {
        // Use consistent cell size with even spacing
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, DEFAULT_PATTERN_LENGTH, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Initialize both storage structures
        triggerButtons = new ArrayList<>(DRUM_PAD_COUNT * DEFAULT_PATTERN_LENGTH); // Pre-size the list
        gridButtons = new DrumSequencerGridButton[DRUM_PAD_COUNT][DEFAULT_PATTERN_LENGTH]; // Initialize the 2D array

        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < DEFAULT_PATTERN_LENGTH; step++) {
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
        fillItem.addActionListener(e -> showFillDialog(drumIndex, step));
        menu.add(fillItem);

        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> clearRow(drumIndex));
        menu.add(clearRowItem);

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
        euclideanItem.addActionListener(e -> showEuclideanDialog(drumIndex));
        menu.add(euclideanItem);

        // Show the menu
        menu.show(component, x, y);
    }

    /**
     * Shows a dialog with Euclidean pattern controls
     * 
     * @param drumIndex The drum index to apply the pattern to
     */
    private void showEuclideanDialog(int drumIndex) {
        // Create dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Euclidean Pattern Generator",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Create panel with border layout
        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the Euclidean pattern panel (compact mode)
        EuclideanPatternPanel patternPanel = new EuclideanPatternPanel(false);

        // Set default values based on current pattern length
        int patternLength = sequencer.getPatternLength(drumIndex);
        patternPanel.getStepsDial().setValue(patternLength);
        patternPanel.getHitsDial().setValue(Math.max(1, patternLength / 4)); // Default to 25% density
        patternPanel.getRotationDial().setValue(0);
        patternPanel.getWidthDial().setValue(0);

        // Add pattern panel to dialog
        dialogPanel.add(patternPanel, BorderLayout.CENTER);

        // Add button panel at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton applyButton = new JButton("Apply Pattern");
        applyButton.addActionListener(e -> {
            // Get the generated Euclidean pattern
            boolean[] euclideanPattern = patternPanel.getPattern();

            // Apply the pattern to the selected drum
            applyEuclideanPattern(drumIndex, euclideanPattern);

            // Close the dialog
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set dialog contents and show
        dialog.setContentPane(dialogPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Applies an Euclidean pattern to the specified drum
     * 
     * @param drumIndex The index of the drum to update
     * @param pattern   The boolean array representing the pattern
     */
    private void applyEuclideanPattern(int drumIndex, boolean[] pattern) {
        if (pattern == null || pattern.length == 0) {
            logger.warn("Cannot apply null or empty Euclidean pattern");
            return;
        }

        try {
            // First clear the existing pattern
            clearRow(drumIndex);

            // Set the pattern length if needed
            int newLength = pattern.length;
            sequencer.setPatternLength(drumIndex, newLength);

            // Set default values for all steps
            for (int step = 0; step < newLength; step++) {

                sequencer.setStepProbability(drumIndex, step, DEFAULT_PROBABILITY);
                sequencer.setStepNudge(drumIndex, step, 0);
                sequencer.setStepDecay(drumIndex, step, DEFAULT_DECAY);
                sequencer.setStepVelocity(drumIndex, step, DEFAULT_VELOCITY);
            }

            // Apply pattern values (activate steps where pattern is true)
            for (int step = 0; step < pattern.length; step++) {
                if (pattern[step]) {
                    // Toggle the step to make it active
                    if (!sequencer.isStepActive(drumIndex, step)) {
                        sequencer.toggleStep(drumIndex, step);
                    }
                }
            }

            // Update the UI to reflect changes
            updateStepButtonsForDrum(drumIndex);
            updateParameterControls();

            logger.info("Applied Euclidean pattern to drum {}, pattern length: {}", drumIndex, pattern.length);
        } catch (Exception e) {
            logger.error("Error applying Euclidean pattern", e);
        }
    }

    /**
     * Show dialog for creating fill patterns
     */
    private void showFillDialog(int drumIndex, int startStep) {
        // Change from boolean modal parameter to ModalityType
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Fill Pattern",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add options for fill pattern
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        // Fill type options
        ButtonGroup group = new ButtonGroup();
        JRadioButton allButton = new JRadioButton("Fill All", true);
        JRadioButton everyOtherButton = new JRadioButton("Every Other Step");
        JRadioButton every4thButton = new JRadioButton("Every 4th Step");
        JRadioButton decayButton = new JRadioButton("Velocity Decay");

        group.add(allButton);
        group.add(everyOtherButton);
        group.add(every4thButton);
        group.add(decayButton);

        optionsPanel.add(allButton);
        optionsPanel.add(everyOtherButton);
        optionsPanel.add(every4thButton);
        optionsPanel.add(decayButton);

        panel.add(optionsPanel, BorderLayout.CENTER);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            // Apply the selected fill pattern
            int patternLength = sequencer.getPatternLength(drumIndex);

            for (int i = startStep; i < patternLength; i++) {
                boolean shouldActivate = false;

                if (allButton.isSelected()) {
                    shouldActivate = true;
                } else if (everyOtherButton.isSelected()) {
                    shouldActivate = ((i - startStep) % 2) == 0;
                } else if (every4thButton.isSelected()) {
                    shouldActivate = ((i - startStep) % 4) == 0;
                } else if (decayButton.isSelected()) {
                    shouldActivate = true;
                    // Apply velocity decay based on distance
                    sequencer.setVelocity(drumIndex,
                            Math.max(DEFAULT_VELOCITY / 2, DEFAULT_VELOCITY - ((i - startStep) * 8)));
                }

                if (shouldActivate) {
                    sequencer.toggleStep(drumIndex, i);
                }
            }

            // Update UI to reflect changes
            updateStepButtonsForDrum(drumIndex);
            dialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Clear all steps in a drum row
     */
    private void clearRow(int drumIndex) {
        for (int step = 0; step < sequencer.getPatternLength(drumIndex); step++) {
            // Deactivate steps by making sure they're not active
            if (sequencer.isStepActive(drumIndex, step)) {
                sequencer.toggleStep(drumIndex, step);
            }
        }

        // Update the UI
        updateStepButtonsForDrum(drumIndex);
        logger.info("Cleared row for drum {}", drumIndex);
    }

    /**
     * Apply a pattern that activates every Nth step
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        int patternLength = sequencer.getPatternLength(drumIndex);

        // Clear existing pattern first
        clearRow(drumIndex);

        // Set every Nth step
        for (int i = 0; i < patternLength; i += n) {
            if (!sequencer.isStepActive(drumIndex, i)) {
                sequencer.toggleStep(drumIndex, i);
            }
        }

        // Update UI
        updateStepButtonsForDrum(drumIndex);
        logger.info("Applied 1/{} pattern to drum {}", n, drumIndex);
    }

    /**
     * Refresh the entire grid UI to match the current sequencer state
     */
    private void refreshGridUI() {
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
                for (int step = 0; step < DEFAULT_PATTERN_LENGTH; step++) { // Just update the visible
                    // DEFAULT_PATTERN_LENGTH steps
                    // Correct index calculation: drumRow * stepsPerRow + stepColumn
                    int buttonIndex = drumIndex * DEFAULT_PATTERN_LENGTH + step;

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

        // Prevent feedback loops during UI updates
        boolean updatingUI = true;
        try {
            // Use getters that take explicit drum index
            int length = sequencer.getPatternLength(selectedPadIndex);
            Direction dir = sequencer.getDirection(selectedPadIndex);
            TimingDivision timing = sequencer.getTimingDivision(selectedPadIndex);
            boolean isLooping = sequencer.isLooping(selectedPadIndex);

            // Update UI components without triggering their change listeners
            lastStepSpinner.setValue(length);

            switch (dir) {
                case FORWARD ->
                    directionCombo.setSelectedIndex(0);
                case BACKWARD ->
                    directionCombo.setSelectedIndex(1);
                case BOUNCE ->
                    directionCombo.setSelectedIndex(2);
                case RANDOM ->
                    directionCombo.setSelectedIndex(3);
            }

            timingCombo.setSelectedItem(timing);
            loopToggleButton.setSelected(isLooping);

            // Don't call revalidate() here - it triggers re-layout
            // Just repaint the components
            lastStepSpinner.repaint();
            directionCombo.repaint();
            timingCombo.repaint();
            loopToggleButton.repaint();

        } finally {
            updatingUI = false;
        }
    }

    /**
     * Sync the UI with the current state of the sequencer
     */
    public void syncUIWithSequencer() {
        // For each drum pad
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < DEFAULT_PATTERN_LENGTH; step++) { // Just update the visible
                                                                        // DEFAULT_PATTERN_LENGTH steps
                // Correct index calculation: drumRow * stepsPerRow + stepColumn
                int buttonIndex = drumIndex * DEFAULT_PATTERN_LENGTH + step;

                if (buttonIndex < triggerButtons.size()) {
                    DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                    button.setToggled(sequencer.isStepActive(drumIndex, step));
                }
            }
        }
    }

    /**
     * Update handlers for pattern generation and clear
     */
    private void setupPatternControls() {
        // Pattern generation
        generatePatternButton.addActionListener(e -> {
            // Get selected density
            int density = (int) densitySpinner.getValue();

            // Generate pattern for current drum
            sequencer.generatePattern(density);

            // Update UI - IMPORTANT: sync the UI after pattern generation
            syncUIWithSequencer();
        });

        // Clear pattern
        clearPatternButton.addActionListener(e -> {
            // Clear pattern
            sequencer.clearPattern();

            // Update UI - IMPORTANT: sync the UI after clearing
            syncUIWithSequencer();
        });
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
                int drumIndex = i / DEFAULT_PATTERN_LENGTH;
                int stepIndex = i % DEFAULT_PATTERN_LENGTH;

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
}

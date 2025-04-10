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

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.DrumSequencerButton;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.DrumSequencerManager;

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
    private int selectedPadIndex = 0;  // Default to first drum

    // Parameters panel components
    private JSpinner lastStepSpinner;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;

    // Number of drum pads and pattern length
    private static final int DRUM_PAD_COUNT = 16; // 16 tracks

    // Debug mode flag
    private boolean debugMode = false;

    // Add this field to the class
    private boolean isPlaying = false;

    // Add this field to the class
    private boolean isSelectingDrumPad = false;

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
                event -> updateStepHighlighting(event.getDrumIndex(), event.getOldStep(), event.getNewStep())
        );

        // Register with the command bus - MAKE SURE THIS IS HERE
        CommandBus.getInstance().register(this);

        // Debug: Print confirmation of registration
        System.out.println("DrumSequencerPanel registered with CommandBus");

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

        // Create top panel to hold navigation, parameters and info
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Create drum info panel (now on right side)
        // drumInfoPanel = new DrumSequencerInfoPanel(sequencer);

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

        // Create main content panel with drum pads on left, grid on right
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));

        // Create drum pads panel on left
        JPanel drumPadsPanel = createDrumPadsPanel();
        contentPanel.add(drumPadsPanel, BorderLayout.WEST);

        // Create step grid panel
        JPanel gridPanel = createStepGridPanel();

        // IMPORTANT: Use only ONE JScrollPane for the grid
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null); // Remove border from scroll pane

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Add content panel to main layout
        add(contentPanel, BorderLayout.CENTER);

        // Select the first drum pad by default
        SwingUtilities.invokeLater(() -> selectDrumPad(0));
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
            logger.info("Setting last step to {} for drum {}", lastStep, selectedPadIndex);

            // Use the selected drum index, not a hardcoded value
            sequencer.setPatternLength(selectedPadIndex, lastStep);
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
        JLabel timingLabel = new JLabel("🕒");
        timingLabel.setPreferredSize(new Dimension(20, 25)); // Make smaller
        timingPanel.add(timingLabel);

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(90, 25));
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
        loopToggleButton.setPreferredSize(new Dimension(40, 25)); // Reduce width
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            boolean loop = loopToggleButton.isSelected();
            logger.info("Setting loop to {} for drum {}", loop, selectedPadIndex);

            // Use the selected drum index, not a hardcoded value
            sequencer.setLooping(selectedPadIndex, loop);
        });

        // Range combo box for pattern generation
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.add(new JLabel("Density:"));

        // Create density spinner
        densitySpinner = new JSpinner(new SpinnerNumberModel(50, 25, 100, 25));
        densitySpinner.setPreferredSize(new Dimension(60, 25));
        rangePanel.add(densitySpinner);

        // ADD CLEAR AND GENERATE BUTTONS - Make skinnier
        clearPatternButton = new JButton("🗑️");
        clearPatternButton.setPreferredSize(new Dimension(40, 25)); // Reduce width
        clearPatternButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding

        generatePatternButton = new JButton("🎲");
        generatePatternButton.setPreferredSize(new Dimension(40, 25)); // Reduce width
        generatePatternButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding

        // Create rotation panel for push/pull buttons
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

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);
        panel.add(loopToggleButton);
        panel.add(rangePanel);
        panel.add(generatePatternButton);
        panel.add(clearPatternButton);
        panel.add(rotationPanel);

        return panel;
    }

    private JPanel createSwingControls() {
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
            36, 38, 42, 46,
            41, 43, 45, 49,
            51, 37, 39, 56,
            75, 70, 60, 61
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
        } finally {
            isSelectingDrumPad = false;
        }
    }

    /**
     * Update appearance of an entire drum row
     */
    private void updateRowAppearance(int drumIndex, boolean isSelected) {
        int patternLength = sequencer.getPatternLength(drumIndex);

        for (int step = 0; step < 16; step++) {
            int buttonIndex = (drumIndex * 16) + step;
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
     * @param oldStep The previous step to un-highlight
     * @param newStep The new step to highlight
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
        int oldButtonIndex = drumIndex * 16 + oldStep;
        int newButtonIndex = drumIndex * 16 + newStep;

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
        for (int step = 0; step < 16; step++) {
            int buttonIndex = (drumIndex * 16) + step;

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

        // Make button square
        button.setPreferredSize(new Dimension(24, 24));

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
    private JPanel createStepGridPanel() {
        // Use consistent cell size with even spacing
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 16, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        triggerButtons = new ArrayList<>(DRUM_PAD_COUNT * 16); // Pre-size the list

        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < 16; step++) {
                DrumSequencerGridButton button = createStepButton(drumIndex, step);

                // IMPORTANT: Set initial state based on sequencer
                boolean isInPattern = step < sequencer.getPatternLength(drumIndex);
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Configure button
                button.setEnabled(isInPattern);  // Use enabled state for in-pattern
                button.setSelected(isActive);
                button.setVisible(true); // Always make buttons visible

                // Add to panel and tracking list
                panel.add(button);
                triggerButtons.add(button);
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
            Object[] options = {"Every 2nd Step", "Every 3rd Step", "Every 4th Step"};
            int choice = javax.swing.JOptionPane.showOptionDialog(
                    this,
                    "Choose pattern type:",
                    "Pattern Generator",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice >= 0) {
                applyPatternEveryN(drumIndex, 2 + choice);
            }
        });
        menu.add(patternItem);

        // Show the menu
        menu.show(component, x, y);
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
                    sequencer.setVelocity(drumIndex, Math.max(40, 100 - ((i - startStep) * 8)));
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
        boolean wasListeningToChanges = true;  // Add field if needed

        try {
            // wasListeningToChanges = disableUIEventListeners();

            // Ensure we refresh ALL drums and ALL steps
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                for (int step = 0; step < 16; step++) {
                    int buttonIndex = drumIndex * 16 + step;

                    if (buttonIndex < triggerButtons.size()) {
                        DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                        if (button != null) {
                            // Get the current state from the sequencer
                            boolean active = sequencer.isStepActive(drumIndex, step);

                            // Force update button state without triggering events
                            button.setToggled(active);
                            button.setHighlighted(false);  // Clear any highlighting
                            button.repaint();  // Force immediate repaint
                        }
                    }
                }

                // Update the drum row's appearance
                updateRowAppearance(drumIndex, drumIndex == selectedPadIndex);
            }
        } finally {
            // Re-enable listeners if needed
            // if (wasListeningToChanges) {
            //    enableUIEventListeners();
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
            for (int step = 0; step < 16; step++) { // Just update the visible 16 steps
                // Correct index calculation: drumRow * stepsPerRow + stepColumn
                int buttonIndex = drumIndex * 16 + step;

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
                int drumIndex = i / 16;
                int stepIndex = i % 16;

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
}

package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
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
import com.angrysurfer.core.sequencer.DrumStepUpdateEvent;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;

/**
 * A sequencer panel with X0X-style step sequencing capabilities.
 * This is the UI component for the DrumSequencer.
 */
public class DrumSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumEffectsSequencerPanel.class);

    // UI Components
    private final List<DrumSequencerButton> drumButtons = new ArrayList<>();
    private List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> decayDials = new ArrayList<>();
    private DrumSequencerInfoPanel drumInfoPanel;
    
    // Core sequencer - manages all sequencing logic
    private DrumSequencer sequencer;

    // UI state
    private int selectedPadIndex = 0;  // Default to first drum
    
    // Parameters panel components
    private JSpinner lastStepSpinner;
    private JCheckBox loopCheckbox;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JComboBox<String> rangeCombo; // For generating patterns
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;
    private JButton prevQuadrantButton;
    private JButton nextQuadrantButton;
    private JLabel currentQuadrantLabel;
    private JRadioButton steps16Radio;
    private JRadioButton steps32Radio;
    private JRadioButton steps48Radio;
    private JRadioButton steps64Radio;
    private JRadioButton followModeRadio;
    private JRadioButton massiveModeRadio;
    
    // Number of drum pads and pattern length
    private static final int DRUM_PAD_COUNT = 16; // 16 tracks
    
    // Flag to prevent listener feedback loops
    private boolean listenersEnabled = true;
    private boolean updatingUI = false;

    // Playback fields
    private ScheduledExecutorService tickExecutor;
    private int tickCount = 0;
    private static final int TICKS_PER_SECOND = 96; // Standard MIDI timing

    // Debug mode flag
    private boolean debugMode = false;

    // Add these fields to track Euclidean pattern state
    private int euclideanStartDrum = -1;
    private int euclideanStartStep = -1;
    private boolean isDragging = false;
    private boolean[] tempEuclideanPattern = null;

    // Quadrant and view mode fields
    private static final int STEPS_PER_QUADRANT = 16;
    private int currentQuadrant = 0; // 0-based (0-3 for quadrants 1-4)
    private int maxPatternLength = 16; // Default to 16 steps

    private enum ViewMode { FOLLOW, MASSIVE }
    private ViewMode currentViewMode = ViewMode.FOLLOW;

    // Store the grid panel for view mode switching
    private JPanel gridPanel;

    /**
     * Create a new DrumSequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        
        // Create the sequencer
        sequencer = new DrumSequencer();
        
        // Set the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);
        
        // Set step update listener for highlighting
        sequencer.setStepUpdateListener(event -> {
            updateStepHighlighting(event.getDrumIndex(), event.getOldStep(), event.getNewStep());
        });
        
        // Register with the command bus - MAKE SURE THIS IS HERE
        CommandBus.getInstance().register(this);
        
        // Debug: Print confirmation of registration
        System.out.println("DrumSequencerPanel registered with CommandBus");
        
        // Initialize UI components
        initialize();
    }

    /**
     * Initialize the UI components
     */
    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create top panel to hold both info and parameters
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Create drum info panel
        drumInfoPanel = new DrumSequencerInfoPanel(sequencer);
        
        // Add sequence parameters panel at the top
        JPanel sequenceParamsPanel = createSequenceParametersPanel();
        
        // Add both to the top panel
        topPanel.add(drumInfoPanel, BorderLayout.WEST);
        topPanel.add(sequenceParamsPanel, BorderLayout.CENTER);
        
        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);
        
        // Create panel for drum pads and steps
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create drum pad buttons panel on the left
        JPanel drumPadsPanel = createDrumPadsPanel();
        mainPanel.add(drumPadsPanel, BorderLayout.WEST);
        
        // Create step sequencer grid in the center
        JPanel stepGridPanel = createStepGridPanel();
        mainPanel.add(stepGridPanel, BorderLayout.CENTER);
        
        // Add to scroll pane
        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        
        // Setup pattern controls
        setupPatternControls();
        
        // Initialize quadrant navigation controls
        updateQuadrantControls();
        
        // Select the first drum by default
        if (sequencer != null) {
            selectDrumPad(0);
        }
    }

    /**
     * Create panel for sequence parameters (last step, loop, etc.)
     */
    private JPanel createSequenceParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new BorderLayout());
        
        // Main parameters row
        JPanel mainControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // === Add existing controls to mainControlsPanel ===
        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last:"));
        
        // Create spinner model with range 1-64, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 64, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(50, 25));
        lastStepSpinner.addChangeListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            int lastStep = (Integer) lastStepSpinner.getValue();
            logger.info("Setting last step to {} for drum {}", lastStep, selectedPadIndex);
            
            sequencer.setPatternLength(selectedPadIndex, lastStep);
            updatePatternLengthControls(); // Update quadrant visibility based on pattern length
        });
        lastStepPanel.add(lastStepSpinner);
        mainControlsPanel.add(lastStepPanel);
        
        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Dir:"));
        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
        directionCombo.addActionListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = Direction.FORWARD; // Default
            
            switch (selectedIndex) {
                case 0 -> direction = Direction.FORWARD;
                case 1 -> direction = Direction.BACKWARD;
                case 2 -> direction = Direction.BOUNCE;
                case 3 -> direction = Direction.RANDOM;
            }
            
            logger.info("Setting direction to {} for drum {}", direction, selectedPadIndex);
            
            sequencer.setDirection(selectedPadIndex, direction);
        });
        directionPanel.add(directionCombo);
        mainControlsPanel.add(directionPanel);
        
        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));
        
        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing to {} for drum {}", division, selectedPadIndex);
                
                sequencer.setTimingDivision(selectedPadIndex, division);
            }
        });
        timingPanel.add(timingCombo);
        mainControlsPanel.add(timingPanel);
        
        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            boolean loop = loopCheckbox.isSelected();
            logger.info("Setting loop to {} for drum {}", loop, selectedPadIndex);
            
            sequencer.setLooping(selectedPadIndex, loop);
        });
        mainControlsPanel.add(loopCheckbox);
        
        // Range combo box for pattern generation
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.add(new JLabel("Density:"));
        
        // Create density spinner
        densitySpinner = new JSpinner(new SpinnerNumberModel(50, 25, 100, 25));
        densitySpinner.setPreferredSize(new Dimension(60, 25));
        rangePanel.add(densitySpinner);
        mainControlsPanel.add(rangePanel);
        
        // ADD CLEAR AND GENERATE BUTTONS
        clearPatternButton = new JButton("Clear");
        generatePatternButton = new JButton("Generate");
        mainControlsPanel.add(clearPatternButton);
        mainControlsPanel.add(generatePatternButton);
        
        // ADD MIXER BUTTON
        JButton mixButton = new JButton("Mix...");
        mixButton.addActionListener(e -> {
            // Show the mixer dialog
            StrikeMixerPanel.showDialog(this, sequencer);
        });
        mainControlsPanel.add(mixButton);  // Add the Mix button to the parameters panel
        
        // === Add quadrant navigation controls ===
        JPanel quadrantPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        prevQuadrantButton = new JButton("◀");
        prevQuadrantButton.setToolTipText("Show previous 16 steps");
        prevQuadrantButton.addActionListener(e -> navigateQuadrant(-1));
        
        currentQuadrantLabel = new JLabel("1 / 1");
        currentQuadrantLabel.setPreferredSize(new Dimension(40, 25));
        currentQuadrantLabel.setHorizontalAlignment(JLabel.CENTER);
        
        nextQuadrantButton = new JButton("▶");
        nextQuadrantButton.setToolTipText("Show next 16 steps");
        nextQuadrantButton.addActionListener(e -> navigateQuadrant(1));
        
        quadrantPanel.add(prevQuadrantButton);
        quadrantPanel.add(currentQuadrantLabel);
        quadrantPanel.add(nextQuadrantButton);
        mainControlsPanel.add(quadrantPanel);
        
        // Create a bottom row for the radio buttons and view mode
        JPanel patternControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Pattern length radio buttons
        JPanel patternLengthPanel = new JPanel();
        patternLengthPanel.setBorder(BorderFactory.createTitledBorder("Pattern Length"));
        patternLengthPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        ButtonGroup patternLengthGroup = new ButtonGroup();
        
        steps16Radio = new JRadioButton("16");
        steps16Radio.setSelected(true);
        steps32Radio = new JRadioButton("32");
        steps48Radio = new JRadioButton("48");
        steps64Radio = new JRadioButton("64");
        
        steps16Radio.addActionListener(e -> setMaxPatternLength(16));
        steps32Radio.addActionListener(e -> setMaxPatternLength(32));
        steps48Radio.addActionListener(e -> setMaxPatternLength(48));
        steps64Radio.addActionListener(e -> setMaxPatternLength(64));
        
        patternLengthGroup.add(steps16Radio);
        patternLengthGroup.add(steps32Radio);
        patternLengthGroup.add(steps48Radio);
        patternLengthGroup.add(steps64Radio);
        
        patternLengthPanel.add(steps16Radio);
        patternLengthPanel.add(steps32Radio);
        patternLengthPanel.add(steps48Radio);
        patternLengthPanel.add(steps64Radio);
        
        patternControlsPanel.add(patternLengthPanel);
        
        // View mode selector
        JPanel viewModePanel = new JPanel();
        viewModePanel.setBorder(BorderFactory.createTitledBorder("View Mode"));
        viewModePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        ButtonGroup viewModeGroup = new ButtonGroup();
        followModeRadio = new JRadioButton("Follow");
        followModeRadio.setSelected(true);
        massiveModeRadio = new JRadioButton("Massive");
        
        followModeRadio.addActionListener(e -> setViewMode(ViewMode.FOLLOW));
        massiveModeRadio.addActionListener(e -> setViewMode(ViewMode.MASSIVE));
        
        viewModeGroup.add(followModeRadio);
        viewModeGroup.add(massiveModeRadio);
        
        viewModePanel.add(followModeRadio);
        viewModePanel.add(massiveModeRadio);
        
        patternControlsPanel.add(viewModePanel);
        
        // Add the buttons to the panel structure
        panel.add(mainControlsPanel, BorderLayout.NORTH);
        panel.add(patternControlsPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Create the drum pads panel on the left side
     */
    private JPanel createDrumPadsPanel() {
        // Use GridLayout for perfect vertical alignment with grid cells
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        // panel.setBackground(new Color(40, 40, 40)); // Match grid background
        
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
            // Create a Strike object for this drum pad with velocity and level settings
            Strike strike = new Strike();
            strike.setRootNote(defaultNotes[i]);
            strike.setMinVelocity(80);  
            strike.setMaxVelocity(127); 
            strike.setLevel(100);      
            strike.setName(drumNames[i]);
            
            sequencer.setStrike(i, strike);
            
            // Create the button with flat, rounded style
            DrumSequencerButton button = new DrumSequencerButton(drumIndex, sequencer);
            button.setDrumName(drumNames[i]);
            
            // Add the button directly to the panel for perfect alignment
            panel.add(button);
            
            // Add to tracked buttons list
            drumButtons.add(button);
        }
        
        return panel;
    }

    /**
     * Create the step grid panel with proper cell visibility
     */
    private JPanel createStepGridPanel() {
        // Create the initial grid panel as the FOLLOW mode with quadrant 0
        gridPanel = createQuadrantGridPanel(0);
        return gridPanel;
    }

    /**
     * Create a step button with proper behavior
     * @return The created button
     */
    private DrumSequencerGridButton createStepButton(int drumIndex, int step) {
        final int finalDrumIndex = drumIndex;
        final int finalStep = step;

        DrumSequencerGridButton button = new DrumSequencerGridButton();
        button.setToolTipText("Step " + (step + 1));
        button.setToggleable(true);

        // Create popup menu for right-click
        JPopupMenu popupMenu = new JPopupMenu();
        
        // Add "Fill..." menu item
        JMenuItem fillItem = new JMenuItem("Fill...");
        fillItem.addActionListener(e -> {
            showFillDialog(finalDrumIndex, finalStep);
        });
        popupMenu.add(fillItem);
        
        // Add "Clear Row" menu item
        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> {
            clearRow(finalDrumIndex);
        });
        popupMenu.add(clearRowItem);
        
        // Add separator and more pattern options
        popupMenu.addSeparator();
        
        // JMenuItem everyItem = new JMenuItem("Every Step");
        // everyItem.addActionListener(e -> {
        //     applyPatternEveryN(finalDrumIndex, 1);
        // });
        // popupMenu.add(everyItem);

        // Add "Every 2 Steps" pattern
        JMenuItem every2Item = new JMenuItem("Every 2 Steps");
        every2Item.addActionListener(e -> {
            applyPatternEveryN(finalDrumIndex, 2);
        });
        popupMenu.add(every2Item);
        
        JMenuItem every3Item = new JMenuItem("Every 3 Steps");
        every3Item.addActionListener(e -> {
            applyPatternEveryN(finalDrumIndex, 3);
        });
        popupMenu.add(every3Item);
 
        // Add "Every 4 Steps" pattern
        JMenuItem every4Item = new JMenuItem("Every 4 Steps");
        every4Item.addActionListener(e -> {
            applyPatternEveryN(finalDrumIndex, 4);
        });
        popupMenu.add(every4Item);
        
        // Mouse adapter for left/right click handling
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Left click toggles the cell
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    sequencer.toggleStep(finalDrumIndex, finalStep);
                    button.setToggled(sequencer.isStepActive(finalDrumIndex, finalStep));
                }
                // Right click shows popup menu
                else if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    popupMenu.show(button, e.getX(), e.getY());
                }
            }
        });

        // Set initial state
        button.setToggled(sequencer.isStepActive(drumIndex, step));
        
        // Update appearance based on pattern length
        updateStepButtonAppearance(button, drumIndex, step);
        
        // Add to our list of buttons
        triggerButtons.add(button);
        
        return button;
    }

    /**
     * Show dialog for filling pattern from a starting point
     */
    private void showFillDialog(int drumIndex, int startStep) {
        // Create a dialog with pattern options
        JDialog fillDialog = new JDialog();
        fillDialog.setTitle("Fill Pattern");
        fillDialog.setLayout(new BorderLayout());
        fillDialog.setSize(300, 220);
        fillDialog.setModal(true);
        fillDialog.setLocationRelativeTo(this);
        
        // Create a panel for pattern types
        JPanel patternPanel = new JPanel(new GridLayout(0, 1));
        patternPanel.setBorder(BorderFactory.createTitledBorder("Pattern Type"));
        
        // Create radio buttons for pattern types
        ButtonGroup patternGroup = new ButtonGroup();
        JRadioButton everyStepRadio = new JRadioButton("Every Step");
        JRadioButton everyNStepsRadio = new JRadioButton("Every N Steps");
        JRadioButton euclideanRadio = new JRadioButton("Euclidean", true);
        
        patternGroup.add(everyStepRadio);
        patternGroup.add(everyNStepsRadio);
        patternGroup.add(euclideanRadio);
        
        patternPanel.add(everyStepRadio);
        patternPanel.add(everyNStepsRadio);
        patternPanel.add(euclideanRadio);
        
        // Parameter panel for inputs
        JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paramPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // N-step spinner
        JLabel stepsLabel = new JLabel("Steps:");
        SpinnerNumberModel stepsModel = new SpinnerNumberModel(2, 1, 16, 1);
        JSpinner stepsSpinner = new JSpinner(stepsModel);
        stepsSpinner.setEnabled(false);
        
        // Density spinner for Euclidean
        JLabel densityLabel = new JLabel("Density:");
        SpinnerNumberModel densityModel = new SpinnerNumberModel(4, 1, 16, 1);
        JSpinner densitySpinner = new JSpinner(densityModel);
        
        paramPanel.add(stepsLabel);
        paramPanel.add(stepsSpinner);
        paramPanel.add(Box.createHorizontalStrut(10));
        paramPanel.add(densityLabel);
        paramPanel.add(densitySpinner);
        
        // Enable/disable appropriate controls based on selection
        everyNStepsRadio.addActionListener(e -> {
            stepsSpinner.setEnabled(true);
            densitySpinner.setEnabled(false);
        });
        
        everyStepRadio.addActionListener(e -> {
            stepsSpinner.setEnabled(false);
            densitySpinner.setEnabled(false);
        });
        
        euclideanRadio.addActionListener(e -> {
            stepsSpinner.setEnabled(false);
            densitySpinner.setEnabled(true);
        });
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        
        // Add action listeners
        applyButton.addActionListener(e -> {
            int patternLength = sequencer.getPatternLength(drumIndex);
            
            if (everyStepRadio.isSelected()) {
                // Fill every step from startStep to end
                fillEveryStep(drumIndex, startStep, patternLength - 1);
            } 
            else if (everyNStepsRadio.isSelected()) {
                // Fill every N steps
                int n = (Integer) stepsSpinner.getValue();
                fillEveryNSteps(drumIndex, startStep, patternLength - 1, n);
            }
            else {
                // Euclidean pattern
                int pulses = (Integer) densitySpinner.getValue();
                fillEuclideanPattern(drumIndex, startStep, patternLength, pulses);
            }
            
            fillDialog.dispose();
        });
        
        cancelButton.addActionListener(e -> fillDialog.dispose());
        
        // Add panels to dialog
        fillDialog.add(patternPanel, BorderLayout.NORTH);
        fillDialog.add(paramPanel, BorderLayout.CENTER);
        fillDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        fillDialog.setVisible(true);
    }

    /**
     * Fill pattern with every step active
     */
    private void fillEveryStep(int drumIndex, int startStep, int endStep) {
        for (int step = startStep; step <= endStep; step++) {
            setStepActive(drumIndex, step, true);
        }
    }

    /**
     * Fill pattern with every N steps active
     */
    private void fillEveryNSteps(int drumIndex, int startStep, int endStep, int n) {
        for (int step = startStep; step <= endStep; step++) {
            if ((step - startStep) % n == 0) {
                setStepActive(drumIndex, step, true);
            } else {
                setStepActive(drumIndex, step, false);
            }
        }
    }

    /**
     * Fill with an Euclidean pattern
     */
    private void fillEuclideanPattern(int drumIndex, int startStep, int patternLength, int pulses) {
        boolean[] pattern = generateEuclideanPattern(startStep, patternLength, pulses);
        
        // Apply the pattern
        for (int step = 0; step < patternLength; step++) {
            setStepActive(drumIndex, step, pattern[step]);
        }
    }

    /**
     * Helper to set a step active and update UI
     */
    private void setStepActive(int drumIndex, int step, boolean active) {
        // Update sequencer
        if (sequencer.isStepActive(drumIndex, step) != active) {
            sequencer.toggleStep(drumIndex, step);
        }
        
        // Update UI
        int buttonIndex = drumIndex * 16 + step;
        if (buttonIndex < triggerButtons.size()) {
            DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
            button.setToggled(active);
        }
    }

    /**
     * Clear all steps in a row
     */
    private void clearRow(int drumIndex) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        // Clear all steps
        for (int step = 0; step < patternLength; step++) {
            if (sequencer.isStepActive(drumIndex, step)) {
                setStepActive(drumIndex, step, false);
            }
        }
    }

    /**
     * Apply a pattern with every N steps active
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        // Clear row first
        clearRow(drumIndex);
        
        // Set every Nth step
        for (int step = 0; step < patternLength; step++) {
            if (step % n == 0) {
                setStepActive(drumIndex, step, true);
            }
        }
    }

    /**
     * Update a step button's appearance based on whether it's within the pattern length
     */
    private void updateStepButtonAppearance(DrumSequencerGridButton button, int drumIndex, int step) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        if (step >= patternLength) {
            // Beyond pattern length - make it look disabled
            button.setVisible(false);
            button.setOpaque(false);
        } else {
            // Within pattern length - normal appearance
            button.setEnabled(true);
            button.setOpaque(true);
        }
    }

    /**
     * Update all step buttons for a specific drum
     */
    private void updateStepButtonsForDrum(int drumIndex) {
        for (int step = 0; step < 64; step++) {
            int buttonIndex = drumIndex * 64 + step;
            if (buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                updateStepButtonAppearance(button, drumIndex, step);
            }
        }
    }

    /**
     * Handle selection of a drum pad
     * 
     * @param padIndex The index of the selected pad
     */
    private void selectDrumPad(int padIndex) {
        // Deselect previous pad
        if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
            drumButtons.get(selectedPadIndex).setSelected(false);
        }
        
        // Select new pad
        selectedPadIndex = padIndex;
        if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
            drumButtons.get(selectedPadIndex).setSelected(true);
        }
        
        // Update sequencer state
        sequencer.setSelectedPadIndex(padIndex);
    }

    /**
     * Update step highlighting during playback
     * Also handles automatic quadrant navigation in FOLLOW mode
     */
    private void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // In FOLLOW mode, automatically navigate to the quadrant containing the new step
        if (currentViewMode == ViewMode.FOLLOW) {
            int targetQuadrant = newStep / STEPS_PER_QUADRANT;
            if (targetQuadrant != currentQuadrant) {
                currentQuadrant = targetQuadrant;
                updateQuadrantControls();
                updateGridView();
                return; // Grid was rebuilt, highlighting will be handled there
            }
        }

        // Otherwise update highlights as usual
        // We need to map from absolute step positions to visible positions
        int visibleOldStep, visibleNewStep;
        
        if (currentViewMode == ViewMode.FOLLOW) {
            // In FOLLOW mode, subtract the quadrant offset
            visibleOldStep = oldStep - (currentQuadrant * STEPS_PER_QUADRANT);
            visibleNewStep = newStep - (currentQuadrant * STEPS_PER_QUADRANT);
        } else {
            // In MASSIVE mode, use absolute positions
            visibleOldStep = oldStep;
            visibleNewStep = newStep;
        }
        
        // Only update visible steps
        if (visibleOldStep >= 0 && visibleOldStep < triggerButtons.size() / DRUM_PAD_COUNT) {
            int oldButtonIndex = drumIndex * (triggerButtons.size() / DRUM_PAD_COUNT) + visibleOldStep;
            if (oldButtonIndex < triggerButtons.size()) {
                triggerButtons.get(oldButtonIndex).setHighlighted(false);
            }
        }
        
        if (visibleNewStep >= 0 && visibleNewStep < triggerButtons.size() / DRUM_PAD_COUNT) {
            int newButtonIndex = drumIndex * (triggerButtons.size() / DRUM_PAD_COUNT) + visibleNewStep;
            if (newButtonIndex < triggerButtons.size()) {
                triggerButtons.get(newButtonIndex).setHighlighted(true);
            }
        }
    }

    /**
     * Start the sequencer playback
     */
    public void startPlayback() {
        if (tickExecutor != null && !tickExecutor.isShutdown()) {
            return; // Already running
        }
        
        // Reset tick count
        tickCount = 0;
        
        // Create a new executor
        tickExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule tick generator (96 ticks per second)
        int tickIntervalMs = 1000 / TICKS_PER_SECOND;
        
        tickExecutor.scheduleAtFixedRate(() -> {
            tickCount++;
            sequencer.processTick(tickCount);
        }, 0, tickIntervalMs, TimeUnit.MILLISECONDS);
        
        // Start the sequencer
        sequencer.play();
    }

    /**
     * Stop the sequencer playback
     */
    public void stopPlayback() {
        if (tickExecutor != null) {
            tickExecutor.shutdown();
            tickExecutor = null;
        }
        
        sequencer.stop();
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.SEQUENCER_STEP_UPDATE -> {
                if (action.getData() instanceof DrumStepUpdateEvent evt) {
                    // If in FOLLOW mode, check if we need to switch quadrants
                    if (currentViewMode == ViewMode.FOLLOW) {
                        int targetQuadrant = evt.getNewStep() / STEPS_PER_QUADRANT;
                        if (targetQuadrant != currentQuadrant) {
                            currentQuadrant = targetQuadrant;
                            updateQuadrantControls();
                            SwingUtilities.invokeLater(this::updateGridView);
                        }
                    }
                }
            }
            case Commands.TRANSPORT_START -> {
                // No need for startPlayback() since we're using the TimingBus
                
                // Update UI to show initial state
                syncUIWithSequencer();
            }
            
            case Commands.TRANSPORT_STOP -> {
                // Clear all highlighting
                for (DrumSequencerGridButton button : triggerButtons) {
                    button.setHighlighted(false);
                    button.repaint();
                }
            }
            
            case Commands.PATTERN_PARAMS_CHANGED -> {
                logger.debug("Pattern parameters changed");
                // Always do a full UI sync when pattern parameters change
                syncUIWithSequencer();
            }
            
            case Commands.DRUM_PAD_SELECTED -> {
                // Update the UI for the changed selection
                if (action.getData() instanceof DrumPadSelectionEvent event) {
                    // Update the selectedPadIndex in the panel
                    selectedPadIndex = event.getNewSelection();
                    
                    logger.info("Drum selected: {} -> {}", event.getOldSelection(), selectedPadIndex);
                    
                    if (selectedPadIndex >= 0 && selectedPadIndex < DRUM_PAD_COUNT) {
                        // Update the selection state of all drum buttons with debug output
                        for (int i = 0; i < drumButtons.size(); i++) {
                            DrumSequencerButton button = (DrumSequencerButton) drumButtons.get(i);
                            boolean shouldBeSelected = (i == selectedPadIndex);
                            
                            // Debug which button is being selected
                            logger.info("Setting button {} selected={}", i, shouldBeSelected);
                            
                            // Force repaint to ensure visual update
                            button.setSelected(shouldBeSelected);
                            button.repaint();
                        }
                        
                        // Add visual indicator elsewhere in UI
                        if (drumInfoPanel != null) {
                            drumInfoPanel.setBorder(BorderFactory.createTitledBorder(
                                BorderFactory.createLineBorder(Color.ORANGE, 2),
                                "Drum: " + (selectedPadIndex + 1)
                             ));
                            
                            // Update the info panel content
                            drumInfoPanel.updateForDrum(selectedPadIndex);
                            drumInfoPanel.repaint();
                        }
                        
                        // Update UI components with explicit repaints
                        updateParameterControls();
                        
                        // Force repaint of controls panel
                        lastStepSpinner.repaint();
                        directionCombo.repaint();
                        timingCombo.repaint();
                        loopCheckbox.repaint();
                    }
                }
            }
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
        updatingUI = true;
        try {
            // Use getters that take explicit drum index
            int length = sequencer.getPatternLength(selectedPadIndex);
            Direction dir = sequencer.getDirection(selectedPadIndex);
            TimingDivision timing = sequencer.getTimingDivision(selectedPadIndex);
            boolean isLooping = sequencer.isLooping(selectedPadIndex);
            
            // Update UI components without triggering their change listeners
            lastStepSpinner.setValue(length);
            
            switch(dir) {
                case FORWARD -> directionCombo.setSelectedIndex(0);
                case BACKWARD -> directionCombo.setSelectedIndex(1);
                case BOUNCE -> directionCombo.setSelectedIndex(2);
                case RANDOM -> directionCombo.setSelectedIndex(3);
            }
            
            timingCombo.setSelectedItem(timing); 
            loopCheckbox.setSelected(isLooping);
            
            // Don't call revalidate() here - it triggers re-layout
            // Just repaint the components
            lastStepSpinner.repaint();
            directionCombo.repaint();
            timingCombo.repaint();
            loopCheckbox.repaint();
            
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
            int density = (int)densitySpinner.getValue();
            
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
     * Generate a Euclidean rhythm pattern
     * 
     * @param startPosition Position to start the pattern (rotation)
     * @param steps Total number of steps in the pattern
     * @param pulses Number of active beats to distribute
     * @return Boolean array with the pattern
     */
    private boolean[] generateEuclideanPattern(int startPosition, int steps, int pulses) {
        boolean[] pattern = new boolean[steps];
        
        // Handle edge cases
        if (pulses <= 0) return pattern;
        if (pulses >= steps) {
            Arrays.fill(pattern, true);
            return pattern;
        }
        
        // Generate the pattern using Bresenham's line algorithm
        // which effectively creates evenly spaced pulses
        int error = steps - pulses;
        for (int i = 0; i < steps; i++) {
            if (error < 0) {
                pattern[(i + startPosition) % steps] = true;
                error += steps - pulses;
            } else {
                pattern[(i + startPosition) % steps] = false;
                error -= pulses;
            }
        }
        
        return pattern;
    }

    /**
     * Update display of the Euclidean pattern as it's being created
     */
    private void updateEuclideanPatternDisplay(int drumIndex, boolean[] pattern) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        for (int step = 0; step < patternLength; step++) {
            int buttonIndex = drumIndex * 16 + step;
            if (buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                button.setTemporaryState(pattern[step]);
            }
        }
    }

    /**
     * Apply the final Euclidean pattern to the sequencer
     */
    private void applyEuclideanPattern(int drumIndex, boolean[] pattern) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        // First clear existing pattern
        for (int step = 0; step < patternLength; step++) {
            if (sequencer.isStepActive(drumIndex, step)) {
                sequencer.toggleStep(drumIndex, step);
            }
        }
        
        // Set new pattern
        for (int step = 0; step < patternLength; step++) {
            if (pattern[step]) {
                sequencer.toggleStep(drumIndex, step);
            }
        }
        
        // Update UI
        for (int step = 0; step < patternLength; step++) {
            int buttonIndex = drumIndex * 16 + step;
            if (buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                button.setToggled(sequencer.isStepActive(drumIndex, step));
                button.clearTemporaryState();
            }
        }
    }

    /**
     * Navigate to a different quadrant
     * 
     * @param delta The direction to move (-1 for previous, +1 for next)
     */
    private void navigateQuadrant(int delta) {
        int maxQuadrants = maxPatternLength / STEPS_PER_QUADRANT;
        int newQuadrant = currentQuadrant + delta;
        
        if (newQuadrant >= 0 && newQuadrant < maxQuadrants) {
            currentQuadrant = newQuadrant;
            updateGridView();
            updateQuadrantControls();
        }
    }

    /**
     * Update the quadrant navigation controls based on current state
     */
    private void updateQuadrantControls() {
        int maxQuadrants = maxPatternLength / STEPS_PER_QUADRANT;
        
        // Update quadrant label
        currentQuadrantLabel.setText((currentQuadrant + 1) + " / " + maxQuadrants);
        
        // Enable/disable navigation buttons
        prevQuadrantButton.setEnabled(currentQuadrant > 0);
        nextQuadrantButton.setEnabled(currentQuadrant < maxQuadrants - 1);
    }

    /**
     * Set the maximum pattern length
     * 
     * @param steps The new maximum pattern length (16, 32, 48, or 64)
     */
    private void setMaxPatternLength(int steps) {
        if (steps != 16 && steps != 32 && steps != 48 && steps != 64) {
            throw new IllegalArgumentException("Pattern length must be 16, 32, 48, or 64");
        }
        
        if (maxPatternLength == steps) return; // No change
        
        // Store previous length for potential UI updates
        int previousLength = maxPatternLength;
        maxPatternLength = steps;
        
        // If current quadrant is now out of bounds, adjust it
        int maxQuadrants = maxPatternLength / STEPS_PER_QUADRANT;
        if (currentQuadrant >= maxQuadrants) {
            currentQuadrant = maxQuadrants - 1;
        }
        
        // Update UI
        updateQuadrantControls();
        
        // Force grid rebuild since dimensions changed
        updateGridView();
        
        // Adjust last step spinner's maximum
        ((SpinnerNumberModel)lastStepSpinner.getModel()).setMaximum(maxPatternLength);
        
        // If we're in Massive mode, we might need to adjust pattern lengths for
        // individual drums to match the new maximum
        if (currentViewMode == ViewMode.MASSIVE) {
            // Optionally adjust pattern lengths for all drums
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                int currentLength = sequencer.getPatternLength(drumIndex);
                if (currentLength > maxPatternLength) {
                    sequencer.setPatternLength(drumIndex, maxPatternLength);
                }
            }
        }
        
        logger.info("Maximum pattern length set to {}", maxPatternLength);
    }

    /**
     * Set the view mode
     * 
     * @param mode The new view mode (FOLLOW or MASSIVE)
     */
    private void setViewMode(ViewMode mode) {
        if (currentViewMode == mode) return; // No change
        
        currentViewMode = mode;
        
        // Update grid display
        updateGridView();
        
        // Enable/disable quadrant navigation based on view mode
        boolean enableNavigation = (currentViewMode == ViewMode.FOLLOW);
        prevQuadrantButton.setEnabled(enableNavigation && currentQuadrant > 0);
        nextQuadrantButton.setEnabled(enableNavigation && 
                                     currentQuadrant < (maxPatternLength / STEPS_PER_QUADRANT) - 1);
        currentQuadrantLabel.setEnabled(enableNavigation);
        
        logger.info("View mode set to {}", currentViewMode);
    }

    /**
     * Update the grid view based on current view mode and quadrant
     */
    private void updateGridView() {
        // Remove the existing grid panel from the container
        Container parent = gridPanel.getParent();
        parent.remove(gridPanel);
        
        // Create a new grid panel based on the current view mode
        if (currentViewMode == ViewMode.FOLLOW) {
            gridPanel = createQuadrantGridPanel(currentQuadrant);
        } else {
            gridPanel = createMassiveGridPanel();
        }
        
        // Add the new grid panel to the container
        parent.add(gridPanel, BorderLayout.CENTER);
        
        // Refresh the UI
        parent.revalidate();
        parent.repaint();
        
        // Ensure all buttons reflect the correct state
        fullSyncGridWithSequencer();
    }

    /**
     * Create a grid panel showing just one quadrant (16 steps)
     * 
     * @param quadrant The quadrant index to show (0-3)
     * @return The new grid panel
     */
    private JPanel createQuadrantGridPanel(int quadrant) {
        // Use consistent cell size with even spacing
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, STEPS_PER_QUADRANT, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        triggerButtons = new ArrayList<>();
        
        // Calculate the step offset for this quadrant
        int stepOffset = quadrant * STEPS_PER_QUADRANT;
        
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < STEPS_PER_QUADRANT; step++) {
                int absoluteStep = stepOffset + step;
                DrumSequencerGridButton button = createStepButton(drumIndex, absoluteStep);
                panel.add(button);
            }
        }
        
        return panel;
    }

    /**
     * Create a grid panel showing all steps (up to 64)
     * 
     * @return The new grid panel
     */
    private JPanel createMassiveGridPanel() {
        // Calculate number of columns needed
        int columns = maxPatternLength;
        
        // Create panel with smaller cells
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, columns, 1, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Reset trigger buttons list - critical for proper state tracking
        triggerButtons = new ArrayList<>();
        
        // Create all buttons with proper state
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < maxPatternLength; step++) {
                // Create button with correct state from sequencer
                DrumSequencerGridButton button = createStepButton(drumIndex, step);
                
                // Apply special massive mode styling
                int cellSize = calculateCellSize(maxPatternLength);
                button.setPreferredSize(new Dimension(cellSize, cellSize));
                button.setMinimumSize(new Dimension(cellSize, cellSize));
                button.setMaximumSize(new Dimension(cellSize, cellSize));
                
                // Visual indicators for better rhythm reference
                if (step % 4 == 0) {
                    button.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 1));
                } else {
                    button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                }
                
                // Add button to panel and tracking list
                panel.add(button);
                triggerButtons.add(button);
            }
        }
        
        return panel;
    }

    /**
     * Calculate appropriate cell size based on pattern length
     */
    private int calculateCellSize(int patternLength) {
        // Scale cells inversely with pattern length
        switch (patternLength) {
            case 16:  return 24; // Default size
            case 32:  return 16;
            case 48:  return 12;
            case 64:  return 10;
            default:  return 24;
        }
    }

    /**
     * Update pattern length radio buttons and quadrant navigation controls
     * based on current pattern length
     */
    private void updatePatternLengthControls() {
        // Get the current pattern length for the selected drum
        int patternLength = sequencer.getPatternLength(selectedPadIndex);
        
        // Update radio buttons without triggering their action listeners
        updatingUI = true;
        try {
            // Select the appropriate radio button based on pattern length
            if (patternLength <= 16) {
                steps16Radio.setSelected(true);
                maxPatternLength = 16;
            } else if (patternLength <= 32) {
                steps32Radio.setSelected(true);
                maxPatternLength = 32;
            } else if (patternLength <= 48) {
                steps48Radio.setSelected(true);
                maxPatternLength = 48;
            } else {
                steps64Radio.setSelected(true);
                maxPatternLength = 64;
            }
        } finally {
            updatingUI = false;
        }
        
        // Update quadrant navigation
        int maxQuadrants = maxPatternLength / STEPS_PER_QUADRANT;
        
        // Ensure current quadrant is within valid range
        if (currentQuadrant >= maxQuadrants) {
            currentQuadrant = maxQuadrants - 1;
        }
        
        // Update navigation buttons state and label
        updateQuadrantControls();
        
        // If in FOLLOW mode, update the grid view to show the current quadrant
        if (currentViewMode == ViewMode.FOLLOW) {
            updateGridView();
        }
        
        // Force update for all step buttons
        updateStepButtonsForDrum(selectedPadIndex);
        
        logger.debug("Pattern length controls updated for length {}", patternLength);
    }

    /**
     * Completely synchronize all step buttons with sequencer state
     * This ensures all buttons correctly reflect the pattern state
     */
    private void fullSyncGridWithSequencer() {
        // For MASSIVE view, we need to sync all visible steps
        if (currentViewMode == ViewMode.MASSIVE) {
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                int patternLength = sequencer.getPatternLength(drumIndex);
                
                // For each visible button in this row
                for (int step = 0; step < maxPatternLength; step++) {
                    // Calculate button index in the triggerButtons list
                    int buttonIndex = drumIndex * maxPatternLength + step;
                    
                    // Make sure we don't go out of bounds
                    if (buttonIndex < triggerButtons.size()) {
                        DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                        
                        // Update toggle state from sequencer
                        button.setToggled(sequencer.isStepActive(drumIndex, step));
                        
                        // Update enabled state based on pattern length
                        boolean isWithinPatternLength = step < patternLength;
                        button.setEnabled(isWithinPatternLength);
                        
                        // Clear any temporary states
                        button.clearTemporaryState();
                    }
                }
            }
        }
        // For FOLLOW view, only sync the current quadrant
        else {
            int startStep = currentQuadrant * STEPS_PER_QUADRANT;
            int endStep = startStep + STEPS_PER_QUADRANT;
            
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                int patternLength = sequencer.getPatternLength(drumIndex);
                
                for (int stepOffset = 0; stepOffset < STEPS_PER_QUADRANT; stepOffset++) {
                    int step = startStep + stepOffset;
                    int buttonIndex = drumIndex * STEPS_PER_QUADRANT + stepOffset;
                    
                    if (buttonIndex < triggerButtons.size()) {
                        DrumSequencerGridButton button = triggerButtons.get(buttonIndex);
                        button.setToggled(sequencer.isStepActive(drumIndex, step));
                        button.setEnabled(step < patternLength);
                        button.clearTemporaryState();
                    }
                }
            }
        }
    }
}
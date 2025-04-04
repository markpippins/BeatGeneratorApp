package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.beats.widget.DrumSequencerButton;
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
        
        // Add debug button
        JButton debugButton = new JButton("Debug Grid");
        debugButton.addActionListener(e -> toggleDebugMode());
        sequenceParamsPanel.add(debugButton);
        
        // Select the first drum by default
        if (sequencer != null) {
            sequencer.selectDrumPad(0);
        }
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
            if (updatingUI) return; // Avoid feedback loops
            
            int lastStep = (Integer) lastStepSpinner.getValue();
            logger.info("Setting last step to {} for drum {}", lastStep, selectedPadIndex);
            
            // Use the selected drum index, not a hardcoded value
            sequencer.setPatternLength(selectedPadIndex, lastStep);
        });
        lastStepPanel.add(lastStepSpinner);

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
            
            // Use the selected drum index, not a hardcoded value
            sequencer.setDirection(selectedPadIndex, direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));

        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing to {} for drum {}", division, selectedPadIndex);
                
                // Use the selected drum index, not a hardcoded value
                sequencer.setTimingDivision(selectedPadIndex, division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            if (updatingUI) return; // Avoid feedback loops
            
            boolean loop = loopCheckbox.isSelected();
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

        // ADD CLEAR AND GENERATE BUTTONS
        clearPatternButton = new JButton("Clear");
        generatePatternButton = new JButton("Generate");

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);
        panel.add(loopCheckbox);
        panel.add(rangePanel);
        panel.add(clearPatternButton);
        panel.add(generatePatternButton);

        return panel;
    }

    /**
     * Create the drum pads panel on the left side
     */
    private JPanel createDrumPadsPanel() {
        // Use GridLayout for perfect vertical alignment with grid cells
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 1, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        panel.setBackground(new Color(40, 40, 40)); // Match grid background
        
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
            strike.setMinVelocity(80L);  
            strike.setMaxVelocity(127L); 
            strike.setLevel(100L);      
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
        // Use consistent cell size with even spacing
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 16, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(40, 40, 40));
        
        triggerButtons = new ArrayList<>();
        
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < 16; step++) {
                final int finalDrumIndex = drumIndex;
                final int finalStep = step;
                
                // Create button with empty label
                DrumSequencerGridButton button = new DrumSequencerGridButton("");
                // Use consistent size for alignment
                button.setPreferredSize(new Dimension(25, 25));
                button.setMinimumSize(new Dimension(25, 25));
                button.setMaximumSize(new Dimension(25, 25));
                
                // Make button visible
                button.setBackground(Color.DARK_GRAY);
                button.setOpaque(true);
                
                // Set toggle action
                button.addActionListener(e -> {
                    sequencer.toggleStep(finalDrumIndex, finalStep);
                    button.setToggled(sequencer.isStepActive(finalDrumIndex, finalStep));
                });
                
                // Set initial state
                button.setToggled(sequencer.isStepActive(drumIndex, step));
                
                // Track and add
                triggerButtons.add(button);
                panel.add(button);
            }
        }
        
        return panel;
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
     * 
     * @param drumIndex Index of the drum pad
     * @param oldStep Previous step to unhighlight
     * @param newStep New step to highlight
     */
    private void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Calculate button indices - use correct formula: drumIndex * 16 + step
        int oldButtonIndex = drumIndex * 16 + oldStep;
        int newButtonIndex = drumIndex * 16 + newStep;
        
        // Unhighlight old step button
        if (oldStep >= 0 && oldStep < 16 && oldButtonIndex < triggerButtons.size()) {
            triggerButtons.get(oldButtonIndex).setHighlighted(false);
            triggerButtons.get(oldButtonIndex).repaint();
        }
        
        // Highlight new step button
        if (newStep >= 0 && newStep < 16 && newButtonIndex < triggerButtons.size()) {
            triggerButtons.get(newButtonIndex).setHighlighted(true);
            triggerButtons.get(newButtonIndex).repaint();
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
        if (action == null || action.getCommand() == null) return;
        
        switch (action.getCommand()) {
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
}
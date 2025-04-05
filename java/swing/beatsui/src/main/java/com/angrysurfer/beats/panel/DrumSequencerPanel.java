package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.DrumSequencerButton;
import com.angrysurfer.beats.widget.TriggerButton;
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
    private final List<DrumButton> drumButtons = new ArrayList<>();
    private final List<TriggerButton> triggerButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> decayDials = new ArrayList<>();
    private DrumInfoPanel drumInfoPanel;
    
    // Core sequencer - manages all sequencing logic
    private DrumSequencer sequencer;

    // UI state
    private int selectedPadIndex = -1;
    
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
        drumInfoPanel = new DrumInfoPanel(sequencer);
        
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
            int lastStep = (Integer) lastStepSpinner.getValue();
            sequencer.setPatternLength(lastStep);
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Dir:"));

        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = switch (selectedIndex) {
                case 0 -> Direction.FORWARD;
                case 1 -> Direction.BACKWARD;
                case 2 -> Direction.BOUNCE;
                case 3 -> Direction.RANDOM;
                default -> Direction.FORWARD;
            };
            sequencer.setDirection(direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));

        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            sequencer.setTimingDivision(division);
        });
        timingPanel.add(timingCombo);

        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            sequencer.setLooping(loopCheckbox.isSelected());
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
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
            // Create a Strike object for this drum pad with velocity and level settings
            Strike strike = new Strike();
            strike.setRootNote(defaultNotes[i]);
            strike.setMinVelocity(80L);  // Set reasonable minimum velocity
            strike.setMaxVelocity(127L); // Set maximum velocity
            strike.setLevel(100L);       // Set full level
            strike.setName(drumNames[i]); // Set the name
            
            sequencer.setStrike(i, strike);
            
            // Create the button - use DrumSequencerButton instead of DrumButton
            DrumSequencerButton button = new DrumSequencerButton(i, sequencer);
            button.setDrumName(drumNames[i]);
            
            // Add manual note trigger on right-click
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                        // Right click - play the note
                        Strike strike = sequencer.getStrike(button.getDrumPadIndex());
                        if (strike != null) {
                            NoteEvent noteEvent = new NoteEvent(
                                strike.getRootNote(),
                                127, // Full velocity
                                250  // Duration in ms
                            );
                            sequencer.getNoteEventListener().accept(noteEvent);
                        }
                    }
                }
            });
            
            drumButtons.add(button);
            
            // Add the button to the panel with some spacing
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            buttonPanel.add(button);
            panel.add(buttonPanel);
            
            // Add a small gap between buttons
            if (i < DRUM_PAD_COUNT - 1) {
                panel.add(Box.createRigidArea(new Dimension(0, 2)));
            }
        }
        
        return panel;
    }

    /**
     * Create the step grid panel with proper cell sizing
     */
    private JPanel createStepGridPanel() {
        // Use a more explicit layout strategy for accurate control
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(DRUM_PAD_COUNT, 16, 2, 2)); // 16 steps visible at once
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < 16; step++) { // Show only 16 steps at a time
                final int di = drumIndex;
                final int s = step;
                
                TriggerButton button = new TriggerButton("");
                button.setPreferredSize(new Dimension(25, 25)); // Fixed size for buttons
                button.setMinimumSize(new Dimension(20, 20));   // Minimum size
                
                button.addActionListener(e -> {
                    sequencer.toggleStep(di, s);
                    button.setToggled(sequencer.isStepActive(di, s));
                });
                
                // Set button state based on pattern
                button.setToggled(sequencer.isStepActive(drumIndex, step));
                
                // Store reference to button
                triggerButtons.add(button);
                
                // Add to panel
                panel.add(button);
            }
        }
        
        return panel;
    }

    /**
     * Update a step button's appearance based on whether it's within the pattern length
     */
    private void updateStepButtonAppearance(TriggerButton button, int drumIndex, int step) {
        int patternLength = sequencer.getPatternLength(drumIndex);
        
        if (step >= patternLength) {
            // Beyond pattern length - make it look disabled
            button.setEnabled(false);
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
                TriggerButton button = triggerButtons.get(buttonIndex);
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
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                    button.repaint();
                }
            }
            
            case Commands.DRUM_PAD_SELECTED -> {
                // Update the UI for the changed selection
                if (action.getData() instanceof DrumPadSelectionEvent event) {
                    // Update the selectedPadIndex in the panel
                    int oldSelection = selectedPadIndex;
                    selectedPadIndex = event.getNewSelection();
                    
                    // Update selected button visually
                    if (oldSelection >= 0 && oldSelection < drumButtons.size()) {
                        DrumSequencerButton oldButton = (DrumSequencerButton) drumButtons.get(oldSelection);
                        oldButton.setSelected(false);
                    }
                    
                    if (selectedPadIndex >= 0 && selectedPadIndex < drumButtons.size()) {
                        DrumSequencerButton newButton = (DrumSequencerButton) drumButtons.get(selectedPadIndex);
                        newButton.setSelected(true);
                    }
                    
                    // Update drum info panel
                    if (drumInfoPanel != null) {
                        drumInfoPanel.updateInfo(selectedPadIndex);
                    }
                    
                    // Update parameters display
                    updateParameterControls();
                    
                    // Log the selection change
                    logger.debug("Drum selected: {} -> {}", event.getOldSelection(), event.getNewSelection());
                }
            }
            
            case Commands.PATTERN_PARAMS_CHANGED -> {
                // Update the UI for the changed pattern
                if (action.getData() instanceof Integer drumIndex) {
                    // Update step buttons
                    updateStepButtonsForDrum(drumIndex);
                    
                    // If this is the selected drum, update parameters display
                    if (drumIndex == selectedPadIndex) {
                        updateParameterControls();
                    }
                }
            }
        }
    }

    /**
     * Update the parameter controls to reflect the current selected drum
     */
    private void updateParameterControls() {
        // Prevent feedback loops
        updatingUI = true;
        try {
            // Set last step spinner
            lastStepSpinner.setValue(sequencer.getPatternLength());
            
            // Set direction combo
            Direction dir = sequencer.getDirection(selectedPadIndex);
            switch(dir) {
                case FORWARD -> directionCombo.setSelectedIndex(0);
                case BACKWARD -> directionCombo.setSelectedIndex(1);
                case BOUNCE -> directionCombo.setSelectedIndex(2);
                case RANDOM -> directionCombo.setSelectedIndex(3);
            }
            
            // Set timing combo
            timingCombo.setSelectedItem(sequencer.getTimingDivision(selectedPadIndex));
            
            // Set loop checkbox
            loopCheckbox.setSelected(sequencer.isLooping(selectedPadIndex));
            
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
                    TriggerButton button = triggerButtons.get(buttonIndex);
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
}
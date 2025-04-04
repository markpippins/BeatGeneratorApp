package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
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
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;
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
    
    // Number of drum pads and pattern length
    private static final int DRUM_PAD_COUNT = 16; // 16 tracks
    
    // Flag to prevent listener feedback loops
    private boolean listenersEnabled = true;

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
            updateStepHighlighting(event.getOldStep(), event.getNewStep());
        });
        
        // Initialize UI components
        initialize();
        
        // Register with command bus for UI updates
        CommandBus.getInstance().register(this);
    }

    /**
     * Initialize the UI components
     */
    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Add sequence parameters panel at the top
        JPanel sequenceParamsPanel = createSequenceParametersPanel();
        add(sequenceParamsPanel, BorderLayout.NORTH);
        
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

        // Create range combo with density options
        String[] rangeOptions = {"25%", "50%", "75%", "100%"};
        rangeCombo = new JComboBox<>(rangeOptions);
        rangeCombo.setSelectedIndex(1); // Default to 50%
        rangeCombo.setPreferredSize(new Dimension(60, 25));
        rangePanel.add(rangeCombo);

        // ADD CLEAR AND GENERATE BUTTONS
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            sequencer.clearPattern();
            
            // Update UI after clearing pattern
            for (TriggerButton button : triggerButtons) {
                button.setSelected(false);
                button.repaint();
            }
        });
        
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> {
            // Get selected density from the combo
            int densityIndex = rangeCombo.getSelectedIndex();
            int density = switch(densityIndex) {
                case 0 -> 25;
                case 1 -> 50;
                case 2 -> 75;
                case 3 -> 100;
                default -> 50;
            };
            
            // Generate pattern with selected density
            sequencer.generatePattern(density);
            
            // Update UI to reflect the generated pattern
            syncUIWithSequencer();
        });

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);
        panel.add(loopCheckbox);
        panel.add(rangePanel);
        panel.add(clearButton);
        panel.add(generateButton);

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
            
            sequencer.setStrike(i, strike);
            
            // Create the button
            DrumButton button = new DrumButton();
            button.setText(drumNames[i]);
            
            button.setToolTipText(drumNames[i] + " (Note: " + defaultNotes[i] + ")");
            
            // Set button action to select this pad
            int padIndex = i;
            button.addActionListener(e -> {
                selectDrumPad(padIndex);
            });
            
            // Add manual note trigger on right-click
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                        // Right click - play the note
                        Strike strike = sequencer.getStrike(padIndex);
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
     * Create the step grid panel for the sequencer
     */
    private JPanel createStepGridPanel() {
        JPanel panel = new JPanel(new GridLayout(DRUM_PAD_COUNT, 16, 3, 3));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
        
        // Create trigger buttons for each drum and step
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < 16; step++) {
                TriggerButton button = new TriggerButton("");
                button.setToggleable(true);
                button.setToolTipText("Step " + (step + 1) + " for " + drumButtons.get(drumIndex).getText());
                
                // Add action listener
                final int finalDrumIndex = drumIndex;
                final int finalStep = step;
                button.addActionListener(e -> {
                    if (!listenersEnabled) return;
                    
                    boolean isSelected = button.isSelected();
                    sequencer.setStep(finalDrumIndex, finalStep, isSelected);
                });
                
                triggerButtons.add(button);
                panel.add(button);
            }
        }
        
        return panel;
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
     * @param oldStep Previous step to unhighlight
     * @param newStep New step to highlight
     */
    private void updateStepHighlighting(int oldStep, int newStep) {
        // For each drum pad
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            // Calculate button indices
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
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                // Ensure UI is in sync with sequencer state
                syncUIWithSequencer();
                logger.info("DrumSequencerPanel: Transport start/play received");
            }
            
            case Commands.TRANSPORT_STOP -> {
                // Clear all highlighting
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                    button.repaint();
                }
            }
        }
    }

    /**
     * Synchronize UI components with sequencer state
     */
    private void syncUIWithSequencer() {
        // Disable listeners to prevent feedback loops during updates
        boolean originalListenersState = disableAllListeners();
        
        try {
            // Sync buttons with pattern data
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                for (int step = 0; step < 16; step++) {
                    int buttonIndex = drumIndex * 16 + step;
                    
                    if (buttonIndex < triggerButtons.size()) {
                        // Get the active state directly from sequencer with bounds checking
                        boolean stepActive = step < sequencer.getPatternLength() && 
                                             drumIndex < DRUM_PAD_COUNT &&
                                             sequencer.isStepActive(drumIndex, step);
                        
                        // Update button state and force repaint
                        TriggerButton button = triggerButtons.get(buttonIndex);
                        if (button.isSelected() != stepActive) {
                            button.setSelected(stepActive);
                            button.repaint();
                        }
                    }
                }
            }
        } finally {
            // Always restore listener state
            restoreListeners(originalListenersState);
        }
    }

    /**
     * Disable all listeners and return the original state.
     */
    private boolean disableAllListeners() {
        boolean originalState = listenersEnabled;
        listenersEnabled = false;
        return originalState;
    }

    /**
     * Restore listeners to their original state.
     */
    private void restoreListeners(boolean originalState) {
        listenersEnabled = originalState;
    }
}
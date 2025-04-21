package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;

import java.awt.*;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A standalone panel for managing drum sequence parameters
 */
public class DrumParamsSequencerParametersPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(DrumParamsSequencerParametersPanel.class.getName());
    
    // Sequencer reference
    private final DrumSequencer sequencer;
    
    // UI components
    private JSpinner lastStepSpinner;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    
    /**
     * Create a new DrumSequencerParametersPanel that uses CommandBus for communication
     * 
     * @param sequencer The sequencer to control
     */
    public DrumParamsSequencerParametersPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
    }
    
    /**
     * Initialize the panel
     */
    private void initialize() {
        setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        // Last step spinner with label
        add(new JLabel("Length:"));
        lastStepSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 64, 1));
        lastStepSpinner.setPreferredSize(new Dimension(60, 25));
        lastStepSpinner.addChangeListener(e -> {
            int selectedPadIndex = sequencer.getSelectedPadIndex();
            if (selectedPadIndex >= 0) {
                int value = (Integer) lastStepSpinner.getValue();
                sequencer.setPatternLength(selectedPadIndex, value);
                
                // Notify other components through command bus
                CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_UPDATED, 
                    this, 
                    selectedPadIndex);
            }
        });
        add(lastStepSpinner);
        
        // Loop toggle
        loopToggleButton = new JToggleButton("Loop");
        loopToggleButton.setPreferredSize(new Dimension(70, 25));
        loopToggleButton.addActionListener(e -> {
            int selectedPadIndex = sequencer.getSelectedPadIndex();
            if (selectedPadIndex >= 0) {
                sequencer.setLooping(selectedPadIndex, loopToggleButton.isSelected());
                
                // Notify other components through command bus
                CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_UPDATED, 
                    this, 
                    selectedPadIndex);
            }
        });
        add(loopToggleButton);
        
        // Direction combo
        add(new JLabel("Direction:"));
        directionCombo = new JComboBox<>(
            Arrays.stream(Direction.values())
                .map(Enum::name)
                .toArray(String[]::new)
        );
        directionCombo.setPreferredSize(new Dimension(100, 25));
        directionCombo.addActionListener(e -> {
            int selectedPadIndex = sequencer.getSelectedPadIndex();
            if (selectedPadIndex >= 0 && !updatingControls) {
                String dirName = (String) directionCombo.getSelectedItem();
                if (dirName != null) {
                    Direction dir = Direction.valueOf(dirName);
                    sequencer.setDirection(selectedPadIndex, dir);
                    
                    // Notify other components through command bus
                    CommandBus.getInstance().publish(
                        Commands.DRUM_SEQUENCE_UPDATED, 
                        this, 
                        selectedPadIndex);
                }
            }
        });
        add(directionCombo);
        
        // Timing division combo
        add(new JLabel("Timing:"));
        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(80, 25));
        timingCombo.addActionListener(e -> {
            int selectedPadIndex = sequencer.getSelectedPadIndex();
            if (selectedPadIndex >= 0 && !updatingControls) {
                TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
                if (division != null) {
                    sequencer.setTimingDivision(selectedPadIndex, division);
                    
                    // Notify other components through command bus
                    CommandBus.getInstance().publish(
                        Commands.TIMING_DIVISION_CHANGED, 
                        this, 
                        division);
                }
            }
        });
        add(timingCombo);
    }
    
    private boolean updatingControls = false;
    
    /**
     * Update the panel controls to reflect the settings for a drum pad
     * 
     * @param drumIndex Index of the drum to display settings for
     */
    public void updateControls(int drumIndex) {
        if (drumIndex < 0) return;
        
        updatingControls = true;
        try {
            // Update pattern length
            lastStepSpinner.setValue(sequencer.getPatternLength(drumIndex));
            
            // Update loop state
            loopToggleButton.setSelected(sequencer.isLooping(drumIndex));
            
            // Update direction
            Direction direction = sequencer.getDirection(drumIndex);
            directionCombo.setSelectedItem(direction.name());
            
            // Update timing division
            TimingDivision division = sequencer.getTimingDivision(drumIndex);
            timingCombo.setSelectedItem(division);
        } finally {
            updatingControls = false;
        }
    }
}
package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel containing the maximum pattern length control
 */
public class DrumSequencerMaxLengthPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerMaxLengthPanel.class);
    
    // UI components
    private JComboBox<Integer> maxLengthCombo;
    
    // References
    private final DrumSequencer sequencer;
    
    // UI constants
    private static final int CONTROL_HEIGHT = 25;
    private static final int MEDIUM_CONTROL_WIDTH = 60;
    
    /**
     * Create a new MaxLengthPanel
     * 
     * @param sequencer The drum sequencer
     */
    public DrumSequencerMaxLengthPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        setBorder(BorderFactory.createTitledBorder("Sequencer Parameters"));
        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
        
        initializeComponents();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        add(new JLabel("Max Length:"));

        // Create combo box with standard pattern lengths
        Integer[] maxLengths = {16, 32, 64, 128}; 
        maxLengthCombo = new JComboBox<>(maxLengths);
        maxLengthCombo.setSelectedItem(sequencer.getMaxPatternLength());
        maxLengthCombo.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        maxLengthCombo.setToolTipText("Set maximum pattern length");
        
        maxLengthCombo.addActionListener(e -> {
            int newMaxLength = (Integer) maxLengthCombo.getSelectedItem();
            
            // Set new max pattern length in sequencer
            sequencer.setMaxPatternLength(newMaxLength);
            
            logger.info("Set maximum pattern length to: {}", newMaxLength);
            
            // First publish event for updating spinner constraints
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_MAX_LENGTH_CHANGED, this, newMaxLength);
            
            // Then publish a command to recreate the grid
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_GRID_RECREATE_REQUESTED, this, newMaxLength);
        });
        
        // Add components to panel
        add(maxLengthCombo);
    }
    
    /**
     * Update the control to reflect current sequencer state
     */
    public void updateControl() {
        maxLengthCombo.setSelectedItem(sequencer.getMaxPatternLength());
    }
}
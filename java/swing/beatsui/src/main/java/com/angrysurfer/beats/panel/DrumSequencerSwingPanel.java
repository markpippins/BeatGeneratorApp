package com.angrysurfer.beats.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel containing swing controls for drum sequencer
 * Copied from DrumEffectsSequencerPanel to maintain consistent layout
 */
public class DrumSequencerSwingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerSwingPanel.class);
    
    // Swing values
    private static final int MIN_SWING = 0;
    private static final int MAX_SWING = 100;
    private static final int DEFAULT_SWING = 50;
    
    // UI components
    private JSlider swingSlider;
    private JLabel swingValueLabel;
    
    // Reference to sequencer
    private final DrumSequencer sequencer;
    
    /**
     * Creates a new swing control panel
     */
    public DrumSequencerSwingPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        // Match the original layout exactly
        setBorder(BorderFactory.createTitledBorder("Swing"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        // Create the slider with same dimensions as original
        swingSlider = new JSlider(JSlider.HORIZONTAL, MIN_SWING, MAX_SWING, DEFAULT_SWING);
        swingSlider.setPreferredSize(new Dimension(120, 20));
        swingSlider.addChangeListener(e -> {
            int value = swingSlider.getValue();
            updateSwingValueLabel(value);
            
            if (!swingSlider.getValueIsAdjusting()) {
                logger.info("Setting swing to: {}", value);
                sequencer.setSwingPercentage(value);
            }
        });
        
        // Create the label with the same position and size
        swingValueLabel = new JLabel(DEFAULT_SWING + "%");
        swingValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        swingValueLabel.setPreferredSize(new Dimension(30, 20));
        
        // Match the original component order
        add(swingSlider);
        add(swingValueLabel);
        
        // Add reset button with same appearance as original
        JButton resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(60, 20));
        resetButton.addActionListener(e -> {
            swingSlider.setValue(DEFAULT_SWING);
            sequencer.setSwingPercentage(DEFAULT_SWING);
        });
        add(resetButton);
    }
    
    /**
     * Updates the displayed swing percentage
     */
    private void updateSwingValueLabel(int value) {
        swingValueLabel.setText(value + "%");
    }
    
    /**
     * Updates controls to match current sequencer state
     */
    public void updateControls() {
        int currentSwing = sequencer.getSwingPercentage();
        swingSlider.setValue(currentSwing);
        updateSwingValueLabel(currentSwing);
    }
}
package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.angrysurfer.beats.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel for generating random patterns in the drum sequencer
 */
public class DrumSequenceGeneratorPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceGeneratorPanel.class);
    
    // UI components
    private JComboBox<String> densityCombo;
    private JButton generateButton;
    
    // Reference to the sequencer
    private final DrumSequencer sequencer;
    

    /**
     * Create a new DrumSequenceGeneratorPanel
     * 
     * @param sequencer The drum sequencer
     */
    public DrumSequenceGeneratorPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        setBorder(BorderFactory.createTitledBorder("Generate"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        initializeComponents();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Create density combo without a label
        String[] densityOptions = { "25%", "50%", "75%", "100%" };
        densityCombo = new JComboBox<>(densityOptions);
        densityCombo.setSelectedIndex(1); // Default to 50%
        densityCombo.setPreferredSize(new Dimension(UIUtils.MEDIUM_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        densityCombo.setToolTipText("Set pattern density");

        // Generate button with dice icon
        generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        generateButton.setMargin(new Insets(2, 2, 2, 2));
        generateButton.addActionListener(e -> {
            // Get selected density from the combo
            int density = (densityCombo.getSelectedIndex() + 1) * 25;
            logger.info("Generating pattern with density: {}%", density);
            
            // Generate pattern in the sequencer
            sequencer.generatePattern(density);
            
            // Publish event to refresh UI
            CommandBus.getInstance().publish(
                Commands.DRUM_GRID_REFRESH_REQUESTED,
                this,
                null
            );
        });

        // Add components to panel
        add(generateButton);
        add(densityCombo);
    }
    
    /**
     * Set the density value
     * 
     * @param densityPercent Density percentage (25, 50, 75, 100)
     */
    public void setDensity(int densityPercent) {
        // Convert percentage to index (0-3)
        int index = (densityPercent / 25) - 1;
        
        // Ensure index is within bounds
        if (index >= 0 && index < densityCombo.getItemCount()) {
            densityCombo.setSelectedIndex(index);
        }
    }
    
    /**
     * Get the current density percentage value
     * 
     * @return The density percentage (25, 50, 75, 100)
     */
    public int getDensity() {
        return (densityCombo.getSelectedIndex() + 1) * 25;
    }
}
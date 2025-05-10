package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.MelodicSequencerManager;

/**
 * Panel for pattern generation controls for a melodic sequencer
 */
public class MelodicSequencerGeneratorPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerGeneratorPanel.class);
    
    // Reference to sequencer
    private final MelodicSequencer sequencer;
    
    // UI Components
    private JComboBox<String> rangeCombo;
    private JToggleButton latchToggleButton;
    
    // Random generator
    private final Random random = new Random();
    
    /**
     * Constructor
     * 
     * @param sequencer The melodic sequencer to generate patterns for
     */
    public MelodicSequencerGeneratorPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initializeUI();
    }
    
    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        setBorder(BorderFactory.createTitledBorder("Generate"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        
        // Range combo
        String[] rangeOptions = { "1 Octave", "2 Octaves", "3 Octaves", "4 Octaves" };
        rangeCombo = new JComboBox<>(rangeOptions);
        rangeCombo.setSelectedIndex(1); // Default to 2 octaves
        rangeCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        rangeCombo.setToolTipText("Set the octave range for pattern generation");
        
        // Generate button with consistent styling
        JButton generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(24, 24));
        generateButton.setMargin(new Insets(2, 2, 2, 2));
        generateButton.addActionListener(e -> generatePattern());
        
        // Latch toggle button
        latchToggleButton = new JToggleButton("L", false);
        latchToggleButton.setToolTipText("Generate new pattern each cycle");
        latchToggleButton.setPreferredSize(new Dimension(24, 24));
        latchToggleButton.addActionListener(e -> {
            if (sequencer != null) {
                sequencer.setLatchEnabled(latchToggleButton.isSelected());
                logger.info("Latch mode set to: {}", latchToggleButton.isSelected());
            }
        });
        
        // Add components to panel
        add(generateButton);
        add(rangeCombo);
        add(latchToggleButton);
    }
    
    /**
     * Generate a pattern based on current settings
     * @return true if generation was successful
     */
    public boolean generatePattern() {
        try {
            // Get selected octave range from the combo
            int octaveRange = rangeCombo.getSelectedIndex() + 1;
            int density = 50; // Fixed density for now
            
            logger.info("Generating new pattern with octave range: {}, density: {}", octaveRange, density);
            
            // Check if sequencer is valid
            if (sequencer == null) {
                logger.error("Cannot generate pattern - sequencer is null");
                return false;
            }
            
            // Get the current sequence data
            MelodicSequenceData data = sequencer.getSequenceData();
            if (data == null) {
                logger.error("Cannot generate pattern - sequence data is null");
                return false;
            }
            
            // Generate the pattern
            boolean success = generatePatternData(data, octaveRange, density);
            
            if (success) {
                // Apply the generated data to the sequencer
                sequencer.setSequenceData(data);
                
                // Save the changes to the sequence data
                MelodicSequencerManager.getInstance().saveSequence(sequencer);
                
                // Notify that pattern was updated
                SwingUtilities.invokeLater(() -> {
                    CommandBus.getInstance().publish(
                        Commands.PATTERN_UPDATED,
                        sequencer,
                        new MelodicSequencerEvent(
                            sequencer.getId(),
                            data.getId())
                    );
                });
                
                logger.info("Pattern successfully generated and applied");
                return true;
            } else {
                logger.warn("Pattern generation failed");
                return false;
            }
        } catch (Exception ex) {
            logger.error("Error generating pattern: {}", ex.getMessage(), ex);
            return false;
        }
    }
    
    /**
     * Generate a pattern and store it in the provided data object
     * 
     * @param data The sequence data to update
     * @param octaveRange The number of octaves to use (1-4)
     * @param density The note density (0-100)
     * @return true if generation was successful
     */
    private boolean generatePatternData(MelodicSequenceData data, int octaveRange, int density) {
        try {
            // Validate parameters
            if (octaveRange < 1) octaveRange = 1;
            if (octaveRange > 4) octaveRange = 4;
            if (density < 0) density = 0;
            if (density > 100) density = 100;
            
            int maxSteps = data.getMaxSteps();
            
            // Clear current pattern
            for (int step = 0; step < maxSteps; step++) {
                data.setStepActive(step, false);
                data.setNoteValue(step, 60); // Middle C default
                data.setVelocityValue(step, 64); // Medium velocity
                data.setGateValue(step, 75); // Medium gate
                data.setProbabilityValue(step, 100); // Full probability
            }
            
            // Calculate note range based on octave selection
            int baseNote = 60 - (12 * (octaveRange / 2)); // Center around middle C
            int noteRange = 12 * octaveRange;
            
            // Determine active steps based on density
            int stepsToActivate = (int) Math.round(maxSteps * (density / 100.0));
            
            // Ensure we have at least one step if density > 0
            if (density > 0 && stepsToActivate == 0) {
                stepsToActivate = 1;
            }
            
            logger.debug("Will activate {} steps out of {}", stepsToActivate, maxSteps);
            
            // Generate steps
            for (int i = 0; i < stepsToActivate; i++) {
                // Choose a random step that's not already active
                int step;
                int attempts = 0;
                do {
                    step = random.nextInt(maxSteps);
                    attempts++;
                    // Prevent infinite loops
                    if (attempts > 100) break;
                } while (data.isStepActive(step) && attempts < 100);
                
                // Activate the step
                data.setStepActive(step, true);
                
                // Generate a random note in the selected range
                int noteOffset = random.nextInt(noteRange);
                int note = baseNote + noteOffset;
                
                // Set the note
                data.setNoteValue(step, note);
                
                // Random velocity between 70-100
                int velocity = 70 + random.nextInt(31);
                data.setVelocityValue(step, velocity);
                
                // Random gate between 50-100
                int gate = 50 + random.nextInt(51);
                data.setGateValue(step, gate);
                
                // Sometimes add randomized probability
                if (random.nextDouble() < 0.3) { // 30% chance of partial probability
                    int probability = 50 + random.nextInt(51); // 50-100
                    data.setProbabilityValue(step, probability);
                }
            }
            
            // Create varying tilt values for more musical interest
            int[] tiltValues = new int[maxSteps];
            for (int i = 0; i < maxSteps; i++) {
                // Create small tilt values (-3 to +3)
                tiltValues[i] = random.nextInt(7) - 3;
            }
            data.setHarmonicTiltValues(tiltValues);
            
            logger.info("Successfully generated new pattern with {} active steps", stepsToActivate);
            return true;
        } catch (Exception e) {
            logger.error("Error generating pattern data: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update UI controls to match current sequencer state
     */
    public void syncWithSequencer() {
        if (sequencer != null) {
            latchToggleButton.setSelected(sequencer.isLatchEnabled());
        }
    }
}
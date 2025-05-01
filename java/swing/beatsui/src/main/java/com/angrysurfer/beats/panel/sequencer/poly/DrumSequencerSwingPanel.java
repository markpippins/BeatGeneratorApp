package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel containing swing controls for drum sequencer
 * Copied from DrumEffectsSequencerPanel to maintain consistent layout
 */
public class DrumSequencerSwingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerSwingPanel.class);
    
    // UI components
    private JSlider swingSlider;
    private JLabel valueLabel;
    private JToggleButton swingToggle;
    
    // Reference to sequencer
    private final DrumSequencer sequencer;
    
    /**
     * Creates a new swing control panel
     */
    public DrumSequencerSwingPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        // ALREADY OPTIMIZED: using 2,1
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        UIUtils.setWidgetPanelBorder(this, "Swing");
            
        // Use compact toggle button
        swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        // ALREADY OPTIMIZED: using 45,22
        swingToggle.setPreferredSize(new Dimension(45, 22));
        swingToggle.setMargin(new Insets(2, 2, 2, 2));
        swingToggle.addActionListener(e -> {
            sequencer.setSwingEnabled(swingToggle.isSelected());
        });
        add(swingToggle);

        // Swing amount slider
        swingSlider = new JSlider(JSlider.HORIZONTAL, 50, 75, sequencer.getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        // Make slider slightly narrower
        // REDUCED: from 90,25 to 85,22
        swingSlider.setPreferredSize(new Dimension(85, 22));

        valueLabel = new JLabel(sequencer.getSwingPercentage() + "%");
        // ADDED: smaller size
        valueLabel.setPreferredSize(new Dimension(25, 22));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));

        swingSlider.addChangeListener(e -> {
            int value = swingSlider.getValue();
            sequencer.setSwingPercentage(value);
            valueLabel.setText(value + "%");
        });

        add(swingSlider);
        add(valueLabel);
    }
    
    /**
     * Updates controls to match current sequencer state
     */
    public void updateControls() {
        swingToggle.setSelected(sequencer.isSwingEnabled());
        swingSlider.setValue(sequencer.getSwingPercentage());
        valueLabel.setText(sequencer.getSwingPercentage() + "%");
    }
}
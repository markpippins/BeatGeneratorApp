package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.sequencer.MelodicSequencer;

/**
 * Panel containing swing controls for melodic sequencer
 */
public class MelodicSequencerSwingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerSwingPanel.class);
    
    // UI components
    private JSlider swingSlider;
    private JLabel valueLabel;
    private JToggleButton swingToggle;
    
    // Reference to sequencer
    private final MelodicSequencer sequencer;
    
    /**
     * Creates a new swing control panel for melodic sequencer
     */
    public MelodicSequencerSwingPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        setBorder(BorderFactory.createTitledBorder("Swing"));

        // Swing on/off toggle
        swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        swingToggle.setPreferredSize(new Dimension(50, 25)); // Slightly wider for text "On"
        swingToggle.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        swingToggle.addActionListener(e -> {
            sequencer.setSwingEnabled(swingToggle.isSelected());
        });
        add(swingToggle);

        // Swing amount slider - use MelodicSequencer.MIN_SWING and MAX_SWING
        swingSlider = new JSlider(JSlider.HORIZONTAL, 
            MelodicSequencer.MIN_SWING, MelodicSequencer.MAX_SWING, sequencer.getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        swingSlider.setPreferredSize(new Dimension(100, 30));

        valueLabel = new JLabel(sequencer.getSwingPercentage() + "%");

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
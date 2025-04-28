package com.angrysurfer.beats.panel.sequencer.mono;

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
        
        // Reduce layout spacing
        setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));

        // Use compact titled border
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Swing",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null, 
            null
        ));

        // Swing on/off toggle
        swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        // Further reduce toggle button size
        swingToggle.setPreferredSize(new Dimension(40, 22));
        swingToggle.setMargin(new Insets(2, 2, 2, 2)); // Already compact
        swingToggle.addActionListener(e -> {
            sequencer.setSwingEnabled(swingToggle.isSelected());
        });
        add(swingToggle);

        // Swing amount slider
        swingSlider = new JSlider(JSlider.HORIZONTAL, 
            MelodicSequencer.MIN_SWING, MelodicSequencer.MAX_SWING, sequencer.getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        // Reduce slider size
        swingSlider.setPreferredSize(new Dimension(85, 22));

        valueLabel = new JLabel(sequencer.getSwingPercentage() + "%");
        // ADDED: Smaller font for more compact display
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        // Make value label more compact
        valueLabel.setPreferredSize(new Dimension(25, 22));

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
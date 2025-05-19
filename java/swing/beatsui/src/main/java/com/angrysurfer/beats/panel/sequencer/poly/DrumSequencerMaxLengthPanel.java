package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing the maximum pattern length control
 */
public class DrumSequencerMaxLengthPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerMaxLengthPanel.class);
    // References
    private final DrumSequencer sequencer;
    // UI components
    private JComboBox<Integer> maxLengthCombo;

    /**
     * Create a new MaxLengthPanel
     *
     * @param sequencer The drum sequencer
     */
    public DrumSequencerMaxLengthPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        // In DrumSequencerMaxLengthPanel's constructor
        UIHelper.setWidgetPanelBorder(this, "Sequencer");

        // REDUCED: from 5,2 to 2,1
        setLayout(new FlowLayout(FlowLayout.CENTER, 2, 1));

        initializeComponents();
    }

    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Create a label with minimum width
        JLabel label = new JLabel("Max Length:");
        label.setFont(label.getFont().deriveFont(11f)); // Smaller font
        add(label);

        // Create combo box with standard pattern lengths
        Integer[] maxLengths = {16, 32, 64, 128};
        maxLengthCombo = new JComboBox<>(maxLengths);
        maxLengthCombo.setSelectedItem(sequencer.getMaxPatternLength());

        // REDUCED: width from MEDIUM_CONTROL_WIDTH to SMALL_CONTROL_WIDTH + 10
        maxLengthCombo.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH + 10, UIHelper.CONTROL_HEIGHT));
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
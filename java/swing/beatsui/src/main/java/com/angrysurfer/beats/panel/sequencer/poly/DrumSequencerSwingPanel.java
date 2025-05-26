package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.DrumSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing swing controls for drum sequencer
 * Copied from DrumEffectsSequencerPanel to maintain consistent layout
 */
public class DrumSequencerSwingPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerSwingPanel.class);
    // Reference to sequencer
    private final DrumSequencer sequencer;
    // UI components
    private final JSlider swingSlider;
    private final JLabel valueLabel;
    private final JToggleButton swingToggle;

    /**
     * Creates a new swing control panel
     */
    public DrumSequencerSwingPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;

        // ALREADY OPTIMIZED: using 2,1
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        UIHelper.setWidgetPanelBorder(this, "Swing");

        // Use compact toggle button
        swingToggle = new JToggleButton("On", sequencer.isSwingEnabled());
        // ALREADY OPTIMIZED: using 45,22
        swingToggle.setPreferredSize(new Dimension(45, 22));
        swingToggle.setMargin(new Insets(2, 2, 2, 2));
        swingToggle.addActionListener(e -> sequencer.setSwingEnabled(swingToggle.isSelected()));
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

        CommandBus.getInstance().register(this, new String[]{Commands.NEW_VALUE_SWING});
    }

    /**
     * Updates controls to match current sequencer state
     */
    public void updateControls() {
        swingToggle.setSelected(sequencer.isSwingEnabled());
        swingSlider.setValue(sequencer.getSwingPercentage());
        valueLabel.setText(sequencer.getSwingPercentage() + "%");
    }

    @Override
    public void onAction(Command action) {

    }
}
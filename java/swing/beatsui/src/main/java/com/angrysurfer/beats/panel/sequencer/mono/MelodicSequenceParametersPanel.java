package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicScaleSelectionEvent;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel containing sequence parameters for melodic sequencers
 */
public class MelodicSequenceParametersPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceParametersPanel.class);

    // UI Controls
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton loopToggleButton;
    private JSpinner lastStepSpinner;

    // Reference to sequencer
    private MelodicSequencer sequencer;

    // Flag to prevent event loops
    private boolean updatingUI = false;

    /**
     * Create a new sequence parameters panel
     * 
     * @param sequencer The melodic sequencer to control
     */
    public MelodicSequenceParametersPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
    }

    /**
     * Initialize the panel with all controls
     */
    private void initialize() {
        setLayout(new BorderLayout(0, 0)); // No gaps between components
        UIHelper.setWidgetPanelBorder(this,"Sequence Parameters");

        // Reduce spacing in the controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));

        // Add all controls to the left panel EXCEPT those now in the scale panel
        createLastStepControls(controlsPanel);
        createDirectionControls(controlsPanel);
        createTimingControls(controlsPanel);
        createLoopButton(controlsPanel);
        createRotationControls(controlsPanel);

        add(controlsPanel, BorderLayout.WEST);

        // Reduce spacing in the clear button panel
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        JButton clearButton = createClearButton();
        clearPanel.add(clearButton);

        // Add the clear panel to the EAST position
        add(clearPanel, BorderLayout.EAST);
    }

    /**
     * Create last step spinner control
     */
    private void createLastStepControls(JPanel parentPanel) {
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0)); // Changed from 2,0 to 1,0
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 64, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH - 5, UIHelper.CONTROL_HEIGHT)); // Reduced
                                                                                                                   // width
        lastStepSpinner.setToolTipText("Set the last step for the pattern (1-16)");
        lastStepSpinner.addChangeListener(e -> {
            if (!updatingUI) {
                int lastStep = (Integer) lastStepSpinner.getValue();
                sequencer.setPatternLength(lastStep);
            }
        });
        lastStepPanel.add(lastStepSpinner);

        parentPanel.add(lastStepPanel);
    }

    /**
     * Create direction combo control
     */
    private void createDirectionControls(JPanel parentPanel) {
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        directionCombo = new JComboBox<>(new String[] { "Forward", "Backward", "Bounce", "Random" });
        directionCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
        directionCombo.addActionListener(e -> {
            if (!updatingUI) {
                int selectedIndex = directionCombo.getSelectedIndex();
                Direction direction = switch (selectedIndex) {
                    case 0 -> Direction.FORWARD;
                    case 1 -> Direction.BACKWARD;
                    case 2 -> Direction.BOUNCE;
                    case 3 -> Direction.RANDOM;
                    default -> Direction.FORWARD;
                };
                sequencer.setDirection(direction);
            }
        });
        directionPanel.add(directionCombo);

        parentPanel.add(directionPanel);
    }

    /**
     * Create timing division combo control
     */
    private void createTimingControls(JPanel parentPanel) {
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        timingCombo.setToolTipText("Set the timing division for this pattern");
        timingCombo.addActionListener(e -> {
            if (!updatingUI) {
                TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
                if (division != null) {
                    logger.info("Setting timing division to {}", division);
                    sequencer.setTimingDivision(division);
                }
            }
        });
        timingPanel.add(timingCombo);

        parentPanel.add(timingPanel);
    }

    /**
     * Create loop toggle button
     */
    private void createLoopButton(JPanel parentPanel) {
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.setLooping(loopToggleButton.isSelected());
            }
        });

        parentPanel.add(loopToggleButton);
    }

    /**
     * Create rotation controls
     */
    private void createRotationControls(JPanel parentPanel) {
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        // Rotate Left button
        JButton rotateLeftButton = new JButton("⟵");
        rotateLeftButton.setToolTipText("Rotate pattern one step left");
        rotateLeftButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        rotateLeftButton.setMargin(new Insets(2, 2, 2, 2));
        rotateLeftButton.addActionListener(e -> {
            sequencer.rotatePatternLeft();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        // Rotate Right button
        JButton rotateRightButton = new JButton("⟶");
        rotateRightButton.setToolTipText("Rotate pattern one step right");
        rotateRightButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        rotateRightButton.setMargin(new Insets(2, 2, 2, 2));
        rotateRightButton.addActionListener(e -> {
            sequencer.rotatePatternRight();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        // Add buttons to rotation panel
        rotationPanel.add(rotateLeftButton);
        rotationPanel.add(rotateRightButton);

        parentPanel.add(rotationPanel);
    }

    /**
     * Create clear button
     * 
     * @return The created button
     */
    private JButton createClearButton() {
        JButton clearButton = new JButton("🗑️");
        clearButton.setToolTipText("Clear pattern");
        clearButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        clearButton.setMargin(new Insets(2, 2, 2, 2));
        clearButton.addActionListener(e -> {
            sequencer.clearPattern();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        return clearButton;
    }

    /**
     * Update the panel UI to reflect sequencer state
     * 
     * @param sequencer The sequencer to sync with
     */
    public void updateUI(MelodicSequencer sequencer) {
        if (sequencer == null) {
            return;
        }

        // Store the current sequencer reference
        this.sequencer = sequencer;

        // Set flag to prevent event loops
        updatingUI = true;

        try {
            // Update timing division
            TimingDivision timingDivision = sequencer.getTimingDivision();
            if (timingDivision != null) {
                timingCombo.setSelectedItem(timingDivision);
            }

            // Update direction
            Direction direction = sequencer.getDirection();
            if (direction != null) {
                switch (direction) {
                    case FORWARD -> directionCombo.setSelectedIndex(0);
                    case BACKWARD -> directionCombo.setSelectedIndex(1);
                    case BOUNCE -> directionCombo.setSelectedIndex(2);
                    case RANDOM -> directionCombo.setSelectedIndex(3);
                }
            }

            // Update loop state
            loopToggleButton.setSelected(sequencer.isLooping());

            // Update last step
            lastStepSpinner.setValue(sequencer.getPatternLength());

        } finally {
            // Reset flag after updates
            updatingUI = false;
        }
    }

}
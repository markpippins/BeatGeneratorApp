package com.angrysurfer.beats.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;

/**
 * Panel containing sequence parameters controls for a drum sequencer
 */
public class DrumSequencerParametersPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerParametersPanel.class);
    
    // UI components
    private JSpinner lastStepSpinner;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton loopToggleButton;
    private JButton clearPatternButton;
    
    // Reference to the sequencer and parent panel
    private final DrumSequencer sequencer;
    private final DrumSequencerPanel parentPanel;
    
    // UI constants
    private static final int CONTROL_HEIGHT = 25;
    private static final int SMALL_CONTROL_WIDTH = 40;
    private static final int MEDIUM_CONTROL_WIDTH = 60;
    private static final int LARGE_CONTROL_WIDTH = 90;
    
    /**
     * Creates a new Sequence Parameters panel
     */
    public DrumSequencerParametersPanel(DrumSequencer sequencer, DrumSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;
        
        setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        initializeComponents();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeComponents() {
        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-sequencer.getMaxSteps(), default sequencer.getDefaultPatternLength()
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(
            sequencer.getDefaultPatternLength(), 1, sequencer.getMaxSteps(), 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        lastStepSpinner.setToolTipText("Set the last step of the pattern (1-" + sequencer.getMaxSteps() + ")");
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            logger.info("Setting last step to {} for drum {}", lastStep, parentPanel.getSelectedPadIndex());

            // Use the selected drum index from the parent panel
            sequencer.setPatternLength(parentPanel.getSelectedPadIndex(), lastStep);
            
            // Update UI in the parent panel
            parentPanel.updateStepButtonsForDrum(parentPanel.getSelectedPadIndex());
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo - Make label skinnier
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        directionCombo = new JComboBox<>(new String[] { "Forward", "Backward", "Bounce", "Random" });
        directionCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = Direction.FORWARD; // Default

            switch (selectedIndex) {
                case 0 -> direction = Direction.FORWARD;
                case 1 -> direction = Direction.BACKWARD;
                case 2 -> direction = Direction.BOUNCE;
                case 3 -> direction = Direction.RANDOM;
            }

            logger.info("Setting direction to {} for drum {}", direction, parentPanel.getSelectedPadIndex());

            // Use the selected drum index from the parent panel
            sequencer.setDirection(parentPanel.getSelectedPadIndex(), direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo - Make label skinnier
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing to {} for drum {}", division, parentPanel.getSelectedPadIndex());

                // Use the selected drum index from the parent panel
                sequencer.setTimingDivision(parentPanel.getSelectedPadIndex(), division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox - Make skinnier
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT)); // Reduce width
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            boolean loop = loopToggleButton.isSelected();
            logger.info("Setting loop to {} for drum {}", loop, parentPanel.getSelectedPadIndex());

            // Use the selected drum index from the parent panel
            sequencer.setLooping(parentPanel.getSelectedPadIndex(), loop);
        });

        // Clear pattern button
        clearPatternButton = new JButton("🗑️");
        clearPatternButton.setToolTipText("Clear the pattern for this drum");
        clearPatternButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT)); // Reduce width
        clearPatternButton.setMargin(new Insets(2, 2, 2, 2));
        clearPatternButton.addActionListener(e -> {
            parentPanel.clearRow(parentPanel.getSelectedPadIndex());
        });

        // Create rotation panel for push/pull buttons
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Push forward button
        JButton pushForwardButton = new JButton("⟶");
        pushForwardButton.setToolTipText("Push pattern forward (right)");
        pushForwardButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        pushForwardButton.setMargin(new Insets(2, 2, 2, 2));
        pushForwardButton.addActionListener(e -> {
            sequencer.pushForward();
            parentPanel.refreshGridUI();
        });

        // Pull backward button
        JButton pullBackwardButton = new JButton("⟵");
        pullBackwardButton.setToolTipText("Pull pattern backward (left)");
        pullBackwardButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        pullBackwardButton.setMargin(new Insets(2, 2, 2, 2));
        pullBackwardButton.addActionListener(e -> {
            sequencer.pullBackward();
            parentPanel.refreshGridUI();
        });

        // Add buttons to rotation panel
        rotationPanel.add(pullBackwardButton);
        rotationPanel.add(pushForwardButton);

        // Add all components to panel in a single row
        add(timingPanel);
        add(directionPanel);
        add(loopToggleButton);
        add(lastStepPanel);
        add(rotationPanel);
        add(clearPatternButton);
    }
    
    /**
     * Update controls to match the selected drum's parameters
     */
    public void updateControls(int selectedDrumIndex) {
        // Get values for the selected drum
        int length = sequencer.getPatternLength(selectedDrumIndex);
        Direction dir = sequencer.getDirection(selectedDrumIndex);
        TimingDivision timing = sequencer.getTimingDivision(selectedDrumIndex);
        boolean isLooping = sequencer.isLooping(selectedDrumIndex);

        // Update UI components
        lastStepSpinner.setValue(length);

        switch (dir) {
            case FORWARD -> directionCombo.setSelectedIndex(0);
            case BACKWARD -> directionCombo.setSelectedIndex(1);
            case BOUNCE -> directionCombo.setSelectedIndex(2);
            case RANDOM -> directionCombo.setSelectedIndex(3);
        }

        timingCombo.setSelectedItem(timing);
        loopToggleButton.setSelected(isLooping);

        // Repaint components
        lastStepSpinner.repaint();
        directionCombo.repaint();
        timingCombo.repaint();
        loopToggleButton.repaint();
    }
}
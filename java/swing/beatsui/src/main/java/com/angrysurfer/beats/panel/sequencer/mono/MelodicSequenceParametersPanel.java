package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.beats.event.MelodicScaleSelectionEvent;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

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
    
    // Size constants
    private static final int SMALL_CONTROL_WIDTH = 35;   // REDUCED: from 40 to 35
    private static final int MEDIUM_CONTROL_WIDTH = 55;  // REDUCED: from 60 to 55
    private static final int LARGE_CONTROL_WIDTH = 85;   // REDUCED: from 90 to 85
    private static final int CONTROL_HEIGHT = 22;        // REDUCED: from 25 to 22
    
    // UI Controls
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton loopToggleButton;
    private JSpinner lastStepSpinner;
    private JComboBox<String> rootNoteCombo;
    private JComboBox<String> scaleCombo;
    private JCheckBox quantizeCheckbox;
    private JLabel octaveLabel;
    private JButton octaveDownBtn;
    private JButton octaveUpBtn;
    
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
        // Keep the titled border
        setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        
        // Use BorderLayout for the main panel instead of FlowLayout
        setLayout(new BorderLayout(0, 0));
        
        // Create a container panel for all the controls except clear button
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        
        // Add all controls to the left panel except clear button
        createLastStepControls(controlsPanel);
        createDirectionControls(controlsPanel);
        createTimingControls(controlsPanel);
        createLoopButton(controlsPanel);
        createRotationControls(controlsPanel);
        // Don't add clear button here
        createRootNoteControls(controlsPanel);
        createQuantizeControls(controlsPanel);
        createScaleControls(controlsPanel);
        createOctaveControls(controlsPanel);
        
        // Add the controls panel to the CENTER of the BorderLayout
        add(controlsPanel, BorderLayout.CENTER);
        
        // Create a panel for the clear button with right alignment
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));
        JButton clearButton = createClearButton();
        clearPanel.add(clearButton);
        
        // Add the clear panel to the EAST position
        add(clearPanel, BorderLayout.EAST);
    }
    
    /**
     * Create last step spinner control
     */
    private void createLastStepControls(JPanel parentPanel) {
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 64, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
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
        directionCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
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
        timingCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
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
        loopToggleButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
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
        rotateLeftButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        rotateLeftButton.setMargin(new Insets(2, 2, 2, 2));
        rotateLeftButton.addActionListener(e -> {
            sequencer.rotatePatternLeft();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                Commands.PATTERN_UPDATED,
                sequencer,
                null
            );
        });

        // Rotate Right button
        JButton rotateRightButton = new JButton("⟶");
        rotateRightButton.setToolTipText("Rotate pattern one step right");
        rotateRightButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        rotateRightButton.setMargin(new Insets(2, 2, 2, 2));
        rotateRightButton.addActionListener(e -> {
            sequencer.rotatePatternRight();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                Commands.PATTERN_UPDATED,
                sequencer,
                null
            );
        });

        // Add buttons to rotation panel
        rotationPanel.add(rotateLeftButton);
        rotationPanel.add(rotateRightButton);
        
        parentPanel.add(rotationPanel);
    }
    
    /**
     * Create clear button
     * @return The created button
     */
    private JButton createClearButton() {
        JButton clearButton = new JButton("🗑️");
        clearButton.setToolTipText("Clear pattern");
        clearButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        clearButton.setMargin(new Insets(2, 2, 2, 2));
        clearButton.addActionListener(e -> {
            sequencer.clearPattern();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                Commands.PATTERN_UPDATED,
                sequencer,
                null
            );
        });
        
        return clearButton;
    }
    
    /**
     * Create root note controls
     */
    private void createRootNoteControls(JPanel parentPanel) {
        JPanel rootNotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0
        rootNotePanel.add(new JLabel("Root:"));

        String[] noteNames = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        rootNoteCombo = new JComboBox<>(noteNames);
        rootNoteCombo.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        rootNoteCombo.setToolTipText("Set the root note");
        rootNoteCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !updatingUI) {
                String rootNote = (String) e.getItem();
                sequencer.setRootNote(rootNote);

                // Publish event for other listeners
                CommandBus.getInstance().publish(
                    Commands.ROOT_NOTE_SELECTED,
                    this,
                    rootNote
                );

                logger.info("Root note selected: {}", rootNote);
            }
        });
        
        rootNotePanel.add(rootNoteCombo);
        
        parentPanel.add(rootNotePanel);
    }
    
    /**
     * Create quantize checkbox
     */
    private void createQuantizeControls(JPanel parentPanel) {
        quantizeCheckbox = new JCheckBox("Q", true);
        quantizeCheckbox.setToolTipText("Quantize notes to scale");
        quantizeCheckbox.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        quantizeCheckbox.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.setQuantizeEnabled(quantizeCheckbox.isSelected());
            }
        });
        
        parentPanel.add(quantizeCheckbox);
    }
    
    /**
     * Create scale combo control
     */
    private void createScaleControls(JPanel parentPanel) {
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0
        scalePanel.add(new JLabel("Scale:"));

        String[] scaleNames = Scale.getScales();
        scaleCombo = new JComboBox<>(scaleNames);
        scaleCombo.setPreferredSize(new Dimension(120, CONTROL_HEIGHT));
        scaleCombo.setToolTipText("Set the scale");
        scaleCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !updatingUI) {
                String selectedScale = (String) e.getItem();
                sequencer.setScale(selectedScale);

                // Publish event with sequencer ID to indicate which sequencer it's for
                CommandBus.getInstance().publish(
                    Commands.SCALE_SELECTED,
                    this,
                    new MelodicScaleSelectionEvent(sequencer.getId(), selectedScale)
                );

                logger.info("Scale selected for sequencer {}: {}", sequencer.getId(), selectedScale);
            }
        });
        
        scalePanel.add(scaleCombo);
        
        parentPanel.add(scalePanel);
    }
    
    /**
     * Create octave controls
     */
    private void createOctaveControls(JPanel parentPanel) {
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Octave:"));

        octaveDownBtn = new JButton("-");
        octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveDownBtn.setFocusable(false);
        octaveDownBtn.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        octaveDownBtn.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.decrementOctaveShift();
                updateOctaveLabel();
            }
        });
        octaveDownBtn.setToolTipText("Lower the octave");

        octaveLabel = new JLabel("0");
        octaveLabel.setPreferredSize(new Dimension(20, 20));
        octaveLabel.setHorizontalAlignment(JLabel.CENTER);

        octaveUpBtn = new JButton("+");
        octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveUpBtn.setFocusable(false);
        octaveUpBtn.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        octaveUpBtn.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.incrementOctaveShift();
                updateOctaveLabel();
            }
        });
        octaveUpBtn.setToolTipText("Raise the octave");

        octavePanel.add(octaveDownBtn);
        octavePanel.add(octaveLabel);
        octavePanel.add(octaveUpBtn);
        
        parentPanel.add(octavePanel);
    }
    
    /**
     * Update the octave label to show current octave shift
     */
    private void updateOctaveLabel() {
        if (octaveLabel != null && sequencer != null) {
            octaveLabel.setText(Integer.toString(sequencer.getOctaveShift()));
        }
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
            
            // Update root note
            String rootNote = sequencer.getRootNote() != null ? sequencer.getRootNote().toString() : null;
            if (rootNote != null) {
                rootNoteCombo.setSelectedItem(rootNote);
            }
            
            // Update scale
            String scale = sequencer.getScale();
            if (scale != null) {
                scaleCombo.setSelectedItem(scale);
            }
            
            // Update quantize
            quantizeCheckbox.setSelected(sequencer.isQuantizeEnabled());
            
            // Update octave
            updateOctaveLabel();
            
        } finally {
            // Reset flag after updates
            updatingUI = false;
        }
    }

    /**
     * Set the selected scale (without triggering events)
     */ 
    public void setSelectedScale(String scale) {
        if (scale != null && scaleCombo != null) {
            updatingUI = true;
            try {
                scaleCombo.setSelectedItem(scale);
            } finally {
                updatingUI = false;
            }
        }
    }
}
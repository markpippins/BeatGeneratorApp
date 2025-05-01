package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.event.MelodicScaleSelectionEvent;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.Scale;

/**
 * Panel containing sequence parameters for melodic sequencers
 */
public class MelodicSequencerScalePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerScalePanel.class);

    // Declare fields for all UI controls
    private JComboBox<String> rootNoteCombo;
    private JComboBox<String> scaleCombo;
    private JToggleButton quantizeToggle;
    private JSpinner octaveSpinner;

    // Reference to sequencer - should be marked as final 
    private final MelodicSequencer sequencer;
    
    // Flag to prevent event loops
    private boolean updatingUI = false;
    
    /**
     * Create a new scale panel
     * 
     * @param sequencer The melodic sequencer to control
     */
    public MelodicSequencerScalePanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
    }
    
    /**
     * Initialize the panel with all controls
     */
    private void initialize() {
        // Use compact titled border
        // setBorder(BorderFactory.createTitledBorder(
        //     BorderFactory.createLineBorder(Color.GRAY),
        //     "Scale Parameters",
        //     TitledBorder.DEFAULT_JUSTIFICATION,
        //     TitledBorder.DEFAULT_POSITION,
        //     null,
        //     null
        // ));
        
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Scale Parameters"),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)));

        // Use BorderLayout for the main panel instead of FlowLayout
        setLayout(new BorderLayout(0, 0));
        
        // Reduce spacing in the controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        
        createQuantizeControls(controlsPanel);
        createRootNoteControls(controlsPanel);
        createScaleControls(controlsPanel);
        createOctaveControls(controlsPanel);
        
        // Add the controls panel to the CENTER of the BorderLayout
        add(controlsPanel, BorderLayout.CENTER);
    }
    
    /**
     * Create root note controls
     */
    private void createRootNoteControls(JPanel parentPanel) {
        JPanel rootNotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        rootNotePanel.add(new JLabel("Root:"));

        String[] noteNames = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        rootNoteCombo = new JComboBox<>(noteNames);
        rootNoteCombo.setPreferredSize(new Dimension(UIUtils.MEDIUM_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        rootNoteCombo.setToolTipText("Set the root note");
        rootNoteCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !updatingUI) {
                String rootNote = (String) e.getItem();
                sequencer.getSequenceData().setRootNote(rootNote);

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
     * Create quantize toggle
     */
    private void createQuantizeControls(JPanel parentPanel) {
        quantizeToggle = new JToggleButton("Q", true);
        quantizeToggle.setToolTipText("Quantize notes to scale");
        quantizeToggle.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH - 2, UIUtils.CONTROL_HEIGHT));
        quantizeToggle.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.setQuantizeEnabled(quantizeToggle.isSelected());
            }
        });
        
        parentPanel.add(quantizeToggle);
    }
    
    /**
     * Create scale combo control
     */
    private void createScaleControls(JPanel parentPanel) {
        String[] scaleNames = Scale.getScales();
        scaleCombo = new JComboBox<>(scaleNames);
        scaleCombo.setPreferredSize(new Dimension((UIUtils.LARGE_CONTROL_WIDTH * 2) - 10, UIUtils.CONTROL_HEIGHT));
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
        
        parentPanel.add(scaleCombo);
    }
    
    /**
     * Create octave controls
     */
    private void createOctaveControls(JPanel parentPanel) {
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Octave:"));

        // Create spinner for octave selection
        SpinnerNumberModel octaveModel = new SpinnerNumberModel(4, -6, 6, 1);
        octaveSpinner = new JSpinner(octaveModel);
        octaveSpinner.setPreferredSize(new Dimension(50, 25));
        
        // Add change listener that uses setOctaveShift instead of setOctave
        octaveSpinner.addChangeListener(e -> {
            if (!updatingUI) {
                int octave = (Integer) octaveSpinner.getValue();
                // Use setOctaveShift instead of setOctave
                sequencer.setOctaveShift(octave);
                logger.debug("Set octave shift to: {}", octave);
            }
        });
        
        octavePanel.add(octaveSpinner);
        parentPanel.add(octavePanel);
    }
    
    /**
     * Update UI with sequencer state
     */
    public void updateUI(MelodicSequencer sequencer) {
        if (sequencer == null) {
            return;
        }
        
        // Instead, just use the passed parameter directly
        updatingUI = true;
        try {
            // Update root note
            if (rootNoteCombo != null) {
                rootNoteCombo.setSelectedItem(sequencer.getSelectedRootNote());
            }
            
            // Update scale
            if (scaleCombo != null) {
                scaleCombo.setSelectedItem(sequencer.getScale());
            }
            
            // Update octave - use getOctaveShift instead of getOctave
            if (octaveSpinner != null) {
                octaveSpinner.setValue(sequencer.getOctaveShift());
            }
            
            // Update quantize toggle
            if (quantizeToggle != null) {
                quantizeToggle.setSelected(sequencer.isQuantizeEnabled());
            }
            
        } finally {
            // Reset flag after updates
            updatingUI = false;
        }
    }
    
    /**
     * Set the selected scale without triggering events
     */
    public void setSelectedScale(String scale) {
        if (scaleCombo != null && scale != null) {
            updatingUI = true;
            try {
                scaleCombo.setSelectedItem(scale);
            } finally {
                updatingUI = false;
            }
        }
    }
}
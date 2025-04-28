package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.sequencer.MelodicSequencer;

/**
 * Grid panel for melodic sequencer with step controls
 */
public class MelodicSequencerGridPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerGridPanel.class);
    
    // UI state variables
    private List<TriggerButton> triggerButtons = new ArrayList<>();
    private List<Dial> noteDials = new ArrayList<>();
    private List<Dial> velocityDials = new ArrayList<>();
    private List<Dial> gateDials = new ArrayList<>();
    private List<Dial> probabilityDials = new ArrayList<>();
    private List<Dial> nudgeDials = new ArrayList<>();
    
    // Reference to sequencer
    private final MelodicSequencer sequencer;
    
    // Flag to prevent recursive updates
    private boolean listenersEnabled = true;
    
    /**
     * Create a new melodic sequencer grid panel
     * @param sequencer The melodic sequencer to control
     */
    public MelodicSequencerGridPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
    }
    
    /**
     * Initialize the grid panel
     */
    private void initialize() {
        // REDUCED: from 5,0 to 2,0
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        // REDUCED: from 10,10,10,10 to 5,5,5,5
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setPreferredSize(new Dimension(getPreferredSize().width, 400));
        setMinimumSize(new Dimension(800, 700));

        // Create panel for the 16 columns (fix the initialization)
        JPanel sequencePanel = new JPanel();
        sequencePanel.setLayout(new BoxLayout(sequencePanel, BoxLayout.X_AXIS));
        
        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }
        
        // Add the sequence panel to the main panel
        add(sequencePanel);
    }
    
    /**
     * Create a column for one step in the sequence
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        // REDUCED: from 5,2,5,2 to 2,1,2,1
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            // Make label more compact with smaller padding
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            // Set consistent font size
            label.setFont(label.getFont().deriveFont(11f));

            if (i < 4) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 4 ? new NoteSelectionDial() : new Dial();

            // Store the dial in the appropriate collection based on its type
            switch (i) {
                case 0 -> {
                    velocityDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("velocity"));
                }
                case 1 -> {
                    gateDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("gate"));
                }
                case 4 -> {
                    dial.setPreferredSize(new Dimension(75, 75));
                    noteDials.add(dial);
                }
                case 2 -> {
                    dial.setMinimum(0);
                    dial.setMaximum(100);
                    dial.setValue(100); // Default to 100%
                    dial.setKnobColor(UIUtils.getDialColor("probability"));
                    dial.addChangeListener(e -> {
                        if (!listenersEnabled)
                            return;
                        sequencer.setProbabilityValue(index, dial.getValue());
                    });
                    probabilityDials.add(dial);
                }
                case 3 -> {
                    dial.setMinimum(0);
                    dial.setMaximum(250);
                    dial.setValue(0); // Default to no nudge
                    dial.setKnobColor(UIUtils.getDialColor("nudge"));
                    dial.addChangeListener(e -> {
                        if (!listenersEnabled)
                            return;
                        sequencer.setNudgeValue(index, dial.getValue());
                    });
                    nudgeDials.add(dial);
                }
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally with minimal padding
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        // REDUCED: from 0,5 to 0,2
        column.add(Box.createRigidArea(new Dimension(0, 2)));

        // Make trigger button more compact
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setPreferredSize(new Dimension(20, 20)); // Smaller size
        triggerButton.setToggleable(true);

        triggerButton.addActionListener(e -> {
            boolean isSelected = triggerButton.isSelected();
            // Get existing step data
            int note = noteDials.get(index).getValue();
            int velocity = velocityDials.get(index).getValue();
            int gate = gateDials.get(index).getValue();
            int probability = probabilityDials.get(index).getValue();
            int nudge = nudgeDials.get(index).getValue();
            // Update sequencer pattern data
            sequencer.setStepData(index, isSelected, note, velocity, gate, probability, nudge);
        });

        triggerButtons.add(triggerButton);
        // Compact panel for trigger button
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        noteDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton.isSelected(),
                    noteDials.get(index).getValue(), velocityDials.get(index).getValue(),
                    gateDials.get(index).getValue(), probabilityDials.get(index).getValue(),
                    nudgeDials.get(index).getValue());
        });

        velocityDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton.isSelected(),
                    noteDials.get(index).getValue(), velocityDials.get(index).getValue(),
                    gateDials.get(index).getValue(), probabilityDials.get(index).getValue(),
                    nudgeDials.get(index).getValue());
        });

        gateDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton.isSelected(),
                    noteDials.get(index).getValue(), velocityDials.get(index).getValue(),
                    gateDials.get(index).getValue(), probabilityDials.get(index).getValue(),
                    nudgeDials.get(index).getValue());
        });

        return column;
    }
    
    /**
     * Get the label for a knob based on its type
     */
    private String getKnobLabel(int i) {
        return i == 0 ? "Velocity" : i == 1 ? "Gate" : i == 2 ? "Probability" : i == 3 ? "Nudge" : "Note";
    }
    
    /**
     * Update step highlighting based on sequence position
     */
    public void updateStepHighlighting(int oldStep, int newStep) {
        // Un-highlight old step
        if (oldStep >= 0 && oldStep < triggerButtons.size()) {
            triggerButtons.get(oldStep).setHighlighted(false);
            triggerButtons.get(oldStep).repaint();
        }
        
        // Highlight new step with color based on position
        if (newStep >= 0 && newStep < triggerButtons.size()) {
            Color highlightColor;
            
            if (newStep < 16) {
                // First 16 steps - orange highlight
                highlightColor = UIUtils.fadedOrange;
            } else if (newStep < 32) {
                // Steps 17-32
                highlightColor = UIUtils.coolBlue;
            } else if (newStep < 48) {
                // Steps 33-48
                highlightColor = UIUtils.deepNavy;
            } else {
                // Steps 49-64
                highlightColor = UIUtils.mutedOlive;
            }
            
            triggerButtons.get(newStep).setHighlighted(true);
            triggerButtons.get(newStep).setHighlightColor(highlightColor);
            triggerButtons.get(newStep).repaint();
        }
    }
    
    /**
     * Synchronize all UI elements with the current sequencer state
     */
    public void syncWithSequencer() {
        listenersEnabled = false;
        try {
            // Update trigger buttons
            List<Boolean> activeSteps = sequencer.getActiveSteps();
            for (int i = 0; i < Math.min(triggerButtons.size(), activeSteps.size()); i++) {
                triggerButtons.get(i).setSelected(activeSteps.get(i));
            }
            
            // Update note dials
            List<Integer> notes = sequencer.getNotes();
            for (int i = 0; i < Math.min(noteDials.size(), notes.size()); i++) {
                noteDials.get(i).setValue(notes.get(i));
            }
            
            // Update velocity dials
            List<Integer> velocities = sequencer.getVelocities();
            for (int i = 0; i < Math.min(velocityDials.size(), velocities.size()); i++) {
                velocityDials.get(i).setValue(velocities.get(i));
            }
            
            // Update gate dials
            List<Integer> gates = sequencer.getGates();
            for (int i = 0; i < Math.min(gateDials.size(), gates.size()); i++) {
                gateDials.get(i).setValue(gates.get(i));
            }
            
            // Update probability dials
            List<Integer> probabilities = sequencer.getProbabilities();
            for (int i = 0; i < Math.min(probabilityDials.size(), probabilities.size()); i++) {
                probabilityDials.get(i).setValue(probabilities.get(i));
            }
            
            // Update nudge dials
            List<Integer> nudges = sequencer.getNudges();
            for (int i = 0; i < Math.min(nudgeDials.size(), nudges.size()); i++) {
                nudgeDials.get(i).setValue(nudges.get(i));
            }
            
        } finally {
            listenersEnabled = true;
        }
        
        // Force revalidate and repaint
        revalidate();
        repaint();
    }
    
    /**
     * Get the trigger buttons
     */
    public List<TriggerButton> getTriggerButtons() {
        return triggerButtons;
    }
}
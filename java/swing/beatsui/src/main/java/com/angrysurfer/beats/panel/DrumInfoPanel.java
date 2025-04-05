package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;

public class DrumInfoPanel extends JPanel {
    private JLabel titleLabel;
    private JLabel drumNameLabel;
    private JLabel noteLabel;
    private JLabel velocityLabel;
    private JLabel patternUsageLabel;

    // Add controls for per-drum parameters
    private JSpinner lastStepSpinner;
    private JComboBox<Direction> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JCheckBox loopCheckbox;

    private DrumSequencer sequencer;
    private boolean updatingUI = false;  // Flag to prevent feedback loops
    private int currentDrumIndex = 0;

    public DrumInfoPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        // Use a BorderLayout for the panel
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createTitledBorder("Selected Drum"));
        
        // Create top panel for drum info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Create labels
        titleLabel = new JLabel("Pad: -");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        drumNameLabel = new JLabel("Name: -");
        noteLabel = new JLabel("Note: -");
        velocityLabel = new JLabel("Velocity: -");
        patternUsageLabel = new JLabel("Pattern: 0/16");
        
        // Add info labels horizontally
        infoPanel.add(titleLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(drumNameLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(noteLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(velocityLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(patternUsageLabel);
        
        // Create parameters panel
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Drum Parameters"));
        
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 5, 5, 5);
        
        // Last Step parameter
        gc.gridx = 0;
        gc.gridy = 0;
        paramsPanel.add(new JLabel("Last Step:"), gc);
        
        gc.gridx = 1;
        lastStepSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 64, 1));
        lastStepSpinner.setPreferredSize(new Dimension(60, 25));
        lastStepSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!updatingUI && currentDrumIndex >= 0) {
                    int value = (Integer) lastStepSpinner.getValue();
                    sequencer.setPatternLength(currentDrumIndex, value);
                    updatePatternUsageLabel();
                }
            }
        });
        paramsPanel.add(lastStepSpinner, gc);
        
        // Direction parameter
        gc.gridx = 2;
        paramsPanel.add(new JLabel("Direction:"), gc);
        
        gc.gridx = 3;
        directionCombo = new JComboBox<>(Direction.values());
        directionCombo.setPreferredSize(new Dimension(100, 25));
        directionCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!updatingUI && currentDrumIndex >= 0) {
                    Direction dir = (Direction) directionCombo.getSelectedItem();
                    sequencer.setDirection(currentDrumIndex, dir);
                }
            }
        });
        paramsPanel.add(directionCombo, gc);
        
        // Timing parameter
        gc.gridx = 0;
        gc.gridy = 1;
        paramsPanel.add(new JLabel("Timing:"), gc);
        
        gc.gridx = 1;
        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(100, 25));
        timingCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!updatingUI && currentDrumIndex >= 0) {
                    TimingDivision timing = (TimingDivision) timingCombo.getSelectedItem();
                    sequencer.setTimingDivision(currentDrumIndex, timing);
                }
            }
        });
        paramsPanel.add(timingCombo, gc);
        
        // Loop parameter
        gc.gridx = 2;
        paramsPanel.add(new JLabel("Loop:"), gc);
        
        gc.gridx = 3;
        loopCheckbox = new JCheckBox();
        loopCheckbox.setSelected(true);
        loopCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!updatingUI && currentDrumIndex >= 0) {
                    boolean loop = loopCheckbox.isSelected();
                    sequencer.setLooping(currentDrumIndex, loop);
                }
            }
        });
        paramsPanel.add(loopCheckbox, gc);
        
        // Add panels to main layout
        add(infoPanel, BorderLayout.NORTH);
        add(paramsPanel, BorderLayout.CENTER);
        
        // Initial update with the default selected pad
        updateInfo(sequencer.getSelectedPadIndex());
    }
    
    public void updateInfo(int drumPadIndex) {
        if (drumPadIndex < 0) return;
        
        currentDrumIndex = drumPadIndex;
        updatingUI = true;  // Prevent event feedback
        
        try {
            Strike strike = sequencer.getStrike(drumPadIndex);
            
            if (strike == null) {
                titleLabel.setText("Pad: " + (drumPadIndex + 1));
                drumNameLabel.setText("Name: Not assigned");
                noteLabel.setText("Note: -");
                velocityLabel.setText("Velocity: -");
            } else {
                titleLabel.setText("Pad: " + (drumPadIndex + 1));
                drumNameLabel.setText("Name: " + (strike.getName() != null ? strike.getName() : "Unnamed"));
                noteLabel.setText("Note: " + strike.getRootNote());
                velocityLabel.setText("Velocity: " + strike.getLevel());
            }
            
            // Update the pattern usage
            updatePatternUsageLabel();
            
            // Update parameter controls
            lastStepSpinner.setValue(sequencer.getPatternLength(drumPadIndex));
            directionCombo.setSelectedItem(sequencer.getDirection(drumPadIndex));
            timingCombo.setSelectedItem(sequencer.getTimingDivision(drumPadIndex));
            loopCheckbox.setSelected(sequencer.isLooping(drumPadIndex));
            
        } finally {
            updatingUI = false;  // Re-enable event handling
        }
        
        repaint();
    }
    
    private void updatePatternUsageLabel() {
        // Count how many steps are active for this drum pad
        boolean[][] patterns = sequencer.getPatterns();
        int activeSteps = 0;
        int patternLength = sequencer.getPatternLength(currentDrumIndex);
        
        for (int step = 0; step < patternLength; step++) {
            if (patterns[currentDrumIndex][step]) {
                activeSteps++;
            }
        }
        
        patternUsageLabel.setText("Pattern: " + activeSteps + "/" + patternLength);
    }
}
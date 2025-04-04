package com.angrysurfer.beats.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumSequencer;

public class DrumInfoPanel extends JPanel {
    private JLabel titleLabel;
    private JLabel drumNameLabel;
    private JLabel noteLabel;
    private JLabel velocityLabel;
    private JLabel patternUsageLabel;

    private DrumSequencer sequencer;

    public DrumInfoPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        
        // Use FlowLayout for horizontal arrangement
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createTitledBorder("Selected Drum"));
        
        // Create labels
        titleLabel = new JLabel("Pad: -");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        drumNameLabel = new JLabel("Name: -");
        noteLabel = new JLabel("Note: -");
        velocityLabel = new JLabel("Velocity: -");
        patternUsageLabel = new JLabel("Pattern: 0/16");
        
        // Add components horizontally
        add(titleLabel);
        add(new JLabel("|"));
        add(drumNameLabel);
        add(new JLabel("|"));
        add(noteLabel);
        add(new JLabel("|"));
        add(velocityLabel);
        add(new JLabel("|"));
        add(patternUsageLabel);
        
        // Adjust preferred size for horizontal layout
        setPreferredSize(new Dimension(500, 50));
        
        // Initial update with the default selected pad
        updateInfo(sequencer.getSelectedPadIndex());
    }
    
    public void updateInfo(int drumPadIndex) {
        System.out.println("DrumInfoPanel.updateInfo(" + drumPadIndex + ")"); // Debug
        
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
        
        // Count how many steps are active for this drum pad
        boolean[][] patterns = sequencer.getPatterns();
        int activeSteps = 0;
        for (int step = 0; step < sequencer.getPatternLength(); step++) {
            if (patterns[drumPadIndex][step]) {
                activeSteps++;
            }
        }
        
        patternUsageLabel.setText("Pattern: " + activeSteps + "/" + 
                                  sequencer.getPatternLength());
        
        repaint();
    }
}
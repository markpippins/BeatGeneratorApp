package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
public class SequencerPanel extends JPanel {
    
    // UI Components
    private final List<TriggerButton> triggerButtons = new ArrayList<>();
    private final List<NoteSelectionDial> noteDials = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();
    
    // Sequence parameters
    private JSpinner lastStepSpinner;
    private JCheckBox loopCheckbox;
    private int patternLength = 16;
    private boolean isLooping = true;
    
    // Direction parameters
    public enum Direction {
        FORWARD, BACKWARD, BOUNCE, RANDOM
    }
    private Direction currentDirection = Direction.FORWARD;
    private boolean bounceForward = true; // Used for bounce direction to track current direction
    private JComboBox<String> directionCombo;

    // Timing parameters
    public enum TimingDivision {
        NORMAL("Normal", 4),           // Standard 4 steps per beat
        DOUBLE("Double Time", 8),      // Twice as fast (8 steps per beat)
        HALF("Half Time", 2),          // Half as fast (2 steps per beat) 
        TRIPLET("Triplets", 3),        // Triplet timing (3 steps per beat)
        EIGHTH_TRIPLET("1/8 Triplets", 6),  // Eighth note triplets (6 steps per beat)
        SIXTEENTH("1/16 Notes", 16),        // Sixteenth notes (16 steps per beat)
        SIXTEENTH_TRIPLET("1/16 Triplets", 12); // Sixteenth note triplets (12 steps per beat)
        
        private final String displayName;
        private final int stepsPerBeat;
        
        TimingDivision(String displayName, int stepsPerBeat) {
            this.displayName = displayName;
            this.stepsPerBeat = stepsPerBeat;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getStepsPerBeat() {
            return stepsPerBeat;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    private TimingDivision timingDivision = TimingDivision.NORMAL;
    private JComboBox<TimingDivision> timingCombo;

    // Callback for playing notes
    private Consumer<NoteEvent> noteEventConsumer;

    // Callback support for timing changes
    private Consumer<TimingDivision> timingChangeListener;

    /**
     * Create a new SequencerPanel
     * 
     * @param noteEventConsumer Callback for when a note should be played
     */
    public SequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        this.noteEventConsumer = noteEventConsumer;
        initialize();
    }
    
    /**
     * Initialize the panel
     */
    private void initialize() {
        // Add sequence parameters panel at the top
        JPanel sequenceParamsPanel = createSequenceParametersPanel();
        add(sequenceParamsPanel, BorderLayout.NORTH);
        
        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create panel for sequence parameters (last step, loop, etc.)
     */
    private JPanel createSequenceParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        
        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last Step:"));
        
        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(60, 25));
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            System.out.println("Last step set to: " + lastStep);
            
            // Update pattern length
            patternLength = lastStep;
        });
        lastStepPanel.add(lastStepSpinner);
        
        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Direction:"));
        
        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(100, 25));
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            switch (selectedIndex) {
                case 0: currentDirection = Direction.FORWARD; break;
                case 1: currentDirection = Direction.BACKWARD; break;
                case 2: 
                    currentDirection = Direction.BOUNCE; 
                    bounceForward = true; // Reset bounce direction when selected
                    break;
                case 3: currentDirection = Direction.RANDOM; break;
            }
            System.out.println("Direction set to: " + currentDirection);
        });
        directionPanel.add(directionCombo);
        
        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));
        
        timingCombo = new JComboBox<>(TimingDivision.values());
        timingCombo.setPreferredSize(new Dimension(110, 25));
        timingCombo.addActionListener(e -> {
            TimingDivision selected = (TimingDivision) timingCombo.getSelectedItem();
            timingDivision = selected;
            System.out.println("Timing set to: " + selected + " (" + selected.getStepsPerBeat() + " steps per beat)");
            
            // Notify listeners that timing has changed
            if (timingChangeListener != null) {
                timingChangeListener.accept(selected);
            }
        });
        timingPanel.add(timingCombo);
        
        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            boolean looping = loopCheckbox.isSelected();
            System.out.println("Loop set to: " + looping);
            
            // Update looping state
            isLooping = looping;
        });
        
        // Add components to panel - reorganize for better fit
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.add(lastStepPanel);
        topRow.add(directionPanel);
        topRow.add(loopCheckbox);
        
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bottomRow.add(timingPanel);
        
        // Use box layout for the main panel to stack rows
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(topRow);
        panel.add(bottomRow);
        
        return panel;
    }
    
    /**
     * Create a column for the sequencer
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 3 knobs
        for (int i = 0; i < 3; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (i > 0) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                // Add label to the column
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 0 ? new NoteSelectionDial() : new Dial();

            // Store the dial in the appropriate collection based on its type
            switch (i) {
            case 0:
                noteDials.add((NoteSelectionDial) dial); // Store the note dial for this column
                break;

            case 1:
                velocityDials.add(dial); // Store the velocity dial
                break;

            case 2:
                gateDials.add(dial); // Store the gate dial
                break;
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }
        // Add small spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add the trigger button - make it a toggle button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));

        // Make it toggleable
        triggerButton.setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton.addActionListener(e -> {
            // No need to manually toggle - JToggleButton handles it automatically
            System.out.println("Trigger " + index + " is now " + (triggerButton.isSelected() ? "ON" : "OFF"));
        });

        triggerButtons.add(triggerButton);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = new DrumButton();
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        // Add action to manually trigger the note when pad button is clicked
        padButton.addActionListener(e -> {
            if (index < noteDials.size()) {
                // Get note from dial
                NoteSelectionDial noteDial = noteDials.get(index);
                int noteValue = noteDial.getValue();

                // Get velocity
                int velocity = 127; // Full velocity for manual triggers
                if (index < velocityDials.size()) {
                    velocity = (int) Math.round(velocityDials.get(index).getValue() * 1.27);
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time
                int gateTime = 250; // Longer gate time for manual triggers
                if (index < gateDials.size()) {
                    gateTime = (int) Math.round(50 + gateDials.get(index).getValue() * 4.5);
                }

                // Trigger the note through the callback
                if (noteEventConsumer != null) {
                    noteEventConsumer.accept(new NoteEvent(noteValue, velocity, gateTime));
                }
            }
        });

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }
    
    /**
     * Update the sequencer step indicator
     * 
     * @param oldStep Previous step
     * @param newStep New step
     * @return Whether a note should be played
     */
    public NoteEvent updateStep(int oldStep, int newStep) {
        // Clear previous step highlight
        if (oldStep >= 0 && oldStep < triggerButtons.size()) {
            TriggerButton oldButton = triggerButtons.get(oldStep);
            oldButton.setHighlighted(false);
        }

        // Highlight current step
        if (newStep >= 0 && newStep < triggerButtons.size()) {
            TriggerButton newButton = triggerButtons.get(newStep);
            newButton.setHighlighted(true);
            
            // Check if a note should be played
            if (newButton.isSelected() && newStep < noteDials.size()) {
                // Get note value
                NoteSelectionDial noteDial = noteDials.get(newStep);
                int noteValue = noteDial.getValue();

                // Get velocity from velocity dial
                int velocity = 100; // Default
                if (newStep < velocityDials.size()) {
                    // Scale dial value (0-100) to MIDI velocity range (0-127)
                    velocity = (int) Math.round(velocityDials.get(newStep).getValue() * 1.27);
                    // Ensure it's within valid MIDI range
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time from gate dial
                int gateTime = 100; // Default (ms)
                if (newStep < gateDials.size()) {
                    // Scale dial value (0-100) to reasonable gate times (10-500ms)
                    gateTime = (int) Math.round(10 + gateDials.get(newStep).getValue() * 4.9);
                }
                
                return new NoteEvent(noteValue, velocity, gateTime);
            }
        }
        
        return null; // No note to play
    }
    
    /**
     * Reset the sequencer
     */
    public void reset() {
        // Clear all highlights when stopped
        for (TriggerButton button : triggerButtons) {
            button.setHighlighted(false);
        }
    }
    
    /**
     * Get the maximum pattern length
     */
    public int getPatternLength() {
        return patternLength;
    }
    
    /**
     * Check if the sequencer is in loop mode
     */
    public boolean isLooping() {
        return isLooping;
    }
    
    /**
     * Get the current direction
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }
    
    /**
     * Check if bounce is forward
     */
    public boolean isBounceForward() {
        return bounceForward;
    }
    
    /**
     * Set bounce direction
     */
    public void setBounceForward(boolean forward) {
        this.bounceForward = forward;
    }
    
    /**
     * Get the knob label for a specific index
     */
    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }
    
    /**
     * Class to represent a note event
     */
    public static class NoteEvent {
        private final int note;
        private final int velocity;
        private final int durationMs;
        
        public NoteEvent(int note, int velocity, int durationMs) {
            this.note = note;
            this.velocity = velocity;
            this.durationMs = durationMs;
        }
        
        public int getNote() {
            return note;
        }
        
        public int getVelocity() {
            return velocity;
        }
        
        public int getDurationMs() {
            return durationMs;
        }
    }

    public void setTimingChangeListener(Consumer<TimingDivision> listener) {
        this.timingChangeListener = listener;
    }

    public TimingDivision getTimingDivision() {
        return timingDivision;
    }
}
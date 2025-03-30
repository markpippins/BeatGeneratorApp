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
import java.util.Hashtable;
import java.util.function.BiConsumer;

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
import java.awt.event.ItemEvent;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.model.Scale;
import com.angrysurfer.core.util.Quantizer;

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

    // Scale and quantization parameters
    private String selectedRootNote = "C";
    private String selectedScale = "Chromatic";
    private JComboBox<String> scaleCombo;
    private Quantizer quantizer;
    private Boolean[] currentScaleNotes;
    private boolean quantizeEnabled = true;
    private JCheckBox quantizeCheckbox;
    private JComboBox<String> rootNoteCombo;

    // Octave shift parameters
    private int octaveShift = 0;  // Current octave shift (can be negative)
    private JLabel octaveLabel;   // Label to show current octave

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
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last:"));
        
        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(50, 25));
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            System.out.println("Last step set to: " + lastStep);
            
            // Update pattern length
            patternLength = lastStep;
        });
        lastStepPanel.add(lastStepSpinner);
        
        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Dir:"));
        
        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
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
        timingCombo.setPreferredSize(new Dimension(90, 25));
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
        
        // Octave shift controls
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Oct:"));
        
        // Down button
        JButton octaveDownBtn = new JButton("-");
        octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveDownBtn.setFocusable(false);
        octaveDownBtn.addActionListener(e -> {
            if (octaveShift > -3) {  // Limit to -3 octaves
                octaveShift--;
                updateOctaveLabel();
            }
        });
        
        // Up button
        JButton octaveUpBtn = new JButton("+");
        octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveUpBtn.setFocusable(false);
        octaveUpBtn.addActionListener(e -> {
            if (octaveShift < 3) {  // Limit to +3 octaves
                octaveShift++;
                updateOctaveLabel();
            }
        });
        
        // Label showing current octave
        octaveLabel = new JLabel("0");
        octaveLabel.setPreferredSize(new Dimension(20, 20));
        octaveLabel.setHorizontalAlignment(JLabel.CENTER);
        
        octavePanel.add(octaveDownBtn);
        octavePanel.add(octaveLabel);
        octavePanel.add(octaveUpBtn);
        
        // Root Note combo
        JPanel rootNotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rootNotePanel.add(new JLabel("Root:"));
        
        // Create root note selector
        rootNoteCombo = createRootNoteCombo();
        rootNoteCombo.setPreferredSize(new Dimension(50, 25));
        rootNotePanel.add(rootNoteCombo);
        
        // Scale selection panel
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        scalePanel.add(new JLabel("Scale:"));
        
        // Add scale selector (similar to SessionControlPanel)
        scaleCombo = createScaleCombo();
        scaleCombo.setPreferredSize(new Dimension(120, 25));
        scalePanel.add(scaleCombo);
        
        // Quantize checkbox
        quantizeCheckbox = new JCheckBox("Quantize", true);
        quantizeCheckbox.addActionListener(e -> {
            quantizeEnabled = quantizeCheckbox.isSelected();
            System.out.println("Quantize set to: " + quantizeEnabled);
        });
        
        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            boolean looping = loopCheckbox.isSelected();
            System.out.println("Loop set to: " + looping);
            
            // Update looping state
            isLooping = looping;
        });
        
        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);
        panel.add(octavePanel);     // Add the octave panel
        panel.add(rootNotePanel);
        panel.add(scalePanel);
        panel.add(quantizeCheckbox);
        panel.add(loopCheckbox);
        
        // Initialize quantizer with chromatic scale
        updateQuantizer();
        
        return panel;
    }

    /**
     * Updates the octave label to show current octave shift
     */
    private void updateOctaveLabel() {
        String prefix = octaveShift > 0 ? "+" : "";
        octaveLabel.setText(prefix + octaveShift);
        System.out.println("Octave shift: " + octaveShift);
    }

    /**
     * Create a combo box with all available scales
     */
    private JComboBox<String> createScaleCombo() {
        String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic");
        
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedScale = (String) combo.getSelectedItem();
                updateQuantizer();
                System.out.println("Scale set to: " + selectedScale);
            }
        });

        return combo;
    }

    /**
     * Create a combo box with all available root notes
     */
    private JComboBox<String> createRootNoteCombo() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        
        JComboBox<String> combo = new JComboBox<>(noteNames);
        combo.setSelectedItem("C"); // Default to C
        
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedRootNote = (String) combo.getSelectedItem();
                updateQuantizer();
                System.out.println("Root note set to: " + selectedRootNote);
            }
        });
        
        return combo;
    }

    /**
     * Update the quantizer based on selected root note and scale
     */
    private void updateQuantizer() {
        try {
            currentScaleNotes = Scale.getScale(selectedRootNote, selectedScale);
            quantizer = new Quantizer(currentScaleNotes);
            System.out.println("Quantizer updated for " + selectedRootNote + " " + selectedScale);
        } catch (Exception e) {
            System.err.println("Error creating quantizer: " + e.getMessage());
            // Default to chromatic scale if there's an error
            Boolean[] chromaticScale = new Boolean[12];
            for (int i = 0; i < 12; i++) {
                chromaticScale[i] = true;
            }
            currentScaleNotes = chromaticScale;
            quantizer = new Quantizer(currentScaleNotes);
        }
    }

    /**
     * Set the root note for scale quantization
     */
    public void setRootNote(String rootNote) {
        this.selectedRootNote = rootNote;
        if (rootNoteCombo != null) {
            rootNoteCombo.setSelectedItem(rootNote);
        } else {
            // If UI not created yet, just update the internal state
            updateQuantizer();
        }
    }

    /**
     * Sets the selected scale in the scale combo box
     * @param scaleName The name of the scale to select
     */
    public void setSelectedScale(String scaleName) {
        if (scaleCombo != null) {
            scaleCombo.setSelectedItem(scaleName);
        }
    }

    /**
     * Quantize a note to the current scale
     * 
     * @param note The MIDI note number to quantize
     * @return The quantized MIDI note number
     */
    private int quantizeNote(int note) {
        if (quantizer != null && quantizeEnabled) {
            return quantizer.quantizeNote(note);
        }
        return note; // Return original note if quantizer not available or quantization disabled
    }

    /**
     * Apply octave shift to a note after quantization
     * 
     * @param note The note to apply octave shift to
     * @return The shifted note
     */
    private int applyOctaveShift(int note) {
        // Add 12 semitones per octave
        int shiftedNote = note + (octaveShift * 12);
        
        // Ensure the note is within valid MIDI range (0-127)
        return Math.max(0, Math.min(127, shiftedNote));
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
                
                // Apply quantization if enabled
                int quantizedNote = quantizeNote(noteValue);

                // Apply octave shift
                int shiftedNote = applyOctaveShift(quantizedNote);

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
                    noteEventConsumer.accept(new NoteEvent(shiftedNote, velocity, gateTime));
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
                
                // Apply quantization if enabled
                int quantizedNote = quantizeNote(noteValue);

                // Apply octave shift
                int shiftedNote = applyOctaveShift(quantizedNote);

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
                
                return new NoteEvent(shiftedNote, velocity, gateTime);
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
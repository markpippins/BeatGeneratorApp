package com.angrysurfer.beats.panel;
// package com.angrysurfer.beats.widget.panel;

// import java.awt.BorderLayout;
// import java.awt.Color;
// import java.awt.Component;
// import java.awt.Dimension;
// import java.awt.FlowLayout;
// import java.awt.GridLayout;
// import java.awt.event.ItemEvent;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.function.Consumer;
// import java.util.logging.Logger;

// import javax.swing.BorderFactory;
// import javax.swing.Box;
// import javax.swing.BoxLayout;
// import javax.swing.JButton;
// import javax.swing.JCheckBox;
// import javax.swing.JComboBox;
// import javax.swing.JLabel;
// import javax.swing.JPanel;
// import javax.swing.JScrollPane;
// import javax.swing.JSpinner;
// import javax.swing.SpinnerNumberModel;
// import javax.swing.SwingUtilities;
// import javax.swing.border.EmptyBorder;

// import com.angrysurfer.beats.widget.Dial;
// import com.angrysurfer.beats.widget.DrumButton;
// import com.angrysurfer.beats.widget.NoteSelectionDial;
// import com.angrysurfer.beats.widget.TriggerButton;
// import com.angrysurfer.core.model.Scale;
// import com.angrysurfer.core.util.Quantizer;

// /**
//  * A sequencer panel with X0X-style step sequencing capabilities
//  */
// public class DrumSequencerPanel2 extends JPanel {

//     private static final Logger logger = Logger.getLogger(DrumSequencerPanel2.class.getName());

//     // UI Components
//     private final List<TriggerButton> triggerButtons = new ArrayList<>();
//     private final List<Dial> velocityDials = new ArrayList<>();
//     private final List<Dial> decayDials = new ArrayList<>();
//     private final List<Dial> cutoffDials = new ArrayList<>();
//     private final List<Dial> resonanceDials = new ArrayList<>();

//     // Sequence parameters
//     private JSpinner lastStepSpinner;
//     private JCheckBox loopCheckbox;
//     private int patternLength = 16;
//     private boolean isLooping = true;

//     // Direction parameters
//     public enum Direction {
//         FORWARD, BACKWARD, BOUNCE, RANDOM
//     }
//     private Direction currentDirection = Direction.FORWARD;
//     private boolean bounceForward = true; // Used for bounce direction to track current direction
//     private JComboBox<String> directionCombo;

//     // Timing parameters
//     public enum TimingDivision {
//         NORMAL("Normal", 4), // Standard 4 steps per beat
//         DOUBLE("Double Time", 8), // Twice as fast (8 steps per beat)
//         HALF("Half Time", 2), // Half as fast (2 steps per beat) 
//         TRIPLET("Triplets", 3), // Triplet timing (3 steps per beat)
//         EIGHTH_TRIPLET("1/8 Triplets", 6), // Eighth note triplets (6 steps per beat)
//         SIXTEENTH("1/16 Notes", 16), // Sixteenth notes (16 steps per beat)
//         SIXTEENTH_TRIPLET("1/16 Triplets", 12); // Sixteenth note triplets (12 steps per beat)

//         private final String displayName;
//         private final int stepsPerBeat;

//         TimingDivision(String displayName, int stepsPerBeat) {
//             this.displayName = displayName;
//             this.stepsPerBeat = stepsPerBeat;
//         }

//         public String getDisplayName() {
//             return displayName;
//         }

//         public int getStepsPerBeat() {
//             return stepsPerBeat;
//         }

//         @Override
//         public String toString() {
//             return displayName;
//         }
//     }

//     private TimingDivision timingDivision = TimingDivision.NORMAL;
//     private JComboBox<TimingDivision> timingCombo;

//     // Callback for playing notes
//     private Consumer<NoteEvent> noteEventConsumer;

//     // Callback support for timing changes
//     private Consumer<TimingDivision> timingChangeListener;

//     // Scale and quantization parameters
//     private String selectedRootNote = "C";
//     private String selectedScale = "Chromatic";
//     private JComboBox<String> scaleCombo;
//     private Quantizer quantizer;
//     private Boolean[] currentScaleNotes;
//     private boolean quantizeEnabled = true;
//     private JCheckBox quantizeCheckbox;
//     private JComboBox<String> rootNoteCombo;

//     // Octave shift parameters
//     private int octaveShift = 0;  // Current octave shift (can be negative)
//     private JLabel octaveLabel;   // Label to show current octave

//     private JComboBox<String> rangeCombo;

//     // Two-dimensional array to hold patterns for each drum pad
//     private boolean[][] patterns; // [drumPad][step]
//     private int[][] velocities; // [drumPad][step]
//     private int[][] decays; // [drumPad][step]
//     private int[][] cutoffs; // [drumPad][step]
//     private int[][] resonances; // [drumPad][step]

//     // Additional arrays for each drum pad
//     private int[] lastSteps; // Last step for each drum pad
//     private Direction[] directions; // Direction for each drum pad
//     private boolean[] loops; // Loop state for each drum pad
//     private TimingDivision[] timingDivisions; // Timing for each drum pad

//     private int selectedPadIndex;
    

//     /**
//      * Create a new SequencerPanel
//      *
//      * @param noteEventConsumer Callback for when a note should be played
//      */
//     public DrumSequencerPanel2(Consumer<NoteEvent> noteEventConsumer) {
//         super(new BorderLayout());
//         this.noteEventConsumer = noteEventConsumer;

//         // Initialize the arrays
//         patterns = new boolean[DRUM_PAD_COUNT][PATTERN_LENGTH];
//         velocities = new int[DRUM_PAD_COUNT][PATTERN_LENGTH];
//         decays = new int[DRUM_PAD_COUNT][PATTERN_LENGTH];
//         cutoffs = new int[DRUM_PAD_COUNT][PATTERN_LENGTH];
//         resonances = new int[DRUM_PAD_COUNT][PATTERN_LENGTH];

//         lastSteps = new int[DRUM_PAD_COUNT]; // Initialize last steps
//         directions = new Direction[DRUM_PAD_COUNT]; // Initialize directions
//         loops = new boolean[DRUM_PAD_COUNT]; // Initialize loop states
//         timingDivisions = new TimingDivision[DRUM_PAD_COUNT]; // Initialize timing divisions

//         // Set default values
//         for (int i = 0; i < DRUM_PAD_COUNT; i++) {
//             lastSteps[i] = 0; // Default last step
//             directions[i] = Direction.FORWARD; // Default direction
//             loops[i] = true; // Default loop state
//             timingDivisions[i] = TimingDivision.NORMAL; // Default timing division
//         }

//         // Create and add the drum pads panel
//         JPanel drumPadsPanel = createDrumPadsPanel();
//         add(drumPadsPanel, BorderLayout.NORTH); // Add to the top of the panel

//         // Create and add the trigger buttons row
//         JPanel triggerButtonsRow = createTriggerButtonsRow();
//         add(triggerButtonsRow, BorderLayout.CENTER); // Add to the center of the panel

//         // Create and add the drum buttons row
//         JPanel drumButtonsRow = createDrumButtonsRow();
//         add(drumButtonsRow, BorderLayout.SOUTH); // Add to the bottom of the panel

//         initialize();
//     }

//     /**
//      * Initialize the panel
//      */
//     private void initialize() {

//         setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

//         // Add sequence parameters panel at the top
//         JPanel sequenceParamsPanel = createSequenceParametersPanel();
//         add(sequenceParamsPanel, BorderLayout.NORTH);

//         // Create panel for the 16 columns
//         JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
//         sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

//         // Create 16 columns
//         for (int i = 0; i < 16; i++) {
//             JPanel columnPanel = createSequenceColumn(i);
//             sequencePanel.add(columnPanel);
//         }

//         // Wrap in scroll pane in case window gets too small
//         JScrollPane scrollPane = new JScrollPane(sequencePanel);
//         scrollPane.setBorder(null);
//         scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//         scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

//         add(scrollPane, BorderLayout.CENTER);
//     }

//     /**
//      * Create panel for sequence parameters (last step, loop, etc.)
//      */
//     private JPanel createSequenceParametersPanel() {
//         JPanel panel = new JPanel();
//         panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
//         panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

//         // Last Step spinner
//         JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//         lastStepPanel.add(new JLabel("Last:"));

//         // Create spinner model with range 1-16, default 16
//         SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
//         lastStepSpinner = new JSpinner(lastStepModel);
//         lastStepSpinner.setPreferredSize(new Dimension(50, 25));
//         lastStepSpinner.addChangeListener(e -> {
//             int lastStep = (Integer) lastStepSpinner.getValue();
//             System.out.println("Last step set to: " + lastStep);

//             // Update pattern length
//             patternLength = lastStep;
//         });
//         lastStepPanel.add(lastStepSpinner);

//         // Direction combo
//         JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//         directionPanel.add(new JLabel("Dir:"));

//         directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
//         directionCombo.setPreferredSize(new Dimension(90, 25));
//         directionCombo.addActionListener(e -> {
//             int selectedIndex = directionCombo.getSelectedIndex();
//             switch (selectedIndex) {
//                 case 0:
//                     currentDirection = Direction.FORWARD;
//                     break;
//                 case 1:
//                     currentDirection = Direction.BACKWARD;
//                     break;
//                 case 2:
//                     currentDirection = Direction.BOUNCE;
//                     bounceForward = true; // Reset bounce direction when selected
//                     break;
//                 case 3:
//                     currentDirection = Direction.RANDOM;
//                     break;
//             }
//             System.out.println("Direction set to: " + currentDirection);
//         });
//         directionPanel.add(directionCombo);

//         // Timing division combo
//         JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//         timingPanel.add(new JLabel("Timing:"));

//         timingCombo = new JComboBox<>(TimingDivision.values());
//         timingCombo.setPreferredSize(new Dimension(90, 25));
//         timingCombo.addActionListener(e -> {
//             TimingDivision selected = (TimingDivision) timingCombo.getSelectedItem();
//             timingDivision = selected;
//             System.out.println("Timing set to: " + selected + " (" + selected.getStepsPerBeat() + " steps per beat)");

//             // Notify listeners that timing has changed
//             if (timingChangeListener != null) {
//                 timingChangeListener.accept(selected);
//             }
//         });
//         timingPanel.add(timingCombo);

//         // Octave shift controls
//         JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
//         octavePanel.add(new JLabel("Oct:"));

//         // Down button
//         JButton octaveDownBtn = new JButton("-");
//         octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
//         octaveDownBtn.setFocusable(false);
//         octaveDownBtn.addActionListener(e -> {
//             if (octaveShift > -3) {  // Limit to -3 octaves
//                 octaveShift--;
//                 updateOctaveLabel();
//             }
//         });

//         // Up button
//         JButton octaveUpBtn = new JButton("+");
//         octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
//         octaveUpBtn.setFocusable(false);
//         octaveUpBtn.addActionListener(e -> {
//             if (octaveShift < 3) {  // Limit to +3 octaves
//                 octaveShift++;
//                 updateOctaveLabel();
//             }
//         });

//         // Label showing current octave
//         octaveLabel = new JLabel("0");
//         octaveLabel.setPreferredSize(new Dimension(20, 20));
//         octaveLabel.setHorizontalAlignment(JLabel.CENTER);

//         octavePanel.add(octaveDownBtn);
//         octavePanel.add(octaveLabel);
//         octavePanel.add(octaveUpBtn);

//         // Root Note combo
//         JPanel rootNotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//         rootNotePanel.add(new JLabel("Root:"));

//         // Create root note selector
//         rootNoteCombo = createRootNoteCombo();
//         rootNoteCombo.setPreferredSize(new Dimension(50, 25));
//         rootNotePanel.add(rootNoteCombo);

//         // Scale selection panel
//         JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//         scalePanel.add(new JLabel("Scale:"));

//         // Add scale selector (similar to SessionControlPanel)
//         scaleCombo = createScaleCombo();
//         scaleCombo.setPreferredSize(new Dimension(120, 25));
//         scalePanel.add(scaleCombo);

//         // Quantize checkbox
//         quantizeCheckbox = new JCheckBox("Quantize", true);
//         quantizeCheckbox.addActionListener(e -> {
//             quantizeEnabled = quantizeCheckbox.isSelected();
//             System.out.println("Quantize set to: " + quantizeEnabled);
//         });

//         // Loop checkbox
//         loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
//         loopCheckbox.addActionListener(e -> {
//             boolean looping = loopCheckbox.isSelected();
//             System.out.println("Loop set to: " + looping);

//             // Update looping state
//             isLooping = looping;
//         });

//         JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
//         rangeCombo = new JComboBox<>(new String[]{"1", "2", "3", "4"});
//         generatePanel.add(rangeCombo);
//         JButton clearBtn = new JButton("Clear");
//         clearBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
//         clearBtn.setFocusable(false);
//         clearBtn.addActionListener(e -> {
//             clearPattern();
//         });

//         JButton generateBtn = new JButton("Generate");
//         generateBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
//         generateBtn.setFocusable(false);
//         generateBtn.addActionListener(e -> {
//             generatePattern();
//         });

//         generatePanel.add(clearBtn);
//         generatePanel.add(generateBtn);

//         // Add all components to panel in a single row
//         panel.add(lastStepPanel);
//         panel.add(directionPanel);
//         panel.add(timingPanel);
//         panel.add(octavePanel);     // Add the octave panel
//         panel.add(rootNotePanel);
//         panel.add(scalePanel);
//         panel.add(quantizeCheckbox);
//         panel.add(loopCheckbox);
//         panel.add(generatePanel);

//         // Initialize quantizer with chromatic scale
//         updateQuantizer();

//         return panel;
//     }

//     private void clearPattern() {
//         logger.info("Clearing pattern - resetting all controls");
        
//         // Clear all dials and buttons
//         for (int i = 0; i < velocityDials.size(); i++) {
//             velocityDials.get(i).setValue(70); // 70% velocity is a good default
//         }
        
//         for (int i = 0; i < decayDials.size(); i++) {
//             decayDials.get(i).setValue(50); // 50% decay is a good default
//         }
        
//         for (int i = 0; i < cutoffDials.size(); i++) {
//             cutoffDials.get(i).setValue(50); // 50% cutoff is a good default
//         }
        
//         for (int i = 0; i < resonanceDials.size(); i++) {
//             resonanceDials.get(i).setValue(50); // 50% resonance is a good default
//         }
        
//         // Unselect all buttons
//         for (int i = 0; i < triggerButtons.size(); i++) {
//             triggerButtons.get(i).setSelected(false);
//             triggerButtons.get(i).setHighlighted(false);
//         }
        
//         // Force repaint to ensure UI updates
//         validate();
//         repaint();
//     }

//     private void generatePattern() {
//         // First clear the pattern to ensure clean state
//         clearPattern();
        
//         // Get selected octave range (1-4)
//         int octaveRange = Integer.parseInt((String) rangeCombo.getSelectedItem());
        
//         // Calculate note range based on octaves
//         int baseNote = 60 - ((octaveRange * 12) / 2); // Center around middle C (60)
//         int totalNoteRange = octaveRange * 12;
        
//         logger.info("Generating pattern with " + octaveRange + " octave range: " + 
//                    baseNote + " to " + (baseNote + totalNoteRange - 1));
        
//         // Process all steps
//         for (int i = 0; i < velocityDials.size(); i++) {
//             final int stepIndex = i;
            
//             // Only process steps within the current pattern length
//             if (i >= patternLength) continue;
            
//             // Randomly decide if this step should be active (70% chance)
//             boolean activateStep = Math.random() < 0.7;
            
//             if (activateStep) {
//                 // Activate the step
//                 triggerButtons.get(i).setSelected(true);
                
//                 // Calculate random note within the specified range
//                 // Force note range to be within reasonable MIDI bounds
//                 int randomNote = baseNote + (int)(Math.random() * totalNoteRange);
//                 randomNote = Math.max(24, Math.min(96, randomNote));
                
//                 // Quantize if enabled
//                 if (quantizeEnabled && quantizer != null) {
//                     randomNote = quantizeNote(randomNote);
//                 }
                
//                 // Generate random velocity and gate values
//                 int velocity = 40 + (int)(Math.random() * 60);  // 40-100
//                 int gate = 30 + (int)(Math.random() * 50);     // 30-80
                
//                 // Use SwingUtilities.invokeLater for all UI updates
//                 final int noteToSet = randomNote;
//                 final int velToSet = velocity;
//                 final int gateToSet = gate;
                
//                 SwingUtilities.invokeLater(() -> {
//                     try {
//                         // Set note value - try multiple approaches
//                         Dial velocityDial = velocityDials.get(stepIndex);
//                         logger.info("Setting dial " + stepIndex + " to note: " + noteToSet + 
//                                   " (current value: " + velocityDial.getValue() + ")");
                        
//                         // Direct setValue approach
//                         velocityDial.setValue(velToSet, true); // Adjust for octave shift
                        
//                         // Verify it was set
//                         logger.info("After setValue: dial " + stepIndex + " note = " + 
//                                    velocityDial.getValue());
                        
//                         // Set velocity and gate
//                         if (stepIndex < decayDials.size()) {
//                             decayDials.get(stepIndex).setValue(gateToSet);
//                         }
                        
//                         if (stepIndex < cutoffDials.size()) {
//                             cutoffDials.get(stepIndex).setValue(gateToSet);
//                         }
                        
//                         if (stepIndex < resonanceDials.size()) {
//                             resonanceDials.get(stepIndex).setValue(gateToSet);
//                         }
//                     } catch (Exception ex) {
//                         logger.warning("Error at step " + stepIndex + ": " + ex.getMessage());
//                         ex.printStackTrace();
//                     }
//                 });
//             }
//         }
        
//         // Final UI refresh to ensure all components display correctly
//         SwingUtilities.invokeLater(() -> {
//             for (Dial dial : velocityDials) {
//                 dial.repaint();
//             }
//             for (Dial dial : decayDials) {
//                 dial.repaint();
//             }
//             for (Dial dial : cutoffDials) {
//                 dial.repaint();
//             }
//             for (Dial dial : resonanceDials) {
//                 dial.repaint();
//             }
//             validate();
//             repaint();
//         });
//     }

//     /**
//      * Updates the octave label to show current octave shift
//      */
//     private void updateOctaveLabel() {
//         String prefix = octaveShift > 0 ? "+" : "";
//         octaveLabel.setText(prefix + octaveShift);
//         System.out.println("Octave shift: " + octaveShift);
//     }

//     /**
//      * Create a combo box with all available scales
//      */
//     private JComboBox<String> createScaleCombo() {
//         String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
//                 .stream()
//                 .sorted()
//                 .toArray(String[]::new);

//         JComboBox<String> combo = new JComboBox<>(scaleNames);
//         combo.setSelectedItem("Chromatic");

//         combo.addItemListener(e -> {
//             if (e.getStateChange() == ItemEvent.SELECTED) {
//                 selectedScale = (String) combo.getSelectedItem();
//                 updateQuantizer();
//                 System.out.println("Scale set to: " + selectedScale);
//             }
//         });

//         return combo;
//     }

//     /**
//      * Create a combo box with all available root notes
//      */
//     private JComboBox<String> createRootNoteCombo() {
//         String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

//         JComboBox<String> combo = new JComboBox<>(noteNames);
//         combo.setSelectedItem("C"); // Default to C

//         combo.addItemListener(e -> {
//             if (e.getStateChange() == ItemEvent.SELECTED) {
//                 selectedRootNote = (String) combo.getSelectedItem();
//                 updateQuantizer();
//                 System.out.println("Root note set to: " + selectedRootNote);
//             }
//         });

//         return combo;
//     }

//     /**
//      * Update the quantizer based on selected root note and scale
//      */
//     private void updateQuantizer() {
//         try {
//             currentScaleNotes = Scale.getScale(selectedRootNote, selectedScale);
//             quantizer = new Quantizer(currentScaleNotes);
//             System.out.println("Quantizer updated for " + selectedRootNote + " " + selectedScale);
//         } catch (Exception e) {
//             System.err.println("Error creating quantizer: " + e.getMessage());
//             // Default to chromatic scale if there's an error
//             Boolean[] chromaticScale = new Boolean[12];
//             for (int i = 0; i < 12; i++) {
//                 chromaticScale[i] = true;
//             }
//             currentScaleNotes = chromaticScale;
//             quantizer = new Quantizer(currentScaleNotes);
//         }
//     }

//     /**
//      * Set the root note for scale quantization
//      */
//     public void setRootNote(String rootNote) {
//         this.selectedRootNote = rootNote;
//         if (rootNoteCombo != null) {
//             rootNoteCombo.setSelectedItem(rootNote);
//         } else {
//             // If UI not created yet, just update the internal state
//             updateQuantizer();
//         }
//     }

//     /**
//      * Sets the selected scale in the scale combo box
//      *
//      * @param scaleName The name of the scale to select
//      */
//     public void setSelectedScale(String scaleName) {
//         if (scaleCombo != null) {
//             scaleCombo.setSelectedItem(scaleName);
//         }
//     }

//     /**
//      * Quantize a note to the current scale
//      *
//      * @param note The MIDI note number to quantize
//      * @return The quantized MIDI note number
//      */
//     private int quantizeNote(int note) {
//         if (quantizer != null && quantizeEnabled) {
//             return quantizer.quantizeNote(note);
//         }
//         return note; // Return original note if quantizer not available or quantization disabled
//     }

//     /**
//      * Apply octave shift to a note after quantization
//      *
//      * @param note The note to apply octave shift to
//      * @return The shifted note
//      */
//     private int applyOctaveShift(int note) {
//         // Add 12 semitones per octave
//         int shiftedNote = note + (octaveShift * 12);

//         // Ensure the note is within valid MIDI range (0-127)
//         return Math.max(0, Math.min(127, shiftedNote));
//     }

//     /**
//      * Create a column for the sequencer
//      */
//     private JPanel createSequenceColumn(int index) {
//         JPanel column = new JPanel();
//         column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
//         column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

//         // Create dials for each parameter
//         Dial velocityDial = new Dial();
//         Dial decayDial = new Dial();
//         Dial cutoffDial = new Dial();
//         Dial resonanceDial = new Dial();

//         // Store the dials in their respective lists
//         velocityDials.add(velocityDial);
//         decayDials.add(decayDial);
//         cutoffDials.add(cutoffDial);
//         resonanceDials.add(resonanceDial);

//         // Add dials to the column
//         column.add(createDialPanel("Velocity", velocityDial));
//         column.add(createDialPanel("Decay", decayDial));
//         column.add(createDialPanel("Cutoff", cutoffDial));
//         column.add(createDialPanel("Resonance", resonanceDial));

//         // Create and add the trigger button
//         TriggerButton triggerButton = new TriggerButton("Pad " + (index + 1));
//         triggerButton.addActionListener(e -> {
//             // Logic to handle pad selection and pattern update
//             updatePatternForPad(index);
//         });
//         triggerButtons.add(triggerButton);
//         column.add(triggerButton);

//         return column;
//     }

//     // Method to update the pattern for the selected drum pad
//     private void updatePatternForPad(int padIndex) {
//         // Logic to refresh the trigger buttons and show the current pattern
//         // Highlight the selected pad and update the UI accordingly
//         for (int i = 0; i < triggerButtons.size(); i++) {
//             triggerButtons.get(i).setHighlighted(i == padIndex);
//         }
//         // Refresh the dials based on the current pattern for the selected pad
//         refreshDialsForPad(padIndex);
//     }

//     // Method to refresh dials based on the current pattern
//     private void refreshDialsForPad(int padIndex) {
//         for (int step = 0; step < PATTERN_LENGTH; step++) {
//             // Update dials based on the current pattern
//             velocityDials.get(padIndex).setValue(velocities[padIndex][step]);
//             decayDials.get(padIndex).setValue(decays[padIndex][step]);
//             cutoffDials.get(padIndex).setValue(cutoffs[padIndex][step]);
//             resonanceDials.get(padIndex).setValue(resonances[padIndex][step]);
//         }
//     }

//     /**
//      * Update the sequencer step indicator
//      *
//      * @param oldStep Previous step
//      * @param newStep New step
//      * @return Whether a note should be played
//      */
//     public NoteEvent updateStep(int oldStep, int newStep) {
//         // Clear previous step highlight
//         if (oldStep >= 0 && oldStep < triggerButtons.size()) {
//             TriggerButton oldButton = triggerButtons.get(oldStep);
//             oldButton.setHighlighted(false);
//         }

//         // Highlight current step
//         if (newStep >= 0 && newStep < triggerButtons.size()) {
//             TriggerButton newButton = triggerButtons.get(newStep);
//             newButton.setHighlighted(true);

//             // Check if a note should be played
//             if (newButton.isSelected() && newStep < velocityDials.size()) {
//                 // Get note value
//                 Dial velocityDial = velocityDials.get(newStep);
//                 int noteValue = (int) velocityDial.getValue();

//                 // Apply quantization if enabled
//                 int quantizedNote = quantizeNote(noteValue);

//                 // Apply octave shift
//                 int shiftedNote = applyOctaveShift(quantizedNote);

//                 // Get velocity from velocity dial
//                 int velocity = 100; // Default
//                 if (newStep < velocityDials.size()) {
//                     // Scale dial value (0-100) to MIDI velocity range (0-127)
//                     velocity = (int) Math.round(velocityDials.get(newStep).getValue() * 1.27);
//                     // Ensure it's within valid MIDI range
//                     velocity = Math.max(1, Math.min(127, velocity));
//                 }

//                 // Get gate time from gate dial
//                 int gateTime = 100; // Default (ms)
//                 if (newStep < decayDials.size()) {
//                     // Scale dial value (0-100) to reasonable gate times (10-500ms)
//                     gateTime = (int) Math.round(10 + decayDials.get(newStep).getValue() * 4.9);
//                 }

//                 return new NoteEvent(shiftedNote, velocity, gateTime);
//             }
//         }

//         return null; // No note to play
//     }

//     /**
//      * Reset the sequencer
//      */
//     public void reset() {
//         // Clear all highlights when stopped
//         for (TriggerButton button : triggerButtons) {
//             button.setHighlighted(false);
//         }
//     }

//     /**
//      * Get the maximum pattern length
//      */
//     public int getPatternLength() {
//         return patternLength;
//     }

//     /**
//      * Check if the sequencer is in loop mode
//      */
//     public boolean isLooping() {
//         return isLooping;
//     }

//     /**
//      * Get the current direction
//      */
//     public Direction getCurrentDirection() {
//         return currentDirection;
//     }

//     /**
//      * Check if bounce is forward
//      */
//     public boolean isBounceForward() {
//         return bounceForward;
//     }

//     /**
//      * Set bounce direction
//      */
//     public void setBounceForward(boolean forward) {
//         this.bounceForward = forward;
//     }

//     /**
//      * Get the knob label for a specific index
//      */
//     private String getKnobLabel(int i) {
//         return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
//     }

//     /**
//      * Class to represent a note event
//      */
//     public static class NoteEvent {

//         private final int note;
//         private final int velocity;
//         private final int durationMs;

//         public NoteEvent(int note, int velocity, int durationMs) {
//             this.note = note;
//             this.velocity = velocity;
//             this.durationMs = durationMs;
//         }

//         public int getNote() {
//             return note;
//         }

//         public int getVelocity() {
//             return velocity;
//         }

//         public int getDurationMs() {
//             return durationMs;
//         }
//     }

//     public void setTimingChangeListener(Consumer<TimingDivision> listener) {
//         this.timingChangeListener = listener;
//     }

//     public TimingDivision getTimingDivision() {
//         return timingDivision;
//     }

//     private JPanel createDialPanel(String label, Dial dial) {
//         JPanel panel = new JPanel();
//         panel.setLayout(new FlowLayout(FlowLayout.LEFT));
//         panel.add(new JLabel(label));
//         panel.add(dial);
//         return panel;
//     }

//     // Method to update the last step for a specific drum pad
//     public void setLastStep(int padIndex, int step) {
//         if (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) {
//             lastSteps[padIndex] = step;
//         }
//     }

//     // Method to get the last step for a specific drum pad
//     public int getLastStep(int padIndex) {
//         return (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) ? lastSteps[padIndex] : 0;
//     }

//     // Method to set the direction for a specific drum pad
//     public void setDirection(int padIndex, Direction direction) {
//         if (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) {
//             directions[padIndex] = direction;
//         }
//     }

//     // Method to get the direction for a specific drum pad
//     public Direction getDirection(int padIndex) {
//         return (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) ? directions[padIndex] : Direction.FORWARD;
//     }

//     // Method to set the loop state for a specific drum pad
//     public void setLoop(int padIndex, boolean loop) {
//         if (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) {
//             loops[padIndex] = loop;
//         }
//     }

//     // Method to get the loop state for a specific drum pad
//     public boolean isLooping(int padIndex) {
//         return (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) ? loops[padIndex] : true;
//     }

//     // Method to set the timing division for a specific drum pad
//     public void setTimingDivision(int padIndex, TimingDivision timingDivision) {
//         if (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) {
//             timingDivisions[padIndex] = timingDivision;
//         }
//     }

//     // Method to get the timing division for a specific drum pad
//     public TimingDivision getTimingDivision(int padIndex) {
//         return (padIndex >= 0 && padIndex < DRUM_PAD_COUNT) ? timingDivisions[padIndex] : TimingDivision.NORMAL;
//     }

//     // Add a method to create drum pads
//     private JPanel createDrumPadsPanel() {
//         JPanel drumPadsPanel = new JPanel();
//         drumPadsPanel.setLayout(new GridLayout(1, DRUM_PAD_COUNT)); // One row, multiple columns

//         for (int i = 0; i < DRUM_PAD_COUNT; i++) {
//             final int padIndex = i; // Final variable for use in lambda
//             TriggerButton drumPadButton = new TriggerButton("Pad " + (i + 1));
//             drumPadButton.addActionListener(e -> {
//                 // Logic to handle drum pad selection
//                 selectDrumPad(padIndex);
//             });
//             drumPadsPanel.add(drumPadButton);
//         }

//         return drumPadsPanel;
//     }

//     // Method to select a drum pad and update the UI
//     private void selectDrumPad(int padIndex) {
//         // Highlight the selected drum pad
//         for (int i = 0; i < triggerButtons.size(); i++) {
//             triggerButtons.get(i).setHighlighted(i == padIndex);
//         }

//         // Refresh the trigger buttons to show the current pattern for the selected drum pad
//         refreshTriggerButtonsForPad(padIndex);
//     }

//     // Method to refresh trigger buttons based on the current pattern
//     private void refreshTriggerButtonsForPad(int padIndex) {
//         for (int step = 0; step < PATTERN_LENGTH; step++) {
//             // Update the trigger buttons based on the current pattern
//             boolean isActive = patterns[padIndex][step];
//             triggerButtons.get(step).setHighlighted(isActive);
//         }
//     }

//     // Method to create a row of trigger buttons
//     private JPanel createTriggerButtonsRow() {
//         JPanel triggerButtonsPanel = new JPanel();
//         triggerButtonsPanel.setLayout(new GridLayout(1, PATTERN_LENGTH)); // One row, multiple columns

//         for (int i = 0; i < PATTERN_LENGTH; i++) {
//             final int stepIndex = i; // Final variable for use in lambda
//             TriggerButton triggerButton = new TriggerButton("Step " + (i + 1));
//             triggerButton.addActionListener(e -> {
//                 // Logic to handle step activation
//                 toggleStepForActivePad(stepIndex);
//             });
//             triggerButtonsPanel.add(triggerButton);
//         }

//         return triggerButtonsPanel;
//     }

//     // Method to create a row of drum buttons
//     private JPanel createDrumButtonsRow() {
//         JPanel drumButtonsPanel = new JPanel();
//         drumButtonsPanel.setLayout(new GridLayout(1, DRUM_PAD_COUNT)); // One row, multiple columns

//         for (int i = 0; i < DRUM_PAD_COUNT; i++) {
//             final int padIndex = i; // Final variable for use in lambda
//             TriggerButton drumButton = new TriggerButton("Pad " + (i + 1));
//             drumButton.addActionListener(e -> {
//                 // Logic to handle drum pad selection
//                 selectDrumPad(padIndex);
//             });
//             drumButtonsPanel.add(drumButton);
//         }

//         return drumButtonsPanel;
//     }

//     // Method to toggle the step for the active drum pad
//     private void toggleStepForActivePad(int stepIndex) {
//         // Get the currently selected drum pad index
//         int activePadIndex = getActivePadIndex(); // Implement this method to return the currently selected pad index

//         // Toggle the state of the specified step for the active pad
//         if (activePadIndex >= 0 && activePadIndex < DRUM_PAD_COUNT) {
//             patterns[activePadIndex][stepIndex] = !patterns[activePadIndex][stepIndex]; // Toggle the state
//             refreshTriggerButtonsForPad(activePadIndex); // Refresh the trigger buttons to reflect the change
//         }
//     }

//     // Method to get the currently active pad index (you may need to implement this)
//     private int getActivePadIndex() {
//         // Logic to determine which pad is currently active
//         // This could be based on a variable that tracks the selected pad
//         // For example, you might have a member variable `selectedPadIndex`
//         return selectedPadIndex; // Replace with your actual logic
//     }
// }

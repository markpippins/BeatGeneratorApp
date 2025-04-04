package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.Scale;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencerPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerPanel.class);

    private JCheckBox loopCheckbox;

    // CORE SEQUENCER - manages all sequencing logic
    private MelodicSequencer sequencer;

    // UI state variables - keep these in the panel
    private List<TriggerButton> triggerButtons = new ArrayList<>();
    private List<Dial> noteDials = new ArrayList<>();
    private List<Dial> velocityDials = new ArrayList<>();
    private List<Dial> gateDials = new ArrayList<>();

    // Labels and UI components
    private JLabel octaveLabel;
    private JComboBox<String> rootNoteCombo;
    private JComboBox<String> scaleCombo;
    private JComboBox<String> directionCombo;
    private JComboBox<String> rangeCombo;

    public MelodicSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer
        sequencer = new MelodicSequencer();

        // Set up the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);

        // Set up step update listener
        sequencer.setStepUpdateListener(event -> {
            updateStepHighlighting(event.getOldStep(), event.getNewStep());
        });

        // Initialize the UI
        initialize();

        // Register with command bus for UI updates
        CommandBus.getInstance().register(this);
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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

        // Initialize UI state from sequencer
        updateOctaveLabel();

        // Sync UI controls with sequencer state
        loopCheckbox.setSelected(sequencer.isLooping());
    }

    private JPanel createSequenceParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
        JSpinner lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(50, 25));
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            sequencer.setPatternLength(lastStep);
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        directionPanel.add(new JLabel("Dir:"));

        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(90, 25));
        directionCombo.addActionListener(e -> {
            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = switch (selectedIndex) {
                case 0 -> Direction.FORWARD;
                case 1 -> Direction.BACKWARD;
                case 2 -> Direction.BOUNCE;
                case 3 -> Direction.RANDOM;
                default -> Direction.FORWARD;
            };
            sequencer.setDirection(direction);
        });
        directionPanel.add(directionCombo);

        // Octave shift controls
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Oct:"));

        // Down button
        JButton octaveDownBtn = new JButton("-");
        octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveDownBtn.setFocusable(false);
        octaveDownBtn.addActionListener(e -> {
            sequencer.decrementOctaveShift();
            updateOctaveLabel();
        });

        // Up button
        JButton octaveUpBtn = new JButton("+");
        octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveUpBtn.setFocusable(false);
        octaveUpBtn.addActionListener(e -> {
            sequencer.incrementOctaveShift();
            updateOctaveLabel();
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
        JCheckBox quantizeCheckbox = new JCheckBox("Quantize", true);
        quantizeCheckbox.addActionListener(e -> {
            sequencer.setQuantizeEnabled(quantizeCheckbox.isSelected());
        });

        // Loop checkbox
        loopCheckbox = new JCheckBox("Loop", true); // Default to looping enabled
        loopCheckbox.addActionListener(e -> {
            sequencer.setLooping(loopCheckbox.isSelected());
        });

        // Range combo box
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.add(new JLabel("Range:"));

        // Create range combo with octave range options
        String[] rangeOptions = {"1 Octave", "2 Octaves", "3 Octaves", "4 Octaves"};
        rangeCombo = new JComboBox<>(rangeOptions);
        rangeCombo.setSelectedIndex(1); // Default to 2 octaves
        rangeCombo.setPreferredSize(new Dimension(90, 25));
        rangePanel.add(rangeCombo);

        // ADD BACK CLEAR AND GENERATE BUTTONS
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            sequencer.clearPattern();
            
            // Update UI after clearing pattern
            for (TriggerButton button : triggerButtons) {
                button.setSelected(false);
                button.repaint();
            }
            
            // Reset dials to default values
            for (Dial dial : noteDials) {
                dial.setValue(60); // Middle C
            }
            for (Dial dial : velocityDials) {
                dial.setValue(100);
            }
            for (Dial dial : gateDials) {
                dial.setValue(50);
            }
        });
        
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> {
            // Get selected octave range from the combo
            int octaveRange = rangeCombo.getSelectedIndex() + 1; // 1-4 octaves
            
            // Default settings: selected octave range, 50% density
            sequencer.generatePattern(octaveRange, 50);
            
            // Update UI to reflect the generated pattern
            syncUIWithSequencer();
        });

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(octavePanel);
        panel.add(rootNotePanel);
        panel.add(scalePanel);
        panel.add(quantizeCheckbox);
        panel.add(loopCheckbox);
        panel.add(rangePanel);       // Add Range combo before the buttons
        panel.add(clearButton);      // Add Clear button
        panel.add(generateButton);   // Add Generate button

        return panel;
    }

    private JComboBox<String> createScaleCombo() {
        String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic");

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedScale = (String) combo.getSelectedItem();
                sequencer.setScale(selectedScale);
                sequencer.updateQuantizer();
            }
        });

        return combo;
    }

    private JComboBox<String> createRootNoteCombo() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        JComboBox<String> combo = new JComboBox<>(noteNames);
        combo.setSelectedItem("C"); // Default to C

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedRootNote = (String) combo.getSelectedItem();
                sequencer.setRootNote(selectedRootNote);
                sequencer.updateQuantizer();
            }
        });

        return combo;
    }

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
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 0 ? new NoteSelectionDial() : new Dial();

            // Store the dial in the appropriate collection based on its type
            switch (i) {
                case 0 -> noteDials.add(dial);
                case 1 -> velocityDials.add(dial);
                case 2 -> gateDials.add(dial);
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
            boolean isSelected = triggerButton.isSelected();

            // Get existing step data
            int note = noteDials.get(index).getValue();
            int velocity = velocityDials.get(index).getValue();
            int gate = gateDials.get(index).getValue();

            // Update sequencer pattern data
            sequencer.setStepData(index, isSelected, note, velocity, gate);
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
                int noteValue = noteDials.get(index).getValue();

                // Apply quantization if enabled
                int quantizedNote = sequencer.quantizeNote(noteValue);

                // Apply octave shift
                int shiftedNote = sequencer.applyOctaveShift(quantizedNote);

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

                // Create note event
                NoteEvent noteEvent = new NoteEvent(shiftedNote, velocity, gateTime);
                
                // Pass to sequencer's note event listener directly
                if (sequencer.getNoteEventListener() != null) {
                    sequencer.getNoteEventListener().accept(noteEvent);
                } else {
                    // Fallback if no listener is set - log the error
                    logger.error("No note event listener set in sequencer");
                }
            }
        });

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    private void updateStepHighlighting(int oldStep, int newStep) {
        // Clear previous step highlight
        if (oldStep >= 0 && oldStep < triggerButtons.size()) {
            TriggerButton oldButton = triggerButtons.get(oldStep);
            oldButton.setHighlighted(false);
            oldButton.repaint();
        }

        // Highlight current step
        if (newStep >= 0 && newStep < triggerButtons.size()) {
            TriggerButton newButton = triggerButtons.get(newStep);
            newButton.setHighlighted(true);
            newButton.repaint();
        }
    }

    private void updateOctaveLabel() {
        if (octaveLabel != null) {
            octaveLabel.setText("Octave: " + sequencer.getOctaveShift());
        }
    }

    /**
     * Synchronizes all UI components with the current state of the sequencer
     */
    private void syncUIWithSequencer() {
        // Sync loop checkbox
        loopCheckbox.setSelected(sequencer.isLooping());
        
        // Sync direction combo
        for (int i = 0; i < directionCombo.getItemCount(); i++) {
            String item = directionCombo.getItemAt(i);
            Direction currentDirection = sequencer.getDirection();
            if (currentDirection != null && item.equalsIgnoreCase(currentDirection.toString())) {
                directionCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Sync scale settings
        rootNoteCombo.setSelectedItem(sequencer.getRootNote());
        scaleCombo.setSelectedItem(sequencer.getScale());
        
        // Update octave label
        updateOctaveLabel();
        
        // Sync step data
        for (int i = 0; i < triggerButtons.size(); i++) {
            if (i < sequencer.getActiveSteps().size()) {
                // Get step data from sequencer
                boolean isActive = sequencer.getActiveSteps().get(i);
                int noteValue = sequencer.getNoteValues().get(i);
                int velocityValue = sequencer.getVelocityValues().get(i);
                int gateValue = sequencer.getGateValues().get(i);
                
                // Update UI without triggering change listeners
                triggerButtons.get(i).setSelected(isActive);
                noteDials.get(i).setValue(noteValue);
                velocityDials.get(i).setValue(velocityValue);
                gateDials.get(i).setValue(gateValue);
            }
        }
        
        // Force repaint to ensure UI updates
        revalidate();
        repaint();
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.TRANSPORT_STOP -> {
                // Only update UI elements, sequencer handles its own state
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                    button.repaint();
                }
            }
            
            case Commands.TRANSPORT_START -> {
                // Make sure UI is in sync when transport starts
                syncUIWithSequencer();
            }
            
            // Handle sequencer step updates
            case Commands.SEQUENCER_STEP_UPDATE -> {
                if (action.getData() instanceof MelodicSequencer.StepUpdateEvent stepEvent) {
                    updateStepHighlighting(stepEvent.getOldStep(), stepEvent.getNewStep());
                }
            }

            // Handle other UI-specific commands as needed
            case Commands.NEW_VALUE_OCTAVE -> {
                if (action.getData() instanceof Integer octaveShift) {
                    sequencer.setOctaveShift(octaveShift);
                    updateOctaveLabel();
                }
            }
            
            // Add handling for scale and root note global changes
            case Commands.ROOT_NOTE_SELECTED -> {
                if (action.getData() instanceof String rootNote) {
                    rootNoteCombo.setSelectedItem(rootNote);
                    sequencer.setRootNote(rootNote);
                    sequencer.updateQuantizer();
                }
            }
            
            case Commands.SCALE_SELECTED -> {
                if (action.getData() instanceof String scaleName) {
                    scaleCombo.setSelectedItem(scaleName);
                    sequencer.setScale(scaleName);
                    sequencer.updateQuantizer();
                }
            }
        }
    }
}

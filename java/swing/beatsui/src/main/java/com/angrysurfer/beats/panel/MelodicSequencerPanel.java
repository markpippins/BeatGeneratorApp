package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.SwingUtilities;
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
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.InternalSynthManager;

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
    private JComboBox<TimingDivision> timingCombo;

    private boolean listenersEnabled = true;
    private boolean updatingUI = false;

    /**
     * Modify constructor to use only one step update mechanism (direct listener)
     */
    public MelodicSequencerPanel(Integer channel, Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer
        sequencer = new MelodicSequencer(channel);

        // Set up the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);

        // Set up step update listener with DIRECT callback (no CommandBus)
        sequencer.setStepUpdateListener(event -> {
            updateStepHighlighting(event.getOldStep(), event.getNewStep());
        });

        // Initialize the UI
        initialize();

        // Register with command bus for other UI updates (not step highlighting)
        CommandBus.getInstance().register(this);
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel paramsPanel = new JPanel(new BorderLayout());
        add(paramsPanel, BorderLayout.NORTH);

        // Add sequence parameters panel at the top

        paramsPanel.add(createSequenceParametersPanel(), BorderLayout.NORTH);


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


    /**
     * Create panel for sequence parameters (loop, direction, timing, etc.)
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
                case 0 ->
                    Direction.FORWARD;
                case 1 ->
                    Direction.BACKWARD;
                case 2 ->
                    Direction.BOUNCE;
                case 3 ->
                    Direction.RANDOM;
                default ->
                    Direction.FORWARD;
            };
            sequencer.setDirection(direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timingPanel.add(new JLabel("Timing:"));

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(90, 25));
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing division to {}", division);

                // Apply the timing division setting to the sequencer
                sequencer.setTimingDivision(division);
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
        loopCheckbox = new JCheckBox("🔁", true); // Default to looping enabled
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

            // Set density to 50% for now (could add a slider for this)
            int density = 50;

            // Generate pattern with selected parameters
            sequencer.generatePattern(octaveRange, density);

            // IMPORTANT: Update UI to match generated pattern
            syncUIWithSequencer();
        });

        // Add Preset selection combo box before Edit Sound button
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.add(new JLabel("Preset:"));
        
        // Create combo box for instrument presets
        JComboBox<String> presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(180, 25));
        
        // Populate with GM instrument presets from InternalSynthManager
        populatePresetCombo(presetCombo);
        
        presetCombo.addActionListener(e -> {
            if (!updatingUI) {
                String selectedItem = (String) presetCombo.getSelectedItem();
                if (selectedItem != null) {
                    // Parse preset number from format "0: Acoustic Grand Piano"
                    int presetNumber = parsePresetNumber(selectedItem);
                    
                    // Update the sequencer's note preset
                    if (sequencer.getNotePlayer() != null) {
                        sequencer.getNotePlayer().setPreset(presetNumber);
                        
                        // Apply preset change via instrument
                        if (sequencer.getNotePlayer().getInstrument() != null) {
                            try {
                                sequencer.getNotePlayer().getInstrument().programChange(
                                    sequencer.getNotePlayer().getChannel(), 
                                    presetNumber,
                                    0
                                );
                                logger.info("Changed preset to: {}", selectedItem);
                            } catch (Exception ex) {
                                logger.error("Failed to set preset: {}", ex.getMessage());
                            }
                        }
                    }
                }
            }
        });
        
        presetPanel.add(presetCombo);

        // Add Edit Player button
        JButton editPlayerButton = new JButton("Edit Sound");
        editPlayerButton.addActionListener(e -> {
            // Get the Note from the sequencer and publish edit request
            if (sequencer != null && sequencer.getNotePlayer() != null) {
                logger.info("Opening player editor for: {}", sequencer.getNotePlayer().getName());
                CommandBus.getInstance().publish(
                    Commands.PLAYER_SELECTED,
                    this,
                    sequencer.getNotePlayer()
                );

                CommandBus.getInstance().publish(
                    Commands.PLAYER_EDIT_REQUEST,
                    this,
                    sequencer.getNotePlayer()
                );
            } else {
                logger.warn("Cannot edit player - Note is not initialized");
            }
        });

        // Add all components to panel in a single row
        panel.add(lastStepPanel);
        panel.add(directionPanel);
        panel.add(timingPanel);      // Add Timing combo
        panel.add(octavePanel);
        panel.add(rootNotePanel);
        panel.add(scalePanel);
        panel.add(quantizeCheckbox);
        panel.add(loopCheckbox);
        panel.add(rangePanel);       // Add Range combo before the buttons
        panel.add(clearButton);      // Add Clear button
        panel.add(generateButton);   // Add Generate button
        panel.add(presetPanel);      // Add preset panel before Edit Sound button
        panel.add(editPlayerButton); // Add Edit Player button

        return panel;
    }

    /**
     * Populate the preset combo with General MIDI instrument names
     */
    private void populatePresetCombo(JComboBox<String> combo) {
        updatingUI = true;
        try {
            combo.removeAllItems();
            
            // Get the list of GM preset names from InternalSynthManager
            List<String> presetNames = InternalSynthManager.getInstance().getGeneralMIDIPresetNames();
            
            // Add each preset with its index
            for (int i = 0; i < presetNames.size(); i++) {
                combo.addItem(i + ": " + presetNames.get(i));
            }
            
            // Select current preset if available
            if (sequencer.getNotePlayer() != null && sequencer.getNotePlayer().getPreset() != null) {
                int currentPreset = sequencer.getNotePlayer().getPreset();
                if (currentPreset >= 0 && currentPreset < presetNames.size()) {
                    combo.setSelectedItem(currentPreset + ": " + presetNames.get(currentPreset));
                }
            }
        } finally {
            updatingUI = false;
        }
    }

    private int parsePresetNumber(String presetString) {
        try {
            return Integer.parseInt(presetString.split(":")[0].trim());
        } catch (NumberFormatException e) {
            logger.error("Failed to parse preset number from: {}", presetString);
            return -1;
        }
    }

    /**
     * Improve the createScaleCombo method to use Scale.getScales()
     */
    private JComboBox<String> createScaleCombo() {
        // Use the Scale class to get scale names instead of accessing SCALE_PATTERNS directly
        String[] scaleNames = Scale.getScales();

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic"); // Default to Chromatic

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !updatingUI) {
                String selectedScale = (String) e.getItem();
                sequencer.setScale(selectedScale);
                
                // Publish event for other listeners
                CommandBus.getInstance().publish(
                    Commands.SCALE_SELECTED,
                    this,
                    selectedScale
                );
                
                logger.info("Scale selected: {}", selectedScale);
            }
        });

        return combo;
    }

    /**
     * Improve the createRootNoteCombo method
     */
    private JComboBox<String> createRootNoteCombo() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        JComboBox<String> combo = new JComboBox<>(noteNames);
        combo.setSelectedItem("C"); // Default to C

        combo.addItemListener(e -> {
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

        return combo;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 3 knobs
        Dial[] noteDial = {null};
        Dial[] velocityDial = {null};
        Dial[] gateDial = {null};
        TriggerButton[] triggerButton = {null};

        for (int i = 0; i < 3; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            // label.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (i > 0) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 0 ? new NoteSelectionDial() : new Dial();
            
            // Store the dial in the appropriate collection based on its type
            switch (i) {
                case 0 -> {
                    dial.setPreferredSize(new Dimension(75, 75));
                    noteDial[0] = dial;
                    noteDials.add(dial);
                }
                case 1 -> {
                    velocityDial[0] = dial;
                    velocityDials.add(dial);
                }
                case 2 -> {
                    gateDial[0] = dial;
                    gateDials.add(dial);
                }
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
        triggerButton[0] = new TriggerButton("");
        triggerButton[0].setName("TriggerButton-" + index);
        triggerButton[0].setToolTipText("Step " + (index + 1));

        // Make it toggleable
        triggerButton[0].setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton[0].addActionListener(e -> {
            boolean isSelected = triggerButton[0].isSelected();

            // Get existing step data
            int note = noteDials.get(index).getValue();
            int velocity = velocityDials.get(index).getValue();
            int gate = gateDials.get(index).getValue();

            // Update sequencer pattern data
            sequencer.setStepData(index, isSelected, note, velocity, gate);
        });

        triggerButtons.add(triggerButton[0]);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton[0]);
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

        noteDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(), gateDial[0].getValue());
        });

        velocityDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(), gateDial[0].getValue());
        });

        gateDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(), gateDial[0].getValue());
        });

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    /**
     * Update step highlighting during playback with improved thread safety and consistency
     */
    private void updateStepHighlighting(int oldStep, int newStep) {
        // Use SwingUtilities to ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateStepHighlighting(oldStep, newStep));
            return;
        }

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

        // Debug log to track step changes
        logger.debug("Step highlight updated: {} -> {}", oldStep, newStep);
    }

    private void updateOctaveLabel() {
        if (octaveLabel != null) {
            octaveLabel.setText(Integer.toString(sequencer.getOctaveShift()));
        }
    }

    /**
     * Update the syncUIWithSequencer method to handle scale settings
     */
    private void syncUIWithSequencer() {
        updatingUI = true;
        try {
            // Update existing trigger buttons
            for (int i = 0; i < Math.min(triggerButtons.size(), sequencer.getActiveSteps().size()); i++) {
                TriggerButton button = triggerButtons.get(i);
                button.setSelected(sequencer.getActiveSteps().get(i));
            }
            
            // Update existing dials
            for (int i = 0; i < Math.min(noteDials.size(), sequencer.getNoteValues().size()); i++) {
                noteDials.get(i).setValue(sequencer.getNoteValues().get(i), true);
            }
            
            // Update existing velocity dials with force repaint
            for (int i = 0; i < Math.min(velocityDials.size(), sequencer.getVelocityValues().size()); i++) {
                velocityDials.get(i).setValue(sequencer.getVelocityValues().get(i));
                velocityDials.get(i).repaint();  // Force repaint
            }
            
            // Update existing gate dials with force repaint
            for (int i = 0; i < Math.min(gateDials.size(), sequencer.getGateValues().size()); i++) {
                gateDials.get(i).setValue(sequencer.getGateValues().get(i));
                gateDials.get(i).repaint();  // Force repaint
            }
            
            // Update other UI controls
            loopCheckbox.setSelected(sequencer.isLooping());
            
            // Update direction combo
            int dirIndex = 0;
            switch (sequencer.getDirection()) {
                case FORWARD -> dirIndex = 0;
                case BACKWARD -> dirIndex = 1;
                case BOUNCE -> dirIndex = 2;
                case RANDOM -> dirIndex = 3;
            }
            directionCombo.setSelectedIndex(dirIndex);
            
            // Update timing division combo
            timingCombo.setSelectedItem(sequencer.getTimingDivision());
            
            // Update scale and root note combos
            rootNoteCombo.setSelectedItem(sequencer.getSelectedRootNote());
            scaleCombo.setSelectedItem(sequencer.getSelectedScale());
            
        } finally {
            updatingUI = false;
        }
    }

    /**
     * Disables all change listeners temporarily
     *
     * @return Original state of listeners
     */
    private boolean disableAllListeners() {
        boolean original = listenersEnabled;
        listenersEnabled = false;
        return original;
    }

    /**
     * Restores listeners to their previous state
     */
    private void restoreListeners(boolean previousState) {
        listenersEnabled = previousState;
    }

    /**
     * Modify onAction to prevent the infinite scale selection loop
     */
    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.ROOT_NOTE_SELECTED -> {
                if (action.getData() instanceof String rootNote) {
                    // Only update if this isn't our own event
                    if (action.getSender() != this) {
                        boolean wasUpdating = updatingUI;
                        updatingUI = true;
                        try {
                            // Update root note combo without triggering more events
                            rootNoteCombo.setSelectedItem(rootNote);
                            // Update the sequencer
                            if (sequencer != null) {
                                sequencer.setRootNote(rootNote);
                                sequencer.updateQuantizer();
                            }
                        } finally {
                            updatingUI = wasUpdating;
                        }
                    }
                }
            }
            
            case Commands.SCALE_SELECTED -> {
                if (action.getData() instanceof String scaleName) {
                    // Only update if this isn't our own event
                    if (action.getSender() != this) {
                        boolean wasUpdating = updatingUI;
                        updatingUI = true;
                        try {
                            // Update scale combo without triggering more events
                            scaleCombo.setSelectedItem(scaleName);
                            // Update the sequencer
                            if (sequencer != null) {
                                sequencer.setScale(scaleName);
                                sequencer.updateQuantizer();
                            }
                        } finally {
                            updatingUI = wasUpdating;
                        }
                    }
                }
            }
        }
    }
}

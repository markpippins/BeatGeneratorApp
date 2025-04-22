package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.*;
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
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.core.redis.MelodicSequencerHelper;
import com.angrysurfer.core.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
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
import com.angrysurfer.core.service.MelodicSequencerManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerPanel.class);

    private JToggleButton loopToggleButton;

    // CORE SEQUENCER - manages all sequencing logic
    private MelodicSequencer sequencer;

    // UI state variables - keep these in the panel
    private List<TriggerButton> triggerButtons = new ArrayList<>();
    private List<Dial> noteDials = new ArrayList<>();
    private List<Dial> velocityDials = new ArrayList<>();
    private List<Dial> gateDials = new ArrayList<>();
    private List<DrumButton> melodicPadButtons = new ArrayList<>();
    private List<Dial> probabilityDials = new ArrayList<>();
    private List<Dial> nudgeDials = new ArrayList<>();

    // Labels and UI components
    private JLabel octaveLabel;
    private JComboBox<String> rootNoteCombo;
    private JComboBox<String> scaleCombo;
    private JComboBox<String> directionCombo;
    private JComboBox<String> rangeCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton latchToggleButton;
    private JSpinner lastStepSpinner;

    private boolean listenersEnabled = true;
    private boolean updatingUI = false;

    private MelodicSequenceNavigationPanel navigationPanel;

    private MelodicSequencerSwingPanel swingPanel;

    private MelodicSequenceParametersPanel sequenceParamsPanel;

    private JPanel southPanel;

    /**
     * Modify constructor to use only one step update mechanism (direct
     * listener) and load the first sequence if available
     */
    public MelodicSequencerPanel(Integer channel, Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer
        sequencer = MelodicSequencerManager.getInstance().newSequencer(channel);

        // Set up the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);

        // Set up step update listener with DIRECT callback (no CommandBus)
        sequencer.setStepUpdateListener(event -> {
            updateStepHighlighting(event.getOldStep(), event.getNewStep());
        });

        // Initialize the UI
        initialize();
        
        // Try to load the first sequence for this sequencer
        loadFirstSequenceIfExists();

        // Register with command bus for other UI updates (not step highlighting)
        CommandBus.getInstance().register(this);
    }

    /**
     * Attempts to load the first sequence for this sequencer if any exist
     */
    private void loadFirstSequenceIfExists() {
        // Check if the sequencer has an ID (it should, but verify)
        if (sequencer.getId() == null) {
            logger.warn("Cannot load first sequence - sequencer has no ID");
            return;
        }
        
        // Get the manager reference
        MelodicSequencerManager manager = MelodicSequencerManager.getInstance();
        
        // Check if this sequencer has any sequences
        if (manager.hasSequences(sequencer.getId())) {
            Long firstId = manager.getFirstSequenceId(sequencer.getId());
            
            if (firstId != null) {
                logger.info("Loading first sequence {} for sequencer {}", firstId, sequencer.getId());
                
                // Load the sequence
                RedisService redisService = RedisService.getInstance();
                redisService.applyMelodicSequenceToSequencer(
                    redisService.findMelodicSequenceById(firstId, sequencer.getId()),
                    sequencer
                );
                
                // Reset the sequencer to ensure proper step indicator state
                sequencer.reset();
                
                // Update the UI to reflect loaded sequence
                syncUIWithSequencer();
                
                // Update the tilt sequencer panel specifically
                updateTiltSequencerPanel();
                
                // Notify that a pattern was loaded
                CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_LOADED,
                    this,
                    new MelodicSequencerHelper.MelodicSequencerEvent(
                        sequencer.getId(), 
                        sequencer.getMelodicSequenceId()
                    )
                );
                
                // If we have a navigation panel, update its display
                if (navigationPanel != null) {
                    navigationPanel.updateSequenceIdDisplay();
                }
            }
        } else {
            logger.info("No saved sequences found for sequencer {}, using default empty pattern", 
                       sequencer.getId());
        }
    }

    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Use a consistent BorderLayout
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create west panel to hold navigation
        JPanel westPanel = new JPanel(new BorderLayout(5, 5));

        // Create east panel for sound parameters
        JPanel eastPanel = new JPanel(new BorderLayout(5, 5));

        // Create top panel to hold west and east panels
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Create sequence navigation panel
        navigationPanel = new MelodicSequenceNavigationPanel(sequencer);


        // Create sequence parameters panel
        sequenceParamsPanel = new MelodicSequenceParametersPanel(sequencer);

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.NORTH);

        // Sound parameters go NORTH-EAST
        eastPanel.add(createSoundParametersPanel(), BorderLayout.NORTH);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(eastPanel, BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 2, 0));
        sequencePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

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

        // Add sequence grid to center
        add(scrollPane, BorderLayout.CENTER);

        // Create a container panel for both southern panels - SWAPPED ORDER
        southPanel = new JPanel(new BorderLayout(5, 5));

        // Create tilt panel with LIMITED HEIGHT and add it to the TOP of the south
        // panel
        TiltSequencerPanel tiltPanel = new TiltSequencerPanel(sequencer);

        // Color[] colors = { Color.MAGENTA, new Color(0, 0, 0, 0), Color.BLUE, Color.GREEN, Color.YELLOW };

        // Set a fixed preferred height for the tilt panel to prevent it from taking too
        // much space
        tiltPanel.setPreferredSize(new Dimension(tiltPanel.getPreferredSize().width, 100));
        southPanel.add(tiltPanel, BorderLayout.NORTH);

        // Create a container for the bottom controls (parameters + generate)
        JPanel bottomControlsPanel = new JPanel(new BorderLayout(5, 5));

        // Add sequence parameters to the center of bottom controls
        bottomControlsPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create a container for the right-side panels
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Create and add generate panel
        JPanel generatePanel = createGeneratePanel();
        rightPanel.add(generatePanel);

        // Create and add swing panel
        swingPanel = new MelodicSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        // Add the right panel container to the east position
        bottomControlsPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom controls container to the south panel
        southPanel.add(bottomControlsPanel, BorderLayout.SOUTH);

        // Add the south panel to the main layout
        add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the sound parameters panel with preset selection and sound editing
     */
    private JPanel createSoundParametersPanel() {
        // Size constants
        final int SMALL_CONTROL_WIDTH = 30;
        final int LARGE_CONTROL_WIDTH = 90;
        final int CONTROL_HEIGHT = 25;

        // Create the panel with a titled border
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sound Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Create preset combo
        JComboBox<String> presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH * 2, CONTROL_HEIGHT));
        presetCombo.setToolTipText("Select instrument preset");
        populatePresetCombo(presetCombo);

        // Add listener for preset changes
        presetCombo.addActionListener(e -> {
            if (updatingUI || presetCombo.getSelectedIndex() < 0)
                return;

            int presetIndex = presetCombo.getSelectedIndex();
            logger.info("Selected preset index: {}", presetIndex);

            if (sequencer.getNotePlayer() != null) {
                sequencer.getNotePlayer().setPreset(presetIndex);

                // Inform the system about the preset change
                CommandBus.getInstance().publish(
                        Commands.PLAYER_UPDATED,
                        this,
                        sequencer.getNotePlayer());
            }
        });

        // Create edit button with pencil icon and skinny width
        JButton editButton = new JButton("✎");
        editButton.setToolTipText("Edit sound for this sequencer");
        editButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        editButton.setMargin(new Insets(1, 1, 1, 1));
        editButton.setFocusable(false);

        // Add listener for edit button
        editButton.addActionListener(e -> {
            if (sequencer != null && sequencer.getNotePlayer() != null) {
                logger.info("Opening player editor for: {}", sequencer.getNotePlayer().getName());
                CommandBus.getInstance().publish(
                        Commands.PLAYER_SELECTED,
                        this,
                        sequencer.getNotePlayer());

                CommandBus.getInstance().publish(
                        Commands.PLAYER_EDIT_REQUEST,
                        this,
                        sequencer.getNotePlayer());
            } else {
                logger.warn("Cannot edit player - Note player is not initialized");
            }
        });

        // Add components to panel
        panel.add(presetCombo);
        panel.add(editButton);

        return panel;
    }

    /**
     * Create panel for sequence parameters (loop, direction, timing, etc.)
     */
    private JPanel createGeneratePanel() {
        // Size constants
        final int SMALL_CONTROL_WIDTH = 40;
        final int MEDIUM_CONTROL_WIDTH = 90;
        final int CONTROL_HEIGHT = 25;

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Generate"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

        // Range combo (moved from sequence parameters panel)
        String[] rangeOptions = { "1 Octave", "2 Octaves", "3 Octaves", "4 Octaves" };
        rangeCombo = new JComboBox<>(rangeOptions);
        rangeCombo.setSelectedIndex(1); // Default to 2 octaves
        rangeCombo.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        rangeCombo.setToolTipText("Set the octave range for pattern generation");
 
        // Generate button with consistent styling
        JButton generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        generateButton.setMargin(new Insets(2, 2, 2, 2));
        generateButton.addActionListener(e -> {
            // Get selected octave range from the combo
            int octaveRange = rangeCombo.getSelectedIndex() + 1;
            int density = 50; // Fixed density for now
            sequencer.generatePattern(octaveRange, density);
            syncUIWithSequencer();
        });
 
        // Latch toggle button (moved from sequence parameters panel)
        latchToggleButton = new JToggleButton("L", false);
        latchToggleButton.setToolTipText("Generate new pattern each cycle");
        latchToggleButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        latchToggleButton.addActionListener(e -> {
            sequencer.setLatchEnabled(latchToggleButton.isSelected());
            logger.info("Latch mode set to: {}", latchToggleButton.isSelected());
        });
 
        panel.add(generateButton);
        panel.add(rangeCombo);
        panel.add(latchToggleButton);

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

    // Update the createSequenceColumn method to make columns more compact
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        // Reduce border padding to make columns narrower
        column.setBorder(BorderFactory.createEmptyBorder(3, 1, 3, 1));

        // Add 5 knobs
        Dial[] noteDial = { null };
        Dial[] velocityDial = { null };
        Dial[] gateDial = { null };
        Dial[] probabilityDial = { null };
        Dial[] nudgeDial = { null };
        TriggerButton[] triggerButton = { null };

        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            // Make label more compact with smaller padding
            label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            label.setForeground(Color.GRAY);
            label.setFont(label.getFont().deriveFont(10f)); // Smaller font

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
                    velocityDial[0] = dial;
                    velocityDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("velocity"));
                }
                case 1 -> {
                    gateDial[0] = dial;
                    gateDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("gate"));
                }
                case 4 -> {
                    dial.setPreferredSize(new Dimension(75, 75)); // Reduced from 75x75
                    noteDial[0] = dial;
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
                    probabilityDial[0] = dial;
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
                    nudgeDial[0] = dial;
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

        // Add minimal spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 2)));

        // Make trigger button more compact
        triggerButton[0] = new TriggerButton("");
        triggerButton[0].setName("TriggerButton-" + index);
        triggerButton[0].setToolTipText("Step " + (index + 1));
        triggerButton[0].setPreferredSize(new Dimension(20, 20)); // Smaller size
        triggerButton[0].setToggleable(true);

        triggerButton[0].addActionListener(e -> {
            boolean isSelected = triggerButton[0].isSelected();
            // Get existing step data
            int note = noteDials.get(index).getValue();
            int velocity = velocityDials.get(index).getValue();
            int gate = gateDials.get(index).getValue();
            int probability = probabilityDials.get(index).getValue();
            int nudge = nudgeDials.get(index).getValue();
            // Update sequencer pattern data
            sequencer.setStepData(index, isSelected, note, velocity, gate, probability, nudge);
        });

        triggerButtons.add(triggerButton[0]);
        // Compact panel for trigger button
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton[0]);
        column.add(buttonPanel1);

        noteDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Get probability and nudge values
            int probability = 100;
            int nudge = 0;

            if (index < probabilityDials.size()) {
                probability = probabilityDials.get(index).getValue();
            }

            if (index < nudgeDials.size()) {
                nudge = nudgeDials.get(index).getValue();
            }

            // Update sequencer with all step data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(),
                    gateDial[0].getValue(), probability, nudge);
        });

        velocityDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(),
                    gateDial[0].getValue(), probabilityDials.get(index).getValue(),
                    nudgeDials.get(index).getValue());
        });

        gateDial[0].addChangeListener(e -> {
            if (!listenersEnabled) {
                return;
            }

            // Update sequencer pattern data
            sequencer.setStepData(index, triggerButton[0].isSelected(),
                    noteDial[0].getValue(), velocityDial[0].getValue(),
                    gateDial[0].getValue(), probabilityDials.get(index).getValue(),
                    nudgeDials.get(index).getValue());
        });

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Velocity" : i == 1 ? "Gate" : i == 2 ? "Probability" : i == 3 ? "Nudge" : "Unknown";
    }

    /**
     * Update step highlighting during playback with improved thread safety and
     * consistency
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
     * Synchronize UI elements with sequencer state
     */
    private void syncUIWithSequencer() {
        // Update sequence parameters first
        if (sequenceParamsPanel != null) {
            sequenceParamsPanel.updateUI(sequencer);
        }

        // Update step buttons based on current pattern
        updateStepButtons();

        // Update swing panel if available
        if (swingPanel != null) {
            swingPanel.updateControls();
        }

        // Update tilt sequencer panel specifically
        updateTiltSequencerPanel();
        
        // Force revalidate and repaint of the entire panel
        revalidate();
        repaint();
        
        logger.debug("UI synchronized with sequencer state");
    }

    /**
     * Update step buttons and dials to reflect current sequencer state
     */
    private void updateStepButtons() {
        // Save previous listener state
        boolean previousListenerState = disableAllListeners();
        
        try {
            // Update all 16 steps
            for (int i = 0; i < triggerButtons.size(); i++) {
                // Update trigger button state
                TriggerButton button = triggerButtons.get(i);
                boolean active = sequencer.isStepActive(i);
                button.setSelected(active);
                
                // Update note dial
                if (i < noteDials.size()) {
                    noteDials.get(i).setValue(sequencer.getNoteValue(i));
                }
                
                // Update velocity dial
                if (i < velocityDials.size()) {
                    velocityDials.get(i).setValue(sequencer.getVelocityValue(i));
                }
                
                // Update gate dial
                if (i < gateDials.size()) {
                    gateDials.get(i).setValue(sequencer.getGateValue(i));
                }
                
                // Update probability dial
                if (i < probabilityDials.size()) { // && sequencer.getProbabilityValue(i) != null) {
                    probabilityDials.get(i).setValue(sequencer.getProbabilityValue(i));
                }
                
                // Update nudge dial
                if (i < nudgeDials.size()) { // && sequencer.getNudgeValue(i) != null) {
                    nudgeDials.get(i).setValue(sequencer.getNudgeValue(i));
                }
            }
            
            // Force repaint of all components
            for (TriggerButton button : triggerButtons) {
                button.repaint();
            }
        } finally {
            // Restore previous listener state
            restoreListeners(previousListenerState);
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
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            // Add cases for sequence loaded, created and updated events
            case Commands.MELODIC_SEQUENCE_LOADED, 
                 Commands.MELODIC_SEQUENCE_CREATED, 
                 Commands.MELODIC_SEQUENCE_UPDATED -> {
                // Sync the UI with sequencer state
                syncUIWithSequencer();
                
                // Make sure to update the tilt sequencer panel
                updateTiltSequencerPanel();
            }
            
            // Other cases remain the same...
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

            case Commands.PATTERN_UPDATED -> {
                // Only handle events from our sequencer to avoid loops
                if (action.getSender() == sequencer) {
                    logger.info("Received PATTERN_UPDATED event, refreshing UI");

                    // Update UI on EDT
                    SwingUtilities.invokeLater(() -> {
                        // Disable listeners while updating UI
                        listenersEnabled = false;
                        try {
                            // Completely refresh UI from sequencer state
                            syncUIWithSequencer();

                            // Repaint all components
                            repaint();
                        } finally {
                            // Re-enable listeners
                            listenersEnabled = true;
                        }
                    });
                }
            }
        }
    }

    /**
     * Update the TiltSequencerPanel to reflect current sequencer state
     */
    private void updateTiltSequencerPanel() {
        // Find and update any TiltSequencerPanel components
        for (Component c : southPanel.getComponents()) {
            if (c instanceof JPanel panel) {
                for (Component innerComp : panel.getComponents()) {
                    if (innerComp instanceof TiltSequencerPanel tiltPanel) {
                        logger.info("Syncing TiltSequencerPanel with sequencer");
                        tiltPanel.syncWithSequencer();
                        break;
                    }
                }
            } else if (c instanceof TiltSequencerPanel tiltPanel) {
                logger.info("Syncing TiltSequencerPanel with sequencer");
                tiltPanel.syncWithSequencer();
            }
        }
    }

    /**
     * Initialize melodic pads and apply numbered labels with beat indicators.
     */
    private void initializeMelodicPads() {
        // Apply numbered labels to each pad with beat indicators
        for (int i = 0; i < melodicPadButtons.size(); i++) {
            DrumButton pad = melodicPadButtons.get(i);

            // Set the pad number (1-based)
            pad.setPadNumber(i + 1);

            // Set main beat flag for pads 1, 5, 9, 13 (zero-indexed as 0, 4, 8, 12)
            pad.setIsMainBeat(i == 0 || i == 4 || i == 8 || i == 12);

            // FORCE REPAINT for each pad
            pad.repaint();
        }

        // FORCE LAYOUT UPDATE to ensure sizes are applied
        revalidate();
    }
}

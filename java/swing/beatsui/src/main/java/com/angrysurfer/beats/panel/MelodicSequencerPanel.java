package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.ColorUtils;
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

    /**
     * Modify constructor to use only one step update mechanism (direct
     * listener)
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

        // Register with command bus for other UI updates (not step highlighting)
        CommandBus.getInstance().register(this);
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
        MelodicSequenceNavigationPanel navigationPanel = new MelodicSequenceNavigationPanel(sequencer);

        // Create sequence parameters panel
        JPanel sequenceParamsPanel = createSequenceParametersPanel();

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
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));

        // Create tilt panel with LIMITED HEIGHT and add it to the TOP of the south
        // panel
        TiltSequencerPanel tiltPanel = new TiltSequencerPanel(sequencer);
        tiltPanel.setBackground(ColorUtils.getColors()[MelodicSequencerManager.getInstance().getSequencerCount()]);

        // Set a fixed preferred height for the tilt panel to prevent it from taking too
        // much space
        tiltPanel.setPreferredSize(new Dimension(tiltPanel.getPreferredSize().width, 100));
        southPanel.add(tiltPanel, BorderLayout.NORTH);

        // Create a container for the bottom controls (parameters + generate)
        JPanel bottomControlsPanel = new JPanel(new BorderLayout(5, 5));

        // Add sequence parameters to the center of bottom controls
        sequenceParamsPanel = createSequenceParametersPanel();
        bottomControlsPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create and add generate panel to the right of sequence parameters
        JPanel generatePanel = createGeneratePanel();
        bottomControlsPanel.add(generatePanel, BorderLayout.EAST);

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
    private JPanel createSequenceParametersPanel() {
        // Size constants to match DrumSequencerPanel
        final int SMALL_CONTROL_WIDTH = 40;
        final int MEDIUM_CONTROL_WIDTH = 60;
        final int LARGE_CONTROL_WIDTH = 90;
        final int CONTROL_HEIGHT = 25;

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Sequence Parameters"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Last Step spinner
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 16, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        lastStepSpinner.setToolTipText("Set the last step for the pattern (1-16)");
        lastStepSpinner.addChangeListener(e -> {
            int lastStep = (Integer) lastStepSpinner.getValue();
            sequencer.setPatternLength(lastStep);
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo - remove the label
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        directionCombo = new JComboBox<>(new String[] { "Forward", "Backward", "Bounce", "Random" });
        directionCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
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

        // Timing division combo - remove the label
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        timingCombo.setToolTipText("Set the timing division for this pattern");
        timingCombo.addActionListener(e -> {
            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                logger.info("Setting timing division to {}", division);
                sequencer.setTimingDivision(division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox - standardize with emoji
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT)); // Reduce width
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            sequencer.setLooping(loopToggleButton.isSelected());
        });

        // Create rotation panel
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Rotate Left button
        JButton rotateLeftButton = new JButton("⟵");
        rotateLeftButton.setToolTipText("Rotate pattern one step left");
        rotateLeftButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        rotateLeftButton.setMargin(new Insets(2, 2, 2, 2));
        rotateLeftButton.addActionListener(e -> {
            sequencer.rotatePatternLeft();
            syncUIWithSequencer();
        });

        // Rotate Right button
        JButton rotateRightButton = new JButton("⟶");
        rotateRightButton.setToolTipText("Rotate pattern one step right");
        rotateRightButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        rotateRightButton.setMargin(new Insets(2, 2, 2, 2));
        rotateRightButton.addActionListener(e -> {
            sequencer.rotatePatternRight();
            syncUIWithSequencer();
        });

        // Add buttons to rotation panel
        rotationPanel.add(rotateLeftButton);
        rotationPanel.add(rotateRightButton);

        // Clear button
        JButton clearButton = new JButton("🗑️");
        clearButton.setToolTipText("Clear pattern");
        clearButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        clearButton.setMargin(new Insets(2, 2, 2, 2));
        clearButton.addActionListener(e -> {
            sequencer.clearPattern();
            syncUIWithSequencer();
        });

        // Octave panel
        JPanel octavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        octavePanel.add(new JLabel("Octave:"));

        JButton octaveDownBtn = new JButton("-");
        octaveDownBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveDownBtn.setFocusable(false);
        octaveDownBtn.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        octaveDownBtn.addActionListener(e -> {
            sequencer.decrementOctaveShift();
            updateOctaveLabel();
        });
        octaveDownBtn.setToolTipText("Lower the octave");

        octaveLabel = new JLabel("0");
        octaveLabel.setPreferredSize(new Dimension(20, 20));
        octaveLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton octaveUpBtn = new JButton("+");
        octaveUpBtn.setMargin(new java.awt.Insets(1, 5, 1, 5));
        octaveUpBtn.setFocusable(false);
        octaveUpBtn.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        octaveUpBtn.addActionListener(e -> {
            sequencer.incrementOctaveShift();
            updateOctaveLabel();
        });
        octaveUpBtn.setToolTipText("Raise the octave");

        octavePanel.add(octaveDownBtn);
        octavePanel.add(octaveLabel);
        octavePanel.add(octaveUpBtn);

        // Root Note panel
        JPanel rootNotePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rootNotePanel.add(new JLabel("Root:"));

        rootNoteCombo = createRootNoteCombo();
        rootNoteCombo.setPreferredSize(new Dimension(MEDIUM_CONTROL_WIDTH, CONTROL_HEIGHT));
        rootNoteCombo.setToolTipText("Set the root note");
        rootNotePanel.add(rootNoteCombo);

        // Scale panel
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        scalePanel.add(new JLabel("Scale:"));

        scaleCombo = createScaleCombo();
        scaleCombo.setPreferredSize(new Dimension(120, CONTROL_HEIGHT));
        scaleCombo.setToolTipText("Set the scale");
        scalePanel.add(scaleCombo);

        // Quantize checkbox
        JCheckBox quantizeCheckbox = new JCheckBox("Q", true);
        quantizeCheckbox.setToolTipText("Quantize notes to scale");
        quantizeCheckbox.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        quantizeCheckbox.addActionListener(e -> {
            sequencer.setQuantizeEnabled(quantizeCheckbox.isSelected());
        });

        // --- Add controls to panel in the desired order ---

        // First, add core controls in the same order as DrumSequencerPanel
        panel.add(timingPanel);
        panel.add(directionPanel);
        panel.add(loopToggleButton);
        panel.add(lastStepPanel);
        panel.add(rotationPanel);  // Add the rotation panel here
        panel.add(clearButton);    // Add the clear button here

        // Then add additional controls specific to MelodicSequencerPanel
        panel.add(rootNotePanel);
        panel.add(quantizeCheckbox);
        panel.add(scalePanel);
        panel.add(octavePanel);

        return panel;
    }

    /**
     * Creates a dedicated panel for generation controls
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

    /**
     * Improve the createScaleCombo method to use Scale.getScales()
     */
    private JComboBox<String> createScaleCombo() {
        // Use the Scale class to get scale names instead of accessing SCALE_PATTERNS
        // directly
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
                        selectedScale);

                logger.info("Scale selected: {}", selectedScale);
            }
        });

        return combo;
    }

    /**
     * Improve the createRootNoteCombo method
     */
    private JComboBox<String> createRootNoteCombo() {
        String[] noteNames = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

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
                        rootNote);

                logger.info("Root note selected: {}", rootNote);
            }
        });

        return combo;
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
                    dial.setKnobColor(ColorUtils.getDialColor("velocity"));
                }
                case 1 -> {
                    gateDial[0] = dial;
                    gateDials.add(dial);
                    dial.setKnobColor(ColorUtils.getDialColor("gate"));
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
                    dial.setKnobColor(ColorUtils.getDialColor("probability"));
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
                    dial.setKnobColor(ColorUtils.getDialColor("nudge"));
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
     * Synchronize all UI elements with the current sequencer state
     */
    private void syncUIWithSequencer() {
        updatingUI = true;
        try {
            // Update trigger buttons
            List<Boolean> activeSteps = sequencer.getActiveSteps();
            for (int i = 0; i < Math.min(triggerButtons.size(), activeSteps.size()); i++) {
                triggerButtons.get(i).setSelected(activeSteps.get(i));
                triggerButtons.get(i).repaint();
            }

            // Update note dials
            List<Integer> noteValues = sequencer.getNoteValues();
            for (int i = 0; i < Math.min(noteDials.size(), noteValues.size()); i++) {
                noteDials.get(i).setValue(noteValues.get(i));
                noteDials.get(i).repaint();
            }

            // Update velocity dials
            List<Integer> velocityValues = sequencer.getVelocityValues();
            for (int i = 0; i < Math.min(velocityDials.size(), velocityValues.size()); i++) {
                velocityDials.get(i).setValue(velocityValues.get(i));
                velocityDials.get(i).repaint();
            }

            // Update gate dials
            List<Integer> gateValues = sequencer.getGateValues();
            for (int i = 0; i < Math.min(gateDials.size(), gateValues.size()); i++) {
                gateDials.get(i).setValue(gateValues.get(i));
                gateDials.get(i).repaint();
            }

            // Update probability dials
            List<Integer> probabilityValues = sequencer.getProbabilityValues();
            for (int i = 0; i < Math.min(probabilityDials.size(), probabilityValues.size()); i++) {
                probabilityDials.get(i).setValue(probabilityValues.get(i));
                probabilityDials.get(i).repaint();
            }

            // Update nudge dials
            List<Integer> nudgeValues = sequencer.getNudgeValues();
            for (int i = 0; i < Math.min(nudgeDials.size(), nudgeValues.size()); i++) {
                nudgeDials.get(i).setValue(nudgeValues.get(i));
                nudgeDials.get(i).repaint();
            }

            // Update parameter controls
            if (loopToggleButton != null) {
                loopToggleButton.setSelected(sequencer.isLooping());
            }

            if (lastStepSpinner != null) {
                lastStepSpinner.setValue(sequencer.getPatternLength());
            }

            if (directionCombo != null) {
                switch (sequencer.getDirection()) {
                    case FORWARD -> directionCombo.setSelectedIndex(0);
                    case BACKWARD -> directionCombo.setSelectedIndex(1);
                    case BOUNCE -> directionCombo.setSelectedIndex(2);
                    case RANDOM -> directionCombo.setSelectedIndex(3);
                }
            }

            if (latchToggleButton != null) {
                latchToggleButton.setSelected(sequencer.isLatchEnabled());
            }

            // Force a revalidate and repaint of the entire panel
            revalidate();
            repaint();

            logger.debug("UI synchronized with sequencer state");
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
        if (action == null || action.getCommand() == null) {
            return;
        }

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

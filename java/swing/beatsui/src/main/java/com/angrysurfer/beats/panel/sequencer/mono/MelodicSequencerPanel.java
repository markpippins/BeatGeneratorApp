package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.TiltSequencerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.event.MelodicScaleSelectionEvent;
import com.angrysurfer.beats.panel.SessionControlPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.MelodicSequencerHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.NoteEvent;
import com.angrysurfer.core.sequencer.TimingDivision;
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

    private TiltSequencerPanel tiltSequencerPanel;

    private MelodicSequencerGridPanel gridPanel;

    private MelodicSequencerScalePanel scalePanel;

    private JLabel instrumentInfoLabel;

    /**
     * Constructor for MelodicSequencerPanel
     */
    public MelodicSequencerPanel(Integer index, Integer channel, Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer with properly assigned channel
        sequencer = MelodicSequencerManager.getInstance().newSequencer(index, channel);

        // Set up the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);

        // Set up step update listener with DIRECT callback (no CommandBus)
        sequencer.setStepUpdateListener(event -> {
            updateStepHighlighting(event.getOldStep(), event.getNewStep());
        });

        // Apply instrument preset immediately to ensure correct sound
        sequencer.applyInstrumentPreset();

        // Initialize the UI
        initialize();

        // Try to load the first sequence for this sequencer
        loadFirstSequenceIfExists();

        // Register with command bus for other UI updates (not step highlighting)
        CommandBus.getInstance().register(this);

        logger.info("Created MelodicSequencerPanel with index {} on channel {}", index, channel);
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
                MelodicSequenceData data = redisService.findMelodicSequenceById(firstId, sequencer.getId());

                // Log what we're loading
                if (data.getHarmonicTiltValues() != null) {
                    logger.info("Loaded sequence has {} tilt values", data.getHarmonicTiltValues().size());
                } else {
                    logger.warn("Loaded sequence has no tilt values");
                }

                redisService.applyMelodicSequenceToSequencer(data, sequencer);

                // Reset the sequencer to ensure proper step indicator state
                sequencer.reset();

                // Update the UI to reflect loaded sequence
                syncUIWithSequencer();

                // EXPLICIT CALL to update tilt panel
                if (tiltSequencerPanel != null) {
                    logger.info("Explicitly updating tilt panel after sequence load");
                    tiltSequencerPanel.syncWithSequencer();
                }

                // Notify that a pattern was loaded
                CommandBus.getInstance().publish(
                        Commands.MELODIC_SEQUENCE_LOADED,
                        this,
                        new MelodicSequencerHelper.MelodicSequencerEvent(
                                sequencer.getId(),
                                sequencer.getMelodicSequenceId()));

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

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // REDUCED: from 5,5 to 2,2
        setLayout(new BorderLayout(2, 2));
        // REDUCED: from 5,5,5,5 to 2,2,2,2
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Create west panel to hold navigation
        // REDUCED: from 5,5 to 2,2
        JPanel westPanel = new JPanel(new BorderLayout(2, 2));

        // Create east panel for sound parameters
        // REDUCED: from 5,5 to 2,2
        JPanel eastPanel = new JPanel(new BorderLayout(2, 2));

        // Create top panel to hold west and east panels
        // REDUCED: from 5,5 to 2,2
        JPanel topPanel = new JPanel(new BorderLayout(2, 2));

        // Create sequence navigation panel
        navigationPanel = new MelodicSequenceNavigationPanel(sequencer, this);

        // Create sequence parameters panel
        sequenceParamsPanel = new MelodicSequenceParametersPanel(sequencer);

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.NORTH);

        // Sound parameters go NORTH-EAST
        eastPanel.add(createSoundParametersPanel(), BorderLayout.NORTH);

        // Create and add the center info panel with instrument info
        JPanel centerPanel = new JPanel(new GridBagLayout());  // Use GridBagLayout for true vertical centering
        
        // Create the instrument info label
        instrumentInfoLabel = new JLabel();
        instrumentInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        updateInstrumentInfoLabel(); // Initialize with current values

        // Add constraints to center vertically and horizontally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;  // Give horizontal weight
        gbc.weighty = 1.0;  // Give vertical weight - this is key for vertical centering
        centerPanel.add(instrumentInfoLabel, gbc);

        // Add centerPanel to the top panel
        UIUtils.addSafely(topPanel, centerPanel, BorderLayout.CENTER);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(eastPanel, BorderLayout.EAST);

        // Add top panel to main layout
        add(topPanel, BorderLayout.NORTH);

        // Create the grid panel and add to center
        gridPanel = new MelodicSequencerGridPanel(sequencer);
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);



        JPanel sequencersPanel = new JPanel(new BorderLayout(2, 1));
    
        // Create the tilt panel and add it to the NORTH of bottom panel


        tiltSequencerPanel = new TiltSequencerPanel(sequencer);
        sequencersPanel.add(tiltSequencerPanel, BorderLayout.NORTH);
        sequencersPanel.add(new MuteSequencerPanel(sequencer), BorderLayout.SOUTH);
        // Create bottom panel with BorderLayout for proper positioning
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 1));

        bottomPanel.add(sequencersPanel, BorderLayout.NORTH);

        // Add the parameters panel to the CENTER of the bottom panel
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create right panel for additional controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));

        // Create and add the scale panel to the RIGHT of the bottom panel
        scalePanel = new MelodicSequencerScalePanel(sequencer);
        rightPanel.add(scalePanel);

        rightPanel.add(createGeneratePanel());
        // Add swing panel to the right panel
        swingPanel = new MelodicSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        // Add the right panel to the bottom panel's EAST region
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Store reference to southPanel for use in updateTiltSequencerPanel()
        this.southPanel = bottomPanel;

        // Add the bottom panel to the SOUTH region of the main panel
        add(bottomPanel, BorderLayout.SOUTH);

        // Register for command updates
        CommandBus.getInstance().register(this);
    }

    /**
     * Update the instrument info label with current player and instrument
     * information
     */
    private void updateInstrumentInfoLabel() {
        if (sequencer == null || sequencer.getPlayer() == null) {
            instrumentInfoLabel.setText("No Player");
            return;
        }

        Player player = sequencer.getPlayer();
        String playerName = player.getName();
        String instrumentName = "No Instrument";
        String channelInfo = "";

        if (player.getInstrument() != null) {
            instrumentName = player.getInstrument().getName();
            int channel = player.getChannel() != null ? player.getChannel() : 0;
            channelInfo = " (Ch " + (channel + 1) + ")";
        }

        instrumentInfoLabel.setText(playerName + " - " + instrumentName + channelInfo);
    }

    /**
     * Creates the sound parameters panel for instrument selection and editing
     */
    private JPanel createSoundParametersPanel() {
        // Create the standardized SoundParametersPanel with the current player
        // Player is null-safe in SoundParametersPanel constructor
        SoundParametersPanel panel = new SoundParametersPanel(sequencer.getPlayer());

        // Return the panel
        return panel;
    }

    private JPanel createGeneratePanel() {

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Generate"));
        // REDUCED: from 5,2 to 2,1
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));

        // Range combo (moved from sequence parameters panel)
        String[] rangeOptions = { "1 Octave", "2 Octaves", "3 Octaves", "4 Octaves" };
        rangeCombo = new JComboBox<>(rangeOptions);
        rangeCombo.setSelectedIndex(1); // Default to 2 octaves
        rangeCombo.setPreferredSize(new Dimension(UIUtils.LARGE_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        rangeCombo.setToolTipText("Set the octave range for pattern generation");

        // Generate button with consistent styling
        JButton generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
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
        latchToggleButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        latchToggleButton.addActionListener(e -> {
            sequencer.setLatchEnabled(latchToggleButton.isSelected());
            logger.info("Latch mode set to: {}", latchToggleButton.isSelected());
        });

        panel.add(generateButton);
        panel.add(rangeCombo);
        panel.add(latchToggleButton);

        return panel;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        // REDUCED: from 5,2,5,2 to 2,1,2,1
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            // Make label more compact with smaller padding - KEEPING this the same
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            // ADDED: Set consistent font size to match DrumEffectsSequencerPanel
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
                    // velocityDials[0] = dial;
                    velocityDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("velocity"));
                }
                case 1 -> {
                    // gateDial[0] = dial;
                    gateDials.add(dial);
                    dial.setKnobColor(UIUtils.getDialColor("gate"));
                }
                case 4 -> {
                    dial.setPreferredSize(new Dimension(75, 75)); // Reduced from 75x75
                    // noteDial[0] = dial;
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
                    // probabilityDial[0] = dial;
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
                    // nudgeDial[0] = dial;
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
            sequencer.setStepData(index, triggerButton.isSelected(),
                    noteDials.get(index).getValue(), velocityDials.get(index).getValue(),
                    gateDials.get(index).getValue(), probability, nudge);
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

    private String getKnobLabel(int i) {
        return i == 0 ? "Velocity" : i == 1 ? "Gate" : i == 2 ? "Probability" : i == 3 ? "Nudge" : "Unknown";
    }

    /**
     * Update step highlighting in the grid panel
     */
    private void updateStepHighlighting(int oldStep, int newStep) {
        // Delegate to grid panel
        if (gridPanel != null) {
            gridPanel.updateStepHighlighting(oldStep, newStep);
        }
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
            // Update sequence parameters panel
            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateUI(sequencer);
            }

            // Update scale panel
            if (scalePanel != null) {
                scalePanel.updateUI(sequencer);
            }

            // Update grid panel
            if (gridPanel != null) {
                gridPanel.syncWithSequencer();
            }

            // Update other panels
            // ...

        } finally {
            updatingUI = false;
        }

        // Force revalidate and repaint
        revalidate();
        repaint();
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
            pad.setMainBeat(i == 0 || i == 4 || i == 8 || i == 12);

            // FORCE REPAINT for each pad
            pad.repaint();
        }

        // FORCE LAYOUT UPDATE to ensure sizes are applied
        revalidate();
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
            // Arrow syntax for all cases
            case Commands.MELODIC_SEQUENCE_LOADED,
                    Commands.MELODIC_SEQUENCE_CREATED,
                    Commands.MELODIC_SEQUENCE_SELECTED,
                    Commands.MELODIC_SEQUENCE_UPDATED -> {
                // Check if this event applies to our sequencer
                if (action.getData() instanceof MelodicSequencerHelper.MelodicSequencerEvent event) {
                    if (event.getSequencerId().equals(sequencer.getId())) {
                        logger.info("Updating UI for sequence event: {}", action.getCommand());
                        syncUIWithSequencer();
                    }
                } else {
                    // If no specific sequencer event data, update anyway
                    syncUIWithSequencer();
                }
            }

            case Commands.SCALE_SELECTED -> {
                // Only update if this event is for our specific sequencer or from the global
                // controller
                if (action.getData() instanceof MelodicScaleSelectionEvent event) {
                    // Check if this event is for our sequencer
                    if (event.getSequencerId() != null && event.getSequencerId().equals(sequencer.getId())) {
                        // Update the scale in the sequencer
                        sequencer.setScale(event.getScale());

                        // Update the UI without publishing new events
                        if (scalePanel != null) {
                            scalePanel.setSelectedScale(event.getScale());
                        }

                        // Log the specific change
                        logger.debug("Set scale to {} for sequencer {}", event.getScale(), sequencer.getId());
                    }
                }
                // Handle global scale changes from session panel (separate implementation)
                else if (action.getData() instanceof String scale &&
                        (action.getSender() instanceof SessionControlPanel)) {
                    // This is a global scale change from the session panel
                    sequencer.setScale(scale);

                    // Update UI without publishing new events
                    if (scalePanel != null) {
                        scalePanel.setSelectedScale(scale);
                    }

                    logger.debug("Set scale to {} from global session change", scale);
                }
            }

            case Commands.PATTERN_UPDATED -> {
                // Your existing PATTERN_UPDATED handler code
                // Only handle events from our sequencer to avoid loops
                if (action.getSender() == sequencer) {
                    syncUIWithSequencer();
                }
            }

            case Commands.PLAYER_UPDATED, Commands.INSTRUMENT_CHANGED -> {
                // Check if this update is for our sequencer's player
                if (action.getData() instanceof Player &&
                        sequencer != null &&
                        sequencer.getPlayer() != null &&
                        sequencer.getPlayer().getId().equals(((Player) action.getData()).getId())) {

                    SwingUtilities.invokeLater(this::updateInstrumentInfoLabel);
                }
            }

            // Add other cases with arrow syntax
            default -> {
                // Optional default case
            }
        }
    }
}

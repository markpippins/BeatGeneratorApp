package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.BorderLayout;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.event.MelodicScaleSelectionEvent;
import com.angrysurfer.beats.panel.SessionControlPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.TiltSequencerPanel;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
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
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.MelodicSequencerManager;
import com.angrysurfer.core.service.PlayerManager;

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

    // Labels and UI components
    private JLabel octaveLabel;
    private JComboBox<String> rangeCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton latchToggleButton;

    private MelodicSequenceNavigationPanel navigationPanel;

    private MelodicSequencerSwingPanel swingPanel;

    private MelodicSequenceParametersPanel sequenceParamsPanel;

    private TiltSequencerPanel tiltSequencerPanel;

    private MelodicSequencerGridPanel gridPanel;

    private MelodicSequencerScalePanel scalePanel;

    private JLabel instrumentInfoLabel;

    private boolean updatingUI = false;

    /**
     * Constructor for MelodicSequencerPanel
     */
    public MelodicSequencerPanel(int index, Integer channel, Consumer<NoteEvent> noteEventConsumer) {
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
        PlayerManager.getInstance().applyInstrumentPreset(sequencer.getPlayer());

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
                MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(firstId,
                        sequencer.getId());

                // Apply the sequence data
                RedisService.getInstance().applyMelodicSequenceToSequencer(data, sequencer);

                // Ensure tilt values are present
                List<Integer> tiltValues = sequencer.getHarmonicTiltValues();
                logger.info("Loaded sequence with {} tilt values: {}",
                        tiltValues.size(), tiltValues);

                // Force UI update
                SwingUtilities.invokeLater(() -> {
                    // Be sure to update tilt panel
                    if (tiltSequencerPanel != null) {
                        tiltSequencerPanel.syncWithSequencer();
                    }

                    // Regular UI sync
                    syncUIWithSequencer();
                });

                // Make sure player settings are properly applied
                Player player = sequencer.getPlayer();
                if (player != null && player.getInstrument() != null) {
                    player.getInstrument().setSoundbankName(data.getSoundbankName());
                    player.getInstrument().setBankIndex(data.getBankIndex());
                    player.getInstrument().setPreset(data.getPreset());
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                }

                // Update the UI to reflect loaded sequence
                syncUIWithSequencer();
                if (tiltSequencerPanel != null) {
                    tiltSequencerPanel.syncWithSequencer();
                    logger.info("Synced tilt panel after loading sequence");
                }

                if (tiltSequencerPanel != null) {
                    logger.info("Explicitly updating tilt panel after sequence load");
                    tiltSequencerPanel.syncWithSequencer();
                }

                // Reset the sequencer to ensure proper step indicator state
                sequencer.reset();

                // Notify that a pattern was loaded
                CommandBus.getInstance().publish(
                        Commands.MELODIC_SEQUENCE_LOADED,
                        this,
                        new MelodicSequencerHelper.MelodicSequencerEvent(
                                sequencer.getId(),
                                data.getId()));

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
        JPanel centerPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout for true vertical centering

        // Create the instrument info label
        // instrumentInfoLabel = new JLabel();
        // instrumentInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        // updateInstrumentInfoLabel(); // Initialize with current values

        // Add constraints to center vertically and horizontally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0; // Give horizontal weight
        gbc.weighty = 1.0; // Give vertical weight - this is key for vertical centering
        // centerPanel.add(instrumentInfoLabel, gbc);

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

        // Add the bottom panel to the SOUTH region of the main panel
        add(bottomPanel, BorderLayout.SOUTH);

        // Register for command updates
        CommandBus.getInstance().register(this);
    }

    /**
     * Update the instrument info label with current player and instrument
     * information
     */
    // private void updateInstrumentInfoLabel() {
    //     if (sequencer == null || sequencer.getPlayer() == null) {
    //         instrumentInfoLabel.setText("No Player");
    //         return;
    //     }

    //     Player player = sequencer.getPlayer();
    //     String playerName = player.getName() + " (" + player.getId() + ")";
    //     String instrumentName = "No Instrument";
    //     String channelInfo = "";

    //     if (player.getInstrument() != null) {
    //         instrumentName = player.getInstrument().getName() + " (" + player.getInstrument().getId() + ")";
    //         int channel = player.getChannel() != null ? player.getChannel() : 0;
    //         channelInfo = " (Ch " + (channel + 1) + ")";
    //     }

    //     instrumentInfoLabel.setText(playerName + " - " + instrumentName + " - " + channelInfo);
    // }

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

            // Update tilt sequencer panel - THIS WAS MISSING
            if (tiltSequencerPanel != null) {
                logger.debug("Syncing tilt panel with sequencer values: {}",
                        sequencer.getHarmonicTiltValues().size());
                tiltSequencerPanel.syncWithSequencer();
            }

            // Update swing panel - THIS WAS MISSING
            if (swingPanel != null) {
                swingPanel.updateControls();
            }

            // Update navigation panel - THIS WAS MISSING
            if (navigationPanel != null) {
                navigationPanel.updateSequenceIdDisplay();
            }

            // Update instrument info label - THIS WAS MISSING
            // updateInstrumentInfoLabel();

            // Check and update latch toggle button if needed - THIS WAS MISSING
            if (latchToggleButton != null)
                latchToggleButton.setSelected(sequencer.isLatchEnabled());

        } finally {
            updatingUI = false;
        }

        // Force revalidate and repaint
        revalidate();
        repaint();
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
                // if (action.getData() instanceof Player &&
                //         sequencer != null &&
                //         sequencer.getPlayer() != null &&
                //         sequencer.getPlayer().getId().equals(((Player) action.getData()).getId())) {

                //     SwingUtilities.invokeLater(this::updateInstrumentInfoLabel);
                // }
            }

            default -> {
                // Optional default case
            }
        }
    }
}

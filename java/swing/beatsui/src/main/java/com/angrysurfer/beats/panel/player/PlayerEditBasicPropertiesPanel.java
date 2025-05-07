package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.beats.widget.InstrumentCombo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.DrumItem;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.PresetItem;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.ReceiverManager;
import com.angrysurfer.core.service.SoundbankManager;

/**
 * Panel for editing basic player properties with improved state management.
 */
public class PlayerEditBasicPropertiesPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEditBasicPropertiesPanel.class);
    private static final int PREVIEW_DURATION_MS = 500;

    // Player reference
    private Player player;

    // UI Components
    private JTextField nameField;
    private InstrumentCombo instrumentCombo; // Changed from JComboBox<InstrumentWrapper>
    private ChannelCombo channelCombo; // Changed from JSpinner
    private JPanel presetControlPanel;
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JSpinner presetSpinner;
    private JButton loadSoundbankButton;
    private JButton deleteSoundbankButton;
    private JButton previewButton;
    private JComboBox<DrumItem> drumCombo;
    private JComboBox<PresetItem> presetCombo;

    // Service references
    private final InstrumentManager instrumentManager = InstrumentManager.getInstance();
    private final ReceiverManager receiverManager = ReceiverManager.getInstance();
    private final InternalSynthManager synthManager = InternalSynthManager.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();

    // State tracking
    private boolean isInternalSynth = false;
    private boolean isDrumChannel = false;
    private final AtomicBoolean initializing = new AtomicBoolean(false);
    private Long selectedInstrumentId;
    private boolean updating = false;

    // Add these fields to the class
    private int initialChannel = -1; // Initial channel when dialog opens
    private boolean isDrumPlayer = false; // Is this player on channel 9 (drums)
    private InstrumentWrapper initialInstrument; // Initial instrument

    /**
     * Initialize player edit panel with channel caching for drum detection
     */
    public PlayerEditBasicPropertiesPanel(Player player) {
        super(new GridBagLayout());
        this.player = player;

        // Cache initial state - important for detecting changes
        if (player != null) {
            initialChannel = player.getChannel();
            isDrumPlayer = initialChannel == 9;
            initialInstrument = player.getInstrument();
        }

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Basic Properties"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Store initial instrument state before building UI
        if (player.getInstrument() != null) {
            selectedInstrumentId = player.getInstrument().getId();
            isDrumChannel = player.getChannel() == 9;
            isInternalSynth = synthManager.isInternalSynth(player.getInstrument());
        } else {
            // Initialize with defaults if no instrument is set
            isDrumChannel = player.getChannel() == 9;
            isInternalSynth = false;
        }

        initComponents();
        layoutComponents();
        // registerForInstrumentUpdates();
        // registerForDrumEvents();
        registerForCommandBusEvents();

        // Run this after component initialization to ensure proper state
        SwingUtilities.invokeLater(this::updatePresetControls);
    }

    /**
     * Initialize all UI components with improved error handling
     */
    private void initComponents() {
        logger.debug("Initializing components for player {}", player.getName());
        initializing.set(true);

        try {
            // Text field for player name
            nameField = new JTextField(player.getName(), 20);

            // Replace channelSpinner with ChannelCombo
            channelCombo = new ChannelCombo();
            channelCombo.setCurrentPlayer(player);

            // Preset control panel will contain either the presetSpinner or presetCombo
            presetControlPanel = new JPanel(new BorderLayout());

            // Preset spinner for external instruments
            presetSpinner = new JSpinner(new SpinnerNumberModel(
                    player.getPreset() != null ? Math.min(Math.max(player.getPreset(), 0), 127) : 0,
                    0,
                    127,
                    1));

            // Initialize preview button
            previewButton = new JButton("Preview");

            // Preset combo for internal synths
            presetCombo = new JComboBox<>();

            // Drum combo for drum channel
            drumCombo = new JComboBox<>();

            // Replace instrumentCombo initialization with InstrumentCombo
            instrumentCombo = new InstrumentCombo();
            instrumentCombo.setCurrentPlayer(player);

            // Create soundbank panel components
            setupSoundbankPanel();

            // We don't need to add listeners to instrumentCombo and channelCombo
            // as they handle their own events
            addListeners();
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Set up the soundbank panel components
     */
    private void setupSoundbankPanel() {
        // Create components
        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new java.awt.Dimension(60, bankCombo.getPreferredSize().height));
        loadSoundbankButton = new JButton("Load...");
        deleteSoundbankButton = new JButton("Delete");
    }

    /**
     * Add listeners for UI components
     */
    private void addListeners() {
        // Preset spinner change listener
        presetSpinner.addChangeListener(e -> {
            if (initializing.get())
                return;

            int preset = ((Number) presetSpinner.getValue()).intValue();
            player.getInstrument().setPreset(preset);

            // Immediately apply preset change for external instruments
            if (!isInternalSynth && player.getInstrument() != null) {
                try {
                    PlayerManager.getInstance().applyPlayerPreset(player);
                    ;
                } catch (Exception ex) {
                    logger.error("Error applying preset: {}", ex.getMessage());
                }
            }
        });

        // Soundbank combo listener
        soundbankCombo.addActionListener(e -> {
            if (initializing.get() || soundbankCombo.getSelectedItem() == null)
                return;

            String soundbankName = (String) soundbankCombo.getSelectedItem();

            // Update instrument with selected soundbank
            if (player.getInstrument() != null) {
                player.getInstrument().setSoundbankName(soundbankName);

                // Apply soundbank change
                Soundbank soundbank = SoundbankManager.getInstance().getSoundbankByName(soundbankName);
                if (soundbank != null && player.getDevice() instanceof Synthesizer) {
                    try {
                        Synthesizer synth = (Synthesizer) player.getDevice();
                        synth.loadAllInstruments(soundbank);
                    } catch (Exception ex) {
                        logger.error("Error loading soundbank instruments: {}", ex.getMessage());
                    }
                }

                // Update instrument through manager
                instrumentManager.updateInstrument(player.getInstrument());

                // Apply preset changes
                PlayerManager.getInstance().applyPlayerPreset(player);
            }

            // Populate banks for this soundbank
            populateBanksCombo();
        });

        // Bank combo listener to populate preset combo
        bankCombo.addActionListener(e -> {
            if (initializing.get())
                return;

            Integer selectedBank = (Integer) bankCombo.getSelectedItem();
            if (selectedBank != null) {
                populatePresetComboForBank(selectedBank);
            }
        });

        // Preset combo listener
        presetCombo.addActionListener(e -> {
            if (initializing.get() || !(presetCombo.getSelectedItem() instanceof PresetItem))
                return;

            PresetItem item = (PresetItem) presetCombo.getSelectedItem();

            // Update player's preset
            player.getInstrument().setPreset(item.getNumber());

            // Update instrument's current preset
            if (player.getInstrument() != null) {
                player.getInstrument().setPreset(item.getNumber());

                // Update instrument through manager
                instrumentManager.updateInstrument(player.getInstrument());

                // Apply preset change
                PlayerManager.getInstance().applyPlayerPreset(player);
            }
        });

        // Drum combo listener
        drumCombo.addActionListener(e -> {
            if (initializing.get())
                return;

            if (drumCombo.getSelectedItem() instanceof DrumItem) {
                DrumItem item = (DrumItem) drumCombo.getSelectedItem();
                if (player != null) {
                    player.setRootNote(item.getNoteNumber());
                }
            }
        });

        // Load soundbank button listener
        loadSoundbankButton.addActionListener(e -> loadSoundbankFile());

        // Delete soundbank button listener
        deleteSoundbankButton.addActionListener(e -> {
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            if (selectedSoundbank == null) {
                JOptionPane.showMessageDialog(this,
                        "No soundbank selected",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Don't allow deleting the default Java soundbank
            if ("Java Internal Soundbank".equals(selectedSoundbank)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete the default Java soundbank",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirm deletion
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete soundbank: " + selectedSoundbank + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    // Delete the soundbank
                    boolean deleted = SoundbankManager.getInstance().deleteSoundbank(selectedSoundbank);
                    if (deleted) {
                        // Update UI
                        initializeSoundbanks();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to delete soundbank",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    logger.error("Error deleting soundbank: {}", ex.getMessage());
                }
            }
        });

        // Preview button listener
        previewButton.addActionListener(e -> {
            if (isDrumChannel) {
                // For drum channel, preview the selected drum
                if (drumCombo.getSelectedItem() instanceof DrumItem) {
                    DrumItem drum = (DrumItem) drumCombo.getSelectedItem();
                    playDrumPreview(drum.getNoteNumber());
                }
            } else {
                // For melodic channel, preview the current preset
                playPreviewNote();
            }
        });
    }

    /**
     * Layout all components in the panel with improved organization
     */
    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Combined row with Name, Instrument and Channel
        JPanel topRow = new JPanel(new GridBagLayout());
        GridBagConstraints trGbc = new GridBagConstraints();
        trGbc.insets = new Insets(2, 5, 2, 5);
        trGbc.fill = GridBagConstraints.HORIZONTAL;

        // Name label and field
        trGbc.gridx = 0;
        trGbc.gridy = 0;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Name:"), trGbc);

        trGbc.gridx = 1;
        trGbc.weightx = 0.3;
        nameField.setPreferredSize(new Dimension(100, nameField.getPreferredSize().height));
        topRow.add(nameField, trGbc);

        // Instrument label and combo
        trGbc.gridx = 2;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Instrument:"), trGbc);

        trGbc.gridx = 3;
        trGbc.weightx = 0.5;
        instrumentCombo.setPreferredSize(new Dimension(150, instrumentCombo.getPreferredSize().height));
        topRow.add(instrumentCombo, trGbc);

        // Channel label and combo (replacing spinner)
        trGbc.gridx = 4;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Channel:"), trGbc);

        trGbc.gridx = 5;
        trGbc.weightx = 0.1;
        channelCombo.setPreferredSize(new Dimension(60, channelCombo.getPreferredSize().height));
        topRow.add(channelCombo, trGbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        add(topRow, gbc);
        gbc.gridwidth = 1;

        // Row 1: Soundbank row
        JPanel soundbankRow = new JPanel(new GridBagLayout());
        GridBagConstraints sbGbc = new GridBagConstraints();
        sbGbc.insets = new Insets(2, 5, 2, 5);
        sbGbc.fill = GridBagConstraints.HORIZONTAL;

        sbGbc.gridx = 0;
        sbGbc.gridy = 0;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Soundbank:"), sbGbc);

        sbGbc.gridx = 1;
        sbGbc.weightx = 1.0;
        soundbankRow.add(soundbankCombo, sbGbc);

        sbGbc.gridx = 2;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Bank:"), sbGbc);

        sbGbc.gridx = 3;
        sbGbc.weightx = 0.0;
        bankCombo.setPreferredSize(new Dimension(60, bankCombo.getPreferredSize().height));
        soundbankRow.add(bankCombo, sbGbc);

        sbGbc.gridx = 4;
        sbGbc.weightx = 0.0;
        soundbankRow.add(loadSoundbankButton, sbGbc);

        sbGbc.gridx = 5;
        sbGbc.weightx = 0.0;
        soundbankRow.add(deleteSoundbankButton, sbGbc);

        JButton debugButton = new JButton("Debug");
        debugButton.addActionListener(e -> {
            try {
                boolean synthAvailable = synthManager.checkInternalSynthAvailable();
                List<String> bankNames = synthManager.getSoundbankNames();
                String msg = "Internal Synth Available: " + synthAvailable +
                        "\nSoundbank count: " + (bankNames != null ? bankNames.size() : 0) +
                        "\nBanks: " + (bankNames != null ? String.join(", ", bankNames) : "none");
                JOptionPane.showMessageDialog(this, msg, "Synth Status", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error checking synth: " + ex.getMessage(),
                        "Debug Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        sbGbc.gridx = 6;
        sbGbc.weightx = 0.0;
        soundbankRow.add(debugButton, sbGbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        add(soundbankRow, gbc);
        gbc.gridwidth = 1;

        // Row 2: Preset row
        JPanel presetPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pGbc = new GridBagConstraints();
        pGbc.insets = new Insets(2, 5, 2, 5);
        pGbc.fill = GridBagConstraints.HORIZONTAL;

        pGbc.gridx = 0;
        pGbc.gridy = 0;
        pGbc.weightx = 0.0;
        presetPanel.add(new JLabel("Preset:"), pGbc);

        pGbc.gridx = 1;
        pGbc.weightx = 1.0;
        presetPanel.add(presetControlPanel, pGbc);

        pGbc.gridx = 2;
        pGbc.weightx = 0.0;
        presetPanel.add(previewButton, pGbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        add(presetPanel, gbc);
        gbc.gridwidth = 1;

        // Row 3: Empty space
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc);
    }

    /**
     * Update preset controls based on instrument type and channel with improved
     * error handling
     */
    private void updatePresetControls() {
        // Set initializing flag to prevent listener events
        initializing.set(true);

        try {
            logger.debug("Updating preset controls - isDrumChannel: {}, isInternalSynth: {}", isInternalSynth);

            // Clear preset control panel
            presetControlPanel.removeAll();

            // Update control visibility based on state
            boolean showSoundbankControls = !isDrumChannel && isInternalSynth;
            loadSoundbankButton.setEnabled(showSoundbankControls);
            soundbankCombo.setEnabled(showSoundbankControls);
            bankCombo.setEnabled(showSoundbankControls);
            deleteSoundbankButton.setEnabled(showSoundbankControls);

            if (isDrumChannel) {
                // For drum use drum kit selector
                presetControlPanel.add(drumCombo, BorderLayout.CENTER);

                // Populate with drum sounds
                populateDrumCombo();

            } else {
                if (isInternalSynth) {
                    // For internal melodic instruments, use preset selector
                    presetControlPanel.add(presetCombo, BorderLayout.CENTER);

                    // Initialize soundbanks
                    initializeSoundbanks();
                } else {
                    // For external instruments, use preset spinner
                    presetControlPanel.add(presetSpinner, BorderLayout.CENTER);
                }
            }

            // Refresh UI
            revalidate();
            repaint();
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Apply changes to the player based on the current UI state.
     */
    public void applyToPlayer() {
        // Apply name
        player.setName(nameField.getText());

        // We don't need to apply channel changes here since ChannelCombo does this
        // automatically

        // Apply preset based on UI state
        if (!isInternalSynth && !isDrumChannel && presetSpinner.isVisible()) {
            // ...existing code...
        }

        // Apply instrument settings if available
        if (player.getInstrument() != null) {
            // ...existing code...
        }
    }

    /**
     * Apply all changes to the player with error handling.
     */
    public void applyChanges() {
        if (initializing.get()) {
            return;
        }

        try {
            // Apply name
            player.setName(nameField.getText());

            // We don't need to manually apply instrument or channel changes
            // since the specialized components handle this automatically

            // Apply soundbank and preset settings
            if (player.getInstrument() != null) {
                // ...existing code...
            }

        } catch (Exception e) {
            logger.error("Error applying changes: {}", e.getMessage(), e);
        }
    }

    /**
     * Update the panel from a new player instance.
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        // Update player reference
        this.player = newPlayer;

        boolean wasInitializing = initializing.get();
        initializing.set(true);

        try {
            // Update name field
            if (nameField != null) {
                nameField.setText(newPlayer.getName());
            }

            // Update specialized components
            if (instrumentCombo != null) {
                instrumentCombo.setCurrentPlayer(newPlayer);
            }

            if (channelCombo != null) {
                channelCombo.setCurrentPlayer(newPlayer);
            }

            // Rest of update logic remains the same
            // ...

        } finally {
            initializing.set(wasInitializing);
        }
    }

    /**
     * Register for relevant command bus events to detect changes
     */
    private void registerForCommandBusEvents() {
        commandBus.register(action -> {
            // Only process if this is our player
            if (player == null || action.getData() == null)
                return;

            boolean playerUpdated = false;

            // Handle different event types
            switch (action.getCommand()) {
                case Commands.PLAYER_UPDATED:
                    // Check if this is our player
                    if (action.getData() instanceof Player updatedPlayer &&
                            updatedPlayer.getId().equals(player.getId())) {

                        // Check if anything relevant changed
                        boolean channelChanged = !Objects.equals(player.getChannel(), updatedPlayer.getChannel());
                        boolean instrumentChanged = !Objects.equals(
                                player.getInstrumentId(), updatedPlayer.getInstrumentId());

                        // Update our reference
                        player = updatedPlayer;

                        // Only update UI if relevant properties changed
                        if (channelChanged || instrumentChanged) {
                            logger.debug("Player updated: channel changed={}, instrument changed={}",
                                    channelChanged, instrumentChanged);

                            // Update UI state flags
                            isDrumChannel = player.getChannel() == 9;
                            isInternalSynth = player.getInstrument() != null &&
                                    synthManager.isInternalSynth(player.getInstrument());

                            // Update UI components
                            SwingUtilities.invokeLater(this::updatePresetControls);
                        }
                    }
                    break;
            }
        });
    }

    /**
     * Load a soundbank file
     */
    private void loadSoundbankFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".sf2") 
                    || f.getName().toLowerCase().endsWith(".dls");
            }

            @Override
            public String getDescription() {
                return "Soundbank Files (*.sf2, *.dls)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Use SoundbankManager instead of InternalSynthManager
            CommandBus.getInstance().publish(Commands.LOAD_SOUNDBANK, this, selectedFile);
            
            // Register for one-time notification when soundbank is loaded
            CommandBus.getInstance().registerOneTime(action -> {
                if (Commands.SOUNDBANK_LOADED.equals(action.getCommand()) && 
                    action.getData() instanceof String) {
                    
                    String soundbankName = (String) action.getData();
                    SwingUtilities.invokeLater(() -> {
                        // Refresh soundbanks
                        initializeSoundbanks();
                        // Select the newly loaded soundbank
                        soundbankCombo.setSelectedItem(soundbankName);
                    });
                }
            });
        }
    }

    /**
     * Initialize soundbanks in the combo box
     */
    private void initializeSoundbanks() {
        // Store current selection
        String currentSelection = soundbankCombo.getSelectedItem() != null 
                ? soundbankCombo.getSelectedItem().toString() : null;
                
        // Clear the combo
        soundbankCombo.removeAllItems();
        
        // Get available soundbanks from SoundbankManager
        List<String> soundbanks = SoundbankManager.getInstance().getSoundbankNames();
        
        // Add them to combo
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }
        
        // Restore selection if possible
        if (currentSelection != null) {
            soundbankCombo.setSelectedItem(currentSelection);
        } else if (soundbankCombo.getItemCount() > 0) {
            soundbankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Play a preview note with current instrument settings
     */
    private void playPreviewNote() {
        if (player == null || player.getChannel() == 9) {
            return; // Don't preview for drum channel
        }

        try {
            // Apply current settings to player's instrument
            if (player.getInstrument() != null) {
                // Get settings from UI
                if (soundbankCombo.isVisible() && soundbankCombo.getSelectedItem() != null) {
                    player.getInstrument().setSoundbankName(soundbankCombo.getSelectedItem().toString());
                }

                if (bankCombo.isVisible() && bankCombo.getSelectedItem() != null) {
                    player.getInstrument().setBankIndex((Integer) bankCombo.getSelectedItem());
                }

                if (presetCombo.isVisible() && presetCombo.getSelectedItem() instanceof PresetItem) {
                    player.getInstrument().setPreset(((PresetItem) presetCombo.getSelectedItem()).getNumber());
                }

                // Apply settings
                InternalSynthManager synthManager = InternalSynthManager.getInstance();
                synthManager.applyPresetChange(
                        player.getInstrument(),
                        player.getInstrument().getBankIndex() != null ? player.getInstrument().getBankIndex() : 0,
                        player.getInstrument().getPreset() != null ? player.getInstrument().getPreset() : 0);

                // Play middle C for 500ms on the instrument's channel
                synthManager.playNote(60, 100, 500, player.getChannel());
            }
        } catch (Exception e) {
            logger.error("Error playing preview note: {}", e.getMessage());
        }
    }

    /**
     * Populate drum combo with GM drum sounds
     */
    private void populateDrumCombo() {
        if (drumCombo == null) {
            return;
        }

        // Store current selection
        DrumItem currentSelection = drumCombo.getSelectedItem() instanceof DrumItem
                ? (DrumItem) drumCombo.getSelectedItem()
                : null;
        int currentNoteNumber = currentSelection != null ? currentSelection.getNoteNumber() : -1;

        // Clear the combo
        drumCombo.removeAllItems();

        // Get drum sounds from manager
        List<DrumItem> drums = InternalSynthManager.getInstance().getDrumItems();

        // Add them to combo
        for (DrumItem drum : drums) {
            drumCombo.addItem(drum);
        }

        // Restore selection if possible
        if (currentNoteNumber >= 0) {
            for (int i = 0; i < drumCombo.getItemCount(); i++) {
                if (drumCombo.getItemAt(i) instanceof DrumItem) {
                    DrumItem item = (DrumItem) drumCombo.getItemAt(i);
                    if (item.getNoteNumber() == currentNoteNumber) {
                        drumCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else if (player != null && player.getRootNote() != null) {
            // Select by player's root note
            for (int i = 0; i < drumCombo.getItemCount(); i++) {
                if (drumCombo.getItemAt(i) instanceof DrumItem) {
                    DrumItem item = (DrumItem) drumCombo.getItemAt(i);
                    if (item.getNoteNumber() == player.getRootNote()) {
                        drumCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else if (drumCombo.getItemCount() > 0) {
            // Default to first item
            drumCombo.setSelectedIndex(0);
        }
    }

    /**
     * Populate preset combo with presets for a specific bank
     *
     * @param bankIndex The bank index to get presets for
     */
    private void populatePresetComboForBank(Integer bankIndex) {
        if (presetCombo == null || bankIndex == null) {
            return;
        }

        // Store current selection
        PresetItem currentSelection = presetCombo.getSelectedItem() instanceof PresetItem
                ? (PresetItem) presetCombo.getSelectedItem()
                : null;
        int currentPreset = currentSelection != null ? currentSelection.getNumber() : -1;

        // Clear the combo
        presetCombo.removeAllItems();

        // Get soundbank name
        String soundbankName = soundbankCombo.getSelectedItem() != null
                ? soundbankCombo.getSelectedItem().toString()
                : "Java Internal Soundbank";

        // Get preset names for this soundbank and bank
        List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(soundbankName, bankIndex);

        // Add all presets to combo
        for (int i = 0; i < presetNames.size(); i++) {
            String name = presetNames.get(i);
            if (name != null && !name.isEmpty()) {
                presetCombo.addItem(new PresetItem(i, name));
            }
        }

        // If no presets were found, use GM names
        if (presetCombo.getItemCount() == 0) {
            List<String> gmPresets = InternalSynthManager.getInstance().getGeneralMIDIPresetNames();
            for (int i = 0; i < gmPresets.size(); i++) {
                presetCombo.addItem(new PresetItem(i, gmPresets.get(i)));
            }
        }

        // Restore selection if possible
        if (currentPreset >= 0) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                if (presetCombo.getItemAt(i) instanceof PresetItem) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == currentPreset) {
                        presetCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }

        // Select player's preset if set
        if (player != null && player.getInstrument() != null && player.getInstrument().getPreset() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                if (presetCombo.getItemAt(i) instanceof PresetItem) {
                    PresetItem item = (PresetItem) presetCombo.getItemAt(i);
                    if (item.getNumber() == player.getInstrument().getPreset()) {
                        presetCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }

        // Default to first item
        if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    /**
     * Play a preview of a drum sound
     *
     * @param noteNumber The MIDI note number of the drum sound
     */
    private void playDrumPreview(int noteNumber) {
        try {
            // Get the drum channel (9)
            int drumChannel = 9;

            // Send bank select and program change for GM drum kit
            if (player != null && player.getInstrument() != null) {
                player.getInstrument().controlChange(0, 0); // Bank MSB (standard drum kits)
                player.getInstrument().controlChange(32, 0); // Bank LSB
                player.getInstrument().programChange(0, 0); // Standard kit
            }

            // Play the drum note with InternalSynthManager
            InternalSynthManager.getInstance().playNote(noteNumber, 100, 500, drumChannel);
        } catch (Exception e) {
            logger.error("Error playing drum preview: {}", e.getMessage());
        }
    }

    /**
     * PresetItem inner class for preset combo box
     */
    private static class PresetItem {
        private final int number;
        private final String name;

        public PresetItem(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return number + ": " + name;
        }
    }

    /**
     * Populate bank combo with available banks for the selected soundbank
     */
    private void populateBanksCombo() {
        if (bankCombo == null || soundbankCombo.getSelectedItem() == null) {
            return;
        }

        // Store current selection
        Integer currentBank = bankCombo.getSelectedItem() != null
                ? (Integer) bankCombo.getSelectedItem()
                : null;

        // Get the selected soundbank
        String soundbankName = soundbankCombo.getSelectedItem().toString();

        // Clear the bank combo
        bankCombo.removeAllItems();

        try {
            // Use getAvailableBanksByName instead of getBanksForSoundbank
            List<Integer> banks = InternalSynthManager.getInstance().getAvailableBanksByName(soundbankName);

            if (banks == null || banks.isEmpty()) {
                // Add default GM bank (0) if no banks found
                bankCombo.addItem(0);
            } else {
                // Add all available banks
                for (Integer bank : banks) {
                    bankCombo.addItem(bank);
                }
            }

            // Restore selection if possible
            if (currentBank != null) {
                bankCombo.setSelectedItem(currentBank);
            } else if (player != null && player.getInstrument() != null &&
                    player.getInstrument().getBankIndex() != null) {
                // Use player's bank
                bankCombo.setSelectedItem(player.getInstrument().getBankIndex());
            } else if (bankCombo.getItemCount() > 0) {
                // Default to first bank
                bankCombo.setSelectedIndex(0);
            }

            // Populate presets for the selected bank
            if (bankCombo.getSelectedItem() != null) {
                populatePresetComboForBank((Integer) bankCombo.getSelectedItem());
            }

        } catch (Exception e) {
            logger.error("Error populating banks combo: {}", e.getMessage(), e);
        }
    }
}

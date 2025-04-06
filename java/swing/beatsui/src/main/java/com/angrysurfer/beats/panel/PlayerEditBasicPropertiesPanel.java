package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumItem;
import com.angrysurfer.core.sequencer.PresetItem;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.UserConfigManager;

/**
 * Panel for editing basic player properties including instrument selection,
 * channel, and preset/soundbank settings.
 */
public class PlayerEditBasicPropertiesPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEditBasicPropertiesPanel.class);

    // Player reference
    private Player player;

    // UI Components
    private JTextField nameField;
    private JComboBox<InstrumentWrapper> instrumentCombo;
    private JSpinner channelSpinner;
    private JPanel presetControlPanel;

    // Sound panel components
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JSpinner presetSpinner;
    private JButton loadSoundbankButton;
    private JButton deleteSoundbankButton;
    private JButton previewButton;

    // Special Components for Drum Channel
    private JComboBox<DrumItem> drumCombo;

    // Preset combo for internal synths
    private JComboBox<PresetItem> presetCombo;

    // State tracking
    private boolean isInternalSynth = false;
    private boolean isDrumChannel = false;
    private boolean initializing = false;

    /**
     * Create a new basic properties panel for a player
     *
     * @param player The player whose properties to edit
     */
    public PlayerEditBasicPropertiesPanel(Player player) {
        super(new GridBagLayout());
        this.player = player;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Basic Properties"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Initialize components
        initComponents();

        // Layout components
        layoutComponents();

        // Register for instrument updates
        registerForInstrumentUpdates();

        // Initialize state based on player settings
        updatePresetControls();

        // Make sure the button action works
        loadSoundbankButton.removeActionListener(loadSoundbankButton.getActionListeners()[0]);
        loadSoundbankButton.addActionListener(e -> {
            logger.info("Load soundbank button clicked");
            loadSoundbankFile();
        });
    }

    /**
     * Initialize all UI components
     */
    private void initComponents() {
        // Text field for player name
        nameField = new JTextField(player.getName(), 20);

        // Channel spinner (0-15)
        channelSpinner = new JSpinner(new SpinnerNumberModel(
                (long) player.getChannel(), // value
                0, // min
                15, // max
                1 // step
        ));

        // Preset control panel will contain either the presetSpinner or presetCombo
        presetControlPanel = new JPanel(new BorderLayout());

        // Preset spinner for external instruments
        presetSpinner = new JSpinner(new SpinnerNumberModel(
                player.getPreset() != null ? player.getPreset().intValue() : 0, // value
                0, // min
                127, // max
                1 // step
        ));

        // Initialize preview button
        previewButton = new JButton("Preview");

        // Preset combo for internal synths
        presetCombo = new JComboBox<>();

        // Drum combo for drum channel
        drumCombo = new JComboBox<>();

        // Initialize instrument combo with all available instruments
        setupInstrumentCombo();

        // Create soundbank panel components
        setupSoundbankPanel();

        // Add listeners
        addListeners();
    }

    /**
     * Set up the instrument combo box
     */
    private void setupInstrumentCombo() {
        instrumentCombo = new JComboBox<>();

        // Custom renderer to show instrument names
        instrumentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InstrumentWrapper) {
                    setText(((InstrumentWrapper) value).getName());
                }
                return this;
            }
        });

        // Get all available instruments
        List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();

        // Add internal synthesizers
        instruments.addAll(InternalSynthManager.getInstance().getInternalSynths());

        // Add instruments to combo box
        if (instruments != null && !instruments.isEmpty()) {
            for (InstrumentWrapper instrument : instruments) {
                if (instrument.getAvailable() && instrument.getDevice() != null) {
                    instrumentCombo.addItem(instrument);
                }
            }
        }

        // Select the player's current instrument
        if (player.getInstrument() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (item.getId().equals(player.getInstrument().getId())) {
                    instrumentCombo.setSelectedIndex(i);
                    // Check if it's an internal synth
                    isInternalSynth = InternalSynthManager.getInstance().isInternalSynth(item);
                    break;
                }
            }
        }
    }

    /**
     * Set up the soundbank panel components
     */
    private void setupSoundbankPanel() {
        // Create components
        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        // Limit bank combo's width to simulate 4 columns wide
        bankCombo.setPreferredSize(new java.awt.Dimension(60, bankCombo.getPreferredSize().height));
        loadSoundbankButton = new JButton("Load...");
        deleteSoundbankButton = new JButton("Delete");

        // Note: We no longer add these components to the soundbankPanel here
        // because they'll be added directly to the main layout in layoutComponents()
    }

    /**
     * Add listeners to UI components
     */
    private void addListeners() {
        // Instrument selection listener
        instrumentCombo.addActionListener(e -> {
            InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
            if (selectedInstrument != null) {
                // Update player instrument
                player.setInstrument(selectedInstrument);
                player.setInstrumentId(selectedInstrument.getId());

                // Check if internal synth
                boolean isInternal = InternalSynthManager.getInstance().isInternalSynth(selectedInstrument);

                // Update UI if instrument type changed
                if (isInternal != isInternalSynth) {
                    isInternalSynth = isInternal;
                    updatePresetControls();
                }

                // Initialize soundbanks if internal synth
                if (isInternal) {
                    initializeSoundbanks();
                }
            }
        });

        // Channel change listener
        channelSpinner.addChangeListener(e -> {
            int channel = ((Number) channelSpinner.getValue()).intValue();
            player.setChannel(channel);

            // Check if this is the drum channel (9)
            boolean newIsDrumChannel = (channel == 9);
            if (newIsDrumChannel != isDrumChannel) {
                isDrumChannel = newIsDrumChannel;
                updatePresetControls();
            }
        });

        // Preset spinner change listener
        presetSpinner.addChangeListener(e -> {
            if (!initializing) {
                int preset = ((Number) presetSpinner.getValue()).intValue();
                player.setPreset(preset);
            }
        });

        // Soundbank combo listener
        soundbankCombo.addActionListener(e -> {
            if (!initializing && soundbankCombo.getSelectedItem() != null) {
                String soundbankName = (String) soundbankCombo.getSelectedItem();

                List<String> names = InternalSynthManager.getInstance().getSoundbankNames();
                // Update instrument with selected soundbank
                if (player.getInstrument() != null) {
                    player.getInstrument().setSoundbankName(soundbankName);
                    applyPresetChange();

                    Soundbank soundbank = InternalSynthManager.getInstance().getSoundbank(soundbankName);
                    // if (soundbank != null)
                    if (soundbank != null) {
                        ((Synthesizer) player.getInstrument().getDevice()).loadAllInstruments(soundbank);
                    }
                }

                // Populate banks for this soundbank
                populateBanksCombo();
            }
        });

        // Bank combo listener
        bankCombo.addActionListener(e -> {
            if (!initializing && bankCombo.getSelectedItem() != null) {
                Integer bank = (Integer) bankCombo.getSelectedItem();

                // Update instrument with selected bank
                if (player.getInstrument() != null) {
                    player.getInstrument().setBankIndex(bank);
                    applyPresetChange();
                }

                // Populate presets for this bank
                populatePresetComboForBank(bank);
            }
        });

        // Preset combo listener
        presetCombo.addActionListener(e -> {
            if (!initializing && presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();

                // Update player's preset
                player.setPreset(item.getNumber());

                // Update instrument's current preset
                if (player.getInstrument() != null) {
                    player.getInstrument().setCurrentPreset(item.getNumber());
                }

                // Apply preset change to instrument
                if (isInternalSynth) {
                    try {
                        applyPresetChange();
                    } catch (Exception ex) {
                        logger.error("Error applying preset change: {}", ex.getMessage());
                    }
                }
            }
        });

        // Drum combo listener
        drumCombo.addActionListener(e -> {
            if (!initializing && drumCombo.getSelectedItem() instanceof DrumItem) {
                DrumItem item = (DrumItem) drumCombo.getSelectedItem();

                // For drum channel, store note in rootNote
                player.setRootNote(Integer.valueOf(item.getNoteNumber()));

                // Preview the drum sound
                playDrumPreview(item.getNoteNumber());
            }
        });

        // Replace your existing listener with this direct approach
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
            
            // Confirm deletion
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete soundbank: " + selectedSoundbank + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                try {
                    // Call InternalSynthManager to delete the soundbank
                    boolean deleted = InternalSynthManager.getInstance().deleteSoundbank(selectedSoundbank);
                    
                    if (deleted) {
                        // Refresh the UI
                        initializeSoundbanks();
                        JOptionPane.showMessageDialog(this,
                                "Soundbank deleted: " + selectedSoundbank,
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to delete soundbank",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    logger.error("Error deleting soundbank: {}", ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(this,
                            "Error deleting soundbank: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Preview button listener
        previewButton.addActionListener(e -> playPreviewNote());
    }

    /**
     * Layout all components in the panel
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

        // Name label and field (shortened)
        trGbc.gridx = 0;
        trGbc.gridy = 0;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Name:"), trGbc);

        trGbc.gridx = 1;
        trGbc.weightx = 0.3; // Shorter width for name field
        // Set preferred size to make name field shorter
        nameField.setPreferredSize(new Dimension(100, nameField.getPreferredSize().height));
        topRow.add(nameField, trGbc);

        // Instrument label and combo (shortened)
        trGbc.gridx = 2;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Instrument:"), trGbc);

        trGbc.gridx = 3;
        trGbc.weightx = 0.5; // More space for instrument, but still constrained
        // Set preferred size to make instrument combo shorter
        instrumentCombo.setPreferredSize(new Dimension(150, instrumentCombo.getPreferredSize().height));
        topRow.add(instrumentCombo, trGbc);

        // Channel label and spinner
        trGbc.gridx = 4;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Channel:"), trGbc);

        trGbc.gridx = 5;
        trGbc.weightx = 0.1;
        // Set channel spinner preferred size (4 columns wide max)
        channelSpinner.setPreferredSize(new Dimension(60, channelSpinner.getPreferredSize().height));
        topRow.add(channelSpinner, trGbc);

        // Add the combined top row to the main layout
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        add(topRow, gbc);
        gbc.gridwidth = 1;

        // Row 1: Soundbank row (previously Row 2)
        JPanel soundbankRow = new JPanel(new GridBagLayout());
        GridBagConstraints sbGbc = new GridBagConstraints();
        sbGbc.insets = new Insets(2, 5, 2, 5);
        sbGbc.fill = GridBagConstraints.HORIZONTAL;

        // Column 0: Soundbank label
        sbGbc.gridx = 0;
        sbGbc.gridy = 0;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Soundbank:"), sbGbc);

        // Column 1: Soundbank combo
        sbGbc.gridx = 1;
        sbGbc.weightx = 1.0;
        soundbankRow.add(soundbankCombo, sbGbc);

        // Column 2: Bank label
        sbGbc.gridx = 2;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Bank:"), sbGbc);

        // Column 3: Bank combo
        sbGbc.gridx = 3;
        sbGbc.weightx = 0.0;
        // Limit bank combo width to 4 columns
        bankCombo.setPreferredSize(new Dimension(60, bankCombo.getPreferredSize().height));
        soundbankRow.add(bankCombo, sbGbc);

        // Column 4: Load button
        sbGbc.gridx = 4;
        sbGbc.weightx = 0.0;
        soundbankRow.add(loadSoundbankButton, sbGbc);

        // Column 5: Delete button
        sbGbc.gridx = 5;
        sbGbc.weightx = 0.0;
        soundbankRow.add(deleteSoundbankButton, sbGbc);

        // Add the soundbank row to the main panel (now at row 1)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        add(soundbankRow, gbc);
        gbc.gridwidth = 1;

        // Row 2: Preset row (now moved up)
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
        gbc.gridy = 2;  // Now at row 2
        gbc.gridwidth = 4;
        add(presetPanel, gbc);
        gbc.gridwidth = 1;

        // Row 3: Sound panel row (mostly empty since we moved components)
        gbc.gridx = 0;
        gbc.gridy = 3;  // Now at row 3
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc); // Placeholder for sound panel
        gbc.gridwidth = 1;
    }

    /**
     * Update preset controls based on instrument type and channel
     */
    private void updatePresetControls() {
        // Clear preset control panel
        presetControlPanel.removeAll();

        loadSoundbankButton.setEnabled(!isDrumChannel && isInternalSynth);
        soundbankCombo.setEnabled(!isDrumChannel && isInternalSynth);
        bankCombo.setEnabled(!isDrumChannel && isInternalSynth);

        if (isDrumChannel) {
            // For drum channel, use drum kit selector
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
    }

    /**
     * Register for instrument update notifications
     */
    private void registerForInstrumentUpdates() {
        CommandBus.getInstance().register(action -> {
            if (action.getCommand() == null) {
                return;
            }

            if (Commands.INSTRUMENT_UPDATED.equals(action.getCommand())
                    || Commands.USER_CONFIG_LOADED.equals(action.getCommand())) {

                // Refresh instruments in UI
                SwingUtilities.invokeLater(() -> {
                    // Remember current selection
                    InstrumentWrapper selected = (InstrumentWrapper) instrumentCombo.getSelectedItem();

                    // Clear and repopulate
                    instrumentCombo.removeAllItems();

                    // Get all available instruments
                    List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();
                    instruments.addAll(InternalSynthManager.getInstance().getInternalSynths());

                    for (InstrumentWrapper instrument : instruments) {
                        if (instrument.getAvailable() && instrument.getDevice() != null) {
                            instrumentCombo.addItem(instrument);
                        }
                    }

                    // Try to restore previous selection
                    if (selected != null) {
                        for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                            InstrumentWrapper item = instrumentCombo.getItemAt(i);
                            if (item.getId().equals(selected.getId())) {
                                instrumentCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Initialize soundbanks with enhanced logging
     */
    public void initializeSoundbanks() {
        try {
            initializing = true;

            // Debug: show state before we start
            logger.info("=== INITIALIZING SOUNDBANKS ===");
            logger.info("Current player instrument: {}",
                    player.getInstrument() != null ? player.getInstrument().getName() : "null");
            logger.info("Current soundbank: {}",
                    player.getInstrument() != null ? player.getInstrument().getSoundbankName() : "null");

            // Clear existing items
            soundbankCombo.removeAllItems();

            // Force the InternalSynthManager to initialize its soundbanks
            InternalSynthManager.getInstance().initializeSoundbanks();

            // Get soundbank names
            List<String> names = InternalSynthManager.getInstance().getSoundbankNames();

            // Debug output
            logger.info("Retrieved {} soundbanks from InternalSynthManager", names.size());
            
            // Filter out empty names and sort alphabetically
            List<String> filteredAndSorted = names.stream()
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .sorted()
                    .collect(Collectors.toList());
            
            logger.info("After filtering and sorting, have {} soundbanks", filteredAndSorted.size());
            
            // Add to combo box
            for (String name : filteredAndSorted) {
                soundbankCombo.addItem(name);
                logger.info("  Added soundbank: {}", name);
            }

            // Select appropriate soundbank based on player settings
            boolean selected = false;
            if (player.getInstrument() != null && player.getInstrument().getSoundbankName() != null) {
                String savedSoundbank = player.getInstrument().getSoundbankName();
                logger.info("Trying to select saved soundbank: {}", savedSoundbank);

                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    if (soundbankCombo.getItemAt(i).equals(savedSoundbank)) {
                        logger.info("Found saved soundbank at index {}", i);
                        soundbankCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first soundbank if none selected
            if (!selected && soundbankCombo.getItemCount() > 0) {
                logger.info("Using default soundbank at index 0");
                soundbankCombo.setSelectedIndex(0);
            }

            // Log the final selection
            logger.info("Final soundbank selection: {}",
                    soundbankCombo.getSelectedItem() != null ? soundbankCombo.getSelectedItem() : "null");

            // Populate banks for selected soundbank
            populateBanksCombo();

            initializing = false;
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage(), e);
            initializing = false;
        }
    }

    /**
     * Populate the bank combo box based on selected soundbank
     */
    private void populateBanksCombo() {
        try {
            initializing = true;

            // Debug: show state before we start
            logger.info("=== POPULATING BANKS ===");
            logger.info("Selected soundbank: {}",
                    soundbankCombo.getSelectedItem() != null ? soundbankCombo.getSelectedItem() : "null");

            // Clear existing banks
            bankCombo.removeAllItems();

            // Get the selected soundbank
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            if (selectedSoundbank == null) {
                logger.warn("No soundbank selected - cannot populate banks");
                initializing = false;
                return;
            }

            // Get available banks for this soundbank
            List<Integer> banks = InternalSynthManager.getInstance().getAvailableBanksByName(selectedSoundbank);
            logger.info("Found {} banks for soundbank '{}'", banks.size(), selectedSoundbank);

            // Add banks to combo
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
                logger.info("  Added bank: {}", bank);
            }

            // Select bank from player's instrument if available
            boolean selected = false;
            if (player.getInstrument() != null && player.getInstrument().getBankIndex() != null) {
                Integer savedBank = player.getInstrument().getBankIndex();
                logger.info("Trying to select saved bank: {}", savedBank);

                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    if (bankCombo.getItemAt(i).equals(savedBank)) {
                        logger.info("Found saved bank at index {}", i);
                        bankCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first bank if none selected
            if (!selected && bankCombo.getItemCount() > 0) {
                logger.info("Using default bank at index 0");
                bankCombo.setSelectedIndex(0);
            }

            // Get selected bank
            Integer bank = (Integer) bankCombo.getSelectedItem();
            logger.info("Final bank selection: {}", bank != null ? bank : "null");

            // Populate presets for this bank
            if (bank != null) {
                populatePresetComboForBank(bank);
            }

            initializing = false;
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage(), e);
            initializing = false;
        }
    }

    /**
     * Populate the preset combo box for a specific bank
     */
    private void populatePresetComboForBank(int bank) {
        try {
            initializing = true;

            // Clear existing presets
            presetCombo.removeAllItems();

            // Get soundbank name
            String soundbankName = (String) soundbankCombo.getSelectedItem();
            if (soundbankName == null) {
                initializing = false;
                return;
            }

            // Get presets for this soundbank and bank
            List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(soundbankName, bank);

            // Add presets to combo
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String name = presetNames.get(i);
                if (name == null || name.isEmpty()) {
                    name = "Program " + i;
                }
                presetCombo.addItem(new PresetItem(i, i + ": " + name));
            }

            // Select preset from player if available
            boolean selected = false;
            if (player.getPreset() != null) {
                int savedPreset = player.getPreset().intValue();

                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == savedPreset) {
                        presetCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first preset if none selected
            if (!selected && presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);
            }

            initializing = false;
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
            initializing = false;
        }
    }

    /**
     * Populate the drum combo box with drum sounds
     */
    private void populateDrumCombo() {
        try {
            initializing = true;

            // Clear existing items
            drumCombo.removeAllItems();

            // Add standard GM drum sounds (35-81)
            for (int note = 35; note <= 81; note++) {
                String drumName = InternalSynthManager.getInstance().getDrumName(note);
                drumCombo.addItem(new DrumItem(note, note + ": " + drumName));
            }

            // Select drum sound from player's root note if available
            boolean selected = false;
            if (player.getRootNote() != null) {
                int savedNote = player.getRootNote().intValue();

                for (int i = 0; i < drumCombo.getItemCount(); i++) {
                    DrumItem item = drumCombo.getItemAt(i);
                    if (item.getNoteNumber() == savedNote) {
                        drumCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first drum sound if none selected
            if (!selected && drumCombo.getItemCount() > 0) {
                drumCombo.setSelectedIndex(0);
            }

            initializing = false;
        } catch (Exception e) {
            logger.error("Error populating drum sounds: {}", e.getMessage());
            initializing = false;
        }
    }

    /**
     * Plays a preview note with the current instrument/preset
     */
    private void playPreviewNote() {
        try {
            if (isDrumChannel) {
                // For drum channel, play selected drum sound
                if (drumCombo.getSelectedItem() instanceof DrumItem) {
                    DrumItem item = (DrumItem) drumCombo.getSelectedItem();
                    playDrumPreview(item.getNoteNumber());
                }
                return;
            }

            // For melodic channels, play a C major chord
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null || instrument.getDevice() == null) {
                return;
            }

            // Ensure device is open
            if (!instrument.getDevice().isOpen()) {
                instrument.getDevice().open();
            }

            int channel = player.getChannel();

            // Apply current preset first
            applyPresetChange();

            // Play C major chord (C4, E4, G4)
            instrument.noteOn(channel, 60, 100); // C4
            instrument.noteOn(channel, 64, 100); // E4
            instrument.noteOn(channel, 67, 100); // G4

            // Schedule note off after 500ms
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    instrument.noteOff(channel, 60, 0);
                    instrument.noteOff(channel, 64, 0);
                    instrument.noteOff(channel, 67, 0);
                } catch (Exception e) {
                    // Ignore interruption
                }
            }).start();
        } catch (Exception e) {
            logger.error("Error playing preview: {}", e.getMessage());
        }
    }

    /**
     * Play a drum sound preview
     */
    private void playDrumPreview(int noteNumber) {
        try {
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null || instrument.getDevice() == null) {
                return;
            }

            // Ensure device is open
            if (!instrument.getDevice().isOpen()) {
                instrument.getDevice().open();
            }

            // Always use channel 9 (MIDI drum channel)
            int drumChannel = 9;

            // Apply standard drum kit
            instrument.controlChange(drumChannel, 0, 0);   // Bank MSB
            instrument.controlChange(drumChannel, 32, 0);  // Bank LSB
            instrument.programChange(drumChannel, 0, 0);   // Standard kit

            // Play the drum note
            instrument.noteOn(drumChannel, noteNumber, 100);

            // No need for noteOff with percussion sounds
        } catch (Exception e) {
            logger.error("Error playing drum preview: {}", e.getMessage());
        }
    }

    /**
     * Apply the current preset selection to the instrument
     */
    private void applyPresetChange() {
        // Get required values
        InstrumentWrapper instrument = player.getInstrument();
        if (instrument == null || instrument.getDevice() == null) {
            return;
        }

        try {
            // Get channel and soundbank info
            int channel = player.getChannel();
            String soundbankName = (String) soundbankCombo.getSelectedItem();

            // Store soundbank name in instrument
            if (soundbankName != null) {
                instrument.setSoundbankName(soundbankName);
            }

            // Get bank and preset to apply
            Integer bank = null;
            Integer preset = null;

            // Get bank/preset based on UI state
            if (isDrumChannel) {
                // For drum channel, always use bank 0, program 0
                bank = 0;
                preset = 0;
            } else if (isInternalSynth) {
                // For internal synths, get bank and preset from UI
                if (bankCombo.getSelectedItem() != null) {
                    bank = (Integer) bankCombo.getSelectedItem();
                }

                if (presetCombo.getSelectedItem() instanceof PresetItem) {
                    preset = ((PresetItem) presetCombo.getSelectedItem()).getNumber();
                }
            } else {
                // For external instruments, just use the preset spinner
                bank = 0; // Default to bank 0
                preset = ((Number) presetSpinner.getValue()).intValue();
            }

            // Apply values if we have them
            if (bank != null && preset != null) {
                // Store values in instrument
                instrument.setBankIndex(bank);
                instrument.setCurrentPreset(preset);
                player.setPreset(preset);

                // For synths, use the InstrumentWrapper's method to apply bank/program changes
                instrument.applyBankAndProgram(channel);

                // Log the change
                logger.info("Applied preset change: channel={}, bank={}, preset={}", channel, bank, preset);
            }
        } catch (Exception e) {
            logger.error("Error applying preset change: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank file using InternalSynthManager's direct methods
     */
    private void loadSoundbankFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Soundbank");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Soundbank Files (*.sf2, *.dls)", "sf2", "dls"));

        // Show dialog
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                // Use the InternalSynthManager's loadSoundbank method directly
                // It's already implemented properly in your manager class
                String newSoundbankName = InternalSynthManager.getInstance().loadSoundbank(selectedFile);

                if (newSoundbankName != null) {
                    // Force reinitialization of the UI
                    initializeSoundbanks();

                    // Select the newly loaded soundbank
                    for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                        if (soundbankCombo.getItemAt(i).equals(newSoundbankName)) {
                            soundbankCombo.setSelectedIndex(i);
                            break;
                        }
                    }

                    // Update the instrument's soundbank name
                    if (player != null && player.getInstrument() != null) {
                        player.getInstrument().setSoundbankName(newSoundbankName);
                    }

                    // Report success
                    JOptionPane.showMessageDialog(this,
                            "Soundbank loaded successfully: " + newSoundbankName,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to load soundbank file",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                logger.error("Error loading soundbank: {}", e.getMessage(), e);
                JOptionPane.showMessageDialog(this,
                        "Error loading soundbank: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Apply current UI values to the player
     */
    public void applyToPlayer() {
        // Apply player name
        player.setName(nameField.getText());

        // Apply channel (should already be set via listener, but ensure it's up to date)
        player.setChannel(((Number) channelSpinner.getValue()).intValue());

        // For external instruments, ensure preset is updated from spinner
        if (!isInternalSynth && !isDrumChannel && presetSpinner.isVisible()) {
            player.setPreset((Integer) presetSpinner.getValue());
        }

        // For internal instruments, apply soundbank/bank settings
        if (isInternalSynth && player.getInstrument() != null) {
            if (soundbankCombo.getSelectedItem() != null) {
                player.getInstrument().setSoundbankName((String) soundbankCombo.getSelectedItem());
            }

            if (bankCombo.getSelectedItem() != null) {
                player.getInstrument().setBankIndex((Integer) bankCombo.getSelectedItem());
            }
        }

        // Apply any pending preset changes
        try {
            if (isInternalSynth) {
                applyPresetChange();
            }
        } catch (Exception e) {
            logger.error("Error applying preset change: {}", e.getMessage());
        }

    }

    /**
     * Test the connection to the InternalSynthManager - call this from a debug
     * button if needed
     */
    private void testSoundbankConnection() {
        try {
            logger.info("=== TESTING SOUNDBANK CONNECTION ===");

            // Get current soundbanks
            List<String> names = InternalSynthManager.getInstance().getSoundbankNames();
            logger.info("Found {} soundbanks in manager", names.size());
            for (String name : names) {
                logger.info("  Soundbank: {}", name);
            }

            // Report UI state
            logger.info("UI has {} soundbanks in combo", soundbankCombo.getItemCount());
            for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                logger.info("  UI Soundbank {}: {}", i, soundbankCombo.getItemAt(i));
            }

            // Show currently selected item
            logger.info("Selected soundbank in UI: {}", soundbankCombo.getSelectedItem());

            // Show player instrument state
            if (player != null && player.getInstrument() != null) {
                logger.info("Player instrument soundbank: {}", player.getInstrument().getSoundbankName());
                logger.info("Player instrument bank: {}", player.getInstrument().getBankIndex());
                logger.info("Player instrument preset: {}", player.getInstrument().getCurrentPreset());
            } else {
                logger.info("Player has no instrument or null instrument");
            }
        } catch (Exception e) {
            logger.error("Error testing soundbank connection: {}", e.getMessage(), e);
        }
    }
}

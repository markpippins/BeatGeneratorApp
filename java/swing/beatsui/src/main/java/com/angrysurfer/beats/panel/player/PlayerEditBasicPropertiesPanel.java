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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumItem;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.PresetItem;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.ReceiverManager;

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
    private JComboBox<InstrumentWrapper> instrumentCombo;
    private JSpinner channelSpinner;
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
        registerForInstrumentUpdates();
        registerForDrumEvents();
        
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
    
            // Channel spinner (0-15)
            channelSpinner = new JSpinner(new SpinnerNumberModel(
                    Math.min(Math.max(player.getChannel(), 0), 15), // value (bounded)
                    0, // min
                    15, // max
                    1 // step
            ));
    
            // Preset control panel will contain either the presetSpinner or presetCombo
            presetControlPanel = new JPanel(new BorderLayout());
    
            // Preset spinner for external instruments
            presetSpinner = new JSpinner(new SpinnerNumberModel(
                    player.getPreset() != null ? Math.min(Math.max(player.getPreset(), 0), 127) : 0, // value (bounded)
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
    
            // Add listeners after all components are initialized
            addListeners();
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Set up the instrument combo box with improved handling
     */
    private void setupInstrumentCombo() {
        // Log initial state
        if (player.getInstrument() != null) {
            logger.debug("Player instrument before setup: {} (ID: {})", 
                player.getInstrument().getName(), 
                player.getInstrument().getId());
        } else {
            logger.debug("Player has no instrument before setup");
        }
        
        // Clear any previous selections
        if (instrumentCombo != null) {
            instrumentCombo.removeAllItems();
        } else {
            instrumentCombo = new JComboBox<>();
        }
        
        // Custom renderer to show better debug info
        instrumentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InstrumentWrapper) {
                    InstrumentWrapper instrument = (InstrumentWrapper) value;
                    setText(String.format("%s [ID:%s, Ch:%s]", 
                        instrument.getName(),
                        instrument.getId(),
                        instrument.getChannel()));
                }
                return this;
            }
        });
        
        // Get ALL instruments from InstrumentManager for debugging
        List<InstrumentWrapper> instruments = instrumentManager.getCachedInstruments();
        
        // Sort alphabetically by name
        instruments.sort((a, b) -> {
            String nameA = a.getName() != null ? a.getName() : "";
            String nameB = b.getName() != null ? b.getName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });
        
        // DEBUGGING: Print all instruments
        logger.debug("=== ALL CACHED INSTRUMENTS ===");
        for (InstrumentWrapper instrument : instruments) {
            logger.debug("Instrument: {} (ID: {}, Channel: {}, Internal: {})",
                instrument.getName(),
                instrument.getId(),
                instrument.getChannel(),
                instrument.getInternal());
        }
        
        // Add instruments to combo box
        for (InstrumentWrapper instrument : instruments) {
            instrumentCombo.addItem(instrument);
        }
        
        // Select correct instrument - try multiple approaches
        boolean selected = false;
        
        // Try by ID if we have one
        if (player.getInstrumentId() != null) {
            logger.debug("Attempting to select by ID: {}", player.getInstrumentId());
            
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (item.getId() != null && item.getId().equals(player.getInstrumentId())) {
                    logger.debug("Found match by ID at index {}", i);
                    instrumentCombo.setSelectedIndex(i);
                    selected = true;
                    break;
                }
            }
        }
        
        // If ID didn't work, try by player's instrument reference
        if (!selected && player.getInstrument() != null) {
            logger.debug("Attempting to select by instrument reference");
            
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (item == player.getInstrument()) {
                    logger.debug("Found match by reference at index {}", i);
                    instrumentCombo.setSelectedIndex(i);
                    selected = true;
                    break;
                }
            }
        }
        
        // Last resort: try by name and channel
        if (!selected && player.getInstrument() != null && player.getInstrument().getName() != null) {
            String name = player.getInstrument().getName();
            Integer channel = player.getChannel();
            
            logger.debug("Attempting to select by name ({}) and channel ({})", name, channel);
            
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (name.equals(item.getName()) && 
                    (channel == null || channel.equals(item.getChannel()))) {
                    logger.debug("Found match by name/channel at index {}", i);
                    instrumentCombo.setSelectedIndex(i);
                    selected = true;
                    break;
                }
            }
        }
        
        // If still not found, log the issue
        if (!selected) {
            logger.warn("Could not find matching instrument for player");
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
        // Add listener to instrument combo box
        instrumentCombo.addActionListener(e -> {
            if (initializing.get()) return;
            
            InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
            if (selectedInstrument != null) {
                // Log before change
                logger.debug("Changing player instrument from {} (ID:{}) to {} (ID:{})", 
                    player.getInstrument() != null ? player.getInstrument().getName() : "none",
                    player.getInstrument() != null ? player.getInstrument().getId() : "none",
                    selectedInstrument.getName(), 
                    selectedInstrument.getId());
                
                // Store the selected instrument
                selectedInstrumentId = selectedInstrument.getId();
                
                // Update player with the selected instrument
                player.setInstrument(selectedInstrument);
                player.setInstrumentId(selectedInstrument.getId());
                
                // Check if this is an internal synth
                isInternalSynth = synthManager.isInternalSynth(selectedInstrument);
                
                // Immediately save the player to persist changes
                PlayerManager.getInstance().savePlayerProperties(player);
                
                // Update drum instrument dropdown if this is channel 9
                if (player.getChannel() == 9) {
                    updateDrumInstrumentCombo();
                }
                
                // Update soundbank, bank and preset dropdowns
                updatePresetControls();
                
                logger.debug("Player instrument updated and saved");
            }
        });

        // Channel change listener
        channelSpinner.addChangeListener(e -> {
            if (initializing.get()) return;
            
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
            if (initializing.get()) return;
            
            int preset = ((Number) presetSpinner.getValue()).intValue();
            player.getInstrument().setPreset(preset);
            
            // Immediately apply preset change for external instruments
            if (!isInternalSynth && player.getInstrument() != null) {
                try {
                    applyPresetChange();
                } catch (Exception ex) {
                    logger.error("Error applying preset: {}", ex.getMessage());
                }
            }
        });

        // Soundbank combo listener
        soundbankCombo.addActionListener(e -> {
            if (initializing.get() || soundbankCombo.getSelectedItem() == null) return;
            
            String soundbankName = (String) soundbankCombo.getSelectedItem();
            
            // Update instrument with selected soundbank
            if (player.getInstrument() != null) {
                player.getInstrument().setSoundbankName(soundbankName);
                
                // Apply soundbank change
                Soundbank soundbank = synthManager.getSoundbank(soundbankName);
                if (soundbank != null && player.getInstrument().getDevice() instanceof Synthesizer) {
                    try {
                        Synthesizer synth = (Synthesizer)player.getInstrument().getDevice();
                        synth.loadAllInstruments(soundbank);
                    } catch (Exception ex) {
                        logger.error("Error loading soundbank instruments: {}", ex.getMessage());
                    }
                }
                
                // Update instrument through manager
                instrumentManager.updateInstrument(player.getInstrument());
                
                // Apply preset changes 
                applyPresetChange();
            }

            // Populate banks for this soundbank
            populateBanksCombo();
        });

        // Bank combo listener
        bankCombo.addActionListener(e -> {
            if (initializing.get() || bankCombo.getSelectedItem() == null) return;
            
            Integer bank = (Integer) bankCombo.getSelectedItem();

            // Update instrument with selected bank
            if (player.getInstrument() != null) {
                player.getInstrument().setBankIndex(bank);
                
                // Update instrument through manager
                instrumentManager.updateInstrument(player.getInstrument());
                
                // Apply changes
                applyPresetChange();
            }

            // Populate presets for this bank
            populatePresetComboForBank(bank);
        });

        // Preset combo listener
        presetCombo.addActionListener(e -> {
            if (initializing.get() || !(presetCombo.getSelectedItem() instanceof PresetItem)) return;
            
            PresetItem item = (PresetItem) presetCombo.getSelectedItem();

            // Update player's preset
            player.getInstrument().setPreset(item.getNumber());

            // Update instrument's current preset
            if (player.getInstrument() != null) {
                player.getInstrument().setPreset(item.getNumber());
                
                // Update instrument through manager
                instrumentManager.updateInstrument(player.getInstrument());
                
                // Apply preset change
                applyPresetChange();
            }
        });

        // Drum combo listener
        drumCombo.addActionListener(e -> {
            if (initializing.get() || !(drumCombo.getSelectedItem() instanceof DrumItem)) return;
            
            DrumItem item = (DrumItem) drumCombo.getSelectedItem();

            // For drum store note in rootNote
            player.setRootNote(Integer.valueOf(item.getNoteNumber()));

            // Preview the drum sound
            playDrumPreview(item.getNoteNumber());
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
                    boolean deleted = synthManager.deleteSoundbank(selectedSoundbank);
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
        previewButton.addActionListener(e -> playPreviewNote());
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

        // Channel label and spinner
        trGbc.gridx = 4;
        trGbc.weightx = 0.0;
        topRow.add(new JLabel("Channel:"), trGbc);

        trGbc.gridx = 5;
        trGbc.weightx = 0.1;
        channelSpinner.setPreferredSize(new Dimension(60, channelSpinner.getPreferredSize().height));
        topRow.add(channelSpinner, trGbc);

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
     * Update preset controls based on instrument type and channel with improved error handling
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
     * Update the drum instrument combo when switching to drum channel
     * or when changing instruments on drum channel
     */
    private void updateDrumInstrumentCombo() {
        if (!isDrumChannel) {
            return; // Only apply for drum channel
        }
        
        initializing.set(true);
        try {
            logger.debug("Updating drum instruments for channel 9");

            // Clear existing items
            drumCombo.removeAllItems();

            // Get drum items from InternalSynthManager
            List<DrumItem> drumItems = synthManager.getDrumItems();

            // Add to combo
            for (DrumItem item : drumItems) {
                drumCombo.addItem(item);
            }

            // Select drum sound from player's root note if available
            boolean selected = false;
            if (player.getRootNote() != null) {
                int desiredNote = player.getRootNote();
                
                for (int i = 0; i < drumCombo.getItemCount(); i++) {
                    DrumItem item = drumCombo.getItemAt(i);
                    if (item.getNoteNumber() == desiredNote) {
                        drumCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first drum sound if none selected
            if (!selected && drumCombo.getItemCount() > 0) {
                drumCombo.setSelectedIndex(0);
                
                // Update player's root note with default drum
                DrumItem item = (DrumItem)drumCombo.getSelectedItem();
                if (item != null) {
                    player.setRootNote(item.getNoteNumber());
                    
                    // Apply the drum selection immediately
                    if (player.getInstrument() != null) {
                        // Set standard drum kit
                        player.getInstrument().controlChange(0, 0);   // Bank MSB
                        player.getInstrument().controlChange(32, 0);  // Bank LSB
                        player.getInstrument().programChange(0, 0);  // Standard kit
                    }
                }
            }
            
            // Ensure drum control panel is visible
            updatePresetControls();
        } catch (Exception e) {
            logger.error("Error updating drum instruments: {}", e.getMessage(), e);
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Register for instrument update notifications with improved handling
     */
    private void registerForInstrumentUpdates() {
        commandBus.register(action -> {
            if (action.getCommand() == null) {
                return;
            }

            if (Commands.INSTRUMENT_UPDATED.equals(action.getCommand()) && action.getData() instanceof InstrumentWrapper) {
                InstrumentWrapper updatedInstrument = (InstrumentWrapper)action.getData();
                
                // Only update UI if this is the current instrument
                if (selectedInstrumentId != null && selectedInstrumentId.equals(updatedInstrument.getId())) {
                    SwingUtilities.invokeLater(() -> {
                        initializing.set(true);
                        try {
                            // Update player reference
                            if (player.getInstrumentId() != null && 
                                player.getInstrumentId().equals(updatedInstrument.getId())) {
                                player.setInstrument(updatedInstrument);
                            }
                            
                            // If soundbank selection changed, update related UI
                            if (isInternalSynth && updatedInstrument.getSoundbankName() != null) {
                                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                                    String sbName = (String)soundbankCombo.getItemAt(i);
                                    if (sbName.equals(updatedInstrument.getSoundbankName())) {
                                        soundbankCombo.setSelectedIndex(i);
                                        break;
                                    }
                                }
                                
                                // Update bank and preset selection
                                populateBanksCombo();
                            }
                        } finally {
                            initializing.set(false);
                        }
                    });
                }
            } else if (Commands.INSTRUMENTS_REFRESHED.equals(action.getCommand())) {
                SwingUtilities.invokeLater(this::refreshInstrumentsList);
            }
        });
    }
    
    /**
     * Register for drum events
     */
    private void registerForDrumEvents() {
        commandBus.register(action -> {
            if (Commands.DRUM_PLAYER_INSTRUMENT_CHANGED.equals(action.getCommand()) && 
                    action.getData() instanceof Object[] data) {
                
                // Extract the data
                DrumSequencer sequencer = (DrumSequencer) data[0];
                int playerIndex = (Integer) data[1];
                InstrumentWrapper instrument = (InstrumentWrapper) data[2];
                
                // Get the player that was changed
                Player changedPlayer = sequencer.getPlayer(playerIndex);
                
                // Ask user if they want to apply to all drums
                int response = JOptionPane.showConfirmDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Apply this instrument change to all drum pads in the sequencer?",
                        "Update All Drum Pads",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                
                if (response == JOptionPane.YES_OPTION) {
                    // Apply the same instrument to all drum pads
                    for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                        Player drumPlayer = sequencer.getPlayer(i);
                        if (drumPlayer != null && i != playerIndex) {
                            // Update instrument
                            drumPlayer.setInstrument(instrument);
                            drumPlayer.setInstrumentId(instrument.getId());
                            
                            // Save the player
                            PlayerManager.getInstance().savePlayerProperties(drumPlayer);
                            
                            // Apply the instrument settings
                            PlayerManager.getInstance().applyPlayerInstrument(drumPlayer);
                            
                            // Notify about update
                            commandBus.publish(Commands.PLAYER_UPDATED, this, drumPlayer);
                        }
                    }
                    
                    logger.info("Applied instrument {} to all drum pads in sequencer", 
                            instrument.getName());
                }
            }
        });
    }

    /**
     * Refresh the instruments list with improved handling
     */
    private void refreshInstrumentsList() {
        initializing.set(true);
        try {
            // Remember selection
            InstrumentWrapper currentSelection = (InstrumentWrapper)instrumentCombo.getSelectedItem();
            Long currentId = currentSelection != null ? currentSelection.getId() : null;
            
            // Get fresh instruments list
            List<InstrumentWrapper> instruments = instrumentManager.getCachedInstruments();
            
            // Sort alphabetically
            instruments.sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });
            
            // Update combo
            instrumentCombo.removeAllItems();
            for (InstrumentWrapper instrument : instruments) {
                if (instrument.getAvailable())
                    instrumentCombo.addItem(instrument);
            }
            
            // Restore selection
            if (currentId != null) {
                for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                    InstrumentWrapper item = instrumentCombo.getItemAt(i);
                    if (currentId.equals(item.getId())) {
                        instrumentCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (instrumentCombo.getItemCount() > 0) {
                instrumentCombo.setSelectedIndex(0);
            }
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Initialize soundbanks with improved error handling
     */
    public void initializeSoundbanks() {
        // Set initializing flag to prevent listener cascades
        initializing.set(true);
        
        // Show loading cursor
        Cursor originalCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        try {
            logger.debug("Initializing soundbanks");
            
            // Clear existing items
            soundbankCombo.removeAllItems();

            // Force the InternalSynthManager to initialize its soundbanks
            // Let's do this in a more direct way
            boolean success = synthManager.initializeSoundbanks();
            
            if (!success) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to initialize soundbanks. Check console for errors.",
                    "Soundbank Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get soundbank names
            List<String> names = synthManager.getSoundbankNames();
            
            if (names == null || names.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No soundbanks available. Please check your Java sound configuration.",
                    "No Soundbanks", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            logger.debug("Found {} soundbanks", names.size());
            
            // Filter and add to combo box
            for (String name : names) {
                if (name != null && !name.trim().isEmpty()) {
                    soundbankCombo.addItem(name);
                    logger.debug("Added soundbank: {}", name);
                }
            }

            // Select instrument's existing soundbank if available
            boolean selected = false;
            if (player.getInstrument() != null && player.getInstrument().getSoundbankName() != null) {
                String desiredSoundbank = player.getInstrument().getSoundbankName();
                
                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    if (soundbankCombo.getItemAt(i).equals(desiredSoundbank)) {
                        soundbankCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }

            // Default to first item if none selected
            if (!selected && soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
                
                // Update player's instrument with the default soundbank
                if (player.getInstrument() != null) {
                    player.getInstrument().setSoundbankName((String)soundbankCombo.getSelectedItem());
                    instrumentManager.updateInstrument(player.getInstrument());
                }
            }

            // Populate banks for selected soundbank
            populateBanksCombo();
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this, 
                "Error initializing soundbanks: " + e.getMessage(),
                "Initialization Error", 
                JOptionPane.ERROR_MESSAGE);
        } finally {
            initializing.set(false);
            setCursor(originalCursor);
        }
    }

    /**
     * Populate the bank combo box with improved error handling 
     */
    private void populateBanksCombo() {
        initializing.set(true);
        
        try {
            // Clear existing banks
            bankCombo.removeAllItems();
    
            // Get the selected soundbank
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            if (selectedSoundbank == null) {
                return;
            }
    
            // Get available banks for this soundbank
            List<Integer> banks = synthManager.getAvailableBanksByName(selectedSoundbank);
    
            // Add banks to combo
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }
    
            // Select instrument's bank if available
            boolean selected = false;
            if (player.getInstrument() != null && player.getInstrument().getBankIndex() != null) {
                Integer desiredBank = player.getInstrument().getBankIndex();
                
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    Integer item = bankCombo.getItemAt(i);
                    if (item != null && item.equals(desiredBank)) {
                        bankCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }
    
            // Default to first bank if none selected
            if (!selected && bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);
                
                // Update player's instrument with default bank
                if (player.getInstrument() != null) {
                    player.getInstrument().setBankIndex((Integer)bankCombo.getSelectedItem());
                    instrumentManager.updateInstrument(player.getInstrument());
                }
            }
    
            // Populate presets for selected bank
            Integer bank = (Integer) bankCombo.getSelectedItem();
            if (bank != null) {
                populatePresetComboForBank(bank);
            }
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage());
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Populate the preset combo box with improved error handling
     */
    private void populatePresetComboForBank(int bank) {
        initializing.set(true);
        
        try {
            // Clear existing presets
            presetCombo.removeAllItems();
    
            // Get soundbank name
            String soundbankName = (String) soundbankCombo.getSelectedItem();
            if (soundbankName == null) {
                return;
            }
    
            // Get presets for this soundbank and bank
            List<String> presetNames = synthManager.getPresetNames(soundbankName, bank);
    
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
                int desiredPreset = player.getPreset();
                
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == desiredPreset) {
                        presetCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }
    
            // Default to first preset if none selected
            if (!selected && presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);
                
                // Update player's preset with default preset
                PresetItem item = (PresetItem)presetCombo.getSelectedItem();
                if (item != null) {
                    player.getInstrument().setPreset(item.getNumber());
                    if (player.getInstrument() != null) {
                        player.getInstrument().setPreset(item.getNumber());
                        instrumentManager.updateInstrument(player.getInstrument());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Populate the drum combo box with improved error handling
     */
    private void populateDrumCombo() {
        initializing.set(true);
        
        try {
            // Clear existing items
            drumCombo.removeAllItems();
    
            // Get drum items from InternalSynthManager
            List<DrumItem> drumItems = synthManager.getDrumItems();
    
            // Add to combo
            for (DrumItem item : drumItems) {
                drumCombo.addItem(item);
            }
    
            // Select drum sound from player's root note if available
            boolean selected = false;
            if (player.getRootNote() != null) {
                int desiredNote = player.getRootNote();
                
                for (int i = 0; i < drumCombo.getItemCount(); i++) {
                    DrumItem item = drumCombo.getItemAt(i);
                    if (item.getNoteNumber() == desiredNote) {
                        drumCombo.setSelectedIndex(i);
                        selected = true;
                        break;
                    }
                }
            }
    
            // Default to first drum sound if none selected
            if (!selected && drumCombo.getItemCount() > 0) {
                drumCombo.setSelectedIndex(0);
                
                // Update player's root note with default drum
                DrumItem item = (DrumItem)drumCombo.getSelectedItem();
                if (item != null) {
                    player.setRootNote(item.getNoteNumber());
                }
            }
        } catch (Exception e) {
            logger.error("Error populating drum combo: {}", e.getMessage());
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Plays a preview note using ReceiverManager for reliable output
     */
    private void playPreviewNote() {
        try {
            if (isDrumChannel) {
                // For drum play selected drum sound
                if (drumCombo.getSelectedItem() instanceof DrumItem) {
                    DrumItem item = (DrumItem) drumCombo.getSelectedItem();
                    playDrumPreview(item.getNoteNumber());
                }
                return;
            }

            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                return;
            }

            // Apply preset changes first
            applyPresetChange();
            
            int channel = player.getChannel();
            try {
                // Play C major chord
                instrument.noteOn(60, 100); // C4
                Thread.sleep(50); // Small delay between notes
                instrument.noteOn(64, 100); // E4
                Thread.sleep(50);
                instrument.noteOn(67, 100); // G4
                
                // Schedule note off after specified duration
                new Thread(() -> {
                    try {
                        Thread.sleep(PREVIEW_DURATION_MS);
                        instrument.noteOff(60, 0);
                        instrument.noteOff(64, 0);
                        instrument.noteOff(67, 0);
                    } catch (Exception e) {
                        // Ignore interruption
                    }
                }).start();
            } catch (Exception e) {
                logger.error("Error playing preview: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error in preview: {}", e.getMessage());
        }
    }

    /**
     * Play a drum sound preview using ReceiverManager
     */
    private void playDrumPreview(int noteNumber) {
        try {
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                return;
            }

            // Always use channel 9 (MIDI drum channel)
            int drumChannel = 9;

            // Apply standard drum kit
            try {
                // Apply bank and program changes for drum channel
                instrument.controlChange(0, 0);  // Bank MSB
                instrument.controlChange(32, 0); // Bank LSB
                instrument.programChange(0, 0);  // Standard kit

                // Play the drum note
                instrument.noteOn(noteNumber, 100);
                // No need for note off with percussion sounds
            } catch (Exception e) {
                logger.error("Error sending MIDI commands for drum preview: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error playing drum preview: {}", e.getMessage());
        }
    }

    /**
     * Apply the current preset using the instrument's methods
     */
    private void applyPresetChange() {
        InstrumentWrapper instrument = player.getInstrument();
        if (instrument == null) {
            return;
        }

        try {
            // Get channel for preset changes
            int channel = player.getChannel();
            
            // Get preset data from appropriate source based on type
            Integer bank = null;
            Integer preset = null;
            
            if (isDrumChannel) {
                // For drum always use bank 0, program 0
                bank = 0;
                preset = 0;
            } else if (isInternalSynth) {
                // For internal synths, get from UI controls
                bank = (Integer) bankCombo.getSelectedItem();
                preset = player.getPreset();
            } else {
                // For external instruments, use spinner
                bank = 0; // Default bank
                preset = (Integer) presetSpinner.getValue();
            }

            // Apply bank and program changes if we have valid values
            if (bank != null && preset != null) {
                // First bank select MSB/LSB
                int bankMSB = (bank >> 7) & 0x7F;
                int bankLSB = bank & 0x7F;
                
                instrument.controlChange(0, bankMSB);
                instrument.controlChange(32, bankLSB);
                instrument.programChange(preset, 0);
                
                // Update instrument object
                instrument.setBankIndex(bank);
                instrument.setPreset(preset);
                player.getInstrument().setPreset(preset);
                
                // Update in InstrumentManager to persist change
                instrumentManager.updateInstrument(instrument);
                
                logger.debug("Applied preset change: channel={}, bank={}, preset={}",
                        bank, preset);
            }
        } catch (Exception e) {
            logger.error("Error applying preset change: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank file with improved error handling
     */
    private void loadSoundbankFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Soundbank");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Soundbank Files (*.sf2, *.dls)", "sf2", "dls"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Show loading indicator
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            
            try {
                // Load soundbank in background thread to prevent UI freeze
                new Thread(() -> {
                    try {
                        String newSoundbankName = synthManager.loadSoundbank(selectedFile);
                        
                        // Update UI on EDT
                        SwingUtilities.invokeLater(() -> {
                            try {
                                if (newSoundbankName != null) {
                                    // Reinitialize UI with new soundbank
                                    initializeSoundbanks();
                                    
                                    // Select new soundbank
                                    for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                                        if (Objects.equals(soundbankCombo.getItemAt(i), newSoundbankName)) {
                                            soundbankCombo.setSelectedIndex(i);
                                            break;
                                        }
                                    }
                                    
                                    // Update instrument if we have one
                                    if (player.getInstrument() != null) {
                                        player.getInstrument().setSoundbankName(newSoundbankName);
                                        instrumentManager.updateInstrument(player.getInstrument());
                                    }
                                    
                                    // Report success
                                    JOptionPane.showMessageDialog(this,
                                            "Soundbank loaded successfully: " + newSoundbankName,
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(this,
                                            "Failed to load soundbank",
                                            "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } finally {
                                // Restore cursor
                                setCursor(java.awt.Cursor.getDefaultCursor());
                            }
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            logger.error("Error loading soundbank: {}", e.getMessage());
                            JOptionPane.showMessageDialog(this,
                                    "Error loading soundbank: " + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            setCursor(java.awt.Cursor.getDefaultCursor());
                        });
                    }
                }).start();
                
            } catch (Exception e) {
                logger.error("Error initiating soundbank load: {}", e.getMessage());
                setCursor(java.awt.Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this,
                        "Error loading soundbank: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Commit all UI changes to the player object
     */
    public void applyToPlayer() {
        // Apply name
        player.setName(nameField.getText());
        
        // Ensure channel is within range
        player.setChannel(Math.min(Math.max(0, ((Number) channelSpinner.getValue()).intValue()), 15));
        
        // Apply preset based on UI state
        if (!isInternalSynth && !isDrumChannel && presetSpinner.isVisible()) {
            player.getInstrument().setPreset((Integer) presetSpinner.getValue());
        } else if (isDrumChannel && drumCombo.getSelectedItem() instanceof DrumItem) {
            DrumItem item = (DrumItem) drumCombo.getSelectedItem();
            player.setRootNote(item.getNoteNumber());
        }
        
        // Apply instrument settings if available
        if (player.getInstrument() != null) {
            // Apply soundbank settings to instrument
            if (isInternalSynth && soundbankCombo.getSelectedItem() != null) {
                player.getInstrument().setSoundbankName((String) soundbankCombo.getSelectedItem());
            }
            
            // Apply bank if available
            if (isInternalSynth && bankCombo.getSelectedItem() != null) {
                player.getInstrument().setBankIndex((Integer) bankCombo.getSelectedItem());
            }
            
            // Apply preset from combo if available
            if (isInternalSynth && presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                player.getInstrument().setPreset(item.getNumber());
                player.getInstrument().setPreset(item.getNumber());
            }
            
            // Update instrument through manager to persist changes
            instrumentManager.updateInstrument(player.getInstrument());
        }
    }

    /**
     * Apply changes from UI controls to the player object
     */
    public void applyChanges() {
        if (initializing.get()) return;
        
        try {
            // 1. Update player name from field
            player.setName(nameField.getText().trim());
            
            // 2. Update instrument selection
            InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
            if (selectedInstrument != null) {
                // Set both references to ensure consistency
                player.setInstrument(selectedInstrument);
                player.setInstrumentId(selectedInstrument.getId());
                
                logger.debug("Updated instrument: {} (ID: {})", 
                    selectedInstrument.getName(), selectedInstrument.getId());
            }
            
            // 3. Update channel
            int channel = ((Number) channelSpinner.getValue()).intValue();
            player.setChannel(channel);
            
            // 4. Update soundbank/bank/preset depending on context
            applyInstrumentSettings();
            
            // 5. Force immediate save to persistent storage
            PlayerManager.getInstance().savePlayerProperties(player);
            
            // 6. Notify the system
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, player);
            if (player.getInstrument() != null) {
                CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
            }
            
            logger.info("Applied all changes to player {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying changes: {}", e.getMessage(), e);
        }
    }

    /**
     * Apply all changes to player and ensure they propagate
     */
    /**
 * Apply all changes to player and ensure they propagate
 */
public void applyAllChanges() {
    // Apply UI values to player
    if (nameField != null && player != null) {
        player.setName(nameField.getText());
    }
    
    // Apply instrument selection
    InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
    // Apply channel setting
    if (channelSpinner != null) {
        player.setChannel(((Number) channelSpinner.getValue()).intValue());
    }

    if (selectedInstrument != null) {
        player.setInstrument(selectedInstrument);
        player.setInstrumentId(selectedInstrument.getId());
        player.getInstrument().setChannel(player.getChannel());
    }
    
    
    // Apply soundbank, bank and preset based on instrument type
    if (player.getInstrument() != null) {
        // Apply soundbank setting for internal instruments
        if (isInternalSynth && soundbankCombo != null && soundbankCombo.getSelectedItem() != null) {
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            player.getInstrument().setSoundbankName(selectedSoundbank);
        }
        
        // Apply bank setting for internal instruments
        if (isInternalSynth && bankCombo != null && bankCombo.getSelectedItem() != null) {
            Integer selectedBank = (Integer) bankCombo.getSelectedItem();
            player.getInstrument().setBankIndex(selectedBank);
        }
        
        // Apply preset setting based on instrument type
        if (isDrumChannel && drumCombo != null && drumCombo.getSelectedItem() instanceof DrumItem) {
            // For drum channel, store drum note in rootNote
            DrumItem selectedDrum = (DrumItem) drumCombo.getSelectedItem();
            player.setRootNote(selectedDrum.getNoteNumber());
        } else if (isInternalSynth && presetCombo != null && 
                  presetCombo.getSelectedItem() instanceof PresetItem) {
            // For internal synth instruments
            PresetItem selectedPreset = (PresetItem) presetCombo.getSelectedItem();
            player.getInstrument().setPreset(selectedPreset.getNumber());
            player.getInstrument().setPreset(selectedPreset.getNumber());
        } else if (!isInternalSynth && presetSpinner != null) {
            // For external instruments
            player.getInstrument().setPreset((Integer) presetSpinner.getValue());
        }
    }
    
    // CRITICAL: Save to PlayerManager to ensure changes persist
    PlayerManager.getInstance().savePlayerProperties(player);
    
    // Force sequencer update if player is owned by one
    if (player.getOwner() instanceof MelodicSequencer sequencer) {
        PlayerManager.getInstance().initializeInstrument(player);
        PlayerManager.getInstance().applyInstrumentPreset(player);
    }
    
    // Apply changes to MIDI device if available
    if (player.getInstrument() != null) {
        try {
            // Apply bank and program changes
            if (isInternalSynth) {
                Integer bank = player.getInstrument().getBankIndex();
                Integer preset = player.getPreset();
                
                if (bank != null && preset != null) {
                    int bankMSB = (bank >> 7) & 0x7F;
                    int bankLSB = bank & 0x7F;
                    player.getInstrument().controlChange(0, bankMSB);
                    player.getInstrument().controlChange(32, bankLSB);
                    player.getInstrument().programChange(preset, 0);
                }
            } else {
                // For external instruments, just send program change
                Integer preset = player.getPreset();
                if (preset != null) {
                    player.getInstrument().programChange(preset, 0);
                }
            }
        } catch (Exception e) {
            logger.error("Error applying MIDI changes: {}", e.getMessage());
        }
    }
    
    // Publish events to update other components
    CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, player);
    if (player.getInstrument() != null) {
        CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
    }
    
    logger.info("Applied and propagated changes to player: {}", player.getName());
}

    /**
     * Load instrument data for the player
     */
    private void loadInstrumentData() {
        if (player == null) {
            return;
        }
        
        // Clear current selections
        instrumentCombo.removeAllItems();
        
        // Get all available instruments
        List<InstrumentWrapper> instruments = InstrumentManager.getInstance().getCachedInstruments();
        
        // Sort alphabetically by name
        instruments.sort((a, b) -> {
            String nameA = a.getName() != null ? a.getName() : "";
            String nameB = b.getName() != null ? b.getName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });
        
        // Add all instruments to combo box
        for (InstrumentWrapper instrument : instruments) {
            if (instrument.getAvailable())
                instrumentCombo.addItem(instrument);
        }
        
        // Select the current instrument if it exists
        if (player.getInstrument() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                // Match by ID for precise selection
                if (item.getId() != null && item.getId().equals(player.getInstrument().getId())) {
                    instrumentCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Handle instrument selection change
     */
    private void handleInstrumentChange(InstrumentWrapper instrument) {
        if (initializing.get() || player == null || instrument == null) return;
        
        try {
            // Update player with the selected instrument
            player.setInstrument(instrument);
            player.setInstrumentId(instrument.getId());
            
            // Apply the instrument preset immediately if player has a sequencer
            if (player.getOwner() instanceof MelodicSequencer sequencer) {
                PlayerManager.getInstance().applyInstrumentPreset(player);
            }
            
            // Save player to persist changes
            PlayerManager.getInstance().savePlayerProperties(player);
            
            logger.debug("Applied instrument {} for player {}", instrument.getName(), player.getName());
            
            // Update other UI components
            updatePresetControls();
        } catch (Exception e) {
            logger.error("Error changing instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Apply all current instrument settings immediately
     */
    private void applyInstrumentSettings() {
        if (player == null || player.getInstrument() == null || !player.getInstrument().getInternal()) {
            return;
        }
        
        try {
            // Get current settings from UI components
            String soundbank = getCurrentSoundbankSelection();
            int bankIndex = getCurrentBankIndex();
            int presetIndex = getPresetIndex();
            int channel = player.getChannel() != null ? player.getChannel() : 0;
            
            // Apply settings to the instrument
            InternalSynthManager.getInstance().applySoundbank(player.getInstrument(), soundbank);
            
            // Apply bank and program changes
            int bankMSB = (bankIndex >> 7) & 0x7F;
            int bankLSB = bankIndex & 0x7F;
            player.getInstrument().controlChange(0, bankMSB);
            player.getInstrument().controlChange(32, bankLSB);
            player.getInstrument().programChange(presetIndex, 0);
            
            logger.info("Applied instrument settings: soundbank={}, bank={}, preset={}, channel={}",
                soundbank, bankIndex, presetIndex, channel);
        } catch (Exception e) {
            logger.error("Error applying instrument settings: {}", e.getMessage());
        }
    }

    // Helper methods to get current selections
    private String getCurrentSoundbankSelection() {
        Object selectedItem = soundbankCombo.getSelectedItem();
        return selectedItem != null ? selectedItem.toString() : null;
    }

    private int getCurrentBankIndex() {
        String bankItem = (String) bankCombo.getSelectedItem();
        if (bankItem != null && bankItem.startsWith("Bank ")) {
            try {
                return Integer.parseInt(bankItem.substring(5));
            } catch (NumberFormatException e) {
                logger.warn("Invalid bank format: {}", bankItem);
            }
        }
        return 0;
    }

    private int getPresetIndex() {
        String presetItem = (String) presetCombo.getSelectedItem();
        if (presetItem != null && presetItem.contains(":")) {
            try {
                return Integer.parseInt(presetItem.substring(0, presetItem.indexOf(":")));
            } catch (NumberFormatException e) {
                logger.warn("Invalid preset format: {}", presetItem);
            }
        }
        return 0;
    }

    /**
     * Update this panel from the player object (when player changes externally)
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }
        
        boolean wasInitializing = initializing.get();
        initializing.set(true);
        try {
            // Update player reference
            this.player = newPlayer;
            
            // Update name field
            if (nameField != null && player.getName() != null) {
                nameField.setText(player.getName());
            }
            
            // Update channel spinner
            if (channelSpinner != null && player.getChannel() != null) {
                channelSpinner.setValue(player.getChannel());
            }
            
            // Update instrument-related UI components
            if (player.getInstrument() != null) {
                // Update soundbank combo if it exists
                if (soundbankCombo != null) {
                    // Fix type conversion - use the string name, not the index
                    soundbankCombo.setSelectedItem(player.getInstrument().getSoundbankName());
                }
                
                // Update bank combo if it exists
                if (bankCombo != null) {
                    // Fix type conversion - directly check the Integer value, not string format
                    Integer bankIndex = player.getInstrument().getBankIndex();
                    if (bankIndex != null) {
                        for (int i = 0; i < bankCombo.getItemCount(); i++) {
                            Integer item = bankCombo.getItemAt(i);
                            if (item != null && item.equals(bankIndex)) {
                                bankCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
                
                // Update preset combo if it exists
                if (presetCombo != null) {
                    // Fix type conversion - find the preset item by preset number
                    int currentPreset = player.getInstrument().getPreset();
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        // Get the actual object from the combo box, which might be a PresetItem
                        Object item = presetCombo.getItemAt(i);
                        
                        // If it's a string formatted like "0: Piano", parse the number
                        if (item instanceof String) {
                            String preset = (String) item;
                            if (preset.startsWith(currentPreset + ":")) {
                                presetCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                        // If it's a PresetItem object, check its preset number
                        else if (item instanceof PresetItem) {
                            PresetItem presetItem = (PresetItem) item;
                            if (presetItem.getNumber() == currentPreset) { // Changed from getPresetNumber() to getNumber()
                                presetCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            }
            
            logger.debug("PlayerEditBasicPropertiesPanel updated from player {}", player.getName());
        } finally {
            initializing.set(wasInitializing);
        }
    }
}

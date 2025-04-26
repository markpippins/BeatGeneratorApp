package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * A reusable panel for selecting soundbanks, banks, and presets for a player.
 * This panel works directly with a Player's InstrumentWrapper to configure
 * sounds.
 */
public class SoundParametersPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // The player whose instrument we're configuring
    private Player player;

    // UI Components
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JComboBox<PresetItem> presetCombo;

    // Track the currently selected soundbank name
    private String currentSoundbankName = null;

    /**
     * Create a new SoundPanel for the specified player
     *
     * @param player The player whose instrument will be configured
     */
    public SoundParametersPanel(Player player) {
        super(new BorderLayout(5, 5));
        this.player = player;

        if (player.getInstrument() == null)
            logger.warn("Player or instrument is null - creating panel in disconnected mode");

        setBorder(BorderFactory.createTitledBorder("Sounds"));
        setupUI();
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Create a single row panel for all controls
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));

        // 1. Soundbank selector section
        JPanel soundbankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        soundbankSection.add(new JLabel("Soundbank:"));
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPrototypeDisplayValue("SoundFont 2.0 (Default)XXXXXX");
        soundbankSection.add(soundbankCombo);

        // Load soundbank button
        JButton loadSoundbankBtn = new JButton("Load...");
        loadSoundbankBtn.addActionListener(e -> loadSoundbankFile());
        soundbankSection.add(loadSoundbankBtn);

        // 2. Bank selector section
        JPanel bankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bankSection.add(new JLabel("Bank:"));  // Fixed the label (was "Soundbank:")
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(60, 25));
        bankSection.add(bankCombo);

        // 3. Preset selector section
        JPanel presetSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetSection.add(new JLabel("Preset:"));
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(250, 25));
        presetSection.add(presetCombo);

        // Add all sections to the row
        controlsRow.add(soundbankSection);
        controlsRow.add(bankSection);
        controlsRow.add(presetSection);

        // Add the controls row to the main panel
        add(controlsRow, BorderLayout.NORTH);

        // Initialize from player's instrument settings
        initializeFromInstrument();

        // Add listeners
        setupListeners();
    }

    /**
     * Add action listeners to UI components
     */
    private void setupListeners() {
        soundbankCombo.addActionListener(e -> {
            if (soundbankCombo.getSelectedItem() != null) {
                String selectedName = (String) soundbankCombo.getSelectedItem();
                selectSoundbank(selectedName);
            }
        });

        bankCombo.addActionListener(e -> {
            if (bankCombo.getSelectedIndex() >= 0) {
                Integer bank = (Integer) bankCombo.getSelectedItem();
                if (bank != null) {
                    populatePresetComboForBank(bank);
                }
            }
        });

        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem item) {
                int bank = bankCombo.getSelectedIndex() >= 0
                        ? (Integer) bankCombo.getSelectedItem() : 0;

                // Apply preset change to the instrument
                applyProgramChange(bank, item.getNumber());
                logger.info("Selected preset: {} (Bank {}, Program {})",
                        item.getName(), bank, item.getNumber());
            }
        });
    }

    /**
     * Initialize UI from player's instrument settings
     */
    private void initializeFromInstrument() {
        try {
            // Initialize soundbanks first
            initializeSoundbanks();

            // If we have a valid instrument with soundbank, bank, and preset settings
            if (player.getInstrument() != null) {
                // Get current instrument settings
                String soundbankName = player.getInstrument().getSoundbankName();
                Integer bankIndex = player.getInstrument().getBankIndex();
                Integer preset = player.getInstrument().getCurrentPreset();

                logger.info("Initializing from instrument: soundbank='{}', bank={}, preset={}",
                        soundbankName, bankIndex, preset);

                // Set up soundbank combo
                if (soundbankName != null && !soundbankName.isEmpty()) {
                    for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                        if (soundbankName.equals(soundbankCombo.getItemAt(i))) {
                            soundbankCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }

                // If we have a soundbank selected, set up banks and presets
                if (soundbankCombo.getSelectedItem() != null) {
                    currentSoundbankName = (String) soundbankCombo.getSelectedItem();
                    populateBanksCombo(currentSoundbankName);

                    // Set the bank index if it exists
                    if (bankIndex != null) {
                        for (int i = 0; i < bankCombo.getItemCount(); i++) {
                            if (bankIndex.equals(bankCombo.getItemAt(i))) {
                                bankCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }

                    // Populate presets for the selected bank
                    if (bankCombo.getSelectedItem() != null) {
                        Integer bank = (Integer) bankCombo.getSelectedItem();
                        populatePresetComboForBank(bank);

                        // Select the current preset if it exists
                        if (preset != null) {
                            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                                PresetItem item = (PresetItem) presetCombo.getItemAt(i);
                                if (preset == item.getNumber()) {
                                    presetCombo.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing from instrument: {}", e.getMessage());
        }
    }

    /**
     * Initialize available soundbanks
     */
    private void initializeSoundbanks() {
        try {
            // Use the manager instance to initialize soundbanks
            InternalSynthManager manager = InternalSynthManager.getInstance();
            manager.initializeSoundbanks();

            // Update UI with data from the manager
            soundbankCombo.removeAllItems();
            List<String> names = manager.getSoundbankNames();
            for (String name : names) {
                soundbankCombo.addItem(name);
            }

            // Temporarily remove the action listener to prevent double-triggering
            ActionListener[] listeners = soundbankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                soundbankCombo.removeActionListener(listener);
            }

            // Select first soundbank if nothing is already selected
            if (soundbankCombo.getSelectedItem() == null && soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
                String selectedName = (String) soundbankCombo.getSelectedItem();
                currentSoundbankName = selectedName;
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                soundbankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage());
        }
    }

    /**
     * Select a soundbank and update the instrument
     */
    private void selectSoundbank(String soundbankName) {
        if (soundbankName == null || soundbankName.isEmpty()) {
            return;
        }

        try {
            // Store the selected soundbank name
            currentSoundbankName = soundbankName;

            // Update the instrument
            if (player.getInstrument() != null) {
                player.getInstrument().setSoundbankName(soundbankName);
                logger.info("Updated instrument soundbank to: {}", soundbankName);
            }

            // Populate banks for this soundbank
            populateBanksCombo(soundbankName);
        } catch (Exception e) {
            logger.error("Error selecting soundbank: {}", e.getMessage());
        }
    }

    /**
     * Populate the bank combo box with available banks for a specific soundbank
     */
    private void populateBanksCombo(String soundbankName) {
        try {
            bankCombo.removeAllItems();

            // Get banks by soundbank name
            List<Integer> banks = InternalSynthManager.getInstance()
                    .getAvailableBanksByName(soundbankName);

            // Add banks to combo box
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }

            // Temporarily remove the action listener
            ActionListener[] listeners = bankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                bankCombo.removeActionListener(listener);
            }

            // Select the first bank if nothing is already selected
            if (bankCombo.getSelectedItem() == null && bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);

                // Populate presets for this bank
                Integer bank = (Integer) bankCombo.getSelectedItem();
                if (bank != null) {
                    populatePresetComboForBank(bank);
                }
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                bankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage());
        }
    }

    /**
     * Populate the preset combo box with presets from the selected bank
     */
    private void populatePresetComboForBank(int bank) {
        try {
            presetCombo.removeAllItems();

            // Get preset names from InternalSynthManager for the current soundbank
            InternalSynthManager manager = InternalSynthManager.getInstance();
            List<String> presetNames = manager.getPresetNames(currentSoundbankName, bank);

            logger.debug("Retrieved {} preset names for soundbank '{}', bank {}",
                    presetNames.size(), currentSoundbankName, bank);

            // Add all presets to the combo box with format: "0: Acoustic Grand Piano"
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String presetName = presetNames.get(i);

                // Use generic name if the specific name is empty
                if (presetName == null || presetName.isEmpty()) {
                    presetName = "Program " + i;
                }

                // Add the preset to the combo box
                presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
            }

            // Temporarily remove the action listener
            ActionListener[] listeners = presetCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                presetCombo.removeActionListener(listener);
            }

            // Select the first preset if nothing is already selected
            if (presetCombo.getSelectedItem() == null && presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);

                // Apply the program change
                if (presetCombo.getSelectedItem() instanceof PresetItem item) {
                    applyProgramChange(bank, item.getNumber());
                }
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                presetCombo.addActionListener(listener);
            }

            // Update UI after bank change
            if (player.getInstrument() != null) {
                player.getInstrument().setBankIndex(bank);
            }
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
        }
    }

    /**
     * Apply program change to the instrument using appropriate methods based on instrument type
     */
    private void applyProgramChange(int bank, int program) {
        if (player.getInstrument() != null && player != null) {
            try {
                // Update instrument's current preset and bank properties
                player.getInstrument().setCurrentPreset(program);
                player.getInstrument().setBankIndex(bank);
                
                // Get the currently selected soundbank
                if (currentSoundbankName != null) {
                    player.getInstrument().setSoundbankName(currentSoundbankName);
                }
                
                logger.info("Applying program change: bank={}, program={}, soundbank={}",
                        bank, program, currentSoundbankName);
                
                // Safely check if this is an internal synth
                boolean isInternalSynth = Boolean.TRUE.equals(player.getInstrument().getInternal());
                
                // Get channel safely
                Integer channelObj = player.getChannel();
                if (channelObj == null) {
                    logger.warn("Player has null channel, defaulting to channel 0");
                    channelObj = 0;
                }
                final int channel = channelObj;
                
                if (isInternalSynth) {
                    // For internal synths, use InternalSynthManager for better handling
                    InternalSynthManager synthManager = InternalSynthManager.getInstance();
                    if (synthManager != null) {
                        // Use the manager's specialized method for preset changes
                        synthManager.applyPresetChange(player.getInstrument(), channel, program);
                        logger.info("Applied internal synth preset via InternalSynthManager");
                        
                        // Send a notification that a preset was applied
                        CommandBus commandBus = CommandBus.getInstance();
                        if (commandBus != null) {
                            commandBus.publish(Commands.PRESET_CHANGED, this, player.getInstrument());
                        }
                    } else {
                        logger.error("InternalSynthManager instance is null");
                    }
                } else {
                    // For hardware/external devices, use direct MIDI messages
                    if (player.getInstrument().getDevice() != null) {
                        // Send bank select MSB/LSB if bank is not zero
                        if (bank > 0) {
                            // Bank MSB (CC#0)
                            player.getInstrument().controlChange(channel, 0, 0);
                            
                            // Bank LSB (CC#32)
                            player.getInstrument().controlChange(channel, 32, bank);
                            
                            logger.debug("Sent bank select: MSB=0, LSB={}", bank);
                        }
                        
                        // Send program change
                        player.getInstrument().programChange(channel, program, 0);
                        logger.info("Applied external device program change via direct MIDI");
                    } else {
                        logger.warn("Cannot send MIDI messages - instrument device is null");
                    }
                }
                
                // Update the instrument in the InstrumentManager to persist changes
                InstrumentManager manager = InstrumentManager.getInstance();
                if (manager != null && player.getInstrument().getId() != null) {
                    manager.updateInstrument(player.getInstrument());
                    logger.info("Successfully updated instrument in InstrumentManager");
                } else {
                    logger.warn("Cannot update instrument: manager={}, instrumentId={}", 
                               manager, player.getInstrument().getId());
                }
                
                logger.info("Successfully applied bank={}, program={} to channel={}",
                        bank, program, channel);
                        
            } catch (Exception e) {
                logger.error("Error changing program: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot apply program change - instrument={}, player={}", player.getInstrument(), player);
        }
    }

    /**
     * Load a soundbank file from disk
     */
    private void loadSoundbankFile() {
        try {
            // Use file chooser dialog
            File soundbankFile = showSoundbankFileChooser();

            if (soundbankFile != null && soundbankFile.exists()) {
                logger.info("Loading soundbank file: {}", soundbankFile.getAbsolutePath());

                // Use the manager to load the soundbank
                String loadedName = InternalSynthManager.getInstance().loadSoundbank(soundbankFile);

                if (loadedName != null) {
                    // Refresh the soundbanks in the combo box
                    List<String> names = InternalSynthManager.getInstance().getSoundbankNames();

                    // Store current selection
                    String currentSelection = (String) soundbankCombo.getSelectedItem();

                    // Update combo box items
                    soundbankCombo.removeAllItems();
                    for (String name : names) {
                        soundbankCombo.addItem(name);
                    }

                    // Try to restore previous selection, otherwise select the new soundbank
                    boolean found = false;
                    if (currentSelection != null) {
                        for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                            if (currentSelection.equals(soundbankCombo.getItemAt(i))) {
                                soundbankCombo.setSelectedIndex(i);
                                found = true;
                                break;
                            }
                        }
                    }

                    // If previous selection not found, select the newly added soundbank
                    if (!found) {
                        for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                            if (loadedName.equals(soundbankCombo.getItemAt(i))) {
                                soundbankCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
        }
    }

    /**
     * Show a file chooser dialog for selecting soundbank files
     */
    private File showSoundbankFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Soundbank File");

        // Add filters for soundbank formats
        FileNameExtensionFilter sfFilter = new FileNameExtensionFilter(
                "SoundFont Files (*.sf2, *.dls)", "sf2", "dls");
        fileChooser.addChoosableFileFilter(sfFilter);
        fileChooser.setFileFilter(sfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        // Show open dialog
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    /**
     * Set the player this panel configures
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Inner class to represent preset items in the combo box
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
            return name;
        }
    }

    /**
     * Get the currently selected soundbank name
     */
    public String getCurrentSoundbankName() {
        return currentSoundbankName;
    }

    /**
     * Get the currently selected bank
     */
    public Integer getCurrentBank() {
        return bankCombo.getSelectedItem() != null
                ? (Integer) bankCombo.getSelectedItem() : null;
    }

    /**
     * Get the currently selected preset
     */
    public Integer getCurrentPreset() {
        if (presetCombo.getSelectedItem() instanceof PresetItem item) {
            return item.getNumber();
        }
        return null;
    }
}

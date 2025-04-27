package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.angrysurfer.core.model.InstrumentPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;

public class SoundParametersPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // The player whose instrument we're configuring
    private Player player;

    private boolean initializing = false;

    // UI components
    private JComboBox<String> soundbankCombo;
    private JComboBox<String> bankCombo;
    private JComboBox<String> presetCombo;

    /**
     * Constructor for SoundParametersPanel
     * 
     * @param player The player to configure sounds for (can be null)
     */
    public SoundParametersPanel(Player player) {
        this.player = player;
        
        // Set layout
        setLayout(new BorderLayout(3, 3));
        setBorder(BorderFactory.createTitledBorder("Sound Parameters"));
        
        // Create UI components
        initialize();
        
        // Update UI based on player state
        updateUI();
    }
    
    private void initialize() {
        // Create main panel for controls with padding
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        
        // Create soundbank selector
        JPanel soundbankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPreferredSize(new Dimension(UIUtils.LARGE_CONTROL_WIDTH  * 3, 25));
        soundbankCombo.addActionListener(e -> {
            if (soundbankCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                String soundbank = soundbankCombo.getSelectedItem().toString();
                player.getInstrument().setSoundbankName(soundbank);
                
                // Apply the soundbank change immediately
                if (player.getInstrument().getInternal()) {
                    InternalSynthManager.getInstance().applySoundbank(player.getInstrument(), soundbank);
                }
                
                // Save changes and notify the system
                PlayerManager.getInstance().savePlayerProperties(player);
                CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
                
                // THEN update banks and presets UI
                updateBanksAndPresets();
            }
        });
        soundbankPanel.add(soundbankCombo);
        
        // Create bank selector
        JPanel bankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(UIUtils.LARGE_CONTROL_WIDTH, 25));
        bankCombo.addActionListener(e -> {
            if (bankCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                // Extract bank number from selection
                String bankItem = (String) bankCombo.getSelectedItem();
                int bankIndex = 0;
                
                if (bankItem != null && bankItem.startsWith("Bank ")) {
                    try {
                        bankIndex = Integer.parseInt(bankItem.substring(5));
                        
                        // Set bank index on instrument
                        player.getInstrument().setBankIndex(bankIndex);
                        
                        // Apply bank selection immediately
                        if (player.getInstrument().getInternal()) {
                            int channel = player.getChannel() != null ? player.getChannel() : 0;
                            int bankMSB = (bankIndex >> 7) & 0x7F;
                            int bankLSB = bankIndex & 0x7F;
                            
                            player.getInstrument().controlChange(channel, 0, bankMSB);
                            player.getInstrument().controlChange(channel, 32, bankLSB);
                        }
                        
                        // Save changes and notify the system
                        PlayerManager.getInstance().savePlayerProperties(player);
                        CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
                        
                        // THEN update presets UI
                        updatePresets();
                    } catch (NumberFormatException ex) {
                        logger.warn("Invalid bank format: {}", bankItem);
                    } catch (InvalidMidiDataException ex) {
                        throw new RuntimeException(ex);
                    } catch (MidiUnavailableException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        bankPanel.add(bankCombo);
        
        // Create preset selector
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(150, 25));
        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                String presetItem = (String) presetCombo.getSelectedItem();
                try {
                    // Extract preset number from selection (format: "0: Preset Name")
                    if (presetItem != null && presetItem.contains(":")) {
                        int presetIndex = Integer.parseInt(presetItem.substring(0, presetItem.indexOf(":")));
                        
                        // Set preset on both instrument and player
                        player.getInstrument().setCurrentPreset(presetIndex);
                        player.setPreset(presetIndex);
                        
                        // Apply preset change immediately
                        if (player.getInstrument().getInternal()) {
                            int channel = player.getChannel() != null ? player.getChannel() : 0;
                            player.getInstrument().programChange(channel, presetIndex, 0);
                        }
                        
                        // Save changes and notify the system
                        PlayerManager.getInstance().savePlayerProperties(player);
                        CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
                        CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, player);
                    }
                } catch (Exception ex) {
                    logger.warn("Error parsing preset: {}", ex.getMessage());
                }
            }
        });
        presetPanel.add(presetCombo);
        
        // Create edit button
        JButton editButton = new JButton(Symbols.getSymbol(Symbols.MIDI));
        editButton.setToolTipText("Edit player settings");
        editButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, 25));
        editButton.addActionListener(e -> {
            if (player != null) {
                // Apply any pending changes first
                applySettings();
                
                // Send command to DialogManager to open player editor
                CommandBus.getInstance().publish(
                    Commands.PLAYER_EDIT_REQUEST, 
                    this, 
                    player
                );
            }
        });
        
        // Add components to main panel
        controlsPanel.add(soundbankPanel);
        controlsPanel.add(bankPanel);
        controlsPanel.add(presetPanel);
        controlsPanel.add(editButton);
        
        // Add to main layout
        add(controlsPanel, BorderLayout.CENTER);
    }
    
    /**
     * Set the player to configure
     * 
     * @param player The player to configure (can be null)
     */
    public void setPlayer(Player player) {
        this.player = player;
        updateUI();
    }
    
    /**
     * Update the UI to reflect the current player's instrument state
     */
    @Override
    public void updateUI() {
        super.updateUI(); // Always call super.updateUI() first
        
        // Check if UI components are created yet
        if (soundbankCombo == null) return;
        
        // Clear all combo boxes first
        soundbankCombo.removeAllItems();
        bankCombo.removeAllItems();
        presetCombo.removeAllItems();
        
        // If player is null or has no instrument, show unavailable state
        if (player == null || player.getInstrument() == null) {
            soundbankCombo.addItem("No instrument available");
            bankCombo.addItem("--");
            presetCombo.addItem("--");
            
            // Disable all controls
            soundbankCombo.setEnabled(false);
            bankCombo.setEnabled(false);
            presetCombo.setEnabled(false);
            return;
        }
        
        // Player has an instrument, enable controls and populate data
        soundbankCombo.setEnabled(true);
        bankCombo.setEnabled(true);
        presetCombo.setEnabled(true);
        
        // Populate soundbank combo
        List<String> soundbanks = InternalSynthManager.getInstance().getSoundbankNames();
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }
        
        // Set current soundbank if available
        String currentSoundbankName = player.getInstrument().getSoundbankName();
        if (currentSoundbankName != null) {
            soundbankCombo.setSelectedItem(currentSoundbankName);
        }
        
        // Update banks and presets based on selected soundbank
        updateBanksAndPresets();
    }
    
    /**
     * Update banks and presets based on selected soundbank
     */
    private void updateBanksAndPresets() {
        if (player == null || player.getInstrument() == null) {
            return;
        }
        
        // Clear existing items
        bankCombo.removeAllItems();
        
        // Get available banks for selected soundbank
        String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
        
        // Use the correct method: getAvailableBanksByName
        List<Integer> banks = InternalSynthManager.getInstance().getAvailableBanksByName(selectedSoundbank);
        
        // Add banks to combo box
        for (Integer bankIndex : banks) {
            bankCombo.addItem("Bank " + bankIndex);
        }
        
        // Set current bank if available
        int currentBank = player.getInstrument().getBankIndex();
        for (int i = 0; i < bankCombo.getItemCount(); i++) {
            String item = (String) bankCombo.getItemAt(i);
            if (item.equals("Bank " + currentBank)) {
                bankCombo.setSelectedIndex(i);
                break;
            }
        }
        
        // Update presets based on selected bank
        updatePresets();
    }
    
    /**
     * Update presets based on selected bank
     */
    private void updatePresets() {
        if (player == null || player.getInstrument() == null) {
            return;
        }
        
        // Clear existing items
        presetCombo.removeAllItems();
        
        // Get available presets for selected soundbank and bank
        String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
        int selectedBank = 0;
        
        // Extract bank number from selection
        String bankItem = (String) bankCombo.getSelectedItem();
        if (bankItem != null && bankItem.startsWith("Bank ")) {
            try {
                selectedBank = Integer.parseInt(bankItem.substring(5));
            } catch (NumberFormatException e) {
                logger.warn("Invalid bank format: {}", bankItem);
            }
        }
        
        // Use the correct method: getPresetNames
        List<String> presets = InternalSynthManager.getInstance().getPresetNames(selectedSoundbank, selectedBank);
        
        // Add presets to combo box
        for (int i = 0; i < presets.size(); i++) {
            String preset = presets.get(i);
            if (preset != null && !preset.isEmpty()) {
                presetCombo.addItem(i + ": " + preset);
            }
        }
        
        // Set current preset if available
        int currentPreset = player.getInstrument().getCurrentPreset();
        for (int i = 0; i < presetCombo.getItemCount(); i++) {
            String item = (String) presetCombo.getItemAt(i);
            if (item.startsWith(currentPreset + ": ")) {
                presetCombo.setSelectedIndex(i);
                break;
            }
        }
    }
    
    /**
     * Open a file dialog to browse for soundbank files
     */
    private void browseForSoundbank() {
        if (player == null || player.getInstrument() == null) {
            logger.warn("Cannot browse for soundbank: No instrument available");
            return;
        }
        
        // Create file chooser dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Soundbank File");
        chooser.setFileFilter(new FileNameExtensionFilter("Soundbank Files", "sf2", "dls"));
        
        // Show dialog and process result
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                // Load soundbank through InternalSynthManager
                InternalSynthManager.getInstance().loadSoundbank(selectedFile);
                
                // Update UI to show new soundbank
                updateUI();
                
            } catch (Exception e) {
                logger.error("Failed to load soundbank: {}", e.getMessage());
            }
        }
    }

    /**
     * Handle preset selection change
     */
    private void handlePresetChange(InstrumentPreset preset) {
        if (initializing || player == null || preset == null) return;
        
        try {
            // Update player with the selected preset
            player.setPreset(preset.getPresetNumber());
            
            // Apply the preset immediately if player has a sequencer
            if (player.getOwner() instanceof MelodicSequencer sequencer) {
                sequencer.applyInstrumentPreset();
            }
            
            // Save player to persist changes
            PlayerManager.getInstance().savePlayerProperties(player);
            
            logger.debug("Applied preset {} for player {}", preset.getPresetNumber(), player.getName());
        } catch (Exception e) {
            logger.error("Error changing preset: {}", e.getMessage(), e);
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
            // Get current settings
            String soundbank = (String) soundbankCombo.getSelectedItem();
            int bankIndex = 0;
            int presetIndex = 0;
            int channel = player.getChannel() != null ? player.getChannel() : 0;
            
            // Parse bank from selection
            String bankItem = (String) bankCombo.getSelectedItem();
            if (bankItem != null && bankItem.startsWith("Bank ")) {
                bankIndex = Integer.parseInt(bankItem.substring(5));
            }
            
            // Parse preset from selection
            String presetItem = (String) presetCombo.getSelectedItem();
            if (presetItem != null && presetItem.contains(":")) {
                presetIndex = Integer.parseInt(presetItem.substring(0, presetItem.indexOf(":")));
            }
            
            // Apply settings to the instrument
            InternalSynthManager.getInstance().applySoundbank(player.getInstrument(), soundbank);
            
            // Apply bank and program changes
            int bankMSB = (bankIndex >> 7) & 0x7F;
            int bankLSB = bankIndex & 0x7F;
            player.getInstrument().controlChange(channel, 0, bankMSB);
            player.getInstrument().controlChange(channel, 32, bankLSB);
            player.getInstrument().programChange(channel, presetIndex, 0);
            
            logger.info("Applied instrument settings: soundbank={}, bank={}, preset={}, channel={}",
                soundbank, bankIndex, presetIndex, channel);
        } catch (Exception e) {
            logger.error("Error applying instrument settings: {}", e.getMessage());
        }
    }

    /**
     * Apply current UI settings to the player
     */
    public void applySettings() {
        if (player == null || player.getInstrument() == null) {
            return;
        }
        
        // Apply all settings from UI components
        applyInstrumentSettings();
        
        // Save player properties through PlayerManager
        PlayerManager.getInstance().savePlayerProperties(player);
        
        // Publish events to notify the system
        CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, player.getInstrument());
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, player);
    }
}

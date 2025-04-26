package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InternalSynthManager;

public class SoundParametersPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // The player whose instrument we're configuring
    private Player player;
    
    // UI components
    private JComboBox<String> soundbankCombo;
    private JComboBox<String> bankCombo;
    private JComboBox<String> presetCombo;
    private JButton editButton; // Add this field
    
    /**
     * Constructor for SoundParametersPanel
     * 
     * @param player The player to configure sounds for (can be null)
     */
    public SoundParametersPanel(Player player) {
        this.player = player;
        
        // Set layout
        setLayout(new BorderLayout(3, 3));
        setBorder(BorderFactory.createTitledBorder("Sound"));
        
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
        // soundbankPanel.add(new JLabel("Soundbank:"));
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPreferredSize(new Dimension(150, 25));
        soundbankCombo.addActionListener(e -> {
            if (soundbankCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                String soundbank = soundbankCombo.getSelectedItem().toString();
                player.getInstrument().setSoundbankName(soundbank);
                updateBanksAndPresets();
            }
        });
        soundbankPanel.add(soundbankCombo);
        
        // Create bank selector
        JPanel bankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        // bankPanel.add(new JLabel("Bank:"));
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(100, 25));
        bankCombo.addActionListener(e -> {
            if (bankCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                int bankIndex = bankCombo.getSelectedIndex();
                player.getInstrument().setBankIndex(bankIndex);
                updatePresets();
            }
        });
        bankPanel.add(bankCombo);
        
        // Create preset selector
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        // presetPanel.add(new JLabel("Preset:"));
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(200, 25));
        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() != null && player != null && player.getInstrument() != null) {
                int presetIndex = presetCombo.getSelectedIndex();
                player.getInstrument().setCurrentPreset(presetIndex);
                
                // Notify system of instrument update
                CommandBus.getInstance().publish(
                    Commands.INSTRUMENT_UPDATED, 
                    this, 
                    player.getInstrument()
                );
            }
        });
        presetPanel.add(presetCombo);
        
        // Create edit button panel
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        editButton = new JButton(Symbols.getSymbol(Symbols.MIDI));
        editButton.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, 25));
        editButton.setToolTipText("Edit player parameters");
        editButton.addActionListener(e -> {
            if (player != null) {
                // Send command to open player editor dialog
                CommandBus.getInstance().publish(
                    Commands.PLAYER_EDIT_REQUEST, 
                    this, 
                    player
                );
            }
        });
        editPanel.add(editButton);
        
        // Add components to main panel
        controlsPanel.add(soundbankPanel);
        controlsPanel.add(bankPanel);
        controlsPanel.add(presetPanel);
        controlsPanel.add(editPanel);
        
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
            editButton.setEnabled(false); // Disable edit button as well
            return;
        }
        
        // Player has an instrument, enable controls and populate data
        soundbankCombo.setEnabled(true);
        bankCombo.setEnabled(true);
        presetCombo.setEnabled(true);
        editButton.setEnabled(true); // Enable edit button
        
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
}

package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * A reusable panel for managing sound parameters that works with any Player
 */
public class SoundParametersPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);
    
    // Size constants
    private static final int SMALL_CONTROL_WIDTH = 30;
    private static final int MEDIUM_CONTROL_WIDTH = 100;
    private static final int LARGE_CONTROL_WIDTH = 150;
    private static final int CONTROL_HEIGHT = 25;
    
    // UI Components
    private JComboBox<String> presetCombo;
    private JComboBox<String> soundbankCombo;
    private JButton editButton;
    
    // References
    private Player player;
    private final InternalSynthManager synthManager = InternalSynthManager.getInstance();
    
    // State tracking
    private boolean updatingUI = false;
    
    /**
     * Simple class to represent a preset item with a number and display name
     */
    private static class PresetItem {
        private final int number;
        private final String displayName;
        
        public PresetItem(int number, String displayName) {
            this.number = number;
            this.displayName = displayName;
        }
        
        public int getNumber() {
            return number;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    /**
     * Create a new sound parameters panel for the specified player
     * 
     * @param player The player to edit sound parameters for
     */
    public SoundParametersPanel(Player player) {
        this.player = player;
        initialize();
    }
    
    /**
     * Set a new player for this panel and update the UI
     * 
     * @param player The new player to use
     */
    public void setPlayer(Player player) {
        this.player = player;
        updateUI();
    }
    
    /**
     * Initialize the panel components
     */
    private void initialize() {
        // Create the panel with a titled border
        setBorder(BorderFactory.createTitledBorder("Sound Parameters"));
        setLayout(new BorderLayout(5, 5));
        
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        // Create soundbank combo
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        soundbankCombo.setToolTipText("Select soundbank");
        
        // Create preset combo
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(LARGE_CONTROL_WIDTH, CONTROL_HEIGHT));
        presetCombo.setToolTipText("Select instrument preset");
        
        // Create edit button with pencil icon and skinny width
        editButton = new JButton("✎");
        editButton.setToolTipText("Edit sound for this player");
        editButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));
        editButton.setMargin(new Insets(1, 1, 1, 1));
        editButton.setFocusable(false);
        
        // Add components to panel
        controlsPanel.add(soundbankCombo);
        controlsPanel.add(presetCombo);
        controlsPanel.add(editButton);
        
        add(controlsPanel, BorderLayout.CENTER);
        
        // Set up action listeners
        setupListeners();
        
        // Populate the combos
        populateSoundbankCombo();
    }
    
    /**
     * Set up the action listeners for the UI components
     */
    private void setupListeners() {
        // Soundbank selection listener
        soundbankCombo.addActionListener(e -> {
            if (updatingUI) return;
            
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            if (selectedSoundbank != null && player != null && player.getInstrument() != null) {
                logger.debug("Selected soundbank: {}", selectedSoundbank);
                
                // Update the instrument with the new soundbank
                player.getInstrument().setSoundbankName(selectedSoundbank);
                
                // Update the presets combo with presets from this soundbank
                populatePresetCombo(selectedSoundbank);
                
                // Notify that the player was updated
                CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATED,
                    this,
                    player
                );
            }
        });
        
        // Preset selection listener
        presetCombo.addActionListener(e -> {
            if (updatingUI || presetCombo.getSelectedIndex() < 0) return;
            
            int presetIndex = parsePresetNumber((String)presetCombo.getSelectedItem());
            if (presetIndex >= 0 && player != null) {
                logger.debug("Selected preset: {}", presetIndex);
                
                // Update the player's preset
                player.setPreset(presetIndex);
                
                // Apply the preset change to the instrument
                if (player.getInstrument() != null) {
                    String soundbankName = (String) soundbankCombo.getSelectedItem();
                    int bankIndex = player.getInstrument().getBankIndex() != null ? 
                                    player.getInstrument().getBankIndex() : 0;
                    
                    // Apply the preset change
                    synthManager.applyPresetChange(player.getInstrument(), bankIndex, presetIndex);
                }
                
                // Notify that the player was updated
                CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATED,
                    this,
                    player
                );
            }
        });
        
        // Edit button listener
        editButton.addActionListener(e -> {
            if (player != null) {
                logger.debug("Opening player editor for: {}", player.getName());
                
                // Select the player first
                CommandBus.getInstance().publish(
                    Commands.PLAYER_SELECTED,
                    this,
                    player
                );
                
                // Request to edit the player
                CommandBus.getInstance().publish(
                    Commands.PLAYER_EDIT_REQUEST,
                    this,
                    player
                );
            } else {
                logger.warn("Cannot edit player - player is not initialized");
            }
        });
    }
    
    /**
     * Populate the soundbank combo with available soundbanks
     */
    private void populateSoundbankCombo() {
        updatingUI = true;
        try {
            soundbankCombo.removeAllItems();
            
            // Get available soundbanks
            List<String> soundbanks = synthManager.getSoundbankNames();
            logger.debug("Available soundbanks: {}", soundbanks);
            if (soundbanks != null) {
                for (String name : soundbanks) {
                    // Skip blank soundbank names
                    if (name != null && !name.trim().isEmpty()) {
                        soundbankCombo.addItem(name);
                    }
                }
            }
            
            // Select current soundbank if available
            if (player != null && player.getInstrument() != null && 
                player.getInstrument().getSoundbankName() != null) {
                String currentSoundbank = player.getInstrument().getSoundbankName();
                
                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    if (soundbankCombo.getItemAt(i).equals(currentSoundbank)) {
                        soundbankCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (soundbankCombo.getItemCount() > 0) {
                // Default to first soundbank
                soundbankCombo.setSelectedIndex(0);
            }
            
            // Now populate the preset combo based on selected soundbank
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            if (selectedSoundbank != null) {
                populatePresetCombo(selectedSoundbank);
            }
            
        } finally {
            updatingUI = false;
        }
    }
    
    /**
     * Populate the preset combo with presets from the selected soundbank
     */
    private void populatePresetCombo(String soundbankName) {
        updatingUI = true;
        try {
            presetCombo.removeAllItems();
            logger.debug("Populating presets for soundbank: {}", soundbankName);
            
            // Get the bank index from the player's instrument
            int bankIndex = 0;
            if (player != null && player.getInstrument() != null && 
                player.getInstrument().getBankIndex() != null) {
                bankIndex = player.getInstrument().getBankIndex();
            }
            
            // Get preset names for this soundbank and bank
            List<String> presetNames;
            if (player != null && player.getInstrument() != null && 
                player.getInstrument().getInternal()) {
                // For internal synth, get presets from the soundbank
                presetNames = synthManager.getPresetNames(soundbankName, bankIndex);
                logger.debug("Got {} presets for internal synth", 
                             presetNames != null ? presetNames.size() : 0);
            } else {
                // For external instruments, use General MIDI preset names
                presetNames = synthManager.getGeneralMIDIPresetNames();
                logger.debug("Using General MIDI presets for external instrument");
            }
            
            // Ensure we have enough preset names
            if (presetNames == null || presetNames.size() < 128) {
                // Fill with default names or extend the list
                if (presetNames == null) {
                    presetNames = synthManager.getGeneralMIDIPresetNames();
                    logger.debug("Using General MIDI presets as fallback");
                } else {
                    int startIdx = presetNames.size();
                    for (int i = startIdx; i < 128; i++) {
                        presetNames.add("Program " + i);
                    }
                    logger.debug("Extended preset list to 128 entries");
                }
            }
            
            // Add formatted preset strings directly to the combo box
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String presetName = presetNames.get(i);
                if (presetName != null && !presetName.trim().isEmpty()) {
                    presetCombo.addItem(i + ": " + presetName);
                } else {
                    presetCombo.addItem(i + ": Program " + i);
                }
            }
            
            logger.debug("Added {} presets to combo", presetCombo.getItemCount());
            
            // Select current preset if available
            if (player != null && player.getPreset() != null) {
                int presetIndex = player.getPreset();
                if (presetIndex >= 0 && presetIndex < presetCombo.getItemCount()) {
                    presetCombo.setSelectedIndex(presetIndex);
                    logger.debug("Selected preset index: {}", presetIndex);
                }
            }
        } finally {
            updatingUI = false;
        }
    }
    
    /**
     * Extract preset number from preset string (e.g., "12: Piano" -> 12)
     */
    private int parsePresetNumber(String presetString) {
        try {
            if (presetString != null && presetString.contains(":")) {
                return Integer.parseInt(presetString.split(":")[0].trim());
            }
            return -1;
        } catch (NumberFormatException e) {
            logger.error("Failed to parse preset number from: {}", presetString);
            return -1;
        }
    }
    
    /**
     * Update the UI to reflect the current player state
     */
    public void updateUI() {
        if (player == null) return;
        
        // Reload the soundbanks and presets
        populateSoundbankCombo();
    }
}
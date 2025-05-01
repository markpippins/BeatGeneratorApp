package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.sequencer.DrumItem;
import com.angrysurfer.core.sequencer.PresetItem;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.UIUtils;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for editing sound parameters using CommandBus pattern
 */
@Getter
@Setter
public class SoundParametersPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // Player reference
    private Player player;
    private Long playerId;

    // UI Components
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JComboBox<PresetItem> presetCombo;
    private JButton editButton;
    private JLabel soundbankLabel;
    private JLabel bankLabel;
    private JLabel presetLabel;

    // State management
    private boolean isInitializing = false;
    private boolean isDrumChannel = false;

    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();
    private final InstrumentManager instrumentManager = InstrumentManager.getInstance();

    /**
     * Constructor
     */
    public SoundParametersPanel(Player player) {
        super(new BorderLayout());
        this.player = player;

        if (player != null) {
            this.playerId = player.getId();
        }

        initComponents();
        layoutComponents();
        registerForEvents();

        // Initial UI update
        updateUI();
    }

    /**
     * Register for command bus events
     */
    private void registerForEvents() {
        commandBus.register(this);
    }

    /**
     * Add explicit handling for PLAYER_INSTRUMENT_CHANGED and PLAYER_CHANNEL_CHANGE_REQUEST events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        // PLAYER_SELECTED is a special case - we need to handle it even if playerId is null
        if (Commands.PLAYER_SELECTED.equals(action.getCommand())) {
            handlePlayerActivated(action);
            return;
        }
        
        // For all other events, only process if we have a valid player
        if (playerId == null)
            return;

        switch (action.getCommand()) {
            case Commands.PLAYER_UPDATED -> handlePlayerUpdated(action);
            case Commands.PLAYER_PRESET_CHANGED -> handlePlayerPresetChanged(action);
            case Commands.PLAYER_INSTRUMENT_CHANGED -> handlePlayerInstrumentChanged(action);
            case Commands.PLAYER_CHANNEL_CHANGE_REQUEST -> handlePlayerChannelChanged(action);
        }
    }

    /**
     * Handle player update events
     */
    private void handlePlayerUpdated(Command action) {
        if (action.getData() instanceof Player updatedPlayer &&
                playerId.equals(updatedPlayer.getId())) {

            SwingUtilities.invokeLater(() -> {
                // Check what properties have changed
                boolean channelChanged = player.getChannel() != updatedPlayer.getChannel();
                boolean instrumentChanged = !Objects.equals(
                        player.getInstrumentId(), updatedPlayer.getInstrumentId());
                
                // Update our reference
                player = updatedPlayer;
                
                logger.debug("Player updated: channel={} (changed={}), instrument={} (changed={})",
                        player.getChannel(), channelChanged,
                        player.getInstrumentId(), instrumentChanged);
                
                // Check if we're switching to/from drum channel
                boolean wasDrumChannel = isDrumChannel;
                isDrumChannel = player.getChannel() == 9;
                
                // If we've switched channel type or instrument type, do a full refresh
                if (wasDrumChannel != isDrumChannel || instrumentChanged) {
                    updateUI();
                } else {
                    // Just update from the player without redoing the whole UI
                    updateFromPlayer(player);
                }
            });
        }
    }

    /**
     * Handle player activation events
     */
    private void handlePlayerActivated(Command action) {
        if (action.getData() instanceof Player activatedPlayer) {
            logger.info("Received player selection: {}", activatedPlayer.getName());

            SwingUtilities.invokeLater(() -> {
                // Save previous player ID to check if this is a change
                Long oldPlayerId = playerId;

                // Update references to the new player
                player = activatedPlayer;
                playerId = activatedPlayer.getId();

                // Log the change for debugging
                logger.debug("Updating UI for player {}: {} (changed from: {})",
                        playerId, player.getName(), oldPlayerId);

                // Always call updateUI() to refresh the display
                updateUI();

                // Additional refresh for internal components
                updateFromPlayer(player);
            });
        } else {
            logger.warn("Received PLAYER_SELECTED event with invalid data: {}",
                    action.getData() != null ? action.getData().getClass().getName() : "null");
        }
    }

    /**
     * Handle preset change events
     */
    private void handlePlayerPresetChanged(Command action) {
        if (action.getData() instanceof Object[] data &&
                data.length >= 2 &&
                data[0] instanceof Long id &&
                playerId.equals(id)) {

            Integer presetNumber = (Integer) data[1];

            SwingUtilities.invokeLater(() -> {
                // Update UI to reflect new preset without triggering events
                isInitializing = true;
                try {
                    // Find and select preset in combo
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        PresetItem item = presetCombo.getItemAt(i);
                        if (item.getNumber() == presetNumber) {
                            presetCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                } finally {
                    isInitializing = false;
                }
            });
        }
    }

    /**
     * Handle instrument changes from InstrumentCombo
     */
    private void handlePlayerInstrumentChanged(Command action) {
        if (action.getData() instanceof Object[] data && 
                data.length >= 2 && 
                data[0] instanceof Long id && 
                playerId.equals(id)) {
            
            // Refresh from updated player
            Player updatedPlayer = playerManager.getPlayerById(playerId);
            if (updatedPlayer != null) {
                logger.debug("Instrument changed for player {}, refreshing UI", playerId);
                
                SwingUtilities.invokeLater(() -> {
                    player = updatedPlayer;
                    updateUI();
                    
                    // Handle switching instrument controls based on internal/external instrument
                    boolean isInternal = isInternalSynth(player.getInstrument());
                    updateControlsVisibility(isInternal, player.getChannel() == 9);
                });
            }
        }
    }

    /**
     * Handle channel changes from ChannelCombo
     */
    private void handlePlayerChannelChanged(Command action) {
        if (action.getData() instanceof Player updatedPlayer && 
                playerId.equals(updatedPlayer.getId())) {
            
            logger.debug("Channel changed for player {} to {}", 
                    playerId, updatedPlayer.getChannel());
            
            SwingUtilities.invokeLater(() -> {
                // Check if we're switching to/from drum channel
                boolean wasDrumChannel = isDrumChannel;
                isDrumChannel = updatedPlayer.getChannel() == 9;
                
                if (wasDrumChannel != isDrumChannel) {
                    logger.debug("Switching between melodic/drum channel, refreshing controls");
                    
                    player = updatedPlayer;
                    
                    // Clear and update UI for channel type change
                    if (isDrumChannel) {
                        populatePresetComboWithDrumSounds();
                    } else {
                        if (isInternalSynth(player.getInstrument())) {
                            populateSoundbankCombo();
                        }
                    }
                    
                    // Update visibility of controls
                    updateControlsVisibility(
                        isInternalSynth(player.getInstrument()), 
                        isDrumChannel
                    );
                }
            });
        }
    }

    /**
     * Extract control visibility logic to a separate method for reuse
     */
    private void updateControlsVisibility(boolean isInternal, boolean isDrumChannel) {
        // For drum channel, hide soundbank and bank controls
        if (isDrumChannel) {
            soundbankCombo.setVisible(false);
            bankCombo.setVisible(false);
            soundbankLabel.setVisible(false);
            bankLabel.setVisible(false);
            presetLabel.setText("Drum:");
        } 
        // For melodic instruments, show/hide based on internal/external
        else {
            // Always show preset for melodic
            presetCombo.setVisible(true);
            presetLabel.setVisible(true);
            presetLabel.setText("Preset:");
            
            // Only show soundbank/bank for internal instruments
            soundbankCombo.setVisible(isInternal);
            bankCombo.setVisible(isInternal);
            soundbankLabel.setVisible(isInternal);
            bankLabel.setVisible(isInternal);
        }
        
        // Update the panel layout
        revalidate();
        repaint();
    }

    /**
     * Initialize UI components
     */
    private void initComponents() {
        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        presetCombo = new JComboBox<>();
        editButton = new JButton(Symbols.getSymbol(Symbols.MIDI));
        editButton.setToolTipText("Edit Player Properties");

        // Add listeners
        soundbankCombo.addActionListener(e -> {
            if (!isInitializing && soundbankCombo.getSelectedItem() != null) {
                handleSoundbankChange((String) soundbankCombo.getSelectedItem());
            }
        });

        bankCombo.addActionListener(e -> {
            if (!isInitializing && bankCombo.getSelectedItem() != null) {
                handleBankChange((Integer) bankCombo.getSelectedItem());
            }
        });

        presetCombo.addActionListener(e -> {
            if (!isInitializing && presetCombo.getSelectedItem() != null) {
                PresetItem preset = (PresetItem) presetCombo.getSelectedItem();
                handlePresetChange(preset.getNumber());
            }
        });


        editButton.addActionListener(e -> {
            if (player != null) {
                // Request to open player editor
                commandBus.publish(Commands.PLAYER_EDIT_REQUEST, this, player);
            }
        });

        // Add tooltip support in the renderer
        presetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel && value instanceof PresetItem) {
                    PresetItem item = (PresetItem) value;
                    String text = item.toString();

                    // Set tooltip with full text
                    ((JLabel) c).setToolTipText(text);

                    // Display logic as before...
                }
                return c;
            }
        });
    }

    /**
     * Layout components in a horizontal arrangement
     */
    private void layoutComponents() {
        UIUtils.setWidgetPanelBorder(this, "Sound");

        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JPanel soundbankPanel = new JPanel(new BorderLayout(5, 0));
        soundbankPanel.add(new JLabel("Soundbank:"), BorderLayout.WEST);
        soundbankCombo = new JComboBox<>();
        soundbankPanel.add(soundbankCombo, BorderLayout.CENTER);
        soundbankCombo.setPreferredSize(new Dimension(150, soundbankCombo.getPreferredSize().height));
        mainPanel.add(soundbankPanel);

        // 4. Bank combo
        JPanel bankPanel = new JPanel(new BorderLayout(5, 0));
        bankPanel.add(new JLabel("Bank:"), BorderLayout.WEST);
        bankPanel.add(bankCombo, BorderLayout.CENTER);
        bankCombo.setPreferredSize(new Dimension(80, bankCombo.getPreferredSize().height));
        mainPanel.add(bankPanel);

        // 5. Preset combo
        JPanel presetPanel = new JPanel(new BorderLayout(5, 0));
        presetPanel.add(new JLabel("Preset:"), BorderLayout.WEST);
        presetPanel.add(presetCombo, BorderLayout.CENTER);
        presetCombo.setPreferredSize(new Dimension(180, presetCombo.getPreferredSize().height));
        mainPanel.add(presetPanel);

        // 6. Edit button
        mainPanel.add(editButton);

        // Save label references for later access
        soundbankLabel = (JLabel) soundbankPanel.getComponent(0);
        bankLabel = (JLabel) bankPanel.getComponent(0);
        presetLabel = (JLabel) presetPanel.getComponent(0);


        // Add everything to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Update UI from player data
     */
    @Override
    public void updateUI() {
        super.updateUI();
        
        if (soundbankCombo == null || player == null)
            return;
            
        isInitializing = true;
        try {
            // Set channel type
            isDrumChannel = player.getChannel() == 9;
            
            // Clear all combos
            soundbankCombo.removeAllItems();
            bankCombo.removeAllItems();
            presetCombo.removeAllItems();
            
            // Populate appropriate combos based on channel and instrument type
            if (isDrumChannel) {
                populatePresetComboWithDrumSounds();
            } else if (isInternalSynth(player.getInstrument())) {
                populateSoundbankCombo();
            } else {
                // External synth - just populate presets
                populateExternalPresets();
            }
            
            // Update which controls are visible
            updateControlsVisibility(
                isInternalSynth(player.getInstrument()), 
                isDrumChannel
            );
            
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Populate presets for external instruments
     */
    private void populateExternalPresets() {
        // Add preset items 0-127 for external instruments
        for (int i = 0; i < 128; i++) {
            presetCombo.addItem(new PresetItem(i, "Program " + i));
        }
        
        // Select current preset if available
        if (player != null && player.getInstrument() != null && 
                player.getInstrument().getPreset() != null) {
            int preset = player.getInstrument().getPreset();
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == preset) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void setControlsEnabled(boolean enabled) {
        soundbankCombo.setEnabled(enabled);
        bankCombo.setEnabled(enabled);
        presetCombo.setEnabled(enabled);
        editButton.setEnabled(enabled);
    }

    /**
     * Populate soundbank combo
     */
    private void populateSoundbankCombo() {
        if (player == null || player.getInstrument() == null)
            return;

        // Clear combo
        soundbankCombo.removeAllItems();

        // Get soundbanks from manager
        List<String> soundbanks = InternalSynthManager.getInstance().getSoundbankNames();

        // Add to combo
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }

        // Select current soundbank
        String currentSoundbank = player.getInstrument().getSoundbankName();
        if (currentSoundbank != null) {
            soundbankCombo.setSelectedItem(currentSoundbank);
        } else if (soundbankCombo.getItemCount() > 0) {
            soundbankCombo.setSelectedIndex(0);

            // Update the instrument with selected soundbank
            String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            player.getInstrument().setSoundbankName(selectedSoundbank);

            // Update the UI based on the selected soundbank
            handleSoundbankChange(selectedSoundbank);
        }
    }

    /**
     * Populate bank combo based on selected soundbank
     */
    private void updateBankCombo(String selectedSoundbank) {
        if (player == null || player.getInstrument() == null)
            return;

        isInitializing = true;
        try {
            // Clear existing items
            bankCombo.removeAllItems();

            // Get banks for selected soundbank
            List<Integer> banks = InternalSynthManager.getInstance()
                    .getAvailableBanksByName(selectedSoundbank);

            // Add to combo
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }

            // Select current bank
            Integer currentBank = player.getInstrument().getBankIndex();
            if (currentBank != null) {
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    Integer bank = bankCombo.getItemAt(i);
                    if (bank.equals(currentBank)) {
                        bankCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);

                // Update player's instrument with selected bank
                player.getInstrument().setBankIndex((Integer) bankCombo.getSelectedItem());
            }

            // This will trigger updatePresetCombo through action listener
            Integer selectedBank = (Integer) bankCombo.getSelectedItem();
            if (selectedBank != null) {
                updatePresetCombo(selectedSoundbank, selectedBank);
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Populate preset combo based on selected bank
     */
    private void updatePresetCombo(String soundbank, Integer bank) {
        if (player == null || player.getInstrument() == null)
            return;

        isInitializing = true;
        try {
            // Clear existing items
            presetCombo.removeAllItems();

            // Get instrument name
            String instrumentName = player.getInstrument().getName() != null ? player.getInstrument().getName()
                    : "Unknown";

            // Get presets for selected soundbank and bank
            List<String> presets = InternalSynthManager.getInstance()
                    .getPresetNames(soundbank, bank);

            // Add to combo as PresetItems with instrument name included
            for (int i = 0; i < Math.min(128, presets.size()); i++) {
                String name = presets.get(i);
                if (name == null || name.isEmpty()) {
                    name = "Program " + i;
                }
                // Include instrument name and soundbank in the displayed text
                String toolTipText = String.format("%s [%s] - %d: %s",
                        instrumentName, soundbank, i, name);

                presetCombo.setToolTipText(toolTipText);
                presetCombo.addItem(new PresetItem(i, name));
            }

            // Select current preset
            Integer currentPreset = player.getInstrument().getPreset();
            if (currentPreset != null) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == currentPreset) {
                        presetCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);

                // Update player's instrument with selected preset
                PresetItem item = presetCombo.getItemAt(0);
                player.getInstrument().setPreset(item.getNumber());
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Populate preset combo with drum sounds for channel 9
     */
    private void populatePresetComboWithDrumSounds() {
        if (player == null) return;
        
        // Clear existing items
        presetCombo.removeAllItems();
        
        try {
            // Get drum sounds from InternalSynthManager
            List<DrumItem> drumItems = InternalSynthManager.getInstance().getDrumItems();
            
            // Add to combo
            for (DrumItem item : drumItems) {
                presetCombo.addItem(new PresetItem(item.getNoteNumber(), item.getName()));
            }
            
            // Select current drum sound if available
            if (player.getRootNote() != null) {
                int desiredNote = player.getRootNote();
                
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == desiredNote) {
                        presetCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (presetCombo.getItemCount() > 0) {
                // Default to first drum sound
                presetCombo.setSelectedIndex(0);
                
                // Update player's root note
                PresetItem item = presetCombo.getItemAt(0);
                player.setRootNote(item.getNumber());
            }
            
            logger.debug("Populated {} drum sounds for channel 9", presetCombo.getItemCount());
        } catch (Exception e) {
            logger.error("Error populating drum sounds: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle soundbank selection change
     */
    private void handleSoundbankChange(String soundbank) {
        if (player == null || player.getInstrument() == null)
            return;

        try {
            // Update player's instrument
            player.getInstrument().setSoundbankName(soundbank);

            // Request update through command bus
            commandBus.publish(Commands.PLAYER_UPDATE_REQUEST, this, player);

            // Update dependent UI components
            updateBankCombo(soundbank);
        } catch (Exception e) {
            logger.error("Error handling soundbank change: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle bank selection change
     */
    private void handleBankChange(Integer bank) {
        if (player == null || player.getInstrument() == null)
            return;

        try {
            // Update player's instrument
            player.getInstrument().setBankIndex(bank);

            // Request update through command bus
            commandBus.publish(Commands.PLAYER_UPDATE_REQUEST, this, player);

            // Update dependent UI components
            String soundbank = (String) soundbankCombo.getSelectedItem();
            if (soundbank != null) {
                updatePresetCombo(soundbank, bank);
            }
        } catch (Exception e) {
            logger.error("Error handling bank change: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle preset selection change
     */
    private void handlePresetChange(int presetNumber) {
        if (player == null) return;
        
        try {
            if (isDrumChannel) {
                // For drum channel, this is a drum note number
                player.setRootNote(presetNumber);
                
                // Apply drum selection
                playerManager.savePlayerProperties(player);
                
                // Publish change event
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                
                logger.info("Changed drum sound for player {} to {}", 
                        player.getName(), presetNumber);
            } else {
                // For melodic channels, regular preset handling
                commandBus.publish(Commands.PLAYER_PRESET_CHANGE_REQUEST, this,
                        new Object[] { player.getId(), presetNumber });
            }
        } catch (Exception e) {
            logger.error("Error handling preset change: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if instrument is an internal synth
     */
    private boolean isInternalSynth(InstrumentWrapper instrument) {
        if (instrument == null)
            return false;

        // Check based on device name
        if (instrument.getDeviceName() != null) {
            String deviceName = instrument.getDeviceName().toLowerCase();
            return deviceName.contains("gervill") ||
                    deviceName.contains("gs wavetable") ||
                    deviceName.contains("java sound synthesizer");
        }

        // Also check internal flag
        return Boolean.TRUE.equals(instrument.getInternal());
    }

    /**
     * Update this panel from the player object
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        // Store reference to the new player
        this.player = newPlayer;

        // Temporarily disable initialization to prevent events
        boolean wasInitializing = isInitializing;
        isInitializing = true;

        try {
 
            UIUtils.setWidgetPanelBorder(this, player.getName() + " [" + player.getId().toString() + "]");

            // Update soundbank combo
            if (soundbankCombo != null && player.getInstrument() != null &&
                    player.getInstrument().getSoundbankName() != null) {
                soundbankCombo.setSelectedItem(player.getInstrument().getSoundbankName());
            }

            // Update bank combo
            if (bankCombo != null && player.getInstrument() != null &&
                    player.getInstrument().getBankIndex() != null) {
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    Integer bank = bankCombo.getItemAt(i);
                    if (bank != null && bank.equals(player.getInstrument().getBankIndex())) {
                        bankCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // Update preset combo
            if (presetCombo != null && player.getInstrument() != null &&
                    player.getInstrument().getPreset() != null) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item != null && item.getNumber() == player.getInstrument().getPreset()) {
                        presetCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

            logger.debug("Updated SoundParametersPanel from player: {}", player.getName());
        } finally {
            // Re-enable event processing
            isInitializing = wasInitializing;
        }
    }
}

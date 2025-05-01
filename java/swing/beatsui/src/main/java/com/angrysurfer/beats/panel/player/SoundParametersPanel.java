package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

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
    private JLabel playerNameLabel;
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

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        // Only process events relevant to our current player
        if (playerId == null)
            return;

        switch (action.getCommand()) {
            case Commands.PLAYER_UPDATED -> handlePlayerUpdated(action);
            case Commands.PLAYER_SELECTED -> handlePlayerActivated(action);
            case Commands.PLAYER_PRESET_CHANGED -> handlePlayerPresetChanged(action);
//            case Commands.PLAYER_INSTRUMENT_CHANGED -> handlePlayerInstrumentChanged(action);
        }
    }

    /**
     * Handle player update events
     */
    private void handlePlayerUpdated(Command action) {
        if (action.getData() instanceof Player updatedPlayer &&
                playerId.equals(updatedPlayer.getId())) {

            // Update our player reference with fresh data
            SwingUtilities.invokeLater(() -> {
                player = updatedPlayer;
                updateUI();
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
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Sound"),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)));

        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // 1. Add Player name label - NEW!
        JPanel playerPanel = new JPanel(new BorderLayout(5, 0));
        playerPanel.add(new JLabel("Player:"), BorderLayout.WEST);
        JLabel playerNameLabel = new JLabel(player != null ? player.getName() : "");
        playerNameLabel.setPreferredSize(new Dimension(120, playerNameLabel.getPreferredSize().height));
        playerNameLabel.setFont(playerNameLabel.getFont().deriveFont(Font.BOLD));
        playerPanel.add(playerNameLabel, BorderLayout.CENTER);
        mainPanel.add(playerPanel);


        // 3. Soundbank combo - NEW!
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

        // Store references for later updates
        this.playerNameLabel = playerNameLabel;

        // Add everything to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Update UI from player data
     */
    @Override
    public void updateUI() {
        super.updateUI();
        
        if (soundbankCombo == null)
            return;
            
        isInitializing = true;
        try {
            // Update player name
            if (playerNameLabel != null) {
                playerNameLabel.setText(player != null ? player.getName() + " " + player.getId(): "");
            }
            
            // Update channel selection
            if (player != null) {
                // Check if we're on drum channel
                boolean wasDrumChannel = isDrumChannel;
                isDrumChannel = player.getChannel() == 9;
                
                // If switching to/from drum channel, update combo contents
                if (isDrumChannel != wasDrumChannel || presetCombo.getItemCount() == 0) {
                    if (isDrumChannel) {
                        // Change preset label
                        presetLabel.setText("Drum:");
                        
                        // Hide soundbank/bank for drums
                        soundbankCombo.setVisible(false);
                        bankCombo.setVisible(false);
                        soundbankLabel.setVisible(false);
                        bankLabel.setVisible(false);
                        
                        // Populate with drum sounds
                        populatePresetComboWithDrumSounds();
                    } else {
                        // Restore label
                        presetLabel.setText("Preset:");
                        
                        // Show controls for melodic instruments
                        soundbankCombo.setVisible(true);
                        bankCombo.setVisible(true);
                        soundbankLabel.setVisible(true);
                        bankLabel.setVisible(true);
                        
                        // Only for internal synth
                        if (isInternalSynth(player.getInstrument())) {
                            populateSoundbankCombo();
                        }
                    }
                }
            }
            
            // Rest of your updateUI method...
            
        } finally {
            isInitializing = false;
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

            // Update player name label
            if (playerNameLabel != null) {
                playerNameLabel.setText(player.getName());
            }

            logger.debug("Updated SoundParametersPanel from player: {}", player.getName());
        } finally {
            // Re-enable event processing
            isInitializing = wasInitializing;
        }
    }
}
 
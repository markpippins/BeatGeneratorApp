package com.angrysurfer.beats.panel.player;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import com.angrysurfer.beats.panel.PlayerAwarePanel;
import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.beats.widget.InstrumentCombo;
import com.angrysurfer.core.service.InternalSynthManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.AddInstrumentButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.DrumItem;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.PresetItem;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SoundbankManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Refactored panel for sound parameters with better manager use
 */
@Getter
@Setter
public class SoundParametersPanel extends PlayerAwarePanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // Player reference

    // UI Components
    private JTextField nameTextField;
    private ChannelCombo channelCombo;
    private InstrumentCombo instrumentCombo;
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JComboBox<PresetItem> presetCombo;
    private JButton editButton;
    private JLabel soundbankLabel;
    private JLabel bankLabel;
    private JLabel presetLabel;

    // State management
    private boolean isInitializing = false;

    private PresetItem currentPreset = null;


    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();
    private final SoundbankManager soundbankManager = SoundbankManager.getInstance();

    /**
     * Constructor with improved manager delegation
     */
    public SoundParametersPanel() {
        super();
        setLayout(new BorderLayout());

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
     * Layout the components in a horizontal arrangement with standardized sizing
     */
    private void layoutComponents() {
        // Use compact FlowLayout instead of BoxLayout for more consistent spacing
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1)); // REDUCED: Use 2,1 spacing like other
                                                                                 // panels

        UIHelper.setWidgetPanelBorder(this, "Player");

        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        // playerPanel.add(new JLabel("Player:"));
        playerPanel.add(nameTextField);

        JPanel instrumentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        // instrumentPanel.add(new JLabel("Instrument:"));
        instrumentPanel.add(instrumentCombo);

        AddInstrumentButton addInstrumentButton = new AddInstrumentButton();
        instrumentPanel.add(addInstrumentButton);

        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        channelPanel.add(new JLabel("Ch:"));
        channelPanel.add(channelCombo);

        // Standardize control sizes to match other panels
        instrumentCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH * 3, UIHelper.CONTROL_HEIGHT));
        channelCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH - 5, UIHelper.CONTROL_HEIGHT));
        soundbankCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));
        bankCombo.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));
        presetCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));

        // Use standard small button size for edit button
        editButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        editButton.setMargin(new Insets(2, 2, 2, 2)); // Match other buttons' margins

        // Simplify panel structure - inline labels instead of separate panels
        JPanel soundbankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        soundbankPanel.add(new JLabel("Soundbank:"));
        soundbankPanel.add(soundbankCombo);

        JPanel bankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        bankPanel.add(new JLabel("Bank:"));
        bankPanel.add(bankCombo);

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        presetPanel.add(new JLabel("Preset:"));
        presetPanel.add(presetCombo);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        buttonPanel.add(editButton);

        // Add all panels to content panel
        contentPanel.add(playerPanel);
        contentPanel.add(channelPanel);
        contentPanel.add(instrumentPanel);
        contentPanel.add(soundbankPanel);
        contentPanel.add(bankPanel);
        contentPanel.add(presetPanel);
        contentPanel.add(buttonPanel);

        // Add to main layout
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Handle command events with improved manager delegation
     */
    @Override
    public void onAction(Command action) {
        super.onAction(action);
        if (action == null || action.getCommand() == null)
            return;

        // Only process relevant commands for efficiency
        switch (action.getCommand()) {

            case Commands.SOUNDBANKS_REFRESHED:
                if (getPlayer() != null) {
                    SwingUtilities.invokeLater(this::updateSoundbankCombo);
                }
                break;
        }
    }

    @Override
    public void handlePlayerActivated() {
        updateUI();
    }

    @Override
    public void handlePlayerUpdated() {
        updateUI();
    }

    /**
     * Initialize UI components with better manager delegation
     */
    private void initComponents() {
        nameTextField = new JTextField(15);
        nameTextField.setEditable(false);
        nameTextField.setFocusable(false);
        nameTextField.setBackground(Color.GRAY);

        channelCombo = new ChannelCombo();
        instrumentCombo = new InstrumentCombo();

        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        presetCombo = new JComboBox<>();
        editButton = new JButton("Edit");
        editButton.setToolTipText("Edit Player Properties");

        // Add listeners with improved manager delegation
        soundbankCombo.addActionListener(e -> {
            if (!isInitializing && soundbankCombo.getSelectedItem() != null) {
                String soundbank = (String) soundbankCombo.getSelectedItem();
                if (getPlayer() != null && getPlayer().getInstrument() != null) {
                    // Let SoundbankManager handle the change
                    soundbankManager.applySoundbank(getPlayer().getInstrument(), soundbank);

                    // Update UI for the change
                    updateBankCombo();

                    // Notify system of player update
                    playerManager.savePlayerProperties(getPlayer());
                    commandBus.publish(Commands.PLAYER_UPDATED, this, getPlayer());
                }
            }
        });

        bankCombo.addActionListener(e -> {
            if (!isInitializing && bankCombo.getSelectedItem() != null) {
                Integer bank = (Integer) bankCombo.getSelectedItem();
                if (getPlayer() != null && getPlayer().getInstrument() != null) {
                    // Update instrument with bank
                    getPlayer().getInstrument().setBankIndex(bank);

                    // Let SoundbankManager apply the change
                    soundbankManager.applyPresetChange(
                            getPlayer().getInstrument(),
                            bank,
                            getPlayer().getInstrument().getPreset());

                    // Update UI for the change
                    updatePresetCombo();

                    // Notify system of player update
                    playerManager.savePlayerProperties(getPlayer());
                    commandBus.publish(Commands.PLAYER_UPDATED, this, getPlayer());
                }
            }
        });

        presetCombo.addActionListener(e -> {
            if (!isInitializing && presetCombo.getSelectedItem() != null) {
                PresetItem preset = (PresetItem) presetCombo.getSelectedItem();
                if (preset != null && currentPreset != null && preset.getNumber() == currentPreset.getNumber())
                    return;

                currentPreset = preset;

                // Get the current player to ensure changes only affect this player
                Player targetPlayer = getPlayer();
                if (targetPlayer != null && targetPlayer.getInstrument() != null) {
                    // CRITICAL: Store player ID to verify we're affecting the same player
                    final Long targetPlayerId = targetPlayer.getId();
                    
                    // CRITICAL: Update the instrument's preset property directly
                    targetPlayer.getInstrument().setPreset(preset.getNumber());
                    
                    // Let SoundbankManager handle the preset change - ONLY for this specific player
                    if (targetPlayer.getChannel() == 9) {
                        // Handle drum note change
                        targetPlayer.setRootNote(preset.getNumber());
                        
                        // Only apply to this specific player's instrument
                        soundbankManager.applyPresetChangeToPlayer(
                            targetPlayer,
                            targetPlayer.getInstrument().getBankIndex(),
                            preset.getNumber()
                        );
                    } else {
                        // Handle melodic preset change - for THIS player only
                        soundbankManager.applyPresetChangeToPlayer(
                            targetPlayer,
                            targetPlayer.getInstrument().getBankIndex(),
                            preset.getNumber()
                        );
                    }
                    
                    // Save the changes
                    playerManager.savePlayerProperties(targetPlayer);
                    
                    // Verify player is still the one we were working with
                    if (getPlayer() != null && getPlayer().getId().equals(targetPlayerId)) {
                        // Notify system of player update
                        commandBus.publish(Commands.PLAYER_UPDATED, this, targetPlayer);
                        
                        // Add explicit message to ensure this player's instrument is refreshed
                        commandBus.publish(
                            Commands.REFRESH_PLAYER_INSTRUMENT, 
                            this,
                            targetPlayer.getId()
                        );
                    }
                }
            }
        });
        
        editButton.addActionListener(e -> {
            if (getPlayer() != null) {
                commandBus.publish(Commands.PLAYER_EDIT_REQUEST, this, getPlayer());
            }
        });
    }

    /**
     * Update UI from player data with improved manager integration
     */
    @Override
    public void updateUI() {
        super.updateUI();

        if (soundbankCombo == null || getPlayer() == null)
            return;

        // Store the active player to ensure changes only affect this player
        final Player targetPlayer = getPlayer();
        
        isInitializing = true;
        try {
            nameTextField.setText(targetPlayer.getName());

            boolean isDrumChannel = targetPlayer.getChannel() == 9;
            boolean isInternalSynth = targetPlayer.getInstrument() != null &&
                    InternalSynthManager.getInstance().isInternalSynthInstrument(targetPlayer.getInstrument());

            // Update UI according to instrument type
            updateControlVisibility(isInternalSynth, isDrumChannel);

            if (isDrumChannel) {
                // Let SoundbankManager handle populating drum sounds
                updateDrumPresetCombo();
            } else if (isInternalSynth) {
                // Update all components for internal synth - ENSURE CORRECT SEQUENCE
                updateSoundbankCombo();
                // Wait until soundbank is selected before updating banks
                SwingUtilities.invokeLater(() -> {
                    updateBankCombo();
                    // Wait until bank is selected before updating presets
                    SwingUtilities.invokeLater(this::updatePresetCombo);
                });
            } else {
                // For external synth, just populate generic presets
                updateExternalPresetCombo();
            }

        } finally {
            isInitializing = false;
        }
    }

    /**
     * Update control visibility based on instrument type
     */
    private void updateControlVisibility(boolean isInternalSynth, boolean isDrumChannel) {
        // For drum channel, hide soundbank and bank controls
        if (isDrumChannel) {
            soundbankCombo.getParent().setVisible(false);
            bankCombo.getParent().setVisible(false);
            ((JLabel) ((JPanel) presetCombo.getParent()).getComponent(0)).setText("Drum:");
        } else {
            // For melodic instruments, show/hide based on internal/external
            presetCombo.getParent().setVisible(true);
            ((JLabel) ((JPanel) presetCombo.getParent()).getComponent(0)).setText("Preset:");

            // Only show soundbank/bank for internal instruments
            soundbankCombo.getParent().setVisible(isInternalSynth);
            bankCombo.getParent().setVisible(isInternalSynth);
        }

        // Update the panel layout
        revalidate();
        repaint();
    }

    /**
     * Update soundbank combo with data from SoundbankManager
     */
    private void updateSoundbankCombo() {
        if (getPlayer() == null || getPlayer().getInstrument() == null)
            return;

        soundbankCombo.removeAllItems();

        // Let SoundbankManager provide the soundbank list
        List<String> soundbanks = soundbankManager.getSoundbankNames();
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }

        // Select current soundbank
        String currentSoundbank = getPlayer().getInstrument().getSoundbankName();
        if (currentSoundbank != null && !currentSoundbank.isEmpty()) {
            soundbankCombo.setSelectedItem(currentSoundbank);
        } else if (soundbankCombo.getItemCount() > 0) {
            // If no soundbank is set, select the first one and update the instrument
            String defaultSoundbank = soundbankCombo.getItemAt(0).toString();
            getPlayer().getInstrument().setSoundbankName(defaultSoundbank);
            logger.debug("Setting default soundbank: {}", defaultSoundbank);
            soundbankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Update bank combo with data from SoundbankManager
     */
    private void updateBankCombo() {
        if (getPlayer() == null || getPlayer().getInstrument() == null)
            return;

        bankCombo.removeAllItems();

        // Get banks from SoundbankManager for current soundbank
        String soundbank = getPlayer().getInstrument().getSoundbankName();
        if ((soundbank == null || soundbank.isEmpty()) && soundbankCombo.getSelectedItem() != null) {
            soundbank = soundbankCombo.getSelectedItem().toString();
            // Update the instrument with the selected soundbank
            getPlayer().getInstrument().setSoundbankName(soundbank);
            logger.debug("Updated instrument soundbank to: {}", soundbank);
        }

        if (soundbank != null && !soundbank.isEmpty()) {
            // Let SoundbankManager provide the bank list
            List<Integer> banks = soundbankManager.getAvailableBanksByName(soundbank);
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }
        }

        // Select current bank
        Integer currentBank = getPlayer().getInstrument().getBankIndex();
        if (currentBank != null) {
            // Check if the current bank exists in the combo
            boolean bankExists = false;
            for (int i = 0; i < bankCombo.getItemCount(); i++) {
                if (Objects.equals(bankCombo.getItemAt(i), currentBank)) {
                    bankExists = true;
                    bankCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            if (!bankExists && bankCombo.getItemCount() > 0) {
                // Current bank not found, select first available
                Integer defaultBank = (Integer) bankCombo.getItemAt(0);
                getPlayer().getInstrument().setBankIndex(defaultBank);
                logger.debug("Bank {} not found, setting default bank: {}", currentBank, defaultBank);
                bankCombo.setSelectedIndex(0);
            }
        } else if (bankCombo.getItemCount() > 0) {
            // No bank set, select the first one
            Integer defaultBank = (Integer) bankCombo.getItemAt(0);
            getPlayer().getInstrument().setBankIndex(defaultBank);
            logger.debug("Setting default bank: {}", defaultBank);
            bankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Update preset combo with data from SoundbankManager
     */
    private void updatePresetCombo() {
        if (getPlayer() == null || getPlayer().getInstrument() == null)
            return;

        presetCombo.removeAllItems();

        // Get parameters
        String soundbank = getPlayer().getInstrument().getSoundbankName();
        Integer bank = getPlayer().getInstrument().getBankIndex();

        if ((soundbank == null || soundbank.isEmpty()) && soundbankCombo.getSelectedItem() != null) {
            soundbank = soundbankCombo.getSelectedItem().toString();
            getPlayer().getInstrument().setSoundbankName(soundbank);
            logger.debug("Updating instrument soundbank for presets: {}", soundbank);
        }

        if (bank == null && bankCombo.getSelectedItem() != null) {
            bank = (Integer) bankCombo.getSelectedItem();
            getPlayer().getInstrument().setBankIndex(bank);
            logger.debug("Updating instrument bank for presets: {}", bank);
        }

        if (soundbank != null && !soundbank.isEmpty() && bank != null) {
            // Let SoundbankManager provide the preset list
            List<String> presets = soundbankManager.getPresetNames(soundbank, bank);
            logger.debug("Found {} presets for soundbank: {}, bank: {}", presets.size(), soundbank, bank);
            
            for (int i = 0; i < presets.size(); i++) {
                String preset = presets.get(i);
                if (preset != null && !preset.isEmpty()) {
                    presetCombo.addItem(new PresetItem(i, preset));
                }
            }
        } else {
            logger.warn("Cannot load presets - soundbank: {}, bank: {}", soundbank, bank);
        }

        // Select current preset
        Integer currentPreset = getPlayer().getInstrument().getPreset();
        if (currentPreset != null) {
            boolean presetFound = false;
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == currentPreset) {
                    presetCombo.setSelectedIndex(i);
                    presetFound = true;
                    break;
                }
            }
            
            if (!presetFound && presetCombo.getItemCount() > 0) {
                // If preset not found, select first one
                PresetItem firstPreset = presetCombo.getItemAt(0);
                getPlayer().getInstrument().setPreset(firstPreset.getNumber());
                logger.debug("Preset {} not found, setting default: {}", currentPreset, firstPreset.getNumber());
                presetCombo.setSelectedIndex(0);
            }
        } else if (presetCombo.getItemCount() > 0) {
            // No preset set, select first one
            PresetItem firstPreset = presetCombo.getItemAt(0);
            getPlayer().getInstrument().setPreset(firstPreset.getNumber());
            logger.debug("Setting default preset: {}", firstPreset.getNumber());
            presetCombo.setSelectedIndex(0);
        }
        
        // If we still don't have a preset, there might be an issue with the SoundbankManager
        if (presetCombo.getItemCount() == 0) {
            logger.error("No presets available for soundbank: {}, bank: {}", soundbank, bank);
            // Add a placeholder item to indicate the problem
            presetCombo.addItem(new PresetItem(0, "[No Presets Available]"));
        }
    }

    /**
     * Update drum preset combo with data from SoundbankManager
     */
    private void updateDrumPresetCombo() {
        if (getPlayer() == null)
            return;

        presetCombo.removeAllItems();

        // Let SoundbankManager provide the drum items
        List<DrumItem> drums = soundbankManager.getDrumItems();
        for (DrumItem drum : drums) {
            presetCombo.addItem(new PresetItem(drum.getNoteNumber(), drum.getName()));
        }

        // Select current drum note
        if (getPlayer().getRootNote() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == getPlayer().getRootNote()) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }
        } else if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    /**
     * Update external preset combo with generic program numbers
     */
    private void updateExternalPresetCombo() {
        if (getPlayer() == null)
            return;

        presetCombo.removeAllItems();

        // Use standard GM preset list for external instruments
        List<String> presets = soundbankManager.getGeneralMIDIPresetNames();
        for (int i = 0; i < presets.size(); i++) {
            presetCombo.addItem(new PresetItem(i, presets.get(i)));
        }

        // Select current preset
        if (getPlayer().getInstrument() != null && getPlayer().getInstrument().getPreset() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == getPlayer().getInstrument().getPreset()) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }
        } else if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    // Layout components method remains largely unchanged
}

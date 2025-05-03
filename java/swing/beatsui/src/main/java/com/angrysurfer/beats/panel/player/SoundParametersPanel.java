package com.angrysurfer.beats.panel.player;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import com.angrysurfer.core.sequencer.DrumItem;
import com.angrysurfer.core.service.InternalSynthManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.PresetItem;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SoundbankManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Refactored panel for sound parameters with better manager use
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

    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();
    private final SoundbankManager soundbankManager = SoundbankManager.getInstance();

    /**
     * Constructor with improved manager delegation
     */
    public SoundParametersPanel() {
        super(new BorderLayout());

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
     * Layout the components in a horizontal arrangement
     */
    private void layoutComponents() {
        // Use BoxLayout for horizontal arrangement
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
        //contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        UIUtils.setPanelBorder(this);
        // Create sub-panels for each control group
        JPanel soundbankPanel = new JPanel(new BorderLayout(0, 3));
        JPanel bankPanel = new JPanel(new BorderLayout(0, 3));
        JPanel presetPanel = new JPanel(new BorderLayout(0, 3));
        
        // Create labels for the components
        soundbankLabel = new JLabel("Soundbank:");
        bankLabel = new JLabel("Bank:");
        presetLabel = new JLabel("Preset:");
        
        // Build soundbank section (label above combo)
        soundbankPanel.add(soundbankLabel, BorderLayout.WEST);
        soundbankPanel.add(soundbankCombo, BorderLayout.CENTER);
        
        // Build bank section (label above combo)
        bankPanel.add(bankLabel, BorderLayout.WEST);
        bankPanel.add(bankCombo, BorderLayout.CENTER);
        
        // Build preset section (label above combo)
        presetPanel.add(presetLabel, BorderLayout.WEST);
        presetPanel.add(presetCombo, BorderLayout.CENTER);
        
        // Add spacing between panels
        contentPanel.add(soundbankPanel);
        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(bankPanel);
        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(presetPanel);
        
        // Set consistent preferred sizes for better UI
        soundbankCombo.setPreferredSize(new Dimension(UIUtils.LARGE_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        bankCombo.setPreferredSize(new Dimension(UIUtils.MEDIUM_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        presetCombo.setPreferredSize(new Dimension(UIUtils.LARGE_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        
        // Button panel for the edit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(editButton);
        
        // Add panels to main layout
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    /**
     * Handle command events with improved manager delegation
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        // Only process relevant commands for efficiency
        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTED:
                if (action.getData() instanceof Player activatedPlayer) {
                    SwingUtilities.invokeLater(() -> {
                        player = activatedPlayer;
                        playerId = activatedPlayer.getId();
                        updateUI();
                    });
                }
                break;

            case Commands.PLAYER_UPDATED:
                if (action.getData() instanceof Player updatedPlayer &&
                        playerId != null && playerId.equals(updatedPlayer.getId())) {
                    SwingUtilities.invokeLater(() -> {
                        player = updatedPlayer;
                        updateUI();
                    });
                }
                break;

            case Commands.SOUNDBANKS_REFRESHED:
                if (player != null) {
                    SwingUtilities.invokeLater(this::updateSoundbankCombo);
                }
                break;
        }
    }

    /**
     * Initialize UI components with better manager delegation
     */
    private void initComponents() {
        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        presetCombo = new JComboBox<>();
        editButton = new JButton("Edit");
        editButton.setToolTipText("Edit Player Properties");

        // Add listeners with improved manager delegation
        soundbankCombo.addActionListener(e -> {
            if (!isInitializing && soundbankCombo.getSelectedItem() != null) {
                String soundbank = (String) soundbankCombo.getSelectedItem();
                if (player != null && player.getInstrument() != null) {
                    // Let SoundbankManager handle the change
                    soundbankManager.applySoundbank(player.getInstrument(), soundbank);

                    // Update UI for the change
                    updateBankCombo();

                    // Notify system of player update
                    playerManager.savePlayerProperties(player);
                    commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                }
            }
        });

        bankCombo.addActionListener(e -> {
            if (!isInitializing && bankCombo.getSelectedItem() != null) {
                Integer bank = (Integer) bankCombo.getSelectedItem();
                if (player != null && player.getInstrument() != null) {
                    // Update instrument with bank
                    player.getInstrument().setBankIndex(bank);

                    // Let SoundbankManager apply the change
                    soundbankManager.applyPresetChange(
                            player.getInstrument(),
                            bank,
                            player.getInstrument().getPreset());

                    // Update UI for the change
                    updatePresetCombo();

                    // Notify system of player update
                    playerManager.savePlayerProperties(player);
                    commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                }
            }
        });

        presetCombo.addActionListener(e -> {
            if (!isInitializing && presetCombo.getSelectedItem() != null) {
                PresetItem preset = (PresetItem) presetCombo.getSelectedItem();
                if (player != null) {
                    // Delegate to PlayerManager through command bus
                    commandBus.publish(
                            Commands.PLAYER_PRESET_CHANGE_REQUEST,
                            this,
                            new Object[] { player.getId(), preset.getNumber() });
                }
            }
        });

        editButton.addActionListener(e -> {
            if (player != null) {
                commandBus.publish(Commands.PLAYER_EDIT_REQUEST, this, player);
            }
        });
    }

    /**
     * Update UI from player data with improved manager integration
     */
    @Override
    public void updateUI() {
        super.updateUI();

        if (soundbankCombo == null || player == null)
            return;

        isInitializing = true;
        try {
            UIUtils.setWidgetPanelBorder(this,
                    player.getName() + " [" + player.getId().toString() + "]");

            boolean isDrumChannel = player.getChannel() == 9;
            boolean isInternalSynth = player.getInstrument() != null &&
                    InternalSynthManager.getInstance().isInternalSynthInstrument(player.getInstrument());

            // Update UI according to instrument type
            updateControlVisibility(isInternalSynth, isDrumChannel);

            if (isDrumChannel) {
                // Let SoundbankManager handle populating drum sounds
                updateDrumPresetCombo();
            } else if (isInternalSynth) {
                // Update all components for internal synth
                updateSoundbankCombo();
                updateBankCombo();
                updatePresetCombo();
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
            soundbankCombo.setVisible(false);
            bankCombo.setVisible(false);
            soundbankLabel.setVisible(false);
            bankLabel.setVisible(false);
            presetLabel.setText("Drum:");
        } else {
            // For melodic instruments, show/hide based on internal/external
            presetCombo.setVisible(true);
            presetLabel.setVisible(true);
            presetLabel.setText("Preset:");

            // Only show soundbank/bank for internal instruments
            soundbankCombo.setVisible(isInternalSynth);
            bankCombo.setVisible(isInternalSynth);
            soundbankLabel.setVisible(isInternalSynth);
            bankLabel.setVisible(isInternalSynth);
        }

        // Update the panel layout
        revalidate();
        repaint();
    }

    /**
     * Update soundbank combo with data from SoundbankManager
     */
    private void updateSoundbankCombo() {
        if (player == null || player.getInstrument() == null)
            return;

        soundbankCombo.removeAllItems();

        // Let SoundbankManager provide the soundbank list
        List<String> soundbanks = soundbankManager.getSoundbankNames();
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }

        // Select current soundbank
        String currentSoundbank = player.getInstrument().getSoundbankName();
        if (currentSoundbank != null) {
            soundbankCombo.setSelectedItem(currentSoundbank);
        } else if (soundbankCombo.getItemCount() > 0) {
            soundbankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Update bank combo with data from SoundbankManager
     */
    private void updateBankCombo() {
        if (player == null || player.getInstrument() == null)
            return;

        bankCombo.removeAllItems();

        // Get banks from SoundbankManager for current soundbank
        String soundbank = player.getInstrument().getSoundbankName();
        if (soundbank == null && soundbankCombo.getSelectedItem() != null) {
            soundbank = soundbankCombo.getSelectedItem().toString();
        }

        if (soundbank != null) {
            // Let SoundbankManager provide the bank list
            List<Integer> banks = soundbankManager.getAvailableBanksByName(soundbank);
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }
        }

        // Select current bank
        Integer currentBank = player.getInstrument().getBankIndex();
        if (currentBank != null) {
            bankCombo.setSelectedItem(currentBank);
        } else if (bankCombo.getItemCount() > 0) {
            bankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Update preset combo with data from SoundbankManager
     */
    private void updatePresetCombo() {
        if (player == null || player.getInstrument() == null)
            return;

        presetCombo.removeAllItems();

        // Get parameters
        String soundbank = player.getInstrument().getSoundbankName();
        Integer bank = player.getInstrument().getBankIndex();

        if (soundbank == null && soundbankCombo.getSelectedItem() != null) {
            soundbank = soundbankCombo.getSelectedItem().toString();
        }

        if (bank == null && bankCombo.getSelectedItem() != null) {
            bank = (Integer) bankCombo.getSelectedItem();
        }

        if (soundbank != null && bank != null) {
            // Let SoundbankManager provide the preset list
            List<String> presets = soundbankManager.getPresetNames(soundbank, bank);
            for (int i = 0; i < presets.size(); i++) {
                String preset = presets.get(i);
                if (preset != null && !preset.isEmpty()) {
                    presetCombo.addItem(new PresetItem(i, preset));
                }
            }
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
        }
    }

    /**
     * Update drum preset combo with data from SoundbankManager
     */
    private void updateDrumPresetCombo() {
        if (player == null)
            return;

        presetCombo.removeAllItems();

        // Let SoundbankManager provide the drum items
        List<DrumItem> drums = soundbankManager.getDrumItems();
        for (DrumItem drum : drums) {
            presetCombo.addItem(new PresetItem(drum.getNoteNumber(), drum.getName()));
        }

        // Select current drum note
        if (player.getRootNote() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == player.getRootNote()) {
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
        if (player == null)
            return;

        presetCombo.removeAllItems();

        // Use standard GM preset list for external instruments
        List<String> presets = soundbankManager.getGeneralMIDIPresetNames();
        for (int i = 0; i < presets.size(); i++) {
            presetCombo.addItem(new PresetItem(i, presets.get(i)));
        }

        // Select current preset
        if (player.getInstrument() != null && player.getInstrument().getPreset() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == player.getInstrument().getPreset()) {
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

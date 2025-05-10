package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.PlayerAwarePanel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.core.model.PresetItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.SoundbankManager;
import com.angrysurfer.core.model.BankItem;
import com.angrysurfer.core.model.SoundbankItem;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * Panel for editing player sound parameters
 */
public class SoundParametersPanel extends PlayerAwarePanel {
    private static final Logger logger = LoggerFactory.getLogger(SoundParametersPanel.class);

    // private JTextField nameTextField;
    private JComboBox<SoundbankItem> soundbankCombo;
    private JComboBox<BankItem> bankCombo;
    private JComboBox<PresetItem> presetCombo;
    //private JLabel titleLabel;

    private BankItem currentBank;
    private PresetItem currentPreset;

    // UI Components
    private JPanel soundbankPanel;
    private JPanel bankPanel;
    private JPanel presetPanel;

    /**
     * Constructor
     */
    public SoundParametersPanel() {
        initializeUI();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        //UIHelper.setWidgetPanelBorder(this, "Sound Parameters");

        JButton refreshButton = UIHelper.createPlayerRefreshButton(null, null);

        JPanel horizontalPanel = new JPanel();
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        horizontalPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        Player player = getPlayer();
        if (player != null) {
            // UIHelper.setWidgetPanelBorder(this, player.getName());
            requestPlayerUpdate();
        }
        // Create channel combo - display 1-16 but store 0-15
        ChannelComboPanel channelComboPanel = new ChannelComboPanel();
        channelComboPanel.setMaximumSize(new Dimension(channelComboPanel.getPreferredSize().width,
                channelComboPanel.getPreferredSize().height));
        
        // Soundbank panel
        soundbankPanel = new JPanel(new BorderLayout(5, 0));
        soundbankPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Soundbank"),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        soundbankCombo = new JComboBox<>();
        
        // 2. Restore soundbank action listener
        soundbankCombo.addActionListener(e -> {
            if (!isInitializing && soundbankCombo.getSelectedItem() != null) {
                Player currentPlayer = getPlayer();
                if (currentPlayer != null && currentPlayer.getInstrument() != null) {
                    SoundbankItem item = (SoundbankItem) soundbankCombo.getSelectedItem();
                    currentPlayer.getInstrument().setSoundbankName(item.getName());
                    
                    // Update bank and preset UI
                    updateBankCombo();
                    
                    requestPlayerUpdate();
                }
            }
        });

        soundbankPanel.add(soundbankCombo, BorderLayout.CENTER);
        soundbankPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, soundbankPanel.getPreferredSize().height));
        
        // Bank panel
        bankPanel = new JPanel(new BorderLayout(5, 0));
        bankPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Bank"),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        bankCombo = new JComboBox<>();
        
        // 2. Restore bank action listener
        bankCombo.addActionListener(e -> {
            if (!isInitializing && bankCombo.getSelectedItem() != null) {
                Player currentPlayer = getPlayer();
                if (currentPlayer != null && currentPlayer.getInstrument() != null) {
                    BankItem item = (BankItem) bankCombo.getSelectedItem();
                    currentPlayer.getInstrument().setBankIndex(item.getIndex());
                    currentBank = item;
                    
                    // Update preset UI based on new bank
                    updatePresetCombo();
                    
                    requestPlayerUpdate();
                }
            }
        });
        
        bankPanel.add(bankCombo, BorderLayout.CENTER);
        bankPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bankPanel.getPreferredSize().height));
        
        // Preset panel
        presetPanel = new JPanel(new BorderLayout(5, 0));
        presetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Preset"),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        presetCombo = new JComboBox<>();
        
        // 2. Restore preset action listener
        presetCombo.addActionListener(e -> {
            if (!isInitializing && presetCombo.getSelectedItem() != null) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                if (item != null && currentPreset != null && item.getNumber() == currentPreset.getNumber()) {
                    return; // No change
                }
                
                currentPreset = item;
                Player currentPlayer = getPlayer();
                
                if (currentPlayer != null && currentPlayer.getInstrument() != null) {
                    // Use SoundbankManager to update player sound
                    SoundbankItem sbItem = (SoundbankItem) soundbankCombo.getSelectedItem();
                    BankItem bankItem = (BankItem) bankCombo.getSelectedItem();
                    
                    String soundbankName = sbItem != null ? sbItem.getName() : null;
                    Integer bankIndex = bankItem != null ? bankItem.getIndex() : null;

                    // For drum channel, update root note
                    if (currentPlayer.getChannel() == 9) {
                        currentPlayer.setRootNote(item.getNumber());
                    }

                    SoundbankManager.getInstance().updatePlayerSound(
                        currentPlayer, soundbankName, bankIndex, item.getNumber());

                    // Request update
                    requestPlayerUpdate();
                }
            }
        });
        
        presetPanel.add(presetCombo, BorderLayout.CENTER);
        presetPanel.add(refreshButton, BorderLayout.EAST);
        presetPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, presetPanel.getPreferredSize().height));

        // 3. Create edit button panel
        JPanel editButtonPanel = new JPanel(new BorderLayout(5, 0));
        editButtonPanel.setBorder(
                BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Actions"),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        JButton editButton = new JButton(Symbols.get(Symbols.MIDI));
        editButton.setMaximumSize(new Dimension(UIHelper.CONTROL_HEIGHT, UIHelper.CONTROL_HEIGHT));
        editButton.setToolTipText("Edit player properties");
        editButton.addActionListener(e -> {
            Player currentPlayer = getPlayer();
            if (currentPlayer != null) {
                // Request player edit dialog
                CommandBus.getInstance().publish(
                    com.angrysurfer.core.api.Commands.PLAYER_EDIT_REQUEST, 
                    this, 
                    currentPlayer);
            }
        });
        
        editButtonPanel.add(editButton, BorderLayout.CENTER);
        editButtonPanel.setMaximumSize(new Dimension(80, editButtonPanel.getPreferredSize().height));

        // Add components to horizontal panel with spacing
        //horizontalPanel.add(instrumentPanel);
        horizontalPanel.add(channelComboPanel);

        InstrumentComboPanel instrumentComboPanel = new InstrumentComboPanel();
        instrumentComboPanel.setMaximumSize(new Dimension(instrumentComboPanel.getPreferredSize().width, instrumentComboPanel.getPreferredSize().height));

        horizontalPanel.add(instrumentComboPanel);
        //horizontalPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        horizontalPanel.add(soundbankPanel);
        //horizontalPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        horizontalPanel.add(bankPanel);
        //horizontalPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        horizontalPanel.add(presetPanel);
        //horizontalPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        horizontalPanel.add(editButtonPanel);

        // Add the horizontal panel to the CENTER of the main panel
        add(horizontalPanel, BorderLayout.CENTER);
    }

    /**
     * Update UI visibility based on instrument type
     */
    private void updateControlVisibility(boolean isInternalSynth, boolean isDrumChannel) {
        soundbankPanel.setVisible(isInternalSynth && !isDrumChannel);
        bankPanel.setVisible(isInternalSynth && !isDrumChannel);
        presetPanel.setVisible(true);
    }

    /**
     * Handle when a new player is activated
     */
    @Override
    public void handlePlayerActivated() {
        Player player = getPlayer();
        if (player == null)
            return;
        //UIHelper.setWidgetPanelBorder(this, getTargetPlayer().getName());
        // Update the title
        // titleLabel.setText(player.getName() + " Sound Parameters");

        // Update the refresh button with this player
        Component[] components = ((JPanel) getComponent(0)).getComponents();
        for (Component c : components) {
            if (c instanceof JButton) {
                JButton refreshButton = (JButton) c;
                refreshButton.removeActionListener(refreshButton.getActionListeners()[0]);
                refreshButton.addActionListener(e -> requestPlayerRefresh());
            }
        }

        // Update all UI fields
        updateUI();
    }

    /**
     * Handle when the player is updated
     */
    @Override
    public void handlePlayerUpdated() {
        // Just update the UI
        updateUI();
    }

    /**
     * Update UI from player data
     */
    @Override
    public void updateUI() {
        super.updateUI();

        Player player = getPlayer();
        if (player == null || soundbankCombo == null)
            return;

        isInitializing = true;
        try {
            // UIHelper.setWidgetPanelBorder(this, getTargetPlayer().getName());

            boolean isDrumChannel = player.getChannel() == 9;
            boolean isInternalSynth = player.getInstrument() != null &&
                    InternalSynthManager.getInstance().isInternalSynthInstrument(player.getInstrument());

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
     * Update for external synth presets
     */
    private void updateExternalPresetCombo() {
        // Hide soundbank and bank panels for external synth
        soundbankPanel.setVisible(false);
        bankPanel.setVisible(false);

        // Simply add generic presets 0-127
        presetCombo.removeAllItems();

        Player player = getPlayer();
        if (player == null)
            return;

        for (int i = 0; i < 128; i++) {
            presetCombo.addItem(new PresetItem(i, "Program " + i));
        }

        // Select current preset if possible
        if (player.getInstrument() != null && player.getInstrument().getPreset() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == player.getInstrument().getPreset()) {
                    presetCombo.setSelectedItem(item);
                    currentPreset = item;
                    break;
                }
            }
        }
    }

    /**
     * Update the soundbank combo box based on player's channel
     */
    private void updateSoundbankCombo() {
        isInitializing = true;
        try {
            Player player = getPlayer();
            if (player == null)
                return;

            soundbankCombo.removeAllItems();

            // Use new helper method
            List<SoundbankItem> soundbanks = SoundbankManager.getInstance().getPlayerSoundbanks(player);
            for (SoundbankItem item : soundbanks) {
                soundbankCombo.addItem(item);
            }

            // Select current soundbank if possible
            if (player.getInstrument() != null && player.getInstrument().getSoundbankName() != null) {
                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    SoundbankItem item = soundbankCombo.getItemAt(i);
                    if (item.getName().equals(player.getInstrument().getSoundbankName())) {
                        soundbankCombo.setSelectedItem(item);
                        break;
                    }
                }
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Update the bank combo box based on selected soundbank
     */
    private void updateBankCombo() {
        isInitializing = true;
        try {
            bankCombo.removeAllItems();

            Player player = getPlayer();
            if (player == null)
                return;

            SoundbankItem soundbankItem = (SoundbankItem) soundbankCombo
                    .getSelectedItem();
            if (soundbankItem == null)
                return;

            // Use new helper method
            List<BankItem> banks = SoundbankManager.getInstance().getPlayerBanks(player, soundbankItem.getName());
            for (BankItem item : banks) {
                bankCombo.addItem(item);
            }

            // Select current bank if possible
            if (player.getInstrument() != null && player.getInstrument().getBankIndex() != null) {
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    BankItem item = bankCombo.getItemAt(i);
                    if (item.getIndex() == player.getInstrument().getBankIndex()) {
                        bankCombo.setSelectedItem(item);
                        currentBank = item;
                        break;
                    }
                }
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Update the preset combo box based on selected bank
     */
    private void updatePresetCombo() {
        isInitializing = true;
        try {
            presetCombo.removeAllItems();

            Player player = getPlayer();
            if (player == null)
                return;

            SoundbankItem soundbankItem = (SoundbankItem) soundbankCombo
                    .getSelectedItem();
            BankItem bankItem = (BankItem) bankCombo.getSelectedItem();

            String soundbankName = null;
            Integer bankIndex = null;

            if (soundbankItem != null) {
                soundbankName = soundbankItem.getName();
                currentBank = bankItem;
            }

            if (bankItem != null) {
                bankIndex = bankItem.getIndex();
            }

            // Use new helper method
            List<PresetItem> presets = SoundbankManager.getInstance().getPlayerPresets(
                    player, soundbankName, bankIndex);

            for (PresetItem item : presets) {
                presetCombo.addItem(item);
            }

            // Select current preset if possible
            if (player.getInstrument() != null && player.getInstrument().getPreset() != null) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item.getNumber() == player.getInstrument().getPreset()) {
                        presetCombo.setSelectedItem(item);
                        currentPreset = item;
                        break;
                    }
                }
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Update the display for a drum channel player
     */
    private void updateDrumPresetCombo() {
        isInitializing = true;
        try {
            presetCombo.removeAllItems();

            Player player = getPlayer();
            if (player == null)
                return;

            // Hide soundbank and bank panels for drums
            soundbankPanel.setVisible(false);
            bankPanel.setVisible(false);

            // Use new helper method
            List<PresetItem> drumPresets = SoundbankManager.getInstance().getDrumPresets();
            for (PresetItem item : drumPresets) {
                presetCombo.addItem(item);
            }


            // Select current preset if possible
            if (player.getInstrument() != null) {
                Integer presetNumber = player.getRootNote(); // player.getInstrument().getPreset();
                if (presetNumber == null && player.getRootNote() != null) {
                    presetNumber = player.getRootNote();
                }

                if (presetNumber != null) {
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        PresetItem item = presetCombo.getItemAt(i);
                        logger.info(item.toString());
                        if (item.getNumber() == presetNumber) {
                            presetCombo.setSelectedItem(item);
                            currentPreset = item;
                            break;
                        }
                    }
                }
            }
        } finally {
            isInitializing = false;
        }
    }

}

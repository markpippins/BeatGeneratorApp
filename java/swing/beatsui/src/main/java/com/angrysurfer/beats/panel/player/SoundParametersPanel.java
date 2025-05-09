package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.panel.PlayerAwarePanel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

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

    private JTextField nameTextField;
    private JComboBox<SoundbankItem> soundbankCombo;
    private JComboBox<BankItem> bankCombo;
    private JComboBox<PresetItem> presetCombo;
    private JLabel titleLabel;

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
        
        // Title panel at the top
        JPanel titlePanel = new JPanel(new BorderLayout(5, 0));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Add title if needed
        titleLabel = new JLabel("Sound Parameters", JLabel.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = UIHelper.createPlayerRefreshButton(null, null);
        titlePanel.add(refreshButton, BorderLayout.EAST);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Create a horizontal panel to hold all controls
        JPanel horizontalPanel = new JPanel();
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        horizontalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Player name field
        JPanel namePanel = new JPanel(new BorderLayout(5, 0));
        namePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Player Name"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        nameTextField = new JTextField(10);
        nameTextField.addActionListener(e -> {
            Player player = getPlayer();
            if (player != null) {
                player.setName(nameTextField.getText());
                requestPlayerUpdate();
            }
        });
        namePanel.add(nameTextField, BorderLayout.CENTER);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, namePanel.getPreferredSize().height));
        
        // Soundbank panel
        soundbankPanel = new JPanel(new BorderLayout(5, 0));
        soundbankPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Soundbank"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        soundbankCombo = new JComboBox<>();
        //soundbankCombo.addActionListener(/* your existing listener */);
        soundbankPanel.add(soundbankCombo, BorderLayout.CENTER);
        soundbankPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, soundbankPanel.getPreferredSize().height));
        
        // Bank panel
        bankPanel = new JPanel(new BorderLayout(5, 0));
        bankPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Bank"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        bankCombo = new JComboBox<>();
        //bankCombo.addActionListener(/* your existing listener */);
        bankPanel.add(bankCombo, BorderLayout.CENTER);
        bankPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bankPanel.getPreferredSize().height));
        
        // Preset panel
        presetPanel = new JPanel(new BorderLayout(5, 0));
        presetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Preset"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        presetCombo = new JComboBox<>();
        // presetCombo.addActionListener(/* your existing listener */);
        presetPanel.add(presetCombo, BorderLayout.CENTER);
        presetPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, presetPanel.getPreferredSize().height));
        
        // Add components to horizontal panel with spacing
        horizontalPanel.add(namePanel);
        horizontalPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        horizontalPanel.add(soundbankPanel);
        horizontalPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        horizontalPanel.add(bankPanel);
        horizontalPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        horizontalPanel.add(presetPanel);
        
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

        // Update the title
        titleLabel.setText(player.getName() + " Sound Parameters");

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
            nameTextField.setText(player.getName());

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
                Integer presetNumber = player.getInstrument().getPreset();
                if (presetNumber == null && player.getRootNote() != null) {
                    presetNumber = player.getRootNote();
                }

                if (presetNumber != null) {
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        PresetItem item = presetCombo.getItemAt(i);
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

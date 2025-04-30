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
    private JComboBox<InstrumentWrapper> instrumentCombo;
    private JButton editButton;
    private JLabel playerNameLabel;

    // State management
    private boolean isInitializing = false;

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
            case Commands.PLAYER_INSTRUMENT_CHANGED -> handlePlayerInstrumentChanged(action);
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
            SwingUtilities.invokeLater(() -> {
                // Check if this panel should update to the activated player
                // This depends on your application's UI flow
                // Here I'm assuming SoundParametersPanel is always showing the active player
                player = activatedPlayer;
                playerId = activatedPlayer.getId();
                updateUI();
            });
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
     * Handle instrument change events
     */
    private void handlePlayerInstrumentChanged(Command action) {
        if (action.getData() instanceof Object[] data &&
                data.length >= 2 &&
                data[0] instanceof Long id &&
                playerId.equals(id)) {

            Long instrumentId = (Long) data[1];

            SwingUtilities.invokeLater(() -> {
                // Update UI to reflect new instrument
                player = playerManager.getPlayerById(playerId);
                updateUI();
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
        instrumentCombo = new JComboBox<>();
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

        instrumentCombo.addActionListener(e -> {
            if (!isInitializing && instrumentCombo.getSelectedItem() != null) {
                handleInstrumentChange((InstrumentWrapper) instrumentCombo.getSelectedItem());
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

        // 2. Instrument combo
        JPanel instrumentPanel = new JPanel(new BorderLayout(5, 0));
        instrumentPanel.add(new JLabel("Instrument:"), BorderLayout.WEST);
        instrumentPanel.add(instrumentCombo, BorderLayout.CENTER);
        instrumentCombo.setPreferredSize(new Dimension(180, instrumentCombo.getPreferredSize().height));
        instrumentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InstrumentWrapper) {
                    InstrumentWrapper inst = (InstrumentWrapper) value;
                    setText(inst.getName());
                    setToolTipText(inst.getName() + " (" + (inst.getDeviceName() != null ? inst.getDeviceName() : "N/A")
                            + ")");
                }
                return c;
            }
        });
        mainPanel.add(instrumentPanel);

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
                playerNameLabel.setText(player != null ? player.getName() : "");
            }

            // Clear existing items
            instrumentCombo.removeAllItems();
            soundbankCombo.removeAllItems();
            bankCombo.removeAllItems();
            presetCombo.removeAllItems();

            // If no player, disable controls
            if (player == null || player.getInstrument() == null) {
                setControlsEnabled(false);
                return;
            }

            // Enable controls
            setControlsEnabled(true);

            // Populate instrument combo
            populateInstrumentCombo();

            // Check if current instrument is internal synth
            boolean isInternal = isInternalSynth(player.getInstrument());

            // Show/hide soundbank controls based on instrument type
            soundbankCombo.setVisible(isInternal);

            // Populate soundbank combo if applicable
            if (isInternal) {
                populateSoundbankCombo();
            }

            // IMPORTANT: Explicitly call update methods since action listeners are
            // suppressed
            if (isInternal && soundbankCombo.getSelectedItem() != null) {
                String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
                updateBankCombo(selectedSoundbank);

                // Get selected bank after banks are populated
                if (bankCombo.getSelectedItem() != null) {
                    Integer selectedBank = (Integer) bankCombo.getSelectedItem();
                    updatePresetCombo(selectedSoundbank, selectedBank);
                }
            }

        } finally {
            isInitializing = false;
        }
    }

    private void setControlsEnabled(boolean enabled) {
        instrumentCombo.setEnabled(enabled);
        soundbankCombo.setEnabled(enabled);
        bankCombo.setEnabled(enabled);
        presetCombo.setEnabled(enabled);
        editButton.setEnabled(enabled);
    }

    /**
     * Populate instrument combo
     */
    private void populateInstrumentCombo() {
        if (player == null)
            return;

        // Get all instruments
        List<InstrumentWrapper> instruments = instrumentManager.getCachedInstruments();

        // Sort by name
        instruments.sort((a, b) -> {
            String nameA = a.getName() != null ? a.getName() : "";
            String nameB = b.getName() != null ? b.getName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });

        // Add to combo
        for (InstrumentWrapper instrument : instruments) {
            if (instrument.getAvailable()) {
                instrumentCombo.addItem(instrument);
            }
        }

        // Select current instrument
        if (player.getInstrument() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (item.getId().equals(player.getInstrument().getId())) {
                    instrumentCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
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
            Integer currentPreset = player.getInstrument().getCurrentPreset();
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
                player.getInstrument().setCurrentPreset(item.getNumber());
                player.setPreset(item.getNumber());
            }
        } finally {
            isInitializing = false;
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
        if (player == null)
            return;

        try {
            // Request preset change through command bus
            commandBus.publish(Commands.PLAYER_PRESET_CHANGE_REQUEST, this,
                    new Object[] { player.getId(), presetNumber });
        } catch (Exception e) {
            logger.error("Error handling preset change: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle instrument selection change
     */
    private void handleInstrumentChange(InstrumentWrapper instrument) {
        if (player == null)
            return;

        try {
            // Check if this is an internal synth instrument
            boolean isInternalSynth = isInternalSynth(instrument);

            // Show/hide soundbank controls based on instrument type
            soundbankCombo.setVisible(isInternalSynth);

            // Request instrument change through command bus
            commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST, this,
                    new Object[] { player.getId(), instrument });

            // For internal instruments, populate soundbank combo
            if (isInternalSynth) {
                populateSoundbankCombo();
            }

            logger.info("Instrument changed to: {} (Internal: {})",
                    instrument.getName(), isInternalSynth);
        } catch (Exception e) {
            logger.error("Error handling instrument change: {}", e.getMessage(), e);
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
                    deviceName.contains("wavetable") ||
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
            // Update instrument combo
            if (instrumentCombo != null && player.getInstrument() != null) {
                // Find and select the matching instrument
                for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                    InstrumentWrapper instrument = instrumentCombo.getItemAt(i);
                    if (instrument != null &&
                            instrument.getId() != null &&
                            instrument.getId().equals(player.getInstrument().getId())) {
                        instrumentCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

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
                    player.getInstrument().getCurrentPreset() != null) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    PresetItem item = presetCombo.getItemAt(i);
                    if (item != null && item.getNumber() == player.getInstrument().getCurrentPreset()) {
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

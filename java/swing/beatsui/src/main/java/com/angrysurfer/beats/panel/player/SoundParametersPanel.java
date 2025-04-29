package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
    private JComboBox<Integer> channelCombo;
    private JButton editButton;
    
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
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        try {
            switch (action.getCommand()) {
                case Commands.PLAYER_SELECTED:
                case Commands.PLAYER_ACTIVATED:
                    if (action.getData() instanceof Player) {
                        Player selectedPlayer = (Player) action.getData();
                        updateFromPlayer(selectedPlayer);
                    }
                    break;
                
                // Other cases...
            }
        } catch (Exception e) {
            logger.error("Error handling action in SoundParametersPanel: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update this panel from a player object
     * @param player The player to use for updating
     */
    public void updateFromPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        // Store reference to the player
        this.player = player;
        
        // Temporarily disable component listeners to prevent feedback loops
        boolean wasInitializing = isInitializing;
        isInitializing = true;
        
        try {
            // Update instrument selection
            if (instrumentCombo != null && player.getInstrument() != null) {
                InstrumentWrapper playerInst = player.getInstrument();
                
                // Find and select matching instrument
                for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                    InstrumentWrapper item = instrumentCombo.getItemAt(i);
                    if (item != null && item.getId().equals(playerInst.getId())) {
                        instrumentCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            
            // Update bank and preset selections
            if (player.getInstrument() != null) {
                InstrumentWrapper instrument = player.getInstrument();
                
                // Set soundbank selection
                if (soundbankCombo != null && instrument.getSoundbankName() != null) {
                    soundbankCombo.setSelectedItem(instrument.getSoundbankName());
                }
                
                // Set bank selection 
                if (bankCombo != null && instrument.getBankIndex() != null) {
                    bankCombo.setSelectedItem(instrument.getBankIndex());
                }
                
                // Set preset selection
                updatePresetList(instrument.getBankIndex());
                if (presetCombo != null && instrument.getCurrentPreset() != null) {
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        PresetItem presetItem = presetCombo.getItemAt(i);
                        if (presetItem.getNumber() == instrument.getCurrentPreset()) {
                            presetCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                
                // Update channel selection if needed
                if (channelCombo != null && player.getChannel() != null) {
                    channelCombo.setSelectedItem(player.getChannel());
                }
            }
            
            logger.debug("Updated SoundParametersPanel from player: {} (ID: {})", 
                player.getName(), player.getId());
            
        } finally {
            // Re-enable component listeners
            isInitializing = wasInitializing;
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
        channelCombo = new JComboBox<>();
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
    }
    
    /**
     * Layout components in a horizontal arrangement
     */
    private void layoutComponents() {
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Sound"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Instrument combo
        JPanel instrumentPanel = new JPanel(new BorderLayout(5, 0));
        instrumentPanel.add(new JLabel("Instrument:"), BorderLayout.WEST);
        instrumentPanel.add(instrumentCombo, BorderLayout.CENTER);
        instrumentCombo.setPreferredSize(new Dimension(180, instrumentCombo.getPreferredSize().height));
        mainPanel.add(instrumentPanel);
        
        // Bank combo
        JPanel bankPanel = new JPanel(new BorderLayout(5, 0));
        bankPanel.add(new JLabel("Bank:"), BorderLayout.WEST);
        bankPanel.add(bankCombo, BorderLayout.CENTER);
        bankCombo.setPreferredSize(new Dimension(80, bankCombo.getPreferredSize().height));
        mainPanel.add(bankPanel);
        
        // Preset combo
        JPanel presetPanel = new JPanel(new BorderLayout(5, 0));
        presetPanel.add(new JLabel("Preset:"), BorderLayout.WEST);
        presetPanel.add(presetCombo, BorderLayout.CENTER);
        presetCombo.setPreferredSize(new Dimension(180, presetCombo.getPreferredSize().height));
        mainPanel.add(presetPanel);
        
        // Edit button
        mainPanel.add(editButton);
        
        // Add everything to this panel
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Update UI from player data
     */
    @Override
    public void updateUI() {
        super.updateUI();
        
        if (soundbankCombo == null) return;
        
        isInitializing = true;
        try {
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
            
            // Populate soundbank combo
            populateSoundbankCombo();
            
            // Bank and preset combos are populated by cascade from soundbank selection
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
        if (player == null) return;
        
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
        if (player == null || player.getInstrument() == null) return;
        
        // Get soundbanks from internal synth manager
        List<String> soundbanks = InternalSynthManager.getInstance().getSoundbankNames();
        
        // Add to combo
        for (String soundbank : soundbanks) {
            soundbankCombo.addItem(soundbank);
        }
        
        // Select current soundbank
        if (player.getInstrument().getSoundbankName() != null) {
            soundbankCombo.setSelectedItem(player.getInstrument().getSoundbankName());
            
            // This will trigger population of banks and presets through action listener
        } else if (soundbankCombo.getItemCount() > 0) {
            soundbankCombo.setSelectedIndex(0);
        }
    }
    
    /**
     * Populate bank combo based on selected soundbank
     */
    private void updateBankCombo(String selectedSoundbank) {
        if (player == null || player.getInstrument() == null) return;
        
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
        if (player == null || player.getInstrument() == null) return;
        
        isInitializing = true;
        try {
            // Clear existing items
            presetCombo.removeAllItems();
            
            // Get presets for selected soundbank and bank
            List<String> presets = InternalSynthManager.getInstance()
                .getPresetNames(soundbank, bank);
            
            // Add to combo as PresetItems
            for (int i = 0; i < Math.min(128, presets.size()); i++) {
                String name = presets.get(i);
                if (name == null || name.isEmpty()) {
                    name = "Program " + i;
                }
                presetCombo.addItem(new PresetItem(i, i + ": " + name));
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
     * Convenience method to update preset list with just the bank index
     * @param bankIndex The bank index to use
     */
    private void updatePresetList(Integer bankIndex) {
        if (bankIndex == null || soundbankCombo == null || 
            soundbankCombo.getSelectedItem() == null) {
            return;
        }
        
        String selectedSoundbank = (String) soundbankCombo.getSelectedItem();
        updatePresetCombo(selectedSoundbank, bankIndex);
    }
    
    /**
     * Handle soundbank selection change
     */
    private void handleSoundbankChange(String soundbank) {
        if (player == null || player.getInstrument() == null) return;
        
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
        if (player == null || player.getInstrument() == null) return;
        
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
            // Request preset change through command bus
            commandBus.publish(Commands.PLAYER_PRESET_CHANGE_REQUEST, this, 
                new Object[]{player.getId(), presetNumber});
        } catch (Exception e) {
            logger.error("Error handling preset change: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handle instrument selection change
     */
    private void handleInstrumentChange(InstrumentWrapper instrument) {
        if (player == null) return;
        
        try {
            // Request instrument change through command bus
            commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST, this, 
                new Object[]{player.getId(), instrument});
        } catch (Exception e) {
            logger.error("Error handling instrument change: {}", e.getMessage(), e);
        }
    }
}

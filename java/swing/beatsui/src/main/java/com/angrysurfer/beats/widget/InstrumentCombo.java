package com.angrysurfer.beats.widget;

import java.awt.Component;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InstrumentManager;

import lombok.Getter;
import lombok.Setter;

/**
 * A specialized combo box for selecting instruments that handles its own
 * interactions with the command bus and updates when players change.
 */
@Getter
@Setter
public class InstrumentCombo extends JComboBox<InstrumentWrapper> implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentCombo.class);
    private final CommandBus commandBus = CommandBus.getInstance();
    private Player currentPlayer;
    private boolean isInitializing = false;
    
    /**
     * Create a new InstrumentCombo that listens for player changes
     */
    public InstrumentCombo() {
        super();
        configureRenderer();
        commandBus.register(this);
        
        // Add action listener to handle selection changes
        addActionListener(e -> {
            if (isInitializing) return;
            handleSelectionChange();
        });
    }
    
    /**
     * Configure the cell renderer for instruments
     */
    private void configureRenderer() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                
                if (value instanceof InstrumentWrapper) {
                    InstrumentWrapper instrument = (InstrumentWrapper) value;
                    setText(instrument.getName());
                    setToolTipText(instrument.getName() + " (" + 
                            (instrument.getDeviceName() != null ? 
                             instrument.getDeviceName() : "N/A") + ")");
                }
                
                return c;
            }
        });
    }
    
    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTED:
                if (action.getData() instanceof Player player) {
                    setCurrentPlayer(player);
                }
                break;
                
            case Commands.PLAYER_UPDATED:
                if (action.getData() instanceof Player player && 
                        currentPlayer != null && 
                        player.getId().equals(currentPlayer.getId())) {
                    updateSelectedInstrument(player);
                }
                break;
                
            case Commands.INSTRUMENTS_REFRESHED:
                // Reload instruments if we have a current player
                if (currentPlayer != null) {
                    populateInstruments();
                }
                break;
        }
    }
    
    /**
     * Set the current player and update the combo accordingly
     */
    public void setCurrentPlayer(Player player) {
        if (player == null) return;
        
        currentPlayer = player;
        SwingUtilities.invokeLater(this::populateInstruments);
    }
    
    /**
     * Update selected instrument to match the player
     */
    private void updateSelectedInstrument(Player player) {
        if (player == null || player.getInstrument() == null) return;
        
        isInitializing = true;
        try {
            currentPlayer = player;
            
            // Find and select the matching instrument
            for (int i = 0; i < getItemCount(); i++) {
                InstrumentWrapper instrument = getItemAt(i);
                if (instrument.getId().equals(player.getInstrument().getId())) {
                    setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            isInitializing = false;
        }
    }
    
    /**
     * Populate the combo with all available instruments
     */
    public void populateInstruments() {
        isInitializing = true;
        try {
            removeAllItems();
            
            // Get all instruments 
            List<InstrumentWrapper> instruments = 
                InstrumentManager.getInstance().getCachedInstruments();
            
            // Sort alphabetically
            Collections.sort(instruments, 
                Comparator.comparing(InstrumentWrapper::getName));
            
            // Add to combo
            for (InstrumentWrapper instrument : instruments) {
                addItem(instrument);
            }
            
            // Select current player's instrument if available
            if (currentPlayer != null && currentPlayer.getInstrument() != null) {
                updateSelectedInstrument(currentPlayer);
            }
            
        } finally {
            isInitializing = false;
        }
    }
    
    /**
     * Handle instrument selection change
     */
    private void handleSelectionChange() {
        if (currentPlayer == null || isInitializing) return;
        
        InstrumentWrapper selectedInstrument = (InstrumentWrapper) getSelectedItem();
        if (selectedInstrument == null) return;
        
        // Publish instrument change request
        commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST, this,
                new Object[] { currentPlayer.getId(), selectedInstrument });
        
        logger.info("Instrument change requested for player {} to {}",
                currentPlayer.getName(), selectedInstrument.getName());
    }
}
package com.angrysurfer.beats.widget;

import java.awt.Component;
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
import com.angrysurfer.core.model.Player;

import lombok.Getter;
import lombok.Setter;

/**
 * A specialized combo box for selecting MIDI channels that
 * automatically handles player selection and command bus events
 */
@Getter
@Setter
public class ChannelCombo extends JComboBox<Integer> implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(ChannelCombo.class);
    private final CommandBus commandBus = CommandBus.getInstance();
    private Player currentPlayer;
    private boolean isInitializing = false;
    
    /**
     * Create a new ChannelCombo with all 16 MIDI channels
     */
    public ChannelCombo() {
        super();
        commandBus.register(this);
        
        // Populate channels 0-15
        for (int i = 0; i < 16; i++) {
            addItem(i);
        }
        
        configureRenderer();
        
        // Add action listener to handle channel selection changes
        addActionListener(e -> {
            if (isInitializing) return;
            handleChannelChange();
        });
    }
    
    /**
     * Configure the cell renderer to show drum channel differently
     */
    private void configureRenderer() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                        
                if (value instanceof Integer) {
                    int channel = (Integer) value;
                    if (channel == 9) {
                        setText(channel + " (Drums)");
                    } else {
                        setText(String.valueOf(channel));
                    }
                    
                    setToolTipText(channel == 9 ? 
                        "Channel 9 - Reserved for drum sounds" : 
                        "Channel " + channel);
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
                    updateSelectedChannel(player);
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
        SwingUtilities.invokeLater(() -> updateSelectedChannel(player));
    }
    
    /**
     * Update selected channel to match the player
     */
    private void updateSelectedChannel(Player player) {
        if (player == null || player.getChannel() == null) return;
        
        isInitializing = true;
        try {
            currentPlayer = player;
            setSelectedItem(player.getChannel());
            logger.debug("Updated channel combo to channel {} for player {}", 
                         player.getChannel(), player.getName());
        } finally {
            isInitializing = false;
        }
    }
    
    /**
     * Handle channel selection change
     */
    private void handleChannelChange() {
        if (currentPlayer == null) return;
        
        Integer selectedChannel = (Integer) getSelectedItem();
        if (selectedChannel == null) return;
        
        // Skip if no change
        if (selectedChannel.equals(currentPlayer.getChannel())) {
            return;
        }
        
        // Update player's channel
        currentPlayer.setChannel(selectedChannel);
        
        // Publish change to command bus
        commandBus.publish(Commands.PLAYER_CHANNEL_CHANGE_REQUEST, this, currentPlayer);
        
        logger.info("Channel change requested for player {} to {} (isDrum: {})",
                   currentPlayer.getName(), selectedChannel, selectedChannel == 9);
    }
}
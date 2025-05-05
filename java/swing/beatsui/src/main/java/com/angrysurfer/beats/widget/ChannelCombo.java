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
import com.angrysurfer.core.service.ChannelManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Refactored ChannelCombo with better manager integration
 */
@Getter
@Setter
public class ChannelCombo extends JComboBox<Integer> implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(ChannelCombo.class);
    private final CommandBus commandBus = CommandBus.getInstance();
    private final ChannelManager channelManager = ChannelManager.getInstance();
    
    private Player currentPlayer;
    private boolean isInitializing = false;
    
    /**
     * Create a new ChannelCombo with ChannelManager integration
     */
    public ChannelCombo() {
        super();
        commandBus.register(this);
        
        // Populate all 16 channels
        for (int i = 0; i < 16; i++) {
            addItem(i);
        }
        
        // Add highlight for channels already in use
        configureRenderer();
        
        // Add action listener to handle channel selection changes
        addActionListener(e -> {
            if (isInitializing) return;
            handleChannelChange();
        });
    }
    
    /**
     * Configure the cell renderer with ChannelManager integration
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
                    String text;
                    String tooltip;
                    
                    // Use ChannelManager to check channel state
                    boolean inUse = channelManager.isChannelInUse(channel);
                    
                    if (channel == 9) {
                        // Special case for drums
                        text = channel + " (Drums)";
                        tooltip = "Channel 9 - Reserved for drum sounds";
                    } else if (inUse && (currentPlayer == null || currentPlayer.getChannel() != channel)) {
                        // Channel in use by another player
                        text = channel + " (In Use)";
                        tooltip = "Channel " + channel + " - Already in use by another player";
                        
                        // Visual indicator for in-use channels
                        setForeground(java.awt.Color.GRAY);
                    } else {
                        // Normal channel
                        text = String.valueOf(channel);
                        tooltip = "Channel " + channel;
                        
                        // Reset color
                        setForeground(list.getForeground());
                    }
                    
                    setText(text);
                    setToolTipText(tooltip);
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
            case Commands.PLAYER_ACTIVATED:
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
                
            // Listen for channel manager changes
            case Commands.CHANNEL_ASSIGNMENT_CHANGED:
                // Refresh renderer to update visual indicators
                repaint();
                break;
        }
    }
    
    /**
     * Set the current player with ChannelManager integration
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
            
            // Refresh UI to update visual indicators
            repaint();
            
            logger.debug("Updated channel combo to channel {} for player {}", 
                         player.getChannel(), player.getName());
        } finally {
            isInitializing = false;
        }
    }
    
    /**
     * Handle channel selection change with ChannelManager integration
     */
    private void handleChannelChange() {
        if (currentPlayer == null) return;
        
        Integer selectedChannel = (Integer) getSelectedItem();
        if (selectedChannel == null) return;
        
        // Skip if no change
        if (selectedChannel.equals(currentPlayer.getChannel())) {
            return;
        }
        
        // Delegate to ChannelManager to handle channel allocation
        boolean canUseChannel = selectedChannel == 9 || // Always allow drum channel
                                !channelManager.isChannelInUse(selectedChannel) || 
                                channelManager.reserveChannel(selectedChannel);
        
        if (canUseChannel) {
            // Release previous channel
            if (currentPlayer.getChannel() != null && currentPlayer.getChannel() != 9) {
                channelManager.releaseChannel(currentPlayer.getChannel());
            }
            
            // Update player's channel
            currentPlayer.setChannel(selectedChannel);
            
            // Publish change to command bus
            commandBus.publish(Commands.PLAYER_CHANNEL_CHANGE_REQUEST, this, currentPlayer);
            
            logger.info("Channel change requested for player {} to {} (isDrum: {})",
                       currentPlayer.getName(), selectedChannel, selectedChannel == 9);
        } else {
            // Revert to previous selection
            logger.warn("Cannot assign channel {} to player {} - already in use",
                        selectedChannel, currentPlayer.getName());
            
            isInitializing = true;
            try {
                setSelectedItem(currentPlayer.getChannel());
            } finally {
                isInitializing = false;
            }
        }
    }
}
package com.angrysurfer.beats.panel.player;

import java.awt.*;
import javax.swing.*;

import com.angrysurfer.core.service.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;

/**
 * Panel for editing player properties using CommandBus pattern
 */
public class PlayerEditPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class);
    
    // Player reference - treated as transient, always get fresh from PlayerManager
    private Player player;
    private Long playerId;
    
    // UI Components
    private PlayerEditBasicPropertiesPanel basicPropertiesPanel;
    private PlayerEditDetailPanel detailPanel;
    private SoundParametersPanel soundParametersPanel;
    
    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();
    
    /**
     * Constructor
     */
    public PlayerEditPanel(Player player) {
        super(new BorderLayout());
        this.player = player;
        
        if (player != null) {
            this.playerId = player.getId();
        }
        
        initComponents();
        layoutComponents();
        registerForEvents();
    }
    
    /**
     * Register for command bus events
     */
    private void registerForEvents() {
        commandBus.register(this);
    }
    
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;
        
        // Only process events relevant to our player
        if (playerId == null) return;
        
        switch (action.getCommand()) {
            case Commands.PLAYER_UPDATED -> handlePlayerUpdated(action);
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
                updatePanels();
            });
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initComponents() {
        // Create panels with player reference
        basicPropertiesPanel = new PlayerEditBasicPropertiesPanel(player);
        detailPanel = new PlayerEditDetailPanel(player);
        soundParametersPanel = new SoundParametersPanel(player);
    }
    
    /**
     * Layout components
     */
    private void layoutComponents() {
        // Create main panel with all components
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Add basic properties at top
        mainPanel.add(basicPropertiesPanel, BorderLayout.NORTH);
        
        // Add sound parameters panel below basics
        mainPanel.add(soundParametersPanel, BorderLayout.CENTER);
        
        // Add detail panel at bottom
        mainPanel.add(detailPanel, BorderLayout.SOUTH);
        
        // Add to this panel
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Update all panels with latest player data
     */
    private void updatePanels() {
        if (player == null) return;
        
        // Update each panel with fresh player data
        basicPropertiesPanel.updateFromPlayer(player);
        detailPanel.updateFromPlayer(player);
        soundParametersPanel.updateFromPlayer(player);
    }
    
    /**
     * Returns the player with all current UI changes applied
     */
    public Player getUpdatedPlayer() {
        // Make sure we apply any pending changes from all panels
        applyAllChanges();
        
        // Get fresh player from manager to ensure we have latest data
        Player updatedPlayer = playerManager.getPlayerById(playerId);
        
        if (updatedPlayer != null && updatedPlayer.getInstrument() != null) {
            logger.debug("Returning player with instrument settings - Name: {}, Bank: {}, Preset: {}",
                updatedPlayer.getInstrument().getName(),
                updatedPlayer.getInstrument().getBankIndex(),
                updatedPlayer.getInstrument().getPreset());
        }
        
        return updatedPlayer;
    }
    
    /**
     * Apply all changes from UI components to player model
     */
    public void applyAllChanges() {
        // First apply changes in each panel
        basicPropertiesPanel.applyAllChanges();
        detailPanel.applyChanges();
        
        // Then request a player update through command bus
        commandBus.publish(Commands.PLAYER_UPDATE_REQUEST, this, player);
    }
    
    /**
     * Update from player
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) return;
        
        // Update our reference and ID
        this.player = newPlayer;
        this.playerId = newPlayer.getId();
        
        // Update all child panels
        updatePanels();
    }
}

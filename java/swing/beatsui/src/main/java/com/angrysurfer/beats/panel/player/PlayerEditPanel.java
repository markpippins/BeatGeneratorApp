package com.angrysurfer.beats.panel.player;

import java.awt.*;
import javax.swing.*;

import com.angrysurfer.beats.panel.PlayerAwarePanel;
import com.angrysurfer.core.Constants;
import com.angrysurfer.core.service.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel for editing player properties using the PlayerAwarePanel pattern
 */
public class PlayerEditPanel extends PlayerAwarePanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class);

    // UI Components
    private PlayerEditBasicPropertiesPanel basicPropertiesPanel;
    private PlayerEditDetailPanel detailPanel;

    // Services
    private final PlayerManager playerManager = PlayerManager.getInstance();

    // Tracking fields for state changes
    private boolean initialIsDrumPlayer = false;
    private InstrumentWrapper initialInstrument;
    private DrumSequencer owningSequencer = null;
    
    /**
     * Constructor
     */
    public PlayerEditPanel(Player player) {
        super();
        setTargetPlayer(player);
        initComponents();
        layoutComponents();
    }

    /**
     * Handle when a new player is activated for this panel
     */
    @Override
    public void handlePlayerActivated() {
        logger.info("Player activated in edit panel: {}", getPlayer() != null ? getPlayer().getName() : "null");
        
        // Cache initial state for comparison when saving
        Player player = getPlayer();
        if (player != null) {
            initialIsDrumPlayer = player.getChannel() == Constants.MIDI_DRUM_CHANNEL;
            initialInstrument = player.getInstrument();

            // Check if this is part of a drum sequencer
            if (player.getOwner() instanceof DrumSequencer) {
                owningSequencer = (DrumSequencer) player.getOwner();
            } else {
                owningSequencer = null;
            }
        }
        
        // Update all panels with the new player
        updatePanels();
    }

    /**
     * Handle when the panel's player is updated
     */
    @Override
    public void handlePlayerUpdated() {
        logger.info("Player updated in edit panel: {}", getPlayer() != null ? getPlayer().getName() : "null");
        updatePanels();
    }

    /**
     * Initialize UI components
     */
    private void initComponents() {
        // Create panels - will be updated when a player is activated
        basicPropertiesPanel = new PlayerEditBasicPropertiesPanel(getTargetPlayer());
        detailPanel = new PlayerEditDetailPanel(getTargetPlayer());
    }

    /**
     * Layout components
     */
    private void layoutComponents() {
        // Create main panel with all components
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Add basic properties at top
        mainPanel.add(basicPropertiesPanel, BorderLayout.NORTH);

        // Add detail panel at bottom
        mainPanel.add(detailPanel, BorderLayout.SOUTH);

        // Add to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Update all panels with latest player data
     */
    private void updatePanels() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }

        // Update each panel with fresh player data
        basicPropertiesPanel.updateFromPlayer(player);
        detailPanel.updateFromPlayer(player);
    }

    /**
     * Returns the player with all current UI changes applied
     */
    public Player getUpdatedPlayer() {
        // Make sure we apply any pending changes from all panels
        applyAllChanges();
        
        // Return the current player from PlayerAwarePanel
        return getPlayer();
    }

    /**
     * Apply all changes from UI components to player model
     */
    public void applyAllChanges() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        
        // Save the owner reference which might be lost during editing
        Object ownerReference = player.getOwner();
        
        // Apply changes from all sub-panels
        basicPropertiesPanel.applyChanges();
        detailPanel.applyChanges();
    
        // Restore owner reference
        player.setOwner(ownerReference);
        
        // Special handling for drum players
        handleDrumPlayerChanges();
    
        // Save through PlayerManager for consistency
        playerManager.savePlayerProperties(player);
        
        // Request player update to notify other components
        requestPlayerUpdate();
    }
    
    /**
     * Handle changes for drum players
     */
    private void handleDrumPlayerChanges() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }

        // Check if this is a drum player now
        boolean isDrumPlayer = player.getChannel() == Constants.MIDI_DRUM_CHANNEL;

        // Check if instrument changed
        boolean instrumentChanged = false;
        if (player.getInstrument() != null && initialInstrument != null) {
            instrumentChanged = !player.getInstrument().getId().equals(initialInstrument.getId());
        } else {
            instrumentChanged = player.getInstrument() != initialInstrument;
        }

        // If we're on drum channel, instrument changed, and part of a sequencer, prompt
        if (isDrumPlayer && instrumentChanged && owningSequencer != null) {
            int response = JOptionPane.showConfirmDialog(
                this,
                "Apply this instrument change to all drum pads in the sequencer?",
                "Update All Drum Pads",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {
                // Apply to all drum pads
                for (int i = 0; i < Constants.DRUM_PAD_COUNT; i++) {
                    Player drumPlayer = owningSequencer.getPlayers()[i];
                    if (drumPlayer != null && !drumPlayer.equals(player)) {
                        // Update instrument for this pad
                        drumPlayer.setInstrument(player.getInstrument());
                        drumPlayer.setInstrumentId(player.getInstrument().getId());

                        // Save changes
                        playerManager.savePlayerProperties(drumPlayer);
                    }
                }

                logger.info("Applied instrument {} to all drum pads in sequencer", 
                    player.getInstrument().getName());
            }
        }
    }
}

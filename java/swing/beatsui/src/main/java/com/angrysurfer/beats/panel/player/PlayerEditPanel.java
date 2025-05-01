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
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;

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
    // private SoundParametersPanel soundParametersPanel;

    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();

    // Add tracking fields to PlayerEditPanel
    private boolean initialIsDrumPlayer = false;
    private InstrumentWrapper initialInstrument;
    private DrumSequencer owningSequencer = null;

    /**
     * Constructor
     */
    public PlayerEditPanel(Player player) {
        super(new BorderLayout(5, 5));
        this.player = player;

        // Cache initial state
        if (player != null) {
            this.playerId = player.getId();
            initialIsDrumPlayer = player.getChannel() == 9;
            initialInstrument = player.getInstrument();

            // Check if this is part of a drum sequencer
            if (player.getOwner() instanceof DrumSequencer) {
                owningSequencer = (DrumSequencer) player.getOwner();
            }
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
        if (action == null || action.getCommand() == null)
            return;

        // Only process events relevant to our player
        if (playerId == null)
            return;

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
        // soundParametersPanel = new SoundParametersPanel(player);
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
        // mainPanel.add(soundParametersPanel, BorderLayout.CENTER);

        // Add detail panel at bottom
        mainPanel.add(detailPanel, BorderLayout.SOUTH);

        // Add to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Update all panels with latest player data
     */
    private void updatePanels() {
        if (player == null)
            return;

        // Update each panel with fresh player data
        basicPropertiesPanel.updateFromPlayer(player);
        detailPanel.updateFromPlayer(player);
        // soundParametersPanel.updateFromPlayer(player);
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
        // Apply changes from all sub-panels
        basicPropertiesPanel.applyChanges();
        detailPanel.applyChanges();

        // Special handling for drum players
        handleDrumPlayerChanges();

        // Then request a player update through command bus
        commandBus.publish(Commands.PLAYER_UPDATE_REQUEST, this, player);
    }

    /**
     * Handle changes for drum players
     */
    private void handleDrumPlayerChanges() {
        // Only process if we have a player
        if (player == null) return;

        // Check if this is a drum player now
        boolean isDrumPlayer = player.getChannel() == 9;

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
                for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                    Player drumPlayer = owningSequencer.getPlayers()[i];
                    if (drumPlayer != null && !drumPlayer.equals(player)) {
                        // Update instrument for this pad
                        drumPlayer.setInstrument(player.getInstrument());
                        drumPlayer.setInstrumentId(player.getInstrument().getId());

                        // Save changes
                        PlayerManager.getInstance().savePlayerProperties(drumPlayer);
                    }
                }

//                PlayerManager.getInstance().getActivePlayer().setInstrument(player.getInstrument());

                logger.info("Applied instrument {} to all drum pads in sequencer", 
                    player.getInstrument().getName());
            }
        }
    }

    /**
     * Update from player
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null)
            return;

        // Update our reference and ID
        this.player = newPlayer;
        this.playerId = newPlayer.getId();

        // Update all child panels
        updatePanels();
    }
}

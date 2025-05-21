package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerSelectionEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;

/**
 * Base class for panels that work with a specific player
 */
@Getter
@Setter
public abstract class LivePanel extends JPanel implements IBusListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LivePanel.class);
    // Flag to prevent recursive calls during initialization
    protected boolean isInitializing = false;
    // The player this panel is working with
    private Player player;

    /**
     * Constructor
     */
    public LivePanel() {
        super();
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_SELECTION_EVENT,
                Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_ACTIVATED,
                Commands.PLAYER_UPDATED
        });
    }

    public boolean hasPlayer() {
        return Objects.nonNull(player);
    }

    public boolean hasNoPlayer() {
        return Objects.isNull(player);
    }

    public boolean hasMelodicPlayer() {
        return Objects.nonNull(player) && (player.isMelodicPlayer() || player.getOwner() instanceof MelodicSequencer);
    }

    public boolean hasDrumPlayer() {
        return Objects.nonNull(player) && (player.isDrumPlayer() || player.getOwner() instanceof DrumSequencer);
    }

    public boolean hasMelodicSequencer() {
        return Objects.nonNull(player) && (Objects.nonNull(player.getOwner()) &&
                player.getOwner() instanceof MelodicSequencer);
    }

    public boolean hasDrumSequwncer() {
        return Objects.nonNull(player) && (Objects.nonNull(player.getOwner()) &&
                player.getOwner() instanceof DrumSequencer);
    }

    public MelodicSequencer getMelodicSequencer() {
        if (Objects.nonNull(player) && (Objects.nonNull(player.getOwner()) &&
                player.getOwner() instanceof MelodicSequencer))
            return (MelodicSequencer) player.getOwner();

        return null;
    }

    public DrumSequencer getDrumSequencer() {
        if (Objects.nonNull(player) && (Objects.nonNull(player.getOwner()) &&
                player.getOwner() instanceof DrumSequencer))
            return (DrumSequencer) player.getOwner();

        return null;
    }


    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        logger.debug(getClass().getSimpleName() + " received command: " + action.getCommand());

        try {
            switch (action.getCommand()) {

                // Handle new player selection event
                case Commands.PLAYER_SELECTION_EVENT -> {
                    if (action.getData() instanceof PlayerSelectionEvent event) {
                        handlePlayerSelectionEvent(event);
                    } else if (action.getData() instanceof Player player) {
                        handleLegacyPlayerActivated(player);
                    }
                }

                // Handle player update event
                case Commands.PLAYER_UPDATE_EVENT -> {
                    if (action.getData() instanceof PlayerUpdateEvent event) {
                        handlePlayerUpdateEvent(event);
                    }
                }

                // Legacy support for old events
                case Commands.PLAYER_ACTIVATED -> {
                    if (action.getData() instanceof Player player) {
                        handleLegacyPlayerActivated(player);
                    }
                }

                case Commands.PLAYER_UPDATED -> {
                    if (action.getData() instanceof Player player) {
                        handleLegacyPlayerUpdated(player);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle a player selection event
     */
    protected void handlePlayerSelectionEvent(PlayerSelectionEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        Player newPlayer = event.getPlayer();

        // Only process if this is a different player
        if (player == null || !player.getId().equals(newPlayer.getId())) {
            logger.debug("Player selected: {} (ID: {})", newPlayer.getName(), newPlayer.getId());
            player = newPlayer;
            handlePlayerActivated();
        }
//        CommandBus.getInstance().publish(Commands.STATUS_UPDATE, new StatusUpdate(player.getName() +
//                " activated."));
    }

    /**
     * Handle a player update event
     */
    protected void handlePlayerUpdateEvent(PlayerUpdateEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        Player updatedPlayer = event.getPlayer();

        // Only process if this is our target player
        if (player != null && player.getId().equals(updatedPlayer.getId())) {
            logger.debug("Target player updated: {} (ID: {})", updatedPlayer.getName(), updatedPlayer.getId());
            player = updatedPlayer;
            handlePlayerUpdated();
        }
    }

    /**
     * Legacy handler for PLAYER_ACTIVATED command
     */
    protected void handleLegacyPlayerActivated(Player player) {
        if (player == null) {
            return;
        }

        // Only process if this is a different player
        if (this.player == null || !this.player.getId().equals(player.getId())) {
            logger.debug("Legacy player activated: {} (ID: {})", player.getName(), player.getId());
            this.player = player;
            handlePlayerActivated();
        }
    }

    /**
     * Legacy handler for PLAYER_UPDATED command
     */
    protected void handleLegacyPlayerUpdated(Player player) {
        if (player == null) {
            return;
        }

        // Only process if this is our target player
        if (this.player != null && this.player.getId().equals(player.getId())) {
            logger.debug("Legacy player updated: {} (ID: {})", player.getName(), player.getId());
            this.player = player;
            handlePlayerUpdated();
        }
    }

    /**
     * Set the target player for this panel
     *
     * @param player The player to set
     */
    public void setPlayer(Player player) {
        if (player == null) {
            return;
        }

        boolean differentPlayer = this.player == null || !this.player.getId().equals(player.getId());

        this.player = player;

        if (differentPlayer) {
            handlePlayerActivated();
        } else {
            handlePlayerUpdated();
        }
    }

    /**
     * Request player refresh (force instrument preset application)
     */
    protected void requestPlayerRefresh() {
        if (player != null && player.getInstrument() != null) {
            CommandBus.getInstance().publish(
                    Commands.PLAYER_REFRESH_EVENT,
                    this,
                    new com.angrysurfer.core.event.PlayerRefreshEvent(this, player)
            );
        }
    }

    /**
     * Send a player update event
     */
    protected void requestPlayerUpdate() {
        if (player != null) {
            CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATE_EVENT,
                    this,
                    new com.angrysurfer.core.event.PlayerUpdateEvent(this, player)
            );
        }
    }

    /**
     * Called when a new player is activated for this panel
     */
    public abstract void handlePlayerActivated();

    /**
     * Called when the panel's player is updated
     */
    public abstract void handlePlayerUpdated();
}

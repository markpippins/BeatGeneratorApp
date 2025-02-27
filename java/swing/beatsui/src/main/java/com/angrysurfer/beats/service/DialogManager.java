package com.angrysurfer.beats.service;

import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.Frame;
import com.angrysurfer.beats.panel.ControlsPanel;
import com.angrysurfer.beats.panel.PlayerEditPanel;
import com.angrysurfer.beats.panel.RuleEditPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.TickerManager;

public class DialogManager implements CommandListener {

    private static final Logger logger = Logger.getLogger(DialogManager.class.getName());

    private static DialogManager instance;
    private final CommandBus commandBus = CommandBus.getInstance();
    private final RedisService redisService = RedisService.getInstance();
    private final Frame frame;

    public static DialogManager initialize(Frame frame) {
        if (instance == null) {
            instance = new DialogManager(frame);
        }
        return instance;
    }

    private DialogManager(Frame frame) {
        this.frame = frame;
        commandBus.register(this);
    }

    @Override
    public void onAction(Command action) {
        logger.info("DialogService received command: " + action.getCommand());
        switch (action.getCommand()) {
            case Commands.PLAYER_ADD_REQUEST -> handleAddPlayer();
            case Commands.PLAYER_EDIT_REQUEST -> handleEditPlayer((Player) action.getData());
            case Commands.RULE_ADD_REQUEST -> handleAddRule((Player) action.getData());
            case Commands.RULE_EDIT_REQUEST -> handleEditRule((Rule) action.getData());
            case Commands.EDIT_PLAYER_PARAMETERS -> handlePlayerParameters((Player) action.getData());
        }
    }

    private void handleAddPlayer() {
        logger.info("Starting handleAddPlayer");
        SwingUtilities.invokeLater(() -> {
            try {
                Ticker currentTicker = TickerManager.getInstance().getActiveTicker();
                logger.info(
                        String.format("Current ticker: %s", currentTicker != null ? currentTicker.getId() : "null"));

                if (currentTicker != null) {
                    // Initialize player
                    Player newPlayer = initializeNewPlayer();
                    logger.info(String.format("Created new player with ID: %d", newPlayer.getId()));

                    // Create panel and dialog
                    PlayerEditPanel panel = new PlayerEditPanel(newPlayer);
                    Dialog<Player> dialog = frame.createDialog(newPlayer, panel);
                    dialog.setTitle("Add Player");

                    boolean result = dialog.showDialog();
                    logger.info(String.format("Dialog closed with result: %s", result));

                    if (result) {
                        Player updatedPlayer = panel.getUpdatedPlayer();
                        TickerManager.getInstance().addPlayerToTicker(currentTicker, updatedPlayer);
                        logger.info(String.format("Player %d added successfully", updatedPlayer.getId()));
                    }
                }
            } catch (Exception e) {
                logger.severe("Error in handleAddPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private Player initializeNewPlayer() {
        Player player = new Strike();
        player.setId(redisService.getNextPlayerId());
        player.setRules(new HashSet<>());
        return player;
    }

    private void handleEditPlayer(Player player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    PlayerEditPanel panel = new PlayerEditPanel(player);
                    Dialog<Player> dialog = frame.createDialog(player, panel);
                    dialog.setTitle("Edit Player: " + player.getName());

                    logger.info("Showing edit player dialog");
                    boolean result = dialog.showDialog();
                    logger.info("Dialog result: " + result);

                    if (result) {
                        Player updatedPlayer = panel.getUpdatedPlayer();
                        // Use SHOW_PLAYER_EDITOR_OK to ensure consistent handling
                        commandBus.publish(Commands.SHOW_PLAYER_EDITOR_OK, this, updatedPlayer);
                        logger.info(String.format("Published player update for {} with instrument: {} (ID: {})",
                                updatedPlayer.getName(),
                                updatedPlayer.getInstrument() != null ? updatedPlayer.getInstrument().getName()
                                        : "none",
                                updatedPlayer.getInstrumentId()));
                    }
                } catch (Exception e) {
                    logger.severe("Error in handleEditPlayer: " + e);
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleAddRule(Player player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Rule newRule = redisService.newRule();
                    RuleEditPanel panel = new RuleEditPanel(newRule);
                    Dialog<Rule> dialog = frame.createDialog(newRule, panel);
                    dialog.setTitle("Add Rule");

                    if (dialog.showDialog()) {
                        Rule updatedRule = panel.getUpdatedRule();
                        if (redisService.isValidNewRule(player, updatedRule)) {
                            redisService.addRuleToPlayer(player, updatedRule);
                            // Get fresh state
                            // Player refreshedPlayer = redisService.findPlayerById(player.getId());
                            // Ticker ticker = redisService.findTickerForPlayer(refreshedPlayer);

                            // Re-select the player to update rules display
                            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                            commandBus.publish(Commands.RULE_ADDED, this, player);
                            // commandBus.publish(Commands.TICKER_UPDATED, this, ticker);
                        } else {
                            // ... existing error handling ...
                        }
                    }
                } catch (Exception e) {
                    // ... existing error handling ...
                }
            });
        }
    }

    private void handleEditRule(Rule rule) {
        if (rule != null) {
            SwingUtilities.invokeLater(() -> {
                RuleEditPanel panel = new RuleEditPanel(rule);
                Dialog<Rule> dialog = frame.createDialog(rule, panel);
                dialog.setTitle("Edit Rule");

                if (dialog.showDialog()) {
                    Rule updatedRule = panel.getUpdatedRule();
                    Player player = redisService.findPlayerForRule(rule);
                    if (player != null) {
                        redisService.saveRule(updatedRule);
                        // Get fresh state and re-select player
                        Player refreshedPlayer = redisService.findPlayerById(player.getId());
                        commandBus.publish(Commands.PLAYER_SELECTED, this, refreshedPlayer);
                    }
                }
            });
        }
    }

    private void handlePlayerParameters(Player player) {
        if (player != null && player.getInstrument() != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Create controls panel with instrument context
                    ControlsPanel controlsPanel = new ControlsPanel();

                    // Pre-select the player's instrument
                    controlsPanel.selectInstrument(player.getInstrument());

                    // Create dialog using Frame's createDialog method
                    Dialog<Player> dialog = frame.createDialog(player, controlsPanel);
                    dialog.setTitle("Controls - " + player.getName() + " (" + player.getInstrument().getName() + ")");

                    // Show dialog
                    dialog.showDialog();

                    logger.info("Showing controls dialog for player: " + player.getName() +
                            " with instrument: " + player.getInstrument().getName());
                } catch (Exception e) {
                    logger.severe("Error showing controls dialog: " + e.getMessage());
                    JOptionPane.showMessageDialog(frame,
                            "Could not show controls: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } else {
            JOptionPane.showMessageDialog(frame,
                    "No instrument assigned to player",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}

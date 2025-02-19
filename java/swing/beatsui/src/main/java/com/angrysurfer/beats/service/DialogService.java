package com.angrysurfer.beats.service;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.Frame;
import com.angrysurfer.beats.panel.PlayerEditPanel;
import com.angrysurfer.beats.panel.RuleEditPanel;
import com.angrysurfer.beats.panel.RulesPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

public class DialogService implements CommandListener {

    private static final Logger logger = Logger.getLogger(DialogService.class.getName());

    private static DialogService instance;
    private final CommandBus commandBus = CommandBus.getInstance();
    private final RedisService redisService = RedisService.getInstance();
    private final Frame frame;

    public static DialogService initialize(Frame frame) {
        if (instance == null) {
            instance = new DialogService(frame);
        }
        return instance;
    }

    private DialogService(Frame frame) {
        this.frame = frame;
        commandBus.register(this);
    }

    @Override
    public void onAction(Command action) {
        logger.info("DialogService received command: " + action.getCommand());
        switch (action.getCommand()) {
            case Commands.PLAYER_ADD_REQUEST -> {
                logger.info("Handling add player request");
                handleAddPlayer();
            }
            case Commands.PLAYER_EDIT_REQUEST -> handleEditPlayer((ProxyStrike) action.getData());
            case Commands.RULE_ADD_REQUEST -> {
                if (action.getData() instanceof RulesPanel.RuleActionData data) {
                    handleAddRule(data.player());
                }
            }
            case Commands.RULE_EDIT_REQUEST -> {
                if (action.getData() instanceof RulesPanel.RuleActionData data) {
                    handleEditRule(data.player(), data.rule());
                }
            }
        }
    }

    private void handleAddPlayer() {
        logger.info("Starting handleAddPlayer");
        SwingUtilities.invokeLater(() -> {
            try {
                ProxyTicker currentTicker = TickerManager.getInstance().getActiveTicker();
                logger.info("Current ticker: " + (currentTicker != null ? currentTicker.getId() : "null"));
                
                if (currentTicker != null) {
                    // Initialize player
                    ProxyStrike newPlayer = initializeNewPlayer();
                    logger.info("Created new player with ID: " + newPlayer.getId());
                    redisService.savePlayer(newPlayer);
                    redisService.addPlayerToTicker(currentTicker, newPlayer);

                    // Create panel and dialog
                    logger.info("Creating PlayerEditPanel");
                    PlayerEditPanel panel = new PlayerEditPanel(newPlayer, null);
                    logger.info("Creating Dialog");
                    Dialog<ProxyStrike> dialog = frame.createDialog(newPlayer, panel);
                    dialog.setTitle("Add Player");
                    
                    logger.info("About to show dialog");
                    boolean result = dialog.showDialog();
                    logger.info("Dialog closed with result: " + result);
                    
                    if (result) {
                        ProxyStrike updatedPlayer = panel.getUpdatedPlayer();
                        redisService.savePlayer(updatedPlayer);
                        commandBus.publish(Commands.TICKER_UPDATED, this, currentTicker);
                        logger.info("Player saved and ticker updated");
                    } else {
                        redisService.removePlayerFromTicker(currentTicker, newPlayer);
                        redisService.deletePlayer(newPlayer);
                        commandBus.publish(Commands.TICKER_UPDATED, this, currentTicker);
                        logger.info("Player add cancelled and cleaned up");
                    }
                }
            } catch (Exception e) {
                logger.severe("Error in handleAddPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private ProxyStrike initializeNewPlayer() {
        ProxyStrike player = redisService.newPlayer();
        player.setName("New Player");
        player.setChannel(1);
        player.setPreset(0L);
        player.setSwing(0L);
        player.setLevel(100L);
        player.setNote(60L);
        player.setMinVelocity(64L);
        player.setMaxVelocity(127L);
        player.setProbability(100L);
        player.setRandomDegree(0L);
        player.setRatchetCount(1L);
        player.setRatchetInterval(1L);
        player.setPanPosition(64L);
        player.setStickyPreset(false);
        player.setUseInternalBeats(false);
        player.setUseInternalBars(false);
        player.setPreserveOnPurge(false);
        player.setSparse(0L);
        return player;
    }

    private void handleEditPlayer(ProxyStrike player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    PlayerEditPanel panel = new PlayerEditPanel(player, null);
                    Dialog<ProxyStrike> dialog = frame.createDialog(player, panel);
                    dialog.setTitle("Edit Player: " + player.getName());
                    
                    logger.info("Showing edit player dialog");
                    boolean result = dialog.showDialog();
                    logger.info("Dialog result: " + result);
                    
                    if (result) {
                        ProxyStrike updatedPlayer = panel.getUpdatedPlayer();
                        redisService.savePlayer(updatedPlayer);
                        commandBus.publish(Commands.TICKER_UPDATED, this, TickerManager.getInstance().getActiveTicker());
                    }
                } catch (Exception e) {
                    logger.severe("Error in handleEditPlayer: " + e);
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleAddRule(ProxyStrike player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                ProxyRule newRule = redisService.newRule();
                RuleEditPanel panel = new RuleEditPanel(newRule);
                Dialog<ProxyRule> dialog = frame.createDialog(newRule, panel);
                dialog.setTitle("Add Rule");

                if (dialog.showDialog()) {
                    ProxyRule updatedRule = panel.getUpdatedRule();
                    redisService.addRuleToPlayer(player, updatedRule);
                    commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                } else {
                    // If canceled, use removeRuleFromPlayer instead of direct delete
                    redisService.removeRuleFromPlayer(player, newRule);
                }
            });
        }
    }

    private void handleEditRule(ProxyStrike player, ProxyRule rule) {
        if (player != null && rule != null) {
            SwingUtilities.invokeLater(() -> {
                RuleEditPanel panel = new RuleEditPanel(rule);
                Dialog<ProxyRule> dialog = frame.createDialog(rule, panel);
                dialog.setTitle("Edit Rule");

                if (dialog.showDialog()) {
                    ProxyRule updatedRule = panel.getUpdatedRule();
                    redisService.saveRule(updatedRule);

                    // Notify about rule update
                    commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                }
            });
        }
    }
}

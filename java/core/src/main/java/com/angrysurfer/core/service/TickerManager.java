package com.angrysurfer.core.service;

import java.util.Objects;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerManager {

    private static final Logger logger = Logger.getLogger(TickerManager.class.getName());
    private static TickerManager instance;
    private final CommandBus commandBus = CommandBus.getInstance();
    private ProxyTicker activeTicker;
    private final RedisService redisService = RedisService.getInstance();

    private TickerManager() {
        commandBus.register(new CommandListener() {
            public void onAction(Command action) {
                if (action == null || action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.TICKER_REQUEST -> {
                        if (Objects.nonNull(activeTicker)) {
                            commandBus.publish(Commands.TICKER_SELECTED, this, activeTicker);
                        }
                    }
                    case Commands.TRANSPORT_REWIND -> moveBack();
                    case Commands.TRANSPORT_FORWARD -> moveForward();
                    case Commands.PLAYER_ADD_REQUEST -> {
                        // Just show the editor, don't create player yet
                        commandBus.publish(Commands.SHOW_PLAYER_EDITOR, this, null);
                    }
                    case Commands.PLAYER_EDIT_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike player) { // Changed from ProxyStrike[]
                            // Open editor with the selected player
                            commandBus.publish(Commands.SHOW_PLAYER_EDITOR, this, player);
                        }
                    }
                    case Commands.SHOW_PLAYER_EDITOR_OK -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            RedisService redis = RedisService.getInstance();
                            if (player.getId() == null) {
                                // This is a new player
                                player = redis.newPlayer();
                                initializeDefaultPlayerValues(player);
                                redis.addPlayerToTicker(activeTicker, player);
                            }
                            redis.savePlayer(player);

                            // Refresh UI with updated ticker
                            activeTicker = redis.findTickerById(activeTicker.getId());
                            commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
                        }
                    }
                    case Commands.RULE_DELETE_REQUEST -> {
                        if (action.getData() instanceof ProxyRule[] rules) {
                            RedisService redis = RedisService.getInstance();
                            ProxyStrike player = redis.findPlayerForRule(rules[0]);
                            if (player != null) {
                                for (ProxyRule rule : rules) {
                                    redis.removeRuleFromPlayer(player, rule);
                                }
                                // Get fresh state of both player and ticker
                                player = redis.findPlayerById(player.getId());
                                activeTicker = redis.findTickerById(activeTicker.getId());

                                // Notify UI of updates in correct order:
                                // 1. Update ticker (updates player list)
                                commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
                                // 2. Re-select the player (refreshes rules panel)
                                commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                            }
                        }
                    }
                    case Commands.PLAYER_DELETE_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike[] players) {
                            RedisService redis = RedisService.getInstance();
                            for (ProxyStrike player : players) {
                                redis.removePlayerFromTicker(activeTicker, player);
                                redis.deletePlayer(player);
                            }

                            // Reload ticker after deletion to get fresh state
                            activeTicker = redis.findTickerById(activeTicker.getId());

                            // Notify UI of update with fresh ticker state
                            commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
                        }
                    }
                    case Commands.RULE_ADD_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            // Just show the dialog, don't create rule yet
                            commandBus.publish(Commands.SHOW_RULE_EDITOR, this, player);
                        }
                    }
                    case Commands.SHOW_RULE_EDITOR_OK -> {
                        if (action.getData() instanceof ProxyRule rule) {
                            RedisService redis = RedisService.getInstance();
                            ProxyStrike player = redis.findPlayerForRule(rule);

                            if (player != null) {
                                // For existing rule, just save it
                                if (rule.getId() != null) {
                                    redis.saveRule(rule);
                                } else {
                                    // For new rule, add it to player
                                    redis.addRuleToPlayer(player, rule);
                                }

                                // Get fresh state and notify UI
                                player = redis.findPlayerById(player.getId());
                                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                            }
                        }
                    }
                    case Commands.RULE_EDIT_REQUEST -> {
                        if (action.getData() instanceof ProxyRule rule) {
                            commandBus.publish(Commands.SHOW_RULE_EDITOR, this, rule);
                        }
                    }
                    case Commands.RULE_UPDATED -> {
                        if (action.getData() instanceof ProxyRule rule) {
                            RedisService redis = RedisService.getInstance();
                            ProxyStrike player = redis.findPlayerForRule(rule);
                            if (player != null) {
                                redis.saveRule(rule);

                                // Get fresh state of both player and ticker
                                player = redis.findPlayerById(player.getId());
                                activeTicker = redis.findTickerById(activeTicker.getId());

                                // Notify UI of updates in correct order:
                                commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
                                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                                // Send rule selected to restore selection
                                // commandBus.publish(Commands.RULE_SELECTED, this, rule);
                            }
                        }
                    }
                }
            }
        });
        loadActiveTicker();
        logger.info("TickerManager initialized");
    }

    public static TickerManager getInstance() {
        if (instance == null) {
            instance = new TickerManager();
        }
        return instance;
    }

    public ProxyTicker getActiveTicker() {
        return activeTicker;
    }

    public boolean canMoveBack() {
        RedisService redis = RedisService.getInstance();
        Long minId = redis.getMinimumTickerId();
        return (activeTicker != null && minId != null && activeTicker.getId() > minId);
    }

    public boolean canMoveForward() {
        if (activeTicker == null || activeTicker.getPlayers() == null || activeTicker.getPlayers().isEmpty()) {
            return false;
        }

        for (IPlayer player : activeTicker.getPlayers()) {
            if (((ProxyStrike) player).getRules() != null && !((ProxyStrike) player).getRules().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void moveBack() {
        RedisService redis = RedisService.getInstance();
        Long prevId = redis.getPreviousTickerId(activeTicker);
        if (prevId != null) {
            tickerSelected(redis.findTickerById(prevId));
            if (activeTicker != null) {
                logger.info("Moved back to ticker: " + activeTicker.getId());
            }
        }
    }

    public void moveForward() {
        RedisService redis = RedisService.getInstance();
        Long maxId = redis.getMaximumTickerId();

        if (activeTicker != null && maxId != null && activeTicker.getId().equals(maxId)) {
            // Create a new ticker if we're at the end
            ProxyTicker newTicker = redis.newTicker();
            tickerSelected(newTicker);
            logger.info("Created new ticker and moved forward to it: " + newTicker.getId());
        } else {
            // Otherwise, move to the next existing ticker
            Long nextId = redis.getNextTickerId(activeTicker);
            if (nextId != null) {
                tickerSelected(redis.findTickerById(nextId));
                if (activeTicker != null) {
                    logger.info("Moved forward to ticker: " + activeTicker.getId());
                }
            }
        }
    }

    public void tickerSelected(ProxyTicker ticker) {
        if (ticker != null && !ticker.equals(this.activeTicker)) {
            this.activeTicker = ticker;
            commandBus.publish(Commands.TICKER_SELECTED, this, ticker);
            logger.info("Ticker selected: " + ticker.getId());
        }
    }

    private void loadActiveTicker() {
        RedisService redis = RedisService.getInstance();
        ProxyTicker ticker = redis.findTickerById(1L); // Load ticker ID 1
        if (ticker == null) {
            ticker = redis.newTicker();
        }
        tickerSelected(ticker);
    }

    private void initializeDefaultPlayerValues(ProxyStrike player) {
        player.setName("New Player");
        player.setChannel(1);
        player.setPreset(0L);
        player.setLevel(100L);
        player.setNote(60L);
        player.setMinVelocity(64L);
        player.setMaxVelocity(127L);
        player.setProbability(100L);
        player.setRandomDegree(0L);
        player.setRatchetCount(1L);
        player.setPanPosition(64L);
        player.setRatchetInterval(1L);
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, activeTicker);
    }

    public void updatePlayerLevel(ProxyStrike player, int level) {
        player.setLevel((long) level);
    }

    public void updatePlayerSparse(ProxyStrike player, int sparse) {
        player.setSparse(sparse / 100.0);
    }

    public void updatePlayerPan(ProxyStrike player, int pan) {
        player.setPanPosition((long) pan);
    }

    public void updatePlayerRandom(ProxyStrike player, int random) {
        player.setRandomDegree((long) random);
    }

    public void updatePlayerVelocityMax(ProxyStrike player, int velocityMax) {
        player.setMaxVelocity((long) velocityMax);
    }

    public void updatePlayerVelocityMin(ProxyStrike player, int velocityMin) {
        player.setMinVelocity((long) velocityMin);
    }

    public void updatePlayerProbability(ProxyStrike player, int probability) {
        player.setProbability((long) probability);
    }

    public void updatePlayerSwing(ProxyStrike player, int swing) {
        player.setSwing((long) swing);
    }

    public void updatePlayerNote(ProxyStrike player, int note) {
        player.setNote((long) note);
    }

    public void savePlayerProperties(ProxyStrike player) {
        RedisService.getInstance().savePlayer(player);
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, activeTicker);
    }

    public void clearRules(IPlayer player) {
        // Logic to clear rules from the player
        CommandBus.getInstance().publish(Commands.RULES_CLEARED, this, activeTicker);
    }

    public void deleteRule(ProxyStrike player, String operator, String comparison, String value, String part) {
        // Logic to delete a rule from the player
        CommandBus.getInstance().publish(Commands.RULE_DELETED, this, activeTicker);
    }

    public void addRule(ProxyStrike player, String operator, String comparison, String value, String part) {
        // Logic to add a rule to the player
        CommandBus.getInstance().publish(Commands.RULE_ADDED, this, activeTicker);
    }

    public void deletePlayer(ProxyStrike player) {
        activeTicker.getPlayers().remove(player);
        CommandBus.getInstance().publish(Commands.PLAYER_DELETED, this, activeTicker);
    }

    public void updatePlayer(ProxyStrike player) {
        // activeTicker.updatePlayer(player);
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATED, this, activeTicker);
    }

    public void addPlayer(ProxyStrike player) {
        // activeTicker.addPlayer(player);
        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, activeTicker);
    }
}
package com.angrysurfer.beats.service;

import java.util.Objects;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

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
                        ProxyTicker currentTicker = getActiveTicker();
                        if (currentTicker != null) {
                            RedisService redis = RedisService.getInstance();
                            // Create new player with default values
                            ProxyStrike newPlayer = redis.newPlayer();
                            initializeDefaultPlayerValues(newPlayer);
                            
                            // Add to ticker and save
                            redis.addPlayerToTicker(currentTicker, newPlayer);
                            redis.savePlayer(newPlayer);

                            // Notify about new player
                            commandBus.publish(Commands.TICKER_UPDATED, this, currentTicker);
                        }
                    }
                    case Commands.RULE_DELETE_REQUEST -> {
                        if (action.getData() instanceof ProxyRule rule) {
                            RedisService redis = RedisService.getInstance();
                            ProxyStrike player = redis.findPlayerForRule(rule);
                            if (player != null) {
                                redis.removeRuleFromPlayer(player, rule);
                                // Notify UI of update
                                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                            }
                        }
                    }
                    case Commands.PLAYER_DELETE_REQUEST -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            RedisService redis = RedisService.getInstance();
                            redis.removePlayerFromTicker(activeTicker, player);
                            redis.deletePlayer(player);
                            
                            // Reload ticker after deletion to get fresh state
                            activeTicker = redis.findTickerById(activeTicker.getId());
                            
                            // Notify UI of update with fresh ticker state
                            commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
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

        for (IProxyPlayer player : activeTicker.getPlayers()) {
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
        player.setRatchetInterval(1L);
        player.setPanPosition(64L);
    }
}

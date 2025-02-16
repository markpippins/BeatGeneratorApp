package com.angrysurfer.beats.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import com.angrysurfer.beats.App;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.util.LogManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerManager implements CommandListener {

    private static final Logger logger = Logger.getLogger(TickerManager.class.getName());

    private ProxyTicker activeTicker;
    private final CommandBus commandBus = CommandBus.getInstance();
    private List<ProxyTicker> tickers;

    public TickerManager() {
        commandBus.register(this);
        loadTicker();
    }

    private void loadTicker() {
        try {
            // Clean state
            if (tickers != null) {
                tickers.clear();
            } else {
                tickers = new ArrayList<>();
            }
            activeTicker = null;

            // Load fresh data
            String activeTickerId = App.getRedisService().getActiveTickerId();
            logger.info("Loading with active ticker ID: " + activeTickerId);

            // Load tickers
            tickers = App.getRedisService().findAllTickers();
            logger.info("Found " + tickers.size() + " tickers in Redis");

            // Set active ticker
            if (activeTickerId != null) {
                activeTicker = tickers.stream()
                    .filter(t -> t.getId().toString().equals(activeTickerId))
                    .findFirst()
                    .orElseGet(() -> {
                        if (!tickers.isEmpty()) {
                            return tickers.get(0);
                        }
                        return createDefaultTicker();
                    });
            } else if (!tickers.isEmpty()) {
                activeTicker = tickers.get(0);
            } else {
                activeTicker = createDefaultTicker();
                tickers.add(activeTicker);
            }

            // Set active ticker in Redis and notify UI
            App.getRedisService().setActiveTickerId(activeTicker.getId());
            publishTickerSelected();

        } catch (Exception e) {
            logger.severe("Error loading ticker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ProxyTicker createDefaultTicker() {
        ProxyTicker newTicker = new ProxyTicker();
        newTicker.setTempoInBPM(120.0f);
        newTicker.setBars(4);
        newTicker.setBeatsPerBar(4);
        newTicker.setTicksPerBeat(24);
        newTicker.setParts(1);
        newTicker.setPartLength(4L);
        newTicker.setPlayers(new HashSet<>());
        logger.info("Creating new default ticker");
        return App.getRedisService().saveTicker(newTicker);
    }

    public void addPlayer(ProxyStrike player) {
        if (activeTicker != null) {
            // Set correct ticker reference
            player.setTicker(activeTicker);
            
            // Save the player first
            ProxyStrike savedPlayer = App.getRedisService().saveStrike(player);
            logger.info("Saved player: " + savedPlayer.getId() + " to Redis");
            
            // Update active ticker's player list
            if (!activeTicker.getPlayers().contains(savedPlayer)) {
                activeTicker.getPlayers().add(savedPlayer);
                
                // Save updated ticker
                App.getRedisService().saveTicker(activeTicker);
                logger.info("Updated ticker " + activeTicker.getId() + 
                          " with player " + savedPlayer.getId());
                
                // Notify UI components
                Command cmd = new Command();
                cmd.setCommand(Commands.PLAYER_ADDED_TO_TICKER);
                cmd.setData(savedPlayer);
                commandBus.publish(cmd);
            } else {
                logger.warning("Player " + savedPlayer.getId() + 
                             " already exists in ticker " + activeTicker.getId());
            }
        }
    }

    public void removePlayer(ProxyStrike player) {
        if (activeTicker != null) {
            logger.info("Removing player " + player.getName() + " from ticker " + activeTicker.getId());
            
            // Remove from active ticker's player list
            activeTicker.getPlayers().remove(player);
            
            // Delete from Redis
            App.getRedisService().deleteStrike(player);
            
            // Save updated ticker
            App.getRedisService().saveTicker(activeTicker);
            
            // Notify UI
            Command cmd = new Command();
            cmd.setCommand(Commands.PLAYER_REMOVED_FROM_TICKER);
            cmd.setData(activeTicker);  // Send updated ticker
            commandBus.publish(cmd);
            
            logger.info("Player removed and ticker updated. Remaining players: " + 
                       activeTicker.getPlayers().size());
        } else {
            logger.warning("Cannot remove player - no active ticker");
        }
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TRANSPORT_FORWARD -> handleForwardCommand();
            case Commands.TRANSPORT_REWIND -> handleRewindCommand();
            case Commands.DATABASE_RESET -> handleDatabaseReset();
            case Commands.PLAYER_ADDED_TO_TICKER -> {
                ProxyStrike player = (ProxyStrike) action.getData();
                if (activeTicker != null && 
                    player.getTicker() != null && 
                    player.getTicker().getId().equals(activeTicker.getId())) {
                    // Add player to active ticker if not already present
                    if (!activeTicker.getPlayers().contains(player)) {
                        activeTicker.getPlayers().add(player);
                        logger.info("Added player " + player.getId() + " to active ticker " + activeTicker.getId());
                    }
                    // Notify UI of update
                    publishTickerSelected();
                }
            }
            case Commands.PLAYER_REMOVED_FROM_TICKER -> publishTickerSelected();
        }
    }

    private void handleDatabaseReset() {

        // Create a new default ticker after clearing
        // ProxyTicker ticker = App.getRedisService().createDefaultTicker();
        // setActiveTicker(ticker);

        // // Notify UI components of the change
        // Command cmd = new Command();
        // cmd.setCommand(Commands.TICKER_CHANGED);
        // cmd.setData(ticker);
        // CommandBus.getInstance().publish(cmd);
    }

    private void handleForwardCommand() {
        try {
            if (tickers == null) {
                tickers = App.getRedisService().findAllTickers();
            }

            int currentIndex = -1;
            if (activeTicker != null) {
                currentIndex = tickers.indexOf(activeTicker);
            }

            if (currentIndex < tickers.size() - 1) {
                activeTicker = tickers.get(currentIndex + 1);
                logger.info("Moving to next ticker: " + activeTicker.getId());
                publishTickerSelected(); // Changed from publishTickerChanged
            } else {
                if (activeTicker != null && !activeTicker.getPlayers().isEmpty()) {
                    createNewTicker();
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error("TickerManager", "Error navigating tickers", e);
        }
    }

    private void handleRewindCommand() {
        try {
            if (tickers == null) {
                tickers = App.getRedisService().findAllTickers();
            }

            int currentIndex = -1;
            if (activeTicker != null) {
                currentIndex = tickers.indexOf(activeTicker);
            }

            if (currentIndex > 0) {
                activeTicker = tickers.get(currentIndex - 1);
                logger.info("Moving to previous ticker: " + activeTicker.getId());
                publishTickerSelected();

                // Save as active ticker in Redis
                App.getRedisService().saveTicker(activeTicker);
            } else {
                logger.info("Already at first ticker, can't rewind further");
            }
        } catch (Exception e) {
            logger.severe("Error handling rewind: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createNewTicker() {
        ProxyTicker newTicker = new ProxyTicker();
        newTicker.setTempoInBPM(120.0f);
        newTicker.setBars(4);
        newTicker.setBeatsPerBar(4);
        newTicker.setTicksPerBeat(24);
        newTicker.setParts(1);
        newTicker.setPartLength(4L);
        newTicker.setPlayers(new HashSet<>());

        activeTicker = App.getRedisService().saveTicker(newTicker);
        tickers.add(activeTicker);
        publishTickerSelected();
    }

    private void publishTickerSelected() {
        if (activeTicker == null) {
            logger.severe("Cannot publish null ticker!");
            return;
        }

        // Log current state
        logger.info("Publishing ticker state - ID: " + activeTicker.getId() + 
                   ", Players: " + activeTicker.getPlayers().size());
        for (IProxyPlayer player : activeTicker.getPlayers()) {
            logger.info("  Player: " + player.getName() + " (ID: " + player.getId() + ")");
        }

        // Update active ticker ID in Redis
        App.getRedisService().setActiveTickerId(activeTicker.getId());

        // Publish update
        Command cmd = new Command();
        cmd.setCommand(Commands.TICKER_SELECTED);
        cmd.setData(activeTicker);
        commandBus.publish(cmd);
    }
}

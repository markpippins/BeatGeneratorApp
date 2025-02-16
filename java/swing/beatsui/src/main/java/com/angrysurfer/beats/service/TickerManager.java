package com.angrysurfer.beats.service;

import java.util.HashSet;
import java.util.List;

import com.angrysurfer.beats.App;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.util.LogManager;

import lombok.Getter;

@Getter
public class TickerManager implements CommandListener {
    private ProxyTicker activeTicker;
    private final CommandBus commandBus = CommandBus.getInstance();
    private List<ProxyTicker> tickers;

    public TickerManager() {
        commandBus.register(this);
        loadTicker();
    }

    private void loadTicker() {
        activeTicker = App.getRedisService().loadTicker();
        publishTickerLoaded();
    }

    private void publishTickerLoaded() {
        Command cmd = new Command();
        cmd.setCommand(Commands.TICKER_LOADED);
        cmd.setData(activeTicker);
        commandBus.publish(cmd);
    }

    public void addPlayer(ProxyStrike player) {
        if (activeTicker != null) {
            activeTicker.getPlayers().add(player);
            App.getRedisService().saveTicker(activeTicker);
            
            Command cmd = new Command();
            cmd.setCommand(Commands.PLAYER_ADDED_TO_TICKER);
            cmd.setData(player);
            commandBus.publish(cmd);
        }
    }

    public void removePlayer(ProxyStrike player) {
        if (activeTicker != null) {
            activeTicker.getPlayers().remove(player);
            App.getRedisService().deleteStrike(player); // Changed from deletePlayerFromTicker
            
            Command cmd = new Command();
            cmd.setCommand(Commands.PLAYER_REMOVED_FROM_TICKER);
            cmd.setData(player);
            commandBus.publish(cmd);
        }
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TRANSPORT_FORWARD -> handleForwardCommand();
            case Commands.TRANSPORT_REWIND -> handleRewindCommand();
        }
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
                publishTickerSelected();
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
        if (tickers == null || tickers.isEmpty()) return;

        int currentIndex = tickers.indexOf(activeTicker);
        if (currentIndex > 0) {
            activeTicker = tickers.get(currentIndex - 1);
            publishTickerSelected();
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
        Command cmd = new Command();
        cmd.setCommand(Commands.TICKER_SELECTED);
        cmd.setData(activeTicker);
        commandBus.publish(cmd);
    }
}

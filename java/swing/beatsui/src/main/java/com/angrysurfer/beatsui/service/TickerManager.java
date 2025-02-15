package com.angrysurfer.beatsui.service;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.api.Command;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.core.proxy.ProxyTicker;

public class TickerManager {
    private static final Logger logger = Logger.getLogger(TickerManager.class.getName());
    private ProxyTicker currentTicker;
    private final CommandBus commandBus;

    public TickerManager() {
        this.commandBus = CommandBus.getInstance();
        initialize();
    }

    private void initialize() {
        try {
            ProxyTicker ticker = App.getRedisService().loadTicker();
            logger.info("Loaded existing ticker: BPM=" + ticker.getTempoInBPM() +
                    ", Bars=" + ticker.getBars());
            setCurrentTicker(ticker);
        } catch (Exception e) {
            logger.info("No existing ticker found, creating default ticker");
            ProxyTicker newTicker = createDefaultTicker();
            App.getRedisService().saveTicker(newTicker);
            setCurrentTicker(newTicker);
            logger.info("Created and saved default ticker");
        }
    }

    private ProxyTicker createDefaultTicker() {
        ProxyTicker ticker = new ProxyTicker();
        ticker.setTempoInBPM(120.0f);
        ticker.setBars(4);
        ticker.setBeatsPerBar(4);
        ticker.setTicksPerBeat(24);
        ticker.setParts(1);
        ticker.setPartLength(4L);
        return ticker;
    }

    public void setCurrentTicker(ProxyTicker ticker) {
        this.currentTicker = ticker;
        publishTickerSelected();
    }

    public ProxyTicker getCurrentTicker() {
        return currentTicker;
    }

    private void publishTickerSelected() {
        SwingUtilities.invokeLater(() -> {
            Command action = new Command();
            action.setCommand(Commands.TICKER_SELECTED);
            action.setData(currentTicker);
            commandBus.publish(action);
        });
    }
}

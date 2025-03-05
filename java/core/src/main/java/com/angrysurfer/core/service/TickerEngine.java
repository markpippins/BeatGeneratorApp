package com.angrysurfer.core.service;

import java.util.Objects;
import java.util.logging.Logger;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerEngine {

    private static final Logger logger = Logger.getLogger(TickerEngine.class.getName());

    private Ticker activeTicker;

    private final CommandBus commandBus = CommandBus.getInstance();

    public void setActiveTicker(Ticker ticker) {
        // RedisService redis = RedisService.getInstance();
        // Ticker ticker = redis.newTicker();
        // redis.saveTicker(ticker);
        activeTicker = ticker;

        commandBus.publish(Commands.TICKER_SELECTED, this, ticker);
    }

    public boolean canMoveBack() {
        Long minId = RedisService.getInstance().getMinimumTickerId();
        return (Objects.nonNull(getActiveTicker()) && Objects.nonNull(minId) && getActiveTicker().getId() > minId);
    }

    public boolean canMoveForward() {
        return Objects.nonNull(getActiveTicker()) && getActiveTicker().isValid();
    }

    public void deleteAllTickers() {
        RedisService redis = RedisService.getInstance();

        logger.info("Deleting tickers");
        redis.getAllTickerIds().forEach(id -> {
            Ticker ticker = redis.findTickerById(id);
            if (ticker != null) {
                logger.info(String.format("Loading ticker {}", id));
                redis.deleteTicker(id);
                commandBus.publish(Commands.TICKER_DELETED, this, id);
            }
        });
    }

    public void loadActiveTicker() {
        RedisService redis = RedisService.getInstance();

        logger.info("Loading ticker");
        Long minId = redis.getMinimumTickerId();
        Long maxId = redis.getMaximumTickerId();

        logger.info("Minimum ticker ID: " + minId);
        logger.info("Maximum ticker ID: " + maxId);

        Ticker ticker = null;

        // If we have existing tickers, try to load the first valid one
        if (minId != null && maxId != null) {
            for (Long id = minId; id <= maxId; id++) {
                ticker = redis.findTickerById(id);
                if (ticker != null) {
                    logger.info("Found valid ticker " + ticker.getId());
                    break;
                }
            }
        }

        // If no valid ticker found or no tickers exist, create a new one
        if (ticker == null) {
            logger.info("No valid ticker found, creating new ticker");
            ticker = redis.newTicker();
            redis.saveTicker(ticker);
            logger.info("Created new ticker with ID: " + ticker.getId());
        }

        setActiveTicker(ticker);
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
            // Only create a new ticker if current one is valid and has active rules
            if (activeTicker.isValid() && !activeTicker.getPlayers().isEmpty() &&
                    activeTicker.getPlayers().stream()
                            .map(p -> p)
                            .anyMatch(p -> p.getRules() != null && !p.getRules().isEmpty())) {

                Ticker newTicker = redis.newTicker();
                tickerSelected(newTicker);
                // commandBus.publish(Commands.TICKER_CREATED, this, newTicker); // Add this
                // line
                logger.info("Created new ticker and moved forward to it: " + newTicker.getId());
            }
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

    public void tickerSelected(Ticker ticker) {
        if (ticker != null && !ticker.equals(this.activeTicker)) {
            this.activeTicker = ticker;
            commandBus.publish(Commands.TICKER_SELECTED, this, ticker);
            logger.info("Ticker selected: " + ticker.getId());
        }
    }

}

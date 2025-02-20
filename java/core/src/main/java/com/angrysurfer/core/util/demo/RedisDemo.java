package com.angrysurfer.core.util.demo;

import java.util.HashSet;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.service.RedisService;

public class RedisDemo {
    private static final Logger logger = Logger.getLogger(RedisDemo.class.getName());

    public static void main(String[] args) {
        RedisService redis = RedisService.getInstance();

        // Clear database and create initial ticker
        redis.onAction(new Command(Commands.CLEAR_DATABASE, null, null));
        logger.info("Database cleared and initial ticker created");

        // Create multiple tickers
        logger.info("=== Creating Multiple Tickers ===");
        ProxyTicker[] tickers = new ProxyTicker[3];
        for (int i = 0; i < 3; i++) {
            tickers[i] = redis.newTicker();
            logger.info("Created ticker " + (i + 1) + " with ID: " + tickers[i].getId());
        }

        // Add players to first ticker
        logger.info("\n=== Adding Players to Ticker 1 ===");
        ProxyStrike[] drums = {
            createDrumPlayer(redis, "Kick", 36, tickers[0]),
            createDrumPlayer(redis, "Snare", 38, tickers[0]),
            createDrumPlayer(redis, "HiHat", 42, tickers[0])
        };
        logger.info("Added " + drums.length + " drum players to ticker 1");

        // Add rules to players
        logger.info("\n=== Adding Rules to Players ===");
        addRhythmRules(redis, drums[0], new double[]{1.0, 5.0});     // Kick on 1 and 2+
        addRhythmRules(redis, drums[1], new double[]{3.0, 7.0});     // Snare on 2 and 4
        addRhythmRules(redis, drums[2], new double[]{1.5, 2.5, 3.5, 4.5}); // HiHat on upbeats
        
        // Enhanced navigation testing
        logger.info("\n=== Testing Bi-directional Navigation ===");
        ProxyTicker current = tickers[0];
        
        // Forward navigation
        logger.info("Forward Navigation:");
        while (true) {
            describeTickerState(redis, current);
            Long nextId = redis.getNextTickerId(current);
            if (nextId == null) break;
            current = redis.findTickerById(nextId);
        }

        // Backward navigation
        logger.info("\nBackward Navigation:");
        while (true) {
            describeTickerState(redis, current);
            Long prevId = redis.getPreviousTickerId(current);
            if (prevId == null) break;
            current = redis.findTickerById(prevId);
        }

        // Interleaved operations and navigation
        logger.info("\n=== Interleaved Operations ===");
        
        // Add player to middle ticker
        current = tickers[1];
        ProxyStrike cymbal = createDrumPlayer(redis, "Cymbal", 49, current);
        describeTickerState(redis, current);

        // Move to next ticker and add rules
        current = redis.findTickerById(redis.getNextTickerId(current));
        ProxyStrike tom = createDrumPlayer(redis, "Tom", 45, current);
        addRhythmRules(redis, tom, new double[]{2.0, 4.0});
        describeTickerState(redis, current);

        // Move back and modify
        current = redis.findTickerById(redis.getPreviousTickerId(current));
        addRhythmRules(redis, cymbal, new double[]{1.0, 3.0});
        describeTickerState(redis, current);

        // Test player modification
        logger.info("\n=== Testing Player Modifications ===");
        logger.info("Removing rules from Snare");
        for (ProxyRule rule : new HashSet<>(drums[1].getRules())) {
            redis.removeRuleFromPlayer(drums[1], rule);
            logger.info("  Removed rule: " + rule.getId());
        }

        logger.info("Moving HiHat to ticker 2");
        redis.removePlayerFromTicker(tickers[0], drums[2]);
        redis.addPlayerToTicker(tickers[1], drums[2]);

        // Verify final state
        logger.info("\n=== Verifying Final State ===");
        for (ProxyTicker ticker : tickers) {
            ProxyTicker loaded = redis.findTickerById(ticker.getId());
            logger.info("Ticker " + loaded.getId() + " has " + loaded.getPlayers().size() + " players");
            for (IPlayer p : loaded.getPlayers()) {
                ProxyStrike player = (ProxyStrike) p;
                logger.info("  Player: " + player.getName() + 
                          " (Rules: " + player.getRules().size() + ")");
            }
        }

        logger.info("\n=== Cleanup ===");
        redis.removeAllPlayers(tickers[0]);
        redis.removeAllPlayers(tickers[1]);
        redis.removeAllPlayers(tickers[2]);
        logger.info("All players removed from all tickers");
    }

    private static ProxyStrike createDrumPlayer(RedisService redis, String name, int note, ProxyTicker ticker) {
        ProxyStrike player = redis.newPlayer();
        player.setName(name);
        player.setChannel(10);  // General MIDI drum channel
        player.setNote((long) note);
        redis.addPlayerToTicker(ticker, player);
        logger.info("Created " + name + " player (ID: " + player.getId() + ")");
        return player;
    }

    private static void addRhythmRules(RedisService redis, ProxyStrike player, double[] beats) {
        logger.info("Adding " + beats.length + " rules to " + player.getName());
        for (double beat : beats) {
            ProxyRule rule = redis.newRule();
            rule.setOperator(0);  // equals
            rule.setValue(beat);
            redis.addRuleToPlayer(player, rule);
            logger.info("  Added rule for beat " + beat);
        }
    }

    private static void describeTickerState(RedisService redis, ProxyTicker ticker) {
        ProxyTicker loaded = redis.findTickerById(ticker.getId());
        logger.info("\nTicker " + loaded.getId() + ":");
        logger.info("Previous ID: " + redis.getPreviousTickerId(loaded));
        logger.info("Next ID: " + redis.getNextTickerId(loaded));
        logger.info("Players (" + loaded.getPlayers().size() + "):");
        
        for (IPlayer p : loaded.getPlayers()) {
            ProxyStrike player = (ProxyStrike) p;
            StringBuilder rules = new StringBuilder();
            for (ProxyRule rule : player.getRules()) {
                if (rules.length() > 0) rules.append(", ");
                rules.append(String.format("%.1f", rule.getValue()));
            }
            logger.info(String.format("  - %s (Note: %d, Rules: [%s])", 
                player.getName(), 
                player.getNote(),
                rules.toString()));
        }
    }
}

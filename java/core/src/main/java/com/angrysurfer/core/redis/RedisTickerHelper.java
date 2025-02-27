package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Ticker;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RedisTickerHelper {
    private static final Logger logger = Logger.getLogger(RedisTickerHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RedisPlayerHelper playerHelper;
    private final CommandBus commandBus = CommandBus.getInstance();

    public RedisTickerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.playerHelper = new RedisPlayerHelper(jedisPool, objectMapper);
    }

    public Ticker findTickerById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("ticker:" + id);
            if (json != null) {
                Ticker ticker = objectMapper.readValue(json, Ticker.class);

                // Initialize players set if null
                if (ticker.getPlayers() == null) {
                    ticker.setPlayers(new HashSet<>());
                }

                // Load players for this ticker
                Set<String> playerKeys = jedis.smembers("ticker:" + id + ":players:strike");
                if (!playerKeys.isEmpty()) {
                    playerKeys.forEach(playerId -> {
                        Player player = playerHelper.findPlayerById(Long.parseLong(playerId), "strike");
                        if (player != null) {
                            player.setTicker(ticker);
                            ticker.getPlayers().add(player);
                        }
                    });
                }

                logger.info(String.format("Loaded ticker %d with %d players", id, ticker.getPlayers().size()));
                return ticker;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding ticker: " + e.getMessage());
            throw new RuntimeException("Failed to find ticker", e);
        }
    }

    public void saveTicker(Ticker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (ticker.getId() == null) {
                ticker.setId(jedis.incr("seq:ticker"));
            }

            // Save player relationships
            String playerSetKey = "ticker:" + ticker.getId() + ":players:strike";
            jedis.del(playerSetKey); // Clear existing relationships

            if (ticker.getPlayers() != null) {
                ticker.getPlayers().forEach(player -> {
                    jedis.sadd(playerSetKey, player.getId().toString());
                });
            }

            // Temporarily remove circular references
            Set<Player> players = ticker.getPlayers();
            ticker.setPlayers(null);

            // Save ticker
            String json = objectMapper.writeValueAsString(ticker);
            jedis.set("ticker:" + ticker.getId(), json);

            // Restore references
            ticker.setPlayers(players);

            logger.info(String.format("Saved ticker %d with %d players",
                    ticker.getId(),
                    players != null ? players.size() : 0));
        } catch (Exception e) {
            logger.severe("Error saving ticker: " + e.getMessage());
            throw new RuntimeException("Failed to save ticker", e);
        }
    }

    public List<Long> getAllTickerIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                try {
                    ids.add(Long.parseLong(key.split(":")[1]));
                } catch (NumberFormatException e) {
                    logger.warning("Invalid ticker key: " + key);
                }
            }
            return ids;
        }
    }

    public Long getMinimumTickerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getMaximumTickerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public void deleteTicker(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Ticker ticker = findTickerById(tickerId);
            if (ticker == null)
                return;

            // Delete all players in the ticker
            if (ticker.getPlayers() != null) {
                ticker.getPlayers().forEach(player -> playerHelper.deletePlayer(player));
            }

            // Delete the ticker itself
            jedis.del("ticker:" + tickerId);

            // Notify via command bus
            commandBus.publish(Commands.TICKER_DELETED, this, tickerId);
            logger.info("Successfully deleted ticker " + tickerId + " and all related entities");
        } catch (Exception e) {
            logger.severe("Error deleting ticker " + tickerId + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete ticker", e);
        }
    }

    public Ticker newTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new ticker");
            Ticker ticker = new Ticker();
            ticker.setId(jedis.incr("seq:ticker"));

            // Set default values
            ticker.setTempoInBPM(120F);
            ticker.setTicksPerBeat(24);
            ticker.setBeatsPerBar(4);
            ticker.setBars(4);
            ticker.setParts(16);

            // Initialize players set
            ticker.setPlayers(new HashSet<>());

            // Save the new ticker
            saveTicker(ticker);
            logger.info("Created new ticker with ID: " + ticker.getId());
            return ticker;
        } catch (Exception e) {
            logger.severe("Error creating new ticker: " + e.getMessage());
            throw new RuntimeException("Failed to create new ticker", e);
        }
    }

    public Long getPreviousTickerId(Ticker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id < ticker.getId())
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getNextTickerId(Ticker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id > ticker.getId())
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public void clearInvalidTickers() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            for (String key : keys) {
                Ticker ticker = findTickerById(Long.parseLong(key.split(":")[1]));
                if (ticker != null && !ticker.isValid()) {
                    deleteTicker(ticker.getId());
                }
            }
        }
    }

    public boolean tickerExists(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("ticker:" + tickerId);
        }
    }

    public Ticker findFirstValidTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            for (String key : keys) {
                Ticker ticker = findTickerById(Long.parseLong(key.split(":")[1]));
                if (ticker != null && ticker.isValid()) {
                    return ticker;
                }
            }
            return null;
        }
    }
}
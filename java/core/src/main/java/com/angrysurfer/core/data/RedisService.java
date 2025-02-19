package com.angrysurfer.core.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisService implements CommandListener {
    private static final Logger logger = Logger.getLogger(RedisService.class.getName());
    private static RedisService instance;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final CommandBus commandBus = CommandBus.getInstance();

    public static RedisService getInstance() {
        if (instance == null) {
            synchronized (RedisService.class) {
                if (instance == null) {
                    instance = new RedisService();
                    logger.info("RedisService singleton instance created");
                }
            }
        }
        return instance;
    }

    private RedisService() {
        // Initialize JedisPool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);

        // Initialize ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        commandBus.register(this);
    }

    // For testing purposes
    public RedisService(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public List<ProxyInstrument> findAllInstruments() {
        List<ProxyInstrument> instruments = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> instrumentKeys = jedis.keys("proxyinstrument:*");
            for (String key : instrumentKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    ProxyInstrument instrument = objectMapper.readValue(json, ProxyInstrument.class);
                    instruments.add(instrument);
                }
            }
        } catch (Exception e) {
            logger.severe("Error finding instruments: " + e.getMessage());
        }
        return instruments;
    }

    public void saveInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                String seqKey = "seq:proxyinstrument";
                instrument.setId(jedis.incr(seqKey));
            }
            String json = objectMapper.writeValueAsString(instrument);
            jedis.set("proxyinstrument:" + instrument.getId(), json);
        } catch (Exception e) {
            logger.severe("Error saving instrument: " + e.getMessage());
        }
    }

    public FrameState loadFrameState() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("framestate");
            if (json != null) {
                return objectMapper.readValue(json, FrameState.class);
            }
        } catch (Exception e) {
            logger.severe("Error loading frame state: " + e.getMessage());
        }
        return new FrameState();
    }

    public void saveFrameState(FrameState state) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("framestate", json);
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    public boolean isDatabaseEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("*").isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking database: " + e.getMessage());
            return true;
        }
    }

    public UserConfig loadConfigFromXml(String configPath) {
        try {
            return objectMapper.readValue(new File(configPath), UserConfig.class);
        } catch (Exception e) {
            logger.severe("Error loading config: " + e.getMessage());
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
        } catch (Exception e) {
            logger.severe("Error saving config: " + e.getMessage());
        }
    }

    // Ticker methods
    public Long getMinimumTickerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getMaximumTickerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public ProxyTicker findTickerById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("ticker:" + id);
            if (json != null) {
                ProxyTicker ticker = objectMapper.readValue(json, ProxyTicker.class);
                // Initialize players collection if needed
                if (ticker.getPlayers() == null) {
                    ticker.setPlayers(new HashSet<>());
                }
                // Add each player individually to maintain proper collection behavior
                findPlayersForTicker(id).forEach(p -> ticker.getPlayers().add(p));
                ticker.setFirst(id.equals(getMinimumTickerId()));
                ticker.setLast(id.equals(getMaximumTickerId()));
                return ticker;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding ticker: " + e.getMessage());
            return null;
        }
    }

    public void addPlayerToTicker(ProxyTicker ticker, ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // First add player to ticker's collection
            if (ticker.getPlayers() == null) {
                ticker.setPlayers(new HashSet<>());
            }
            ticker.getPlayers().add(player);

            // Then save the relationship in Redis
            String tickerKey = "ticker:" + ticker.getId();
            String playersKey = tickerKey + ":players";
            jedis.sadd(playersKey, player.getId().toString());

            // Set up back reference
            player.setTicker(ticker);

            // Save both objects
            savePlayer(player);
            saveTicker(ticker);

            logger.info("Added player " + player.getId() + " to ticker " + ticker.getId() +
                    " (total players: " + ticker.getPlayers().size() + ")");
        }
    }

    public void removePlayerFromTicker(ProxyTicker ticker, ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            String tickerKey = "ticker:" + ticker.getId();
            String playersKey = tickerKey + ":players";
            jedis.srem(playersKey, player.getId().toString());
            player.setTicker(null); // Update to use ProxyStrike's setTicker method
            savePlayer(player);
            saveTicker(ticker);
        }
    }

    public void removeAllPlayers(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            String tickerKey = "ticker:" + ticker.getId();
            String playersKey = tickerKey + ":players";
            Set<String> playerIds = jedis.smembers(playersKey);
            for (String playerId : playerIds) {
                ProxyStrike player = findPlayerById(Long.valueOf(playerId));
                if (player != null) {
                    player.setTicker(null);
                    savePlayer(player);
                }
            }
            jedis.del(playersKey);
            saveTicker(ticker);
        }
    }

    public ProxyTicker newTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            ProxyTicker ticker = new ProxyTicker();
            ticker.setId(jedis.incr("seq:ticker"));
            ticker.setPlayers(new HashSet<>()); // Initialize players collection
            saveTicker(ticker);
            return ticker;
        }
    }

    // Player methods
    public ProxyStrike findPlayerById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("proxystrike:" + id); // Update key prefix
            if (json != null) {
                ProxyStrike player = objectMapper.readValue(json, ProxyStrike.class);
                player.setRules(findRulesForPlayer(id));
                return player;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding player: " + e.getMessage());
            return null;
        }
    }

    public Set<ProxyStrike> findPlayersForTicker(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<ProxyStrike> players = new HashSet<>();
            String playersKey = "ticker:" + tickerId + ":players";
            Set<String> playerIds = jedis.smembers(playersKey);
            for (String id : playerIds) {
                ProxyStrike player = findPlayerById(Long.valueOf(id));
                if (player != null) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    public ProxyStrike newPlayer() {
        try (Jedis jedis = jedisPool.getResource()) {
            ProxyStrike player = new ProxyStrike();
            player.setId(jedis.incr("seq:proxystrike"));
            player.setRules(new HashSet<>()); // Ensure rules are initialized
            savePlayer(player);
            return player;
        }
    }

    public void savePlayer(ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            ProxyTicker ticker = player.getTicker();
            Set<ProxyRule> rules = new HashSet<>(player.getRules() != null ? player.getRules() : new HashSet<>());

            // Temporarily clear references to avoid circular dependencies
            player.setTicker(null);
            player.setRules(null);

            String json = objectMapper.writeValueAsString(player);
            jedis.set("proxystrike:" + player.getId(), json);

            // Restore references
            player.setTicker(ticker);
            player.setRules(rules);
        } catch (Exception e) {
            logger.severe("Error saving player: " + e.getMessage());
            throw new RuntimeException("Failed to save player", e);
        }
    }

    public void deletePlayer(ProxyStrike player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Delete player's rules
            String rulesKey = "player:" + player.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);
            for (String ruleId : ruleIds) {
                deleteRule(Long.valueOf(ruleId));
            }
            jedis.del(rulesKey);

            // Remove from ticker if assigned
            if (player.getTicker() != null) {
                jedis.srem("ticker:" + player.getTicker().getId() + ":players", player.getId().toString());
            }

            // Delete player
            jedis.del("proxystrike:" + player.getId()); // Update key prefix
        }
    }

    // Rule methods
    public ProxyRule newRule() {
        try (Jedis jedis = jedisPool.getResource()) {
            ProxyRule rule = new ProxyRule();
            rule.setId(jedis.incr("seq:proxyrule"));
            saveRule(rule);
            return rule;
        }
    }

    public ProxyRule findRuleById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("proxyrule:" + id);
            return json != null ? objectMapper.readValue(json, ProxyRule.class) : null;
        } catch (Exception e) {
            logger.severe("Error finding rule: " + e.getMessage());
            return null;
        }
    }

    public Set<ProxyRule> findRulesForPlayer(Long playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<ProxyRule> rules = new HashSet<>();
            String rulesKey = "player:" + playerId + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);
            for (String id : ruleIds) {
                ProxyRule rule = findRuleById(Long.valueOf(id));
                if (rule != null) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    public void addRuleToPlayer(ProxyStrike player, ProxyRule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            saveRule(rule);
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.sadd(rulesKey, rule.getId().toString());
            if (player.getRules() == null) {
                player.setRules(new HashSet<>());
            }
            player.getRules().add(rule);
        }
    }

    public void removeRuleFromPlayer(ProxyStrike player, ProxyRule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.srem(rulesKey, rule.getId().toString());
            if (player.getRules() != null) {
                player.getRules().remove(rule);
            }
            deleteRule(rule.getId());
        }
    }

    public void saveRule(ProxyRule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(rule);
            jedis.set("proxyrule:" + rule.getId(), json);
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            throw new RuntimeException("Failed to save rule", e);
        }
    }

    private void deleteRule(Long ruleId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("rule:" + ruleId);
        }
    }

    public ProxyStrike findPlayerForRule(ProxyRule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("proxystrike:*");
            for (String playerKey : playerKeys) {
                String playerId = playerKey.split(":")[1];
                String rulesKey = "player:" + playerId + ":rules";
                if (jedis.sismember(rulesKey, rule.getId().toString())) {
                    return findPlayerById(Long.valueOf(playerId));
                }
            }
        }
        return null;
    }

    // Navigation methods
    public Long getPreviousTickerId(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id < ticker.getId())
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getNextTickerId(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id > ticker.getId())
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    private void saveTicker(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Make a copy of players before clearing
            Set<IProxyPlayer> players = new HashSet<>(ticker.getPlayers());

            // Clear players to avoid circular reference
            ticker.setPlayers(null);

            // Save ticker
            String json = objectMapper.writeValueAsString(ticker);
            jedis.set("ticker:" + ticker.getId(), json);

            // Restore players
            ticker.setPlayers(players);

            logger.info("Saved ticker " + ticker.getId() +
                    " with " + players.size() + " players");
        } catch (Exception e) {
            logger.severe("Error saving ticker: " + e.getMessage());
            throw new RuntimeException("Failed to save ticker", e);
        }
    }

    @Override
    public void onAction(Command action) {
        if (Commands.CLEAR_DATABASE.equals(action.getCommand())) {
            try (Jedis jedis = jedisPool.getResource()) {
                // Clear database
                jedis.flushDB();
                logger.info("Database cleared");

                // Create initial ticker
                ProxyTicker ticker = newTicker();
                logger.info("Created initial ticker with ID: " + ticker.getId());

                // Publish event
                Command cmd = new Command(Commands.TICKER_LOADED, this, ticker);
                CommandBus.getInstance().publish(cmd);
            } catch (Exception e) {
                logger.severe("Error clearing database: " + e.getMessage());
            }
        }
    }
}

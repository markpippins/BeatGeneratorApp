package com.angrysurfer.core.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.util.LogManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;

public class RedisService implements CommandListener {
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisService() {
        // Initialize JedisPool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);

        // Initialize ObjectMapper with proper interface handling
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        
        // Configure proper handling of IProxyPlayer interface
        objectMapper.addMixIn(IProxyPlayer.class, ProxyStrike.class);
        objectMapper.enableDefaultTyping(); // Add this line for interface handling
        
        CommandBus.getInstance().register(this);
    }

    // For testing purposes
    public RedisService(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public List<Strike> findAllPlayers() {
        List<Strike> players = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("strike:*");
            for (String key : playerKeys) {
                String json = jedis.get(key);
                Strike player = objectMapper.readValue(json, Strike.class);
                players.add(player);
            }
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all players", e);
        }
        return players;
    }

    public List<ProxyRule> findRulesByPlayer(IProxyPlayer player) {
        List<ProxyRule> rules = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String rulesKey = "player:" + player.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);

            for (String ruleId : ruleIds) {
                String ruleKey = getKey(ProxyRule.class, Long.valueOf(ruleId));
                String json = jedis.get(ruleKey);
                if (json != null) {
                    ProxyRule rule = objectMapper.readValue(json, ProxyRule.class);
                    rules.add(rule);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding rules for player: " + player.getId(), e);
        }
        return rules;
    }

    public ProxyRule saveRule(ProxyRule rule, IProxyPlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (rule.getId() == null) {
                String seqKey = "seq:" + ProxyRule.class.getSimpleName().toLowerCase();
                rule.setId(jedis.incr(seqKey));
            }

            // Save the rule
            String json = objectMapper.writeValueAsString(rule);
            String ruleKey = getKey(ProxyRule.class, rule.getId());
            jedis.set(ruleKey, json);

            // Add to player's rules set
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.sadd(rulesKey, rule.getId().toString());

            return rule;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving rule", e);
            throw new RuntimeException("Error saving rule", e);
        }
    }

    public void deleteRule(ProxyRule rule, IProxyPlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove from player's rules set
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.srem(rulesKey, rule.getId().toString());

            // Delete the rule itself
            String ruleKey = getKey(ProxyRule.class, rule.getId());
            jedis.del(ruleKey);
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error deleting rule", e);
            throw new RuntimeException("Error deleting rule", e);
        }
    }

    public void deletePlayerFromTicker(ProxyStrike player, ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove player from ticker's player set
            String playersKey = getKey(ProxyTicker.class, ticker.getId()) + ":players";
            jedis.srem(playersKey, player.getId().toString());

            // Delete all rules associated with this player
            String playerRulesKey = "player:" + player.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(playerRulesKey);
            for (String ruleId : ruleIds) {
                jedis.del(getKey(ProxyRule.class, Long.valueOf(ruleId)));
            }
            jedis.del(playerRulesKey);

            // Delete the player itself
            String playerKey = getKey(ProxyStrike.class, player.getId());
            jedis.del(playerKey);

            LogManager.getInstance().info("RedisService",
                    "Deleted player " + player.getName() + " from ticker " + ticker.getId());
        }
    }

    public List<ProxyTicker> findAllTickers() {
        List<ProxyTicker> tickers = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> tickerKeys = jedis.keys("proxyticker:*");
            LogManager.getInstance().info("RedisService", "Found ticker keys: " + tickerKeys);

            for (String key : tickerKeys) {
                try {
                    // Check key type before getting
                    String type = jedis.type(key);
                    LogManager.getInstance().info("RedisService", "Key " + key + " is of type: " + type);

                    if (!"string".equals(type)) {
                        LogManager.getInstance().warn("RedisService", 
                            "Removing invalid key " + key + " of type " + type);
                        jedis.del(key);
                        continue;
                    }

                    String json = jedis.get(key);
                    if (json != null) {
                        ProxyTicker ticker = objectMapper.readValue(json, ProxyTicker.class);
                        
                        // Initialize players set
                        if (ticker.getPlayers() == null) {
                            ticker.setPlayers(new HashSet<>());
                        }

                        // Load associated players
                        String playersKey = key + ":players";
                        Set<String> playerIds = jedis.smembers(playersKey);
                        LogManager.getInstance().info("RedisService", 
                            "Found " + playerIds.size() + " players for ticker " + ticker.getId());

                        for (String playerId : playerIds) {
                            String playerKey = getKey(ProxyStrike.class, Long.valueOf(playerId));
                            String playerJson = jedis.get(playerKey);
                            if (playerJson != null) {
                                ProxyStrike player = objectMapper.readValue(playerJson, ProxyStrike.class);
                                player.setTicker(ticker);
                                ticker.getPlayers().add(player);
                                LogManager.getInstance().info("RedisService", 
                                    "Loaded player: " + player.getId() + " for ticker " + ticker.getId());
                            }
                        }

                        tickers.add(ticker);
                        LogManager.getInstance().info("RedisService", 
                            "Added ticker " + ticker.getId() + " with " + 
                            ticker.getPlayers().size() + " players");
                    }
                } catch (Exception e) {
                    LogManager.getInstance().error("RedisService", 
                        "Error processing ticker key " + key + ": " + e.getMessage());
                    // Try to clean up invalid key
                    try {
                        jedis.del(key);
                    } catch (Exception ex) {
                        // Ignore cleanup errors
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all tickers: " + e.getMessage());
        }
        return tickers;
    }

    private void cleanupOrphanedStrikes(Jedis jedis) {
        Set<String> allStrikeKeys = jedis.keys("proxystrike:*");
        Set<String> validPlayers = new HashSet<>();

        // Get all valid players from all tickers
        Set<String> tickerKeys = jedis.keys("proxyticker:*");
        for (String tickerKey : tickerKeys) {
            if (!tickerKey.endsWith(":players")) {
                String playersKey = tickerKey + ":players";
                validPlayers.addAll(jedis.smembers(playersKey));
            }
        }

        // Delete orphaned strikes
        for (String strikeKey : allStrikeKeys) {
            String playerId = strikeKey.split(":")[1];
            if (!validPlayers.contains(playerId)) {
                LogManager.getInstance().info("RedisService", "Deleting orphaned strike: " + strikeKey);
                jedis.del(strikeKey);
            }
        }
    }

    private void initializeTickerPlayers(ProxyTicker ticker, Jedis jedis) {
        if (ticker == null) return;
        
        ticker.setPlayers(new HashSet<>());
        String playersKey = getKey(ProxyTicker.class, ticker.getId()) + ":players";
        
        // Get the player IDs for this ticker
        Set<String> playerIds = jedis.smembers(playersKey);
        LogManager.getInstance().info("RedisService", 
            "Loading " + playerIds.size() + " players for ticker " + ticker.getId());

        // Load each player
        for (String playerId : playerIds) {
            String playerKey = getKey(ProxyStrike.class, Long.valueOf(playerId));
            String playerJson = jedis.get(playerKey);
            
            if (playerJson != null) {
                try {
                    ProxyStrike player = objectMapper.readValue(playerJson, ProxyStrike.class);
                    player.setTicker(ticker);
                    ticker.getPlayers().add(player);
                    LogManager.getInstance().info("RedisService", 
                        "Loaded player: " + player.getName() + " (ID: " + player.getId() + 
                        ") for ticker: " + ticker.getId());
                } catch (Exception e) {
                    LogManager.getInstance().error("RedisService", 
                        "Error loading player " + playerId + ": " + e.getMessage());
                    // Remove invalid player ID from set
                    jedis.srem(playersKey, playerId);
                }
            } else {
                // Remove missing player ID from set
                LogManager.getInstance().warn("RedisService", 
                    "Removing missing player " + playerId + " from ticker " + ticker.getId());
                jedis.srem(playersKey, playerId);
            }
        }
    }

    public ProxyTicker loadTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Clean up orphaned strikes first
            cleanupOrphanedStrikes(jedis);
            
            // Then load the ticker
            String activeTickerId = jedis.get("active:ticker");
            ProxyTicker ticker = null;
            
            if (activeTickerId != null) {
                ticker = loadTickerById(jedis, Long.valueOf(activeTickerId));
            }
            
            if (ticker == null) {
                ticker = createDefaultTicker();
            }
            
            return ticker;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error loading ticker", e);
            return createDefaultTicker();
        }
    }

    private ProxyTicker loadTickerById(Jedis jedis, Long tickerId) {
        try {
            String tickerKey = getKey(ProxyTicker.class, tickerId);
            
            // Log all keys for debugging
            LogManager.getInstance().info("RedisService", 
                "All keys in database: " + jedis.keys("*"));
            
            String tickerJson = jedis.get(tickerKey);
            LogManager.getInstance().info("RedisService", 
                "Loading ticker " + tickerId + " JSON: " + tickerJson);

            if (tickerJson != null) {
                ProxyTicker ticker = objectMapper.readValue(tickerJson, ProxyTicker.class);
                ticker.setPlayers(new HashSet<>()); // Clear any deserialized players

                // Get player IDs from Redis set
                String playersKey = tickerKey + ":players";
                Set<String> playerIds = jedis.smembers(playersKey);
                LogManager.getInstance().info("RedisService", 
                    "Found " + playerIds.size() + " player IDs for ticker " + tickerId + ": " + playerIds);

                // Load each player
                for (String playerId : playerIds) {
                    String playerKey = getKey(ProxyStrike.class, Long.valueOf(playerId));
                    String playerJson = jedis.get(playerKey);
                    LogManager.getInstance().info("RedisService", 
                        "Loading player " + playerId + ": " + playerJson);
                    
                    if (playerJson != null) {
                        ProxyStrike player = objectMapper.readValue(playerJson, ProxyStrike.class);
                        player.setTicker(ticker);
                        ticker.getPlayers().add(player);
                        LogManager.getInstance().info("RedisService", 
                            "Added player " + player.getName() + " (ID: " + player.getId() + ") to ticker");
                    }
                }

                return ticker;
            }
            return null;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error loading ticker", e);
            return null;
        }
    }

    public ProxyTicker createDefaultTicker() {
        ProxyTicker ticker = new ProxyTicker();
        ticker.setTempoInBPM(120.0f);
        ticker.setBars(4);
        ticker.setBeatsPerBar(4);
        ticker.setTicksPerBeat(24);
        ticker.setParts(1);
        ticker.setPartLength(4L);
        ticker.setPlayers(new HashSet<>());
        return saveTicker(ticker);
    }

    public ProxyTicker saveTicker(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Ensure we have a ticker ID
            if (ticker.getId() == null) {
                String seqKey = "seq:proxyticker";
                ticker.setId(jedis.incr(seqKey));
            }

            // Clear existing state
            String tickerKey = getKey(ProxyTicker.class, ticker.getId());
            String playersKey = tickerKey + ":players";
            
            // Log current state
            LogManager.getInstance().info("RedisService", 
                "Saving ticker " + ticker.getId() + " with " + 
                (ticker.getPlayers() != null ? ticker.getPlayers().size() : 0) + " players");

            // Save ticker without players field to avoid duplication
            HashSet<IProxyPlayer> players = new HashSet<>(ticker.getPlayers());
            ticker.setPlayers(null); // Temporarily clear players
            String tickerJson = objectMapper.writeValueAsString(ticker);
            jedis.set(tickerKey, tickerJson);
            ticker.setPlayers(players); // Restore players

            // Update player relationships
            jedis.del(playersKey); // Clear existing relationships
            if (!players.isEmpty()) {
                for (IProxyPlayer player : players) {
                    if (player.getId() != null) {
                        jedis.sadd(playersKey, player.getId().toString());
                        LogManager.getInstance().info("RedisService", 
                            "Added player " + player.getId() + " to ticker " + ticker.getId());
                    }
                }
            }

            return ticker;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving ticker: " + e.getMessage());
            throw new RuntimeException("Error saving ticker", e);
        }
    }

    public ProxyStrike saveStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Debug existing strikes
            Set<String> existingStrikes = jedis.keys("proxystrike:*");
            LogManager.getInstance().info("RedisService", 
                "Existing strikes before save: " + existingStrikes);

            // Generate new ID if needed
            if (strike.getId() == null) {
                String seqKey = "seq:proxystrike";
                Long newId = jedis.incr(seqKey);
                strike.setId(newId);
                LogManager.getInstance().info("RedisService", 
                    "Generated new strike ID: " + newId + " from sequence " + seqKey);
            }

            // Get active ticker info
            String activeTickerId = getActiveTickerId();
            LogManager.getInstance().info("RedisService", 
                "Saving strike " + strike.getId() + " with ticker " + 
                (strike.getTicker() != null ? strike.getTicker().getId() : "null") + 
                ", active ticker is " + activeTickerId);

            // Save the strike object
            String json = objectMapper.writeValueAsString(strike);
            String strikeKey = getKey(ProxyStrike.class, strike.getId());
            jedis.set(strikeKey, json);

            // Update ticker-player relationship
            if (strike.getTicker() != null) {
                String tickerPlayersKey = getKey(ProxyTicker.class, strike.getTicker().getId()) + ":players";
                
                // Get existing players for this ticker
                Set<String> existingPlayers = jedis.smembers(tickerPlayersKey);
                LogManager.getInstance().info("RedisService", 
                    "Existing players for ticker " + strike.getTicker().getId() + 
                    ": " + existingPlayers);

                // Add the player ID to the set
                jedis.sadd(tickerPlayersKey, strike.getId().toString());
                LogManager.getInstance().info("RedisService", 
                    "Added player " + strike.getId() + " to ticker " + 
                    strike.getTicker().getId() + "'s player set");
            }

            return strike;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving strike", e);
            throw new RuntimeException("Error saving strike", e);
        }
    }

    public void deleteStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            // First remove from all ticker:player relationships
            Set<String> tickerKeys = jedis.keys("proxyticker:*");
            for (String tickerKey : tickerKeys) {
                if (!tickerKey.endsWith(":players")) {
                    String playersKey = tickerKey + ":players";
                    jedis.srem(playersKey, strike.getId().toString());
                }
            }

            // Delete the strike
            String strikeKey = getKey(ProxyStrike.class, strike.getId());
            jedis.del(strikeKey);

            // Clean up rules
            String rulesKey = "player:" + strike.getId() + ":rules";
            jedis.del(rulesKey);

            LogManager.getInstance().info("RedisService", 
                "Deleted strike " + strike.getId() + " and all relationships");
        }
    }

    public List<ProxyStrike> findAllStrikes() {
        List<ProxyStrike> strikes = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> strikeKeys = jedis.keys("proxystrike:*");
            for (String key : strikeKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    ProxyStrike strike = objectMapper.readValue(json, ProxyStrike.class);
                    strikes.add(strike);
                }
            }
            LogManager.getInstance().info("RedisService", "Found " + strikes.size() + " strikes");
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all strikes", e);
        }
        return strikes;
    }

    public List<ProxyInstrument> findAllInstruments() {
        List<ProxyInstrument> instruments = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            // First, check if the key pattern exists and is of the right type
            Set<String> instrumentKeys = jedis.keys("proxyinstrument:*");

            // Debug logging
            LogManager.getInstance().info("RedisService",
                    "Found " + instrumentKeys.size() + " instrument keys");

            for (String key : instrumentKeys) {
                try {
                    String type = jedis.type(key);
                    LogManager.getInstance().info("RedisService",
                            "Key: " + key + " is of type: " + type);

                    if ("string".equals(type)) {
                        String json = jedis.get(key);
                        if (json != null) {
                            ProxyInstrument instrument = objectMapper.readValue(json, ProxyInstrument.class);
                            instruments.add(instrument);
                            LogManager.getInstance().info("RedisService",
                                    "Loaded instrument: " + instrument.getName());
                        }
                    } else {
                        // Wrong type found, delete the key
                        // LogManager.getInstance().warning("RedisService",
                        // "Found wrong type for key " + key + ", deleting it");
                        jedis.del(key);
                    }
                } catch (Exception e) {
                    LogManager.getInstance().error("RedisService",
                            "Error processing key " + key + ": " + e.getMessage());
                }
            }

            // If no instruments found, create default one
            if (instruments.isEmpty()) {
                ProxyInstrument defaultInst = createDefaultInstrument();
                instruments.add(defaultInst);
                saveInstrument(defaultInst);
            }

        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all instruments", e);
        }
        return instruments;
    }

    private ProxyInstrument createDefaultInstrument() {
        ProxyInstrument instrument = new ProxyInstrument();
        instrument.setName("Default Instrument");
        instrument.setDeviceName("Microsoft GS Wavetable Synth");
        // instrument.setDescription("Default MIDI instrument");
        return instrument;
    }

    public void saveInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                String seqKey = "seq:" + ProxyInstrument.class.getSimpleName().toLowerCase();
                instrument.setId(jedis.incr(seqKey));
            }

            String json = objectMapper.writeValueAsString(instrument);
            String instrumentKey = getKey(ProxyInstrument.class, instrument.getId());

            // Check if key exists and is wrong type before setting
            String type = jedis.type(instrumentKey);
            if (!"none".equals(type) && !"string".equals(type)) {
                jedis.del(instrumentKey);
            }

            jedis.set(instrumentKey, json);
            LogManager.getInstance().info("RedisService",
                    "Saved instrument: " + instrument.getName() + " with ID: " + instrument.getId());
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving instrument", e);
            throw new RuntimeException("Error saving instrument", e);
        }
    }

    public void deleteInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            String instrumentKey = getKey(ProxyInstrument.class, instrument.getId());
            jedis.del(instrumentKey);
            LogManager.getInstance().info("RedisService", "Deleted instrument: " + instrument.getName());
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error deleting instrument", e);
            throw new RuntimeException("Error deleting instrument", e);
        }
    }

    public FrameState loadFrameState() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("framestate");
            if (json != null) {
                return objectMapper.readValue(json, FrameState.class);
            }
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error loading frame state", e);
        }
        return new FrameState(); // Return default state if none exists
    }

    public void saveFrameState(FrameState state) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("framestate", json);
            LogManager.getInstance().info("RedisService", "Saved frame state");
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving frame state", e);
            throw new RuntimeException("Error saving frame state", e);
        }
    }

    public boolean isDatabaseEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("*").isEmpty();
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error checking database state", e);
            return true;
        }
    }

    private String getKey(Class<?> cls, Long id) {
        return cls.getSimpleName().toLowerCase() + ":" + id;
    }

    public UserConfig loadConfigFromXml(String configPath) {
        try {
            return objectMapper.readValue(new File(configPath), UserConfig.class);
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error loading config from XML", e);
            throw new RuntimeException("Failed to load config from XML", e);
        }
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
            LogManager.getInstance().info("RedisService", "Saved user configuration");
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving config", e);
            throw new RuntimeException("Error saving config", e);
        }
    }

    public String getActiveTickerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            String id = jedis.get("active:ticker");
            LogManager.getInstance().info("RedisService", "Got active ticker ID: " + id);
            return id;
        }
    }

    public void setActiveTickerId(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("active:ticker", tickerId.toString());
            LogManager.getInstance().info("RedisService", "Set active ticker ID to: " + tickerId);
        }
    }

    @Override
    public void onAction(Command action) {
        if (Commands.CLEAR_DATABASE.equals(action.getCommand())) {
            clearDatabase();
        }
    }

    public void clearDatabase() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Delete all keys
            String result = jedis.flushDB();
            LogManager.getInstance().info("RedisService", "Database cleared: " + result);

            // Reset all sequences
            jedis.del("seq:proxyticker");
            jedis.del("seq:proxystrike");
            jedis.del("seq:proxyrule");
            jedis.del("seq:proxyinstrument");
            LogManager.getInstance().info("RedisService", "All sequences reset");

            // Create new ticker and publish
            Command cmd = new Command();
            cmd.setCommand(Commands.DATABASE_RESET);
            cmd.setData(loadTicker());
            CommandBus.getInstance().publish(cmd);
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error clearing database", e);
            throw new RuntimeException("Error clearing database", e);
        }
    }
}

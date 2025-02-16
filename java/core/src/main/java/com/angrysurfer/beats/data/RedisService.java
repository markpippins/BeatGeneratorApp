package com.angrysurfer.beats.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.angrysurfer.beats.LogManager;
import com.angrysurfer.core.model.player.Strike;
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

public class RedisService {
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisService() {
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
            for (String key : tickerKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    ProxyTicker ticker = objectMapper.readValue(json, ProxyTicker.class);
                    
                    // Load associated players for each ticker
                    String playersKey = getKey(ProxyTicker.class, ticker.getId()) + ":players";
                    Set<String> playerIds = jedis.smembers(playersKey);
                    Set<IProxyPlayer> players = new HashSet<>();
                    
                    for (String playerId : playerIds) {
                        String playerJson = jedis.get(getKey(ProxyStrike.class, Long.valueOf(playerId)));
                        if (playerJson != null) {
                            ProxyStrike player = objectMapper.readValue(playerJson, ProxyStrike.class);
                            players.add(player); // ProxyStrike implements IProxyPlayer, so this is valid
                        }
                    }
                    
                    ticker.setPlayers(players);
                    tickers.add(ticker);
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all tickers", e);
        }
        return tickers;
    }

    public ProxyTicker loadTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Try to get the active ticker ID
            String activeTickerKey = "active:ticker";
            String tickerId = jedis.get(activeTickerKey);
            
            if (tickerId != null) {
                // Load the ticker
                String tickerKey = getKey(ProxyTicker.class, Long.valueOf(tickerId));
                String json = jedis.get(tickerKey);
                if (json != null) {
                    ProxyTicker ticker = objectMapper.readValue(json, ProxyTicker.class);
                    
                    // Load associated players
                    String playersKey = getKey(ProxyTicker.class, ticker.getId()) + ":players";
                    Set<String> playerIds = jedis.smembers(playersKey);
                    Set<IProxyPlayer> players = new HashSet<>();
                    
                    for (String playerId : playerIds) {
                        String playerJson = jedis.get(getKey(ProxyStrike.class, Long.valueOf(playerId)));
                        if (playerJson != null) {
                            players.add(objectMapper.readValue(playerJson, ProxyStrike.class));
                        }
                    }
                    
                    ticker.setPlayers(players);
                    return ticker;
                }
            }
            
            // If no active ticker found, create a new one
            return createDefaultTicker();
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error loading active ticker", e);
            return createDefaultTicker();
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
        ticker.setPlayers(new HashSet<>());
        return saveTicker(ticker);
    }

    public ProxyTicker saveTicker(ProxyTicker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (ticker.getId() == null) {
                String seqKey = "seq:" + ProxyTicker.class.getSimpleName().toLowerCase();
                ticker.setId(jedis.incr(seqKey));
            }

            String json = objectMapper.writeValueAsString(ticker);
            String tickerKey = getKey(ProxyTicker.class, ticker.getId());
            jedis.set(tickerKey, json);
            
            // Set as active ticker
            jedis.set("active:ticker", ticker.getId().toString());

            LogManager.getInstance().info("RedisService", "Saved ticker: " + ticker.getId());
            return ticker;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving ticker", e);
            throw new RuntimeException("Error saving ticker", e);
        }
    }

    public ProxyStrike saveStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (strike.getId() == null) {
                String seqKey = "seq:" + ProxyStrike.class.getSimpleName().toLowerCase();
                strike.setId(jedis.incr(seqKey));
            }

            String json = objectMapper.writeValueAsString(strike);
            String strikeKey = getKey(ProxyStrike.class, strike.getId());
            jedis.set(strikeKey, json);

            // If this strike belongs to a ticker, update the ticker's player set
            if (strike.getTicker() != null) {
                String tickerPlayersKey = getKey(ProxyTicker.class, strike.getTicker().getId()) + ":players";
                jedis.sadd(tickerPlayersKey, strike.getId().toString());
            }

            LogManager.getInstance().info("RedisService", "Saved strike: " + strike.getName());
            return strike;
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error saving strike", e);
            throw new RuntimeException("Error saving strike", e);
        }
    }

    public void deleteStrike(ProxyStrike strike) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Delete player from Redis
            String playerKey = getKey(ProxyStrike.class, strike.getId());
            jedis.del(playerKey);
            
            // Delete all rules associated with this player
            String playerRulesKey = "player:" + strike.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(playerRulesKey);
            for (String ruleId : ruleIds) {
                jedis.del(getKey(ProxyRule.class, Long.valueOf(ruleId)));
            }
            jedis.del(playerRulesKey);
            
            // If strike belongs to a ticker, remove from ticker's player set
            if (strike.getTicker() != null) {
                String tickerPlayersKey = getKey(ProxyTicker.class, strike.getTicker().getId()) + ":players";
                jedis.srem(tickerPlayersKey, strike.getId().toString());
            }

            LogManager.getInstance().info("RedisService", "Deleted strike: " + strike.getName());
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error deleting strike", e);
            throw new RuntimeException("Error deleting strike", e);
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
            Set<String> instrumentKeys = jedis.keys("proxyinstrument:*");
            for (String key : instrumentKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    ProxyInstrument instrument = objectMapper.readValue(json, ProxyInstrument.class);
                    instruments.add(instrument);
                }
            }
            LogManager.getInstance().info("RedisService", "Found " + instruments.size() + " instruments");
        } catch (Exception e) {
            LogManager.getInstance().error("RedisService", "Error finding all instruments", e);
        }
        return instruments;
    }

    public void saveInstrument(ProxyInstrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                String seqKey = "seq:" + ProxyInstrument.class.getSimpleName().toLowerCase();
                instrument.setId(jedis.incr(seqKey));
            }

            String json = objectMapper.writeValueAsString(instrument);
            String instrumentKey = getKey(ProxyInstrument.class, instrument.getId());
            jedis.set(instrumentKey, json);
            
            LogManager.getInstance().info("RedisService", "Saved instrument: " + instrument.getName());
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
}
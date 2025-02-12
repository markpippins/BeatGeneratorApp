package com.angrysurfer.beatsui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;

import com.angrysurfer.beatsui.mock.AbstractPlayer;
import com.angrysurfer.beatsui.mock.Rule;
import com.angrysurfer.beatsui.mock.Strike;
import com.angrysurfer.beatsui.mock.Ticker;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Operator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisService {
    private final JedisPool jedisPool;
    private final ObjectMapper mapper;
    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    public RedisService(String host, int port) {
        this.jedisPool = new JedisPool(host, port);
        this.mapper = new ObjectMapper();
    }

    public void saveTicker(Ticker ticker) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "ticker:" + ticker.getId().toString();
            jedis.hmset(key, convertToMap(ticker));
        } catch (Exception e) {
            logger.error("Error saving ticker: {}", e.getMessage());
        }
    }

    public void savePlayer(AbstractPlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a simplified version of the player for storage
            Map<String, String> playerData = new HashMap<>();
            playerData.put("id", player.getId().toString());
            playerData.put("name", player.getName());
            playerData.put("note", player.getNote().toString());
            playerData.put("channel", String.valueOf(player.getChannel()));
            playerData.put("playerClass", player.getClass().getName());
            // Add other essential fields...

            String key = "player:" + player.getId().toString();
            jedis.hmset(key, playerData);
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage());
        }
    }

    public void saveRule(Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "rule:" + rule.getId().toString();
            jedis.hmset(key, convertToMap(rule));
            // Index for player lookup
            jedis.sadd("player:" + rule.getPlayerId().toString() + ":rules", rule.getId().toString());
        } catch (Exception e) {
            logger.error("Error saving rule: {}", e.getMessage());
        }
    }

    public Optional<Ticker> getTicker(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll("ticker:" + id.toString());
            return Optional.ofNullable(convertFromMap(data, Ticker.class));
        } catch (Exception e) {
            logger.error("Error retrieving ticker: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<AbstractPlayer> getPlayer(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll("player:" + id.toString());
            if (data.isEmpty()) {
                return Optional.empty();
            }

            // Create appropriate player type based on stored class name
            String className = data.get("playerClass");
            Class<?> playerClass = Class.forName(className);
            AbstractPlayer player = (AbstractPlayer) playerClass.getDeclaredConstructor().newInstance();
            
            // Set basic properties
            player.setId(Long.parseLong(data.get("id")));
            player.setName(data.get("name"));
            player.setNote(Long.parseLong(data.get("note")));
            player.setChannel(Integer.parseInt(data.get("channel")));
            // Set other properties...

            return Optional.of(player);
        } catch (Exception e) {
            logger.error("Error retrieving player: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public List<Rule> getRulesForPlayer(Long playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> ruleIds = jedis.smembers("player:" + playerId.toString() + ":rules");
            List<Rule> rules = new ArrayList<>();
            for (String ruleId : ruleIds) {
                Map<String, String> data = jedis.hgetAll("rule:" + ruleId);
                rules.add(convertFromMap(data, Rule.class));
            }
            return rules;
        } catch (Exception e) {
            logger.error("Error retrieving rules for player: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public void deletePlayer(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "player:" + id.toString();
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting player: {}", e.getMessage());
        }
    }

    public void deleteRule(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "rule:" + id.toString();
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting rule: {}", e.getMessage());
        }
    }

    public void deleteTicker(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "ticker:" + id.toString();
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting ticker: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        RedisService redisService = new RedisService("localhost", 6379);

        try {
            // Create and save a ticker
            Ticker ticker = new Ticker();
            ticker.setId(1L);
            ticker.setTempoInBPM(120.0f);
            redisService.saveTicker(ticker);

            // Create and save a player
            Strike kick = new Strike();
            kick.setId(1L);
            kick.setName("Kick");
            kick.setNote(Strike.KICK);
            redisService.savePlayer(kick);

            // Create and save a rule
            Rule rule = new Rule();
            rule.setId(1L);
            rule.setPlayerId(kick.getId());
            rule.setOperator(Operator.BEAT);
            rule.setComparison(Comparison.EQUALS);
            rule.setValue(1.0);
            redisService.saveRule(rule);

            // Retrieve and verify with null checks
            Optional<Ticker> savedTicker = redisService.getTicker(1L);
            Optional<AbstractPlayer> savedPlayer = redisService.getPlayer(1L);
            List<Rule> playerRules = redisService.getRulesForPlayer(1L);

            savedTicker.ifPresent(t -> System.out.println("Ticker BPM: " + t.getTempoInBPM()));
            savedPlayer.ifPresent(p -> System.out.println("Player name: " + p.getName()));
            System.out.println("Rules for player: " + playerRules.size());

            if (savedTicker.isEmpty()) {
                System.out.println("Warning: Ticker was not found");
            }
            if (savedPlayer.isEmpty()) {
                System.out.println("Warning: Player was not found");
            }

        } catch (Exception e) {
            logger.error("Error in main: {}", e.getMessage(), e);
        }
    }

    private Map<String, String> convertToMap(Object obj) {
        try {
            // First convert to JSON string
            String json = mapper.writeValueAsString(obj);
            // Then convert back to Map
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            logger.error("Error converting object to map: {}", e.getMessage());
            throw new RuntimeException("Failed to convert object to map", e);
        }
    }

    private <T> T convertFromMap(Map<String, String> map, Class<T> clazz) {
        try {
            // First convert map to JSON string
            String json = mapper.writeValueAsString(map);
            // Then convert to target class
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("Error converting map to object: {}", e.getMessage());
            throw new RuntimeException("Failed to convert map to object", e);
        }
    }
}

package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.Ticker;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RedisPlayerHelper {
    private static final Logger logger = Logger.getLogger(RedisPlayerHelper.class.getName());

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RedisRuleHelper ruleHelper;

    public RedisPlayerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.ruleHelper = new RedisRuleHelper(jedisPool, objectMapper);
    }

    private String getPlayerKey(String className, Long id) {
        return String.format("player:%s:%d", className.toLowerCase(), id);
    }

    public Player findPlayerById(Long id, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(getPlayerKey(className, id));
            if (json != null) {
                Strike player = objectMapper.readValue(json, Strike.class);

                // Load rules for this player
                Set<String> ruleIds = jedis.smembers("player:" + id + ":rules");
                if (!ruleIds.isEmpty()) {
                    Set<Rule> rules = new HashSet<>();
                    for (String ruleId : ruleIds) {
                        String ruleJson = jedis.get("rule:" + ruleId);
                        if (ruleJson != null) {
                            Rule rule = objectMapper.readValue(ruleJson, Rule.class);
                            rules.add(rule);
                        }
                    }
                    player.setRules(rules);
                } else {
                    player.setRules(new HashSet<>());
                }

                logger.info(String.format("Loaded player %d with %d rules", id, player.getRules().size()));
                return player;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding player: " + e.getMessage());
            throw new RuntimeException("Failed to find player", e);
        }
    }

    public Set<Player> findPlayersForTicker(Long tickerId, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Player> players = new HashSet<>();
            String playersKey = String.format("ticker:%d:players:%s", tickerId, className);
            Set<String> playerIds = jedis.smembers(playersKey);

            for (String id : playerIds) {
                Player player = findPlayerById(Long.valueOf(id), className);
                if (player != null) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    public void savePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (player.getId() == null) {
                player.setId(jedis.incr("seq:player"));
            }

            // Save rules first and maintain relationships
            String rulesKey = String.format("player:%d:rules", player.getId());
            jedis.del(rulesKey); // Clear existing rules

            if (player.getRules() != null) {
                for (Rule rule : player.getRules()) {
                    if (rule.getId() == null) {
                        rule.setId(jedis.incr("seq:rule"));
                    }
                    // Save rule
                    String ruleJson = objectMapper.writeValueAsString(rule);
                    jedis.set("rule:" + rule.getId(), ruleJson);
                    // Add to player's rules set
                    jedis.sadd(rulesKey, rule.getId().toString());
                }
            }

            // Store references before removing
            Set<Rule> rules = new HashSet<>(player.getRules() != null ? player.getRules() : new HashSet<>());
            Ticker ticker = player.getTicker();

            // Temporarily remove circular references
            player.setTicker(null);
            player.setRules(null);

            // Save the player
            String json = objectMapper.writeValueAsString(player);
            String playerKey = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.set(playerKey, json);

            // Restore references
            player.setTicker(ticker);
            player.setRules(rules);

            logger.info(String.format("Saved player %d with %d rules", player.getId(), rules.size()));
        } catch (Exception e) {
            logger.severe("Error saving player: " + e.getMessage());
            throw new RuntimeException("Failed to save player", e);
        }
    }

    public void deletePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove player's rules
            if (player.getRules() != null) {
                player.getRules().forEach(rule -> ruleHelper.deleteRule(rule.getId()));
            }

            // Remove player from ticker's player set
            if (player.getTicker() != null) {
                String playersKey = String.format("ticker:%d:players:%s",
                        player.getTicker().getId(),
                        player.getPlayerClassName());
                jedis.srem(playersKey, player.getId().toString());
            }

            // Delete the player
            String key = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.del(key);
        } catch (Exception e) {
            logger.severe("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    public Long getNextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.severe("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    public void addPlayerToTicker(Ticker ticker, Player player) {
        logger.info("Adding player " + player.getId() + " to ticker " + ticker.getId());

        try {
            // Set up relationships
            player.setTicker(ticker);
            if (ticker.getPlayers() == null) {
                ticker.setPlayers(new HashSet<>());
            }
            ticker.getPlayers().add(player);

            // Save both entities
            savePlayer(player);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to ticker " + ticker.getId());
        } catch (Exception e) {
            logger.severe("Error adding player to ticker: " + e.getMessage());
            throw new RuntimeException("Failed to add player to ticker", e);
        }
    }
}
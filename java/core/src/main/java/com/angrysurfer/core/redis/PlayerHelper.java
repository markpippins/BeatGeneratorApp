package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class PlayerHelper {
    private static final Logger logger = LoggerFactory.getLogger(PlayerHelper.class.getName());

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RuleHelper ruleHelper;

    public PlayerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.ruleHelper = new RuleHelper(jedisPool, objectMapper);
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
            logger.error("Error finding player: " + e.getMessage());
            throw new RuntimeException("Failed to find player", e);
        }
    }

    public Set<Player> findPlayersForSession(Long sessionId, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Player> players = new HashSet<>();
            String playersKey = String.format("session:%d:players:%s", sessionId, className);
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

    public Long nextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    static final int CACHE_SIZE = 50;

    public Long[] getCachedPlayerIds() {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < CACHE_SIZE; i++)
            ids.add(nextPlayerId());

        return ids.toArray(new Long[ids.size()]);

    }
    // public Long[] getPlayerIdsForSession() {
    // try (Jedis jedis = jedisPool.getResource()) {
    // Set<String> keys = jedis.keys("player:*");
    // Long[] ids = new Long[keys.size()];
    // int i = 0;
    // for (String key : keys) {
    // ids[i++] = Long.valueOf(key.split(":")[2]);
    // }
    // return ids;
    // } catch (Exception e) {
    // logger.error("Error getting player IDs: " + e.getMessage());
    // throw new RuntimeException("Failed to get player IDs", e);
    // }
    // }

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
            Session session = player.getSession();

            // Temporarily remove circular references
            player.setSession(null);
            player.setRules(null);

            // Save the player
            String json = objectMapper.writeValueAsString(player);
            String playerKey = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.set(playerKey, json);

            // Restore references
            player.setSession(session);
            player.setRules(rules);

            logger.info(String.format("Saved player %d with %d rules", player.getId(), rules.size()));
        } catch (Exception e) {
            logger.error("Error saving player: " + e.getMessage());
            throw new RuntimeException("Failed to save player", e);
        }
    }

    public void deletePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove player's rules
            if (player.getRules() != null) {
                player.getRules().forEach(rule -> ruleHelper.deleteRule(rule.getId()));
            }

            // Remove player from session's player set
            if (player.getSession() != null) {
                String playersKey = String.format("session:%d:players:%s",
                        player.getSession().getId(),
                        player.getPlayerClassName());
                jedis.srem(playersKey, player.getId().toString());
            }

            // Delete the player
            String key = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    public Long getNextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    public void addPlayerToSession(Session session, Player player) {
        logger.info("Adding player " + player.getId() + " to session " + session.getId());

        try {
            // Set up relationships
            player.setSession(session);
            if (session.getPlayers() == null) {
                session.setPlayers(new HashSet<>());
            }
            session.getPlayers().add(player);

            // Save both entities
            savePlayer(player);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to session " + session.getId());
        } catch (Exception e) {
            logger.error("Error adding player to session: " + e.getMessage());
            throw new RuntimeException("Failed to add player to session", e);
        }
    }

    public Player newPlayer() {
        Player player = new Strike();
        player.setId(getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        savePlayer(player);
        return player;

    }
}
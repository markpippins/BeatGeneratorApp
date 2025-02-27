package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RedisRuleHelper {
    private static final Logger logger = Logger.getLogger(RedisRuleHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisRuleHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public Rule findRuleById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("rule:" + id);
            return json != null ? objectMapper.readValue(json, Rule.class) : null;
        } catch (Exception e) {
            logger.severe("Error finding rule: " + e.getMessage());
            return null;
        }
    }

    public Set<Rule> findRulesForPlayer(Long playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Rule> rules = new HashSet<>();
            String rulesKey = "player:" + playerId + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);
            
            for (String id : ruleIds) {
                Rule rule = findRuleById(Long.valueOf(id));
                if (rule != null) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    public void saveRule(Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (rule.getId() == null) {
                rule.setId(jedis.incr("seq:rule"));
            }

            // Temporarily remove circular references
            Player player = rule.getPlayer();
            rule.setPlayer(null);

            // Save the rule
            String json = objectMapper.writeValueAsString(rule);
            jedis.set("rule:" + rule.getId(), json);

            // Update player-rule relationship
            if (player != null) {
                String rulesKey = "player:" + player.getId() + ":rules";
                jedis.sadd(rulesKey, rule.getId().toString());
            }

            // Restore references
            rule.setPlayer(player);
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            throw new RuntimeException("Failed to save rule", e);
        }
    }

    public void deleteRule(Long ruleId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Rule rule = findRuleById(ruleId);
            if (rule != null && rule.getPlayer() != null) {
                String rulesKey = "player:" + rule.getPlayer().getId() + ":rules";
                jedis.srem(rulesKey, ruleId.toString());
            }
            jedis.del("rule:" + ruleId);
        } catch (Exception e) {
            logger.severe("Error deleting rule: " + e.getMessage());
            throw new RuntimeException("Failed to delete rule", e);
        }
    }

    public Long getNextRuleId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:rule");
        } catch (Exception e) {
            logger.severe("Error getting next rule ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next rule ID", e);
        }
    }
}
package com.angrysurfer.core.redis;

import java.io.File;
import java.util.logging.Logger;

import com.angrysurfer.core.config.UserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
public class UserConfigHelper {
    private static final Logger logger = Logger.getLogger(UserConfigHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public UserConfigHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public UserConfig loadConfigFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("userconfig");
            if (json != null) {
                return objectMapper.readValue(json, UserConfig.class);
            }
        } catch (Exception e) {
            logger.severe("Error loading config from Redis: " + e.getMessage());
        }
        return null;
    }

    public UserConfig loadConfigFromJSON(String configPath) {
        try {
            logger.info("Loading UserConfig from: " + configPath);
            return objectMapper.readValue(new File(configPath), UserConfig.class);
        } catch (Exception e) {
            logger.severe("Error loading config from JSON: " + e.getMessage());
            throw new RuntimeException("Failed to load config from JSON", e);
        }
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
            logger.info("Saved UserConfig to Redis");
        } catch (Exception e) {
            logger.severe("Error saving config to Redis: " + e.getMessage());
            throw new RuntimeException("Failed to save config", e);
        }
    }
}
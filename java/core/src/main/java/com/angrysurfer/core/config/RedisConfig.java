package com.angrysurfer.core.config;

import com.angrysurfer.core.util.LogManager;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig {
    private static final LogManager logger = LogManager.getInstance();
    
    public static JedisPool createJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        
        try {
            JedisPool pool = new JedisPool(poolConfig, "localhost", 6379);
            logger.info("RedisConfig", "Successfully created Redis connection pool");
            return pool;
        } catch (Exception e) {
            logger.error("RedisConfig", "Failed to create Redis connection pool", e);
            throw new RuntimeException("Could not initialize Redis connection", e);
        }
    }
}
